package com.nf.springplayground;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
class InstallOpenTelemetryAppender implements InitializingBean {

    private final OpenTelemetry openTelemetry;

    InstallOpenTelemetryAppender(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    @Override
    public void afterPropertiesSet() {
        OpenTelemetryAppender.install(this.openTelemetry);
        // DIAGNOSTIC: emit a log record directly through the SDK logs bridge
        try {
            this.openTelemetry.getLogsBridge().get("diag")
                .logRecordBuilder()
                .setBody("DIAG-sdk-direct-log-12345")
                .emit();
            System.out.println("DIAG: direct SDK log emitted; loggerProvider class="
                + this.openTelemetry.getLogsBridge().getClass().getName());
        } catch (Throwable t) {
            System.err.println("DIAG: direct SDK log failed: " + t);
        }
    }
}