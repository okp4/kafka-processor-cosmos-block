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
import java.util.*
import kotlin.random.Random.Default.nextBytes

fun List<TxOuterClass.Tx>.wrapIntoBlock(height: Long): Block = Block.newBuilder().setHeader(
    Types.Header.newBuilder().setChainId("okp4-testnet").setHeight(height).build()
).setData(
    Types.Data.newBuilder().addAllTxs(this.map { it.toByteString() }).build()
).build()

val txDefault: TxOuterClass.Tx = TxOuterClass.Tx.newBuilder().addSignatures(ByteString.copyFromUtf8("")).setAuthInfo(
    AuthInfo.newBuilder().addSignerInfos(
        TxOuterClass.SignerInfo.getDefaultInstance()
    ).build()
).setBody(
    TxOuterClass.TxBody.newBuilder().addMessages(
        Any.newBuilder().setValue(ByteString.copyFromUtf8("test message")).build()
    ).build()
).build()

val txSimple: TxOuterClass.Tx = TxOuterClass.Tx.newBuilder().addSignatures(ByteString.copyFromUtf8("")).build()

class TopologyTest : BehaviorSpec({
    val stringSerde = Serdes.StringSerde()
    val byteArraySerde = Serdes.ByteArraySerde()
    val config = mapOf(
        StreamsConfig.APPLICATION_ID_CONFIG to "simple",
        StreamsConfig.BOOTSTRAP_SERVERS_CONFIG to "dummy:1234",
        "topic.in" to "in",
        "topic.out" to "out",
        "topic.error" to "error"
    ).toProperties()

    given("A topology") {
        val topologyProducer = TopologyProducer().apply {
            topicIn = "in"
            topicOut = "out"
            topicError = Optional.of("error")
        }

        val testDriver = TopologyTestDriver(topologyProducer.buildTopology(), config)
        val inputTopic = testDriver.createInputTopic("in", stringSerde.serializer(), byteArraySerde.serializer())
        val outputTopic = testDriver.createOutputTopic("out", stringSerde.deserializer(), byteArraySerde.deserializer())
        val errorTopic = testDriver.createOutputTopic("error", stringSerde.deserializer(), byteArraySerde.deserializer())

        withData(
            mapOf(
                "block with no transaction" to arrayOf(listOf<TxOuterClass.Tx>().wrapIntoBlock(12)),
                "block with one transaction" to arrayOf(listOf(txDefault, txSimple).wrapIntoBlock(13)),
                "block with three transaction" to arrayOf(listOf(txDefault, txSimple, txSimple).wrapIntoBlock(14)),
            )
        ) { (block) ->
            and("a block carrying ${block.data.txsCount} transactions") {
                val serializedBlock = block.toByteArray()

                When("sending the block to the input topic ($inputTopic)") {
                    inputTopic.pipeInput("", serializedBlock)

                    then("${block.data.txsCount} transactions are received from the output topic ($outputTopic)") {
                        val result = outputTopic.readValuesToList()

                        result shouldNotBe null
                        result.size shouldBe block.data.txsCount

                        result.indices.forEach {
                            result[it] shouldBe block.data.txsList[it].toByteArray()
                        }
                    }
                }
            }
        }

        and("an invalid block") {
            val invalidBlock = nextBytes(500)

            When("sending the block to the input topic ($inputTopic)") {
                inputTopic.pipeInput("", invalidBlock)

                then("no transactions are received from the output topic ($outputTopic)") {
                    outputTopic.isEmpty shouldBe true

                    val result = errorTopic.readValuesToList()

                    result shouldNotBe null
                    result.size shouldBe 1
                    result[0] shouldBe invalidBlock
                }
            }
        }
    }
})
