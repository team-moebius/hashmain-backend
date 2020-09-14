package com.moebius.backend.service.trade;

import com.moebius.backend.domain.commons.Exchange;
import com.moebius.backend.dto.trade.AggregatedTradeHistoriesDto;
import com.moebius.backend.dto.trade.TradeHistoryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeHistoryService {
	private static final String COLON = ":";
	private static final String SLASH = "/";
	private static final String HISTORIES_PARAMETER = "?count=%d";
	private static final String AGGREGATED_HISTORIES_PARAMETERS = "?from=%s&to=%s&interval=%d";

	@Value("${moebius.data.host}")
	private String dataApiHost;
	@Value("${moebius.data.port}")
	private int dataApiPort;
	@Value("${moebius.data.rest.trade-histories}")
	private String tradeHistoriesUrl;
	@Value("${moebius.data.rest.aggregated-trade-histories}")
	private String aggregatedTradeHistoriesUrl;

	private final WebClient webClient;

	public Flux<TradeHistoryDto> getTradeHistories(Exchange exchange, String symbol, int count) {
		String dataApiEndpoint = dataApiHost + COLON + dataApiPort;
		String pathParameters = SLASH + exchange + SLASH + symbol;

		return webClient.get()
			.uri(dataApiEndpoint + tradeHistoriesUrl + pathParameters + String.format(HISTORIES_PARAMETER, count))
			.retrieve()
			.bodyToFlux(TradeHistoryDto.class)
			.doOnError(exception -> log.warn("[Trade] Failed to get aggregated trade history.", exception))
			.onErrorReturn(WebClientResponseException.class, TradeHistoryDto.builder().build());
	}

	public Mono<AggregatedTradeHistoriesDto> getAggregatedTradeHistories(Exchange exchange, String symbol, long interval, long range) {
		String dataApiEndpoint = dataApiHost + COLON + dataApiPort;
		String pathParameters = SLASH + exchange + SLASH + symbol;
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
		String from = dateTimeFormatter.format(LocalDateTime.now().minusMinutes(range));
		String to = dateTimeFormatter.format(LocalDateTime.now());

		return webClient.get()
			.uri(dataApiEndpoint + aggregatedTradeHistoriesUrl + pathParameters + String.format(AGGREGATED_HISTORIES_PARAMETERS, from, to, interval))
			.retrieve()
			.bodyToMono(AggregatedTradeHistoriesDto.class)
			.doOnError(exception -> log.warn("[Trade] Failed to get aggregated trade history.", exception))
			.onErrorReturn(WebClientResponseException.class, AggregatedTradeHistoriesDto.builder().build());
	}
}
