/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.utils.io

import io.ktor.test.dispatcher.*
import io.ktor.utils.io.bits.*
import kotlinx.coroutines.*
import kotlin.test.*

class ByteChannelTest {

    @Test
    fun testPeekToFromEmptyToEmpty() = testSuspend {
        val empty = ByteChannel()
        empty.close()

        withMemory(1024) {
            empty.peekTo(it, 0)
        }
    }

    @Test
    fun testPeekToMemoryLessThanContent() = testSuspend {
        val channel = ByteChannel()
        launch {
            repeat(4096) {
                channel.writeInt(42)
                channel.writeByte(42)
            }
            channel.close()
        }

        var total = 0L
        while (!channel.isClosedForRead) {
            total += withMemory(1024) {
                channel.peekTo(it, 0, min = 1, max = Long.MAX_VALUE)
            }
        }
        assertEquals(5 * 4096, total)
    }

    @Test
    fun testPeekToMemoryLargerThanContent() = testSuspend {
        val channel = ByteChannel()
        launch {
            repeat(1024) {
                channel.writeByte(42)
            }
            channel.close()
        }

        var total = 0L
        while (!channel.isClosedForRead) {
            total += withMemory(4096) {
                channel.peekTo(it, 0, min = 1, max = Long.MAX_VALUE)
            }
        }
        assertEquals(1024, total)
    }

    @Test
    fun testPeekToMemoryEqualsToContent() = testSuspend {
        val channel = ByteChannel()
        launch {
            repeat(1024) {
                channel.writeByte(42)
            }
            channel.close()
        }

        var total = 0L
        while (!channel.isClosedForRead) {
            total += withMemory(1024) {
                channel.peekTo(it, 0, min = 1, max = Long.MAX_VALUE)
            }
        }
        assertEquals(1024, total)
    }

    @Test
    fun testPeekToWithMax() = testSuspend {
        val channel = ByteChannel()
        launch {
            repeat(1024) {
                channel.writeByte(42)
            }
            channel.close()
        }

        val total = withMemory(1024) {
            channel.peekTo(it, 0, min = 1, max = 16)
        }
        assertEquals(16, total)
    }

    @Test
    fun testPeekToWithMin() = testSuspend {
        val channel = ByteChannel()
        val writeJob = launch {
            repeat(16) {
                channel.writeByte(42)
            }
            channel.flush()
            delay(5000)
            repeat(16) {
                channel.writeByte(42)
            }
            channel.close()
        }

        val total = withMemory(1024) {
            channel.peekTo(it, 0, min = 16, max = Long.MAX_VALUE)
        }
        assertEquals(16, total)
        writeJob.cancel()
    }

    @Test
    fun testPeekToWithDestinationOffset() = testSuspend {
        val channel = ByteChannel()
        launch {
            repeat(1024) {
                channel.writeByte(42)
            }
            channel.close()
        }

        val total = withMemory(1024) {
            channel.peekTo(it, destinationOffset = 16, min = 1, max = Long.MAX_VALUE)
        }
        assertEquals(1024 - 16, total)
    }

    @Test
    fun testPeekToWithSourceOffset() = testSuspend {
        val channel = ByteChannel()
        launch {
            repeat(1024) {
                channel.writeByte(42)
            }
            channel.close()
        }

        val total = withMemory(1024) {
            channel.peekTo(it, destinationOffset = 0, offset = 16, min = 1, max = Long.MAX_VALUE)
        }
        assertEquals(1024 - 16, total)
    }

    @Test
    fun testPeekToWithSourceAndDestinationOffset() = testSuspend {
        val channel = ByteChannel()
        launch {
            repeat(1024) {
                channel.writeByte(42)
            }
            channel.close()
        }

        val total = withMemory(1024) {
            channel.peekTo(it, destinationOffset = 16, offset = 16, min = 1, max = Long.MAX_VALUE)
        }
        assertEquals(1024 - 16, total)
    }
}
