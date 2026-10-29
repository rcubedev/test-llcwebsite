package org.limelightcreatives.gradle.tailwind

import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.limelightcreatives.gradle.tailwind.verification.sha256
import org.limelightcreatives.gradle.testing.GradleProject
import org.limelightcreatives.gradle.testing.outcome
import java.io.File

/**
 * The checksum check in tailwindInstall, run on local files standing in for the download
 * and the release's sha256sums.txt.
 */
class TailwindInstallFunctionalTest {

    @TempDir
    lateinit var dir: File

    private val asset = "tailwindcss-linux-x64"

    private val project by lazy {
        GradleProject(dir).apply {
            file("build.gradle.kts", """
                import org.limelightcreatives.gradle.tailwind.tasks.TailwindInstall

                plugins { id("org.limelightcreatives.tailwind") }

                tasks.register<TailwindInstall>("install") {
                    downloaded.from("download/$asset")
                    checksums.from("sha256sums.txt")
                    assetName = "$asset"
                    executable = layout.buildDirectory.file("bin/tailwindcss")
                }
            """)
            file("download/$asset", "pretend binary")
        }
    }

    private val installed get() = dir.resolve("build/bin/tailwindcss")

    private val downloadHash get() = project.dir.resolve("download/$asset").sha256()

    private fun publishChecksum(hash: String, name: String = asset) {
        project.file("sha256sums.txt", """
            0000000000000000000000000000000000000000000000000000000000000000  ./tailwindcss-other
            $hash  ./$name
        """)
    }

    @Test
    fun `a matching download is installed as an executable`() {
        publishChecksum(downloadHash)
        assertEquals(SUCCESS, project.build("install").outcome(":install"))
        assertEquals("pretend binary\n", installed.readText())
        assertTrue(installed.canExecute())
    }

    @Test
    fun `a tampered download is refused`() {
        publishChecksum("1".repeat(64))
        val result = project.buildAndFail("install")
        assertTrue("Checksum mismatch for $asset" in result.output, result.output)
        assertFalse(installed.exists(), "nothing was installed")
    }

    @Test
    fun `an asset missing from the release is reported`() {
        publishChecksum(downloadHash, name = "tailwindcss-something-else")
        val result = project.buildAndFail("install")
        assertTrue("No checksum for $asset" in result.output, result.output)
    }

    @Test
    fun `a changed install is verified and replaced again`() {
        publishChecksum(downloadHash)
        project.build("install")
        assertEquals(UP_TO_DATE, project.build("install").outcome(":install"))

        installed.writeText("modified")
        assertEquals(SUCCESS, project.build("install").outcome(":install"))
        assertEquals("pretend binary\n", installed.readText())
    }
}
