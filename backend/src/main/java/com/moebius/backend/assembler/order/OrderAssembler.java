package com.moebius.backend.assembler.order;

import com.moebius.backend.domain.apikeys.ApiKey;
import com.moebius.backend.domain.commons.EventType;
import com.moebius.backend.domain.orders.Order;
import com.moebius.backend.domain.orders.OrderStatus;
import com.moebius.backend.domain.orders.OrderStatusCondition;
import com.moebius.backend.dto.order.OrderDto;
import com.moebius.backend.dto.trade.TradeDto;
import com.moebius.backend.dto.frontend.response.OrderResponseDto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Component
public class OrderAssembler {
	public Order assembleReadyOrder(ApiKey apiKey, OrderDto dto) {
		Order order = new Order();
		order.setApiKeyId(apiKey.getId());
		order.setExchange(dto.getExchange());
		order.setSymbol(dto.getSymbol());
		order.setOrderType(dto.getOrderType());
		order.setOrderStatus(OrderStatus.READY);
		order.setOrderPosition(dto.getOrderPosition());
		order.setPrice(dto.getPrice());
		order.setVolume(dto.getVolume());
		order.setLevel(dto.getLevel());
		order.setCreatedAt(LocalDateTime.now());
		order.setUpdatedAt(LocalDateTime.now());

		return order;
	}

	public Order assembleOrderStatus(Order order, OrderStatus orderStatus) {
		order.setOrderStatus(orderStatus);
		order.setUpdatedAt(LocalDateTime.now());

		return order;
	}

	public OrderDto assembleDto(Order order, EventType eventType) {
		OrderDto orderDto = new OrderDto();
		if (Objects.nonNull(order.getId())) {
			orderDto.setId(order.getId().toHexString());
		}
		orderDto.setEventType(eventType);
		orderDto.setExchange(order.getExchange());
		orderDto.setSymbol(order.getSymbol());
		orderDto.setOrderType(order.getOrderType());
		orderDto.setOrderStatus(order.getOrderStatus());
		orderDto.setOrderPosition(order.getOrderPosition());
		orderDto.setPrice(order.getPrice());
		orderDto.setVolume(order.getVolume());
		orderDto.setLevel(order.getLevel());

		return orderDto;
	}

	public OrderResponseDto assembleResponseDto(List<OrderDto> orders) {
		return OrderResponseDto.builder()
			.orders(orders)
			.build();
	}

	public OrderStatusCondition assembleInProgressStatusCondition(TradeDto tradeDto) {
		return OrderStatusCondition.builder()
			.exchange(tradeDto.getExchange())
			.symbol(tradeDto.getSymbol())
			.orderStatus(OrderStatus.IN_PROGRESS)
			.build();
	}
}
