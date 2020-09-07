package com.moebius.backend.assembler.exchange;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moebius.backend.domain.orders.Order;
import com.moebius.backend.domain.orders.OrderPosition;
import com.moebius.backend.domain.orders.OrderStatus;
import com.moebius.backend.domain.orders.OrderType;
import com.moebius.backend.dto.order.OrderStatusDto;
import com.moebius.backend.dto.exchange.upbit.UpbitOrderDto;
import com.moebius.backend.dto.exchange.upbit.UpbitOrderStatusDto;
import com.moebius.backend.utils.Verifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpbitAssembler implements ExchangeAssembler {
	private static final String ORDER_POSITION_BID = "bid";
	private static final String ORDER_POSITION_ASK = "ask";
	private static final String ORDER_TYPE_LIMIT = "limit";
	private static final String ORDER_TYPE_PRICE = "price";
	private static final String ORDER_TYPE_MARKET = "market";
	private static final String WAIT_STATE = "wait";

	private final ObjectMapper objectMapper;

	public UpbitOrderDto assembleOrder(Order order) {
		return UpbitOrderDto.builder()
			.identifier(order.getId().toHexString())
			.market(order.getSymbol())
			.side(parseOrderPosition(order.getOrderPosition()))
			.ord_type(parseOrderType(order.getOrderPosition(), order.getOrderType()))
			.price(parseOrderPrice(order))
			.volume(parseOrderVolume(order))
			.build();
	}

	public OrderStatusDto assembleOrderStatus(String orderId, UpbitOrderStatusDto upbitOrderStatusDto) {
		Verifier.checkNullFields(upbitOrderStatusDto);

		return OrderStatusDto.builder()
			.id(orderId)
			.orderStatus(parseOrderStatus(upbitOrderStatusDto.getState()))
			.build();
	}

	public String assembleOrderParameters(Order order) {
		UpbitOrderDto orderDto = assembleOrder(order);

		Map<String, String> parameters = objectMapper.convertValue(orderDto, new TypeReference<Map<String, String>>() {});

		return parameters.entrySet().stream()
			.map(entry -> entry.getKey() + "=" + entry.getValue())
			.collect(Collectors.joining("&"));
	}

	private String parseOrderPosition(OrderPosition orderPosition) {
		if (orderPosition == OrderPosition.PURCHASE) {
			return ORDER_POSITION_BID;
		}
		return ORDER_POSITION_ASK;
	}

	private String parseOrderType(OrderPosition orderPosition, OrderType orderType) {
		if (orderType == OrderType.MARKET) {
			if (orderPosition == OrderPosition.PURCHASE) {
				return ORDER_TYPE_PRICE; // 시장가 매수
			}
			return ORDER_TYPE_MARKET; // 시장가 매도
		}
		return ORDER_TYPE_LIMIT; // 지정가 매수, 매도
	}

	private OrderStatus parseOrderStatus(String state) {
		if (StringUtils.isEmpty(state)) {
			return OrderStatus.STOPPED;
		}

		if (WAIT_STATE.equals(state)) {
			return OrderStatus.IN_PROGRESS;
		}

		return OrderStatus.DONE;
	}

	private Double parseOrderPrice(Order order) {
		if (order.getOrderType() == OrderType.MARKET
			&& order.getOrderPosition() != OrderPosition.PURCHASE) {
			return null; // 시장가 매도일 경우 null
		}
		return order.getPrice();
	}

	private Double parseOrderVolume(Order order) {
		if (order.getOrderType() == OrderType.MARKET
			&& order.getOrderPosition() == OrderPosition.PURCHASE) {
			return null; // 시장가 매수일 경우 null
		}
		return order.getVolume();
	}
}
