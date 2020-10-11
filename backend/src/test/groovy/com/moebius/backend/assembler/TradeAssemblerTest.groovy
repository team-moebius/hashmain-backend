package com.moebius.backend.assembler

import com.moebius.backend.dto.slack.TradeSlackDto
import com.moebius.backend.dto.trade.AggregatedTradeHistoriesDto
import com.moebius.backend.dto.trade.AggregatedTradeHistoryDto
import com.moebius.backend.dto.trade.TradeDto
import spock.lang.Specification
import spock.lang.Subject

import java.time.ZonedDateTime

class TradeAssemblerTest extends Specification {
	@Subject
	def tradeAssembler = new TradeAssembler()

	def "Should assemble trade slack"() {
		given:
		def historiesDto = Stub(AggregatedTradeHistoriesDto) {
			getAggregatedTradeHistories() >> [AggregatedTradeHistoryDto.builder()
													  .startTime(ZonedDateTime.now())
													  .endTime(ZonedDateTime.now())
													  .build()]
		}
		
		when:
		def result = tradeAssembler.assembleSlackDto(Stub(TradeDto), historiesDto, "test")

		then:
		result instanceof TradeSlackDto
		result.getReferenceLink() == "test"
	}
}
