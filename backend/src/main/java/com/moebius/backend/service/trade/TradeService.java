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
	private static final double HISTORY_VOLUME_MULTIPLIER_THRESHOLD = 10D;
	private static final double VALID_RISING_PRICE_CHANGE_THRESHOLD = 1.01D;
	private static final double VALID_FALLING_PRICE_CHANGE_THRESHOLD = 0.99D;

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
	 * Valid trade conditions are based on
	 * 1. Volume change
	 * 1-1. Heavy total transaction volume change : the last history is 5x bigger than previous ones' average volume.
	 * 1-2. [Optional] Heavy valid volume(bid - ask) change : the last history is 10x bigger than previous ones' average value,
	 * 		OR change the trade direction. (EX : bid > ask -> bid < ask)
	 *
	 * 2. Price
	 * 2-1. Heavy total transaction price change : the last history has over +-1% price change than previous ones' average price.
	 *
	 * @param historiesDto
	 * @return
	 */
	private boolean isValidTradeHistories(AggregatedTradeHistoriesDto historiesDto) {
		List<AggregatedTradeHistoryDto> histories = historiesDto.getAggregatedTradeHistories();
		if (histories.size() < HISTORY_COUNT_THRESHOLD) {
			return false;
		}

		AggregatedTradeHistoryDto lastHistory = histories.get(histories.size() - 1);
		double previousAverageVolume = IntStream.range(0, histories.size() - 1)
			.mapToDouble(index -> histories.get(index).getTotalTransactionVolume())
			.average()
			.orElse(0D);

		double previousAveragePrice = IntStream.range(0, histories.size() - 1)
			.mapToDouble(index -> histories.get(index).getTotalTransactionPrice() / histories.get(index).getTotalTransactionVolume())
			.average()
			.orElse(0D);

		if (previousAverageVolume == 0D || previousAveragePrice == 0D) {
			return false;
		}

		if (isValidVolume(lastHistory, previousAverageVolume) &&
			isValidPrice(lastHistory, previousAveragePrice)) {
			log.info("[Trade] [{}/{}] The valid trade histories exist. [TTV: {}, PAV: {}, PAP: {}, PVP: {}]",
				historiesDto.getExchange(), historiesDto.getSymbol(), lastHistory.getTotalTransactionVolume(), previousAverageVolume,
				lastHistory.getTotalBidPrice() - lastHistory.getTotalAskPrice(), previousAveragePrice);
			return true;
		}
		return false;
	}

	private boolean isValidVolume(AggregatedTradeHistoryDto lastHistory, double previousAverageVolume) {
		return lastHistory.getTotalTransactionVolume() / previousAverageVolume >= HISTORY_VOLUME_MULTIPLIER_THRESHOLD;
	}

	private boolean isValidPrice(AggregatedTradeHistoryDto lastHistory, double previousAveragePrice) {
		double averagePrice = lastHistory.getTotalTransactionPrice() / lastHistory.getTotalTransactionVolume();

		return averagePrice / previousAveragePrice >= VALID_RISING_PRICE_CHANGE_THRESHOLD ||
			averagePrice / previousAveragePrice <= VALID_FALLING_PRICE_CHANGE_THRESHOLD;
	}
}
