package com.nf.springplayground

import io.micrometer.observation.ObservationPredicate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.server.observation.ServerRequestObservationContext

/**
 * Keeps Actuator endpoints out of OpenTelemetry traces.
 *
 * Vetoes the `http.server.requests` observation (so no span is created at all)
 * for any request whose path starts with [/actuator].
 */
@Configuration(proxyBeanMethods = false)
class OtelObservationConfig {

    @Bean
    fun excludeActuatorFromTraces(): ObservationPredicate =
        ObservationPredicate { name, context ->
            !(name == HTTP_SERVER_REQUESTS && context is ServerRequestObservationContext && context.isActuatorRequest())
        }

    private fun ServerRequestObservationContext.isActuatorRequest(): Boolean {
        // pathPattern is only populated lazily by the observation convention
        // after start(), so match on the request URI (the context carrier).
        val requestUri = carrier?.requestURI
        return requestUri?.startsWith(ACTUATOR_BASE_PATH) == true
    }

    private companion object {
        const val HTTP_SERVER_REQUESTS = "http.server.requests"
        const val ACTUATOR_BASE_PATH = "/actuator"
    }
}
