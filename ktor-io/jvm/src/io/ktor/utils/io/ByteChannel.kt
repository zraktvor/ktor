package io.ktor.utils.io

import io.ktor.utils.io.core.*
import io.ktor.utils.io.core.Buffer
import io.ktor.utils.io.internal.BufferObjectPool
import java.nio.*

/**
 * Creates channel for reading from the specified byte buffer.
 */
public fun ByteReadChannel(content: ByteBuffer): ByteReadChannel = ByteBufferChannel(content)

/**
 * Creates buffered channel for asynchronous reading and writing of sequences of bytes.
 */
public actual fun ByteChannel(autoFlush: Boolean): ByteChannel = ByteBufferChannel(
    autoFlush = autoFlush,
    pool = BufferObjectPool
)

internal actual fun ByteChannelSequential(autoFlush: Boolean): ByteChannel {
    return ByteChannelSequentialJVM(IoBuffer.Empty, autoFlush)
}

internal actual fun ByteBufferChannel(autoFlush: Boolean): ByteChannel =
    ByteBufferChannel(autoFlush, BufferObjectPool)

internal actual fun ByteBufferReadChannel(
    content: ByteArray, offset: Int, length: Int
): ByteReadChannel {
    if (content.isEmpty()) {
        return ByteReadChannel.Empty
    }

    val head = IoBuffer.Pool.borrow()
    var tail = head

    var start = offset
    val end = start + length
    while (true) {
        tail.reserveEndGap(8)
        val size = minOf(end - start, tail.writeRemaining)
        (tail as Buffer).writeFully(content, start, size)
        start += size

        if (start == end) break
        val current = tail
        tail = IoBuffer.Pool.borrow()
        current.next = tail
    }

    return ByteChannelSequentialJVM(head, false).apply { close() }
}

internal actual fun ByteReadChannelSequential(
    content: ByteArray, offset: Int, length: Int
): ByteReadChannel = ByteReadChannel(content, offset, length)

/**
 * Creates channel for reading from the specified byte array.
 */
public actual fun ByteReadChannel(content: ByteArray, offset: Int, length: Int): ByteReadChannel =
    ByteBufferChannel(ByteBuffer.wrap(content, offset, length))

/**
 * Creates buffered channel for asynchronous reading and writing of sequences of bytes using [close] function to close
 * channel.
 */
@ExperimentalIoApi
public fun ByteChannel(autoFlush: Boolean = false, exceptionMapper: (Throwable?) -> Throwable?): ByteChannel =
    object : ByteBufferChannel(autoFlush = autoFlush) {
        override fun close(cause: Throwable?): Boolean {
            val mappedException = exceptionMapper(cause)
            return super.close(mappedException)
        }
    }
