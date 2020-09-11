package com.moebius.backend.assembler;

import com.moebius.backend.dto.slack.TradeSlackDto;
import com.moebius.backend.dto.trade.AggregatedTradeHistoryDto;
import com.moebius.backend.dto.trade.TradeDto;
import org.springframework.stereotype.Component;

@Component
public class TradeAssembler {
	public TradeSlackDto assembleSlackDto(TradeDto tradeDto, AggregatedTradeHistoryDto historyDto) {
		double updatedChangeRate = Math.round(tradeDto.getPrice() / tradeDto.getPrevClosingPrice() - 1) * 100d;

		return TradeSlackDto.builder()
			.tradeDto(tradeDto)
			.totalAskVolume(historyDto.getTotalAskVolume())
			.totalBidVolume(historyDto.getTotalBidVolume())
			.updatedChangeRate(updatedChangeRate)
			.build();
	}
}
