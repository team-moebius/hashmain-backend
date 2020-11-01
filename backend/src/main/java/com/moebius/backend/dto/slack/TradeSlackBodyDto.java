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
	private double totalAskVolume;
	private double totalBidVolume;
	private int totalValidPrice;
	private double price;
	private double priceChangeRate;
	private String unitCurrency;
	private String targetCurrency;
	//Date from to
	private String from;
	private String to;
	private String referenceLink;
}
