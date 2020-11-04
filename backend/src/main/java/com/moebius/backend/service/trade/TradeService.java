package com.moebius.backend.service.trade;

import com.moebius.backend.assembler.TradeAssembler;
import com.moebius.backend.dto.trade.TradeDto;
import com.moebius.backend.service.slack.TradeSlackSender;
import com.moebius.backend.service.trade.strategy.aggregated.AggregatedTradeStrategy;
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
	private final List<AggregatedTradeStrategy> aggregatedTradeStrategies;
	private final TradeHistoryService tradeHistoryService;
	private final TradeSlackSender tradeSlackSender;
	private final TradeAssembler tradeAssembler;
	private static final double TRADE_PRICE_THRESHOLD = 10000D;

	public void identifyValidTrade(TradeDto tradeDto) {
		if (isTradeOverPriceThreshold(tradeDto)) {
			aggregatedTradeStrategies.forEach(strategy -> {
				URI uri = tradeHistoryService.getAggregatedTradeHistoriesUri(tradeDto, strategy.getTimeInterval(), strategy.getTimeRange());

				tradeHistoryService.getAggregatedTradeHistories(uri)
					.subscribeOn(COMPUTE.scheduler())
					.filter(historiesDto -> strategy.isValid(tradeDto, historiesDto))
					.map(historiesDto -> tradeAssembler.assembleSlackDto(tradeDto, historiesDto, uri.toString()))
					.flatMap(tradeSlackSender::sendMessage)
					.subscribe();
			});
		}

	}

	private boolean isTradeOverPriceThreshold(TradeDto tradeDto) {
		return tradeDto.getVolume() * tradeDto.getPrice() >= TRADE_PRICE_THRESHOLD;
	}
}
