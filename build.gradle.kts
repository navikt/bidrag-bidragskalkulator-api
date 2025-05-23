import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

group = "com.github.navikt"
version = "0.0.1-SNAPSHOT"

val bidragBeregnFellesVersion = "2025.04.16.131242"
val bidragFellesVersion = "2025.04.22.161649"
val kotlinLoggingJvmVersion = "7.0.3"
val springDocWebmvcVersion = "2.8.5"
val mockkVersion = "4.0.2"
val tokenSupportVersion = "5.0.19"
val jacksonVersion = "2.18.2"
val junitJupiterVersion = "5.9.3"
val mockitoKotlinVersion = "5.2.0"
val bidragInntektVersion = "2025.04.16.131242"
val coroutinesVersion = "1.10.2"

plugins {
    id("org.jetbrains.kotlin.plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.4.3"
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
    api("no.nav.bidrag:bidrag-inntekt:${bidragInntektVersion}") {
        exclude(group = "com.google.errorprone", module = "error_prone_annotations")
        exclude(group = "io.github.oshai", module = "kotlin-logging-jvm")
    }

    // Kotlin Coroutines
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${coroutinesVersion}")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${coroutinesVersion}")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:${coroutinesVersion}")

    // Annet
    api("io.github.oshai:kotlin-logging-jvm:$kotlinLoggingJvmVersion")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
    testImplementation("org.mockito.kotlin:mockito-kotlin:$mockitoKotlinVersion")
    testImplementation("com.ninja-squad:springmockk:$mockkVersion")
    testImplementation("no.nav.security:token-validation-spring-test:$tokenSupportVersion")
    testImplementation("org.springframework.security:spring-security-test")
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
