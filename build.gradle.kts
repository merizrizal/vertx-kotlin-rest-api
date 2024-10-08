buildscript {
    System.setProperty("kotlinVersion", "2.0.20")
    System.setProperty("vertxVersion", "4.5.10")
}

group = "com.merizrizal"
version = "0.0.1"

val vertxVersion: String by System.getProperties()

plugins {
    val kotlinVersion: String by System.getProperties()

    kotlin("jvm").version(kotlinVersion)
    id("java")
    id("java-library")
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

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Implementation-Title"] = rootProject.name
        attributes["Implementation-Version"] = archiveVersion
        attributes["Main-Class"] = "AppKt"
    }

    val sourcesMain = sourceSets.main.get()
    val contents = configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) } + sourcesMain.output
    from(contents)
}

tasks.register("cleanAndJar") {
    group = "build"
    description = "Clean and create jar"

    dependsOn(tasks.clean)
    dependsOn(tasks.jar)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
}