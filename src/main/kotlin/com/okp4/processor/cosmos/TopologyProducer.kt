package com.okp4.processor.cosmos

import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.*
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.slf4j.LoggerFactory
import tendermint.types.BlockOuterClass
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.inject.Produces

/**
 * Simple Kafka Stream Processor that consumes a block on a topic and returns his transactions on another.
 */
@ApplicationScoped
class TopologyProducer {
    @field:ConfigProperty(name = "topic.in", defaultValue = "topic.in")
    lateinit var topicIn: String

    @field:ConfigProperty(name = "topic.out", defaultValue = "topic.out")
    lateinit var topicOut: String

    @field:ConfigProperty(name = "topic.error")
    var topicError: Optional<String> = Optional.empty()

    @Produces
    fun buildTopology(): Topology {
        val logger = LoggerFactory.getLogger(TopologyProducer::class.java)

        return StreamsBuilder().apply {
            stream(topicIn, Consumed.with(Serdes.String(), Serdes.ByteArray()).withName("input")).mapValues(
                { v ->
                    Pair(v, kotlin.runCatching { BlockOuterClass.Block.parseFrom(v) })
                }, Named.`as`("block-deserialization")
            ).split().branch(
                { _, v -> v.second.isFailure },
                Branched.withConsumer { ks ->
                    ks.peek(
                        { k, v ->
                            v.second.onFailure {
                                logger.warn("Deserialization failed for block with key <$k>: ${it.message}", it)
                            }
                        },
                        Named.`as`("log-deserialization-failure")
                    )
                        .mapValues({ pair -> pair.first }, Named.`as`("extract-original-bytearray"))
                        .apply {
                            if (topicError.isPresent) {
                                logger.info("Failed block will be sent to the topic $topicError")
                                to(
                                    topicError.get(),
                                    Produced.with(Serdes.String(), Serdes.ByteArray()).withName("error")
                                )
                            }
                        }
                }
            ).defaultBranch(
                Branched.withConsumer { ks ->
                    ks.mapValues(
                        { v ->
                            v.second.getOrThrow()
                        }, Named.`as`("extract-block")
                    ).peek(
                        { _, block -> logger.debug("→ block ${block.header.height} (${block.data.txsCount} txs)") },
                        Named.`as`("log-block-extraction")
                    ).flatMapValues(
                        { block ->
                            block.data.txsList
                        }, Named.`as`("extract-transactions")
                    ).mapValues(
                        { tx ->
                            tx.toByteArray()
                        }, Named.`as`("convert-transactions-to-bytearray")
                    ).to(
                        topicOut, Produced.with(Serdes.String(), Serdes.ByteArray()).withName("output")
                    )
                }
            )
        }.build()
    }
}
