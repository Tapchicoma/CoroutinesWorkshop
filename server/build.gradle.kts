import ExternalProcessBuildService.Companion.registerExternalProcessBuildService

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kover)
    application
    `external-process-plugin`
}

group = "com.kcworkshop.coroutines"
version = "0.0.1"

application {
    mainClass.set("com.kotlinconf.workshop.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

kotlin.jvmToolchain(11)

dependencies {
    implementation(projects.shared)
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-call-logging-jvm")
    implementation("io.ktor:ktor-server-websockets")
    implementation("io.ktor:ktor-server-netty")
    implementation(libs.logback.classic)
    implementation(libs.kotlinx.datetime)
    testImplementation("io.ktor:ktor-server-tests")
    testImplementation(libs.kotlin.test.junit)
}

// Way to start a server in the background without blocking a Gradle daemon process
val unzippedDistribution = tasks.register<Copy>("unzipDistribution") {
    val distributionZip = tasks.named<Zip>("distZip").flatMap { it.archiveFile }
    destinationDir = project.layout.buildDirectory.dir("serverDist").get().asFile

    from(zipTree(distributionZip))
    into(destinationDir)
}

tasks.register<RunServerTask>("startServerInBackground") {
    description = "Runs http server required for demo applications"
    group = "Run"

    distributionDir.fileProvider(unzippedDistribution.map { it.destinationDir.resolve("server-$version") })
    runDir.set(layout.buildDirectory.dir("runBackgroundServer"))
    externalProcessBuildService.set(project.registerExternalProcessBuildService())
    javaLauncher.set(
        javaToolchains.launcherFor {
            languageVersion = JavaLanguageVersion.of(11)
        }
    )
}

tasks.register<DefaultTask>("stopServerInBackground") {
    description = "Stops running http server required for demo applications"
    group = "Run"

    val externalProcessBuildService = project.registerExternalProcessBuildService()
    val serverPidFile = layout.buildDirectory.dir("runBackgroundServer").map { it.file("server.pid") }
    outputs.upToDateWhen { false }

    doLast {
        externalProcessBuildService.get().stopServer(serverPidFile.get().asFile)
    }
}