package com.moebius.backend.service.order.factory

import com.moebius.backend.domain.commons.Exchange
import com.moebius.backend.domain.orders.Order
import com.moebius.backend.domain.orders.OrderPosition
import com.moebius.backend.domain.orders.OrderRepository
import com.moebius.backend.dto.trade.TradeDto
import reactor.core.publisher.Flux
import reactor.test.StepVerifier
import spock.lang.Specification
import spock.lang.Subject

class StoplossOrderFactoryTest extends Specification {
	def orderRepository = Mock(OrderRepository)

	@Subject
	def stoplossOrderFactory = new StoplossOrderFactory(orderRepository)

	def "Should get position"() {
		expect:
		stoplossOrderFactory.getPosition() == OrderPosition.STOPLOSS
	}

	def "Should get and update orders to in-progress status"() {
		given:
		def firstOrder = Stub(Order)
		def secondOrder = Stub(Order)

		1 * orderRepository.findAndUpdateAllByBidCondition(_ as Exchange, _ as String, _ as OrderPosition, _ as Double) >> Flux.just(firstOrder, secondOrder)

		expect:
		StepVerifier.create(stoplossOrderFactory.getAndUpdateOrdersToInProgress(Stub(TradeDto)))
				.expectNext(firstOrder, secondOrder)
				.verifyComplete()
	}
}
