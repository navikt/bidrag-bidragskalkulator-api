import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

group = "com.github.navikt"
version = "0.0.1-SNAPSHOT"

val bidragBeregnFellesVersion = "2025.07.10.102838"
val bidragFellesVersion = "2025.07.23.101418"
val kotlinLoggingJvmVersion = "7.0.7"
val springDocWebmvcVersion = "2.8.8"
val springmockkVersion = "4.0.2"
val tokenSupportVersion = "5.0.33"
val jacksonVersion = "2.19.0"
val junitJupiterVersion = "5.13.4"
val coroutinesVersion = "1.10.2"
val pdfBoxVersion = "2.0.31"

plugins {
    id("org.jetbrains.kotlin.plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.5.4"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.1.10"
    id("java")
    id("maven-publish")
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

dependencies {
    //Spring
    api("org.springframework.boot:spring-boot-starter")
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-validation")
    api("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    api("org.springframework.boot:spring-boot-starter-oauth2-client")
    api("org.springframework.boot:spring-boot-starter-graphql")
    api("org.springframework.boot:spring-boot-starter-webflux")
    api("org.jetbrains.kotlin:kotlin-reflect")

    //Springdoc
    api("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocWebmvcVersion")

    //Nav
    api("no.nav.bidrag:bidrag-beregn-barnebidrag:${bidragBeregnFellesVersion}") {
        exclude(group = "com.google.errorprone", module = "error_prone_annotations")
        exclude(group = "io.github.oshai", module = "kotlin-logging-jvm")
    }
    api("no.nav.bidrag:bidrag-transport-felles:${bidragFellesVersion}")
    api("no.nav.bidrag:bidrag-commons-felles:${bidragFellesVersion}"){
        exclude(group = "io.github.oshai", module = "kotlin-logging-jvm")
    }
    api("no.nav.security:token-validation-spring:$tokenSupportVersion")
    api("no.nav.bidrag:bidrag-inntekt:${bidragBeregnFellesVersion}") {
        exclude(group = "com.google.errorprone", module = "error_prone_annotations")
        exclude(group = "io.github.oshai", module = "kotlin-logging-jvm")
    }

    // Kotlin Coroutines
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${coroutinesVersion}")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${coroutinesVersion}")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:${coroutinesVersion}")

    // Caching
    api("org.springframework.boot:spring-boot-starter-cache")
    api("com.github.ben-manes.caffeine:caffeine")

    // Annet
    api("io.github.oshai:kotlin-logging-jvm:$kotlinLoggingJvmVersion")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    // PDF
    implementation("org.apache.pdfbox:pdfbox:${pdfBoxVersion}")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
    testImplementation("com.ninja-squad:springmockk:$springmockkVersion")
    testImplementation("no.nav.security:token-validation-spring-test:$tokenSupportVersion")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
}

java {
    withSourcesJar()
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of("21"))
    }
}


tasks {
    withType<Test> {
        useJUnitPlatform()
        testLogging {
            showExceptions = true
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
            events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        }
        systemProperty("spring.profiles.active", "test")
    }
}
