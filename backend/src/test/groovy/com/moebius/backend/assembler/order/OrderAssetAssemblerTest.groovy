package com.moebius.backend.assembler.order

import com.moebius.backend.dto.order.OrderAssetDto
import com.moebius.backend.dto.order.OrderDto
import com.moebius.backend.dto.exchange.AssetDto
import com.moebius.backend.dto.frontend.response.OrderAssetResponseDto
import com.moebius.backend.utils.OrderUtil
import spock.lang.Specification
import spock.lang.Subject

class OrderAssetAssemblerTest extends Specification {
	def krwBtcOrder = Stub(OrderDto) {
		getSymbol() >> "KRW-BTC"
	}
	def krwEthOrder = Stub(OrderDto) {
		getSymbol() >> "KRW-ETH"
	}
	def krwXrpOrder = Stub(OrderDto) {
		getSymbol() >> "KRW-XRP"
	}
	def orderUtil = Mock(OrderUtil)

	@Subject
	def orderAssetAssembler = new OrderAssetAssembler(orderUtil)

	def "Should assemble currency to order dtos"() {
		given:
		def orders = [krwBtcOrder, krwBtcOrder, krwEthOrder, krwEthOrder, krwXrpOrder]
		2 * orderUtil.getCurrencyBySymbol("KRW-BTC") >> "BTC"
		2 * orderUtil.getCurrencyBySymbol("KRW-ETH") >> "ETH"
		1 * orderUtil.getCurrencyBySymbol("KRW-XRP") >> "XRP"

		when:
		def result = orderAssetAssembler.assembleCurrencyToOrderDtos(orders)

		then:
		result instanceof Map
		result.size() == 3
	}

	def "Should assemble order asset dto"() {
		given:
		def orders = [krwBtcOrder, krwBtcOrder, krwBtcOrder]
		def asset = ASSET
		def currentPrice = CURRENT_PRICE

		1 * orderUtil.getCurrencyBySymbol(_ as String) >> "BTC"

		when:
		def result = orderAssetAssembler.assembleOrderAssetDto(orders, asset, currentPrice)

		then:
		result instanceof OrderAssetDto
		result.getCurrency() == "BTC"
		result.getEvaluatedPrice() == EXPECTED_EVALUATED_PRICE

		where:
		ASSET                                  | CURRENT_PRICE || EXPECTED_EVALUATED_PRICE
		null                                   | 0D            || 0D
		null                                   | 1000D         || 0D
		Stub(AssetDto) { getBalance() >> 10D } | 0D            || 0D
		Stub(AssetDto) { getBalance() >> 10D } | 1000D         || 10000D
	}

	def "Should assemble status response dto"() {
		when:
		def result = orderAssetAssembler.assembleOrderAssetResponse([Stub(OrderAssetDto), Stub(OrderAssetDto)])

		then:
		result instanceof OrderAssetResponseDto
		result.getOrderAssets() instanceof List
		result.getOrderAssets().size() == 2
	}
}
