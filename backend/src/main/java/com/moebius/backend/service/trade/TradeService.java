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
	private static final int DEFAULT_TIME_CONDITION = 10;

	public void identifyValidTrade(TradeDto tradeDto) {
		tradeHistoryService.getAggregatedTradeHistoryDto(tradeDto.getExchange(), tradeDto.getSymbol(), DEFAULT_TIME_CONDITION)
			.subscribeOn(COMPUTE.scheduler())
			.filter(history -> isValidTrade(tradeDto, history))
			.map(history -> tradeAssembler.assembleSlackDto(tradeDto, history))
			.subscribe(tradeSlackSender::sendMessage);
	}

	/**
	 * Valid trade conditions are
	 * 1. average total transaction volume during 1 minute (10 minutes standard) < recent transaction volume
	 * 2. single trade price / average total transaction price during 1 minute (10 minutes standard) > 1.0D or < -1.0D
	 * @param tradeDto
	 * @param historyDto
	 * @return
	 */
	private boolean isValidTrade(TradeDto tradeDto, AggregatedTradeHistoryDto historyDto) {
		if (historyDto == null || historyDto.getTotalTransactionVolume() == 0D) {
			return false;
		}
		double priceChangeRate = Math.round((tradeDto.getPrice() /
			(historyDto.getTotalTransactionPrice() / historyDto.getTotalTransactionVolume()) - 1) * 100d);

		return historyDto.getTotalTransactionVolume() / DEFAULT_TIME_CONDITION < tradeDto.getVolume() &&
			(priceChangeRate > 1.0D || priceChangeRate < -1.0D);
	}
}
