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
 * 1. Total valid price : the histories' total valid price is greater than equal to 20M KRW.
 * 2. Total transaction price : the latest history's total transaction price is greater than equal to
 * 	previous average total transaction price.
 * 3. Valid unit price change : the current trade price is greater than equal to +2% or
 * 	less than equal to -2% than earliest average unit price.
 *
 * The tremendous conditions with total valid price and valid price rate change are added.
 * If any condition of them is satisfied, then consider these trades valid.
 * 1. Total valid price : the histories' total valid price greater than equal to 500M KRW.
 * 2. Valid unit price change : the current trade price is greater than equal to +5% or
 * 	less than equal to -5% than earliest average unit price.
 *
 * @author Seonwoo Kim
 */
@Slf4j
@Component
public class SuddenTurnValidator implements AggregatedTradeValidator {
	private static final int HISTORY_COUNT_THRESHOLD = 2;
	private static final double TREMENDOUS_VALID_PRICE_THRESHOLD = 500000000D;
	private static final double TREMENDOUS_VALID_PRICE_RATE_CHANGE_THRESHOLD = 0.05D;
	private static final double TOTAL_VALID_PRICE_THRESHOLD = 20000000D;
	private static final double VALID_PRICE_RATE_CHANGE_THRESHOLD = 0.02D;
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
		List<AggregatedTradeHistoryDto> validHistoryDtos = historiesDto.getAggregatedTradeHistories().stream()
			.filter(historyDto -> historyDto.getTotalTransactionVolume() > 0D)
			.collect(Collectors.toList());
		if (validHistoryDtos.size() < HISTORY_COUNT_THRESHOLD) {
			return false;
		}

		if ((hasValidTransactionPriceChange(validHistoryDtos) &&
			getTotalValidPrice(validHistoryDtos) >= TOTAL_VALID_PRICE_THRESHOLD &&
			getValidUnitPriceChange(tradeDto, validHistoryDtos) >= VALID_PRICE_RATE_CHANGE_THRESHOLD) ||
			(getTotalValidPrice(validHistoryDtos) >= TREMENDOUS_VALID_PRICE_THRESHOLD ||
				getValidUnitPriceChange(tradeDto, validHistoryDtos) >= TREMENDOUS_VALID_PRICE_RATE_CHANGE_THRESHOLD)) {
			log.info("[Trade] [{}/{}] The valid aggregated trade histories exist.", tradeDto.getExchange(), tradeDto.getSymbol());
			return true;
		}
		return false;
	}

	@Override
	public String getSubscribers(AggregatedTradeHistoriesDto historiesDto) {
		if (Math.abs(historiesDto.getAggregatedTradeHistories().stream()
			.map(history -> history.getTotalBidPrice() - history.getTotalAskPrice())
			.reduce(0D, Double::sum)) >= TREMENDOUS_VALID_PRICE_THRESHOLD) {
			return String.join(StringUtils.SPACE, subscribers);
		}

		return StringUtils.EMPTY;
	}

	private double getTotalValidPrice(List<AggregatedTradeHistoryDto> historyDtos) {
		return Math.abs(historyDtos.stream()
			.map(history -> history.getTotalBidPrice() - history.getTotalAskPrice())
			.reduce(0D, Double::sum));
	}

	private boolean hasValidTransactionPriceChange(List<AggregatedTradeHistoryDto> historyDtos) {
		double previousAverageTotalTransactionPrice = IntStream.range(0, historyDtos.size() - 1)
			.mapToDouble(index -> historyDtos.get(index).getTotalTransactionPrice())
			.average()
			.orElse(1D);

		AggregatedTradeHistoryDto latestHistory = historyDtos.get(historyDtos.size() - 1);

		return latestHistory.getTotalTransactionPrice() >= previousAverageTotalTransactionPrice;
	}

	private double getValidUnitPriceChange(TradeDto tradeDto, List<AggregatedTradeHistoryDto> historyDtos) {
		double earliestUnitPrice = historyDtos.get(0).getTotalTransactionPrice() / historyDtos.get(0).getTotalTransactionVolume();

		return Math.abs(tradeDto.getPrice() / earliestUnitPrice - 1);
	}
}
