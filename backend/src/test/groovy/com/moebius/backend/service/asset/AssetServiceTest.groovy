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
import org.springframework.http.HttpStatus
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import spock.lang.Specification
import spock.lang.Subject

class AssetServiceTest extends Specification {
	def apikeyService = Mock(ApiKeyService)
	def exchangeServiceFactory = Mock(ExchangeServiceFactory)
	def assetAssembler = Mock(AssetAssembler)
	def apiKey = Stub(ApiKey) {
		getAccessKey() >> "accessKey"
		getSecretKey() >> "secretKey"
	}

	@Subject
	def assetService = new AssetService(apikeyService, exchangeServiceFactory, assetAssembler)

	def "Should get asset response"() {
		given:
		def memberId = "5d8620bf46e0fb0001d64260"
		def exchange = Exchange.UPBIT

		and: // mock operation and limit call count
		1 * exchangeServiceFactory.getService(exchange) >> Mock(UpbitService) {
			1 * getAuthToken(_ as String, _ as String) >> Mono.just("authToken")
			1 * getAssets(_ as String) >> Flux.just(UpbitAssetDto.builder().build())
		}
		1 * apikeyService.getApiKeyByMemberIdAndExchange(memberId, exchange) >> Mono.just(apiKey)
		1 * assetAssembler.toResponseDto(_ as List<? extends AssetDto>) >> AssetResponseDto.builder().build()

		expect:
		StepVerifier.create(assetService.getAssetResponse(memberId, exchange))
				.assertNext({
					assert it.getStatusCode() == HttpStatus.OK
					assert it.getBody() instanceof AssetResponseDto
				}).verifyComplete()
	}
}
