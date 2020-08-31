package com.moebius.backend.dto.slack;

import com.moebius.backend.dto.TradeDto;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class TradeSlackDto {
	private TradeDto tradeDto;
	private double totalAskVolume;
	private double totalBidVolume;
	private double updatedChangeRate;
}
