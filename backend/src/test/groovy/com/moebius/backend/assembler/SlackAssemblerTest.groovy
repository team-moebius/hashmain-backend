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
				.build())

		then:
		1 * orderUtil.getUnitCurrencyBySymbol(_ as String)
		1 * orderUtil.getTargetCurrencyBySymbol(_ as String)

		result instanceof SlackMessageDto
		!CollectionUtils.isEmpty(result.getAttachments())
		result.getAttachments().size() == 1
	}
}
