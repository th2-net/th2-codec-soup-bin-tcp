package com.exactpro.th2.codec.soup.bin.tcp.codec

import com.exactpro.th2.codec.soup.bin.tcp.SoupBinTcpCodec.PacketCodec
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.Message.Builder
import com.exactpro.th2.common.grpc.RawMessage
import com.exactpro.th2.common.message.getString
import com.exactpro.th2.common.message.messageType
import java.nio.ByteBuffer

abstract class AbstractPacketCodec(
    override val type: Char,
    override val name: String,
    override val length: Int = 0,
    override val hasMessage: Boolean = false,
) : PacketCodec {
    override fun decode(buffer: ByteBuffer, header: Builder) = Unit

    override fun encode(header: Message?, payload: RawMessage?, buffer: ByteBuffer) {
        checkNotNull(header) { "header cannot be null for $name" }
        check(payload == null) { "$name cannot have payload" }
        encodeHeader(header, buffer)
    }

    protected open fun encodeHeader(header: Message, buffer: ByteBuffer) = Unit

    protected fun Message.getRequiredField(name: String): String = getString(name) ?: error("Message $messageType is missing required field: $name")
}