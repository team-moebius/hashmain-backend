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
	private static final int HISTORY_COUNT_THRESHOLD = 2;
	private static final double TRADE_PRICE_THRESHOLD = 10000D;
	private static final double HISTORY_VOLUME_MULTIPLIER_THRESHOLD = 10D;
	private static final double HISTORY_PRICE_THRESHOLD = 5000000D;

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
	 * 1. Volume
	 * 1-1. Heavy total transaction volume change : the last history is 10x bigger than previous ones' average volume.
	 * 1-2. [Optional] Heavy valid volume(bid - ask) change : the last history is 10x bigger than previous ones' average value,
	 * 		OR change the trade direction. (EX : bid > ask -> bid < ask)
	 *
	 * 2. Price
	 * 2-1. Valid transaction price (Bid - Ask) threshold : the last history is over 5Mil KRW than previous ones' total value.
	 *
	 * @param historiesDto
	 * @return
	 */
	private boolean isValidTradeHistories(AggregatedTradeHistoriesDto historiesDto) {
		List<AggregatedTradeHistoryDto> histories = historiesDto.getAggregatedTradeHistories();
		if (histories.size() < HISTORY_COUNT_THRESHOLD || histories.get(0).getTotalTransactionVolume() == 0D) {
			return false;
		}

		AggregatedTradeHistoryDto lastHistory = histories.get(histories.size() - 1);
		double previousAverageVolume = IntStream.range(0, histories.size() - 1)
			.mapToDouble(index -> histories.get(index).getTotalTransactionVolume())
			.average()
			.orElse(1D);

		double previousValidPrice = IntStream.range(0, histories.size() - 1)
			.mapToDouble(index -> histories.get(index).getTotalBidPrice() - histories.get(index).getTotalAskPrice())
			.sum();

		if (lastHistory.getTotalTransactionVolume() / previousAverageVolume >= HISTORY_VOLUME_MULTIPLIER_THRESHOLD &&
			((lastHistory.getTotalBidPrice() - lastHistory.getTotalAskPrice()) - previousValidPrice >= HISTORY_PRICE_THRESHOLD ||
				(lastHistory.getTotalBidPrice() - lastHistory.getTotalAskPrice()) - previousValidPrice <= -HISTORY_PRICE_THRESHOLD)) {
			log.info("[Trade] [{}/{}] The valid trade histories exist. [TTV: {}, PAV: {}, TVP: {}, PVP: {}]",
				historiesDto.getExchange(), historiesDto.getSymbol(), lastHistory.getTotalTransactionVolume(), previousAverageVolume,
				lastHistory.getTotalBidPrice() - lastHistory.getTotalAskPrice(), previousValidPrice);
			return true;
		}
		return false;
	}
}
