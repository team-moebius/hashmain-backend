package com.moebius.backend.service.trade.strategy;

import com.moebius.backend.dto.trade.AggregatedTradeHistoriesDto;
import com.moebius.backend.dto.trade.TradeDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


/**
 * Heavy valid trade strategy defines the valid trades by conditions below.
 * When all the conditions are satisfied, This strategy considers these trades are valid.
 *
 * ...
 *
 * @author Seonwoo Kim
 */
@Slf4j
@Component
public class HeavyValidTradeStrategy implements TradeStrategy {
	@Override
	public int getTimeInterval() {
		return 1;
	}

	@Override
	public int getTimeRange() {
		return 3;
	}

	@Override
	public boolean isValid(TradeDto tradeDto, AggregatedTradeHistoriesDto historiesDto) {
		return false;
	}
}
