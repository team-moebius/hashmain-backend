package com.moebius.backend.service.kafka.producer

import com.moebius.backend.dto.message.MessageSendRequestDto
import org.apache.commons.lang3.StringUtils
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.kafka.support.serializer.JsonSerializer
import reactor.core.publisher.Flux
import reactor.kafka.sender.SenderResult
import spock.lang.Specification
import spock.lang.Subject

class MessageKafkaProducerTest extends Specification {
    def message = Stub(MessageSendRequestDto) {
        getDedupStrategy() >> "NO_DEDUP"
        getRecipientType() >> "SLACK"
        getTitle() >> "messageTitle"
    }

    @Subject
    def sut = Spy(MessageKafkaProducer, constructorArgs: [:]) {
        produceMessages(_ as MessageSendRequestDto) >> Flux.just(Stub(SenderResult) {
            correlationMetadata() >> "moebius.message.send.NO_DEDUP.SLACK.messageTitle.1234123"
        })
    } as MessageKafkaProducer

    def "Should get topic"() {
        expect:
        sut.getTopic() == "moebius.message.send"
    }

    def "Should get key serializer class"() {
        expect:
        sut.getKeySerializerClass() == StringSerializer.class
    }

    def "Should get value serializer class"() {
        expect:
        sut.getValueSerializerClass() == JsonSerializer.class
    }

    def "Should get key as symbol"() {
        expect:
        sut.getKey(message) == "NO_DEDUP.SLACK.messageTitle"
    }

    def "Should get correlation meta data"() {
        expect:
        def correlationMetaData = sut.getCorrelationMetadata(message)
        StringUtils.startsWith(correlationMetaData, "moebius.message.send.NO_DEDUP.SLACK.messageTitle.")
        correlationMetaData.split("\\.").last().toLong() != null
    }

    def "Should produce messages"() {
        when:
        def result = sut.produceMessages(message)

        then:
        result.subscribe({
            assert StringUtils.startsWith(
                    it.correlationMetadata(),
                    "moebius.message.send.NO_DEDUP.SLACK.messageTitle."
            )
        })
    }
}
