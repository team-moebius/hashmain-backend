package com.moebius.backend.assembler.exchange

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.moebius.backend.domain.orders.Order
import com.moebius.backend.domain.orders.OrderPosition
import com.moebius.backend.domain.orders.OrderStatus
import com.moebius.backend.domain.orders.OrderType
import com.moebius.backend.dto.order.OrderStatusDto
import com.moebius.backend.dto.exchange.upbit.UpbitOrderDto
import com.moebius.backend.dto.exchange.upbit.UpbitOrderStatusDto
import org.bson.types.ObjectId
import spock.lang.Specification
import spock.lang.Subject

class UpbitAssemblerTest extends Specification {
	def orderId = "5eb6df8abd5d1a46cfcef11b"
	def order = Stub(Order) {
		getId() >> new ObjectId(orderId)
	}
	def objectMapper = Mock(ObjectMapper)

	@Subject
	def upbitAssembler = new UpbitAssembler(objectMapper)

	def "Should assemble order"() {
		when:
		def result = upbitAssembler.assembleOrder(order)

		then:
		result instanceof UpbitOrderDto
		result.getIdentifier() == orderId
	}

	def "Should assemble order status"() {
		when:
		def result = upbitAssembler.assembleOrderStatus(orderId, Stub(UpbitOrderStatusDto))

		then:
		result instanceof OrderStatusDto
		result.getId() == orderId
	}

	def "Should assemble order parameters"() {
		given:
		1 * objectMapper.convertValue(_ as UpbitOrderDto, _ as TypeReference) >> ["identifier": orderId]

		when:
		def result = upbitAssembler.assembleOrderParameters(order)

		then:
		result instanceof String
		result.contains("identifier=5eb6df8abd5d1a46cfcef11b")
	}

	def "Should parse order position"() {
		expect:
		upbitAssembler.parseOrderPosition(ORDER_POSITION) == EXPECTED_ORDER_POSITION

		where:
		ORDER_POSITION         || EXPECTED_ORDER_POSITION
		OrderPosition.PURCHASE || "bid"
		OrderPosition.SALE     || "ask"
		OrderPosition.STOPLOSS || "ask"
	}

	def "Should parse order type"() {
		expect:
		upbitAssembler.parseOrderType(ORDER_POSITION, ORDER_TYPE) == EXPECTED_ORDER_TYPE

		where:
		ORDER_POSITION         | ORDER_TYPE       || EXPECTED_ORDER_TYPE
		OrderPosition.PURCHASE | OrderType.LIMIT  || "limit"
		OrderPosition.SALE     | OrderType.LIMIT  || "limit"
		OrderPosition.STOPLOSS | OrderType.LIMIT  || "limit"
		OrderPosition.PURCHASE | OrderType.MARKET || "price"
		OrderPosition.SALE     | OrderType.MARKET || "market"
		OrderPosition.STOPLOSS | OrderType.MARKET || "market"
	}

	def "Should parse order status"() {
		expect:
		upbitAssembler.parseOrderStatus(ORDER_STATUS) == EXPECTED_ORDER_STATUS

		where:
		ORDER_STATUS || EXPECTED_ORDER_STATUS
		null         || OrderStatus.STOPPED
		""           || OrderStatus.STOPPED
		"wait"       || OrderStatus.IN_PROGRESS
		"done"       || OrderStatus.DONE
	}

	def "Should parse order price"() {
		expect:
		upbitAssembler.parseOrderPrice(ORDER) == EXPECTED_ORDER_PRICE

		where:
		ORDER                                                || EXPECTED_ORDER_PRICE
		buildOrder(OrderType.MARKET, OrderPosition.PURCHASE) || 1000D
		buildOrder(OrderType.MARKET, OrderPosition.SALE)     || null
		buildOrder(OrderType.MARKET, OrderPosition.STOPLOSS) || null
		buildOrder(OrderType.LIMIT, OrderPosition.PURCHASE)  || 1000D
		buildOrder(OrderType.LIMIT, OrderPosition.SALE)      || 1000D
		buildOrder(OrderType.LIMIT, OrderPosition.STOPLOSS)  || 1000D
	}

	def "Should parse order volume"() {
		expect:
		upbitAssembler.parseOrderVolume(ORDER) == EXPECTED_ORDER_VOLUME

		where:
		ORDER                                                || EXPECTED_ORDER_VOLUME
		buildOrder(OrderType.MARKET, OrderPosition.PURCHASE) || null
		buildOrder(OrderType.MARKET, OrderPosition.SALE)     || 10D
		buildOrder(OrderType.MARKET, OrderPosition.STOPLOSS) || 10D
		buildOrder(OrderType.LIMIT, OrderPosition.PURCHASE)  || 10D
		buildOrder(OrderType.LIMIT, OrderPosition.SALE)      || 10D
		buildOrder(OrderType.LIMIT, OrderPosition.STOPLOSS)  || 10D
	}

	Order buildOrder(OrderType orderType, OrderPosition orderPosition) {
		Order order = new Order()
		order.setOrderType(orderType)
		order.setOrderPosition(orderPosition)
		order.setPrice(1000D)
		order.setVolume(10D)

		return order
	}
}
