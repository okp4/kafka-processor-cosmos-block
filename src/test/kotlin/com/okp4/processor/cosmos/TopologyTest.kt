package com.okp4.processor.cosmos

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver

class TopologyTest : BehaviorSpec({
    val stringSerde = Serdes.StringSerde()
    val config = mapOf(
        StreamsConfig.APPLICATION_ID_CONFIG to "simple",
        StreamsConfig.BOOTSTRAP_SERVERS_CONFIG to "dummy:1234",
        "topic.in" to "in",
        "topic.out" to "out"
    ).toProperties()

    given("A topology") {
        val topology = topology(config)
        val testDriver = TopologyTestDriver(topology, config)
        val inputTopic = testDriver.createInputTopic("in", stringSerde.serializer(), stringSerde.serializer())
        val outputTopic = testDriver.createOutputTopic("out", stringSerde.deserializer(), stringSerde.deserializer())

        withData(
            mapOf(
                "simple message" to arrayOf("John Doe", "Hello John Doe!"),
                "empty message" to arrayOf("", "Hello !"),
            )
        ) { (message, expectedMessage) ->
            When("sending the message <$message> to the input topic ($inputTopic)") {
                inputTopic.pipeInput("", message)

                then("message is received from the output topic ($outputTopic)") {
                    val result = outputTopic.readKeyValue()

                    result shouldNotBe null
                    result.value shouldBe expectedMessage
                }
            }
        }
    }
})
