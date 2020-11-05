package com.moebius.backend.service.trade.strategy.aggregated;

import com.moebius.backend.dto.trade.AggregatedTradeHistoriesDto;
import com.moebius.backend.dto.trade.TradeDto;

public class DailyTermStrategy implements AggregatedTradeStrategy {
	@Override
	public int getTimeInterval() {
		return 0;
	}

	@Override
	public int getTimeRange() {
		return 0;
	}

	@Override
	public boolean isValid(TradeDto tradeDto, AggregatedTradeHistoriesDto historiesDto) {
		return false;
	}
}
