package com.nf.mockserver.controller

import com.nf.mockserver.model.HttpBinJson
import com.nf.mockserver.model.JsonResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Controller exposing dummy API responses (httpbin.org style).
 *
 * Endpoints:
 *   GET /json        -> httpbin.org/json body + extra headers
 *   GET /            -> simple service banner
 *   GET /anything    -> echoes the request back as JSON (useful stub)
 */
@RestController
@RequestMapping
class MockServerController {

    /** Returns the canonical httpbin.org/json payload. */
    @GetMapping("/json")
    fun json(): JsonResponse = HttpBinJson.sample()

    @GetMapping("/")
    fun index(): Map<String, Any> = mapOf(
        "service" to "mock-server",
        "message" to "Dummy responses for local development",
        "endpoints" to listOf("/json", "/anything"),
    )

    /** Stub that echoes a few request facts back as a JSON document. */
    @GetMapping("/anything")
    fun anything(): Map<String, Any> = mapOf(
        "method" to "GET",
        "url" to "/anything",
        "headers-here" to "see the X-* response headers",
    )

    /** Demo controller returning headers explicitly too (belt & braces). */
    @GetMapping("/json-with-headers")
    fun jsonWithHeaders(): ResponseEntity<JsonResponse> =
        ResponseEntity.ok()
            .header("X-Mock-Server", "mock-server")
            .header("X-Example", "explicit-header")
            .body(HttpBinJson.sample())
}
