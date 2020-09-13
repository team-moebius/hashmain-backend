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
	private static final int DEFAULT_TIME_RANGE = 2;
	private static final double TRADE_PRICE_THRESHOLD = 10000D;
	private static final double HISTORY_VOLUME_MULTIPLIER_THRESHOLD = 10D;
	private static final double HISTORY_PRICE_THRESHOLD = 5000000D;

	public void identifyValidTrade(TradeDto tradeDto) {
		if (isValidTrade(tradeDto)) {
			tradeHistoryService.getAggregatedTradeHistories(tradeDto.getExchange(), tradeDto.getSymbol(), DEFAULT_TIME_INTERVAL, DEFAULT_TIME_RANGE)
				.subscribeOn(COMPUTE.scheduler())
				.filter(this::isValidTradeHistories)
				.map(histories -> tradeAssembler.assembleSlackDto(tradeDto, histories))
				.subscribe(tradeSlackSender::sendMessage);
		}
	}

	private boolean isValidTrade(TradeDto tradeDto) {
		return tradeDto.getVolume() * tradeDto.getPrice() >= TRADE_PRICE_THRESHOLD;
	}

	/**
	 * Valid trade conditions are based on
	 * 1. Volume
	 * 1-1. Heavy total transaction volume change : the last history is 10x bigger than previous ones' average value.
	 * 1-2. [Optional] Heavy valid volume(bid - ask) change : the last history is 10x bigger than previous ones' average value,
	 * 		OR change the trade direction. (EX : bid > ask -> bid < ask)
	 *
	 * 2. Price
	 * 2-1. Total transaction price threshold : Over 5Mil KRW
	 *
	 * @param historiesDto
	 * @return
	 */
	private boolean isValidTradeHistories(AggregatedTradeHistoriesDto historiesDto) {
		List<AggregatedTradeHistoryDto> histories = historiesDto.getAggregatedTradeHistories();
		if (histories.isEmpty() || histories.get(0).getTotalTransactionVolume() == 0D) {
			return false;
		}

		AggregatedTradeHistoryDto lastHistory = histories.get(histories.size() - 1);
		double previousAverageVolume = IntStream.range(0, histories.size() - 1)
			.mapToDouble(index -> histories.get(index).getTotalTransactionVolume())
			.average()
			.orElse(1D);

		double previousTotalPrice = IntStream.range(0, histories.size() - 1)
			.mapToDouble(index -> histories.get(index).getTotalTransactionPrice())
			.sum();

		log.info("[Trade] [{}/{}] Calculated valid trade histories info. [previousAverageVolume: {}, previousTotalPrice: {}]",
			historiesDto.getExchange(), historiesDto.getSymbol(), previousAverageVolume, previousTotalPrice);
		return lastHistory.getTotalTransactionVolume() / previousAverageVolume >= HISTORY_VOLUME_MULTIPLIER_THRESHOLD ||
			lastHistory.getTotalTransactionPrice() - previousTotalPrice >= HISTORY_PRICE_THRESHOLD;
	}
}
