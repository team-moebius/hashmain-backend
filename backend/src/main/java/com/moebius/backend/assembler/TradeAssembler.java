package com.moebius.backend.assembler;

import com.moebius.backend.dto.slack.TradeSlackDto;
import com.moebius.backend.dto.trade.AggregatedTradeHistoriesDto;
import com.moebius.backend.dto.trade.AggregatedTradeHistoryDto;
import com.moebius.backend.dto.trade.TradeDto;
import com.moebius.backend.dto.trade.TradeHistoryDto;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.List;

@Component
public class TradeAssembler {
	public TradeSlackDto assembleByAggregatedTrade(TradeDto tradeDto, AggregatedTradeHistoriesDto historiesDto, String referenceLink) {
		int historySize = historiesDto.getAggregatedTradeHistories().size();
		List<AggregatedTradeHistoryDto> historyDtos = historiesDto.getAggregatedTradeHistories().subList(0, historySize);
		AggregatedTradeHistoryDto earliestTradeHistory = historyDtos.get(0);
		AggregatedTradeHistoryDto latestTradeHistory = historyDtos.get(historyDtos.size() - 1);

		double earliestTradePrice = tradeDto.getPrevClosingPrice();
		if (earliestTradeHistory.getTotalTransactionVolume() != 0) {
			earliestTradePrice = earliestTradeHistory.getTotalTransactionPrice() / earliestTradeHistory.getTotalTransactionVolume();
		}
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
			.referenceLink(referenceLink)
			.build();
	}

	public TradeSlackDto assembleByTrade(TradeDto tradeDto, List<TradeHistoryDto> historyDtos) {
		return TradeSlackDto.builder().build(); // FIXME
	}
}
