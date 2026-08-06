package com.nf.springplayground.counter;

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.BaseUnits
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class MyMetricsController(val meterRegistry: MeterRegistry) {

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping("/count")
    fun prometheus(): String {
        val counter = Counter.builder("count")
            .description("My counter")
            .baseUnit(BaseUnits.EVENTS)
            .tag("one", "tag")
            .tag("another", "tag")
            .register(meterRegistry)

        val copyOfContextMap = MDC.getCopyOfContextMap()
        log.info("Increment counter: {}", counter.count())
        counter.increment()
        log.info("Incremented counter: {}", counter.count())
        return "count: ${counter.count()}"
    }
}
