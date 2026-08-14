// ------------------------------------------------------------------
// Root build script — a multi-project (monorepo) Gradle build.
//
// Responsibilities of the ROOT project:
//   1. Declare plugin versions once (apply false) so every module
//      shares the exact same versions.
//   2. Define `group`/`version` for the whole repo.
//   3. Declare the repositories every module resolves from.
//
// Individual modules then opt-in by applying the plugins they need
// and declaring only their own dependencies (see spring-playground/
// and mock-server/build.gradle.kts).
// ------------------------------------------------------------------

plugins {
    kotlin("jvm")                  version "2.3.21" apply false
    kotlin("plugin.spring")        version "2.3.21" apply false
    id("org.springframework.boot") version "4.1.0" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    base
}

// A single version for the whole monorepo. Override per-module if needed.
allprojects {
    group = "com.nf"
    version = "0.0.1-SNAPSHOT"
    description = "spring-playground"
}

subprojects {
    // Default repositories for every module (mavenCentral + opt-in milestones).
    repositories {
        mavenCentral()
        // Uncomment for Spring Boot 4 milestone artifacts.
        // maven("https://repo.spring.io/milestone") {
        //     name = "spring-milestone"
        //     isAllowInsecureProtocol = false
        // }
    }
}
