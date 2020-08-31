package com.moebius.backend.utils

import com.moebius.backend.domain.orders.Order
import com.moebius.backend.domain.orders.OrderPosition
import com.moebius.backend.domain.orders.OrderType
import com.moebius.backend.dto.OrderDto
import com.moebius.backend.dto.OrderStatusDto
import spock.lang.Specification
import spock.lang.Subject

import static com.moebius.backend.domain.orders.OrderPosition.*
import static com.moebius.backend.domain.orders.OrderStatus.*
import static com.moebius.backend.domain.orders.OrderType.LIMIT
import static com.moebius.backend.domain.orders.OrderType.MARKET

class OrderUtilTest extends Specification {
	@Subject
	def orderUtil = new OrderUtil()

	def "Should get currency by symbol"() {
		expect:
		orderUtil.getTargetCurrencyBySymbol("KRW-BTC") == "BTC"
	}

	def "Should check order request needed"() {
		expect:
		orderUtil.isOrderRequestNeeded(ORDER, PRICE) == RESULT

		where:
		ORDER                           | PRICE || RESULT
		null                            | 0D    || false
		buildOrder(MARKET, null, 0D)    | 0D    || true
		buildOrder(LIMIT, PURCHASE, 2D) | 1D    || true
		buildOrder(LIMIT, PURCHASE, 2D) | 2D    || true
		buildOrder(LIMIT, PURCHASE, 2D) | 3D    || false
		buildOrder(LIMIT, SALE, 2D)     | 1D    || false
		buildOrder(LIMIT, SALE, 2D)     | 2D    || true
		buildOrder(LIMIT, SALE, 2D)     | 3D    || true
		buildOrder(LIMIT, STOPLOSS, 2D) | 1D    || true
		buildOrder(LIMIT, STOPLOSS, 2D) | 2D    || true
		buildOrder(LIMIT, STOPLOSS, 2D) | 3D    || false
	}

	def "Should check order cancel needed"() {
		expect:
		orderUtil.isOrderCancelNeeded(ORDER_STATUS, ORDER_STATUS_DTO) == RESULT

		where:
		ORDER_STATUS | ORDER_STATUS_DTO                                         || RESULT
		READY        | null                                                     || false
		DONE         | null                                                     || false
		STOPPED      | null                                                     || false
		IN_PROGRESS  | null                                                     || false
		IN_PROGRESS  | Stub(OrderStatusDto) { getOrderStatus() >> READY }       || false
		IN_PROGRESS  | Stub(OrderStatusDto) { getOrderStatus() >> IN_PROGRESS } || true
	}

	def "Should filter orders by symbol"() {
		given:
		def orderDtos = [buildOrderDto("KRW-BTC"), buildOrderDto("KRW-ETH")]

		when:
		def result = orderUtil.filterOrdersBySymbol(orderDtos, "KRW-BTC")

		then:
		result instanceof List
		result.size() == 1
		result.get(0) instanceof OrderDto
		result.get(0).getSymbol() == "KRW-BTC"
	}

	Order buildOrder(OrderType orderType, OrderPosition orderPosition, double price) {
		Order order = new Order()
		order.setOrderType(orderType)
		order.setOrderPosition(orderPosition)
		order.setPrice(price)

		return order
	}

	OrderDto buildOrderDto(String symbol) {
		OrderDto orderDto = new OrderDto()
		orderDto.setSymbol(symbol)

		return orderDto
	}
}
