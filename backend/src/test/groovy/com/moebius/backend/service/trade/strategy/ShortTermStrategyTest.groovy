package com.moebius.backend.service.trade.strategy

import com.moebius.backend.dto.trade.AggregatedTradeHistoriesDto
import com.moebius.backend.dto.trade.AggregatedTradeHistoryDto
import com.moebius.backend.dto.trade.TradeDto
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class ShortTermStrategyTest extends Specification {
	@Shared
	def normalHistoriesDto = [AggregatedTradeHistoryDto.builder()
									  .totalAskPrice(2422529.57975545)
									  .totalBidPrice(257805.09729636)
									  .totalTransactionPrice(2680334.67705181)
									  .totalTransactionVolume(7290.22910983)
									  .build(),
							  AggregatedTradeHistoryDto.builder()
									  .totalAskPrice(22842040.314775176)
									  .totalBidPrice(7089009.158298049)
									  .totalTransactionPrice(29931049.473073233)
									  .totalTransactionVolume(81499.2359519599)
									  .build(),
							  AggregatedTradeHistoryDto.builder()
									  .totalAskPrice(6902879.62614532)
									  .totalBidPrice(8889010.722647075)
									  .totalTransactionPrice(15791890.348792398)
									  .totalTransactionVolume(41873.52528619)
									  .build(),
							  AggregatedTradeHistoryDto.builder()
									  .totalAskPrice(8141444.815369129)
									  .totalBidPrice(5298995.127673092)
									  .totalTransactionPrice(13440439.943042208)
									  .totalTransactionVolume(35980.246407609964)
									  .build(),
							  AggregatedTradeHistoryDto.builder()
									  .totalAskPrice(37330207.59736219)
									  .totalBidPrice(71861042.39023557)
									  .totalTransactionPrice(109191249.98759773)
									  .totalTransactionVolume(282334.40898105974)
									  .build(),
							  AggregatedTradeHistoryDto.builder().build()]

	@Subject
	def shortTermStrategy = new ShortTermStrategy()

	def "Should get time range"() {
		expect:
		shortTermStrategy.getTimeRange() == 6
	}

	def "Should get time interval"() {
		expect:
		shortTermStrategy.getTimeInterval() == 1
	}

	def "Should be valid"() {
		given:
		def tradeDto = buildTradeDto(400D)
		def historiesDto = buildHistoriesDto(normalHistoriesDto)

		expect:
		shortTermStrategy.isValid(tradeDto, historiesDto)
	}

	@Unroll
	def "Should not be valid cause of #REASON"() {
		given:
		def tradeDto = buildTradeDto(PRICE)
		def historiesDto = buildHistoriesDto(HISTORY_DTOS)

		expect:
		!shortTermStrategy.isValid(tradeDto, historiesDto)

		where:
		REASON                               | PRICE | HISTORY_DTOS
		"lower history count than threshold" | 400D  | []
		"invalid volume change"              | 400D  | [AggregatedTradeHistoryDto.builder().totalTransactionVolume(7290.22910983).build(),
														AggregatedTradeHistoryDto.builder().totalTransactionVolume(81499.2359519599).build(),
														AggregatedTradeHistoryDto.builder().totalTransactionVolume(41873.52528619).build(),
														AggregatedTradeHistoryDto.builder().totalTransactionVolume(35980.246407609964).build(),
														AggregatedTradeHistoryDto.builder().totalTransactionVolume(70000.40898105974).build(),
														AggregatedTradeHistoryDto.builder().build()]
		"total invalid price"                | 400D  | [AggregatedTradeHistoryDto.builder().totalTransactionVolume(7290.22910983).build(),
														AggregatedTradeHistoryDto.builder().totalTransactionVolume(81499.2359519599).build(),
														AggregatedTradeHistoryDto.builder().totalTransactionVolume(41873.52528619).build(),
														AggregatedTradeHistoryDto.builder().totalTransactionVolume(35980.246407609964).build(),
														AggregatedTradeHistoryDto.builder().totalTransactionVolume(282334.40898105974).build(),
														AggregatedTradeHistoryDto.builder().build()]
		"invalid price change"               | 371D  | normalHistoriesDto
	}

	TradeDto buildTradeDto(double price) {
		def tradeDto = new TradeDto()
		tradeDto.setPrice(price)

		return tradeDto
	}

	AggregatedTradeHistoriesDto buildHistoriesDto(List<AggregatedTradeHistoryDto> historyDtos) {
		def historiesDto = AggregatedTradeHistoriesDto.builder()
				.aggregatedTradeHistories(historyDtos)
				.build()
		return historiesDto
	}
}
