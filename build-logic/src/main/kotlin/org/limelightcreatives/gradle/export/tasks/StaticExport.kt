package org.limelightcreatives.gradle.export.tasks

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Runs the Spring Boot application once on a random local port, rendering the site to static files.
 *
 * When the configured [profiles] are active, the application requests each of its pages from itself,
 * writes the responses to [outputDirectory] (clearing its contents first), then shuts down.
 */
@CacheableTask
abstract class StaticExport : JavaExec() {

    @get:Input
    abstract val profiles: ListProperty<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    init {
        systemProperty("spring.devtools.restart.enabled", "false")
    }

    @TaskAction
    override fun exec() {
        args(
            "--spring.profiles.active=${profiles.get().joinToString(",")}",
            "--server.port=0",
            "--spring.main.banner-mode=off",
            "--export.out=${outputDirectory.get().asFile.absolutePath}",
        )
        super.exec()
    }
}