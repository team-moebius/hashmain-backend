package com.moebius.backend.service.kafka.consumer

import com.moebius.backend.dto.TradeDto
import com.moebius.backend.service.market.MarketService
import com.moebius.backend.service.order.ExchangeOrderService
import com.moebius.backend.service.trade.TradeService
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.kafka.support.serializer.JsonDeserializer
import reactor.kafka.receiver.ReceiverOffset
import reactor.kafka.receiver.ReceiverRecord
import spock.lang.Specification
import spock.lang.Subject

class TradeKafkaConsumerTest extends Specification {
	def exchangeOrderService = Mock(ExchangeOrderService)
	def marketService = Mock(MarketService)
	def tradeService = Mock(TradeService)
	def receiverRecord = Stub(ReceiverRecord) {
		receiverOffset() >> Stub(ReceiverOffset)
		value() >> Stub(TradeDto)
	}

	@Subject
	def tradeKafkaConsumer = new TradeKafkaConsumer([:], exchangeOrderService, marketService, tradeService)

	def "Should consume messages"() {
		when:
		tradeKafkaConsumer.consumeMessages()

		then:
		tradeKafkaConsumer.processRecord(receiverRecord)
	}

	def "Should get topic"() {
		expect:
		tradeKafkaConsumer.getTopic() == "moebius.trade.upbit"
	}

	def "Should process topic and business logic"() {
		when:
		tradeKafkaConsumer.processRecord(receiverRecord)

		then:
		1 * exchangeOrderService.updateOrderStatus(_ as TradeDto)
		1 * exchangeOrderService.orderWithTradeDto(_ as TradeDto)
		1 * marketService.updateMarketPrice(_ as TradeDto)
		1 * tradeService.identifyValidTrade(_ as TradeDto)
	}

	def "Should get key deserializer class"() {
		expect:
		tradeKafkaConsumer.getKeyDeserializerClass() == StringDeserializer.class
	}

	def "Should get value deserializer class"() {
		expect:
		tradeKafkaConsumer.getValueDeserializerClass() == JsonDeserializer.class
	}
}
