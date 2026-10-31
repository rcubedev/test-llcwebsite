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
    }
}
