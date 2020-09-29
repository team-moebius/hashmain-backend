package com.moebius.backend.service.trade

import com.moebius.backend.domain.commons.Change
import com.moebius.backend.domain.commons.Exchange
import com.moebius.backend.domain.commons.TradeType
import com.moebius.backend.dto.trade.AggregatedTradeHistoriesDto
import com.moebius.backend.dto.trade.AggregatedTradeHistoryDto
import com.moebius.backend.dto.trade.TradeHistoryDto
import org.springframework.util.CollectionUtils
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import spock.lang.Specification
import spock.lang.Subject

import java.util.function.Function

class TradeHistoryServiceTest extends Specification {
	def webClient = Mock(WebClient)
	def uriSpec = Mock(WebClient.RequestHeadersUriSpec)
	def headersSpec = Mock(WebClient.RequestHeadersSpec)
	def responseSpec = Mock(WebClient.ResponseSpec)

	def exchange = Exchange.UPBIT
	def symbol = "KRW-BTC"

	@Subject
	def tradeHistoryService = new TradeHistoryService(webClient)

	def "Should get trade histories"() {
		given:
		1 * webClient.get() >> uriSpec
		1 * uriSpec.uri(_ as Function<UriBuilder, URI>) >> headersSpec
		1 * headersSpec.retrieve() >> responseSpec
		1 * responseSpec.bodyToFlux(TradeHistoryDto.class) >> Flux.just(TradeHistoryDto.builder()
				.exchange(exchange)
				.symbol(symbol)
				.tradeType(TradeType.BID)
				.change(Change.RISE)
				.build())

		expect:
		StepVerifier.create(tradeHistoryService.getTradeHistories(exchange, symbol, 10))
				.assertNext({
					it != null
					it.getExchange() == Exchange.UPBIT
					it.getSymbol() == "KRW-BTC"
					it.getTradeType() == TradeType.BID
					it.getChange() == Change.RISE
				})
				.verifyComplete()
	}

	def "Should get aggregated trade history"() {
		given:
		1 * webClient.get() >> uriSpec
		1 * uriSpec.uri(_ as Function<UriBuilder, URI>) >> headersSpec
		1 * headersSpec.retrieve() >> responseSpec
		1 * responseSpec.bodyToMono(AggregatedTradeHistoriesDto.class) >> Mono.just(AggregatedTradeHistoriesDto.builder()
				.exchange(exchange)
				.symbol(symbol)
				.interval(1L)
				.aggregatedTradeHistories([Stub(AggregatedTradeHistoryDto)])
				.build())

		expect:
		StepVerifier.create(tradeHistoryService.getAggregatedTradeHistories(exchange, symbol, 1, 2))
				.assertNext({
					it != null
					it.getExchange() == Exchange.UPBIT
					it.getSymbol() == "KRW-BTC"
					it.getInterval() == 1L
					!CollectionUtils.isEmpty(it.getAggregatedTradeHistories())
					it.getAggregatedTradeHistories().get(0) instanceof AggregatedTradeHistoryDto
				})
				.verifyComplete()
	}
}
