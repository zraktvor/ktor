/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.network.scheduler

import kotlinx.coroutines.*
import kotlinx.coroutines.scheduling.*
import java.nio.channels.*
import java.util.*
import java.util.concurrent.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

@UseExperimental(InternalCoroutinesApi::class)
internal class SelectingParking : Parking<Thread> {
    private val selectors = ConcurrentHashMap<Thread, Selector>()

    override fun park(worker: Thread, nanoseconds: Long) {
        var selector = selectors[worker]
        if (selector == null) {
            selector = Selector()
            selectors[worker] = selector
        }

        selector.select(TimeUnit.NANOSECONDS.toMillis(nanoseconds))
    }

    override fun unpark(worker: Thread) {
        selectors[worker]?.wakeup()
    }

    override fun workerStopped(worker: Thread) {
        selectors.remove(worker)?.stop()
    }

    private class SelectionEntry(val channel: SelectableChannel, val selector: Selector) {
        private var continuation: Continuation<Unit>? = null
        var interest: Int = 0
            private set
        var key: SelectionKey? = null

        var next: SelectionEntry? = null
            private set
        private var prev: SelectionEntry? = null

        fun add(entry: SelectionEntry) {
            assert(entry.channel === channel)

            if (entry.prev != null) {
                remove()
            }
            if (next == null) {
                next = entry
                entry.prev = this
                return
            }

            addFallback(entry)
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

        fun remove() {
            prev?.let { it.next = next }
            prev = null
            next = null
        }

        val id: Int = Random().nextInt()

        suspend fun select(interest: Int): Unit =
            suspendCoroutineUninterceptedOrReturn { continuation ->
                val selector = selector
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

                if (key == null) {
                    key = selector.add(this)
                }

                COROUTINE_SUSPENDED
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

        private fun throwCancellation(job: Job): Nothing {
            throw job.getCancellationException()
        }

        override fun hashCode(): Int = id
        override fun equals(other: Any?): Boolean {
            return this === other || other is SelectionEntry && other.channel === this.channel
        }
    }

    private class Selector {
        private val thread: Thread = Thread.currentThread()
        private val selector = java.nio.channels.Selector.open()!!

        fun select(timeout: Long) {
            selector.select(timeout)
            selector.selectedKeys().forEach { key ->
                val entry = key.attachment() as? SelectionEntry
                key.interestOps(0)
                entry?.let { handleSelection(it, key.readyOps()) }
            }
            selector.selectedKeys().clear()
        }

        fun wakeup() {
            selector.wakeup()
        }

        fun stop() {
            checkThread()
            // TODO migrate all instead of cancelling
            // TODO how do we cancel all when the whole dispatcher is going to stop

            selector.keys().forEach { key ->
                val entry = key.attachment() as? SelectionEntry
                key.cancel()
                if (entry != null) {
                    remove(entry)
                    entry.resume(Result.failure(ClosedSelectorException()))
                }
            }
            selector.close()
        }

        private fun handleSelection(entry: SelectionEntry, readyOps: Int) {
            var current = entry
            do {
                if (current.interest and readyOps != 0) {
                    current.resume(Result.success(Unit))
                }
                current = current.next ?: break
            } while (true)
        }

        fun add(entry: SelectionEntry): SelectionKey {
            entry.key?.let { key ->
                if (key.selector() === selector) return key
                throw IllegalStateException("This entry has been already registered to another selector")
            }

            entry.channel.keyFor(selector)?.let { key ->
                val head = key.attachment() as SelectionEntry
                head.add(entry)
                entry.key = key
                key.interestOps(key.interestOps() or entry.interest)
                return key
            }

            entry.remove()
            return entry.channel.register(selector, entry.interest, entry).also { entry.key = it }
        }

        fun remove(entry: SelectionEntry) {
            val key = entry.key ?: entry.channel.keyFor(selector) ?: return
            val head = key.attachment() as SelectionEntry

            entry.key = null

            if (head === entry) {
                val next = entry.next
                key.attach(next)
                entry.remove()
                if (next == null) {
                    key.cancel() // TODO is it safe to cancel a key?
                }
                return
            }

            entry.remove()
        }

        fun checkThread() {
            assert(thread === Thread.currentThread())
        }

        fun checkSelector() {
            if (!selector.isOpen) throw ClosedSelectorException()
        }

        val isClosed: Boolean get() = !selector.isOpen
    }
}
