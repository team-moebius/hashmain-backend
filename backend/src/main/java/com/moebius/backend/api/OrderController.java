package com.moebius.backend.api;

import com.moebius.backend.domain.commons.Exchange;
import com.moebius.backend.dto.OrderDto;
import com.moebius.backend.dto.frontend.response.OrderResponseDto;
import com.moebius.backend.exception.DataNotFoundException;
import com.moebius.backend.exception.DataNotVerifiedException;
import com.moebius.backend.service.order.InternalOrderService;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
	private final InternalOrderService internalOrderService;

	@ApiOperation(
		value = "주문(매수, 매도, 역지정) 생성 / 갱신 / 삭제",
		httpMethod = "POST",
		notes = "트레이더가 원하는 주문 정보를 Event Type에 따라 생성, 갱신 또는 삭제 한다."
	)
	@ApiImplicitParam(name = "Authorization", value = "Access token", required = true, paramType = "header", dataTypeClass = String.class, example = "Bearer ${ACCESS_TOKEN}")
	@ApiResponses({
		@ApiResponse(code = 200, message = "Success", response = OrderResponseDto.class),
		@ApiResponse(code = 400, message = "Api key is not found", response = DataNotFoundException.class),
		@ApiResponse(code = 401, message = "Member is not verified", response = DataNotVerifiedException.class),
	})
	@PostMapping("")
	public Mono<ResponseEntity<OrderResponseDto>> processOrders(Principal principal,
		@RequestBody @Valid @ApiParam(value = "갱신된 주문 정보", required = true) List<OrderDto> orderDtos) {
		return internalOrderService.processOrders(principal.getName(), Exchange.UPBIT, orderDtos);
	}

	@ApiOperation(
		value = "거래소에 따른 전체 주문(매수, 매도, 역지정) 및 자산 정보 제공",
		httpMethod = "GET",
		notes = "트레이더가 저장한 주문 정보 및 자산 정보를 제공한다. 트레이더의 거래소 api key를 기반으로 특정 거래소에 등록되어 있는 모든 주문 정보가 제공되며 현재 트레이더의 자산 정보도 같이 제공된다."
	)
	@ApiImplicitParam(name = "Authorization", value = "Access token", required = true, paramType = "header", dataTypeClass = String.class, example = "Bearer ${ACCESS_TOKEN}")
	@ApiResponses({
		@ApiResponse(code = 200, message = "Success", response = OrderResponseDto.class),
		@ApiResponse(code = 400, message = "Api key or Exchange is wrong (not found)", response = DataNotFoundException.class),
		@ApiResponse(code = 401, message = "Member is not verified", response = DataNotVerifiedException.class),
	})
	@GetMapping("/{exchange}")
	public Mono<ResponseEntity<OrderResponseDto>> getOrdersAndAssets(Principal principal,
		@PathVariable @NotBlank @ApiParam(value = "거래소", required = true) String exchange) {
		return internalOrderService.getOrdersAndAssets(principal.getName(), exchange);
	}

	@ApiOperation(
		value = "거래소 및 종목에 따른 주문(매수, 매도, 역지정) 및 자산 정보 제공",
		httpMethod = "GET",
		notes = "트레이더가 저장한 주문 정보 및 자산 정보를 제공한다. 트레이더의 거래소 api key를 기반으로, 거래소 및 종목을 조건으로 주문 정보가 제공되며 현재 트레이더의 자산 정보도 같이 제공된다."
	)
	@ApiImplicitParam(name = "Authorization", value = "Access token", required = true, paramType = "header", dataTypeClass = String.class, example = "Bearer ${ACCESS_TOKEN}")
	@ApiResponses({
		@ApiResponse(code = 200, message = "Success", response = OrderResponseDto.class),
		@ApiResponse(code = 400, message = "Api key or Exchange is wrong (not found)", response = DataNotFoundException.class),
		@ApiResponse(code = 401, message = "Member is not verified", response = DataNotVerifiedException.class),
	})
	@GetMapping("/{exchange}/{symbol}")
	public Mono<ResponseEntity<OrderResponseDto>> getOrdersAndAssetsByExchangeAndSymbol(Principal principal,
		@PathVariable @NotBlank @ApiParam(value = "거래소", required = true) String exchange,
		@PathVariable @NotBlank @ApiParam(value = "종목", required = true) String symbol) {
		return internalOrderService.getOrdersAndAssetsWithSymbol(principal.getName(), exchange, symbol);
	}
}
