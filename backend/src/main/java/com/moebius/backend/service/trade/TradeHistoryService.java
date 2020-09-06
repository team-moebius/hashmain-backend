package com.moebius.backend.service.trade;

import com.moebius.backend.domain.commons.Exchange;
import com.moebius.backend.dto.trade.AggregatedTradeHistoryDto;
import com.moebius.backend.dto.trade.TradeHistoryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TradeHistoryService {
	private static final String COLON = ":";
	private static final String SLASH = "/";
	private static final String QUESTION = "?";
	private static final String TIME_CONDITION = "minutesAgo=";

	@Value("${moebius.data.host}")
	private String dataApiHost;
	@Value("${moebius.data.port}")
	private int dataApiPort;
	@Value("${moebius.data.rest.trade-histories}")
	private String tradeHistoriesUrl;
	@Value("${moebius.data.rest.aggregated-trade-histories}")
	private String aggregatedTradeHistoriesUrl;

	private final WebClient webClient;

	public Flux<TradeHistoryDto> getTradeHistories(Exchange exchange, String symbol) {
		String pathParameters = SLASH + exchange + SLASH + symbol;

		return webClient.get()
			.uri(dataApiHost + COLON + dataApiPort + SLASH + tradeHistoriesUrl + SLASH + pathParameters)
			.retrieve()
			.bodyToFlux(TradeHistoryDto.class);
	}

	public Mono<AggregatedTradeHistoryDto> getAggregatedTradeHistoryDto(Exchange exchange, String symbol, int minutesAgo) {
		String pathParameters = SLASH + exchange + SLASH + symbol;

		return webClient.get()
			.uri(dataApiHost + COLON + dataApiPort + SLASH + aggregatedTradeHistoriesUrl + SLASH + pathParameters + QUESTION + TIME_CONDITION
				+ minutesAgo)
			.retrieve()
			.bodyToMono(AggregatedTradeHistoryDto.class);
	}
}
