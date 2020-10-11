package com.moebius.backend.service.trade

import com.moebius.backend.assembler.SlackAssembler
import com.moebius.backend.assembler.TradeAssembler
import com.moebius.backend.domain.commons.Exchange
import com.moebius.backend.dto.slack.TradeSlackDto
import com.moebius.backend.dto.trade.AggregatedTradeHistoriesDto
import com.moebius.backend.dto.trade.AggregatedTradeHistoryDto
import com.moebius.backend.dto.trade.TradeDto
import com.moebius.backend.service.slack.TradeSlackSender
import com.moebius.backend.service.trade.strategy.ShortTermStrategy
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

class TradeServiceTest extends Specification {
	def tradeStrategies = [Mock(ShortTermStrategy) {
		getTimeInterval() >> 1
		getTimeRange() >> 6
		isValid(_ as TradeDto, _ as AggregatedTradeHistoriesDto) >> true
	}]
	def tradeHistoryService = Mock(TradeHistoryService)
	def tradeSlackSender = Spy(TradeSlackSender, constructorArgs: [Stub(WebClient), Stub(SlackAssembler)]) as TradeSlackSender
	def tradeAssembler = Mock(TradeAssembler)

	@Shared
	def uri = UriComponentsBuilder.newInstance().build().toUri()

	@Subject
	def tradeService = new TradeService(tradeStrategies, tradeHistoryService, tradeSlackSender, tradeAssembler)

	def "Should request to send slack message if valid trade and valid histories"() {
		given:
		def aggregatedTradeHistoriesDto = Stub(AggregatedTradeHistoriesDto) {
			getAggregatedTradeHistories() >> [getHistoryDto(10D, 2000D, 10000D),
											  getHistoryDto(10000D, 200000D, 11800000D)]
		}

		when:
		tradeService.identifyValidTrade(getTradeDto(10000D, 1D))

		then:
		1 * tradeHistoryService.getAggregatedTradeHistoriesUri(_ as TradeDto, _, _) >> uri
		1 * tradeHistoryService.getAggregatedTradeHistories(_ as URI) >> Mono.just(aggregatedTradeHistoriesDto)
		1 * tradeAssembler.assembleSlackDto(_ as TradeDto, _ as AggregatedTradeHistoriesDto, _ as String) >> Stub(TradeSlackDto)
		1 * tradeSlackSender.sendMessage(_ as TradeSlackDto) >> Mono.just(Stub(ClientResponse))
	}

	def "Should not request to send slack message if invalid trade"() {
		when:
		tradeService.identifyValidTrade(getTradeDto(1000D, 1D))

		then:
		0 * tradeHistoryService.getAggregatedTradeHistoriesUri(_ as TradeDto, _, _) >> uri
		0 * tradeHistoryService.getAggregatedTradeHistories(_ as URI) >> Mono.just(Stub(AggregatedTradeHistoriesDto))
		0 * tradeAssembler.assembleSlackDto(_ as TradeDto, _ as AggregatedTradeHistoriesDto, _ as String) >> Stub(TradeSlackDto)
		0 * tradeSlackSender.sendMessage(_ as TradeSlackDto) >> Mono.just(Stub(ClientResponse))
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