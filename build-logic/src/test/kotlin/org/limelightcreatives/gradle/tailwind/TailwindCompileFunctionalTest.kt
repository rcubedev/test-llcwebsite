package org.limelightcreatives.gradle.tailwind

import org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import org.gradle.testkit.runner.TaskOutcome.NO_SOURCE
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import org.limelightcreatives.gradle.testing.GradleProject
import org.limelightcreatives.gradle.testing.outcome
import org.limelightcreatives.gradle.testing.reusedConfigurationCache
import java.io.File

/**
 * Real builds with a stand-in Tailwind CLI (set through `tailwind.bin`), so nothing is downloaded.
 * The stand-in writes its arguments and the entry stylesheet to the output, so tests can check
 * what it was given.
 */
@DisabledOnOs(OS.WINDOWS, disabledReason = "The stand-in CLI is a shell script")
class TailwindCompileFunctionalTest {

    @TempDir
    lateinit var dir: File

    private val project by lazy { GradleProject(dir).also(::setUp) }

    private val mainCss = "build/generated/tailwind/main/static/css/app.css"

    private fun setUp(project: GradleProject) = with(project) {
        file("build.gradle.kts", """
            plugins { id("org.limelightcreatives.tailwind") }
            tailwind { version = "4.3.3" }
            sourceSets.main {
                tailwind { content.from("src/main/resources/templates") }
            }
        """)
        file("src/main/css/app.css", """
            @import "tailwindcss";
            @import "./components/button.css";
        """)
        file("src/main/css/components/button.css", ".button { color: red; }")
        file("src/main/resources/templates/index.html", """<p class="flex"></p>""")
        file("gradle.properties", "tailwind.bin=fake-tailwind")
        file("fake-tailwind", """
            #!/bin/sh
            in=""; out=""; prev=""
            for a in "${'$'}@"; do
              [ "${'$'}prev" = "--input" ] && in="${'$'}a"
              [ "${'$'}prev" = "--output" ] && out="${'$'}a"
              prev="${'$'}a"
            done
            mkdir -p "${'$'}(dirname "${'$'}out")"
            { echo "args: ${'$'}*"; cat "${'$'}in"; } > "${'$'}out"
        """).setExecutable(true)
    }

    @Test
    fun `compiles the entry into the main resources`() {
        val result = project.build("processResources")

        assertEquals(SUCCESS, result.outcome(":compileTailwind"))
        assertNull(result.task(":tailwindInstall"), "a local binary must not trigger the download")
        val css = project.read("build/resources/main/static/css/app.css")
        assertTrue("--input ${dir.resolve("src/main/css/app.css").canonicalFile}" in css ||
            "--input ${dir.resolve("src/main/css/app.css")}" in css, css)
        assertTrue("--minify" in css)
        assertTrue("@import \"tailwindcss\";" in css, "the entry was the input")
    }

    @Test
    fun `is up to date on the next run, with the configuration cache reused`() {
        project.build("compileTailwind")
        val again = project.build("compileTailwind")
        assertTrue(again.reusedConfigurationCache)
        assertEquals(UP_TO_DATE, again.outcome(":compileTailwind"))
    }

    @Test
    fun `editing an imported stylesheet reruns it`() {
        project.build("compileTailwind")
        project.file("src/main/css/components/button.css", ".button { color: blue; }")
        assertEquals(SUCCESS, project.build("compileTailwind").outcome(":compileTailwind"))
    }

    @Test
    fun `editing a scanned file reruns it`() {
        project.build("compileTailwind")
        project.file("src/main/resources/templates/index.html", """<p class="grid"></p>""")
        assertEquals(SUCCESS, project.build("compileTailwind").outcome(":compileTailwind"))
    }

    @Test
    fun `files that aren't css in the css folder are ignored`() {
        project.build("compileTailwind")
        project.file("src/main/css/NOTES.md", "not a stylesheet")
        assertEquals(UP_TO_DATE, project.build("compileTailwind").outcome(":compileTailwind"))
    }

    @Test
    fun `a source set without stylesheets is skipped`() {
        val result = project.build("compileTestTailwind")
        assertEquals(NO_SOURCE, result.outcome(":compileTestTailwind"))
    }

    @Test
    fun `the test source set has its own css`() {
        project.file("src/test/css/app.css", "/* test styles */")
        project.build("processResources", "processTestResources")
        assertTrue("/* test styles */" in project.read("build/resources/test/static/css/app.css"))
        assertFalse("/* test styles */" in project.read("build/resources/main/static/css/app.css"))
    }

    @Test
    fun `custom folders and a nested entry`() {
        project.file("styles/site/main.css", "/* nested */")
        project.dir.resolve("build.gradle.kts").appendText("""
            sourceSets.main {
                tailwind {
                    css.setSrcDirs(listOf("styles"))
                    entry = "site/main.css"
                }
            }
        """.trimIndent())

        project.build("compileTailwind")
        assertTrue("/* nested */" in project.read("build/generated/tailwind/main/static/css/site/main.css"))
    }

    @Test
    fun `a missing entry fails clearly`() {
        project.dir.resolve("build.gradle.kts").appendText("\nsourceSets.main { tailwind { entry = \"missing.css\" } }\n")
        val result = project.buildAndFail("compileTailwind")
        assertTrue("No entry stylesheet 'missing.css'" in result.output, result.output)
    }

    @Test
    fun `changing the output path leaves no stale css`() {
        project.build("compileTailwind")
        project.dir.resolve("build.gradle.kts").appendText("\nsourceSets.main { tailwind { outputPath = \"static/site.css\" } }\n")
        project.build("compileTailwind")
        assertTrue(project.exists("build/generated/tailwind/main/static/site.css"))
        assertFalse(project.exists(mainCss), "old output removed")
    }

    @Test
    fun `restored from the build cache, even from another folder`(@TempDir other: File, @TempDir cache: File) {
        val first = GradleProject(other.resolve("a"), cache).also(::setUp)
        assertEquals(SUCCESS, first.build("compileTailwind").outcome(":compileTailwind"))

        val second = GradleProject(other.resolve("b"), cache).also(::setUp)
        assertEquals(FROM_CACHE, second.build("compileTailwind").outcome(":compileTailwind"))
        assertTrue(second.exists(mainCss))
    }

    @Test
    fun `without a local binary, the installed cli is used`() {
        project.dir.resolve("gradle.properties").delete()
        // --dry-run plans the build without running it, so nothing is downloaded.
        val plan = project.build("compileTailwind", "--dry-run").output.lines()
        val install = plan.indexOf(":tailwindInstall SKIPPED")
        assertTrue(install >= 0, "tailwindInstall is planned")
        assertTrue(install < plan.indexOf(":compileTailwind SKIPPED"), "and runs before compileTailwind")
    }
}
