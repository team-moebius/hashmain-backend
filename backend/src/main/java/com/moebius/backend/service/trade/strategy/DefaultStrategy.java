package com.moebius.backend.service.trade.strategy;

import com.moebius.backend.dto.trade.TradeDto;
import com.moebius.backend.dto.trade.TradeHistoryDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * Default trade strategy is for catching the trades not to be catched by aggregated default strategy.
 * This strategy is based on total valid price for recent 500 trade histories, not total transaction price like aggregated default strategy.
 * This strategy is based on dynamic term rather than fixed short term (5 minutes) like aggregated default strategy.
 * When simple one condition is satisfied during recent trades, This strategy considers that trades are valid.
 *
 * ABS : Absolute value
 *
 * 1. Valid unit price change : ABS(The latest price / The earliest price - 1) >= 0.03 (>= 3%)
 *
 * @author Seonwoo Kim
 */
@Slf4j
@Component
public class DefaultStrategy implements TradeStrategy {
	private static final double VALID_UNIT_PRICE_CHANGE_RATE_THRESHOLD = 0.03D;
	private static final int HISTORY_COUNT = 500;

	@Override
	public int getCount() {
		return HISTORY_COUNT;
	}

	@Override
	public boolean isValid(TradeDto tradeDto, List<TradeHistoryDto> historyDtos) {
		if (tradeDto == null || CollectionUtils.isEmpty(historyDtos)) {
			return false;
		}

		if (hasValidUnitPriceChange(tradeDto, historyDtos)) {
			log.info("[Trade] [{}/{}] The valid trade histories exist.", tradeDto.getExchange(), tradeDto.getSymbol());
			return true;
		}
		return false;
	}

	private boolean hasValidUnitPriceChange(TradeDto latestTradeDto, List<TradeHistoryDto> historyDtos) {
		TradeHistoryDto earliestTradeHistoryDto = historyDtos.get(historyDtos.size() - 1);

		return Math.abs(latestTradeDto.getPrice() / earliestTradeHistoryDto.getPrice() - 1) >= VALID_UNIT_PRICE_CHANGE_RATE_THRESHOLD;
	}
}
