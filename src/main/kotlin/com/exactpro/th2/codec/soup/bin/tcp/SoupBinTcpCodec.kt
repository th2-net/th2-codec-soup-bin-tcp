/*
 * Copyright 2022-2022 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exactpro.th2.codec.soup.bin.tcp

import com.exactpro.th2.codec.api.IPipelineCodec
import com.exactpro.th2.codec.api.IReportingContext
import com.exactpro.th2.codec.soup.bin.tcp.SoupBinTcpCodecFactory.Companion.PROTOCOL
import com.exactpro.th2.codec.soup.bin.tcp.codec.ClientHeartbeatPacketPacketCodec
import com.exactpro.th2.codec.soup.bin.tcp.codec.DebugPacketPacketCodec
import com.exactpro.th2.codec.soup.bin.tcp.codec.EndOfSessionPacketPacketCodec
import com.exactpro.th2.codec.soup.bin.tcp.codec.LoginAcceptedPacketPacketCodec
import com.exactpro.th2.codec.soup.bin.tcp.codec.LoginRejectedPacketPacketCodec
import com.exactpro.th2.codec.soup.bin.tcp.codec.LoginRequestPacketPacketCodec
import com.exactpro.th2.codec.soup.bin.tcp.codec.LogoutRequestPacketPacketCodec
import com.exactpro.th2.codec.soup.bin.tcp.codec.SequencedDataPacketPacketCodec
import com.exactpro.th2.codec.soup.bin.tcp.codec.ServerHeartbeatPacketPacketCodec
import com.exactpro.th2.codec.soup.bin.tcp.codec.UnsequencedDataPacketPacketCodec
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.MessageGroup
import com.exactpro.th2.common.grpc.RawMessage
import com.exactpro.th2.common.message.messageType
import com.exactpro.th2.common.message.set
import com.google.protobuf.ByteString
import com.google.protobuf.UnsafeByteOperations
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ByteOrder.BIG_ENDIAN

class SoupBinTcpCodec(private val settings: SoupBinTcpCodecSettings) : IPipelineCodec {
    private val buffer = ByteBuffer.allocate(MAX_PACKET_LENGTH).order(BIG_ENDIAN)

    override fun encode(messageGroup: MessageGroup, context: IReportingContext): MessageGroup {
        val messages = messageGroup.messagesList

        if (messages.isEmpty()) return messageGroup

        check(messages.size <= 2) { "More than 2 messages in group" }

        val first = messages[0]
        val second = messages.getOrNull(1)

        if (messages.size == 2) {
            check(first.hasMessage()) { "First message must be a parsed message" }
            check(second!!.hasRawMessage()) { "Second message must be a raw message" }
        }

        val header = if (first.hasMessage()) first.message else null
        val payload = if (first.hasRawMessage()) first.rawMessage else second?.rawMessage

        val headerProtocol = header?.metadata?.protocol
        val payloadProtocol = payload?.metadata?.protocol

        check(headerProtocol.isNullOrBlank() || headerProtocol == PROTOCOL) { "Unexpected header protocol: $headerProtocol (expected: $PROTOCOL)" }
        check(payloadProtocol.isNullOrBlank() || payloadProtocol == PROTOCOL) { "Unexpected payload protocol: $payloadProtocol (expected: $PROTOCOL)" }

        val messageType = header?.messageType ?: UnsequencedDataPacketPacketCodec.NAME
        val codec = PACKETS_BY_NAME[messageType] ?: error("Unknown message type: $messageType")

        buffer.clear()
        buffer.putShort(0)
        buffer.put(codec.type.code.toByte())

        codec.encode(header, payload, buffer)

        buffer.flip()
        buffer.putShort(0, (buffer.limit() - Short.SIZE_BYTES).toShort())

        val builder = MessageGroup.newBuilder()

        builder.addMessagesBuilder().rawMessageBuilder.apply {
            when {
                header != null && header.hasParentEventId() -> parentEventId = header.parentEventId
                payload != null && payload.hasParentEventId() -> parentEventId = payload.parentEventId
            }

            body = ByteString.copyFrom(buffer)

            metadataBuilder.apply {
                id = header?.metadata?.id ?: payload?.metadata?.id
                protocol = PROTOCOL
                header?.metadata?.propertiesMap?.run(::putAllProperties)
                payload?.metadata?.propertiesMap?.run(::putAllProperties)
            }
        }

        return builder.build()
    }

    override fun decode(messageGroup: MessageGroup, context: IReportingContext): MessageGroup {
        val messages = messageGroup.messagesList

        if (messages.isEmpty()) return messageGroup

        check(messages.size == 1) { "Message group must contain a single raw message" }

        val message = messages[0]

        check(message.hasRawMessage()) { "Message must be a raw one" }

        val encoded = message.rawMessage
        val protocol = encoded.metadata.protocol

        check(protocol.isBlank() || protocol == PROTOCOL) { "Unknown protocol: $protocol" }

        val buffer = encoded.body.asReadOnlyByteBuffer().order(ByteOrder.BIG_ENDIAN)

        check(buffer.remaining() >= HEADER_LENGTH) { "Message does not contain a valid header" }

        val packetLength = buffer.short.toUShort().toInt()

        check(buffer.remaining() == packetLength) { "Unexpected packet length: ${buffer.remaining()} (expected: $packetLength)" }

        val packetType = buffer.get().toInt().toChar()
        val decoder = PACKETS_BY_TYPE[packetType] ?: error("Unknown packet type: $packetType")

        check(decoder.length <= buffer.remaining()) { "No enough enough bytes (${buffer.remaining()} to decode: ${decoder.name} (expected: ${decoder.length})" }

        val builder = MessageGroup.newBuilder()
        val header = builder.addMessagesBuilder().messageBuilder
        val metadata = encoded.metadata

        if (encoded.hasParentEventId()) header.parentEventId = encoded.parentEventId

        header[PACKET_LENGTH_FIELD] = packetLength
        header[PACKET_TYPE_FIELD] = packetType

        header.metadataBuilder.apply {
            this.idBuilder.mergeFrom(metadata.id).addSubsequence(0)
            this.messageType = decoder.name
            this.protocol = PROTOCOL
            putAllProperties(metadata.propertiesMap)
        }

        decoder.decode(buffer, header)

        if (!decoder.hasMessage) {
            check(buffer.remaining() == 0) { "Message has left-over bytes: ${buffer.remaining()}" }
            return builder.build()
        }

        check(buffer.remaining() > 0) { "Packet has message but there are no bytes left to decode" }

        val payload = builder.addMessagesBuilder().rawMessageBuilder

        if (encoded.hasParentEventId()) payload.parentEventId = encoded.parentEventId

        payload.body = UnsafeByteOperations.unsafeWrap(buffer)

        payload.metadataBuilder.apply {
            this.idBuilder.mergeFrom(metadata.id).addSubsequence(1)
            this.protocol = PROTOCOL
            putAllProperties(metadata.propertiesMap)
        }

        return builder.build()
    }

    interface PacketCodec {
        val type: Char
        val name: String
        val length: Int
        val hasMessage: Boolean
        fun decode(buffer: ByteBuffer, header: Message.Builder)
        fun encode(header: Message?, payload: RawMessage?, buffer: ByteBuffer)
    }

    companion object {
        private const val HEADER_LENGTH = 3
        private const val PACKET_LENGTH_FIELD = "PacketLength"
        private const val PACKET_TYPE_FIELD = "PacketType"

        private val MAX_PACKET_LENGTH = UShort.SIZE_BYTES + UShort.MAX_VALUE.toInt()
        val MAX_PAYLOAD_LENGTH = MAX_PACKET_LENGTH - UShort.SIZE_BYTES

        private val PACKETS_BY_TYPE = listOf(
            ClientHeartbeatPacketPacketCodec(),
            DebugPacketPacketCodec(),
            EndOfSessionPacketPacketCodec(),
            LoginAcceptedPacketPacketCodec(),
            LoginRejectedPacketPacketCodec(),
            LoginRequestPacketPacketCodec(),
            LogoutRequestPacketPacketCodec(),
            SequencedDataPacketPacketCodec(),
            ServerHeartbeatPacketPacketCodec(),
            UnsequencedDataPacketPacketCodec(),
        ).associateBy(PacketCodec::type)

        private val PACKETS_BY_NAME = PACKETS_BY_TYPE.values.associateBy(PacketCodec::name)
    }
}
