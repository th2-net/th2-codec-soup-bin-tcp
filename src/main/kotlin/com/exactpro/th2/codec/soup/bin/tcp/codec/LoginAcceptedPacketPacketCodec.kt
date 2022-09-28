package com.exactpro.th2.codec.soup.bin.tcp.codec

import com.exactpro.th2.codec.soup.bin.tcp.getTrimmedString
import com.exactpro.th2.codec.soup.bin.tcp.putString
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.Message.Builder
import com.exactpro.th2.common.message.set
import java.nio.ByteBuffer

class LoginAcceptedPacketPacketCodec : AbstractPacketCodec('A', "LoginAccepted", PACKET_LENGTH) {
    override fun decode(buffer: ByteBuffer, header: Builder) {
        header[SESSION_FIELD] = buffer.getTrimmedString(SESSION_LENGTH)
        header[SEQUENCE_NUMBER_FIELD] = buffer.getTrimmedString(SEQUENCE_NUMBER_LENGTH)
    }

    override fun encodeHeader(header: Message, buffer: ByteBuffer) {
        buffer.putString(header.getRequiredField(SESSION_FIELD), SESSION_LENGTH)
        buffer.putString(header.getRequiredField(SEQUENCE_NUMBER_FIELD), SEQUENCE_NUMBER_LENGTH)
    }

    companion object {
        private const val SESSION_LENGTH = 10
        private const val SESSION_FIELD = "Session"
        private const val SEQUENCE_NUMBER_LENGTH = 20
        private const val SEQUENCE_NUMBER_FIELD = "SequenceNumber"
        private const val PACKET_LENGTH = SESSION_LENGTH + SEQUENCE_NUMBER_LENGTH
    }
}