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
    fun testPeekFromEmptyToEmpty() = testSuspend {
        val empty =  ByteChannel()
        empty.close()

        withMemory(1024) {
            empty.peekTo(it, 0)
        }
    }

    @Test
    fun testPeekFrom() = testSuspend {
        val channel =  ByteChannel()
        launch {
            repeat(4096) {
                channel.writeInt(42)
                channel.writeByte(42)
            }
            channel.close()
        }

        while (!channel.isClosedForRead) {
            withMemory(1024) {
                channel.peekTo(it, 1)
            }
        }
    }
}
