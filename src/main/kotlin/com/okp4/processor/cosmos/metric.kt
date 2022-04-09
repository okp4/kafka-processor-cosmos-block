package com.okp4.processor.cosmos

import com.sun.net.httpserver.HttpExchange
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.micrometer.prometheus.PrometheusRenameFilter
import java.io.PrintWriter

val prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    .apply {
        config().meterFilter(PrometheusRenameFilter())
    }
    .also { registry ->
        arrayOf(
            ClassLoaderMetrics(),
            JvmMemoryMetrics(),
            JvmGcMetrics(),
            ProcessorMetrics(),
            UptimeMetrics(),
            JvmThreadMetrics()
        ).forEach { it.bindTo(registry) }
    }

val metricsHandler = { httpExchange: HttpExchange ->
    val response = prometheusRegistry.scrape()

    httpExchange.run {
        responseHeaders.add("Content-type", "text/plain")
        sendResponseHeaders(200, response.toByteArray().size.toLong())

        PrintWriter(responseBody).use { out ->
            out.print(response)
        }
    }
}
