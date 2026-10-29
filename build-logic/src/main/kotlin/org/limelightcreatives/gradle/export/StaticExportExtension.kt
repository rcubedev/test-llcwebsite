package org.limelightcreatives.gradle.export

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * The `staticExport { }` block. Everything has a default, so with Spring Boot nothing needs setting.
 *
 * ```
 * staticExport {
 *     outputDirectory = layout.buildDirectory.dir("public")
 * }
 * ```
 */
abstract class StaticExportExtension @Inject constructor(layout: ProjectLayout) {

    /** The app's main class. Defaults to the one Spring Boot found for `bootRun`. */
    abstract val mainClass: Property<String>

    /** Spring profiles to run with. Defaults to `export`, which should make the app export and exit. */
    abstract val profiles: ListProperty<String>

    /** Where the site goes, passed to the app as `--export.out`. Defaults to `build/site`. */
    abstract val outputDirectory: DirectoryProperty

    init {
        profiles.convention(listOf("export"))
        outputDirectory.convention(layout.buildDirectory.dir("site"))
    }

    companion object {
        const val NAME = "staticExport"
    }
}
