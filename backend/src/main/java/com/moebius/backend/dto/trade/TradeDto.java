package com.moebius.backend.dto.trade;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.moebius.backend.domain.commons.Change;
import com.moebius.backend.domain.commons.Exchange;
import com.moebius.backend.domain.commons.TradeType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class TradeDto {
	@Nullable
	private String id;
	private Exchange exchange;
	private String symbol;
	private TradeType tradeType;
	@Nullable
	private Change change;
	private double price;
	private double volume;
	private double prevClosingPrice;
	private double changePrice;
	@JsonSerialize(using = LocalDateTimeSerializer.class)
	@JsonDeserialize(using = LocalDateTimeDeserializer.class)
	private LocalDateTime createdAt;
	private long receivedTime;
}
