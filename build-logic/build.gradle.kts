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
            displayName = "Tailwind CSS (standalone CLI)"
            description = "Builds Tailwind CSS with the standalone CLI, fetched through an Ivy repository and checksum-verified."
        }
        register("staticExport") {
            id = "org.limelightcreatives.static-export"
            implementationClass = "org.limelightcreatives.gradle.export.StaticExportPlugin"
            displayName = "Static export for Spring Boot sites"
            description = "Adds exportSite, which runs the app once to render it to static files."
        }
    }
}

dependencies {
    // The Gradle API is compileOnly for the plugin itself (Gradle provides it at runtime), but ProjectBuilder tests need it.
    testImplementation(gradleApi())
    testImplementation(gradleKotlinDsl())
    // To test the static-export plugin against the real Spring Boot plugin. Keep in step with the root build.
    testImplementation("org.springframework.boot:spring-boot-gradle-plugin:4.1.2")
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
