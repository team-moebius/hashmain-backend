package com.moebius.app.api;

import com.moebius.backend.dto.frontend.ApiKeyDto;
import com.moebius.backend.dto.frontend.response.ApiKeyResponseDto;
import com.moebius.backend.exception.DataNotFoundException;
import com.moebius.backend.exception.DataNotVerifiedException;
import com.moebius.backend.exception.DuplicatedDataException;
import com.moebius.backend.service.member.ApiKeyService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {
	private final ApiKeyService apiKeyService;

	@ApiOperation(
		value = "Api key 검증 및 생성",
		httpMethod = "POST",
		notes = "거래에 사용될 Api key를 검증 후 등록한다."
	)
	@ApiImplicitParam(name = "Authorization", value = "Access token", required = true, paramType = "header", dataTypeClass = String.class, example = "Bearer ${ACCESS_TOKEN}")
	@ApiResponses({
		@ApiResponse(code = 200, message = "Success", response = String.class),
		@ApiResponse(code = 400, message = "Api key is not verified", response = DataNotVerifiedException.class),
		@ApiResponse(code = 400, message = "Api key already exists", response = DuplicatedDataException.class),
	})
	@PostMapping("")
	public Mono<ResponseEntity<ApiKeyResponseDto>> createApiKey(@RequestBody @Valid ApiKeyDto apiKeyDto, Principal principal) {
		return apiKeyService.verifyAndCreateApiKey(apiKeyDto, principal.getName());
	}

	@ApiOperation(
		value = "Api keys 조회",
		httpMethod = "GET",
		notes = "거래에 사용될 Api keys를 Access token으로 조회한다."
	)
	@ApiImplicitParam(name = "Authorization", value = "Access token", required = true, paramType = "header", dataTypeClass = String.class, example = "Bearer ${ACCESS_TOKEN}")
	@ApiResponses({
		@ApiResponse(code = 200, message = "Success", responseContainer = "List", response = ApiKeyDto.class),
		@ApiResponse(code = 400, message = "Api keys are not found at all", response = DataNotFoundException.class),
	})
	@GetMapping("")
	public Mono<ResponseEntity<List<ApiKeyResponseDto>>> getApiKeys(Principal principal) {
		return apiKeyService.getApiKeysByMemberId(principal.getName());
	}

	@ApiOperation(
		value = "Api key 삭제",
		httpMethod = "DELETE",
		notes = "거래에 사용되는 특정 Api key를 삭제한다. Api key 소유자의 Access token을 보내지 않으면 Api key를 찾지 못하고, 삭제되지 않는다."
	)
	@ApiImplicitParam(name = "Authorization", value = "Access token", required = true, paramType = "header", dataTypeClass = String.class, example = "Bearer ${ACCESS_TOKEN}")
	@ApiResponses({
		@ApiResponse(code = 200, message = "The deleted api key id", response = String.class),
		@ApiResponse(code = 404, message = "Api key is not found", response = DataNotFoundException.class),
	})
	@DeleteMapping("/{id}")
	public Mono<ResponseEntity<String>> deleteApiKey(@PathVariable String id, Principal principal) {
		return apiKeyService.deleteApiKeyById(id, principal.getName());
	}
}