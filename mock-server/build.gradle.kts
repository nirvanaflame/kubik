import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// ------------------------------------------------------------------
// Module :mock-server — a tiny standalone mock HTTP server.
//
// Exposes an httpbin.org-style JSON endpoint plus other dummy
// responses, and attaches a set of extra response headers on every
// request so it behaves like a lightweight API stub for local dev /
// integration testing.
// ------------------------------------------------------------------

plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

// Tell the Spring Boot Gradle plugin which class to run.
// (spring-playground module auto-detects its single main class.)
springBoot {
    mainClass = "com.nf.mockserver.MockServerApplicationKt"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xannotation-default-target=param-property"
        )
    }
}

dependencies {
    // --- Web --- (no DB / tracing / redis for this stub)
    implementation("org.springframework.boot:spring-boot-starter-webmvc")

    // --- Kotlin helpers --
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")

    // --- Dev loop --
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // --- Tests ---
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
