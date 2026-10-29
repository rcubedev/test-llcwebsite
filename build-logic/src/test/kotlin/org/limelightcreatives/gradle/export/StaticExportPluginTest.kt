package org.limelightcreatives.gradle.export

import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.limelightcreatives.gradle.export.tasks.StaticExport

/** How the plugin sets up a project, without running anything. */
class StaticExportPluginTest {

    private fun project(vararg plugins: String): Project = ProjectBuilder.builder().build().also { project ->
        plugins.forEach(project.pluginManager::apply)
        project.pluginManager.apply(StaticExportPlugin::class.java)
    }

    private val Project.export get() = tasks.named<StaticExport>("exportSite").get()

    @Test
    fun `applies java and registers exportSite`() {
        val project = project()
        assertTrue(project.pluginManager.hasPlugin("java"))
        assertTrue("exportSite" in project.tasks.names)
    }

    @Test
    fun `defaults`() {
        val project = project()
        assertEquals(listOf("export"), project.export.profiles.get())
        assertEquals(project.layout.buildDirectory.dir("site").get(), project.export.outputDirectory.get())
        assertFalse(project.export.mainClass.isPresent, "no main class without Spring Boot")
    }

    @Test
    fun `takes the extension's values`() {
        val project = project()
        project.extensions.getByType<StaticExportExtension>().apply {
            mainClass.set("demo.App")
            profiles.set(listOf("export", "prod"))
            outputDirectory.set(project.layout.buildDirectory.dir("public"))
        }
        assertEquals("demo.App", project.export.mainClass.get())
        assertEquals(listOf("export", "prod"), project.export.profiles.get())
        assertEquals(project.layout.buildDirectory.dir("public").get(), project.export.outputDirectory.get())
    }

    @Test
    fun `runs the main source set's runtime classpath`() {
        val project = project()
        val main = project.extensions.getByType<org.gradle.api.tasks.SourceSetContainer>().getByName("main")
        assertTrue(main.output.classesDirs.files.all { it in project.export.classpath.files })
    }

    // bootRun's own main class is normally found by Boot's resolveMainClassName task, which
    // can't run here, so these set it directly and check exportSite follows bootRun.
    private fun Project.bootRunMainClass(name: String) =
        tasks.named<JavaExec>("bootRun").configure { mainClass.set(name) }

    @Test
    fun `with Spring Boot, the main class comes from bootRun`() {
        val project = project("org.springframework.boot")
        project.bootRunMainClass("demo.BootApp")
        assertEquals("demo.BootApp", project.export.mainClass.get())
    }

    @Test
    fun `Spring Boot can be applied after it`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(StaticExportPlugin::class.java)
        project.pluginManager.apply("org.springframework.boot")
        project.bootRunMainClass("demo.BootApp")
        assertEquals("demo.BootApp", project.export.mainClass.get())
    }

    @Test
    fun `an explicit main class wins over bootRun's`() {
        val project = project("org.springframework.boot")
        project.bootRunMainClass("demo.BootApp")
        project.extensions.getByType<StaticExportExtension>().mainClass.set("demo.ExportApp")
        assertEquals("demo.ExportApp", project.export.mainClass.get())
    }
}
