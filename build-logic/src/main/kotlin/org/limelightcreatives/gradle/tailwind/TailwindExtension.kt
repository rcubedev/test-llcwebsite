package org.limelightcreatives.gradle.tailwind

import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import javax.inject.Inject

/**
 * The `tailwind { }` block: settings shared by every source set. Everything has a default.
 * What to compile is set per source set; see [TailwindSourceSet].
 */
abstract class TailwindExtension @Inject constructor(layout: ProjectLayout, providers: ProviderFactory) {

    /** Tailwind release, e.g. `4.3.3`. Defaults to the `tailwind.version` Gradle property. */
    abstract val version: Property<String>

    /** Defaults to true. */
    abstract val minify: Property<Boolean>

    /**
     * A Tailwind binary to use instead of downloading one, e.g. offline.
     * Defaults to the `tailwind.bin` Gradle property.
     */
    abstract val localBinary: RegularFileProperty

    init {
        version.convention(providers.gradleProperty("tailwind.version"))
        minify.convention(true)
        localBinary.convention(layout.projectDirectory.file(providers.gradleProperty("tailwind.bin")))
    }

    companion object {
        const val NAME = "tailwind"
    }
}
