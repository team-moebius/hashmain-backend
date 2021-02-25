package com.moebius.backend.service.member

import com.moebius.backend.assembler.ApiKeyAssembler
import com.moebius.backend.domain.apikeys.ApiKey
import com.moebius.backend.domain.apikeys.ApiKeyRepository
import com.moebius.backend.domain.commons.Exchange
import com.moebius.backend.dto.frontend.ApiKeyDto
import com.moebius.backend.dto.frontend.response.ApiKeyResponseDto
import com.moebius.backend.exception.DataNotFoundException
import com.moebius.backend.exception.DuplicatedDataException
import com.moebius.backend.service.exchange.ExchangeServiceFactory
import com.moebius.backend.service.exchange.UpbitService
import com.mongodb.DuplicateKeyException
import org.bson.types.ObjectId
import org.slf4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientResponse
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.lang.reflect.Field
import java.lang.reflect.Modifier

class ApiKeyServiceTest extends Specification {
	def apiKeyRepository = Mock(ApiKeyRepository)
	def apiKeyAssembler = Mock(ApiKeyAssembler)
	def exchangeServiceFactory = Mock(ExchangeServiceFactory)
	def memberId = "5d8620bf46e0fb0001d64265"
	def apiKeyId = "5eb6bbb09ee8a71d5ef5b3d8"
	def exchange = Exchange.UPBIT

	@Subject
	def apiKeyService = new ApiKeyService(
			apiKeyRepository,
			apiKeyAssembler,
			exchangeServiceFactory
	)

	def setup() {
		Field field = ApiKeyService.class.getDeclaredField("log")
		field.setAccessible(true)

		Field modifiersField = Field.class.getDeclaredField("modifiers")
		modifiersField.setAccessible(true)
		modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL)

