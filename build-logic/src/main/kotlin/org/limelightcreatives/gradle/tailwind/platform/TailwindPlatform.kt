package org.limelightcreatives.gradle.tailwind.platform

import org.gradle.api.GradleException
import java.util.Locale

/** An OS + CPU combination Tailwind publishes a standalone CLI for. */
data class TailwindPlatform(val os: Os, val cpu: Cpu) {

    enum class Os(val id: String, val extension: String = "") {
        LINUX("linux"),
        MACOS("macos"),
        WINDOWS("windows", ".exe"),
    }

    enum class Cpu(val id: String) {
        X64("x64"),
        ARM64("arm64"),
    }

    /** The release asset name, e.g. `tailwindcss-macos-arm64` or `tailwindcss-windows-x64.exe`. */
    val assetName: String = "tailwindcss-${os.id}-${cpu.id}${os.extension}"

    companion object {
        /** From the `os.name` and `os.arch` system properties. */
        fun of(osName: String, osArch: String): TailwindPlatform {
            val name = osName.lowercase(Locale.ROOT)
            val os = when {
                "win" in name -> Os.WINDOWS
                "mac" in name -> Os.MACOS
                "linux" in name -> Os.LINUX
                else -> throw GradleException("No Tailwind CLI build for OS $osName")
            }
            val cpu = when (osArch.lowercase(Locale.ROOT)) {
                "amd64", "x86_64" -> Cpu.X64
                "aarch64", "arm64" -> Cpu.ARM64
                else -> throw GradleException("No Tailwind CLI build for CPU $osArch")
            }
            return TailwindPlatform(os, cpu)
        }
    }
}
