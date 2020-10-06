package com.moebius.backend.service.trade.strategy;

import com.moebius.backend.dto.trade.AggregatedTradeHistoriesDto;
import com.moebius.backend.dto.trade.TradeDto;

@FunctionalInterface
public interface TradeStrategy {
	boolean isValid(TradeDto tradeDto, AggregatedTradeHistoriesDto historiesDto);
}
