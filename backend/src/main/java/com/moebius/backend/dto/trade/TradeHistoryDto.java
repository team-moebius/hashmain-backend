package com.moebius.backend.dto.trade;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.moebius.backend.domain.commons.Change;
import com.moebius.backend.domain.commons.Exchange;
import com.moebius.backend.domain.commons.TradeType;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Builder(builderClassName = "TradeHistoryDtoBuilder")
@ToString
@JsonDeserialize(builder = TradeHistoryDto.TradeHistoryDtoBuilder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TradeHistoryDto {
	private Exchange exchange;
	private String symbol;
	private TradeType tradeType;
	private Change change;
	private double price;
	private double volume;
	private double prevClosingPrice;
	private double changePrice;
	@JsonSerialize(using = LocalDateTimeSerializer.class)
	@JsonDeserialize(using = LocalDateTimeDeserializer.class)
	private LocalDateTime createdAt;

	@JsonPOJOBuilder(withPrefix = "")
	public static class TradeHistoryDtoBuilder { }
}
