package org.limelightcreatives.gradle.tailwind

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Tailwind settings for one source set, added to every source set as `tailwind`:
 *
 * ```
 * sourceSets.main {
 *     tailwind {
 *         // css.srcDir("src/main/css") and entry = "app.css" are the defaults
 *         content.from("src/main/resources/templates")
 *     }
 * }
 * ```
 *
 * A source set with no stylesheets is skipped (`NO-SOURCE`), like one with no Java.
 */
abstract class TailwindSourceSet @Inject constructor(sourceSetName: String, objects: ObjectFactory) {

    /** Stylesheets: the [entry] and anything it imports. Defaults to `src/<source set>/css`. */
    val css: SourceDirectorySet = objects.sourceDirectorySet("${sourceSetName}Css", "$sourceSetName CSS").apply {
        srcDir("src/$sourceSetName/css")
        include("**/*.css")
    }

    /** The stylesheet with `@import "tailwindcss"`, relative to a [css] folder. Defaults to `app.css`. */
    abstract val entry: Property<String>

    /** Files Tailwind scans for class names. Keep in step with the `@source` lines in [entry]. */
    abstract val content: ConfigurableFileCollection

    /** Where the CSS goes in the source set's resources. Defaults to `static/css/<entry>`, served at `/css/<entry>`. */
    abstract val outputPath: Property<String>

    init {
        entry.convention("app.css")
        outputPath.convention(entry.map { "static/css/$it" })
    }

    companion object {
        const val NAME = "tailwind"
    }
}
