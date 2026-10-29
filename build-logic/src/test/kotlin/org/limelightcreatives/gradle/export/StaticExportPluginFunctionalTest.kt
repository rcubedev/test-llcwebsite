package org.limelightcreatives.gradle.export

import org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.limelightcreatives.gradle.testing.GradleProject
import org.limelightcreatives.gradle.testing.outcome
import org.limelightcreatives.gradle.testing.reusedConfigurationCache
import java.io.File

/**
 * Real builds with a stand-in app: a plain main class that "exports" a page listing the
 * arguments and system properties it was started with. It fails when asked to, to check
 * that a failed export fails the build.
 */
class StaticExportPluginFunctionalTest {

    @TempDir
    lateinit var dir: File

    private val project by lazy { GradleProject(dir).also(::setUp) }

    private val page = "build/site/index.html"

    private fun setUp(project: GradleProject) = with(project) {
        file("build.gradle.kts", """
            plugins { id("org.limelightcreatives.static-export") }
            staticExport { mainClass = "demo.App" }
        """)
        file("src/main/java/demo/App.java", """
            package demo;

            import java.nio.file.*;

            public class App {
                public static void main(String[] args) throws Exception {
                    String out = null;
                    for (String a : args) if (a.startsWith("--export.out=")) out = a.substring(13);
                    if (Files.exists(Path.of("fail"))) {
                        System.err.println("export failed on purpose");
                        System.exit(3);
                    }
                    Path dir = Files.createDirectories(Path.of(out));
                    Files.writeString(dir.resolve("index.html"), String.join("\n", args)
                            + "\ndevtools=" + System.getProperty("spring.devtools.restart.enabled")
                            + "\n" + Files.readString(Path.of("src/main/resources/content.txt")));
                }
            }
        """)
        file("src/main/resources/content.txt", "version one")
    }

    @Test
    fun `runs the app with the export options`() {
        assertEquals(SUCCESS, project.build("exportSite").outcome(":exportSite"))
        val text = project.read(page)
        assertTrue("--spring.profiles.active=export" in text)
        assertTrue("--spring.main.web-application-type=none" in text)
        assertTrue("--spring.main.banner-mode=off" in text)
        assertTrue("--export.out=${dir.resolve("build/site").canonicalPath}" in text ||
            "--export.out=${dir.resolve("build/site").path}" in text, text)
        assertTrue("devtools=false" in text)
    }

    @Test
    fun `is up to date on the next run, with the configuration cache reused`() {
        project.build("exportSite")
        val again = project.build("exportSite")
        assertTrue(again.reusedConfigurationCache)
        assertEquals(UP_TO_DATE, again.outcome(":exportSite"))
    }

    @Test
    fun `a changed resource exports again`() {
        project.build("exportSite")
        project.file("src/main/resources/content.txt", "version two")
        assertEquals(SUCCESS, project.build("exportSite").outcome(":exportSite"))
        assertTrue("version two" in project.read(page))
    }

    @Test
    fun `a deleted output is restored from the build cache`() {
        project.build("exportSite")
        dir.resolve("build/site").deleteRecursively()
        assertEquals(FROM_CACHE, project.build("exportSite").outcome(":exportSite"))
        assertTrue(project.exists(page))
    }

    @Test
    fun `changing the profiles or output folder reruns it`() {
        project.build("exportSite")
        project.dir.resolve("build.gradle.kts").appendText("""
            staticExport {
                profiles = listOf("export", "preview")
                outputDirectory = layout.buildDirectory.dir("public")
            }
        """.trimIndent())
        assertEquals(SUCCESS, project.build("exportSite").outcome(":exportSite"))
        assertTrue("--spring.profiles.active=export,preview" in project.read("build/public/index.html"))
    }

    @Test
    fun `a failed export fails the build`() {
        project.file("fail", "")
        val result = project.buildAndFail("exportSite")
        assertTrue("export failed on purpose" in result.output)
        assertTrue("finished with non-zero exit value 3" in result.output, result.output)
    }
}
