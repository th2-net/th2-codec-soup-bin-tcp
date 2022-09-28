package com.exactpro.th2.codec.soup.bin.tcp.codec

import com.exactpro.th2.codec.soup.bin.tcp.getTrimmedString
import com.exactpro.th2.codec.soup.bin.tcp.putString
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.Message.Builder
import com.exactpro.th2.common.message.getString
import com.exactpro.th2.common.message.set
import java.nio.ByteBuffer

class LoginRequestPacketPacketCodec : AbstractPacketCodec('L', "LoginRequest", PACKET_LENGTH) {
    override fun decode(buffer: ByteBuffer, header: Builder) {
        header[USERNAME_FIELD] = buffer.getTrimmedString(USERNAME_LENGTH)
        header[PASSWORD_FIELD] = buffer.getTrimmedString(PASSWORD_LENGTH)
        header[REQUESTED_SESSION_FIELD] = buffer.getTrimmedString(REQUESTED_SESSION_LENGTH)
        header[REQUESTED_SEQUENCE_NUMBER_FIELD] = buffer.getTrimmedString(REQUESTED_SESSION_LENGTH)
    }

    override fun encodeHeader(header: Message, buffer: ByteBuffer) {
        buffer.putString(header.getRequiredField(USERNAME_FIELD), USERNAME_LENGTH)
        buffer.putString(header.getRequiredField(PASSWORD_FIELD), PASSWORD_LENGTH)
        buffer.putString(header.getString(REQUESTED_SESSION_FIELD) ?: "", REQUESTED_SESSION_LENGTH)
        buffer.putString(header.getString(REQUESTED_SEQUENCE_NUMBER_FIELD) ?: "0", REQUESTED_SEQUENCE_NUMBER_LENGTH)
    }

    companion object {
        private const val USERNAME_LENGTH = 6
        private const val USERNAME_FIELD = "Username"
        private const val PASSWORD_LENGTH = 10
        private const val PASSWORD_FIELD = "Password"
        private const val REQUESTED_SESSION_LENGTH = 10
        private const val REQUESTED_SESSION_FIELD = "RequestedSession"
        private const val REQUESTED_SEQUENCE_NUMBER_LENGTH = 20
        private const val REQUESTED_SEQUENCE_NUMBER_FIELD = "RequestedSequenceNumberLength"
        private const val PACKET_LENGTH = USERNAME_LENGTH + PASSWORD_LENGTH + REQUESTED_SESSION_LENGTH + REQUESTED_SEQUENCE_NUMBER_LENGTH
    }
}