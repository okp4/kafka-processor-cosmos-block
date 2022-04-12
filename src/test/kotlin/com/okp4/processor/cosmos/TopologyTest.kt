package com.okp4.processor.cosmos

import com.google.protobuf.Any
import com.google.protobuf.ByteString
import cosmos.tx.v1beta1.TxOuterClass
import cosmos.tx.v1beta1.TxOuterClass.AuthInfo
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import tendermint.types.BlockOuterClass.Block
import tendermint.types.Types

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
    val txDefault = TxOuterClass.Tx.newBuilder()
        .addSignatures(ByteString.copyFromUtf8(""))
        .setAuthInfo(
            AuthInfo.newBuilder()
                .addSignerInfos(
                    TxOuterClass.SignerInfo.getDefaultInstance()
                )
                .build()
        )
        .setBody(
            TxOuterClass.TxBody.newBuilder()
                .addMessages(
                    Any.newBuilder()
                        .setValue(ByteString.copyFromUtf8("test message"))
                        .build()
                )
                .build()
        )
        .build()
    val txDefaultBA = txDefault.toByteArray()
    val txSimple = TxOuterClass.Tx.newBuilder()
        .addSignatures(ByteString.copyFromUtf8(""))
        .build()
    val txSimpleBA = txSimple.toByteArray()
    val blockTx = Block.newBuilder()
        .setData(Types.Data.newBuilder().addTxs(txDefault.toByteString()))
        .build()
        .toByteArray()
    val blockTxs = Block.newBuilder()
        .setData(
            Types.Data.newBuilder()
                .addAllTxs(
                    listOf(
                        txSimple.toByteString(),
                        txSimple.toByteString(),
                        txSimple.toByteString(),
                    )
                )
                .build()
        )
        .build()
        .toByteArray()
    val blockEmpty = Block.newBuilder()
        .build()
        .toByteArray()
    val brokenBlock = "test".toByteArray()

    given("A topology") {
        val topology = topology(config)
        val testDriver = TopologyTestDriver(topology, config)
        val inputTopic = testDriver.createInputTopic("in", stringSerde.serializer(), byteArraySerde.serializer())
        val outputTopic = testDriver.createOutputTopic("out", stringSerde.deserializer(), byteArraySerde.deserializer())

        withData(
            mapOf(
                "block with one transaction" to arrayOf(blockTx, listOf(txDefaultBA), 1),
                "block with three transactions" to arrayOf(blockTxs, listOf(txSimpleBA, txSimpleBA, txSimpleBA), 3),
                "block with no transaction" to arrayOf(blockEmpty, "".toByteArray(), 0),
                "broken block" to arrayOf(brokenBlock, "".toByteArray(), 0)
            )
        ) { (block, expectedTx, nbTxs) ->
            When("sending block with $nbTxs txs to the input topic ($inputTopic)") {
                inputTopic.pipeInput("", block as ByteArray)

                then("$nbTxs are received from the output topic ($outputTopic)") {
                    val result = outputTopic.readValuesToList()

                    result shouldNotBe null
                    result shouldBe expectedTx
                }
            }
        }
    }
})
