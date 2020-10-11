package com.moebius.backend.service.trade.strategy;

import com.moebius.backend.domain.commons.TradeType;
import com.moebius.backend.dto.trade.AggregatedTradeHistoriesDto;
import com.moebius.backend.dto.trade.AggregatedTradeHistoryDto;
import com.moebius.backend.dto.trade.TradeDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Short term (5 minutes ago ~ now) strategy defines the valid trades by conditions below.
 * When all the conditions are satisfied, This strategy considers these trades are valid.
 *
 * 1. Total transaction volume change : the latest history's total transaction volume is 10x bigger than previous average volume during 5 minutes.
 * 2. Total valid price : the total valid price is over 10M KRW or under -10M KRW during 5 minutes.
 * 3. Total valid price change: the current trade price increases greater than equal to +1%,
 * 								or decreases less than equal to -1% than previous average price during 5 minutes.
 *
 * @author Seonwoo Kim
 */
@Slf4j
@Component
public class ShortTermStrategy implements TradeStrategy {
	private static final int HISTORY_COUNT_THRESHOLD = 2;
	private static final int VALID_LATEST_HISTORY_CORRECTOR = 2;
	private static final double TRADE_HISTORY_PRICE_THRESHOLD = 10000000D;
	private static final double HISTORY_VOLUME_MULTIPLIER_THRESHOLD = 10D;
	private static final double VALID_RISING_PRICE_CHANGE_THRESHOLD = 1.01D;
	private static final double VALID_FALLING_PRICE_CHANGE_THRESHOLD = 0.99D;

	@Override
	public int getTimeInterval() {
		return 1;
	}

	@Override
	public int getTimeRange() {
		return 6;
	}

	@Override
	public boolean isValid(TradeDto tradeDto, AggregatedTradeHistoriesDto historiesDto) {
		List<AggregatedTradeHistoryDto> historyDtos = historiesDto.getAggregatedTradeHistories();
		if (historyDtos.size() < HISTORY_COUNT_THRESHOLD) {
			return false;
		}

		if (isValidVolume(historyDtos) &&
			isValidPrice(tradeDto, historyDtos)) {
			log.info("[Trade] [{}/{}] The valid aggregated trade histories exist.",
				tradeDto.getExchange(), tradeDto.getSymbol());
			return true;
		}
		return false;
	}

	private boolean isValidVolume(List<AggregatedTradeHistoryDto> historyDtos) {
		double previousAverageVolume = IntStream.range(0, historyDtos.size() - VALID_LATEST_HISTORY_CORRECTOR)
			.mapToDouble(index -> historyDtos.get(index).getTotalTransactionVolume())
			.average()
			.orElse(0D);

		if (previousAverageVolume == 0D) {
			return false;
		}

		AggregatedTradeHistoryDto latestHistory = historyDtos.get(historyDtos.size() - VALID_LATEST_HISTORY_CORRECTOR);

		return latestHistory.getTotalTransactionVolume() / previousAverageVolume >= HISTORY_VOLUME_MULTIPLIER_THRESHOLD;
	}

	private boolean isValidPrice(TradeDto tradeDto, List<AggregatedTradeHistoryDto> historyDtos) {
		double previousAveragePrice = IntStream.range(0, historyDtos.size() - VALID_LATEST_HISTORY_CORRECTOR)
			.mapToDouble(index -> historyDtos.get(index).getTotalTransactionPrice() / historyDtos.get(index).getTotalTransactionVolume())
			.average()
			.orElse(0D);

		double totalValidPrice = historyDtos.stream()
			.map(history -> history.getTotalBidPrice() - history.getTotalAskPrice())
			.reduce(0D, Double::sum);

		if (previousAveragePrice == 0D ||
			(totalValidPrice < TRADE_HISTORY_PRICE_THRESHOLD && totalValidPrice > -TRADE_HISTORY_PRICE_THRESHOLD)) {
			return false;
		}

		return tradeDto.getPrice() / previousAveragePrice >= VALID_RISING_PRICE_CHANGE_THRESHOLD ||
			tradeDto.getPrice() / previousAveragePrice <= VALID_FALLING_PRICE_CHANGE_THRESHOLD;
	}
}
