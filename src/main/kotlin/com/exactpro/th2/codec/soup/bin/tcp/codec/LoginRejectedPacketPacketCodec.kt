package com.exactpro.th2.codec.soup.bin.tcp.codec

import com.exactpro.th2.codec.soup.bin.tcp.getString
import com.exactpro.th2.codec.soup.bin.tcp.putString
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.Message.Builder
import com.exactpro.th2.common.message.set
import java.nio.ByteBuffer

class LoginRejectedPacketPacketCodec : AbstractPacketCodec('J', "LoginRejected", REJECT_REASON_CODE_LENGTH) {
    override fun decode(buffer: ByteBuffer, header: Builder) {
        header[REJECT_REASON_CODE_FIELD] = buffer.getString(REJECT_REASON_CODE_LENGTH)
    }

    override fun encodeHeader(header: Message, buffer: ByteBuffer) {
        buffer.putString(header.getRequiredField(REJECT_REASON_CODE_FIELD), REJECT_REASON_CODE_LENGTH)
    }
    
    companion object {
        private const val REJECT_REASON_CODE_LENGTH = 1
        private const val REJECT_REASON_CODE_FIELD = "RejectReasonCode"
    }
}