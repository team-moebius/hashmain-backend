package com.moebius.backend.assembler;

import com.moebius.backend.dto.TradeDto;
import com.moebius.backend.dto.slack.TradeSlackDto;
import org.springframework.stereotype.Component;

@Component
public class TradeAssembler {
	public TradeSlackDto assembleSlackDto(TradeDto tradeDto, double updatedChangeRate) {
		return TradeSlackDto.builder()
			.tradeDto(tradeDto)
			.updatedChangeRate(updatedChangeRate)
			.build();
	}
}
