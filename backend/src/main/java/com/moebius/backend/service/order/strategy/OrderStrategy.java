package com.moebius.backend.service.order.strategy;

import com.moebius.backend.dto.order.OrderDto;

import java.util.List;

public interface OrderStrategy {
	void order(List<OrderDto> orderDtos);
}
