package io.ktor.utils.io

import io.ktor.utils.io.core.internal.*

class StringsTestPooled : BytePacketBuilderStringsTest() {
    override val pool: VerifyingObjectPool<ChunkBuffer> = VerifyingObjectPool(ChunkBuffer.Pool)
}
