package com.exactpro.th2.codec.soup.bin.tcp.codec

import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.RawMessage
import java.nio.ByteBuffer

class UnsequencedDataPacketPacketCodec : AbstractPacketCodec('U', NAME, hasMessage = true) {
    override fun encode(header: Message?, payload: RawMessage?, buffer: ByteBuffer) {
        checkNotNull(payload) { "$name must have payload" }
        buffer.put(payload.body.asReadOnlyByteBuffer())
    }

    companion object {
        const val NAME = "UnsequencedData"
    }
}