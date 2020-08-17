package com.moebius.backend.dto.trade;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.moebius.backend.domain.commons.Exchange;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class AggregatedTradeHistoryDto {
	private Exchange exchange;
	private String symbol;
	private int totalAskCount;
	private double totalAskPrice;
	private double totalAskVolume;
	private int totalBidCount;
	private double totalBidPrice;
	private double totalBidVolume;
	@JsonSerialize(using = LocalDateTimeSerializer.class)
	@JsonDeserialize(using = LocalDateTimeDeserializer.class)
	private LocalDateTime startAt;
	@JsonSerialize(using = LocalDateTimeSerializer.class)
	@JsonDeserialize(using = LocalDateTimeDeserializer.class)
	private LocalDateTime endAt;
}
