package com.exactpro.th2.codec.soup.bin.tcp

import java.nio.ByteBuffer
import kotlin.text.Charsets.US_ASCII

fun ByteBuffer.getBytes(length: Int): ByteArray = ByteArray(length).apply(::get)
fun ByteBuffer.getString(length: Int): String = getBytes(length).toString(US_ASCII)
fun ByteBuffer.getTrimmedString(length: Int): String = getString(length).trim()

fun ByteBuffer.putString(value: String, length: Int = value.length) {
    val bytes = value.padStart(length).toByteArray(US_ASCII)
    check(bytes.size == length) { "Value does not fit in $length bytes: $value" }
    put(bytes)
}