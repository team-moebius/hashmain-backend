package com.moebius.backend.service.trade;

import com.moebius.backend.dto.trade.AggregatedTradeHistoriesDto;
import com.moebius.backend.dto.trade.TradeDto;
import com.moebius.backend.dto.trade.TradeHistoryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeHistoryService {
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

	public Flux<TradeHistoryDto> getTradeHistories(URI uri) {
		return webClient.get()
			.uri(uri)
			.retrieve()
			.bodyToFlux(TradeHistoryDto.class)
			.doOnError(exception -> log.warn("[Trade] Failed to get aggregated trade history.", exception))
			.onErrorResume(WebClientResponseException.class, exception -> Flux.empty());
	}

	public URI getTradeHistoriesUri(TradeDto tradeDto, int count) {
		return UriComponentsBuilder.newInstance()
			.scheme(scheme)
			.host(dataApiHost)
			.port(dataApiPort)
			.path(tradeHistoriesUrl)
			.pathSegment(tradeDto.getExchange().toString(), tradeDto.getSymbol())
			.queryParam("count", "{count}")
			.build(count);
	}

	public Mono<AggregatedTradeHistoriesDto> getAggregatedTradeHistories(URI uri) {
		return webClient.get()
			.uri(uri)
			.retrieve()
			.bodyToMono(AggregatedTradeHistoriesDto.class)
			.doOnError(exception -> log.warn("[Trade] Failed to get aggregated trade history.", exception))
			.onErrorResume(WebClientResponseException.class, exception -> Mono.empty());
	}

	public URI getAggregatedTradeHistoriesUri(TradeDto tradeDto, int interval, int range) {
		ZonedDateTime now = ZonedDateTime.now();

		String from = now.minusMinutes(range).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
		String to = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

		return UriComponentsBuilder.newInstance()
			.scheme(scheme)
			.host(dataApiHost)
			.port(dataApiPort)
			.path(aggregatedTradeHistoriesUrl)
			.pathSegment(tradeDto.getExchange().toString(), tradeDto.getSymbol())
			.queryParam("from", "{from}")
			.queryParam("to", "{to}")
			.queryParam("interval", interval)
			.build(from, to);
	}
}
