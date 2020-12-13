package com.moebius.backend.service.kafka.consumer

import com.moebius.backend.domain.apikeys.ApiKey
import com.moebius.backend.dto.exchange.AssetDto
import com.moebius.backend.dto.trade.TradeDto
import com.moebius.backend.service.asset.AssetService
import com.moebius.backend.service.market.MarketService
import com.moebius.backend.service.order.ExchangeOrderService
import com.moebius.backend.service.order.InternalOrderService
import com.moebius.backend.service.trade.TradeService
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.kafka.support.serializer.JsonDeserializer
import reactor.core.publisher.Flux
import reactor.kafka.receiver.ReceiverOffset
import reactor.kafka.receiver.ReceiverRecord
import reactor.util.function.Tuple2
import spock.lang.Specification
import spock.lang.Subject

class UpbitKafkaConsumerTest extends Specification {
	def exchangeOrderService = Mock(ExchangeOrderService)
	def internalOrderService = Mock(InternalOrderService)
	def marketService = Mock(MarketService)
	def tradeService = Mock(TradeService)
	def assetService = Mock(AssetService)
	def receiverRecord = Stub(ReceiverRecord) {
		receiverOffset() >> Stub(ReceiverOffset)
		value() >> Stub(TradeDto)
	}

	@Subject
	def tradeKafkaConsumer = new UpbitKafkaConsumer([:], exchangeOrderService, internalOrderService, marketService, tradeService, assetService)

	def "Should get topic"() {
		expect:
		tradeKafkaConsumer.getTopic() == "moebius.trade.upbit"
	}

	def "Should process topic and business logic"() {
		when:
		tradeKafkaConsumer.processRecord(receiverRecord)

		then:
		1 * tradeService.notifyIfValidTrade(_ as TradeDto)
		1 * internalOrderService.updateOrderStatusByTrade(_ as TradeDto)
		1 * exchangeOrderService.orderByTrade(_ as TradeDto)
		1 * marketService.updateMarketPrice(_ as TradeDto)
		1 * assetService.getApiKeyWithAssets(_ as TradeDto) >> Flux.just(new Tuple2<ApiKey, AssetDto>(Stub(ApiKey), Stub(AssetDto)))
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
