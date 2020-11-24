package com.moebius.backend.dto.slack;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class TradeSlackBodyDto {
	private String symbol;
	private String exchange;
	private String totalAskPrice;
	private String totalBidPrice;
	private String totalValidPrice;
	private String price;
	private String priceChangeRate;
	private String unitCurrency;
	private String targetCurrency;
	//Date from to
	private String from;
	private String to;
	private String referenceLink;
	private String color;
}
