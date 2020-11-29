package com.moebius.backend.service.trade

import com.moebius.backend.assembler.SlackAssembler
import com.moebius.backend.assembler.TradeAssembler
import com.moebius.backend.domain.commons.Exchange
import com.moebius.backend.dto.trade.AggregatedTradeHistoriesDto
import com.moebius.backend.dto.trade.AggregatedTradeHistoryDto
import com.moebius.backend.dto.trade.TradeDto
import com.moebius.backend.dto.trade.TradeHistoryDto
import com.moebius.backend.service.message.TradeSlackMessageSender
import com.moebius.backend.service.slack.TradeSlackSender
import com.moebius.backend.service.trade.strategy.DefaultStrategy
import com.moebius.backend.service.trade.strategy.aggregated.SuddenTurnStrategy
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

class TradeServiceTest extends Specification {
	def tradeStrategies = [Stub(DefaultStrategy) {
		isValid(_ as TradeDto, _ as List) >> true
		getCount() >> 100
	}]
	def tradeAggregatedStrategies = [Stub(SuddenTurnStrategy) {
		getTimeInterval() >> 1
		getTimeRange() >> 6
		isValid(_ as TradeDto, _ as AggregatedTradeHistoriesDto) >> true
	}]
	def tradeHistoryService = Mock(TradeHistoryService)
	def tradeSlackSender = Spy(TradeSlackSender, constructorArgs: [Stub(WebClient), Stub(SlackAssembler), Stub(TradeSlackMessageSender)]) as TradeSlackSender
	def tradeAssembler = Mock(TradeAssembler)

	@Shared
	def uri = UriComponentsBuilder.newInstance().build().toUri()

	@Subject
	def tradeService = new TradeService(tradeStrategies, tradeAggregatedStrategies, tradeHistoryService, tradeSlackSender, tradeAssembler)

	def "Should request to send slack message if valid trade and valid histories"() {
		given:
		def aggregatedTradeHistoriesDto = Stub(AggregatedTradeHistoriesDto) {
			getAggregatedTradeHistories() >> [getHistoryDto(10D, 2000D, 10000D),
											  getHistoryDto(10000D, 200000D, 11800000D)]
		}

		when:
		tradeService.identifyValidTrade(getTradeDto(10000D, 1D))

		then:
		1 * tradeHistoryService.getTradeHistoriesUri(_ as TradeDto, _) >> uri
		1 * tradeHistoryService.getTradeHistories(_ as URI) >> Flux.just(Stub(TradeHistoryDto))
		1 * tradeHistoryService.getAggregatedTradeHistoriesUri(_ as TradeDto, _, _) >> uri
		1 * tradeHistoryService.getAggregatedTradeHistories(_ as URI) >> Mono.just(aggregatedTradeHistoriesDto)
	}

	def "Should not request to send slack message if invalid trade"() {
		when:
		tradeService.identifyValidTrade(getTradeDto(1000D, 1D))

		then:
		0 * tradeHistoryService.getTradeHistoriesUri(_ as TradeDto, _) >> uri
		0 * tradeHistoryService.getTradeHistories(_ as URI) >> Flux.just(Stub(TradeHistoryDto))
		0 * tradeHistoryService.getAggregatedTradeHistoriesUri(_ as TradeDto, _, _) >> uri
		0 * tradeHistoryService.getAggregatedTradeHistories(_ as URI) >> Mono.just(Stub(AggregatedTradeHistoriesDto))
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