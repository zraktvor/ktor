/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.utils.io

import io.ktor.utils.io.concurrent.*
import kotlinx.coroutines.*
import kotlinx.coroutines.debug.junit4.*
import org.junit.*
import java.io.*
import kotlin.system.*
import kotlin.test.*
import kotlin.test.Test

class ByteBufferChannelTest {

//    @get:Rule
//    public val timeoutRule: CoroutinesTimeout by lazy { CoroutinesTimeout.seconds(120) }

    @Test
    fun testCompleteExceptionallyJob() {
        val channel = ByteBufferChannel(false)
        Job().also { channel.attachJob(it) }.completeExceptionally(IOException("Text exception"))

        assertFailsWith<IOException> { runBlocking { channel.readByte() } }
    }

    @Test
    fun readRemainingThrowsOnClosed() = runBlocking {
        val channel = ByteBufferChannel(false)
        channel.writeFully(byteArrayOf(1, 2, 3, 4, 5))
        channel.close(IllegalStateException("closed"))

        assertFailsWith<IllegalStateException>("closed") {
            channel.readRemaining()
        }
        Unit
    }

    @Test
    fun testWriteWriteAvailableRaceCondition() = runBlocking {
        testWriteXRaceCondition { it.writeAvailable(1) { it.put(1) } }
    }

    @Test
    fun testWriteByteRaceCondition() = runBlocking {
        testWriteXRaceCondition { it.writeByte(1) }
    }

    @Test
    fun testWriteIntRaceCondition() = runBlocking {
        testWriteXRaceCondition { it.writeInt(1) }
    }

    @Test
    fun testWriteShortRaceCondition() = runBlocking {
        testWriteXRaceCondition { it.writeShort(1) }
    }

    @Test
    fun testWriteLongRaceCondition() = runBlocking {
        testWriteXRaceCondition { it.writeLong(1) }
    }

    private fun testWriteXRaceCondition(writer: suspend (ByteChannel) -> Unit): Unit = runBlocking {
        val channel = ByteBufferChannel(false)

        val job1 = GlobalScope.async {
            try {
                repeat(10_000_000) {
                    writer(channel)
                    channel.flush()
                }
                channel.close()
            } catch (cause: Throwable) {
                channel.close(cause)
                throw cause
            }
        }
        val job2 = GlobalScope.async {
            channel.readRemaining()
        }
        job1.await()
        job2.await()
    }

    @Test
    fun testReadRemaining() = runBlocking {
        var job1: Deferred<Unit>? = null
        var job2: Deferred<Unit>? = null
        var finished = false
        lateinit var channel: ByteBufferChannel
        val timeout = GlobalScope.createTimeout(timeoutMs = 1000) {
            channel.Log.println("TIMEOUT")
            channel.Log.stop()
            channel.Log.printAll()
            job1!!.cancel()
            job2!!.cancel()
            finished = true
        }
        repeat(100000) {
            if (finished) return@repeat
            channel = ByteBufferChannel(false)
            job2 = GlobalScope.async {
                try {
                    timeout.withTimeout {
//                     val time =  measureTimeMillis {
                        channel.readRemaining()
                    }
//                    println("TIME $time")
                } catch (cause: Throwable) {
                    channel.close(cause)
                    throw cause
                }
            }
            job1 = GlobalScope.async {
                try {
                    repeat(5000) {
                        channel.writeByte(1)
                        channel.flush()
                    }
                    channel.close()
                } catch (cause: Throwable) {
                    channel.close(cause)
                    throw cause
                }
            }
            job1!!.await()
            job2!!.await()
        }
    }

    @Test
    fun testReadAvailable() = runBlocking {
        val channel = ByteBufferChannel(true)
        channel.writeFully(byteArrayOf(1, 2))

        val read1 = channel.readAvailable(4) { it.position(it.position() + 4) }
        assertEquals(-1, read1)

        channel.writeFully(byteArrayOf(3, 4))
        val read2 = channel.readAvailable(4) { it.position(it.position() + 4) }
        assertEquals(4, read2)
    }

    @Test
    fun testAwaitContent() = runBlocking {
        val channel = ByteBufferChannel(true)

        var awaitingContent = false
        launch {
            awaitingContent = true
            channel.awaitContent()
            awaitingContent = false
        }

        yield()
        assertTrue(awaitingContent)
        channel.writeByte(1)
        yield()
        assertFalse(awaitingContent)
    }
}

/**
 * Infinite timeout in milliseconds.
 */
internal const val INFINITE_TIMEOUT_MS = Long.MAX_VALUE

internal class Timeout(
    private val name: String,
    private val timeoutMs: Long,
    private val clock: () -> Long,
    private val scope: CoroutineScope,
    private val onTimeout: suspend () -> Unit
) {

    private var lastActivityTime: Long by shared(0)
    private var isStarted by shared(false)

    private var workerJob = initTimeoutJob()

    fun start() {
        lastActivityTime = clock()
        isStarted = true
    }

    fun stop() {
        isStarted = false
    }

    fun finish() {
        workerJob?.cancel()
    }

    private fun initTimeoutJob(): Job? {
        if (timeoutMs == INFINITE_TIMEOUT_MS) return null
        return scope.launch(scope.coroutineContext + CoroutineName("Timeout $name")) {
            try {
                while (true) {
                    if (!isStarted) {
                        lastActivityTime = clock()
                    }
                    val remaining = lastActivityTime + timeoutMs - clock()
                    if (remaining <= 0 && isStarted) {
                        break
                    }

                    delay(remaining)
                }
                yield()
                onTimeout()
            } catch (cause: Throwable) {
                // no op
            }
        }
    }
}

/**
 * Starts timeout coroutine that will invoke [onTimeout] after [timeoutMs] of inactivity.
 * Use [Timeout] object to wrap code that can timeout or cancel this coroutine
 */
internal fun CoroutineScope.createTimeout(
    name: String = "",
    timeoutMs: Long,
    clock: () -> Long = { System.currentTimeMillis() },
    onTimeout: suspend () -> Unit
): Timeout {
    return Timeout(name, timeoutMs, clock, this, onTimeout)
}

internal inline fun <T> Timeout?.withTimeout(block: () -> T): T {
    if (this == null) {
        return block()
    }

    start()
    return try {
        block()
    } finally {
        stop()
    }
}
