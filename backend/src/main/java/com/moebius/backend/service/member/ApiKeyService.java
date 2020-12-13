package com.moebius.backend.service.member;

import com.moebius.backend.assembler.ApiKeyAssembler;
import com.moebius.backend.domain.apikeys.ApiKey;
import com.moebius.backend.domain.apikeys.ApiKeyRepository;
import com.moebius.backend.domain.commons.Exchange;
import com.moebius.backend.dto.frontend.ApiKeyDto;
import com.moebius.backend.dto.frontend.response.ApiKeyResponseDto;
import com.moebius.backend.exception.DataNotFoundException;
import com.moebius.backend.exception.DuplicatedDataException;
import com.moebius.backend.exception.ExceptionTypes;
import com.moebius.backend.service.exchange.ExchangeService;
import com.moebius.backend.service.exchange.ExchangeServiceFactory;
import com.moebius.backend.utils.Verifier;
import com.mongodb.DuplicateKeyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.moebius.backend.utils.ThreadScheduler.COMPUTE;
import static com.moebius.backend.utils.ThreadScheduler.IO;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {
	private final ApiKeyRepository apiKeyRepository;
	private final ApiKeyAssembler apiKeyAssembler;
	private final ExchangeServiceFactory exchangeServiceFactory;

	public Mono<ResponseEntity<ApiKeyResponseDto>> verifyAndCreateApiKey(ApiKeyDto apiKeyDto, String memberId) {
		Verifier.checkNullFields(apiKeyDto);
		Verifier.checkBlankString(memberId);

		return verifyApiKey(apiKeyDto)
			.subscribeOn(COMPUTE.scheduler())
			.publishOn(IO.scheduler())
			.flatMap(clientResponse -> createApiKey(apiKeyDto, memberId));
	}

	public Mono<ResponseEntity<List<ApiKeyResponseDto>>> getApiKeysByMemberId(String memberId) {
		Verifier.checkBlankString(memberId);

		return apiKeyRepository.findAllByMemberId(new ObjectId(memberId))
			.subscribeOn(IO.scheduler())
			.publishOn(COMPUTE.scheduler())
			.switchIfEmpty(Mono.defer(() -> Mono.error(new DataNotFoundException(
				ExceptionTypes.NONEXISTENT_DATA.getMessage("[ApiKey] Api key based on memberId(" + memberId + ")")))))
			.map(apiKeyAssembler::assembleResponse)
			.collectList()
			.map(ResponseEntity::ok);
	}

	public Mono<ResponseEntity<String>> deleteApiKeyById(String id, String memberId) {
		Verifier.checkBlankString(id);
		Verifier.checkBlankString(memberId);

		return apiKeyRepository.deleteByIdAndMemberId(new ObjectId(id), new ObjectId(memberId))
			.subscribeOn(IO.scheduler())
			.publishOn(COMPUTE.scheduler())
			.onErrorMap(exception -> {
				log.error("[ApiKey] Deletion failed.", exception);
				return new DataNotFoundException(ExceptionTypes.NONEXISTENT_DATA.getMessage("[ApiKey] Api key"));
			})
			.map(aVoid -> ResponseEntity.ok(id));
	}

	public Mono<ApiKey> getApiKeyByMemberIdAndExchange(String memberId, Exchange exchange) {
		Verifier.checkBlankString(memberId);
		Verifier.checkNullFields(exchange);

		return apiKeyRepository.findByMemberIdAndExchange(new ObjectId(memberId), exchange)
			.subscribeOn(IO.scheduler())
			.publishOn(COMPUTE.scheduler())
			.switchIfEmpty(Mono.defer(() -> Mono.error(new DataNotFoundException(
				ExceptionTypes.NONEXISTENT_DATA.getMessage("[ApiKey] Api key based on memberId(" + memberId + ") and exchange(" + exchange + ")")))));
	}

	public Mono<ApiKey> getApiKeyById(String id) {
		Verifier.checkBlankString(id);

		return apiKeyRepository.findById(new ObjectId(id))
			.subscribeOn(IO.scheduler())
			.publishOn(COMPUTE.scheduler())
			.switchIfEmpty(Mono.defer(() -> Mono.error(new DataNotFoundException(
				ExceptionTypes.NONEXISTENT_DATA.getMessage("[ApiKey] Api key based on id(" + id + ")")))));
	}

	private Mono<ClientResponse> verifyApiKey(ApiKeyDto apiKeyDto) {
		log.info("[ApiKey] Start to verify api key. [{}]", apiKeyDto);

		ExchangeService exchangeService = exchangeServiceFactory.getService(apiKeyDto.getExchange());
		return exchangeService.getAuthToken(apiKeyDto.getAccessKey(), apiKeyDto.getSecretKey())
			.flatMap(exchangeService::checkHealth);
	}

	private Mono<ResponseEntity<ApiKeyResponseDto>> createApiKey(ApiKeyDto apiKeyDto, String memberId) {
		log.info("[ApiKey] Start to create api key. [{}]", apiKeyDto);

		return apiKeyRepository.save(apiKeyAssembler.assembleApiKey(apiKeyDto, memberId))
			.subscribeOn(IO.scheduler())
			.publishOn(COMPUTE.scheduler())
			.onErrorMap(exception -> exception instanceof DuplicateKeyException ?
				new DuplicatedDataException(ExceptionTypes.DUPLICATED_DATA.getMessage(apiKeyDto.getName())) :
				exception)
			.map(apiKeyAssembler::assembleResponse)
			.map(ResponseEntity::ok);
	}
}
