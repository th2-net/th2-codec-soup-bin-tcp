package com.exactpro.th2.codec.soup.bin.tcp.codec

import com.exactpro.th2.codec.soup.bin.tcp.SoupBinTcpCodec.Companion.MAX_PAYLOAD_LENGTH
import com.exactpro.th2.codec.soup.bin.tcp.getString
import com.exactpro.th2.codec.soup.bin.tcp.putString
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.Message.Builder
import com.exactpro.th2.common.message.set
import java.nio.ByteBuffer

private const val TEXT_FIELD = "Text"

class DebugPacketPacketCodec : AbstractPacketCodec('+', "DebugPacket") {
    override fun decode(buffer: ByteBuffer, header: Builder) {
        header[TEXT_FIELD] = buffer.getString(buffer.remaining())
    }

    override fun encodeHeader(header: Message, buffer: ByteBuffer) {
        buffer.putString(header.getRequiredField(TEXT_FIELD), MAX_PAYLOAD_LENGTH)
    }
}