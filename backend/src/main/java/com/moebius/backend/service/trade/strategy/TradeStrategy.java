package com.moebius.backend.service.trade.strategy;

import com.moebius.backend.dto.trade.AggregatedTradeHistoriesDto;
import com.moebius.backend.dto.trade.TradeDto;

public interface TradeStrategy {
	int getTimeInterval();

	int getTimeRange();

	boolean isValid(TradeDto tradeDto, AggregatedTradeHistoriesDto historiesDto);
}
