package org.limelightcreatives.gradle.export

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.limelightcreatives.gradle.export.tasks.StaticExport

/**
 * Adds `exportSite`: renders a Spring Boot site to static files for a static host.
 *
 * Applies the java plugin. With the Spring Boot plugin, the main class comes from `bootRun`,
 * so a typical site needs no configuration at all. The app does the rendering; see [StaticExport].
 */
abstract class StaticExportPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.apply<JavaPlugin>()
        val extension = project.extensions.create<StaticExportExtension>(StaticExportExtension.NAME)

        project.pluginManager.withPlugin("org.springframework.boot") {
            extension.mainClass.convention(project.tasks.named<JavaExec>("bootRun").flatMap { it.mainClass })
        }

        val main = project.extensions.getByType<SourceSetContainer>().named("main")
        project.tasks.register<StaticExport>(TASK_NAME) {
            group = "build"
            description = "Renders the site to static files."
            classpath = project.files(main.map { it.runtimeClasspath })
            mainClass.set(extension.mainClass)
            profiles.set(extension.profiles)
            outputDirectory.set(extension.outputDirectory)
        }
    }

    private companion object {
        const val TASK_NAME = "exportSite"
    }
}
