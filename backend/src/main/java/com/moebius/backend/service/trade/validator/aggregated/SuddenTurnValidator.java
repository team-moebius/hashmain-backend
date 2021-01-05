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
 * Common condition
 * Total transaction price : the latest history's total transaction price is greater than equal to
 * 	previous average total transaction price.
 *
 * Normal turn trades conditions (All match conditions)
 * 1. Total valid price : the histories' total valid price is greater than equal to 20M KRW.
 * 2. Unit price change : the current trade price is greater than equal to +2% or
 * 	less than equal to -2% than earliest average unit price.
 *
 * Huge turn trades conditions (Any match conditions)
 * 1. Total valid price : the histories' total valid price greater than equal to 200M KRW.
 * 2. Unit price change : the current trade price is greater than equal to +3% or
 * 	less than equal to -3% than earliest average unit price.
 *
 * @author Seonwoo Kim (Knunu)
 */
@Slf4j
@Component
public class SuddenTurnValidator implements AggregatedTradeValidator {
	private static final int HISTORY_COUNT_THRESHOLD = 2;
	private static final double TOTAL_VALID_PRICE_THRESHOLD = 20000000D;
	private static final double VALID_PRICE_RATE_CHANGE_THRESHOLD = 0.02D;
	private static final double HUGE_VALID_PRICE_THRESHOLD = 200000000D;
	private static final double HUGE_VALID_PRICE_RATE_CHANGE_THRESHOLD = 0.03D;
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

		if (isNormalTurnTrades(tradeDto, validHistoryDtos) ||
			isHugeTurnTrades(tradeDto, validHistoryDtos)) {
			log.info("[Trade] [{}/{}] The valid aggregated trade histories exist.", tradeDto.getExchange(), tradeDto.getSymbol());
			return true;
		}
		return false;
	}

	@Override
	public String getSubscribers(TradeDto tradeDto, AggregatedTradeHistoriesDto historiesDto) {
		List<AggregatedTradeHistoryDto> validHistoryDtos = getValidHistoryDtos(historiesDto);

		if (isHugeTurnTrades(tradeDto, validHistoryDtos)) {
			return String.join(StringUtils.SPACE, subscribers);
		}

		return StringUtils.EMPTY;
	}

	private List<AggregatedTradeHistoryDto> getValidHistoryDtos(AggregatedTradeHistoriesDto historiesDto) {
		return historiesDto.getAggregatedTradeHistories().stream()
			.filter(historyDto -> historyDto.getTotalTransactionVolume() > 0D)
			.collect(Collectors.toList());
	}

	private boolean isNormalTurnTrades(TradeDto tradeDto, List<AggregatedTradeHistoryDto> validHistoryDtos) {
		return hasValidTransactionPriceChange(validHistoryDtos) &&
			getTotalValidPrice(validHistoryDtos) >= TOTAL_VALID_PRICE_THRESHOLD &&
			getUnitPriceChange(tradeDto, validHistoryDtos) >= VALID_PRICE_RATE_CHANGE_THRESHOLD;
	}

	private boolean isHugeTurnTrades(TradeDto tradeDto, List<AggregatedTradeHistoryDto> validHistoryDtos) {
		return hasValidTransactionPriceChange(validHistoryDtos) &&
			(getTotalValidPrice(validHistoryDtos) >= HUGE_VALID_PRICE_THRESHOLD ||
				getUnitPriceChange(tradeDto, validHistoryDtos) >= HUGE_VALID_PRICE_RATE_CHANGE_THRESHOLD);
	}

	private boolean hasValidTransactionPriceChange(List<AggregatedTradeHistoryDto> historyDtos) {
		double previousAverageTotalTransactionPrice = IntStream.range(0, historyDtos.size() - 1)
			.mapToDouble(index -> historyDtos.get(index).getTotalTransactionPrice())
			.average()
			.orElse(1D);

		AggregatedTradeHistoryDto latestHistory = historyDtos.get(historyDtos.size() - 1);

		return latestHistory.getTotalTransactionPrice() >= previousAverageTotalTransactionPrice;
	}

	private double getTotalValidPrice(List<AggregatedTradeHistoryDto> historyDtos) {
		return Math.abs(historyDtos.stream()
			.map(history -> history.getTotalBidPrice() - history.getTotalAskPrice())
			.reduce(0D, Double::sum));
	}


	private double getUnitPriceChange(TradeDto tradeDto, List<AggregatedTradeHistoryDto> historyDtos) {
		AggregatedTradeHistoryDto latestHistory = historyDtos.get(historyDtos.size() - 2);
		double latestAverageUnitPrice = latestHistory.getTotalTransactionPrice() / latestHistory.getTotalTransactionVolume();

		return Math.abs(tradeDto.getPrice() / latestAverageUnitPrice - 1);
	}
}
