package com.moebius.backend.service.order

import com.moebius.backend.assembler.order.OrderAssembler
import com.moebius.backend.assembler.order.OrderAssetAssembler
import com.moebius.backend.domain.apikeys.ApiKey
import com.moebius.backend.domain.commons.EventType
import com.moebius.backend.domain.commons.Exchange
import com.moebius.backend.domain.commons.TradeType
import com.moebius.backend.domain.orders.*
import com.moebius.backend.dto.exchange.AssetDto
import com.moebius.backend.dto.frontend.response.OrderAssetResponseDto
import com.moebius.backend.dto.frontend.response.OrderResponseDto
import com.moebius.backend.dto.order.OrderAssetDto
import com.moebius.backend.dto.order.OrderDto
import com.moebius.backend.dto.trade.TradeDto
import com.moebius.backend.exception.DataNotFoundException
import com.moebius.backend.service.asset.AssetService
import com.moebius.backend.service.exchange.ExchangeServiceFactory
import com.moebius.backend.service.market.MarketService
import com.moebius.backend.service.member.ApiKeyService
import com.moebius.backend.service.order.validator.OrderValidator
import com.moebius.backend.utils.OrderUtil
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.time.LocalDateTime

class InternalOrderServiceTest extends Specification {
	def orderRepository = Mock(OrderRepository)
	def orderAssembler = Mock(OrderAssembler)
	def orderAssetAssembler = Mock(OrderAssetAssembler)
	def orderValidator = Mock(OrderValidator)
	def orderUtil = Mock(OrderUtil)
	def apiKeyService = Mock(ApiKeyService)
	def assetService = Mock(AssetService)
	def marketService = Mock(MarketService)
	def orderCacheService = Mock(OrderCacheService)
	def exchangeOrderService = Mock(ExchangeOrderService)
	def exchangeServiceFactory = Mock(ExchangeServiceFactory)

	def memberId = "5d8620bf46e0fb0001d64260"
	def exchange = Exchange.UPBIT

	@Subject
	def internalOrderService = new InternalOrderService(
			orderRepository,
			orderAssembler,
			orderAssetAssembler,
			orderValidator,
			orderUtil,
			apiKeyService,
			assetService,
			marketService,
			orderCacheService,
			exchangeOrderService,
			exchangeServiceFactory
	)

	@Unroll
	def "Should process orders"() {
		given:
		def orderDtos = [buildOrderDto(null, EventType.CREATE, "KRW-BTC", OrderPosition.PURCHASE, 1),
						 buildOrderDto("5ee5dd4c4941d136bae8e49b", EventType.DELETE, "KRW-BTC", OrderPosition.SALE, 1)] as List
		orderAssembler.assembleReadyOrder(_ as ApiKey, _ as OrderDto) >> Stub(Order)
		orderRepository.save(_ as Order) >> Mono.just(Stub(Order))
		marketService.getCurrentPrice(_ as Exchange, _ as String) >> Mono.just(10000000D)
		orderUtil.isOrderRequestNeeded(_ as Order, 10000000D) >> true
		orderRepository.deleteById(_ as ObjectId) >> Mono.empty()
		orderAssembler.assembleResponseDto(_ as List) >> OrderResponseDto.builder().orders(orderDtos).build()

		when:
		StepVerifier.create(internalOrderService.processOrders(memberId, exchange, orderDtos))
				.assertNext({
					assert it.getStatusCode() == HttpStatus.OK
					assert it.getBody() instanceof OrderResponseDto
					assert it.getBody().getOrders() instanceof List<? extends OrderDto>
					assert it.getBody().getOrders().size() == 2
				})
				.verifyComplete()

		then:
		1 * orderValidator.validate(orderDtos)
		1 * apiKeyService.getApiKeyByMemberIdAndExchange(memberId, exchange) >> Mono.just(Stub(ApiKey))
	}

	def "Should get orders by exchange"() {
		given:
		def orderDtos = [buildOrderDto("5ee4149e4941d136bae8e49a", EventType.READ, "KRW-BTC", OrderPosition.PURCHASE, 1),
						 buildOrderDto("5ee5dd4c4941d136bae8e49b", EventType.READ, "KRW-BTC", OrderPosition.SALE, 1)] as List

		when:
		StepVerifier.create(internalOrderService.getOrdersByExchange(memberId, exchange))
				.assertNext({
					assert it.getStatusCode() == HttpStatus.OK
					assert it.getBody() instanceof OrderResponseDto
					assert it.getBody().getOrders() instanceof List<? extends OrderDto>
					assert it.getBody().getOrders().size() == 2
				})
				.verifyComplete()

		then:
		1 * apiKeyService.getApiKeyByMemberIdAndExchange(memberId, exchange) >> Mono.just(Stub(ApiKey))
		1 * orderRepository.findAllByApiKeyId(_ as ObjectId) >> Flux.just(Stub(Order), Stub(Order))
		2 * orderAssembler.assembleDto(_ as Order, EventType.READ) >> Stub(OrderDto)
		1 * orderAssembler.assembleResponseDto(_ as List) >> OrderResponseDto.builder().orders(orderDtos).build()
	}

