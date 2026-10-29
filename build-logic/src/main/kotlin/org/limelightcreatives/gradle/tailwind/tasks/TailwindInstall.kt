package org.limelightcreatives.gradle.tailwind.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.limelightcreatives.gradle.tailwind.verification.ChecksumList
import org.limelightcreatives.gradle.tailwind.verification.sha256

/**
 * Checks the CLI Gradle downloaded against the release's `sha256sums.txt`, then copies it into
 * the build folder as an executable (Gradle's dependency cache doesn't keep the executable bit).
 *
 * Gradle tracks the download and the copy, so if the copy changes this reruns and verifies again.
 */
@DisableCachingByDefault(because = "Copying a large binary into the build cache gains nothing")
abstract class TailwindInstall : DefaultTask() {

    /** The resolved `tailwindCliFiles` configuration: exactly one file. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val downloaded: ConfigurableFileCollection

    @get:Input
    abstract val assetName: Property<String>

    /** The resolved `tailwindChecksumsFiles` configuration: the release's sha256sums.txt. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val checksums: ConfigurableFileCollection

    @get:OutputFile
    abstract val executable: RegularFileProperty

    @TaskAction
    fun install() {
        val source = downloaded.singleFile
        val asset = assetName.get()
        val checksumFile = checksums.singleFile

        val expected = ChecksumList.read(checksumFile)[asset]
            ?: throw GradleException(
                "No checksum for $asset in the release's sha256sums.txt. Tailwind may not publish a binary for this platform."
            )
        val actual = source.sha256()
        if (actual != expected) {
            throw GradleException(
                """
                Checksum mismatch for $asset
                  expected $expected
                  got      $actual
                The download may be corrupted or tampered with, so it was not used.
                Run with --refresh-dependencies to fetch it again.
                """.trimIndent()
            )
        }

        val target = executable.get().asFile
        source.copyTo(target, overwrite = true)
        if (!target.setExecutable(true)) {
            throw GradleException("Could not make $target executable.")
        }
    }
}
