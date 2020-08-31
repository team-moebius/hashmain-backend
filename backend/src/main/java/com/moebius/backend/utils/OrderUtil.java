package com.moebius.backend.utils;

import com.moebius.backend.domain.orders.Order;
import com.moebius.backend.domain.orders.OrderPosition;
import com.moebius.backend.domain.orders.OrderStatus;
import com.moebius.backend.domain.orders.OrderType;
import com.moebius.backend.dto.OrderDto;
import com.moebius.backend.dto.OrderStatusDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderUtil {
	private static String SEPARATOR = "-";
	private static int UNIT_CURRENCY_INDEX = 0;
	private static int TARGET_CURRENCY_INDEX = 1;

	public String getUnitCurrencyBySymbol(String symbol) {
		return symbol.split(SEPARATOR)[UNIT_CURRENCY_INDEX];
	}

	public String getTargetCurrencyBySymbol(String symbol) {
		return symbol.split(SEPARATOR)[TARGET_CURRENCY_INDEX];
	}

	public boolean isOrderRequestNeeded(Order order, double price) {
		if (order == null) {
			return false;
		}

		if (order.getOrderType() == OrderType.MARKET) {
			return true;
		}

		return order.getOrderType() == OrderType.LIMIT && (
			(order.getOrderPosition() == OrderPosition.PURCHASE && order.getPrice() >= price) ||
				(order.getOrderPosition() == OrderPosition.SALE && order.getPrice() <= price) ||
				(order.getOrderPosition() == OrderPosition.STOPLOSS && order.getPrice() >= price));
	}

	public boolean isOrderCancelNeeded(OrderStatus orderStatus, OrderStatusDto updatedOrderStatusDto) {
		return orderStatus == OrderStatus.IN_PROGRESS &&
			updatedOrderStatusDto != null &&
			orderStatus == updatedOrderStatusDto.getOrderStatus();
	}

	public List<OrderDto> filterOrdersBySymbol(List<OrderDto> orderDtos, String symbol) {
		return orderDtos.stream()
			.filter(orderDto -> orderDto.getSymbol().equals(symbol.toUpperCase()))
			.collect(Collectors.toList());
	}
}
