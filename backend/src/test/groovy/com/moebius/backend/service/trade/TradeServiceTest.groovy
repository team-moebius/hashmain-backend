package com.moebius.backend.service.trade

import com.moebius.backend.assembler.SlackAssembler
import com.moebius.backend.assembler.TradeAssembler
import com.moebius.backend.domain.commons.Exchange
import com.moebius.backend.dto.trade.AggregatedTradeHistoryDto
import com.moebius.backend.dto.trade.TradeDto
import com.moebius.backend.service.slack.TradeSlackSender
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.time.LocalDateTime

class TradeServiceTest extends Specification {
	def tradeHistoryService = Mock(TradeHistoryService)
	def tradeSlackSender = Spy(TradeSlackSender, constructorArgs: [Stub(WebClient), Stub(SlackAssembler)]) as TradeSlackSender
	def tradeAssembler = Mock(TradeAssembler)

	@Subject
	def tradeService = new TradeService(tradeHistoryService, tradeSlackSender, tradeAssembler)

	@Unroll
	def "Should request to send slack message"() {
		when:
		tradeService.identifyValidTrade(Stub(TradeDto))

		then:
		1 * tradeHistoryService.getAggregatedTradeHistoryDto(_ as Exchange, _ as String, 10) >> Mono.just(Stub(AggregatedTradeHistoryDto))
	}

	def "Should identify valid trade"() {
		expect:
		tradeService.isValidTrade(TRADE_DTO, HISTORY_DTO) == RESULT

		where:
		TRADE_DTO             | HISTORY_DTO                || RESULT
		getTradeDto(100D, 1D) | null                       || false
		getTradeDto(100D, 1D) | getHistoryDto(1000D, 0D)   || false
		getTradeDto(100D, 1D) | getHistoryDto(1000D, 10D)  || false
		getTradeDto(100D, 2D) | getHistoryDto(990D, 10D)   || false
		getTradeDto(200D, 2D) | getHistoryDto(490D, 10D)   || true
		getTradeDto(100D, 2D) | getHistoryDto(10000D, 10D) || true
	}

	TradeDto getTradeDto(double price, double volume) {
		TradeDto tradeDto = new TradeDto()
		tradeDto.setExchange(Exchange.UPBIT)
		tradeDto.setSymbol("KRW-KNU")
		tradeDto.setPrice(price)
		tradeDto.setVolume(volume)

		return tradeDto
	}

	AggregatedTradeHistoryDto getHistoryDto(double totalTransactionPrice, double totalTransactionVolume) {
		return AggregatedTradeHistoryDto.builder()
				.totalTransactionPrice(totalTransactionPrice)
				.totalTransactionVolume(totalTransactionVolume)
				.startAt(LocalDateTime.now().minusMinutes(5L))
				.endAt(LocalDateTime.now())
				.build()
	}
}
