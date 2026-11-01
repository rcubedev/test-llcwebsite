plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(21)
}

gradlePlugin {
    plugins {
        register("tailwind") {
            id = "org.limelightcreatives.tailwind"
            implementationClass = "org.limelightcreatives.gradle.tailwind.TailwindPlugin"
            displayName = "Tailwind CSS"
            description = "Builds Tailwind CSS with the standalone CLI and integrates with IntelliJ IDEA"
        }
        register("staticExport") {
            id = "org.limelightcreatives.static-export"
            implementationClass = "org.limelightcreatives.gradle.export.StaticExportPlugin"
            displayName = "Static export for Spring Boot sites"
            description = "Adds exportSite, which runs the app once to render it to static files."
        }
    }
}
