package com.qtekfun.ultimatefiles.domain.usecase

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/** Why a text file can be shown but not changed. */
enum class TextReadOnly { TOO_LARGE, BINARY, NOT_WRITABLE }

/**
 * A decoded text file that remembers how it was stored (encoding, byte order mark, line endings), so saving it again
 * changes only what the user changed. [text] uses `\n` unless the file mixes line endings, which are then left as they are.
 */
data class TextDocument(
    val text: String,
    val charset: Charset,
    val bom: ByteArray,
    val lineEnding: String,
    val readOnly: TextReadOnly?,
) {
    /** Bytes to store [edited], or null when it has characters the file's encoding cannot represent. */
    fun encode(edited: CharSequence): ByteArray? {
        val raw = if (lineEnding == LF) edited.toString() else edited.toString().replace(CRLF, LF).replace(LF, lineEnding)
        val encoder = charset.newEncoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val buffer = runCatching { encoder.encode(java.nio.CharBuffer.wrap(raw)) }.getOrNull() ?: return null
        return bom + ByteArray(buffer.remaining()).also { buffer.get(it) }
    }

    companion object {
        /** Largest file that can be edited; bigger ones show their first part, read-only. */
        const val MAX_EDIT_BYTES = 1024 * 1024

        private const val LF = "\n"
        private const val CRLF = "\r\n"
        private const val CR = "\r"

        /** [bytes] is the whole file, or at least `MAX_EDIT_BYTES + 1` bytes of it when it is larger. */
        fun decode(bytes: ByteArray, writable: Boolean = true): TextDocument {
            val tooLarge = bytes.size > MAX_EDIT_BYTES
            val data = if (tooLarge) cutAtCharacter(bytes) else bytes
            val (bom, bomCharset) = byteOrderMark(data)
            val body = data.copyOfRange(bom.size, data.size)
            val binary = bomCharset.let { it == null || it == Charsets.UTF_8 } && body.any { it == 0.toByte() }
            // ISO-8859-1 maps every byte to one character, so anything that is not valid UTF-8 still round-trips.
            val charset = bomCharset ?: if (!binary && strictDecode(body, Charsets.UTF_8) != null) Charsets.UTF_8 else Charsets.ISO_8859_1
            val strict = strictDecode(body, charset)
            val raw = strict ?: String(body, charset)
            val lineEnding = lineEndingOf(raw)
            val text = if (lineEnding == LF) raw else raw.replace(lineEnding, LF)
            val readOnly = when {
                tooLarge -> TextReadOnly.TOO_LARGE
                binary || strict == null -> TextReadOnly.BINARY
                !writable -> TextReadOnly.NOT_WRITABLE
                else -> null
            }
            return TextDocument(text, charset, bom, lineEnding, readOnly)
        }

        /** The first part of a large file, ending after a line or at least not inside a UTF-8 character. */
        private fun cutAtCharacter(bytes: ByteArray): ByteArray {
            var end = MAX_EDIT_BYTES
            while (end > 0 && bytes[end - 1] != '\n'.code.toByte()) end--
            if (end == 0 || byteOrderMark(bytes).second.let { it != null && it != Charsets.UTF_8 }) {
                end = MAX_EDIT_BYTES
                while (end > MAX_EDIT_BYTES - 3 && bytes[end].toInt() and 0xC0 == 0x80) end--
            }
            return bytes.copyOf(end)
        }

        private fun byteOrderMark(data: ByteArray): Pair<ByteArray, Charset?> {
            fun startsWith(vararg mark: Int) = data.size >= mark.size && mark.indices.all { data[it] == mark[it].toByte() }
            return when {
                startsWith(0xEF, 0xBB, 0xBF) -> data.copyOf(3) to Charsets.UTF_8
                startsWith(0xFF, 0xFE) -> data.copyOf(2) to Charsets.UTF_16LE
                startsWith(0xFE, 0xFF) -> data.copyOf(2) to Charsets.UTF_16BE
                else -> ByteArray(0) to null
            }
        }

        private fun strictDecode(bytes: ByteArray, charset: Charset): String? = runCatching {
            charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        }.getOrNull()

        /** CRLF or CR only when every line break is that one; mixed files are kept byte for byte. */
        private fun lineEndingOf(text: String): String {
            var crlf = 0
            var lf = 0
            var cr = 0
            var i = 0
            while (i < text.length) {
                when (text[i]) {
                    '\r' -> if (i + 1 < text.length && text[i + 1] == '\n') {
                        crlf++
                        i++
                    } else {
                        cr++
                    }
                    '\n' -> lf++
                }
                i++
            }
            return when {
                crlf > 0 && lf == 0 && cr == 0 -> CRLF
                cr > 0 && lf == 0 && crlf == 0 -> CR
                else -> LF
            }
        }
    }
}
