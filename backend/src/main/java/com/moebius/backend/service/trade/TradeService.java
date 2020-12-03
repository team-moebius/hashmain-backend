package com.moebius.backend.service.trade;

import com.moebius.backend.assembler.TradeAssembler;
import com.moebius.backend.dto.trade.TradeDto;
import com.moebius.backend.service.order.InternalOrderService;
import com.moebius.backend.service.slack.TradeSlackSender;
import com.moebius.backend.service.trade.validator.TradeValidator;
import com.moebius.backend.service.trade.validator.aggregated.AggregatedTradeValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;

import static com.moebius.backend.utils.ThreadScheduler.COMPUTE;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeService {
	private final List<TradeValidator> tradeValidators;
	private final List<AggregatedTradeValidator> aggregatedTradeValidators;
	private final InternalOrderService internalOrderService;
	private final TradeHistoryService tradeHistoryService;
	private final TradeSlackSender tradeSlackSender;
	private final TradeAssembler tradeAssembler;
	private static final double TRADE_PRICE_THRESHOLD = 10000D;

	// TODO : change to orderIfValidTrade
	public void notifyIfValidTrade(TradeDto tradeDto) {
		if (isTradeOverPriceThreshold(tradeDto)) {
			aggregatedTradeValidators.forEach(validator -> {
				URI uri = tradeHistoryService.getAggregatedTradeHistoriesUri(tradeDto, validator.getTimeInterval(), validator.getTimeRange());

				tradeHistoryService.getAggregatedTradeHistories(uri)
					.subscribeOn(COMPUTE.scheduler())
					.filter(historiesDto -> validator.isValid(tradeDto, historiesDto))
					.map(historiesDto -> tradeAssembler.assembleByAggregatedTrade(tradeDto, historiesDto, uri.toString()))
					.flatMap(tradeSlackSender::sendMessage)
					.subscribe();
			});

			tradeValidators.forEach(validator -> {
				URI uri = tradeHistoryService.getTradeHistoriesUri(tradeDto, validator.getCount());

				tradeHistoryService.getTradeHistories(uri)
					.subscribeOn(COMPUTE.scheduler())
					.collectList()
					.filter(historyDtos -> validator.isValid(tradeDto, historyDtos))
					.map(historyDtos -> tradeAssembler.assembleByTrade(tradeDto, historyDtos, uri.toString()))
					.flatMap(tradeSlackSender::sendMessage)
					.subscribe();
			});
		}
	}

	private boolean isTradeOverPriceThreshold(TradeDto tradeDto) {
		return tradeDto.getVolume() * tradeDto.getPrice() >= TRADE_PRICE_THRESHOLD;
	}
}
