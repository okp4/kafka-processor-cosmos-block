package com.okp4.processor.cosmos

import com.google.protobuf.ByteString
import cosmos.tx.v1beta1.TxOuterClass
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import tendermint.types.BlockOuterClass.Block

class TopologyTest : BehaviorSpec({
    val stringSerde = Serdes.StringSerde()
    val byteArraySerde = Serdes.ByteArraySerde()
    val config = mapOf(
        StreamsConfig.APPLICATION_ID_CONFIG to "simple",
        StreamsConfig.BOOTSTRAP_SERVERS_CONFIG to "dummy:1234",
        "topic.in" to "in",
        "topic.out" to "out"
    ).toProperties()

    // Blocks definitions
    val txDefault = TxOuterClass.Tx.getDefaultInstance()
    val blockTx = Block.getDefaultInstance()
    val blockTxs = Block.parseFrom(txDefault.toByteString())
    val blockEmpty = Block.parseFrom("".toByteArray())

    val blockFaulty = Block.parseFrom("".toByteArray())

    given("A topology") {
        val topology = topology(config)
        val testDriver = TopologyTestDriver(topology, config)
        val inputTopic = testDriver.createInputTopic("in", stringSerde.serializer(), byteArraySerde.serializer())
        val outputTopic = testDriver.createOutputTopic("out", stringSerde.deserializer(), byteArraySerde.deserializer())

        withData(
            mapOf(
                "block with one transaction" to arrayOf(blockTx, txDefault.toString(), 1),
                "block with three transactions" to arrayOf(blockTxs, arrayOf(txDefault, txDefault).toString().toByteArray(), 3),
                "block with no transaction" to arrayOf(blockEmpty, "".toByteArray(), 0),
                "block with faulty transaction" to arrayOf(blockFaulty, "".toByteArray(), 0)
            )
        ) { (block, expectedTx, nbTxs) ->
            When("sending block with $nbTxs txs to the input topic ($inputTopic)") {
                inputTopic.pipeInput("", block.toString().toByteArray())

                then("message is received from the output topic ($outputTopic)") {
                    val result = outputTopic.readValuesToList()

                    result shouldNotBe null
                    result.size shouldBe nbTxs
                    result shouldBe expectedTx
                }
            }
        }
    }
})
