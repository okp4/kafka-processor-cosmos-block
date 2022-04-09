package com.okp4.processor.cosmos

import io.micrometer.core.instrument.binder.kafka.KafkaStreamsMetrics
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION
import org.slf4j.LoggerFactory
import sun.misc.Signal
import java.time.Duration.ofSeconds
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import kotlin.system.exitProcess

internal val stream = AtomicReference<KafkaStreams?>()
private val logger = LoggerFactory.getLogger("com.okp4.processor.cosmos.stream")

fun startStream(topology: Topology, props: Properties) {
    KafkaStreams(
        topology, props
    ).run {
        val latch = CountDownLatch(1)

        arrayOf("INT", "TERM")
            .map(::Signal)
            .forEach {
                Signal.handle(
                    it
                ) {
                    close(ofSeconds(10))
                    latch.countDown()
                }
            }

        stream.set(this)

        KafkaStreamsMetrics(this).bindTo(prometheusRegistry)

        try {
            logger.info("Initializing")
            this.setUncaughtExceptionHandler { e ->
                logger.error("Exception caught: '${e.message}'. Exiting.", e)
                latch.countDown()
                SHUTDOWN_APPLICATION
            }

            cleanUp()
            logger.info("Starting stream")
            start()
            logger.info("Stream started")

            latch.await()

            logger.info("Bye!")

            exitProcess(0)
        } catch (e: Exception) {
            logger.error("Unexpected error occurred. Exiting.", e)
            exitProcess(1)
        }
    }
}
