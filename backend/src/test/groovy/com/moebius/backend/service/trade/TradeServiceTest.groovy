package com.moebius.backend.service.trade

import com.moebius.backend.service.kafka.consumer.TradeKafkaConsumer
import org.springframework.boot.context.event.ApplicationReadyEvent
import spock.lang.Specification
import spock.lang.Subject

class TradeServiceTest extends Specification {
	def tradeKafkaConsumer = Mock(TradeKafkaConsumer)

	@Subject
	def tradeService = new TradeService(tradeKafkaConsumer)

	def "Should start to consume on application ready event"() {
		given:
		def event = Stub(ApplicationReadyEvent)

		when:
		tradeService.onApplicationEvent(event)

		then:
		1 * tradeKafkaConsumer.consumeMessages()
	}
}
