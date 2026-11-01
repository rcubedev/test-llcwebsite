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
 * Adds the [exportSite][TASK_NAME] task which renders a Spring Boot application to static files
 * for deployment to a static host.
 *
 * When the Spring Boot plugin is present, the main class is inferred from `bootRun`.
 *
 * The application is responsible for rendering the files; see [StaticExport].
 */
abstract class StaticExportPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.pluginManager.apply(JavaPlugin::class)
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