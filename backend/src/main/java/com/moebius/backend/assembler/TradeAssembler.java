package com.moebius.backend.assembler;

import com.moebius.backend.dto.slack.TradeSlackDto;
import com.moebius.backend.dto.trade.AggregatedTradeHistoriesDto;
import com.moebius.backend.dto.trade.AggregatedTradeHistoryDto;
import com.moebius.backend.dto.trade.TradeDto;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.List;

@Component
public class TradeAssembler {
	public TradeSlackDto assembleSlackDto(TradeDto tradeDto, AggregatedTradeHistoriesDto historiesDto) {
		List<AggregatedTradeHistoryDto> historyDtos = historiesDto.getAggregatedTradeHistories();
		AggregatedTradeHistoryDto earliestTradeHistory = historyDtos.get(0);
		AggregatedTradeHistoryDto latestTradeHistory = historyDtos.get(historyDtos.size() - 1);

		double earliestTradePrice = earliestTradeHistory.getTotalTransactionPrice() / earliestTradeHistory.getTotalTransactionVolume();
		double priceChangeRate = Math.round((tradeDto.getPrice() / earliestTradePrice - 1) * 10000) / 100D;

		return TradeSlackDto.builder()
			.symbol(tradeDto.getSymbol())
			.exchange(tradeDto.getExchange())
			.totalAskVolume(historyDtos.stream()
				.map(AggregatedTradeHistoryDto::getTotalAskVolume)
				.reduce(0D, Double::sum))
			.totalBidVolume(historyDtos.stream()
				.map(AggregatedTradeHistoryDto::getTotalBidVolume)
				.reduce(0D, Double::sum))
			.totalValidPrice(historyDtos.stream()
				.map(history -> history.getTotalBidPrice() - history.getTotalAskPrice())
				.reduce(0D, Double::sum)
				.intValue())
			.price(tradeDto.getPrice())
			.priceChangeRate(priceChangeRate)
			.from(earliestTradeHistory.getStartTime().withZoneSameInstant(ZoneId.of("Asia/Seoul")).toLocalTime())
			.to(latestTradeHistory.getEndTime().withZoneSameInstant(ZoneId.of("Asia/Seoul")).toLocalTime())
			.build();
	}
}
