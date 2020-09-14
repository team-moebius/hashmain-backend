package com.moebius.backend.dto.trade;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Builder(builderClassName = "AggregatedTradeHistoryDtoBuilder")
@ToString
@JsonDeserialize(builder = AggregatedTradeHistoryDto.AggregatedTradeHistoryDtoBuilder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AggregatedTradeHistoryDto {
	private long totalAskCount;
	private double totalAskPrice;
	private double totalAskVolume;
	private long totalBidCount;
	private double totalBidPrice;
	private double totalBidVolume;
	private long totalTransactionCount;
	private double totalTransactionPrice;
	private double totalTransactionVolume;
	@JsonSerialize(using = LocalDateTimeSerializer.class)
	@JsonDeserialize(using = LocalDateTimeDeserializer.class)
	private LocalDateTime startTime;
	@JsonSerialize(using = LocalDateTimeSerializer.class)
	@JsonDeserialize(using = LocalDateTimeDeserializer.class)
	private LocalDateTime endTime;

	@JsonPOJOBuilder(withPrefix = "")
	public static class AggregatedTradeHistoryDtoBuilder {
	}
}
