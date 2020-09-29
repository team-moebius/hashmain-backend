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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeHistoryService {
	private static final String COLON = ":";
	private static final String SLASH = "/";
	private static final String HISTORIES_PARAMETER = "?count=%d";
	private static final String AGGREGATED_HISTORIES_PARAMETERS = "?from=%s&to=%s&interval=%d";

	@Value("${moebius.data.scheme}")
	private String scheme;
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
		return webClient.get()
			.uri(uriBuilder -> uriBuilder
				.scheme(scheme)
				.host(dataApiHost)
				.port(dataApiPort)
				.path(tradeHistoriesUrl)
				.pathSegment(exchange.toString(), symbol)
				.queryParam("count", "{count}")
				.build(count))
			.retrieve()
			.bodyToFlux(TradeHistoryDto.class)
			.doOnError(exception -> log.warn("[Trade] Failed to get aggregated trade history.", exception))
			.onErrorResume(WebClientResponseException.class, exception -> Flux.empty());
	}

	public Mono<AggregatedTradeHistoriesDto> getAggregatedTradeHistories(Exchange exchange, String symbol, long interval, long range) {
		ZonedDateTime now = ZonedDateTime.now();

		String from = now.minusMinutes(range).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
		String to = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

		return webClient.get()
			.uri(uriBuilder -> uriBuilder
				.scheme(scheme)
				.host(dataApiHost)
				.port(dataApiPort)
				.path(aggregatedTradeHistoriesUrl)
				.pathSegment(exchange.toString(), symbol)
				.queryParam("from", "{from}")
				.queryParam("to", "{to}")
				.queryParam("interval", interval)
				.build(from, to))
			.retrieve()
			.bodyToMono(AggregatedTradeHistoriesDto.class)
			.doOnError(exception -> log.warn("[Trade] Failed to get aggregated trade history.", exception))
			.onErrorResume(WebClientResponseException.class, exception -> Mono.empty());
	}
}
