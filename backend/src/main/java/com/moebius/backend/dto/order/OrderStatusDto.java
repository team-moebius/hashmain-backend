package com.moebius.backend.dto.order;

import com.moebius.backend.domain.orders.OrderStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class OrderStatusDto {
	private String id;
	private OrderStatus orderStatus;
}
