import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    java
    id("org.springframework.boot") version "4.1.2"
    id("org.limelightcreatives.tailwind")
    id("org.limelightcreatives.static-export")
}

group = "org.limelightcreatives"
version = "0.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

val mockitoAgent = configurations.dependencyScope("mockitoAgent")
val mockitoAgentClasspath = configurations.resolvable("mockitoAgentClasspath") {
    extendsFrom(mockitoAgent)
}

dependencies {
    // Spring Boot's list of tested library versions, so most dependencies need no version number.
    val springBoot = platform(SpringBootPlugin.BOM_COORDINATES)
    implementation(springBoot)
    developmentOnly(springBoot)
    "mockitoAgent"(springBoot)

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    implementation("org.commonmark:commonmark:0.31.0")
    implementation("org.commonmark:commonmark-ext-gfm-tables:0.31.0")
    implementation("org.yaml:snakeyaml")

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    "mockitoAgent"("org.mockito:mockito-core") { isTransitive = false }
}

tasks.test {
    useJUnitPlatform()

    jvmArgumentProviders.add(JavaAgent(files(mockitoAgentClasspath)))
    // Mockito then adds a class to the bootstrap classpath, which stops class-data sharing from covering
    // anything but JDK classes and makes the JVM warn about it. Turn sharing off: same speed, no warning.
    jvmArgs("-Xshare:off")
}

/** `-javaagent:<jar>` for a test or exec JVM, with the jar tracked as an input. */
class JavaAgent(@get:Classpath val jar: FileCollection) : CommandLineArgumentProvider {
    override fun asArguments() = listOf("-javaagent:${jar.singleFile.absolutePath}")
}

// ---------------------------------------------------------------------------
// Tailwind (plugin in build-logic/)
//
// Every source set gets a css folder (src/main/css here) and a compile task (compileTailwind)
// whose output is added to that source set's resources. The standalone CLI comes from
// Tailwind's GitHub releases through an Ivy repository and is checked against the release's
// own sha256sums.txt. Version and options are in tailwind { }, defaults in gradle.properties.
// ---------------------------------------------------------------------------

sourceSets.main {
    tailwind {
        // Everything Tailwind scans for class names. Keep in step with the @source lines in app.css.
        content.from("src/main/resources/templates", "src/main/resources/static/js", "src/main/java")
    }
}

// ---------------------------------------------------------------------------
// Static export (plugin in build-logic/)
//
// ./gradlew exportSite  ->  build/site/  (plain HTML/CSS/JS, ready for any static host)
// Runs the app once with the "export" profile and no web server; SiteExporter renders
// every page through Thymeleaf and copies the static assets. Defaults need no setup;
// staticExport { } can change the profiles or output folder.
// ---------------------------------------------------------------------------
