package com.okp4.processor.cosmos

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.streams.errors.DeserializationExceptionHandler
import org.apache.kafka.streams.errors.DeserializationExceptionHandler.DeserializationHandlerResponse.CONTINUE
import org.apache.kafka.streams.processor.ProcessorContext

class IgnoreUnMarshallingExceptionHandler : DeserializationExceptionHandler {
    override fun configure(p0: MutableMap<String, *>?) {
    }

    override fun handle(
        context: ProcessorContext?,
        record: ConsumerRecord<ByteArray, ByteArray>?,
        exception: java.lang.Exception?
    ): DeserializationExceptionHandler.DeserializationHandlerResponse {
        return CONTINUE
    }
}
