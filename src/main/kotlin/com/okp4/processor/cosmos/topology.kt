package com.okp4.processor.cosmos

import cosmos.tx.v1beta1.TxOuterClass
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Named
import org.apache.kafka.streams.kstream.Produced
import org.slf4j.LoggerFactory
import tendermint.types.BlockOuterClass
import java.util.*

/**
 * Simple Kafka Stream Processor that consumes a block on a topic and returns his transactions on another.
 */
fun topology(props: Properties): Topology {
    val logger = LoggerFactory.getLogger("com.okp4.processor.cosmos.topology")
    val topicIn = requireNotNull(props.getProperty("topic.in")) {
        "Option 'topic.in' was not specified."
    }
    val topicOut = requireNotNull(props.getProperty("topic.out")) {
        "Option 'topic.out' was not specified."
    }

    return StreamsBuilder()
        .apply {
            stream(topicIn, Consumed.with(Serdes.String(), Serdes.ByteArray()).withName("input"))
                .mapValues({ v -> BlockOuterClass.Block.parseFrom(v) }, Named.`as`("block-deserialization"))
                .peek({ _, block -> logger.debug("→ block ${block.header.height} (${block.data.txsCount} txs)") }, Named.`as`("log"))
                .flatMapValues({ block -> block.data.txsList.map { tx -> TxOuterClass.Tx.parseFrom(tx).toByteArray() } }, Named.`as`("extract-transactions"))
                .to(topicOut, Produced.with(Serdes.String(), Serdes.ByteArray()).withName("output"))
        }.build()
}
