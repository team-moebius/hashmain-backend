package com.moebius.backend.service.stoploss;

import com.moebius.backend.assembler.StoplossAssembler;
import com.moebius.backend.domain.stoplosses.Stoploss;
import com.moebius.backend.domain.stoplosses.StoplossRepository;
import com.moebius.backend.dto.frontend.StoplossDto;
import com.moebius.backend.dto.frontend.response.StoplossResponseDto;
import com.moebius.backend.exception.DataNotFoundException;
import com.moebius.backend.exception.ExceptionTypes;
import com.moebius.backend.service.market.MarketService;
import com.moebius.backend.service.member.ApiKeyService;
import com.moebius.backend.service.tracker.TrackerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static com.moebius.backend.utils.ThreadScheduler.COMPUTE;
import static com.moebius.backend.utils.ThreadScheduler.IO;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoplossService {
	private final StoplossRepository stoplossRepository;
	private final StoplossAssembler stoplossAssembler;
	private final ApiKeyService apiKeyService;
	private final MarketService marketService;
	private final TrackerService trackerService;

	public Mono<ResponseEntity<List<StoplossResponseDto>>> createStoplosses(String apiKeyId, List<StoplossDto> stoplossDtos) {
		List<String> ids = new ArrayList<>();

		return apiKeyService.getApiKeyById(apiKeyId)
			.subscribeOn(COMPUTE.scheduler())
			.switchIfEmpty(Mono.defer(
				() -> Mono.error(new DataNotFoundException(ExceptionTypes.NONEXISTENT_DATA.getMessage("[ApiKey] " + apiKeyId)))))
			.flatMapIterable(apiKey -> stoplossAssembler.toStoplosses(apiKey, stoplossDtos))
			.compose(this::saveStoplosses)
			.map(stoploss -> {
				marketService.createMarketIfNotExist(stoploss.getExchange(), stoploss.getSymbol()).subscribe(); // FIXME : Move this logic to batch.
				return stoplossAssembler.toRespoonseDto(stoploss);
			})
			.collectList()
			.map(responseDtos -> {
				trackerService.reTrackTrades().subscribe(); // FIXME : Move this logic to batch.
				return ResponseEntity.ok(responseDtos);
			});
	}

	public Mono<ResponseEntity<List<StoplossResponseDto>>> getStoplossesByApiKey(String apiKeyId) {
		return stoplossRepository.findAllByApiKeyId(new ObjectId(apiKeyId))
			.subscribeOn(IO.scheduler())
			.publishOn(COMPUTE.scheduler())
			.switchIfEmpty(Mono.defer(() -> Mono.error(new DataNotFoundException(
				ExceptionTypes.NONEXISTENT_DATA.getMessage("[Stoploss] Stoploss information based on  " + apiKeyId)))))
			.map(stoplossAssembler::toRespoonseDto)
			.collectList()
			.map(ResponseEntity::ok);
	}

	public Mono<ResponseEntity<String>> deleteStoplossById(String id) {
		return stoplossRepository.deleteById(new ObjectId(id))
			.subscribeOn(IO.scheduler())
			.publishOn(COMPUTE.scheduler())
			.map(aVoid -> ResponseEntity.ok(id));
	}

	private Flux<Stoploss> saveStoplosses(Flux<Stoploss> stoplosses) {
		return stoplossRepository.saveAll(stoplosses)
			.subscribeOn(IO.scheduler())
			.publishOn(COMPUTE.scheduler());
	}
}
