package com.moebius.backend.service.trade.strategy

import com.moebius.backend.domain.commons.Exchange
import com.moebius.backend.domain.commons.TradeType
import com.moebius.backend.dto.trade.TradeDto
import com.moebius.backend.dto.trade.TradeHistoryDto
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class DefaultStrategyTest extends Specification {
	@Shared
	def bidHigherHistoryDtos = [TradeHistoryDto.builder().tradeType(TradeType.BID).price(100000D).volume(10000D).build(),
								TradeHistoryDto.builder().tradeType(TradeType.ASK).price(100000D).volume(9000D).build()]
	@Shared
	def askHigherHistoryDtos = [TradeHistoryDto.builder().tradeType(TradeType.ASK).price(100000D).volume(10000D).build(),
								TradeHistoryDto.builder().tradeType(TradeType.BID).price(100000D).volume(9000D).build()]
	@Subject
	def defaultStrategy = new DefaultStrategy()

	def "Should be valid"() {
		expect:
		defaultStrategy.isValid(TRADE_DTO, HISTORY_DTOS)

		where:
		TRADE_DTO                                  | HISTORY_DTOS
		buildTradeDto(TradeType.BID, 103000D, 10D) | bidHigherHistoryDtos
		buildTradeDto(TradeType.ASK, 97000D, 10D)  | askHigherHistoryDtos
	}

	@Unroll
	def "Should not be valid cause of #REASON"() {
		expect:
		!defaultStrategy.isValid(TRADE_DTO, HISTORY_DTOS)

		where:
		REASON                       | TRADE_DTO                                    | HISTORY_DTOS
		"no trade dto"               | null                                         | bidHigherHistoryDtos
		"no history dtos"            | Stub(TradeDto)                               | null
		"no total valid price"       | buildTradeDto(TradeType.ASK, 102000D, 1000D) | bidHigherHistoryDtos
		"no valid unit price change" | buildTradeDto(TradeType.ASK, 100000D, 1000D) | askHigherHistoryDtos
	}

	TradeDto buildTradeDto(TradeType tradeType, double price, double volume) {
		TradeDto tradeDto = new TradeDto()
		tradeDto.setExchange(Exchange.UPBIT)
		tradeDto.setSymbol("KRW-BCH")
		tradeDto.setTradeType(tradeType)
		tradeDto.setPrice(price)
		tradeDto.setVolume(volume)

		return tradeDto
	}

	def "Should get count"() {
		expect:
		defaultStrategy.getCount() == 100
	}
}
