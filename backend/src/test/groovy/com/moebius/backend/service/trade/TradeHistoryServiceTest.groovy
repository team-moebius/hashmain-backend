package com.moebius.backend.service.trade

import com.moebius.backend.domain.commons.Change
import com.moebius.backend.domain.commons.Exchange
import com.moebius.backend.domain.commons.TradeType
import com.moebius.backend.dto.trade.AggregatedTradeHistoriesDto
import com.moebius.backend.dto.trade.AggregatedTradeHistoryDto
import com.moebius.backend.dto.trade.TradeDto
import com.moebius.backend.dto.trade.TradeHistoryDto
import org.springframework.util.CollectionUtils
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import spock.lang.Specification
import spock.lang.Subject

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
		def uri = UriComponentsBuilder.newInstance().build().toUri()

		1 * webClient.get() >> uriSpec
		1 * uriSpec.uri(_ as URI) >> headersSpec
		1 * headersSpec.retrieve() >> responseSpec
		1 * responseSpec.bodyToFlux(TradeHistoryDto.class) >> Flux.just(TradeHistoryDto.builder()
				.exchange(exchange)
				.symbol(symbol)
				.tradeType(TradeType.BID)
				.change(Change.RISE)
				.build())

		expect:
		StepVerifier.create(tradeHistoryService.getTradeHistories(uri))
				.assertNext({
					assert it != null
					assert it.getExchange() == Exchange.UPBIT
					assert it.getSymbol() == "KRW-BTC"
					assert it.getTradeType() == TradeType.BID
					assert it.getChange() == Change.RISE
				})
				.verifyComplete()
	}

	def "Should get trade histories url"() {
		when:
		def result = tradeHistoryService.getTradeHistoriesUri(getTradeDto(), 100)

		then:
		result instanceof URI
		result.toString().contains("count=100")
	}

	def "Should get aggregated trade histories"() {
		given:
		def uri = UriComponentsBuilder.newInstance().build().toUri()

		1 * webClient.get() >> uriSpec
		1 * uriSpec.uri(_ as URI) >> headersSpec
		1 * headersSpec.retrieve() >> responseSpec
		1 * responseSpec.bodyToMono(AggregatedTradeHistoriesDto.class) >> Mono.just(AggregatedTradeHistoriesDto.builder()
				.aggregatedTradeHistories([Stub(AggregatedTradeHistoryDto)])
				.build())

		expect:
		StepVerifier.create(tradeHistoryService.getAggregatedTradeHistories(uri))
				.assertNext({
					assert it != null
					assert !CollectionUtils.isEmpty(it.getAggregatedTradeHistories())
					assert it.getAggregatedTradeHistories().get(0) instanceof AggregatedTradeHistoryDto
				})
				.verifyComplete()
	}

	def "Should get aggregated trade histories url"() {
		when:
		def result = tradeHistoryService.getAggregatedTradeHistoriesUri(getTradeDto(), 1, 5)

		then:
		result instanceof URI
		result.toString().contains("%3A")
		result.toString().contains("interval=1")
	}

	TradeDto getTradeDto() {
		TradeDto tradeDto = new TradeDto()
		tradeDto.setExchange(Exchange.UPBIT)
		tradeDto.setSymbol("KRW-BTC")

		return tradeDto
	}
}
