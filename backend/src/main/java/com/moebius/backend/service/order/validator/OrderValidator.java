package com.moebius.backend.service.order.validator;

import com.moebius.backend.domain.commons.EventType;
import com.moebius.backend.domain.commons.Exchange;
import com.moebius.backend.domain.orders.OrderPosition;
import com.moebius.backend.dto.order.OrderDto;
import com.moebius.backend.exception.WrongDataException;
import com.moebius.backend.utils.Verifier;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class OrderValidator {
	private static final String KRW_BTC = "KRW-BTC";
	private static final double UPBIT_BTC_STANDARD = 1000D;
	private static final double UPBIT_STANDARD = 500D;

	public void validate(List<OrderDto> orderDtos) {
		if (CollectionUtils.isEmpty(orderDtos)) {
			throw new WrongDataException("[Order] There isn't order to process.");
		}

		Verifier.checkNullFields(orderDtos);

		if (isInvalidUpdateOrDelete(orderDtos)) {
			throw new WrongDataException("[Order] There isn't id in UPDATE or DELETE event dto.");
		}

		if (isInvalidUpbitOrderTotalPrice(orderDtos)) {
			throw new WrongDataException("[Order] There is a wrong upbit order that total price is less than 1000 KRW(KRW-BTC) or 500 KRW.");
		}

		orderDtos.sort(Comparator.comparing(OrderDto::getLevel));

		if (isInvalidPurchase(filterByOrderPosition(orderDtos, OrderPosition.PURCHASE))) {
			throw new WrongDataException("[Order] There are wrong level and price in PURCHASE. The HIGHER LEVEL is, The LOWER PRICE should be.");
		}

		if (isInvalidSale(filterByOrderPosition(orderDtos, OrderPosition.SALE))) {
			throw new WrongDataException("[Order] There are wrong level and price in SALE. The HIGHER LEVEL is, The HIGHER PRICE should be.");
		}

		if (isInvalidStoploss(filterByOrderPosition(orderDtos, OrderPosition.STOPLOSS))) {
			throw new WrongDataException("[Order] There are wrong level and price in STOPLOSS. The HIGHER LEVEL is, The LOWER PRICE should be.");
		}
	}

	private List<OrderDto> filterByOrderPosition(List<OrderDto> orderDtos, OrderPosition orderPosition) {
		return orderDtos.stream()
			.filter(orderDto -> orderDto.getOrderPosition() == orderPosition)
			.collect(Collectors.toList());
	}

	private boolean isInvalidUpdateOrDelete(List<OrderDto> orderDtos) {
		return orderDtos.stream()
			.filter(orderDto -> Arrays.asList(EventType.UPDATE, EventType.DELETE).contains(orderDto.getEventType()))
			.anyMatch(orderDto -> StringUtils.isBlank(orderDto.getId()));
	}

	private boolean isInvalidUpbitOrderTotalPrice(List<OrderDto> orderDtos) {
		return orderDtos.stream()
			.filter(orderDto -> orderDto.getExchange() == Exchange.UPBIT)
			.anyMatch(this::isLessThanStandardPrice);
	}

	private boolean isLessThanStandardPrice(OrderDto orderDto) {
		double totalPrice = orderDto.getPrice() * orderDto.getVolume();

		if (StringUtils.equals(orderDto.getSymbol(), KRW_BTC)) {
			return totalPrice < UPBIT_BTC_STANDARD;
		}
		return totalPrice < UPBIT_STANDARD;
	}

	private boolean isInvalidPurchase(List<OrderDto> orderDtos) {
		return orderDtos.size() > 1
			&& IntStream.range(0, orderDtos.size() - 1)
			.anyMatch(index -> orderDtos.get(index).getPrice() <= orderDtos.get(index + 1).getPrice());
	}

	private boolean isInvalidSale(List<OrderDto> orderDtos) {
		return orderDtos.size() > 1
			&& IntStream.range(0, orderDtos.size() - 1)
			.anyMatch(index -> orderDtos.get(index).getPrice() >= orderDtos.get(index + 1).getPrice());
	}

	private boolean isInvalidStoploss(List<OrderDto> orderDtos) {
		return orderDtos.size() > 1
			&& IntStream.range(0, orderDtos.size() - 1)
			.anyMatch(index -> orderDtos.get(index).getPrice() <= orderDtos.get(index + 1).getPrice());
	}

}
