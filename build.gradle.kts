import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    application

    id("org.jlleitschuh.gradle.ktlint") version "10.2.1"
    id("org.jmailen.kotlinter") version "3.9.0"
}

group = "com.okp4"
version = "1.0-SNAPSHOT"
description = "A Kafka Streams Processor to unwrap CØSMOS blocks into CØSMOS transactions"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    val kotestVersion = "5.2.1"
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

application {
    mainClass.set("MainKt")
}
