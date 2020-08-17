package com.moebius.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.moebius.backend.domain.commons.Change;
import com.moebius.backend.domain.commons.Exchange;
import com.moebius.backend.domain.commons.TradeType;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class TradeHistoryDto {
	private Exchange exchange;
	private String symbol;
	private TradeType tradeType;
	private Change change;
	private double price;
	private double volume;
	private double prevClosingPrice;
	private LocalDateTime createdAt;
}
