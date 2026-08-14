package com.nf.mockserver

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Verifies the mock server behaves like httpbin.org/json AND attaches
 * the extra response headers on every request.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MockServerApplicationTests {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `json returns httpbin-style slideshow payload`() {
        mockMvc.perform(get("/json"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "application/json"))
            // httpbin.org/json structure
            .andExpect(jsonPath("$.slideshow.title").value("Sample Slide Show"))
            .andExpect(jsonPath("$.slideshow.author").value("Yours Truly"))
            .andExpect(jsonPath("$.slideshow.date").value("date of publication"))
            .andExpect(jsonPath("$.slideshow.slides.length()").value(2))
            .andExpect(jsonPath("$.slideshow.slides[1].items[0]").value("Why <em>WonderWidgets</em> are great"))
    }

    @Test
    fun `every response carries the extra mock headers`() {
        val result = mockMvc.perform(get("/json"))
            .andExpect(status().isOk)
            .andReturn()

        // headers injected by MockServerHeadersInterceptor on all requests
        result.response.getHeader("X-Mock-Server").let { checkNotNull(it) { "missing X-Mock-Server" } }
        result.response.getHeader("X-Served-By").let { checkNotNull(it) { "missing X-Served-By" } }
        result.response.getHeader("X-Request-Id").let {
            check(it != null && it.startsWith("mock-")) { "bad X-Request-Id: $it" }
        }
        result.response.getHeader("X-Cache").let { check(it == "HIT") { "bad X-Cache: $it" } }
    }
}
