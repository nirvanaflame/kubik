package com.nf.springplayground.prometeus;

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class MyMetricsController(val meterRegistry: MeterRegistry) {

    @GetMapping("/count")
    fun prometheus(): String {
        meterRegistry.counter("count").increment()
        return "count: ${meterRegistry.counter("count").count()}"
    }
}
