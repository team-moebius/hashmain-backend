package com.moebius.backend.service.trade

import com.moebius.backend.assembler.SlackAssembler
import com.moebius.backend.assembler.TradeAssembler
import com.moebius.backend.dto.TradeDto
import com.moebius.backend.dto.slack.TradeSlackDto
import com.moebius.backend.service.slack.TradeSlackSender
import org.springframework.web.reactive.function.client.WebClient
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class TradeServiceTest extends Specification {
	def tradeSlackSender = Spy(TradeSlackSender, constructorArgs: [Stub(WebClient), Stub(SlackAssembler)]) as TradeSlackSender
	def tradeAssembler = Mock(TradeAssembler)

	@Subject
	def tradeService = new TradeService(tradeSlackSender, tradeAssembler)

	@Unroll
	def "Should identify valid trade"() {
		given:
		def tradeDto = TRADE_DTO

		when:
		tradeService.identifyValidTrade(tradeDto)

		then:
		ASSEMBLE_COUNT * tradeAssembler.assembleSlackDto(tradeDto, _ as Double) >> Stub(TradeSlackDto)

		where:
		TRADE_DTO               || ASSEMBLE_COUNT
		getTradeDto(100D, 100D) || 0
		getTradeDto(97D, 100D)  || 1
		getTradeDto(103D, 100D) || 1
	}

	TradeDto getTradeDto(double price, double prevClosingPrice) {
		TradeDto tradeDto = new TradeDto()
		tradeDto.setPrice(price)
		tradeDto.setPrevClosingPrice(prevClosingPrice)

		return tradeDto
	}
}
