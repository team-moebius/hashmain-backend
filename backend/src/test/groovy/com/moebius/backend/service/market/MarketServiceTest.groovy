package com.moebius.backend.service.market

import com.moebius.backend.assembler.MarketAssembler
import com.moebius.backend.domain.commons.Exchange
import com.moebius.backend.domain.markets.Market
import com.moebius.backend.domain.markets.MarketRepository
import com.moebius.backend.dto.trade.TradeDto
import com.moebius.backend.dto.exchange.MarketsDto
import com.moebius.backend.dto.exchange.upbit.UpbitTradeMetaDto
import com.moebius.backend.dto.frontend.response.MarketResponseDto
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import spock.lang.Specification
import spock.lang.Subject

class MarketServiceTest extends Specification {
	def webClient = Mock(WebClient)
	def marketRepository = Mock(MarketRepository)
	def marketAssembler = Mock(MarketAssembler)
	def uriSpec = Mock(WebClient.RequestHeadersUriSpec)
	def responseSpec = Mock(WebClient.ResponseSpec)
	def exchange = Exchange.UPBIT
	def marketId = "5e7a30eceea97a67367a4b6a"

	@Subject
	def marketService = new MarketService(webClient, marketRepository, marketAssembler)

	// TODO : Refactor updating market price logic
	def "Should update market price"() {
		given:
		marketAssembler.assembleUpdatedMarket(_ as Market, _ as TradeDto, _ as UpbitTradeMetaDto) >> Stub(Market)

		when:
		marketService.updateMarketPrice(Stub(TradeDto))

		then:
		1 * marketRepository.findByExchangeAndSymbol(_ as Exchange, _ as String) >> Mono.just(Stub(Market))
		1 * webClient.get() >> uriSpec
		1 * uriSpec.uri(_ as String) >> uriSpec
		1 * uriSpec.retrieve() >> responseSpec
		1 * responseSpec.bodyToFlux(UpbitTradeMetaDto.class) >> Flux.just(Stub(UpbitTradeMetaDto))
	}

	def "Should get markets"() {
		given:
		1 * marketRepository.findAllByExchange(_ as Exchange) >> Flux.just(Stub(Market), Stub(Market))
		2 * marketAssembler.assembleResponse(_ as Market) >> Stub(MarketResponseDto)

		expect:
		StepVerifier.create(marketService.getMarkets(exchange))
				.assertNext({
					it != null
					it.getStatusCode() == HttpStatus.OK
				})
				.verifyComplete()
	}

	def "Should delete market"() {
		given:
		1 * marketRepository.deleteById(_ as ObjectId) >> Mono.just(new Void())

		expect:
		StepVerifier.create(marketService.deleteMarket(marketId))
				.assertNext({
					it != null
					it.getStatusCode() == HttpStatus.OK
				})
				.verifyComplete()
	}

	def "Should update markets with create if not exist except non-KRW market"() {
		given:
		1 * webClient.get() >> uriSpec
		1 * uriSpec.uri(_ as String) >> uriSpec
		1 * uriSpec.retrieve() >> responseSpec
		1 * responseSpec.bodyToMono(MarketsDto.class) >> Mono.just(Stub(MarketsDto))
		1 * marketAssembler.assembleMarkets(_ as Exchange, _ as MarketsDto) >> [buildMarket("KRW-BTC"), buildMarket("KRW-ETH"), buildMarket("BTC-ETH")]
		1 * marketAssembler.assembleMarket(Exchange.UPBIT, "KRW-BTC") >> Stub(Market) {
			getExchange() >> Exchange.UPBIT
			getSymbol() >> "KRW-BTC"
		}
		1 * marketRepository.findByExchangeAndSymbol(Exchange.UPBIT, "KRW-BTC") >> Mono.empty()
		1 * marketRepository.findByExchangeAndSymbol(Exchange.UPBIT, "KRW-ETH") >> Mono.just(Stub(Market))
		1 * marketRepository.save(_ as Market) >> Mono.just(Stub(Market))

		expect:
		StepVerifier.create(marketService.updateMarkets(Exchange.UPBIT))
				.assertNext({
					it != null
					it.getStatusCode() == HttpStatus.OK
				})
				.verifyComplete()
	}

	def "Should get currency market prices"() {
		given:
		1 * marketRepository.findAllByExchange(_ as Exchange) >> Flux.just(Stub(Market), Stub(Market))
		1 * marketAssembler.assembleCurrencyMarketPrices(_ as List) >> ["BTC": 10000000D, "ETH": 300000D]

		expect:
		StepVerifier.create(marketService.getCurrencyMarketPriceMap(Exchange.UPBIT))
				.assertNext({
					it instanceof Map<String, Double>
					it.get("BTC") == 10000000D
					it.get("ETH") == 300000D
				})
				.verifyComplete()
	}

	def "Should get current price"() {
		given:
		def market = Stub(Market) {
			getCurrentPrice() >> 100000000000D
		}
		1 * marketRepository.findByExchangeAndSymbol(_ as Exchange, _ as String) >> Mono.just(market)

		expect:
		StepVerifier.create(marketService.getCurrentPrice(Exchange.UPBIT, "KRW-KNU"))
				.assertNext({
					it instanceof Double
					it == 100000000000D
				})
				.verifyComplete()
	}

	Market buildMarket(String symbol) {
		Market market = new Market()
		market.setExchange(Exchange.UPBIT)
		market.setSymbol(symbol)

		return market
	}
}
