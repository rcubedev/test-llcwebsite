package org.limelightcreatives.gradle.tailwind.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

/**
 * Runs the Tailwind CLI over one source set's entry stylesheet. Up to date (or restored from
 * the build cache) unless a stylesheet, a scanned file, an option or the CLI itself changed.
 * Skipped as `NO-SOURCE` when there are no stylesheets.
 */
@CacheableTask
abstract class TailwindCompile @Inject constructor(
    private val exec: ExecOperations,
) : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val binary: RegularFileProperty

    /** The source set's stylesheets: the entry and everything it may import. */
    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val stylesheets: ConfigurableFileCollection

    /** Path of the entry stylesheet within [stylesheets], e.g. `app.css`. */
    @get:Input
    abstract val entry: Property<String>

    /** Files Tailwind scans for class names. */
    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val content: ConfigurableFileCollection

    @get:Input
    abstract val minify: Property<Boolean>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    /** Where the CSS goes inside [outputDirectory], e.g. `static/css/app.css`. */
    @get:Input
    abstract val outputPath: Property<String>

    @TaskAction
    fun compile() {
        val input = findEntry()
        // The task owns its output folder: clear it so a changed outputPath leaves no stale CSS behind.
        outputDirectory.get().asFile.deleteRecursively()
        val output = outputDirectory.file(outputPath).get().asFile
        exec.exec {
            executable(binary.get().asFile.absolutePath)
            args("--input", input.absolutePath, "--output", output.absolutePath)
            if (minify.get()) args("--minify")
        }
    }

    private fun findEntry(): File {
        val wanted = entry.get()
        val found = mutableListOf<File>()
        stylesheets.asFileTree.visit {
            if (!isDirectory && relativePath.pathString == wanted) found += file
        }
        return when (found.size) {
            1 -> found.single()
            0 -> throw GradleException(
                "No entry stylesheet '$wanted' among ${stylesheets.files.joinToString { it.path }}. " +
                    "Set tailwind { entry = ... } on the source set."
            )
            else -> throw GradleException("More than one '$wanted': ${found.joinToString { it.path }}")
        }
    }
}
