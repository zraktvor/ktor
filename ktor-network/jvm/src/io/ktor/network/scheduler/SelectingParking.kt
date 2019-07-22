/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.network.scheduler

import kotlinx.coroutines.*
import kotlinx.coroutines.scheduling.*
import java.util.concurrent.*

@UseExperimental(InternalCoroutinesApi::class)
class SelectingParking : Parking<Thread> {
    private val selectors = ConcurrentHashMap<Thread, Selector>()

    // TODO implement shutdown sequence

    override fun park(worker: Thread, nanoseconds: Long) {
        currentSelector().select(TimeUnit.NANOSECONDS.toMillis(nanoseconds))
    }

    override fun unpark(worker: Thread) {
        selectors[worker]?.wakeup()
    }

    override fun workerStopped(worker: Thread) {
        selectors.remove(worker)?.stop { entry ->
            currentSelector().add(entry)
        }
    }

    internal fun register(job: Job) {
    }

    internal fun currentSelector(): Selector = currentSelectorOrNull() ?: createSelector()
    internal fun currentSelectorOrNull(): Selector? = selectors[Thread.currentThread()]

    private fun createSelector(): Selector {
        val selector = Selector()
        selectors[Thread.currentThread()] = selector
        return selector
    }
}
