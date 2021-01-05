package com.moebius.backend.service.trade.validator.aggregated;

import com.moebius.backend.dto.trade.AggregatedTradeHistoriesDto;
import com.moebius.backend.dto.trade.AggregatedTradeHistoryDto;
import com.moebius.backend.dto.trade.TradeDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Sudden turn validator defines the valid trades by conditions below during recent 5 minutes.
 * When all the conditions are satisfied, This validator considers these trades are valid.
 *
 * 1. Total transaction volume : the latest history's total transaction volume is greater than equal to
 * 	the double (x2) of previous average total transaction volume.
 * 2. Unit price change : the current trade price is greater than equal to +1% or
 * 	less than equal to -1% than penultimate average unit price.
 *
 * When unit price change is greater than equal to +-3%, set subscribers.
 *
 * @author Seonwoo Kim (Knunu)
 */
@Slf4j
@Component
public class SuddenTurnValidator implements AggregatedTradeValidator {
	private static final int HISTORY_COUNT_THRESHOLD = 2;
	private static final int TOTAL_TRANSACTION_VOLUME_RATIO = 2;
	private static final int TOTAL_TRANSACTION_PRICE_THRESHOLD = 100000;
	private static final double UNIT_PRICE_RATE_CHANGE_THRESHOLD = 0.01D;
	private static final double SUBSCRIBED_UNIT_PRICE_RATE_CHANGE_THRESHOLD = 0.03D;
	@Value("${slack.subscribers}")
	private String[] subscribers;

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
		List<AggregatedTradeHistoryDto> validHistoryDtos = getValidHistoryDtos(historiesDto);
		if (validHistoryDtos.size() < HISTORY_COUNT_THRESHOLD) {
			return false;
		}

		if (hasValidTransactionVolumeChange(validHistoryDtos) &&
			getUnitPriceChange(tradeDto, validHistoryDtos) >= UNIT_PRICE_RATE_CHANGE_THRESHOLD) {
			log.info("[Trade] [{}/{}] The valid aggregated trade histories exist.", tradeDto.getExchange(), tradeDto.getSymbol());
			return true;
		}
		return false;
	}

	@Override
	public String getSubscribers(TradeDto tradeDto, AggregatedTradeHistoriesDto historiesDto) {
		List<AggregatedTradeHistoryDto> validHistoryDtos = getValidHistoryDtos(historiesDto);

		if (getUnitPriceChange(tradeDto, validHistoryDtos) >= SUBSCRIBED_UNIT_PRICE_RATE_CHANGE_THRESHOLD) {
			return String.join(StringUtils.SPACE, subscribers);
		}

		return StringUtils.EMPTY;
	}

	private List<AggregatedTradeHistoryDto> getValidHistoryDtos(AggregatedTradeHistoriesDto historiesDto) {
		return historiesDto.getAggregatedTradeHistories().stream()
			.filter(historyDto -> historyDto.getTotalTransactionPrice() >= TOTAL_TRANSACTION_PRICE_THRESHOLD)
			.collect(Collectors.toList());
	}

	private boolean hasValidTransactionVolumeChange(List<AggregatedTradeHistoryDto> historyDtos) {
		double previousAverageTotalTransactionVolume = IntStream.range(0, historyDtos.size() - 1)
			.mapToDouble(index -> historyDtos.get(index).getTotalTransactionVolume())
			.average()
			.orElse(1D);

		AggregatedTradeHistoryDto latestHistory = historyDtos.get(historyDtos.size() - 1);

		return latestHistory.getTotalTransactionVolume() / previousAverageTotalTransactionVolume >= TOTAL_TRANSACTION_VOLUME_RATIO;
	}

	private double getUnitPriceChange(TradeDto tradeDto, List<AggregatedTradeHistoryDto> historyDtos) {
		AggregatedTradeHistoryDto penultimateHistory = historyDtos.get(historyDtos.size() - 2);
		double penultimateAverageUnitPrice = penultimateHistory.getTotalTransactionPrice() / penultimateHistory.getTotalTransactionVolume();

		return Math.abs(tradeDto.getPrice() / penultimateAverageUnitPrice - 1);
	}
}
