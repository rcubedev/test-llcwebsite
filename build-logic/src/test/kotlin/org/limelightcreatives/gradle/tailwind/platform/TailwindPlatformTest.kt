package org.limelightcreatives.gradle.tailwind.platform

import org.gradle.api.GradleException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class TailwindPlatformTest {

    @ParameterizedTest(name = "{0} / {1} -> {2}")
    @CsvSource(
        "Linux, amd64, tailwindcss-linux-x64",
        "Linux, aarch64, tailwindcss-linux-arm64",
        "Mac OS X, x86_64, tailwindcss-macos-x64",
        "Mac OS X, aarch64, tailwindcss-macos-arm64",
        "Windows 11, amd64, tailwindcss-windows-x64.exe",
        "Windows Server 2022, arm64, tailwindcss-windows-arm64.exe",
    )
    fun `maps os name and arch to the release asset`(os: String, arch: String, asset: String) {
        assertEquals(asset, TailwindPlatform.of(os, arch).assetName)
    }

    @ParameterizedTest(name = "{0} / {1}")
    @CsvSource("SunOS, amd64", "FreeBSD, amd64", "Linux, riscv64", "Linux, x86")
    fun `unsupported platforms fail clearly`(os: String, arch: String) {
        val error = assertThrows<GradleException> { TailwindPlatform.of(os, arch) }
        assert("No Tailwind CLI build" in error.message.orEmpty())
    }
}
