package com.moebius.backend.assembler

import com.moebius.backend.domain.commons.Exchange
import com.moebius.backend.domain.markets.Market
import com.moebius.backend.dto.trade.TradeDto
import com.moebius.backend.dto.exchange.MarketDto
import com.moebius.backend.dto.exchange.MarketsDto
import com.moebius.backend.dto.exchange.upbit.UpbitTradeMetaDto
import com.moebius.backend.dto.frontend.response.MarketResponseDto
import com.moebius.backend.utils.OrderUtil
import spock.lang.Specification
import spock.lang.Subject

class MarketAssemblerTest extends Specification {
	def orderUtil = Spy(OrderUtil)

	@Subject
	def marketAssembler = new MarketAssembler(orderUtil)

	def "Should assemble market"() {
		when:
		def result = marketAssembler.assembleMarket(Exchange.UPBIT, "KRW-BTC")

		then:
		result instanceof Market
		result.getExchange() == Exchange.UPBIT
		result.getSymbol() == "KRW-BTC"
	}

	def "Should assemble markets"() {
		given:
		def markets = new MarketsDto()
		markets.add(Stub(MarketDto))
		markets.add(Stub(MarketDto))
		markets.add(null)

		when:
		def result = marketAssembler.assembleMarkets(Exchange.UPBIT, markets)

		then:
		result instanceof List
		result.size() == 2
		result.get(0) instanceof Market
	}

	def "Should assemble api response"() {
		given:
		def market = Stub(Market) {
			getExchange() >> Exchange.UPBIT
		}

		when:
		def result = marketAssembler.assembleResponse(market)

		then:
		result instanceof MarketResponseDto
		result.getExchange() == Exchange.UPBIT
	}

	def "Should assemble currency market prices"() {
		given:
		def krwBtcMarket = Stub(Market) {
			getSymbol() >> "KRW-BTC"
			getCurrentPrice() >> 10000000D
		}
		def krwEthMarket = Stub(Market) {
			getSymbol() >> "KRW-ETH"
			getCurrentPrice() >> 270000D
		}

		when:
		def result = marketAssembler.assembleCurrencyMarketPrices([krwBtcMarket, krwEthMarket])

		then:
		2 * orderUtil.getCurrencyBySymbol(_ as String)

		result instanceof Map
		result.size() == 2
		result.get("BTC") == 10000000D
	}

	def "Should assemble updated market"() {
		given:
		def market = new Market()
		def tradeDto = Stub(TradeDto) {
			getPrice() >> 120D
			getPrevClosingPrice() >> 100D
		}
		def tradeMetaDto = Stub(UpbitTradeMetaDto) {
			getAccumulatedTradePrice() >> 10000D
			getAccumulatedTradeVolume() >> 100D
		}

		when:
		def result = marketAssembler.assembleUpdatedMarket(market, tradeDto, tradeMetaDto)

		then:
		result instanceof Market
		result.getCurrentPrice() == 120D
		result.getAccumulatedTradePrice() == 10000D
	}

	def "Should not assemble updated market when #CONDITION"() {
		given:
		def market = Stub(Market) {
			getCurrentPrice() >> 100D
		}

		when:
		def result = marketAssembler.assembleUpdatedMarket(market, TRADE_DATA, TRADE_META_DATA)

		then:
		result instanceof Market
		result.getCurrentPrice() == 100D

		where:
		CONDITION                 | TRADE_DATA     | TRADE_META_DATA
		"trade data is null"      | null           | Stub(UpbitTradeMetaDto)
		"trade meta data is null" | Stub(TradeDto) | null
	}
}
