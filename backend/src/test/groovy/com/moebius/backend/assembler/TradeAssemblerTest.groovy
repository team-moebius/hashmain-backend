package com.moebius.backend.assembler

import com.moebius.backend.domain.commons.TradeType
import com.moebius.backend.dto.slack.TradeSlackDto
import com.moebius.backend.dto.trade.AggregatedTradeHistoriesDto
import com.moebius.backend.dto.trade.AggregatedTradeHistoryDto
import com.moebius.backend.dto.trade.TradeDto
import com.moebius.backend.dto.trade.TradeHistoryDto
import spock.lang.Specification
import spock.lang.Subject

import java.time.LocalDateTime
import java.time.ZonedDateTime

class TradeAssemblerTest extends Specification {
	@Subject
	def tradeAssembler = new TradeAssembler()

	def "Should assemble slack dto by aggregated trades"() {
		given:
		def historiesDto = Stub(AggregatedTradeHistoriesDto) {
			getAggregatedTradeHistories() >> [AggregatedTradeHistoryDto.builder()
													  .totalTransactionPrice(100000)
													  .totalTransactionVolume(10)
													  .startTime(ZonedDateTime.now().minusMinutes(5))
													  .endTime(ZonedDateTime.now())
													  .build(),
											  AggregatedTradeHistoryDto.builder()
													  .totalTransactionPrice(100000)
													  .totalTransactionVolume(10)
													  .startTime(ZonedDateTime.now().minusMinutes(5))
													  .endTime(ZonedDateTime.now())
													  .build()]
		}
		
		when:
		def result = tradeAssembler.assembleByAggregatedTrade(Stub(TradeDto), historiesDto, "test", "testSubscriber")

		then:
		result instanceof TradeSlackDto
		result.getReferenceLink() == "test"
		result.getSubscribers() == "testSubscriber"
	}

	def "Should assemble slack dto by trades"() {
		given:
		def tradeDto = Stub(TradeDto) {
			getCreatedAt() >> LocalDateTime.now()
		}
		def historyDtos = [TradeHistoryDto.builder()
								   .tradeType(TradeType.ASK)
								   .price(10000D)
								   .volume(10D)
								   .createdAt(LocalDateTime.now().minusMinutes(10))
								   .build(),
						   TradeHistoryDto.builder()
								   .tradeType(TradeType.BID)
								   .price(10100D)
								   .volume(10D)
								   .createdAt(LocalDateTime.now().minusMinutes(1))
								   .build()]

		when:
		def result = tradeAssembler.assembleByTrade(tradeDto, historyDtos, "test")

		then:
		result instanceof  TradeSlackDto
		result.getTotalAskPrice() == 100000D
		result.getTotalBidPrice() == 101000D
		result.getTotalValidPrice() == 1000
		result.getReferenceLink() == "test"
	}
}
