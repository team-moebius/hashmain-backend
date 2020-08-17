package com.moebius.backend.service.kafka.producer

import com.moebius.backend.dto.trade.TradeDto
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.kafka.support.serializer.JsonSerializer
import reactor.core.publisher.Flux
import reactor.kafka.sender.SenderResult
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

class TradeKafkaProducerTest extends Specification {
	@Shared
	def message = Stub(TradeDto) {
		getSymbol() >> "KRW-BTC"
	}

	@Subject
	def tradeKafkaProducer = Spy(TradeKafkaProducer, constructorArgs: [:]) {
		produceMessages(_ as TradeDto) >> Flux.just(Stub(SenderResult) {
			correlationMetadata() >> "moebius.trade.upbit.KRW-BTC"
		})
	} as TradeKafkaProducer

	def "Should get topic"() {
		expect:
		tradeKafkaProducer.getTopic() == "moebius.trade.upbit"
	}

	def "Should get key serializer class"() {
		expect:
		tradeKafkaProducer.getKeySerializerClass() == StringSerializer.class
	}

	def "Should get value serializer class"() {
		expect:
		tradeKafkaProducer.getValueSerializerClass() == JsonSerializer.class
	}

	def "Should get key as symbol"() {
		expect:
		tradeKafkaProducer.getKey(message) == "KRW-BTC"
	}

	def "Should get correlation meta data"() {
		expect:
		tradeKafkaProducer.getCorrelationMetadata(message) == "moebius.trade.upbit.KRW-BTC"
	}

	def "Should produce messages"() {
		when:
		def result = tradeKafkaProducer.produceMessages(message)

		then:
		result.subscribe({
			it -> assert it.correlationMetadata() == "moebius.trade.upbit.KRW-BTC"
		})
	}
}
