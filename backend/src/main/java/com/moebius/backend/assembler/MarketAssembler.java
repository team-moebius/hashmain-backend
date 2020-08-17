package com.moebius.backend.assembler;

import com.moebius.backend.domain.commons.Exchange;
import com.moebius.backend.domain.markets.Market;
import com.moebius.backend.dto.trade.TradeDto;
import com.moebius.backend.dto.exchange.MarketsDto;
import com.moebius.backend.dto.exchange.upbit.UpbitTradeMetaDto;
import com.moebius.backend.dto.frontend.response.MarketResponseDto;
import com.moebius.backend.utils.OrderUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.math3.util.Precision;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MarketAssembler {
	private final OrderUtil orderUtil;

	public Market assembleMarket(@NotNull Exchange exchange, @NotBlank String symbol) {
		Market market = new Market();
		market.setExchange(exchange);
		market.setSymbol(symbol);
		market.setCreatedAt(LocalDateTime.now());

		return market;
	}

	public List<Market> assembleMarkets(@NotNull Exchange exchange, @NotEmpty MarketsDto marketsDto) {
		return marketsDto.stream()
			.filter(Objects::nonNull)
			.map(marketDto -> assembleMarket(exchange, marketDto.getMarket()))
			.collect(Collectors.toList());
	}

	public MarketResponseDto assembleResponse(Market market) {
		return MarketResponseDto.builder()
			.exchange(market.getExchange())
			.symbol(market.getSymbol())
			.currentPrice(market.getCurrentPrice())
			.changeRate(market.getChangeRate())
			.accumulatedTradePrice(market.getAccumulatedTradePrice())
			.build();
	}

	public Map<String, Double> assembleCurrencyMarketPrices(List<Market> markets) {
		return markets.stream()
			.filter(Objects::nonNull)
			.collect(Collectors.toMap(market -> orderUtil.getCurrencyBySymbol(market.getSymbol()), Market::getCurrentPrice));
	}

	public Market assembleUpdatedMarket(Market market, TradeDto tradeDto, UpbitTradeMetaDto tradeMetaDto) {
		if (tradeDto == null || tradeMetaDto == null) {
			return market;
		}
		market.setCurrentPrice(tradeDto.getPrice());
		market.setChangeRate(Precision.round(tradeDto.getPrice() / tradeDto.getPrevClosingPrice() - 1, 4) * 100);
		market.setAccumulatedTradePrice(tradeMetaDto.getAccumulatedTradePrice());
		market.setAccumulatedTradeVolume(tradeMetaDto.getAccumulatedTradeVolume());
		market.setUpdatedAt(LocalDateTime.now());

		return market;
	}
}
