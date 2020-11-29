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
import java.util.stream.Collectors;

@Component
public class TradeAssembler {
	private static final String KOREA_TIME_ZONE = "Asia/Seoul";
	private static final String UTC = "UTC";

	public TradeSlackDto assembleByAggregatedTrade(TradeDto tradeDto, AggregatedTradeHistoriesDto historiesDto, String referenceLink) {
		List<AggregatedTradeHistoryDto> validHistoryDtos = historiesDto.getAggregatedTradeHistories().stream()
			.filter(historyDto -> historyDto.getTotalTransactionVolume() > 0D)
			.collect(Collectors.toList());
		double earliestAveragePrice = validHistoryDtos.get(0).getTotalTransactionPrice() / validHistoryDtos.get(0).getTotalTransactionVolume();
		double priceChangeRate = Math.round((tradeDto.getPrice() / earliestAveragePrice - 1) * 10000) / 100D;

		return TradeSlackDto.builder()
			.symbol(tradeDto.getSymbol())
			.exchange(tradeDto.getExchange())
			.totalAskPrice(validHistoryDtos.stream()
				.map(AggregatedTradeHistoryDto::getTotalAskPrice)
				.reduce(0D, Double::sum))
			.totalBidPrice(validHistoryDtos.stream()
				.map(AggregatedTradeHistoryDto::getTotalBidPrice)
				.reduce(0D, Double::sum))
			.totalValidPrice(validHistoryDtos.stream()
				.map(history -> history.getTotalBidPrice() - history.getTotalAskPrice())
				.reduce(0D, Double::sum)
				.longValue())
			.price(tradeDto.getPrice())
			.priceChangeRate(priceChangeRate)
			.from(validHistoryDtos.get(0).getStartTime().withZoneSameInstant(ZoneId.of(KOREA_TIME_ZONE)).toLocalTime())
			.to(validHistoryDtos.get(validHistoryDtos.size() - 1).getEndTime().withZoneSameInstant(ZoneId.of(KOREA_TIME_ZONE)).toLocalTime())
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
