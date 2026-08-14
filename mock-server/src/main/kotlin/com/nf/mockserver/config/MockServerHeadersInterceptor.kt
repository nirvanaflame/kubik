package com.nf.mockserver.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

/**
 * Adds a set of "extra headers" to every response, similar to how a
 * real API stub / CDN would tag responses. This makes the mock server
 * easy to recognise in logs and lets clients verify header handling.
 */
@Component
class MockServerHeadersInterceptor : HandlerInterceptor {

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        response.setHeader("X-Mock-Server", "mock-server")
        response.setHeader("X-Served-By", "spring-playground/mock-server")
        response.setHeader("X-Request-Id", newRequestId())
        response.setHeader("X-Cache", "HIT")
        response.setHeader("Access-Control-Allow-Origin", "*")
        response.setHeader("Cache-Control", "no-cache, no-store, max-age=0")
        return true
    }

    private fun newRequestId(): String =
        "mock-${java.util.UUID.randomUUID().toString().take(8)}"
}
