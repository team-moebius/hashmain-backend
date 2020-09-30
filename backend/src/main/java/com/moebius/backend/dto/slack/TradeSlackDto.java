package com.moebius.backend.dto.slack;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.moebius.backend.domain.commons.Exchange;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalTime;

@Getter
@Builder
@ToString
public class TradeSlackDto {
	private String symbol;
	private Exchange exchange;
	private double totalAskVolume;
	private double totalBidVolume;
	private double totalValidPrice;
	private double price;
	private double priceChangeRate;
	@JsonSerialize(using = LocalTimeSerializer.class)
	@JsonDeserialize(using = LocalTimeDeserializer.class)
	private LocalTime from;
	@JsonSerialize(using = LocalTimeSerializer.class)
	@JsonDeserialize(using = LocalTimeDeserializer.class)
	private LocalTime to;
}
