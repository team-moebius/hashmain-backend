package com.moebius.backend.assembler;

import com.moebius.backend.dto.slack.SlackMessageDto;
import com.moebius.backend.dto.TradeDto;
import org.springframework.stereotype.Component;

@Component
public class SlackAssembler {
	public SlackMessageDto assemble(TradeDto tradeDto, double changeRate) {
		return SlackMessageDto.builder()

			.build();
	}
}
