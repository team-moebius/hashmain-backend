package com.moebius.backend.service.trade.strategy;

import com.moebius.backend.domain.commons.TradeType;
import com.moebius.backend.dto.trade.TradeDto;
import com.moebius.backend.dto.trade.TradeHistoryDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * Default trade strategy is for catching the trades not to be catched by aggregated default strategy.
 * This strategy is based on total valid price for recent 100 trade histories, not total transaction price like aggregated default strategy.
 * This strategy is based on middle ~ long term rather than fixed short term (5 minutes) like aggregated default strategy.
 * When all the conditions are satisfied during recent trades, This strategy considers that symbol is valid.
 *
 * ABS : Absolute value, SUM : Sum of trades
 *
 * 1. Total valid price : ABS(SUM(Bid - Ask)) >= 10M KRW
 * 2. Valid unit price change : ABS(The latest price / The earliest price * 100 - 1) >= 3%
 *
 * @author Seonwoo Kim
 */
@Slf4j
@Component
public class DefaultStrategy implements TradeStrategy {
	private static final double TOTAL_VALID_PRICE_THRESHOLD = 10000000D;
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

		if (hasTotalValidPrice(tradeDto, historyDtos) &&
			hasValidUnitPriceChange(tradeDto, historyDtos)) {
			log.info("[Trade] [{}/{}] The valid trade histories exist.", tradeDto.getExchange(), tradeDto.getSymbol());
			return true;
		}
		return false;
	}

	private boolean hasTotalValidPrice(TradeDto latestTradeDto, List<TradeHistoryDto> historyDtos) {
		double latestValidPrice = latestTradeDto.getTradeType() == TradeType.BID ?
			latestTradeDto.getPrice() * latestTradeDto.getVolume() :
			-latestTradeDto.getPrice() * latestTradeDto.getVolume();

		return Math.abs(latestValidPrice +
				historyDtos.stream().mapToDouble(historyDto -> historyDto.getTradeType() == TradeType.BID ?
					historyDto.getPrice() * historyDto.getVolume() :
					-historyDto.getPrice() * historyDto.getVolume())
					.sum()) >= TOTAL_VALID_PRICE_THRESHOLD;
	}

	private boolean hasValidUnitPriceChange(TradeDto latestTradeDto, List<TradeHistoryDto> historyDtos) {
		TradeHistoryDto earliestTradeHistoryDto = historyDtos.get(historyDtos.size() - 1);

		return Math.abs(latestTradeDto.getPrice() / earliestTradeHistoryDto.getPrice() - 1) >= VALID_UNIT_PRICE_CHANGE_RATE_THRESHOLD;
	}
}
