package com.moebius.backend.service.trade.strategy.aggregated;

import com.moebius.backend.dto.trade.AggregatedTradeHistoriesDto;
import com.moebius.backend.dto.trade.TradeDto;

public interface AggregatedTradeStrategy {
	int getTimeInterval();

	int getTimeRange();

	boolean isValid(TradeDto tradeDto, AggregatedTradeHistoriesDto historiesDto);
}
