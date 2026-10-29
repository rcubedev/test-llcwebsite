package org.limelightcreatives.gradle.tailwind

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.getByType
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** How the plugin sets up a project, without running anything. */
class TailwindPluginTest {

    // Not .apply { }: Project has its own apply(Action) member, which wins over Kotlin's extension.
    private val project: Project = ProjectBuilder.builder().build().also {
        it.pluginManager.apply(TailwindPlugin::class.java)
    }

    private val sourceSets get() = project.extensions.getByType<SourceSetContainer>()

    private fun tailwindOf(sourceSet: String) =
        sourceSets.getByName(sourceSet).extensions.getByType<TailwindSourceSet>()

    @Test
    fun `applies the java plugin`() {
        assertTrue(project.pluginManager.hasPlugin("java"))
    }

    @Test
    fun `every source set gets a css folder and defaults`() {
        for (name in listOf("main", "test")) {
            val tailwind = tailwindOf(name)
            assertEquals(setOf(project.file("src/$name/css")), tailwind.css.srcDirs)
            assertEquals("app.css", tailwind.entry.get())
            assertEquals("static/css/app.css", tailwind.outputPath.get())
        }
    }

    @Test
    fun `css folders are part of each source set's sources`() {
        for (name in listOf("main", "test")) {
            val allSource = sourceSets.getByName(name).allSource.srcDirs
            assertTrue(project.file("src/$name/css") in allSource, "$name allSource: $allSource")
        }
    }

    @Test
    fun `changed css folders are what allSource reports`() {
        tailwindOf("main").css.setSrcDirs(listOf("styles"))
        val allSource = sourceSets.getByName("main").allSource.srcDirs
        assertTrue(project.file("styles") in allSource)
        assertFalse(project.file("src/main/css") in allSource)
    }

    @Test
    fun `output path follows the entry`() {
        tailwindOf("main").entry.set("site.css")
        assertEquals("static/css/site.css", tailwindOf("main").outputPath.get())
    }

    @Test
    fun `source sets added later are covered too`() {
        sourceSets.create("docs")
        assertTrue("compileDocsTailwind" in project.tasks.names)
        assertEquals(setOf(project.file("src/docs/css")), tailwindOf("docs").css.srcDirs)
    }

    @Test
    fun `each source set's css goes into its own resources`() {
        for ((name, task) in listOf("main" to "compileTailwind", "test" to "compileTestTailwind")) {
            assertTrue(task in project.tasks.names)
            val output = project.layout.buildDirectory.dir("generated/tailwind/$name").get().asFile
            assertTrue(output in sourceSets.getByName(name).resources.srcDirs, "$name resources include $output")
        }
    }

    @Test
    fun `shared defaults`() {
        val tailwind = project.extensions.getByType<TailwindExtension>()
        assertTrue(tailwind.minify.get())
        assertFalse(tailwind.localBinary.isPresent)
    }

    @Test
    fun `declares and resolves through separate configurations`() {
        for (name in listOf("tailwindCli", "tailwindChecksums")) {
            val scope = project.configurations.getByName(name)
            assertFalse(scope.isCanBeResolved || scope.isCanBeConsumed, "$name is only for declaring")

            val files = project.configurations.getByName("${name}Files")
            assertTrue(files.isCanBeResolved, "${name}Files resolves")
            assertFalse(files.isCanBeConsumed, "${name}Files isn't published")
            assertFalse(files.isTransitive)
            assertEquals(setOf(scope), files.extendsFrom)
        }
    }

    @Test
    fun `only the tailwind repository serves tailwind`() {
        val repository = project.repositories.getByName("Tailwind CSS releases")
        assertEquals(1, project.repositories.count { it === repository })
    }

    @Test
    fun `registers tasks without creating them`() {
        var created = false
        project.tasks.configureEach { if (name.contains("ailwind")) created = true }
        assertFalse(created, "tasks were created at configuration time")
    }
}
