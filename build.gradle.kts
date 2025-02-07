import org.gradle.jvm.tasks.Jar

buildscript {
    System.setProperty("kotlinVersion", "2.1.0")
    System.setProperty("vertxVersion", "4.5.12")
}

group = "com.merizrizal"
version = "0.0.1"

val useJava = 23
val vertxVersion: String by System.getProperties()
val appReleaseName = "${rootProject.name}-${version}"

plugins {
    val kotlinVersion: String by System.getProperties()

    kotlin("jvm").version(kotlinVersion)
    id("java")
    id("org.graalvm.buildtools.native").version("0.10.4")
}

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    // Vertx Core
    implementation("io.vertx:vertx-core:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin:$vertxVersion")

    // Vertx Web
    implementation("io.vertx:vertx-web:$vertxVersion")

    // Vertx Rxjava
    implementation("io.vertx:vertx-rx-java3:$vertxVersion")
    implementation("io.vertx:vertx-rx-java3-gen:$vertxVersion")
}

tasks.register<Jar>("kotlinJar") {
    group = "build"
    description = "Kotlin jar"

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Implementation-Title"] = rootProject.name
        attributes["Implementation-Version"] = archiveVersion
        attributes["Main-Class"] = "AppKt"
    }

    val sourcesMain = sourceSets.main.get()
    val contents = configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) } + sourcesMain.output
    from(contents)

    dependsOn(tasks.classes)
}

tasks.register<JavaExec>("runJar") {
    group = "build"
    description = "Run the jar file"

    classpath("${layout.buildDirectory.get().asFile.path}/libs/$appReleaseName.jar")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(useJava))
    }
}

kotlin {
    jvmToolchain(useJava)
}

graalvmNative {
    binaries {
        named("main") {
            javaLauncher.set(javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(useJava))
            })
            imageName.set("${appReleaseName}.run")
            mainClass.set("AppKt")
            fallback.set(false)
            verbose.set(true)
            sharedLibrary.set(false)

            val path = "${projectDir}/src/main/resources/META-INF/native-image"
            buildArgs.add("-H:ReflectionConfigurationFiles=${path}/reflect-config.json")
            buildArgs.add("-H:EnableURLProtocols=http,https")
            buildArgs.add("-H:+InstallExitHandlers")
            buildArgs.add("-H:+ReportUnsupportedElementsAtRuntime")
            buildArgs.add("-H:+ReportExceptionStackTraces")
            buildArgs.add("--enable-http")
            buildArgs.add("--enable-https")
            buildArgs.add("--allow-incomplete-classpath")
            buildArgs.add("--report-unsupported-elements-at-runtime")
        }
    }
}
