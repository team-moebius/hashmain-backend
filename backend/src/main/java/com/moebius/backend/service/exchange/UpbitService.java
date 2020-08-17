package com.moebius.backend.service.exchange;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.moebius.backend.assembler.exchange.UpbitAssembler;
import com.moebius.backend.domain.apikeys.ApiKey;
import com.moebius.backend.domain.commons.Exchange;
import com.moebius.backend.domain.orders.Order;
import com.moebius.backend.dto.order.OrderStatusDto;
import com.moebius.backend.dto.exchange.AssetDto;
import com.moebius.backend.dto.exchange.upbit.UpbitAssetDto;
import com.moebius.backend.dto.exchange.upbit.UpbitOrderStatusDto;
import com.moebius.backend.exception.ExceptionTypes;
import com.moebius.backend.exception.WrongDataException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import static com.moebius.backend.utils.ThreadScheduler.COMPUTE;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpbitService implements ExchangeService {
	@Value("${exchange.upbit.rest.public-uri}")
	private String publicUri;
	@Value("${exchange.upbit.rest.asset}")
	private String assetUri;
	@Value("${exchange.upbit.rest.orders}")
	private String ordersUri;
	@Value("${exchange.upbit.rest.order}")
	private String orderUri;
	@Value("${exchange.upbit.rest.identifier}")
	private String identifierUri;
	@Value("${exchange.upbit.message-digest.hash-algorithm}")
	private String messageDigestHashAlgorithm;
	@Value("${exchange.upbit.message-digest.charset}")
	private String messageDigestCharset;

	private final WebClient webClient;
	private final UpbitAssembler upbitAssembler;

	@Override
	public Exchange getExchange() {
		return Exchange.UPBIT;
	}

	@Override
	public Mono<String> getAuthToken(String accessKey, String secretKey) {
		log.info("[Upbit] Start to get auth token. [accessKey: {}, secretKey: {}]", accessKey, secretKey);

		return Mono.fromCallable(() -> {
			Algorithm algorithm = Algorithm.HMAC256(secretKey);
			return JWT.create()
				.withClaim("access_key", accessKey)
				.withClaim("nonce", UUID.randomUUID().toString())
				.sign(algorithm);
		}).subscribeOn(COMPUTE.scheduler());
	}

	@Override
	public Flux<? extends AssetDto> getAssets(String authToken) {
		return webClient.get()
			.uri(publicUri + assetUri)
			.headers(httpHeaders -> httpHeaders.setBearerAuth(authToken))
			.retrieve()
			.bodyToFlux(UpbitAssetDto.class);
	}

	@Override
	public Mono<ClientResponse> checkHealth(String authToken) {
		log.info("[Upbit] Start to do health check. [authToken: {}]", authToken);

		return webClient.get()
			.uri(publicUri + assetUri)
			.headers(httpHeaders -> httpHeaders.setBearerAuth(authToken))
			.exchange()
			.filter(clientResponse -> clientResponse.statusCode() == HttpStatus.OK)
			.switchIfEmpty(Mono.defer(() -> Mono.error(new WrongDataException(ExceptionTypes.UNVERIFIED_DATA.getMessage("Entered access key and secret key")))));
	}

	@Override
	public Mono<ClientResponse> requestOrder(ApiKey apiKey, Order order) {
		String queryParameters = upbitAssembler.assembleOrderParameters(order);
		String token = getAuthTokenWithParameter(apiKey, queryParameters);

		log.info("[Upbit] Start to request order. [{}]", order);
		return webClient.post()
			.uri(publicUri + ordersUri)
			.contentType(MediaType.APPLICATION_JSON)
			.headers(httpHeaders -> httpHeaders.setBearerAuth(token))
			.bodyValue(upbitAssembler.assembleOrder(order))
			.exchange()
			.doOnError(exception -> log.error("[Upbit] Failed to request order.", exception))
			.doOnSuccess(clientResponse -> log.info("[Upbit] Succeeded to request order. [Response code : {}]", clientResponse.statusCode()));
	}

	@Override
	public Mono<ClientResponse> cancelOrder(ApiKey apiKey, String orderId) {
		String token = getAuthTokenWithParameter(apiKey, identifierUri + orderId);

		log.info("[Upbit] Start to cancel order. [orderId : {}]", orderId);
		return webClient.delete()
			.uri(publicUri + orderUri + identifierUri + orderId)
			.headers(httpHeaders -> httpHeaders.setBearerAuth(token))
			.exchange()
			.doOnError(exception -> log.error("[Upbit] Failed to cancel order.", exception))
			.doOnSuccess(clientResponse -> log.info("[Upbit] Succeeded to cancel order. [Response code : {}]", clientResponse.statusCode()));
	}

	@Override
	public Mono<OrderStatusDto> getCurrentOrderStatus(ApiKey apiKey, String orderId) {
		String token = getAuthTokenWithParameter(apiKey, identifierUri + orderId);

		log.info("[Upbit] Start to get current order status. [orderId : {}])", orderId);
		return webClient.get()
			.uri(publicUri + orderUri + identifierUri + orderId)
			.headers(httpHeaders -> httpHeaders.setBearerAuth(token))
			.retrieve()
			.onStatus(HttpStatus.UNAUTHORIZED::equals,
				response -> Mono.error(new WrongDataException(ExceptionTypes.UNVERIFIED_DATA.getMessage("Auth token (" + apiKey + ")"))))
			.bodyToMono(UpbitOrderStatusDto.class)
			.map(upbitOrderStatusDto -> upbitAssembler.assembleOrderStatus(orderId, upbitOrderStatusDto));
	}

	private String getAuthTokenWithParameter(ApiKey apiKey, String query) {
		MessageDigest messageDigest;
		try {
			messageDigest = MessageDigest.getInstance(messageDigestHashAlgorithm);
			messageDigest.update(query.getBytes(messageDigestCharset));
		} catch (NoSuchAlgorithmException e) {
			log.error("[Upbit] Cannot find message digest hash algorithm.", e);
			return StringUtils.EMPTY;
		} catch (UnsupportedEncodingException e) {
			log.error("[Upbit] Cannot support encoding.", e);
			return StringUtils.EMPTY;
		}

		String queryHash = String.format("%0128x", new BigInteger(1, messageDigest.digest()));

		Algorithm algorithm = Algorithm.HMAC256(apiKey.getSecretKey());

		return JWT.create()
			.withClaim("access_key", apiKey.getAccessKey())
			.withClaim("nonce", UUID.randomUUID().toString())
			.withClaim("query_hash", queryHash)
			.withClaim("query_hash_alg", "SHA512")
			.sign(algorithm);
	}
}
