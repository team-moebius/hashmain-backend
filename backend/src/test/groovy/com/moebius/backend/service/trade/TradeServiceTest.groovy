package com.moebius.backend.service.trade

import com.moebius.backend.assembler.SlackAssembler
import com.moebius.backend.assembler.TradeAssembler
import com.moebius.backend.domain.commons.Exchange
import com.moebius.backend.dto.trade.AggregatedTradeHistoriesDto
import com.moebius.backend.dto.trade.AggregatedTradeHistoryDto
import com.moebius.backend.dto.trade.TradeDto
import com.moebius.backend.service.slack.TradeSlackSender
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import spock.lang.Specification
import spock.lang.Subject

class TradeServiceTest extends Specification {
	def tradeHistoryService = Mock(TradeHistoryService)
	def tradeSlackSender = Spy(TradeSlackSender, constructorArgs: [Stub(WebClient), Stub(SlackAssembler)]) as TradeSlackSender
	def tradeAssembler = Mock(TradeAssembler)

	@Subject
	def tradeService = new TradeService(tradeHistoryService, tradeSlackSender, tradeAssembler)

	def "Should request to send slack message if valid trade and valid histories"() {
		given:
		def aggregatedTradeHistoriesDto = Stub(AggregatedTradeHistoriesDto) {
			getAggregatedTradeHistories() >> [getHistoryDto(10D, 2000D, 10000D),
											  getHistoryDto(10000D, 200000D, 11800000D)]
		}

		when:
		tradeService.identifyValidTrade(getTradeDto(10000D, 1D))

		then:
		1 * tradeHistoryService.getAggregatedTradeHistories(_ as Exchange, _ as String, 1, 5) >> Mono.just(aggregatedTradeHistoriesDto)
	}

	def "Should not request to send slack message if invalid trade"() {
		when:
		tradeService.identifyValidTrade(getTradeDto(1000D, 1D))

		then:
		0 * tradeHistoryService.getAggregatedTradeHistories(_ as Exchange, _ as String, 1, 5) >> Mono.just(Stub(AggregatedTradeHistoriesDto))
	}

	TradeDto getTradeDto(double price, double volume) {
		TradeDto tradeDto = new TradeDto()
		tradeDto.setExchange(Exchange.UPBIT)
		tradeDto.setSymbol("KRW-KNU")
		tradeDto.setPrice(price)
		tradeDto.setVolume(volume)

		return tradeDto
	}

	AggregatedTradeHistoryDto getHistoryDto(double totalTransactionVolume, double totalAskPrice, double totalBidPrice) {
		return AggregatedTradeHistoryDto.builder()
				.totalTransactionVolume(totalTransactionVolume)
				.totalAskPrice(totalAskPrice)
				.totalBidPrice(totalBidPrice)
				.build()
	}
}
