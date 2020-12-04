package com.moebius.backend.service.trade.validator;

import com.moebius.backend.dto.trade.TradeDto;
import com.moebius.backend.dto.trade.TradeHistoryDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * This validator is based on total valid price for recent 100 trade histories, not total transaction price.
 * This validator is based on dynamic term rather than fixed short term (5 minutes).
 * When simple one condition is satisfied during recent trades, This validator considers that trades are valid.
 *
 * ABS : Absolute value
 *
 * 1. Valid unit price change : ABS(The latest price / The earliest price - 1) >= 0.03 (>= 3%)
 *
 * @author Seonwoo Kim
 */
@Slf4j
@Component
public class DefaultTradeValidator implements TradeValidator {
	private static final double VALID_UNIT_PRICE_CHANGE_RATE_THRESHOLD = 0.03D;
	private static final int HISTORY_COUNT = 100;

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
