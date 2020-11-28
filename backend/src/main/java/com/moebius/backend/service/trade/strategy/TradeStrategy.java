package com.moebius.backend.service.trade.strategy;

import com.moebius.backend.dto.trade.TradeDto;
import com.moebius.backend.dto.trade.TradeHistoryDto;

import java.util.List;

public interface TradeStrategy {
	default int getCount() {
		return 100;
	}

	boolean isValid(TradeDto tradeDto, List<TradeHistoryDto> HistoryDtos);
}
