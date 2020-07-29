package com.moebius.app.api;

import com.moebius.backend.domain.commons.Exchange;
import com.moebius.backend.dto.frontend.response.MarketResponseDto;
import com.moebius.backend.exception.DataNotVerifiedException;
import com.moebius.backend.exception.WrongDataException;
import com.moebius.backend.service.market.MarketService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/markets")
@RequiredArgsConstructor
public class MarketController {
	private final MarketService marketService;

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('ADMIN')")
	public Mono<ResponseEntity<String>> deleteMarket(@PathVariable String id) {
		return marketService.deleteMarket(id);
	}

	@PutMapping("/{exchange}")
	@PreAuthorize("hasAuthority('ADMIN')")
	public Mono<ResponseEntity<?>> updateMarketsByExchange(@PathVariable String exchange) {
		return marketService.updateMarkets(Exchange.getBy(exchange));
	}

	@ApiOperation(
		value = "거래소에 따른 종목 정보 제공",
		httpMethod = "GET",
		notes = "트레이더가 원하는 거래소의 종목 정보를 제공한다. 종목 정보는 심볼(기준 화폐-거래 화폐, 예를 들어 KRW-BTC) 및 현재 거래 가격, 전일 종가 대비 가격 변동 비율(예를 들어 종가는 100원이고, 현재가는 105원이라면 0.05), 총 거래 대금을 포함한다."
	)
	@ApiImplicitParam(name = "Authorization", value = "Access token", required = true, paramType = "header", dataTypeClass = String.class, example = "Bearer ${ACCESS_TOKEN}")
	@ApiResponses({
		@ApiResponse(code = 200, message = "Success", responseContainer = "List", response = MarketResponseDto.class),
		@ApiResponse(code = 400, message = "Wrong exchange has been entered.", response = WrongDataException.class),
		@ApiResponse(code = 401, message = "Member is not verified", response = DataNotVerifiedException.class)
	})
	@GetMapping("/{exchange}")
	@PreAuthorize("hasAuthority('MEMBER')")
	public Mono<ResponseEntity<List<MarketResponseDto>>> getMarketsByExchange(@PathVariable String exchange) {
		return marketService.getMarkets(Exchange.getBy(exchange));
	}
}