	def "Should get orders by exchange and symbol"() {
		given:
		def orderDtos = [buildOrderDto("5ee4149e4941d136bae8e49a", EventType.READ, "KRW-BTC", OrderPosition.PURCHASE, 1),
						 buildOrderDto("5ee5dd4c4941d136bae8e49b", EventType.READ, "KRW-ETH", OrderPosition.SALE, 1)] as List
		def filteredOrderDtos = [orderDtos.get(0)]

		when:
		StepVerifier.create(internalOrderService.getOrdersByExchangeAndSymbol(memberId, exchange, "KRW-BTC"))
				.assertNext({
					assert it.getStatusCode() == HttpStatus.OK
					assert it.getBody() instanceof OrderResponseDto
					assert it.getBody().getOrders() instanceof List<? extends OrderDto>
					assert it.getBody().getOrders().size() == 1
				})
				.verifyComplete()

		then:
		1 * apiKeyService.getApiKeyByMemberIdAndExchange(memberId, exchange) >> Mono.just(Stub(ApiKey))
		1 * orderRepository.findAllByApiKeyId(_ as ObjectId) >> Flux.just(Stub(Order), Stub(Order))
		2 * orderAssembler.assembleDto(_ as Order, EventType.READ) >> Stub(OrderDto)
		1 * orderUtil.filterOrdersBySymbol(_ as List, "KRW-BTC") >> filteredOrderDtos
		1 * orderAssembler.assembleResponseDto(_ as List) >> OrderResponseDto.builder().orders(filteredOrderDtos).build()
	}

	def "Should not get orders cause of not exist"() {
		given:
		1 * apiKeyService.getApiKeyByMemberIdAndExchange(memberId, exchange) >> Mono.just(Stub(ApiKey))
		1 * orderRepository.findAllByApiKeyId(_ as ObjectId) >> Flux.empty()
		0 * orderAssembler.assembleDto(_ as Order, EventType.READ)
		0 * orderAssembler.assembleResponseDto(_ as List)

		expect:
		StepVerifier.create(internalOrderService.getOrdersByExchange(memberId, exchange))
				.verifyError(DataNotFoundException.class)
	}

	def "Should get order assets"() {
		given:
		def orderDtos = [buildOrderDto("5ee4149e4941d136bae8e49a", EventType.READ, "KRW-BTC", OrderPosition.PURCHASE, 1),
						 buildOrderDto("5ee5dd4c4941d136bae8e49b", EventType.READ, "KRW-BTC", OrderPosition.SALE, 1)] as List

		when:
		StepVerifier.create(internalOrderService.getOrderAssets(memberId, exchange))
				.assertNext({
					assert it.getStatusCode() == HttpStatus.OK
					assert it.getBody() instanceof OrderAssetResponseDto
					assert it.getBody().getOrderAssets() instanceof List<? extends OrderAssetDto>
					assert it.getBody().getOrderAssets().size() == 1
				})
				.verifyComplete()

		then:
		1 * apiKeyService.getApiKeyByMemberIdAndExchange(memberId, exchange) >> Mono.just(Stub(ApiKey))
		1 * orderRepository.findAllByApiKeyIdAndOrderStatusNot(_ as ObjectId, OrderStatus.DONE) >> Flux.just(Stub(Order), Stub(Order))
		2 * orderAssembler.assembleDto(_ as Order, EventType.READ) >> Stub(OrderDto)
		1 * orderAssetAssembler.assembleCurrencyToOrderDtos(_ as List) >> ["BTC": orderDtos]
		1 * assetService.getCurrencyAssetMap(memberId, exchange) >> Mono.just(["BTC": Stub(AssetDto)])
		1 * marketService.getCurrencyMarketPriceMap(exchange) >> Mono.just(["BTC": 10000000D])
		1 * orderAssetAssembler.assembleOrderAssetDto(_ as List, _ as AssetDto, 10000000D) >> Stub(OrderAssetDto)
		1 * orderAssetAssembler.assembleOrderAssetResponse(_ as List) >> OrderAssetResponseDto.builder().orderAssets([Stub(OrderAssetDto)]).build()
	}

	def "Should request updating order status"() {
		when:
		internalOrderService.updateOrderStatusByTrade(buildTradeDto())

		then:
		1 * orderAssembler.assembleInProgressStatusCondition(_ as TradeDto) >> Stub(OrderStatusCondition)
		1 * orderRepository.findAllByOrderStatusCondition(_ as OrderStatusCondition) >> Flux.just(Stub(Order), Stub(Order))
	}

	OrderDto buildOrderDto(String id, EventType eventType, String symbol, OrderPosition orderPosition, int level) {
		OrderDto orderDto = new OrderDto()
		orderDto.setId(id)
		orderDto.setEventType(eventType)
		orderDto.setExchange(Exchange.UPBIT)
		orderDto.setSymbol(symbol)
		orderDto.setOrderPosition(orderPosition)
		orderDto.setOrderStatus(OrderStatus.READY)
		orderDto.setOrderType(OrderType.LIMIT)
		orderDto.setLevel(level)

		return orderDto
	}

	TradeDto buildTradeDto() {
		TradeDto tradeDto = new TradeDto()
		tradeDto.setExchange(Exchange.UPBIT)
		tradeDto.setSymbol("KRW-BTC")
		tradeDto.setTradeType(TradeType.ASK)
		tradeDto.setCreatedAt(LocalDateTime.now())

		return tradeDto
	}
}
