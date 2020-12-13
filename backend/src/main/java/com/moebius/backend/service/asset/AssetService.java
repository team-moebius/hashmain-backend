package com.moebius.backend.service.asset;

import com.moebius.backend.assembler.AssetAssembler;
import com.moebius.backend.domain.apikeys.ApiKey;
import com.moebius.backend.domain.commons.Exchange;
import com.moebius.backend.dto.exchange.AssetDto;
import com.moebius.backend.dto.frontend.response.AssetResponseDto;
import com.moebius.backend.dto.trade.TradeDto;
import com.moebius.backend.service.exchange.ExchangeService;
import com.moebius.backend.service.exchange.ExchangeServiceFactory;
import com.moebius.backend.service.member.ApiKeyService;
import com.moebius.backend.service.member.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.List;
import java.util.Map;

import static com.moebius.backend.utils.ThreadScheduler.COMPUTE;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetService {
	private final ApiKeyService apiKeyService;
	private final MemberService memberService;
	private final ExchangeServiceFactory exchangeServiceFactory;
	private final AssetAssembler assetAssembler;

	@Cacheable(value = "apiKeyWithAssets", key = "{#tradeDto.exchange}")
	public Flux<Tuple2<ApiKey, List<? extends AssetDto>>> getApiKeyWithAssets(TradeDto tradeDto) {
		return memberService.getValidMembers()
			.map(member -> member.getId().toHexString())
			.flatMap(memberId -> apiKeyService.getApiKeyByMemberIdAndExchange(memberId, tradeDto.getExchange()))
			.onErrorContinue((exception, apiKey) -> {})
			.flatMap(apiKey -> Mono.zip(Mono.just(apiKey), getAssets(apiKey, tradeDto)))
			.cache();
	}

	public Mono<ResponseEntity<AssetResponseDto>> getAssetResponse(String memberId, Exchange exchange) {
		return getAssets(memberId, exchange)
			.subscribeOn(COMPUTE.scheduler())
			.map(assetAssembler::assembleResponse)
			.map(ResponseEntity::ok);
	}

	public Mono<Map<String, AssetDto>> getCurrencyAssetMap(String memberId, Exchange exchange) {
		return getAssets(memberId, exchange)
			.subscribeOn(COMPUTE.scheduler())
			.map(assetAssembler::assembleCurrencyAssets);
	}

	private Mono<List<? extends AssetDto>> getAssets(ApiKey apiKey, TradeDto tradeDto) {
		ExchangeService exchangeService = exchangeServiceFactory.getService(tradeDto.getExchange());

		return exchangeService.getAuthToken(apiKey.getAccessKey(), apiKey.getSecretKey())
			.flatMap(authToken -> exchangeService.getAssets(authToken)
				.switchIfEmpty(Mono.defer(Mono::empty))
				.collectList());
	}

	private Mono<List<? extends AssetDto>> getAssets(String memberId, Exchange exchange) {
		ExchangeService exchangeService = exchangeServiceFactory.getService(exchange);

		return apiKeyService.getApiKeyByMemberIdAndExchange(memberId, exchange)
			.flatMap(apiKey -> exchangeService.getAuthToken(apiKey.getAccessKey(), apiKey.getSecretKey()))
			.flatMap(authToken -> exchangeService.getAssets(authToken)
				.switchIfEmpty(Mono.defer(Mono::empty))
				.collectList());
	}
}
