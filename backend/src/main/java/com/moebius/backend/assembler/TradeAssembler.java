package com.moebius.backend.assembler;

import com.moebius.backend.dto.slack.TradeSlackDto;
import com.moebius.backend.dto.trade.AggregatedTradeHistoriesDto;
import com.moebius.backend.dto.trade.AggregatedTradeHistoryDto;
import com.moebius.backend.dto.trade.TradeDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.IntStream;

@Component
public class TradeAssembler {
	public TradeSlackDto assembleSlackDto(TradeDto tradeDto, AggregatedTradeHistoriesDto historiesDto) {
		List<AggregatedTradeHistoryDto> histories = historiesDto.getAggregatedTradeHistories();
		AggregatedTradeHistoryDto earliestTradeHistory = histories.get(0);

		double previousPrice = earliestTradeHistory.getTotalTransactionPrice() / earliestTradeHistory.getTotalTransactionVolume();
		double priceChangeRate = Math.round((tradeDto.getPrice() / previousPrice - 1) * 10000) / 100D;

		return TradeSlackDto.builder()
			.symbol(tradeDto.getSymbol())
			.exchange(tradeDto.getExchange())
			.totalAskVolume(histories.stream()
				.map(AggregatedTradeHistoryDto::getTotalAskVolume)
				.reduce(0D, Double::sum))
			.totalBidVolume(histories.stream()
				.map(AggregatedTradeHistoryDto::getTotalBidVolume)
				.reduce(0D, Double::sum))
			.totalValidPrice(histories.stream()
				.map(history -> history.getTotalBidPrice() - history.getTotalAskPrice())
				.reduce(0D, Double::sum))
			.price(tradeDto.getPrice())
			.priceChangeRate(priceChangeRate)
			.from(earliestTradeHistory.getStartTime().toLocalTime())
			.to(tradeDto.getCreatedAt().toLocalTime())
			.build();
	}
}
