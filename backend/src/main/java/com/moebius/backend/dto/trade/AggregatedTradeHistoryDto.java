package com.moebius.backend.dto.trade;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.key.ZonedDateTimeKeyDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

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
	@JsonSerialize(using = ZonedDateTimeSerializer.class)
	private ZonedDateTime startTime;
	@JsonSerialize(using = ZonedDateTimeSerializer.class)
	private ZonedDateTime endTime;

	@JsonPOJOBuilder(withPrefix = "")
	public static class AggregatedTradeHistoryDtoBuilder {
	}
}
