package com.nf.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {
    private static final Logger log = LoggerFactory.getLogger(PingController.class);

    @GetMapping("/ping")
    public String ping() {
        log.info("Ping response");
        return "pong";
    }

    @GetMapping()
    public String index() {
        log.debug("Index hit with trace info - check MDC for traceId/spanId");
        return "spring-playground running";
    }
}
