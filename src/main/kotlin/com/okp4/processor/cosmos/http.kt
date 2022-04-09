package com.okp4.processor.cosmos

import com.sun.net.httpserver.HttpServer
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress

const val metricsRoute = "/prometheus"
const val healthRoute = "/health"

private val logger = LoggerFactory.getLogger("com.okp4.processor.cosmos.http")

fun httpServe(port: Int = 8080) {
    HttpServer
        .create(InetSocketAddress(port), 0)
        .apply {
            arrayOf(
                createContext(metricsRoute, metricsHandler),
                createContext(healthRoute, healthHandler)
            ).forEach {
                logger.info("• Endpoint: ${address.hostName}:${address.port}${it.path}")
            }

            start()

            logger.info("Http server started")
        }
}
