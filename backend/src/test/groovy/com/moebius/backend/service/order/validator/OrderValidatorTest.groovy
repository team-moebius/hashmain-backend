package com.moebius.backend.service.order.validator

import com.moebius.backend.domain.commons.EventType
import com.moebius.backend.domain.commons.Exchange
import com.moebius.backend.domain.orders.OrderPosition
import com.moebius.backend.dto.order.OrderDto
import com.moebius.backend.exception.WrongDataException
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class OrderValidatorTest extends Specification {
	@Shared
	def orderValidator = new OrderValidator()

	@Unroll
	def "Should not permit when #CONDITION"() {
		when:
		orderValidator.validate(ORDERS)

		then:
		thrown(WrongDataException.class)

		where:
		CONDITION                           | ORDERS
		"empty orders"                      | []
		"update or delete order without id" | [buildOrder(null, "KRW-BTC", EventType.UPDATE, OrderPosition.PURCHASE, 10000000D, 2D, 1)]
		"less than 1000 KRW(KRW-BTC)"       | [buildOrder(null, "KRW-BTC", EventType.CREATE, OrderPosition.PURCHASE, 990D, 1D, 1)]
		"less than 500 KRW"                 | [buildOrder(null, "KRW-ETH", EventType.CREATE, OrderPosition.PURCHASE, 490D, 1D, 1)]
		"wrong level and price in purchase" | [buildOrder(null, "KRW-BTC", EventType.CREATE, OrderPosition.PURCHASE, 500D, 3D, 1),
											   buildOrder(null, "KRW-BTC", EventType.CREATE, OrderPosition.PURCHASE, 1000D, 5D, 2)]
		"wrong level and price in sale"     | [buildOrder(null, "KRW-BTC", EventType.CREATE, OrderPosition.SALE, 1000D, 5D, 1),
											   buildOrder(null, "KRW-BTC", EventType.CREATE, OrderPosition.SALE, 500D, 3D, 2)]
		"wrong level and price in stoploss" | [buildOrder(null, "KRW-BTC", EventType.CREATE, OrderPosition.STOPLOSS, 500D, 3D, 1),
											   buildOrder(null, "KRW-BTC", EventType.CREATE, OrderPosition.STOPLOSS, 1000D, 3D, 2)]
	}

	def "Should permit orders"() {

	}

	OrderDto buildOrder(String id,
						String symbol,
						EventType eventType,
						OrderPosition orderPosition,
						double price,
						double volume,
						int level) {
		OrderDto order = new OrderDto()
		order.setId(id)
		order.setExchange(Exchange.UPBIT)
		order.setSymbol(symbol)
		order.setEventType(eventType)
		order.setOrderPosition(orderPosition)
		order.setPrice(price)
		order.setVolume(volume)
		order.setLevel(level)
		return order
	}
}
