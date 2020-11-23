/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.utils.io

import io.ktor.utils.io.core.*
import kotlin.test.*

class ByteChannelTest : ByteChannelParameterizedBase() {

    @Test
    fun testCopyToFromConstantChannel() = test {
        val data = ByteArray(16)

        buildPacket {
            writeFully()
        }
        val channel = createReadChannel(data)
        val result = createChannel()

        channel.copyAndClose(result)
        result.readRemaining()
    }


    @Test
    fun testInitialSize() = test {
        val initial = ByteArray(16 * 1024 * 1024)
        val channel = createReadChannel(initial)

        assertEquals(initial.size, channel.availableForRead)

    }
}
