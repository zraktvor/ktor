/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.network.sockets.tests

import io.ktor.network.scheduler.*
import io.ktor.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.scheduling.*
import java.net.*
import java.nio.*
import java.nio.channels.*
import kotlin.test.*

@UseExperimental(InternalCoroutinesApi::class)
class SelectionTest {
    @Test
    fun test() {
        val parking = SelectingParking()
        ExperimentalCoroutineDispatcher(parking = parking).use { dispatcher ->
            runBlocking(dispatcher) {
                SocketChannel.open()!!.use { channel ->
                    channel.configureBlocking(false)

                    selectingScope(channel, parking) {
                        if (!channel.connect(InetSocketAddress(9090))) {
                            do {
                                selectConnect()
                            } while (!channel.finishConnect())
                        }

                        val text = BYTES.duplicate()
                        repeat(100000) {
                            text.clear()
                            while (text.hasRemaining()) {
                                if (channel.write(text) == 0) {
                                    selectWrite()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun echoTest() {
        val parking = SelectingParking()
        ExperimentalCoroutineDispatcher(parking = parking).use { dispatcher ->
            runBlocking(dispatcher) {
                SocketChannel.open()!!.use { channel ->
                    channel.configureBlocking(false)

                    selectingScope(channel, parking) {
                        if (!channel.connect(InetSocketAddress(9090))) {
                            do {
                                selectConnect()
                            } while (!channel.finishConnect())
                        }
                    }

                    coroutineScope {
                        val exchange = Channel<String>(1000)
                        launch {
                            var counter = 0
                            while (true) {
                                delay(5000)
                                if (!exchange.offer("tick ${counter++}\r\n")) break
                            }
                        }
                        launch {
                            selectingScope(channel, parking) {
                                val buffer = ByteBuffer.allocate(8192)
                                do {
                                    buffer.clear()
                                    val rc = channel.read(buffer)
                                    if (rc == -1) break
                                    if (rc == 0) {
                                        selectRead()
                                        continue
                                    }
                                    buffer.flip()
                                    exchange.send(buffer.moveToByteArray().toString(Charsets.UTF_8))
                                } while (true)
                                exchange.close()
                            }
                        }

                        launch {
                            selectingScope(channel, parking) {
                                val buffer = ByteBuffer.allocate(8192)
                                for (message in exchange) {
                                    buffer.clear()
                                    buffer.put(message.toByteArray())
                                    buffer.flip()
                                    while (buffer.hasRemaining()) {
                                        if (channel.write(buffer) == 0) {
                                            selectWrite()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private val TEXT = "Hello, World!\r\n".repeat(100).toByteArray()
        private val BYTES = ByteBuffer.allocateDirect(TEXT.size).apply { put(TEXT); clear() }
    }
}
