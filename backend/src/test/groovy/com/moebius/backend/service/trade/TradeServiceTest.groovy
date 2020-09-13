package com.moebius.backend.service.trade

import com.moebius.backend.assembler.SlackAssembler
import com.moebius.backend.assembler.TradeAssembler
import com.moebius.backend.domain.commons.Exchange
import com.moebius.backend.dto.trade.AggregatedTradeHistoriesDto
import com.moebius.backend.dto.trade.TradeDto
import com.moebius.backend.service.slack.SlackValve
import com.moebius.backend.service.slack.TradeSlackSender
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.time.LocalDateTime

class TradeServiceTest extends Specification {
	def tradeHistoryService = Mock(TradeHistoryService)
	def tradeSlackSender = Spy(TradeSlackSender, constructorArgs: [Stub(WebClient), Stub(SlackAssembler), Stub(SlackValve)]) as TradeSlackSender
	def tradeAssembler = Mock(TradeAssembler)

	@Subject
	def tradeService = new TradeService(tradeHistoryService, tradeSlackSender, tradeAssembler)

	@Unroll
	def "Should request to send slack message"() {
		when:
		tradeService.identifyValidTrade(getTradeDto(10000D, 1D))

		then:
		1 * tradeHistoryService.getAggregatedTradeHistories(_ as Exchange, _ as String, 1, 2) >> Mono.just(Stub(AggregatedTradeHistoriesDto))
	}

	def "Should identify valid trade"() {
		expect:
		tradeService.isValidTrade(TRADE_DTO) == RESULT

		where:
		TRADE_DTO               || RESULT
		getTradeDto(100D, 1D)   || false
		getTradeDto(10000D, 2D) || true
	}

//	def "Should identify valid trade histories"() {
//
//	}

	TradeDto getTradeDto(double price, double volume) {
		TradeDto tradeDto = new TradeDto()
		tradeDto.setExchange(Exchange.UPBIT)
		tradeDto.setSymbol("KRW-KNU")
		tradeDto.setPrice(price)
		tradeDto.setVolume(volume)

		return tradeDto
	}

//	AggregatedTradeHistoriesDto getHistoryDto(double totalTransactionPrice, double totalTransactionVolume) {
//		return AggregatedTradeHistoriesDto.builder()
//				.totalTransactionPrice(totalTransactionPrice)
//				.totalTransactionVolume(totalTransactionVolume)
//				.startAt(LocalDateTime.now().minusMinutes(5L))
//				.endAt(LocalDateTime.now())
//				.build()
//	}
}
