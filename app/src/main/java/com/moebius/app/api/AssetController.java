package com.moebius.app.api;

import com.moebius.backend.domain.commons.Exchange;
import com.moebius.backend.dto.frontend.response.AssetResponseDto;
import com.moebius.backend.dto.frontend.response.OrderResponseDto;
import com.moebius.backend.exception.DataNotFoundException;
import com.moebius.backend.exception.DataNotVerifiedException;
import com.moebius.backend.service.asset.AssetService;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotBlank;
import java.security.Principal;

@RestController
@RequestMapping("/assets")
@RequiredArgsConstructor
public class AssetController {
	private final AssetService assetService;

	@ApiOperation(
		value = "거래소에 따른 트레이더의 자산 정보 제공",
		httpMethod = "GET",
		notes = "특정 거래소 내에 트레이더가 보유한 자산 정보를 제공한다."
	)
	@ApiImplicitParam(name = "Authorization", value = "Access token", required = true, paramType = "header", dataTypeClass = String.class, example = "Bearer ${ACCESS_TOKEN}")
	@ApiResponses({
		@ApiResponse(code = 200, message = "Success", response = OrderResponseDto.class),
		@ApiResponse(code = 400, message = "Api key or Exchange is wrong (not found)", response = DataNotFoundException.class),
		@ApiResponse(code = 401, message = "Member is not verified", response = DataNotVerifiedException.class),
	})
	@GetMapping("/{exchange}")
	public Mono<ResponseEntity<AssetResponseDto>> getAssets(Principal principal,
		@PathVariable @NotBlank @ApiParam(value = "거래소", required = true) String exchange) {
		return assetService.getAssetResponse(principal.getName(), Exchange.getBy(exchange));
	}

}
