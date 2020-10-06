package com.moebius.backend.service.trade;

import com.moebius.backend.assembler.TradeAssembler;
import com.moebius.backend.dto.trade.AggregatedTradeHistoriesDto;
import com.moebius.backend.dto.trade.AggregatedTradeHistoryDto;
import com.moebius.backend.dto.trade.TradeDto;
import com.moebius.backend.service.slack.TradeSlackSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.IntStream;

import static com.moebius.backend.utils.ThreadScheduler.COMPUTE;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeService {
	private final TradeHistoryService tradeHistoryService;
	private final TradeSlackSender tradeSlackSender;
	private final TradeAssembler tradeAssembler;
	private static final int DEFAULT_TIME_INTERVAL = 1;
	private static final int DEFAULT_TIME_RANGE = 5;
	private static final int HISTORY_COUNT_THRESHOLD = 2;
	private static final double TRADE_PRICE_THRESHOLD = 10000D;
	private static final double TRADE_HISTORY_PRICE_THRESHOLD = 5000000D;
	private static final double HISTORY_VOLUME_MULTIPLIER_THRESHOLD = 5D;
	private static final double VALID_RISING_PRICE_CHANGE_THRESHOLD = 1.01D;
	private static final double VALID_FALLING_PRICE_CHANGE_THRESHOLD = 0.99D;

	public void identifyValidTrade(TradeDto tradeDto) {
		if (isTradeOverPriceThreshold(tradeDto)) {
			String uri = tradeHistoryService.getAggregatedTradeHistoriesUri(tradeDto, DEFAULT_TIME_INTERVAL, DEFAULT_TIME_RANGE);

			tradeHistoryService.getAggregatedTradeHistories(uri)
				.subscribeOn(COMPUTE.scheduler())
				.filter(historiesDto -> isValidTrade(tradeDto, historiesDto))
				.map(historiesDto -> tradeAssembler.assembleSlackDto(tradeDto, historiesDto, uri))
				.flatMap(tradeSlackSender::sendMessage)
				.subscribe();
		}
	}

	private boolean isTradeOverPriceThreshold(TradeDto tradeDto) {
		return tradeDto.getVolume() * tradeDto.getPrice() >= TRADE_PRICE_THRESHOLD;
	}

	/**
	 * Valid trade is determined when all of these conditions below are satisfied.
	 *
	 * 1. Volume change
	 * 1-1. Heavy total transaction volume change : the last history is 5x bigger than previous average volume during 5 minutes.
	 * 1-2. [Not applied yet] Heavy valid volume(bid - ask) change : the last history is 5x bigger than previous ones' average value,
	 * 		OR change the trade direction. (EX : bid > ask -> bid < ask)
	 *
	 * 2. Price
	 * 2-1. Heavy total transaction price : valid total price during 5 minutes is over 5M KRW or under -5M KRW
	 * 2-2. Heavy total transaction price change : the last history has greater than equal to +1% or less than equal to -1% price change than previous earliest history's price.
	 *
	 * @param tradeDto
	 * @param historiesDto
	 * @return
	 */
	private boolean isValidTrade(TradeDto tradeDto, AggregatedTradeHistoriesDto historiesDto) {
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
		double previousAverageVolume = IntStream.range(0, historyDtos.size() - 1)
			.mapToDouble(index -> historyDtos.get(index).getTotalTransactionVolume())
			.average()
			.orElse(0D);
		if (previousAverageVolume == 0D) {
			return false;
		}

		AggregatedTradeHistoryDto latestHistory = historyDtos.get(historyDtos.size() - 1);

		return latestHistory.getTotalTransactionVolume() / previousAverageVolume >= HISTORY_VOLUME_MULTIPLIER_THRESHOLD;
	}

	private boolean isValidPrice(TradeDto tradeDto, List<AggregatedTradeHistoryDto> historyDtos) {
		double earliestTradePrice = historyDtos.get(0).getTotalTransactionPrice() / historyDtos.get(0).getTotalTransactionVolume();
		double totalValidPrice = historyDtos.stream()
			.map(history -> history.getTotalBidPrice() - history.getTotalAskPrice())
			.reduce(0D, Double::sum);

		if (earliestTradePrice == 0D ||
			(totalValidPrice < TRADE_HISTORY_PRICE_THRESHOLD && totalValidPrice > -TRADE_HISTORY_PRICE_THRESHOLD)) {
			return false;
		}

		return tradeDto.getPrice() / earliestTradePrice >= VALID_RISING_PRICE_CHANGE_THRESHOLD ||
			tradeDto.getPrice() / earliestTradePrice <= VALID_FALLING_PRICE_CHANGE_THRESHOLD;
	}
}
