package org.limelightcreatives.gradle.export.tasks

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Runs a Spring Boot app once, without a web server, so it can render itself to static files.
 *
 * The app's side of the contract: with the [profiles] active, write the site to the folder
 * given as `--export.out=<dir>` (clearing it first), then exit.
 *
 * Cacheable: the classpath (code, templates, content, static files), main class, JVM and
 * options are all inputs, so an unchanged site comes straight from the build cache.
 */
@CacheableTask
abstract class StaticExport : JavaExec() {

    @get:Input
    abstract val profiles: ListProperty<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    init {
        // Devtools' restarter runs the app on another thread, which would hide a failed export.
        systemProperty("spring.devtools.restart.enabled", "false")
    }

    @TaskAction
    override fun exec() {
        args(
            "--spring.profiles.active=${profiles.get().joinToString(",")}",
            "--spring.main.web-application-type=none",
            "--spring.main.banner-mode=off",
            "--export.out=${outputDirectory.get().asFile.absolutePath}",
        )
        super.exec()
    }
}
