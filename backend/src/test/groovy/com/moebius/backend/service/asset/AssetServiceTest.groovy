package com.moebius.backend.service.asset

import com.moebius.backend.assembler.AssetAssembler
import com.moebius.backend.domain.apikeys.ApiKey
import com.moebius.backend.domain.commons.Exchange
import com.moebius.backend.dto.exchange.AssetDto
import com.moebius.backend.dto.exchange.upbit.UpbitAssetDto
import com.moebius.backend.dto.frontend.response.AssetResponseDto
import com.moebius.backend.service.exchange.ExchangeServiceFactory
import com.moebius.backend.service.exchange.UpbitService
import com.moebius.backend.service.member.ApiKeyService
import com.moebius.backend.service.member.MemberService
import com.moebius.backend.utils.OrderUtil
import org.springframework.http.HttpStatus
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class AssetServiceTest extends Specification {
	def apiKeyService = Mock(ApiKeyService)
	def memberService = Mock(MemberService)
	def exchangeServiceFactory = Mock(ExchangeServiceFactory)
	def assetAssembler = Mock(AssetAssembler)
	def orderUtil = Mock(OrderUtil)
	def apiKey = Stub(ApiKey) {
		getAccessKey() >> "accessKey"
		getSecretKey() >> "secretKey"
	}
	def memberId = "5d8620bf46e0fb0001d64260"
	def exchange = Exchange.UPBIT

	@Shared
	def upbitAssetDto = UpbitAssetDto.builder()
			.currency("BTC")
			.build()

	@Subject
	def assetService = new AssetService(apiKeyService, memberService, exchangeServiceFactory, assetAssembler, orderUtil)

	@Unroll
	def "Should get asset response when #SITUATION"() {
		given:
		1 * exchangeServiceFactory.getService(exchange) >> Mock(UpbitService) {
			1 * getAuthToken(_ as String, _ as String) >> Mono.just("authToken")
			1 * getAssets(_ as String) >> FLUX_ASSET
		}
		1 * apiKeyService.getApiKeyByMemberIdAndExchange(memberId, exchange) >> Mono.just(apiKey)
		1 * assetAssembler.assembleResponse(_ as List<? extends AssetDto>) >> AssetResponseDto.builder().assets(ASSETS).build()

		expect:
		StepVerifier.create(assetService.getAssetResponse(memberId, exchange))
				.assertNext({
					it.getStatusCode() == HttpStatus.OK
					it.getBody() instanceof AssetResponseDto
					it.getBody().getAssets() instanceof List<? extends AssetDto>
					it.getBody().getAssets().size() == ASSET_SIZE
				})
				.verifyComplete()

		where:
		SITUATION           | FLUX_ASSET               | ASSETS          || ASSET_SIZE
		"there is no asset" | Flux.empty()             | []              || 0
		"there are assets"  | Flux.just(upbitAssetDto) | [upbitAssetDto] || 1
	}

	@Unroll
	def "Should get currency assets when #SITUATION"() {
		given:
		1 * exchangeServiceFactory.getService(exchange) >> Mock(UpbitService) {
			1 * getAuthToken(_ as String, _ as String) >> Mono.just("authToken")
			1 * getAssets(_ as String) >> FLUX_ASSET
		}
		1 * apiKeyService.getApiKeyByMemberIdAndExchange(memberId, exchange) >> Mono.just(apiKey)
		1 * assetAssembler.assembleCurrencyAssets(_ as List<? extends AssetDto>) >> CURRENCY_ASSET

		expect:
		StepVerifier.create(assetService.getCurrencyAssetMap(memberId, exchange))
				.assertNext({
					it instanceof Map<String, AssetDto>
					it.size() == CURRENCY_ASSET_SIZE
				})
				.verifyComplete()

		where:
		SITUATION                    | FLUX_ASSET               | CURRENCY_ASSET         || CURRENCY_ASSET_SIZE
		"there is no currency asset" | Flux.empty()             | [:]                     || 0
		"there are currency assets"  | Flux.just(upbitAssetDto) | ["BTC": upbitAssetDto] || 1
	}
}
