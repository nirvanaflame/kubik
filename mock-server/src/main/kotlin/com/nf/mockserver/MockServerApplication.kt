package com.nf.mockserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Standalone mock HTTP server.
 *
 * Start with: `./gradlew :mock-server:bootRun` (defaults to :8080)
 * or override the port with --args='--server.port=8081'.
 */
@SpringBootApplication
class MockServerApplication

fun main(args: Array<String>) {
    runApplication<MockServerApplication>(*args)
}
