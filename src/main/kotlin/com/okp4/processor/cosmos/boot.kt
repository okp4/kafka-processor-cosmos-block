package com.okp4.processor.cosmos

import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import org.slf4j.LoggerFactory
import java.io.StringReader
import java.util.*

private val logger = LoggerFactory.getLogger("com.okp4.processor.cosmos.boot")

fun boot(args: Array<String>, topologyProvider: (props: Properties) -> Topology) {
    logger.info("Booting ${topologyProvider.javaClass.simpleName} topology")

    val props =
        Properties()
            .apply {
                load(StringReader(args.joinToString("\n")))

                put(
                    StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG,
                    IgnoreUnMarshallingExceptionHandler::class.java
                )
            }
    val topology = topologyProvider(props)
        .also {
            logger.info("Topology:\n${it.describe()}")
        }

    httpServe()

    startStream(topology, props)
}
