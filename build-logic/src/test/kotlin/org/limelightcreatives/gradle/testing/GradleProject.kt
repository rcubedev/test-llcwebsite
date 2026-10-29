package org.limelightcreatives.gradle.testing

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File

/**
 * A throwaway Gradle build for TestKit tests, run with the plugins under test, the
 * configuration cache, and a build cache in [buildCache] (shareable between projects).
 */
class GradleProject(val dir: File, private val buildCache: File = dir.resolve("build-cache")) {

    init {
        file("settings.gradle.kts", """
            rootProject.name = "demo"
            buildCache { local { directory = file("${buildCache.invariantSeparatorsPath}") } }
        """)
    }

    /** Writes [text] (trimmed of common indentation) to [path], creating folders. */
    fun file(path: String, text: String): File =
        dir.resolve(path).apply {
            parentFile.mkdirs()
            writeText(text.trimIndent() + "\n")
        }

    fun read(path: String): String = dir.resolve(path).readText()

    fun exists(path: String): Boolean = dir.resolve(path).exists()

    fun build(vararg args: String): BuildResult = runner(*args).build()

    fun buildAndFail(vararg args: String): BuildResult = runner(*args).buildAndFail()

    private fun runner(vararg args: String): GradleRunner = GradleRunner.create()
        .withProjectDir(dir)
        .withPluginClasspath()
        .withArguments(*args, "--configuration-cache", "--build-cache", "--stacktrace")
}

fun BuildResult.outcome(task: String): TaskOutcome? = task(task)?.outcome

val BuildResult.reusedConfigurationCache: Boolean get() = "Reusing configuration cache." in output
