package com.moebius.backend.service.trade.validator.aggregated;

import com.moebius.backend.dto.trade.AggregatedTradeHistoriesDto;
import com.moebius.backend.dto.trade.TradeDto;

public interface AggregatedTradeValidator {
	int getTimeInterval();

	int getTimeRange();

	boolean isValid(TradeDto tradeDto, AggregatedTradeHistoriesDto historiesDto);

	String getSubscribers(AggregatedTradeHistoriesDto historiesDto);
}
