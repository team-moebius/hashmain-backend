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
import com.moebius.backend.utils.OrderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
	private final OrderUtil orderUtil;

	public Flux<Tuple2<ApiKey, AssetDto>> getApiKeyWithAssets(TradeDto tradeDto) {
		return memberService.getValidMembers()
			.map(member -> member.getId().toHexString())
			.flatMap(memberId -> apiKeyService.getApiKeyByMemberIdAndExchange(memberId, tradeDto.getExchange()))
			.onErrorContinue((exception, apiKey) -> {})
			.flatMap(apiKey -> Mono.zip(Mono.just(apiKey), getAsset(apiKey, tradeDto)));
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

	private Mono<AssetDto> getAsset(ApiKey apiKey, TradeDto tradeDto) {
		ExchangeService exchangeService = exchangeServiceFactory.getService(tradeDto.getExchange());

		return exchangeService.getAuthToken(apiKey.getAccessKey(), apiKey.getSecretKey())
			.flatMap(authToken -> exchangeService.getAssets(authToken)
				.switchIfEmpty(Mono.defer(Mono::empty))
				.collectList())
			.map(assetDtos -> assetDtos.stream()
				.filter(asset -> StringUtils.equals(asset.getCurrency(), orderUtil.getTargetCurrencyBySymbol(tradeDto.getSymbol())))
				.findFirst().orElse(null));
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
