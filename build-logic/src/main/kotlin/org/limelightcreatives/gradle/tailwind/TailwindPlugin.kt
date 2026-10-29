package org.limelightcreatives.gradle.tailwind

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvableConfiguration
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.register
import org.limelightcreatives.gradle.tailwind.platform.TailwindPlatform
import org.limelightcreatives.gradle.tailwind.repository.TailwindReleases
import org.limelightcreatives.gradle.tailwind.repository.tailwindReleases
import org.limelightcreatives.gradle.tailwind.tasks.TailwindCompile
import org.limelightcreatives.gradle.tailwind.tasks.TailwindInstall
import javax.inject.Inject

/**
 * Builds Tailwind CSS with the standalone CLI, so no Node or npm is involved.
 *
 * - The CLI for this machine is a normal dependency (see [TailwindReleases]), so Gradle
 *   downloads it once per version into its shared cache.
 * - `tailwindInstall` verifies it against the release's own `sha256sums.txt` and makes it executable.
 * - Every source set gets a `tailwind` block ([TailwindSourceSet]) with a `css` folder
 *   (`src/<name>/css`) and a compile task (`compileTailwind`, `compileTestTailwind`, ...) whose
 *   output is one of that source set's resource folders: jar, bootRun and tests get it with no extra wiring.
 *
 * Everything is lazy: nothing is downloaded, resolved or even looked up (OS, CPU, version)
 * until a task that needs it runs.
 */
abstract class TailwindPlugin @Inject constructor(
    private val providers: ProviderFactory,
) : Plugin<Project> {

    override fun apply(project: Project) {
        project.apply<JavaPlugin>()
        val extension = project.extensions.create<TailwindExtension>(TailwindExtension.NAME)
        val platform = providers.systemProperty("os.name")
            .zip(providers.systemProperty("os.arch"), TailwindPlatform::of)

        project.repositories.tailwindReleases()
        val cli = releaseFile(project, CLI, "The standalone Tailwind CLI for this machine.",
            extension.version.zip(platform) { version, it -> TailwindReleases.cli(it, version) })
        val checksums = releaseFile(project, CHECKSUMS, "The Tailwind release's sha256sums.txt.",
            extension.version.map(TailwindReleases::checksums))

        val install = registerInstall(project, extension, platform, cli, checksums)
        val binary = extension.localBinary.orElse(install.flatMap { it.executable })
        // orElse drops the dependency on tailwindInstall that flatMap carries, so add it back,
        // still lazily: tailwindInstall only runs when the installed binary is the one used.
        val binaryDependency = extension.localBinary.map { emptyList<Any>() }.orElse(listOf(install))

        val layout = project.layout
        project.extensions.getByType<SourceSetContainer>().configureEach {
            val sourceSet = this
            // Created by the project's ObjectFactory: a source set's own extensions can't inject project services.
            val settings = project.objects.newInstance<TailwindSourceSet>(sourceSet.name)
            extensions.add(TailwindSourceSet::class.java, TailwindSourceSet.NAME, settings)
            // Part of the source set's sources, the way the groovy and scala plugins register theirs,
            // so IDEs and anything else reading allSource (e.g. a sources jar) see the stylesheets.
            allSource.source(settings.css)
            val compile = project.tasks.register<TailwindCompile>(getTaskName("compile", "tailwind")) {
                group = TASK_GROUP
                description = "Builds the Tailwind stylesheet for the ${sourceSet.name} source set."
                this.binary.set(binary)
                dependsOn(binaryDependency)
                stylesheets.from(settings.css)
                entry.set(settings.entry)
                content.from(settings.content)
                minify.set(extension.minify)
                outputDirectory.set(layout.buildDirectory.dir("generated/tailwind/${sourceSet.name}"))
                outputPath.set(settings.outputPath)
            }
            resources.srcDir(compile.flatMap { it.outputDirectory })
        }
    }

    /**
     * One file from the Tailwind releases repository: a dependency scope to declare it in
     * (named [name]) and a resolvable configuration to get the file from (`<name>Files`).
     */
    private fun releaseFile(
        project: Project,
        name: String,
        description: String,
        notation: Provider<String>,
    ): NamedDomainObjectProvider<ResolvableConfiguration> {
        val scope = project.configurations.dependencyScope(name) {
            this.description = description
        }
        project.dependencies.addProvider(scope.name, notation)
        return project.configurations.resolvable("${name}Files") {
            this.description = "$description Resolved."
            extendsFrom(scope.get())
            isTransitive = false
        }
    }

    private fun registerInstall(
        project: Project,
        extension: TailwindExtension,
        platform: Provider<TailwindPlatform>,
        cli: NamedDomainObjectProvider<ResolvableConfiguration>,
        checksums: NamedDomainObjectProvider<ResolvableConfiguration>,
    ): TaskProvider<TailwindInstall> {
        val layout = project.layout
        return project.tasks.register<TailwindInstall>("tailwindInstall") {
            group = TASK_GROUP
            description = "Verifies the downloaded Tailwind CLI and makes it executable."
            downloaded.from(cli)
            this.checksums.from(checksums)
            assetName.set(platform.map(TailwindPlatform::assetName))
            executable.set(layout.buildDirectory.file(
                extension.version.zip(platform) { version, it -> "tailwind/$version/${it.assetName}" }))
        }
    }

    private companion object {
        const val TASK_GROUP = "build"
        const val CLI = "tailwindCli"
        const val CHECKSUMS = "tailwindChecksums"
    }
}
