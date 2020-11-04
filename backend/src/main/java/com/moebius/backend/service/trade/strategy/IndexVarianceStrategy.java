package com.moebius.backend.service.trade.strategy;

import com.moebius.backend.dto.trade.TradeDto;
import com.moebius.backend.dto.trade.TradeHistoryDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Heavy trade strategy is for catching the trades not to be catched by short term strategy.
 * When all the conditions are satisfied during recent 5 minutes, This strategy considers the trade is valid.
 *
 * 1. TBD
 *
 * @author Seonwoo Kim
 */
@Slf4j
@Component
public class IndexVarianceStrategy implements TradeStrategy {
	@Override
	public boolean isValid(TradeDto tradeDto, List<TradeHistoryDto> HistoryDtos) {
		return false;
	}
}
