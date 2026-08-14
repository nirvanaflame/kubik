package com.nf.mockserver.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/** Registers {@link MockServerHeadersInterceptor} for all paths. */
@Configuration
class MockServerWebConfig(
    private val headersInterceptor: MockServerHeadersInterceptor,
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(headersInterceptor)
            .addPathPatterns("/**")
    }
}