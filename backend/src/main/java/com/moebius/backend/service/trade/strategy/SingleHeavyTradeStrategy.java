package com.moebius.backend.service.trade.strategy;

import com.moebius.backend.dto.trade.AggregatedTradeHistoriesDto;
import com.moebius.backend.dto.trade.TradeDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


/**
 * Single heavy trade strategy is for catching the trades not to be catched by short term strategy.
 * When all the conditions are satisfied during recent 5 minutes, This strategy considers the trade is valid.
 *
 * 1.
 * 2.
 *
 * @author Seonwoo Kim
 */
@Slf4j
@Component
public class SingleHeavyTradeStrategy implements TradeStrategy {
	@Override
	public int getTimeInterval() {
		return 1;
	}

	@Override
	public int getTimeRange() {
		return 6;
	}

	@Override
	public boolean isValid(TradeDto tradeDto, AggregatedTradeHistoriesDto historiesDto) {
		return false;
	}
}
