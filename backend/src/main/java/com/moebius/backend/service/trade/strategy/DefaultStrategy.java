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
 * When all the conditions are satisfied during recent 100 trades, This strategy considers that symbol is valid.
 *
 * ABS : Absolute value, SUM : Sum of trades
 *
 * 1. Total valid price : ABS(SUM(Bid - Ask)) >= 10M KRW
 * 2. Valid unit price change : ABS(The latest price / The earliest price * 100 - 1) >= 2%
 *
 * @author Seonwoo Kim
 */
@Slf4j
@Component
public class DefaultStrategy implements TradeStrategy {
	private static final double TOTAL_VALID_PRICE_THRESHOLD = 10000000D;

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
		return Math.abs((latestTradeDto.getPrice() / historyDtos.get(historyDtos.size() - 1).getPrice() - 1) * 100) >= 2D;
	}
}
