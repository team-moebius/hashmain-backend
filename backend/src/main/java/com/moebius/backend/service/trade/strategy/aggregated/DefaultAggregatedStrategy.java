package com.moebius.backend.service.trade.strategy.aggregated;

import com.moebius.backend.dto.trade.AggregatedTradeHistoriesDto;
import com.moebius.backend.dto.trade.AggregatedTradeHistoryDto;
import com.moebius.backend.dto.trade.TradeDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Default aggregated strategy defines the valid trades by conditions below during recent 5 minutes.
 * When all the conditions are satisfied, This strategy considers these trades are valid.
 *
 * 1. Total transaction price change : the latest history's total transaction price is greater than equal to 10M KRW,
 * 								10x bigger than previous average total transaction price during 5 minutes.
 * 2. Valid price change rate change: the current trade price increases greater than equal to +3%,
 * 								or decreases less than equal to -3% than previous average price during 5 minutes.
 *
 *
 * @author Seonwoo Kim
 */
@Slf4j
@Component
public class DefaultAggregatedStrategy implements AggregatedTradeStrategy {
	private static final int HISTORY_COUNT_THRESHOLD = 2;
	private static final double VALID_PRICE_RATE_CHANGE_THRESHOLD = 0.03D;
	private static final double TOTAL_TRANSACTION_PRICE_THRESHOLD = 10000000D;
	private static final double TOTAL_TRANSACTION_PRICE_MULTIPLIER_THRESHOLD = 10D;

	@Override
	public int getTimeInterval() {
		return 1;
	}

	@Override
	public int getTimeRange() {
		return 5;
	}

	@Override
	public boolean isValid(TradeDto tradeDto, AggregatedTradeHistoriesDto historiesDto) {
		List<AggregatedTradeHistoryDto> validHistoryDtos = historiesDto.getAggregatedTradeHistories().stream()
			.filter(historyDto -> historyDto.getTotalTransactionVolume() > 0D)
			.collect(Collectors.toList());
		if (validHistoryDtos.size() < HISTORY_COUNT_THRESHOLD) {
			return false;
		}

		if (hasValidPriceChange(validHistoryDtos) &&
			hasValidPriceRateChange(tradeDto, validHistoryDtos)) {
			log.info("[Trade] [{}/{}] The valid aggregated trade histories exist.", tradeDto.getExchange(), tradeDto.getSymbol());
			return true;
		}
		return false;
	}

	private boolean hasValidPriceChange(List<AggregatedTradeHistoryDto> historyDtos) {
		double previousAveragePrice = IntStream.range(0, historyDtos.size() - 1)
			.mapToDouble(index -> historyDtos.get(index).getTotalTransactionPrice())
			.average()
			.orElse(1D);

		AggregatedTradeHistoryDto latestHistory = historyDtos.get(historyDtos.size() - 1);

		return latestHistory.getTotalTransactionPrice() >= TOTAL_TRANSACTION_PRICE_THRESHOLD &&
			latestHistory.getTotalTransactionPrice() / previousAveragePrice >= TOTAL_TRANSACTION_PRICE_MULTIPLIER_THRESHOLD;
	}

	private boolean hasValidPriceRateChange(TradeDto tradeDto, List<AggregatedTradeHistoryDto> historyDtos) {
		double previousAveragePrice = IntStream.range(0, historyDtos.size() - 1)
			.mapToDouble(index -> historyDtos.get(index).getTotalTransactionPrice() / historyDtos.get(index).getTotalTransactionVolume())
			.average()
			.orElse(1D);

		return Math.abs(tradeDto.getPrice() / previousAveragePrice - 1) >= VALID_PRICE_RATE_CHANGE_THRESHOLD;
	}
}
