@file:Suppress("FunctionName")

package io.ktor.utils.io.core

import kotlinx.cinterop.*
import io.ktor.utils.io.bits.*
import io.ktor.utils.io.core.internal.*
import io.ktor.utils.io.pool.*

public actual fun ByteReadPacket(
    array: ByteArray,
    offset: Int,
    length: Int,
    block: (ByteArray) -> Unit
): ByteReadPacket {
    if (length == 0) {
        block(array)
        return ByteReadPacket.Empty
    }

    val pool = object : SingleInstancePool<IoBuffer>() {
        private var pinned: Pinned<*>? = null

        override fun produceInstance(): IoBuffer {
            check(pinned == null) { "This implementation can pin only once." }

            val content = array.pin()
            val base = content.addressOf(offset)
            pinned = content

            @Suppress("DEPRECATION")
            return IoBuffer(Memory.of(base, length), null, this as ObjectPool<ChunkBuffer>)
        }

        override fun disposeInstance(instance: IoBuffer) {
            check(pinned != null) { "The array hasn't been pinned yet" }

            @Suppress("DEPRECATION")
            block(array)
            pinned?.unpin()
            pinned = null
        }
    }

    val buffer = pool.borrow().apply { resetForRead() }
    return ByteReadPacket(buffer, pool as ObjectPool<ChunkBuffer>)
}
