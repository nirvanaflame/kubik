import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// ------------------------------------------------------------------
// Module :spring-playground — the existing main Spring Boot service.
// Applies the Spring Boot Gradle plugin so it produces a runnable fat jar.
//
// Note: plugin versions come from the ROOT build.gradle.kts (apply false),
// so this module does not re-declare versions.
// ------------------------------------------------------------------

plugins {
    kotlin("jvm")
    kotlin("plugin.spring") // enables `allOpen` for marker annotations & open class support
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

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

dependencies {
    // --- Observability (metrics + tracing) --
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    implementation("io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0:2.28.1-alpha")
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")

    // --- Web ---
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")

    // --- Kotlin helpers --
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin") // Spring Boot 4 ships Jackson from tools.jakarta by default

    // --- Dev loop --
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    // --- Annotation processing --
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // --- Tests ---
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
    systemProperty("otel.metrics.exporter", "otlp")          // optional: export test traces to OTLP
    systemProperty("otel.traces.exporter", "logging")        // keep noisy but useful output on console
}
