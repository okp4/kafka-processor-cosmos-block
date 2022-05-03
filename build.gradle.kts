import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.21"
    application

    id("maven-publish")

    id("org.jlleitschuh.gradle.ktlint") version "10.2.1"
    id("io.gitlab.arturbosch.detekt") version "1.20.0"
}

ktlint {
    version.set("0.45.2")
}

group = "com.okp4"
description = "A Kafka Streams Processor to unwrap CØSMOS blocks into CØSMOS transactions"

application {
    mainClass.set("com.okp4.processor.cosmos.MainKt")
}

fun prepareVersion(): String {
    val digits = (project.property("project.version") as String).split(".")
    if (digits.size != 3) {
        throw GradleException("Wrong 'project.version' specified in properties, expects format 'x.y.z'")
    }

    return digits.map { it.toInt() }
        .let {
            it.takeIf { it[2] == 0 }?.subList(0, 2) ?: it
        }.let {
            it.takeIf { !project.hasProperty("release") }?.mapIndexed { i, d ->
                if (i == 1) d + 1 else d
            } ?: it
        }.joinToString(".") + project.hasProperty("release").let { if (it) "" else "-SNAPSHOT" }
}

afterEvaluate {
    project.version = prepareVersion()
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/okp4/okp4-cosmos-proto")
        credentials {
            username = project.property("maven.credentials.username") as String
            password = project.property("maven.credentials.password") as String
        }
    }
}

dependencies {
    val kafkaStreamVersion = "3.1.0"
    api("org.apache.kafka:kafka-streams:$kafkaStreamVersion")

    val slf4jVersion = "1.7.36"
    api("org.slf4j:slf4j-api:$slf4jVersion")
    api("org.slf4j:slf4j-log4j12:$slf4jVersion")

    val micrometerVersion = "1.8.5"
    api("io.micrometer:micrometer-core:$micrometerVersion")
    api("io.micrometer:micrometer-registry-prometheus:$micrometerVersion")

    val cosmosSdkVersion = "1.1"
    implementation("com.okp4.grpc:cosmos-sdk:$cosmosSdkVersion")

    val grpcVersion = "1.45.1"
    api("io.grpc:grpc-protobuf:$grpcVersion")

    testImplementation(kotlin("test"))

    val kotestVersion = "5.3.0"
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")
    testImplementation("io.kotest:kotest-framework-datatest:$kotestVersion")

    testImplementation("org.apache.kafka:kafka-streams-test-utils:$kafkaStreamVersion")
}

tasks {
    val fatJar = register<Jar>("fatJar") {
        dependsOn.addAll(listOf("compileJava", "compileKotlin", "processResources"))
        archiveClassifier.set("standalone")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest { attributes(mapOf("Main-Class" to application.mainClass)) }
        val sourcesMain = sourceSets.main.get()
        val contents = configurations.runtimeClasspath.get()
            .map { if (it.isDirectory) it else zipTree(it) } +
            sourcesMain.output
        from(contents)
    }
    build {
        dependsOn(fatJar)
    }
}

tasks.register("lint") {
    dependsOn.addAll(listOf("ktlintCheck", "detekt"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    testLogging {
        events("PASSED", "SKIPPED", "FAILED")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.apply {
        jvmTarget = "11"
        allWarningsAsErrors = true
    }
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    kotlinOptions.apply {
        jvmTarget = "11"
        allWarningsAsErrors = false
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifact(tasks["fatJar"])
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/okp4/${project.name}")
            credentials {
                username = project.property("maven.credentials.username") as String
                password = project.property("maven.credentials.password") as String
            }
        }
    }
}
