package org.limelightcreatives.gradle.tailwind.verification

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ChecksumListTest {

    @Test
    fun `reads sha256sum output`() {
        val list = ChecksumList.parse(
            """
            # a comment
            ABC123  ./tailwindcss-linux-x64

            def456 *tailwindcss-windows-x64.exe
            789abc  tailwindcss-macos-arm64
            """.trimIndent()
        )
        assertEquals("abc123", list["tailwindcss-linux-x64"], "lower-cased, ./ dropped")
        assertEquals("def456", list["tailwindcss-windows-x64.exe"], "binary-mode * dropped")
        assertEquals("789abc", list["tailwindcss-macos-arm64"])
        assertNull(list["tailwindcss-linux-arm64"])
    }

    @Test
    fun `ignores malformed lines`() {
        val list = ChecksumList.parse("just-one-field\n\n   \nabc  ./ok")
        assertEquals("abc", list["ok"])
        assertNull(list["just-one-field"])
    }

    @Test
    fun `hashes a file`(@TempDir dir: File) {
        val file = dir.resolve("hello.txt").apply { writeText("hello\n") }
        assertEquals("5891b5b522d5df086d0ff0b110fbd9d21bb4fc7163af34d08286a2e846f6be03", file.sha256())
    }

    @Test
    fun `hashes files larger than the read buffer`(@TempDir dir: File) {
        val file = dir.resolve("big.bin").apply { writeBytes(ByteArray(200_000) { (it % 251).toByte() }) }
        val expected = java.security.MessageDigest.getInstance("SHA-256").digest(file.readBytes())
            .joinToString("") { "%02x".format(it) }
        assertEquals(expected, file.sha256())
    }
}
