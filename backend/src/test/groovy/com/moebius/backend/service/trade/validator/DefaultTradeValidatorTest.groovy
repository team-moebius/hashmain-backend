package com.moebius.backend.service.trade.validator

import com.moebius.backend.domain.commons.Exchange
import com.moebius.backend.domain.commons.TradeType
import com.moebius.backend.dto.trade.TradeDto
import com.moebius.backend.dto.trade.TradeHistoryDto
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class DefaultTradeValidatorTest extends Specification {
	@Shared
	def bidHigherHistoryDtos = [TradeHistoryDto.builder().tradeType(TradeType.BID).price(100000D).volume(1000D).build(),
								TradeHistoryDto.builder().tradeType(TradeType.ASK).price(100000D).volume(900D).build()]
	@Shared
	def askHigherHistoryDtos = [TradeHistoryDto.builder().tradeType(TradeType.ASK).price(100000D).volume(1000D).build(),
								TradeHistoryDto.builder().tradeType(TradeType.BID).price(100000D).volume(900D).build()]
	@Subject
	def defaultStrategy = new DefaultTradeValidator()

	def "Should be valid"() {
		expect:
		defaultStrategy.isValid(TRADE_DTO, HISTORY_DTOS)

		where:
		TRADE_DTO                                  | HISTORY_DTOS
		buildTradeDto(TradeType.BID, 105000D, 10D) | bidHigherHistoryDtos
		buildTradeDto(TradeType.ASK, 95000D, 10D)  | askHigherHistoryDtos
	}

	@Unroll
	def "Should not be valid cause of #REASON"() {
		expect:
		!defaultStrategy.isValid(TRADE_DTO, HISTORY_DTOS)

		where:
		REASON                       | TRADE_DTO                                   | HISTORY_DTOS
		"no trade dto"               | null                                        | bidHigherHistoryDtos
		"no history dtos"            | Stub(TradeDto)                              | null
		"no valid unit price change" | buildTradeDto(TradeType.ASK, 100000D, 100D) | askHigherHistoryDtos
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
