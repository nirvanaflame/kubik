rootProject.name = "spring-playground"

// ------------------------------------------------------------------
// Monorepo: one Git repository hosting multiple independent Spring
// microservices plus shared libraries/modules.
//
// To add another microservice/library, drop a folder in the repo and
// register it here with include(":folder"). Each module keeps its own
// build.gradle.kts; shared Gradle configuration lives in the root
// build.gradle.kts.
// ------------------------------------------------------------------

include("spring-playground") // existing main application module
include("mock-server")       // standalone mock HTTP server (httpbin-style)
// include("libs:common-web") // example of a shared library module (uncomment when added)

// Optional: Gradle projects plugin for easier cross-module coordination.
// apply(plugin = "org.gradle.toolchains.foojay-resolver-convention")
