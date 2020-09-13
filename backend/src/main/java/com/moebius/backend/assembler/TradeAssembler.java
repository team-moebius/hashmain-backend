package com.moebius.backend.assembler;

import com.moebius.backend.dto.slack.TradeSlackDto;
import com.moebius.backend.dto.trade.AggregatedTradeHistoriesDto;
import com.moebius.backend.dto.trade.AggregatedTradeHistoryDto;
import com.moebius.backend.dto.trade.TradeDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TradeAssembler {
	public TradeSlackDto assembleSlackDto(TradeDto tradeDto, AggregatedTradeHistoriesDto historiesDto) {
		List<AggregatedTradeHistoryDto> historyDtos = historiesDto.getAggregatedTradeHistories();
		double updatedChangeRate = Math.round(tradeDto.getPrice() /
			(historyDtos.get(0).getTotalTransactionPrice() / historyDtos.get(0).getTotalTransactionVolume()) - 1) * 100d;

		return TradeSlackDto.builder()
			.tradeDto(tradeDto)
			.totalAskVolume(historyDtos.stream()
				.map(AggregatedTradeHistoryDto::getTotalAskVolume)
				.reduce(0D, Double::sum))
			.totalBidVolume(historyDtos.stream()
				.map(AggregatedTradeHistoryDto::getTotalBidVolume)
				.reduce(0D, Double::sum))
			.updatedChangeRate(updatedChangeRate)
			.build();
	}
}
