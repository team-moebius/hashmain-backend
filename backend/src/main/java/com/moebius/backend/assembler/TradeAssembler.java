package com.moebius.backend.assembler;

import com.moebius.backend.domain.commons.TradeType;
import com.moebius.backend.dto.slack.TradeSlackDto;
import com.moebius.backend.dto.trade.AggregatedTradeHistoriesDto;
import com.moebius.backend.dto.trade.AggregatedTradeHistoryDto;
import com.moebius.backend.dto.trade.TradeDto;
import com.moebius.backend.dto.trade.TradeHistoryDto;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.IntStream;

@Component
public class TradeAssembler {
	private static final String KOREA_TIME_ZONE = "Asia/Seoul";
	private static final String UTC = "UTC";

	public TradeSlackDto assembleByAggregatedTrade(TradeDto tradeDto, AggregatedTradeHistoriesDto historiesDto, String referenceLink) {
		int historySize = historiesDto.getAggregatedTradeHistories().size();
		List<AggregatedTradeHistoryDto> historyDtos = historiesDto.getAggregatedTradeHistories().subList(0, historySize);
		AggregatedTradeHistoryDto earliestTradeHistory = historyDtos.get(0);
		AggregatedTradeHistoryDto latestTradeHistory = historyDtos.get(historyDtos.size() - 1);

		double previousAveragePrice = IntStream.range(0, historyDtos.size() - 1)
			.mapToDouble(index -> historyDtos.get(index).getTotalTransactionPrice() / historyDtos.get(index).getTotalTransactionVolume())
			.average()
			.orElse(1D);
		double priceChangeRate = (tradeDto.getPrice() / previousAveragePrice - 1) * 100;

		return TradeSlackDto.builder()
			.symbol(tradeDto.getSymbol())
			.exchange(tradeDto.getExchange())
			.totalAskPrice(historyDtos.stream()
				.map(AggregatedTradeHistoryDto::getTotalAskPrice)
				.reduce(0D, Double::sum))
			.totalBidPrice(historyDtos.stream()
				.map(AggregatedTradeHistoryDto::getTotalBidPrice)
				.reduce(0D, Double::sum))
			.totalValidPrice(historyDtos.stream()
				.map(history -> history.getTotalBidPrice() - history.getTotalAskPrice())
				.reduce(0D, Double::sum)
				.longValue())
			.price(tradeDto.getPrice())
			.priceChangeRate(priceChangeRate)
			.from(earliestTradeHistory.getStartTime().withZoneSameInstant(ZoneId.of(KOREA_TIME_ZONE)).toLocalTime())
			.to(latestTradeHistory.getEndTime().withZoneSameInstant(ZoneId.of(KOREA_TIME_ZONE)).toLocalTime())
			.referenceLink(referenceLink)
			.build();
	}

	public TradeSlackDto assembleByTrade(TradeDto tradeDto, List<TradeHistoryDto> historyDtos, String referenceLink) {
		double totalAskPrice = historyDtos.stream()
			.filter(historyDto -> historyDto.getTradeType() == TradeType.ASK)
			.mapToDouble(historyDto -> historyDto.getPrice() * historyDto.getVolume())
			.sum();
		double totalBidPrice = historyDtos.stream()
			.filter(historyDto -> historyDto.getTradeType() == TradeType.BID)
			.mapToDouble(historyDto -> historyDto.getPrice() * historyDto.getVolume())
			.sum();
		TradeHistoryDto earliestTradeHistoryDto = historyDtos.get(historyDtos.size() - 1);
		double priceChangeRate = Math.round((tradeDto.getPrice() / earliestTradeHistoryDto.getPrice() - 1) * 10000) / 100D;

		return TradeSlackDto.builder()
			.symbol(tradeDto.getSymbol())
			.exchange(tradeDto.getExchange())
			.totalAskPrice(totalAskPrice)
			.totalBidPrice(totalBidPrice)
			.totalValidPrice(Math.round(totalBidPrice - totalAskPrice))
			.price(tradeDto.getPrice())
			.priceChangeRate(priceChangeRate)
			.from(ZonedDateTime.of(earliestTradeHistoryDto.getCreatedAt(), ZoneId.of(UTC))
				.withZoneSameInstant(ZoneId.of(KOREA_TIME_ZONE)).toLocalTime().truncatedTo(ChronoUnit.SECONDS))
			.to(tradeDto.getCreatedAt().atZone(ZoneId.of(KOREA_TIME_ZONE)).toLocalTime().truncatedTo(ChronoUnit.SECONDS))
			.referenceLink(referenceLink)
			.build();
	}
}
