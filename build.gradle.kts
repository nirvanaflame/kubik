// ------------------------------------------------------------------
// Gradle Build — Spring Boot 4.x Playground (Kotlin)
//
// Prerequisites : JDK 25+ to compile; runtime requires Java 17+ (SpringBoot 4 minimum).
// Repository note: For milestone-only artifacts, uncomment the
// `spring-milestone` block in repositories().
// ------------------------------------------------------------------

import org.jetbrains.kotlin.gradle.dsl.JvmTarget


plugins {
    kotlin("jvm")       version "2.3.21"  // @see https://kotlinlang.org/docs/whatsnew23.html (Kotlin 2.3)
    kotlin("plugin.spring") version "2.3.21"  // enables `allOpen` for marker annotations & open class support
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.nf"
version = "0.0.1-SNAPSHOT"
description = "spring-playground"

// -----------------------------------------------
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25) // Spring Boot 4 runtime target
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",                // null-safety via @Nullable/@NonNull
            "-Xannotation-default-target=param-property"
        )
    }
}

// -----------------------------------------------
// Repositories
// -----------------------------------------------
repositories {
    mavenCentral()
    // Uncomment for Spring Boot 4 milestone artifacts.
    // maven("https://repo.spring.io/milestone") {
    //     name = "spring-milestone"
    //     isAllowInsecureProtocol = false
    // }
}

// -----------------------------------------------
// Dependency Management — Spring Boot BOM pulls versions for all `io.spring` and `org.springframework.*` artifacts.
// The Kotlin/OTel/Jackson dependencies are pinned through their respective plugin DSLs or the micrometer-otel starter.
// -----------------------------------------------
dependencyManagement {
    imports {
        // Uncomment only if you want to *override* Boot's BOM with a fixed snapshot:
//         bom("org.springframework.boot:spring-boot-dependencies:4.1.0")
    }
}

dependencies {
    // --- Observability (metrics + tracing) --
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")

    // --- Kotlin helpers --
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")  // Spring Boot 4 ships Jackson from tools.jakarta by default

    // --- Dev loop --
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    // --- Exporters / registries --
    runtimeOnly("io.micrometer:micrometer-registry-otlp")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")  // pull-based Prometheus bridge

    // --- Annotation processing --
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
    testImplementation("org.springframework.boot:spring-boot-starter-opentelemetry-test")
    testImplementation("org.springframework.boot:spring-boot-starter-restclient-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
    // Useful during learn-Phase 4: capture test execution metrics.
    systemProperty("otel.metrics.exporter", "otlp")            // optional: export test traces to OTLP
    systemProperty("otel.traces.exporter",   "logging")       // keep noisy but useful output on console
}
