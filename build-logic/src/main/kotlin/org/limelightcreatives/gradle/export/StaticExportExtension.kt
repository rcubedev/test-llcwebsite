package org.limelightcreatives.gradle.export

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Configures the [exportSite][StaticExportPlugin] task.
 */
abstract class StaticExportExtension @Inject constructor(layout: ProjectLayout) {

    abstract val mainClass: Property<String>

    abstract val profiles: ListProperty<String>

    abstract val outputDirectory: DirectoryProperty

    init {
        profiles.convention(listOf("export"))
        outputDirectory.convention(layout.buildDirectory.dir("site"))
    }

    companion object {
        const val NAME = "staticExport"
    }
}