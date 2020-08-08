package com.moebius.backend.service.market;

import com.moebius.backend.assembler.MarketAssembler;
import com.moebius.backend.domain.commons.Exchange;
import com.moebius.backend.domain.markets.Market;
import com.moebius.backend.domain.markets.MarketRepository;
import com.moebius.backend.dto.TradeDto;
import com.moebius.backend.dto.exchange.MarketsDto;
import com.moebius.backend.dto.exchange.upbit.UpbitTradeMetaDto;
import com.moebius.backend.dto.frontend.response.MarketResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.List;
import java.util.Map;

import static com.moebius.backend.utils.ThreadScheduler.COMPUTE;
import static com.moebius.backend.utils.ThreadScheduler.IO;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketService {
	@Value("${exchange.upbit.rest.public-uri}")
	private String publicUri;
	@Value("${exchange.upbit.rest.secret-uri}")
	private String secretUri;
	@Value("${exchange.upbit.rest.market}")
	private String marketUri;
	@Value("${exchange.upbit.rest.recent}")
	private String recentUri;

	private final WebClient webClient;
	private final MarketRepository marketRepository;
	private final MarketAssembler marketAssembler;

	public void updateMarketPrice(TradeDto tradeDto) {
		getMarketAndTradeMeta(tradeDto)
			.onErrorResume(UncategorizedMongoDbException.class, exception -> getMarketAndTradeMeta(tradeDto))
			.map(tuple -> marketAssembler.assembleUpdatedMarket(tuple.getT1(), tradeDto, tuple.getT2()))
			.flatMap(marketRepository::save)
			.subscribe();
	}

	public Mono<ResponseEntity<List<MarketResponseDto>>> getMarkets(Exchange exchange) {
		return marketRepository.findAllByExchange(exchange)
			.subscribeOn(IO.scheduler())
			.publishOn(COMPUTE.scheduler())
			.map(marketAssembler::assembleResponse)
			.collectList()
			.map(ResponseEntity::ok);
	}

	public Mono<ResponseEntity<String>> deleteMarket(String id) {
		return marketRepository.deleteById(new ObjectId(id))
			.subscribeOn(IO.scheduler())
			.publishOn(COMPUTE.scheduler())
			.map(aVoid -> {
				log.info("[Market] The market has been deleted. [id : {}]", id);
				return ResponseEntity.ok().build();
			});
	}

	// TODO : External api call should be moved to specific exchange service
	public Mono<ResponseEntity<?>> updateMarkets(Exchange exchange) {
		return webClient.get()
			.uri(publicUri + marketUri)
			.retrieve()
			.bodyToMono(MarketsDto.class)
			.subscribeOn(IO.scheduler())
			.publishOn(COMPUTE.scheduler())
			.map(marketsDto -> marketAssembler.assembleMarkets(exchange, marketsDto))
			.map(markets -> {
				markets.stream()
					.filter(market -> market.getSymbol().startsWith("KRW"))
					.forEach(market -> createMarketIfNotExist(market.getExchange(), market.getSymbol()).subscribe());
				return ResponseEntity.ok().build();
			});
	}

	public Mono<Map<String, Double>> getCurrencyMarketPriceMap(Exchange exchange) {
		return marketRepository.findAllByExchange(exchange)
			.subscribeOn(IO.scheduler())
			.publishOn(COMPUTE.scheduler())
			.collectList()
			.map(marketAssembler::assembleCurrencyMarketPrices);
	}

	public Mono<Double> getCurrentPrice(Exchange exchange, String symbol) {
		return marketRepository.findByExchangeAndSymbol(exchange, symbol)
			.subscribeOn(IO.scheduler())
			.publishOn(COMPUTE.scheduler())
			.map(Market::getCurrentPrice);
	}

	private Mono<Tuple2<Market, UpbitTradeMetaDto>> getMarketAndTradeMeta(TradeDto tradeDto) {
		return Mono.zip(
			marketRepository.findByExchangeAndSymbol(tradeDto.getExchange(), tradeDto.getSymbol()),
			getTradeMeta(tradeDto.getSymbol())
		).subscribeOn(IO.scheduler())
			.publishOn(COMPUTE.scheduler());
	}

	// TODO : External api call should be moved to specific exchange service
	// FIXME : This secret api is unstable, it causes mongodb transaction exception. So It will be removed after using tracker api
	private Mono<UpbitTradeMetaDto> getTradeMeta(String symbol) {
		return webClient.get()
			.uri(secretUri + recentUri + symbol)
			.retrieve()
			.bodyToFlux(UpbitTradeMetaDto.class)
			.doOnError(Exception.class, exception -> log.error("[Market] Failed to get trade meta from exchange.", exception))
			.next();
	}

	private Mono<Boolean> createMarketIfNotExist(Exchange exchange, String symbol) {
		return marketRepository.findByExchangeAndSymbol(exchange, symbol)
			.subscribeOn(IO.scheduler())
			.publishOn(COMPUTE.scheduler())
			.hasElement()
			.flatMap(exist -> exist ? Mono.just(Boolean.FALSE) : saveMarket(marketAssembler.assembleMarket(exchange, symbol)));
	}

	private Mono<Boolean> saveMarket(Market market) {
		return marketRepository.save(market)
			.subscribeOn(IO.scheduler())
			.publishOn(COMPUTE.scheduler())
			.map(createdMarket -> {
				log.info("[Market] {} / {} is not found, The new market will be saved.", market.getExchange(), market.getSymbol());
				return true;
			});
	}
}
