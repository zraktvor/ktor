/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.network.scheduler

import kotlinx.coroutines.*
import java.nio.channels.*
import java.util.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class SelectingScope internal constructor(
    override val coroutineContext: CoroutineContext,
    internal val selectionEntry: SelectionEntry
) : CoroutineScope

suspend fun selectingScope(
    channel: SelectableChannel,
    selectors: SelectingParking,
    subRoutine: suspend SelectingScope.() -> Unit
) {
    val entry = SelectionEntry(channel, selectors)
    val selectingJob = Job(coroutineContext[Job])
    selectors.register(selectingJob)
    val scope = SelectingScope(coroutineContext + selectingJob + entry, entry)

    @UseExperimental(InternalCoroutinesApi::class)
    selectingJob.invokeOnCompletion(true) { cause ->
        if (cause != null) {
            entry.resumeConcurrent(cause)
        }
    }

    withContext(scope.coroutineContext) {
        try {
            subRoutine(scope)
        } catch (cause: Throwable) {
            selectingJob.completeExceptionally(cause)
        } finally {
            selectingJob.complete()
            selectors.currentSelectorOrNull()?.let { selector ->
                entry.selectionKey?.cancel()
                entry.selectionKey = null
                selector.remove(entry)
            }
        }
    }
}

private object SelectionEntryKey : CoroutineContext.Key<SelectionEntry>

internal class SelectionEntry(
    val channel: SelectableChannel,
    val selector: SelectingParking
) : ThreadContextElement<Unit> {
    private var continuation: Continuation<Unit>? = null
    var interest: Int = 0
        private set

    override val key: CoroutineContext.Key<*> get() = SelectionEntryKey

    var selectionKey: SelectionKey? = null

    var next: SelectionEntry? = null
        private set

    var generation: Int = 0

    private var failure: Throwable? = null

    private var prev: SelectionEntry? = null

    private var lastSelector: Selector? = null

    fun add(entry: SelectionEntry) {
        assert(entry.channel === channel)

        if (entry.prev != null) {
            entry.remove()
        }
        if (next == null) {
            next = entry
            entry.prev = this
            return
        }

        addFallback(entry)
    }

    fun remove() {
        prev?.let { it.next = next }
        prev = null
        next = null
    }

    override fun updateThreadContext(context: CoroutineContext) {
        assert(continuation == null) {
            "Resumed with a continuation set: $continuation"
        }
        assert(interest == 0) { "Resumed with a non-zero interest: $interest" }

        val selector = selector.currentSelectorOrNull()
        val lastSelector = lastSelector
        if (lastSelector == null) {
            this.lastSelector = selector
            selector?.add(this)
        } else if (lastSelector !== selector) {
            // we are resumed on another thread
            // so we need to notify the previous one that we have migrated
            // this is important because the corresponding selection key should be cancelled once unused.
            this.lastSelector = selector
            lastSelector.scheduleRemove(channel)
        }
    }

    override fun restoreThreadContext(context: CoroutineContext, oldState: Unit) {
        // if there is a continuation then we are not removing it from the selector
        // because we are waiting for selection
        if (continuation == null) {
            val lastSelector = lastSelector
            if (lastSelector != null) {
                this.lastSelector = null
                lastSelector.remove(this)
            }
        } else {
            assert(interest != 0) { "Continuation is set but no interest" }
        }
    }

    /**
     * Invoked when the worker is shutting down
     */
    fun migrateSelectionFrom() {
        lastSelector?.let { selector ->
            selector.checkThread()
            selectionKey?.cancel()
            this.selectionKey = null
            selector.remove(this)
            lastSelector = null
        }
    }

    private fun addFallback(entry: SelectionEntry) {
        var current = next!!

        do {
            val next = current.next
            if (next == null) {
                current.next = entry
                entry.prev = current
                return
            }
            current = next
        } while (true)
    }

    val id: Int = Random().nextInt()

    suspend fun select(interest: Int): Unit =
        suspendCoroutineUninterceptedOrReturn { continuation ->
            require(interest != 0) { "interest shouldn't be zero" }

            val selector = selector.currentSelector()
            val job = continuation.context[Job]
            beforeSuspend(selector, job)

            this.interest = interest
            this.continuation = continuation

            if (job != null && job.isCancelled) {
                this.continuation = null
                this.interest = 0
                throwCancellation(job)
            }
            if (selector.isClosed) {
                this.continuation = null
                this.interest = 0
                throw ClosedSelectorException()
            }

            val selectionKey = selectionKey
            if (selectionKey == null) {
                selector.add(this)
            } else {
                selector.addInterestChange(this)
            }

            lastSelector = selector

            COROUTINE_SUSPENDED
        }

    fun resumeConcurrent(failure: Throwable) {
        this.failure = failure
        lastSelector?.wakeup(this)
    }

    fun resumeIfFailed() {
        failure?.let { resume(Result.failure(it)) }
    }

    fun resume(result: Result<Unit>) {
        interest = 0
        val continuation = continuation
        if (continuation != null) {
            this.continuation = null

            continuation.resumeWith(result)
        }
    }

    private fun beforeSuspend(selector: Selector, job: Job?) {
        selector.checkThread()
        selector.checkSelector()
        if (job != null && job.isCancelled) {
            throwCancellation(job)
        }
    }

    @UseExperimental(InternalCoroutinesApi::class)
    private fun throwCancellation(job: Job): Nothing {
        throw job.getCancellationException()
    }

    override fun hashCode(): Int = id
    override fun equals(other: Any?): Boolean {
        return this === other || other is SelectionEntry && other.channel === this.channel
    }
}
