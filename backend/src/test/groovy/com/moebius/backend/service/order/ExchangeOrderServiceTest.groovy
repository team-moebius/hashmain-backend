package com.moebius.backend.service.order

import com.moebius.backend.assembler.order.OrderAssembler
import com.moebius.backend.utils.OrderUtil
import com.moebius.backend.domain.apikeys.ApiKey
import com.moebius.backend.domain.commons.Exchange
import com.moebius.backend.domain.commons.TradeType
import com.moebius.backend.domain.orders.Order
import com.moebius.backend.domain.orders.OrderRepository
import com.moebius.backend.domain.orders.OrderStatus
import com.moebius.backend.domain.orders.OrderStatusCondition
import com.moebius.backend.dto.order.OrderDto
import com.moebius.backend.dto.order.OrderStatusDto
import com.moebius.backend.dto.trade.TradeDto
import com.moebius.backend.service.exchange.ExchangeService
import com.moebius.backend.service.exchange.ExchangeServiceFactory
import com.moebius.backend.service.member.ApiKeyService
import com.moebius.backend.service.order.factory.OrderFactoryManager
import org.springframework.http.HttpStatus
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.web.reactive.function.client.ClientResponse
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import spock.lang.Specification
import spock.lang.Subject

import java.time.LocalDateTime

class ExchangeOrderServiceTest extends Specification {
	def orderAssembler = Mock(OrderAssembler)
	def orderRepository = Mock(OrderRepository)
	def apiKeyService = Mock(ApiKeyService)
	def orderCacheService = Mock(OrderCacheService)
	def exchangeServiceFactory = Mock(ExchangeServiceFactory)
	def orderFactoryManager = Mock(OrderFactoryManager)
	def transactionalOperator = Mock(TransactionalOperator)
	def orderUtil = Mock(OrderUtil)

	@Subject
	def exchangeOrderService = new ExchangeOrderService(
			orderAssembler,
			orderRepository,
			apiKeyService,
			orderCacheService,
			exchangeServiceFactory,
			orderFactoryManager,
			transactionalOperator,
			orderUtil
	)

	def "Should request order after updating in progress status"() {
		when:
		exchangeOrderService.order(Stub(ApiKey), Stub(Order))

		then:
		1 * exchangeServiceFactory.getService(_ as Exchange) >> Stub(ExchangeService)
		1 * orderAssembler.assembleOrderStatus(_ as Order, _ as OrderStatus) >> Stub(Order)
		1 * orderRepository.save(_ as Order) >> Mono.just(Stub(Order))
	}

	def "Should not request order with trade dto when there is no ready order"() {
		given:
		1 * orderCacheService.getReadyOrderCountByExchangeAndSymbol(_ as Exchange, _ as String) >> Mono.just(0L)

		when:
		exchangeOrderService.orderWithTradeDto(buildTradeDto())

		then:
		0 * exchangeServiceFactory.getService(_ as Exchange)
		0 * apiKeyService.getApiKeyById(_ as String)
		0 * orderCacheService.evictReadyOrderCount(_ as Exchange, _ as String)
		0 * transactionalOperator.transactional(_ as Mono)
	}

	def "Should request order with trade dto"() {
		given:
		1 * orderCacheService.getReadyOrderCountByExchangeAndSymbol(_ as Exchange, _ as String) >> Mono.just(1L)

		when:
		exchangeOrderService.orderWithTradeDto(buildTradeDto())

		then:
		1 * exchangeServiceFactory.getService(_ as Exchange) >> Stub(ExchangeService)
		1 * transactionalOperator.transactional(_ as Mono) >> Mono.just(1L)
	}

	def "Should request updating order status"() {
		when:
		exchangeOrderService.updateOrderStatus(buildTradeDto())

		then:
		1 * orderAssembler.assembleInProgressStatusCondition(_ as TradeDto) >> Stub(OrderStatusCondition)
		1 * orderRepository.findAllByOrderStatusCondition(_ as OrderStatusCondition) >> Flux.just(Stub(Order), Stub(Order))
	}

	def "Should request to cancel order if needed"() {
		given:
		def exchangeService = Mock(ExchangeService)
		def orderStatusPublisher = Mono.just(Stub(OrderStatusDto))

		when:
		exchangeOrderService.cancelIfNeeded(Stub(ApiKey), Stub(OrderDto))

		then:
		1 * exchangeServiceFactory.getService(_ as Exchange) >> exchangeService
		1 * exchangeService.getCurrentOrderStatus(_ as ApiKey, _ as String) >> orderStatusPublisher
		1 * orderUtil.isOrderCancelNeeded(_ as OrderStatus, _ as OrderStatusDto) >> orderStatusPublisher
		1 * exchangeService.cancelOrder(_ as ApiKey, _ as String) >> Mono.just(ClientResponse.create(HttpStatus.OK).build())
	}

	def "Should request order"() {
		given:
		def exchangeService = Mock(ExchangeService) {
			requestOrder(_ as ApiKey, _ as Order) >> Mono.just(ClientResponse.create(HttpStatus.OK).build())
		}

		1 * apiKeyService.getApiKeyById(_ as String) >> Mono.just(Stub(ApiKey))

		expect:
		StepVerifier.create(exchangeOrderService.requestOrder(exchangeService, Stub(Order)))
				.assertNext({
					it -> it.statusCode() == HttpStatus.OK
				})
				.verifyComplete()
	}

	def "Should evict cache only if count is not 0"() {
		given:
		def count = COUNT

		EVICT_COUNT * orderCacheService.evictReadyOrderCount(_ as Exchange, _ as String)

		expect:
		StepVerifier.create(exchangeOrderService.evictIfCountNotZero(Stub(TradeDto), count))
				.assertNext({
					it -> it == RESULT
				})

		where:
		COUNT | EVICT_COUNT || RESULT
		0     | 0           || 0
		1     | 1           || 1
		2     | 1           || 2
	}

	def "Should get and update order status"() {
		given:
		def exchangeService = Mock(ExchangeService)

		1 * exchangeServiceFactory.getService(_ as Exchange) >> exchangeService
		1 * apiKeyService.getApiKeyById(_ as String) >> Mono.just(Stub(ApiKey))
		1 * exchangeService.getCurrentOrderStatus(_ as ApiKey, _ as String) >> Mono.just(Stub(OrderStatusDto))
		1 * orderAssembler.assembleOrderStatus(_ as Order, _ as OrderStatus) >> Stub(Order)
		1 * orderRepository.save(_ as Order) >> Mono.just(Stub(Order))

		expect:
		StepVerifier.create(exchangeOrderService.getAndUpdateOrderStatus(Stub(Order)))
				.assertNext({
					it -> it instanceof Order
				})
				.verifyComplete()
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
