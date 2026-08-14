package com.nf.springplayground.controller

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity

@RestController
class PingController(
    // Inject the Spring-managed, observation-aware RestClient.Builder bean.
    // Using the static RestClient.builder() would BYPASS HTTP-client observation
    // and produce no client span in the trace.
    restClientBuilder: RestClient.Builder,
) {

    private val log = LoggerFactory.getLogger(PingController::class.java)

    private val restClient: RestClient = restClientBuilder.baseUrl("https://httpbin.org").build()

    @GetMapping("/ping")
    fun ping(): String {
        log.info("Ping response")
        return "pong"
    }

    @GetMapping
    fun index(): String {
        log.debug("Index hit with trace info - check MDC for traceId/spanId")
        return "spring-playground running"
    }

    @GetMapping("/json")
    fun json(): String? {
        log.info("GET /json")
        val entity = restClient.get().uri("/json").retrieve().toEntity<String>()
        log.info("json: {}", entity)
        return entity.body
    }
}
