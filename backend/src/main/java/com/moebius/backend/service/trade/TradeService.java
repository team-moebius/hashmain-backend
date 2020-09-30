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
	private static final double VALID_RISING_PRICE_CHANGE_THRESHOLD = 1.03D;
	private static final double VALID_FALLING_PRICE_CHANGE_THRESHOLD = 0.97D;

	public void identifyValidTrade(TradeDto tradeDto) {
		if (isValidTrade(tradeDto)) {
			tradeHistoryService.getAggregatedTradeHistories(tradeDto.getExchange(), tradeDto.getSymbol(), DEFAULT_TIME_INTERVAL, DEFAULT_TIME_RANGE)
				.subscribeOn(COMPUTE.scheduler())
				.filter(this::isValidTradeHistories)
				.map(histories -> tradeAssembler.assembleSlackDto(tradeDto, histories))
				.flatMap(tradeSlackSender::sendMessage)
				.subscribe();
		}
	}

	private boolean isValidTrade(TradeDto tradeDto) {
		return tradeDto.getVolume() * tradeDto.getPrice() >= TRADE_PRICE_THRESHOLD;
	}

	/**
	 * Valid trade is determined when one of these conditions below is satisfied.
	 *
	 * 1. Volume change
	 * 1-1. Heavy total transaction volume change : the last history is 5x bigger than previous average volume.
	 * 1-2. [Not applied yet] Heavy valid volume(bid - ask) change : the last history is 10x bigger than previous ones' average value,
	 * 		OR change the trade direction. (EX : bid > ask -> bid < ask)
	 *
	 * 2. Price
	 * 2-1. Heavy total transaction price : valid accumulated price is over 5M KRW
	 * 2-2. Heavy total transaction price change : the last history has greater than equal to +-3% price change than previous earliest history's price.
	 *
	 * @param historiesDto
	 * @return
	 */
	private boolean isValidTradeHistories(AggregatedTradeHistoriesDto historiesDto) {
		List<AggregatedTradeHistoryDto> histories = historiesDto.getAggregatedTradeHistories();
		if (histories.size() < HISTORY_COUNT_THRESHOLD) {
			return false;
		}

		AggregatedTradeHistoryDto latestHistory = histories.get(histories.size() - 1);
		double previousAverageVolume = IntStream.range(0, histories.size() - 1)
			.mapToDouble(index -> histories.get(index).getTotalTransactionVolume())
			.average()
			.orElse(0D);
		double earliestTradePrice = histories.get(0).getTotalTransactionPrice() / histories.get(0).getTotalTransactionVolume();

		if (isValidVolume(latestHistory, previousAverageVolume) ||
			isValidPrice(latestHistory, earliestTradePrice)) {
			log.info("[Trade] [{}/{}] The valid trade histories exist. [TTV: {}, PAV: {}, PAP: {}, PVP: {}]",
				historiesDto.getExchange(), historiesDto.getSymbol(), latestHistory.getTotalTransactionVolume(), previousAverageVolume,
				latestHistory.getTotalBidPrice() - latestHistory.getTotalAskPrice(), earliestTradePrice);
			return true;
		}
		return false;
	}

	private boolean isValidVolume(AggregatedTradeHistoryDto latestHistory, double previousAverageVolume) {
		if (previousAverageVolume == 0D) {
			return false;
		}

		return latestHistory.getTotalTransactionVolume() / previousAverageVolume >= HISTORY_VOLUME_MULTIPLIER_THRESHOLD;
	}

	private boolean isValidPrice(AggregatedTradeHistoryDto latestHistory, double earliestTradePrice) {
		if (earliestTradePrice == 0D || latestHistory.getTotalTransactionPrice() < TRADE_HISTORY_PRICE_THRESHOLD) {
			return false;
		}

		double latestTradePrice = latestHistory.getTotalTransactionPrice() / latestHistory.getTotalTransactionVolume();

		return latestTradePrice / earliestTradePrice >= VALID_RISING_PRICE_CHANGE_THRESHOLD ||
			latestTradePrice / earliestTradePrice <= VALID_FALLING_PRICE_CHANGE_THRESHOLD;
	}
}
