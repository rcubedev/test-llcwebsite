package org.limelightcreatives.gradle.tailwind.repository

import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.limelightcreatives.gradle.tailwind.platform.TailwindPlatform
import java.net.URI

/**
 * Tailwind's GitHub releases seen as an ivy repository
 *
 * `com.tailwindcss:tailwindcss-linux-x64:[VERSION]` resolves to
 * `https://github.com/tailwindlabs/tailwindcss/releases/download/v[VERSION]/tailwindcss-linux-x64`,
 * so the CLI is downloaded and cached like any other dependency.
 *
 * Each release also publishes `sha256sums.txt` which is fetched the same way for verifying the download.
 */
object TailwindReleases {

    const val GROUP = "com.tailwindcss"
    internal const val URL = "https://github.com/tailwindlabs/tailwindcss/releases/download"

    /**
     * Dependency notation for one platform's CLI
     */
    fun cli(platform: TailwindPlatform, version: String): String = "$GROUP:${platform.assetName}:$version"

    /**
     * Dependency notation for the release's checksum list.
     */
    fun checksums(version: String): String = "$GROUP:sha256sums.txt:$version"
}

/**
 * Adds Tailwind's GitHub releases as an ivy repository. Exclusive both ways:
 * [TailwindReleases.GROUP] is only looked up here, and this repository is never asked for anything else.
 */
internal fun RepositoryHandler.tailwindReleases() {
    val repositories = this
    exclusiveContent {
        forRepository {
            repositories.ivy {
                name = "Tailwind CSS releases"
                url = URI.create(TailwindReleases.URL)
                patternLayout { artifact("v[revision]/[module]") }
                metadataSources { artifact() } // no ivy.xml; the binary is the whole module
            }
        }
        filter { includeGroup(TailwindReleases.GROUP) }
    }
}
