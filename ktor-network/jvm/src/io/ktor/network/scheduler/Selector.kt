/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.network.scheduler

import kotlinx.coroutines.channels.Channel
import java.nio.channels.*
import java.nio.channels.Selector

internal class Selector {
    private val thread: Thread = Thread.currentThread()
    private val selector = Selector.open()!!

    private val interestQueue = ArrayList<SelectionKey>(1024)

    private var generation = 0

    /**
     * This inbox is only used to migrate selections across workers and on job cancellation
     * so it's negative performance impact is limited.
     */
    private val inbox = Channel<Any>(Channel.UNLIMITED)

    fun select(timeout: Long) {
        processInterestQueue()
        if (selector.select(timeout) > 0) {
            selector.selectedKeys().forEach { key ->
                val entry = key.attachment() as? SelectionEntry
                val readyOps = key.readyOps()

                key.interestOps(key.interestOps() and readyOps.inv())
                entry?.let { handleSelection(it, readyOps) }
            }
            selector.selectedKeys().clear()
        }

        if (!inbox.isEmpty) {
            processInbox()
        }
    }

    fun wakeup() {
        selector.wakeup()
    }

    fun wakeup(signal: SelectionEntry) {
        inbox.offer(signal)
        wakeup()
    }

    /**
     * Called when a selectable entity is resumed on another thread so the previous key if exists should be cancelled
     * to avoid selection key pollution that may happen for long-living selectables with a pool with a lot of threads.
     */
    fun scheduleRemove(channel: SelectableChannel) {
        channel.keyFor(selector)?.let { key ->
            if (key.isValid && key.attachment() == null) {
                // We can't cancel key immediately because it is a blocking operation.
                // The other reason is that the key is concurrently used so concurrent cancellation
                // may cause register-cancel race leading to random failures
                // So the checks above are for the optimization purpose only since
                // submitting and waking up a selector is quite expensive.
                inbox.offer(key)
                wakeup()
            }
        }
    }

    fun stop(dispatcher: (SelectionEntry) -> Unit) {
        checkThread()
        // TODO migrate all instead of cancelling
        // TODO how do we cancel all when the whole dispatcher is going to stop

        selector.keys().forEach { key ->
            val entry = key.attachment() as? SelectionEntry
            key.cancel()
            if (entry != null) {
                remove(entry)
                entry.migrateSelectionFrom()
                dispatcher(entry)
            }
        }

        // TODO postpone selector shutdown
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

    private fun processInbox() {
        val inbox = inbox

        do {
            when (val entry = inbox.poll()) {
                null -> return
                is SelectableChannel -> entry.keyFor(selector)?.let { key ->
                    if (key.isValid && key.attachment() == null) {
                        key.cancel()
                    }
                }
                is SelectionEntry -> entry.resumeIfFailed()
            }
        } while (true)
    }

    fun add(entry: SelectionEntry): SelectionKey {
        entry.selectionKey?.let { key ->
            if (key.selector() === selector) return key
            throw IllegalStateException("This entry has been already registered to another selector")
        }

        entry.channel.keyFor(selector)?.let { key ->
            val head = key.attachment() as? SelectionEntry
            if (head == null) {
                key.attach(entry)
            } else {
                head.add(entry)
            }
            entry.selectionKey = key
            addInterestChange(entry)
            return key
        }

        entry.remove()
        return entry.channel.register(selector, entry.interest, entry).also { entry.selectionKey = it }
    }

    fun remove(entry: SelectionEntry) {
        val key = entry.selectionKey ?: entry.channel.keyFor(selector) ?: return
        val head = key.attachment() as? SelectionEntry ?: return

        entry.selectionKey = null

        if (head === entry) {
            val next = entry.next
            entry.remove()
            key.attach(next)
            return
        }

        entry.remove()
    }

    fun addInterestChange(entry: SelectionEntry) {
        entry.selectionKey?.let {
            interestQueue.add(it)
        }
    }

    private fun processInterestQueue() {
        val generation = generation++

        interestQueue.forEach { key ->
            if (key.isValid) {
                key.computeInterest(generation)
            }
        }
        interestQueue.clear()
    }

    private fun SelectionKey.computeInterest(generation: Int) {
        var interestOps = 0
        val head = attachment() as? SelectionEntry
        if (head != null) {
            if (head.generation == generation) {
                return
            }
            var current: SelectionEntry = head

            do {
                interestOps = interestOps or current.interest
                current.generation = generation
                current = current.next ?: break
            } while (true)
        }

        interestOps(interestOps)
    }

    fun checkThread() {
        assert(thread === Thread.currentThread())
    }

    fun checkSelector() {
        if (!selector.isOpen) throw ClosedSelectorException()
    }

    val isClosed: Boolean get() = !selector.isOpen
}
