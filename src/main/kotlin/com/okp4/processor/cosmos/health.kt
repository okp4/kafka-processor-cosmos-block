package com.okp4.processor.cosmos

import com.sun.net.httpserver.HttpExchange
import org.apache.kafka.streams.KafkaStreams.State.RUNNING

/**
 * standard health (kubernetes) for the stream processor.
 */
val healthHandler = { httpExchange: HttpExchange ->
    when (stream.get()?.state()) {
        RUNNING ->
            httpExchange.sendResponseHeaders(200, 0)
        else ->
            httpExchange.sendResponseHeaders(503, 0)
    }
    httpExchange.responseBody.close()
}
