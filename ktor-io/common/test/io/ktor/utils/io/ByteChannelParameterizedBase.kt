/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.utils.io

import io.ktor.test.dispatcher.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.native.concurrent.*
import kotlin.test.*

@SharedImmutable
internal expect val SCOPES: List<TestScopeFactory>

open class ByteChannelParameterizedBase {
    internal fun test(block: suspend TestScope.() -> Unit) = testSuspend {
        val message = StringBuilder()

        SCOPES.forEach {
            val scope = it.create(coroutineContext)
            val result = runCatching {
                block(scope)
            }.exceptionOrNull() ?: return@forEach


            message.appendLine("Test failed for $it")
            message.appendLine(result.message)
            message.appendLine(result.stackTraceToString())
        }

        if (message.isNotEmpty()) {
            fail(message.toString())
        }
    }
}


internal interface TestScope : CoroutineScope {
    fun createChannel(autoFlush: Boolean = false): ByteChannel
}

internal fun interface TestScopeFactory {
    fun create(coroutineContext: CoroutineContext): TestScope
}

internal class ByteBufferChannelScope(override val coroutineContext: CoroutineContext) : TestScope {
    override fun createChannel(autoFlush: Boolean): ByteChannel {
        return ByteBufferChannel(autoFlush)
    }

    override fun toString(): String = "ByteBufferChannel scope"
}

internal class ByteChannelSequentialScope(override val coroutineContext: CoroutineContext) : TestScope {
    override fun createChannel(autoFlush: Boolean): ByteChannel {
        return ByteChannelSequential(autoFlush)
    }

    override fun toString(): String = "ByteChannelSequential scope"
}
