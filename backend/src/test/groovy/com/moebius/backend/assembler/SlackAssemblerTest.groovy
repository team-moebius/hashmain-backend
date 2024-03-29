package com.moebius.backend.assembler

import com.moebius.backend.dto.slack.SlackMessageDto
import com.moebius.backend.dto.slack.TradeSlackDto
import com.moebius.backend.utils.OrderUtil
import org.springframework.util.CollectionUtils
import spock.lang.Specification
import spock.lang.Subject

class SlackAssemblerTest extends Specification {
	def orderUtil = Mock(OrderUtil)

	@Subject
	def slackAssembler = new SlackAssembler(orderUtil)

	def "Should assemble slack message"() {
		when:
		def result = slackAssembler.assemble(TradeSlackDto.builder()
				.symbol("KRW-BTC")
				.totalAskPrice(100000000.123)
				.totalBidPrice(1000000000.456)
				.totalValidPrice(900000000)
				.subscribers("<@URPV8KLP6>")
				.build())

		then:
		1 * orderUtil.getUnitCurrencyBySymbol(_ as String) >> "KRW"

		result instanceof SlackMessageDto
		!CollectionUtils.isEmpty(result.getAttachments())
		result.getAttachments().size() == 1
		result.getAttachments().get(0) instanceof SlackMessageDto.SlackAttachment

		and:
		def fields = result.getAttachments().get(0).getFields()
		!CollectionUtils.isEmpty(fields)
		fields.get(0).getValue() == "100,000,000 KRW"
		fields.get(1).getValue() == "1,000,000,000 KRW"
		fields.get(3).getValue() == "<@URPV8KLP6>"
	}
}
