package com.moebius.backend.dto.order;

import com.moebius.backend.domain.orders.OrderStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class OrderAssetDto {
	private String currency;
	private double averagePurchasePrice;
	private double balance;
	private double tradePrice;
	private double evaluatedPrice;
	private double profitLossRatio;
	private OrderStatus orderStatus;
}
