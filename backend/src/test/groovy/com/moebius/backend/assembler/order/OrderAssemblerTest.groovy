package com.moebius.backend.assembler.order

import com.moebius.backend.domain.apikeys.ApiKey
import com.moebius.backend.domain.commons.EventType
import com.moebius.backend.domain.orders.Order
import com.moebius.backend.domain.orders.OrderStatus
import com.moebius.backend.domain.orders.OrderStatusCondition
import com.moebius.backend.dto.order.OrderDto
import com.moebius.backend.dto.trade.TradeDto
import com.moebius.backend.dto.frontend.response.OrderResponseDto
import org.bson.types.ObjectId
import spock.lang.Specification
import spock.lang.Subject

class OrderAssemblerTest extends Specification {
	@Subject
	def orderAssembler = new OrderAssembler()

	def "Should assemble ready order"() {
		when:
		def result = orderAssembler.assembleReadyOrder(Stub(ApiKey), Stub(OrderDto))

		then:
		result instanceof Order
		result.getOrderStatus() == OrderStatus.READY
	}

	def "Should assemble order status"() {
		given:
		def order = new Order()
		order.setOrderStatus(OrderStatus.IN_PROGRESS)

		def newStatus = OrderStatus.DONE

		when:
		def result = orderAssembler.assembleOrderStatus(order, newStatus)

		then:
		result instanceof Order
		result.getOrderStatus() == OrderStatus.DONE
	}

	def "Should assemble dto"() {
		given:
		def order = new Order()
		order.setId(ID)

		when:
		def result = orderAssembler.assembleDto(order, EventType.READ)

		then:
		result instanceof OrderDto
		result.getId() == EXPECTED_ID

		where:
		ID                                       || EXPECTED_ID
		null                                     || null
		new ObjectId("5eb3c136105fbf20a12077a1") || "5eb3c136105fbf20a12077a1"
	}

	def "Should assemble response dto"() {
		when:
		def result = orderAssembler.assembleResponseDto([Stub(OrderDto), Stub(OrderDto)])

		then:
		result instanceof OrderResponseDto
		result.getOrders() instanceof List
		result.getOrders().size() == 2
	}

	def "Should assemble in progress status condition"() {
		when:
		def result = orderAssembler.assembleInProgressStatusCondition(Stub(TradeDto))
		
		then:
		result instanceof OrderStatusCondition
		result.getOrderStatus() == OrderStatus.IN_PROGRESS
	}
}
