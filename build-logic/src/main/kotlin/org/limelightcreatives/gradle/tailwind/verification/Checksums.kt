package org.limelightcreatives.gradle.tailwind.verification

import java.io.File
import java.security.MessageDigest
import java.util.HexFormat
import java.util.Locale

/**
 * A release's `sha256sums.txt`: lines of `<sha256>  ./<asset name>`.
 * Blank lines and `#` comments are ignored.
 */
class ChecksumList private constructor(private val byAsset: Map<String, String>) {

    operator fun get(asset: String): String? = byAsset[asset]

    companion object {
        fun parse(text: String): ChecksumList = ChecksumList(
            text.lineSequence()
                .map(String::trim)
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .mapNotNull { line ->
                    val parts = line.split(Regex("\\s+"), limit = 2)
                    if (parts.size == 2) parts[1].removePrefix("*").removePrefix("./") to parts[0].lowercase(Locale.ROOT)
                    else null
                }
                .toMap()
        )

        fun read(file: File): ChecksumList = parse(file.readText())
    }
}

/** Hex SHA-256 of a file, streamed so large binaries don't load into memory. */
fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().use { input ->
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return HexFormat.of().formatHex(digest.digest())
}
