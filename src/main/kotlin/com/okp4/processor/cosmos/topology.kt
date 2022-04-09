package com.okp4.processor.cosmos

import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Named
import org.apache.kafka.streams.kstream.Produced
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Simple Kafka Stream Processor that consumes a message on a topic and returns a new message on another.
 */
fun topology(props: Properties): Topology {
    val logger = LoggerFactory.getLogger("com.okp4.processor.cosmos.topology")
    val topicIn = requireNotNull(props.getProperty("topic.in")) {
        "Option 'topic.in' was not specified."
    }
    val topicOut = requireNotNull(props.getProperty("topic.out")) {
        "Option 'topic.out' was not specified."
    }

    val streamsBuilder = StreamsBuilder()

    streamsBuilder.stream(topicIn, Consumed.with(Serdes.String(), Serdes.String()).withName("input"))
        .peek({ _, _ -> logger.info("Received a message") }, Named.`as`("log"))
        .map({ k, v -> KeyValue(k, "Hello $v!") }, Named.`as`("map-value"))
        .to(topicOut, Produced.with(Serdes.String(), Serdes.String()).withName("output"))

    return streamsBuilder.build()
}