		field.set(null, Mock(Logger))
	}

	def "Should verify and create api key"() {
		given:
		def apiKeyDto = buildApiKeyDto()
		def memberId = "5d8620bf46e0fb0001d64265"
		def upbitService = Mock(UpbitService)

		and:
		1 * exchangeServiceFactory.getService(_ as Exchange) >> upbitService
		1 * upbitService.getAuthToken(_ as String, _ as String) >> Mono.just("dummyToken")
		1 * upbitService.checkHealth(_ as String) >> Mono.just(Stub(ClientResponse))
		1 * apiKeyAssembler.assembleApiKey(_ as ApiKeyDto, _ as String) >> Stub(ApiKey)
		1 * apiKeyRepository.save(_ as ApiKey) >> Mono.just(Stub(ApiKey))
		1 * apiKeyAssembler.assembleResponse(_ as ApiKey) >> Stub(ApiKeyResponseDto)

		expect:
		StepVerifier.create(apiKeyService.verifyAndCreateApiKey(apiKeyDto, memberId))
				.assertNext({
					assert it != null
					assert it.getStatusCode() == HttpStatus.OK
					assert it.getBody() instanceof ApiKeyResponseDto
				})
				.verifyComplete()
	}

	@Unroll
	def "Should not verify and create api key cause of #REASON"() {
		given:
		def apiKeyDto = buildApiKeyDto()
		def upbitService = Mock(UpbitService)

		and:
		1 * exchangeServiceFactory.getService(_ as Exchange) >> upbitService
		1 * upbitService.getAuthToken(_ as String, _ as String) >> Mono.just("dummyToken")
		1 * upbitService.checkHealth(_ as String) >> Mono.just(Stub(ClientResponse))
		1 * apiKeyAssembler.assembleApiKey(_ as ApiKeyDto, _ as String) >> Stub(ApiKey)
		1 * apiKeyRepository.save(_ as ApiKey) >> Mono.error(EXCEPTION)

		expect:
		StepVerifier.create(apiKeyService.verifyAndCreateApiKey(apiKeyDto, memberId))
				.verifyError(RESULT_EXCEPTION)

		where:
		REASON               | EXCEPTION                   || RESULT_EXCEPTION
		"duplicated api key" | Stub(DuplicateKeyException) || DuplicatedDataException.class
		"something wrong"    | Stub(Exception)             || Exception.class
	}

	def "Should get api keys by member id"() {
		given:
		1 * apiKeyRepository.findAllByMemberId(_ as ObjectId) >> Flux.just(Stub(ApiKey), Stub(ApiKey))
		2 * apiKeyAssembler.assembleResponse(_ as ApiKey) >> Stub(ApiKeyResponseDto)

		expect:
		StepVerifier.create(apiKeyService.getApiKeysByMemberId(memberId))
				.assertNext({
					assert it != null
					assert it.getStatusCode() == HttpStatus.OK
					assert it.getBody().size() == 2
					assert it.getBody().get(0) instanceof ApiKeyResponseDto
				})
				.verifyComplete()
	}

	def "Should not get api keys by member id cause of not found"() {
		given:
		1 * apiKeyRepository.findAllByMemberId(_ as ObjectId) >> Flux.empty()

		expect:
		StepVerifier.create(apiKeyService.getApiKeysByMemberId(memberId))
				.verifyError(DataNotFoundException.class)
	}

	def "Should delete api key by id"() {
		given:
		1 * apiKeyRepository.deleteByIdAndMemberId(_ as ObjectId, _ as ObjectId) >> Mono.just(new Void())

		expect:
		StepVerifier.create(apiKeyService.deleteApiKeyById(apiKeyId, memberId))
				.assertNext({
					assert it != null
					assert it.getStatusCode() == HttpStatus.OK
				})
				.verifyComplete()
	}

	def "Should not delete api key by id cause of not found"() {
		given:
		1 * apiKeyRepository.deleteByIdAndMemberId(_ as ObjectId, _ as ObjectId) >> Mono.error(Stub(Exception))
		1 * apiKeyService.log.error(_ as String, _ as Throwable)

		expect:
		StepVerifier.create(apiKeyService.deleteApiKeyById(apiKeyId, memberId))
				.verifyError(DataNotFoundException.class)
	}

	def "Should get api key by member id and exchange"() {
		given:
		1 * apiKeyRepository.findByMemberIdAndExchange(_ as ObjectId, _ as Exchange) >> Mono.just(Stub(ApiKey))

		expect:
		StepVerifier.create(apiKeyService.getApiKeyByMemberIdAndExchange(memberId, exchange))
				.assertNext({
					assert it instanceof ApiKey
				})
				.verifyComplete()
	}

	def "Should not get api key by member id and exchange cause of not found"() {
		given:
		1 * apiKeyRepository.findByMemberIdAndExchange(_ as ObjectId, _ as Exchange) >> Mono.empty()

		expect:
		StepVerifier.create(apiKeyService.getApiKeyByMemberIdAndExchange(memberId, exchange))
				.verifyError(DataNotFoundException.class)
	}

	def "Should get api key by id"() {
		given:
		1 * apiKeyRepository.findById(_ as ObjectId) >> Mono.just(Stub(ApiKey))

		expect:
		StepVerifier.create(apiKeyService.getApiKeyById(apiKeyId))
				.assertNext({
					assert it instanceof ApiKey
				})
				.verifyComplete()
	}

	def "Should not get api key by id cause of not found"() {
		given:
		1 * apiKeyRepository.findById(_ as ObjectId) >> Mono.empty()

		expect:
		StepVerifier.create(apiKeyService.getApiKeyById(apiKeyId))
				.verifyError(DataNotFoundException.class)
	}

	ApiKeyDto buildApiKeyDto() {
		ApiKeyDto apiKeyDto = new ApiKeyDto()
		apiKeyDto.setExchange(Exchange.UPBIT)
		apiKeyDto.setName("test")
		apiKeyDto.setAccessKey("yst5NnpqXfrMNhPW4bcdU9NBH948gqT8xUV2VC6L")
		apiKeyDto.setSecretKey("GOOSa892kjNtp3ZeTjuxJxTWaV8aKrMfoVnthmKa")

		return apiKeyDto
	}
}
