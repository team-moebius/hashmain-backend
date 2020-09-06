package com.moebius.backend.service.trade;

import com.moebius.backend.assembler.TradeAssembler;
import com.moebius.backend.dto.trade.AggregatedTradeHistoryDto;
import com.moebius.backend.dto.trade.TradeDto;
import com.moebius.backend.service.slack.TradeSlackSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.moebius.backend.utils.ThreadScheduler.COMPUTE;

@Service
@RequiredArgsConstructor
public class TradeService {
	private final TradeHistoryService tradeHistoryService;
	private final TradeSlackSender tradeSlackSender;
	private final TradeAssembler tradeAssembler;
	private static final int DEFAULT_TIME_CONDITION = 5;

	public void identifyValidTrade(TradeDto tradeDto) {
		tradeHistoryService.getAggregatedTradeHistoryDto(tradeDto.getExchange(), tradeDto.getSymbol(), DEFAULT_TIME_CONDITION)
			.subscribeOn(COMPUTE.scheduler())
			.filter(history -> isValidTrade(tradeDto, history))
			.map(history -> tradeAssembler.assembleSlackDto(tradeDto, history))
			.subscribe(tradeSlackSender::sendMessage);
	}

	/**
	 * Valid trade conditions are
	 * 1. average total transaction volume (per 1 minute, during 5 minutes) < single trade volume
	 * 2. single trade price / average transaction price (per 1 minute, during 5 minutes) > 2.0D or < -2.0D
	 * @param tradeDto
	 * @param historyDto
	 * @return
	 */
	private boolean isValidTrade(TradeDto tradeDto, AggregatedTradeHistoryDto historyDto) {
		double priceChangeRate = Math.round((tradeDto.getPrice() /
			(historyDto.getTotalTransactionPrice() / historyDto.getTotalTransactionVolume()) - 1) * 100d);

		return historyDto.getTotalTransactionVolume() / DEFAULT_TIME_CONDITION < tradeDto.getVolume() &&
			(priceChangeRate > 2.0D || priceChangeRate < -2.0D);
	}
}
