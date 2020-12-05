package com.moebius.backend.service.trade.validator.aggregated

import com.moebius.backend.dto.trade.AggregatedTradeHistoriesDto
import com.moebius.backend.dto.trade.AggregatedTradeHistoryDto
import com.moebius.backend.dto.trade.TradeDto
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class SuddenTurnValidatorTest extends Specification {
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
									  .totalBidPrice(718610420.39023557)
									  .totalTransactionPrice(755940627.98759776)
									  .totalTransactionVolume(9823340.40898105974)
									  .build()]

	@Subject
	def defaultAggregatedStrategy = new SuddenTurnValidator()

	def "Should get time range"() {
		expect:
		defaultAggregatedStrategy.getTimeRange() == 5
	}

	def "Should get time interval"() {
		expect:
		defaultAggregatedStrategy.getTimeInterval() == 1
	}

	def "Should be valid"() {
		given:
		def tradeDto = buildTradeDto(400D)
		def historiesDto = buildHistoriesDto(normalHistoriesDto)

		expect:
		defaultAggregatedStrategy.isValid(tradeDto, historiesDto)
	}

	@Unroll
	def "Should not be valid cause of #REASON"() {
		given:
		def tradeDto = buildTradeDto(PRICE)
		def historiesDto = buildHistoriesDto(HISTORY_DTOS)

		expect:
		!defaultAggregatedStrategy.isValid(tradeDto, historiesDto)

		where:
		REASON                               | PRICE | HISTORY_DTOS
		"lower history count than threshold" | 400D  | []
		"invalid total valid price"          | 400D  | [AggregatedTradeHistoryDto.builder().totalTransactionPrice(969.2123635136)
																.totalTransactionVolume(3028.78863598).build(),
														AggregatedTradeHistoryDto.builder().totalBidPrice(28441.029485256302)
																.totalAskPrice(86184.93783411).build()]
		"invalid price change"               | 400D  | [AggregatedTradeHistoryDto.builder().totalTransactionPrice(729000.22910983)
																.totalTransactionVolume(10).build(),
														AggregatedTradeHistoryDto.builder().totalTransactionPrice(814990.2359519599)
																.totalTransactionVolume(12).build(),
														AggregatedTradeHistoryDto.builder().totalTransactionPrice(4187300.52528619)
																.totalTransactionVolume(60).build(),
														AggregatedTradeHistoryDto.builder().totalTransactionPrice(3598000.246407609964)
																.totalTransactionVolume(50).build(),
														AggregatedTradeHistoryDto.builder()
																.totalBidPrice(10000000)
																.totalAskPrice(0.40898105974)
																.totalTransactionPrice(10000000.40898105974)
																.totalTransactionVolume(141).build()]
		"invalid price change rate change"   | 371D  | normalHistoriesDto
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
