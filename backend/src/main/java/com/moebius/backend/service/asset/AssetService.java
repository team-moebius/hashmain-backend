package com.moebius.backend.service.asset;

import com.moebius.backend.assembler.AssetAssembler;
import com.moebius.backend.domain.commons.Exchange;
import com.moebius.backend.dto.exchange.AssetDto;
import com.moebius.backend.dto.frontend.response.AssetResponseDto;
import com.moebius.backend.service.exchange.ExchangeService;
import com.moebius.backend.service.exchange.ExchangeServiceFactory;
import com.moebius.backend.service.member.ApiKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static com.moebius.backend.utils.ThreadScheduler.COMPUTE;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetService {
	private final ApiKeyService apiKeyService;
	private final ExchangeServiceFactory exchangeServiceFactory;
	private final AssetAssembler assetAssembler;

	public Mono<ResponseEntity<AssetResponseDto>> getAssetResponse(String memberId, Exchange exchange) {
		return getAssets(memberId, exchange)
			.subscribeOn(COMPUTE.scheduler())
			.map(assetAssembler::toResponseDto)
			.map(ResponseEntity::ok);
	}

	public Mono<Map<String, AssetDto>> getCurrencyAssets(String memberId, Exchange exchange) {
		return getAssets(memberId, exchange)
			.subscribeOn(COMPUTE.scheduler())
			.map(assetAssembler::toCurrencyAssetDtos);
	}

	private Mono<List<? extends AssetDto>> getAssets(String memberId, Exchange exchange) {
		ExchangeService exchangeService = exchangeServiceFactory.getService(exchange);

		return apiKeyService.getExchangeAuthToken(memberId, exchange)
			.subscribeOn(COMPUTE.scheduler())
			.flatMap(authToken -> exchangeService.getAssets(authToken)
				.collectList());
	}
}