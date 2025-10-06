import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

group = "com.github.navikt"
version = "0.0.1-SNAPSHOT"

val bidragBeregnFellesVersion = "2025.09.22.105317"
val bidragFellesVersion = "2025.09.24.092109"
val kotlinLoggingJvmVersion = "7.0.13"
val springDocWebmvcVersion = "2.8.13"
val springmockkVersion = "4.0.2"
val tokenSupportVersion = "5.0.37"
val jacksonVersion = "2.20.0"
val junitJupiterVersion = "6.0.0"
val coroutinesVersion = "1.10.2"
val pdfBoxVersion = "2.0.31"
val micrometerPrometheusVersion = "1.15.4"
val logbackVersion = "1.5.18"
val logstashEncoderVersion = "8.1"


plugins {
    id("org.jetbrains.kotlin.plugin.spring") version "2.2.20"
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.2.20"
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
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-graphql")
    implementation("org.jetbrains.kotlin:kotlin-reflect")


    //Springdoc
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocWebmvcVersion")

    //Nav
    implementation("no.nav.bidrag:bidrag-beregn-barnebidrag:${bidragBeregnFellesVersion}") {
        exclude(group = "com.google.errorprone", module = "error_prone_annotations")
        exclude(group = "io.github.oshai", module = "kotlin-logging-jvm")
    }
    implementation("no.nav.bidrag:bidrag-transport-felles:${bidragFellesVersion}"){
        exclude(group = "jakarta.persistence", module = "jakarta.persistence-api")
    }
    implementation("no.nav.bidrag:bidrag-commons-felles:${bidragFellesVersion}"){
        exclude(group = "io.github.oshai", module = "kotlin-logging-jvm")
        exclude(group = "jakarta.persistence", module = "jakarta.persistence-api")

    }

    // logging
    implementation("ch.qos.logback:logback-core:$logbackVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")

    implementation("no.nav.security:token-validation-spring:$tokenSupportVersion")
    implementation("no.nav.bidrag:bidrag-inntekt:${bidragBeregnFellesVersion}") {
        exclude(group = "com.google.errorprone", module = "error_prone_annotations")
        exclude(group = "io.github.oshai", module = "kotlin-logging-jvm")
    }

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${coroutinesVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${coroutinesVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:${coroutinesVersion}")

    // Caching
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine")

    // Monitoring
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus:${micrometerPrometheusVersion}")

    // Annet
    implementation("io.github.oshai:kotlin-logging-jvm:$kotlinLoggingJvmVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    // PDF
    implementation("org.apache.pdfbox:pdfbox:${pdfBoxVersion}") {
        exclude(group = "commons-logging", module = "commons-logging")
    }

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
