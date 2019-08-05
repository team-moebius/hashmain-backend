package com.moebius.backend.api;

import com.moebius.backend.dto.frontend.LoginDto;
import com.moebius.backend.dto.frontend.MemberDto;
import com.moebius.backend.dto.frontend.SignupDto;
import com.moebius.backend.dto.frontend.VerificationDto;
import com.moebius.backend.dto.frontend.response.LoginResponseDto;
import com.moebius.backend.exception.DataNotFoundException;
import com.moebius.backend.exception.DataNotVerifiedException;
import com.moebius.backend.exception.DuplicateDataException;
import com.moebius.backend.exception.VerificationFailedException;
import com.moebius.backend.service.member.EmailService;
import com.moebius.backend.service.member.MemberService;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.security.Principal;

@CrossOrigin
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {
	private final MemberService memberService;
	private final EmailService emailService;

	@ApiOperation(
		value = "로그인",
		httpMethod = "POST",
		notes = "성공할 경우 access token이 body에 담겨져 전달된다. 권한이 필요한 모든 요청의 Header에 'Authorization:Bearer ${ACCESS_TOKEN}'의 형태로 발송하면 된다."
	)
	@ApiResponses({
		@ApiResponse(code = 200, message = "Success", response = LoginResponseDto.class),
		@ApiResponse(code = 400, message = "Password is wrong", response = VerificationFailedException.class),
		@ApiResponse(code = 401, message = "Email is not verified", response = DataNotVerifiedException.class),
		@ApiResponse(code = 404, message = "Email is not found", response = DataNotFoundException.class),
	})
	@PostMapping("")
	public Mono<ResponseEntity<?>> login(@RequestBody @Valid @ApiParam(value = "로그인 시 필요한 정보", required = true) LoginDto loginDto) {
		return memberService.login(loginDto);
	}

	@ApiOperation(
		value = "회원 정보 조회",
		httpMethod = "GET",
		notes = "회원 정보를 조회한다."
	)
	@ApiImplicitParam(name = "Authorization", value = "Access token", required = true, paramType = "header", dataTypeClass = String.class, example = "Bearer ${ACCESS_TOKEN}")
	@ApiResponses({
		@ApiResponse(code = 200, message = "Success", response = MemberDto.class),
		@ApiResponse(code = 401, message = "Member is not verified", response = DataNotVerifiedException.class),
		@ApiResponse(code = 404, message = "Member is not found", response = DataNotFoundException.class),
	})
	@PreAuthorize("hasAuthority('MEMBER')")
	@GetMapping("")
	public Mono<ResponseEntity<MemberDto>> getMember(Principal principal) {
		return memberService.getMember(principal.getName());
	}

	@ApiOperation(
		value = "회원가입",
		httpMethod = "POST",
		notes = "회원가입을 요청한다. 회원 가입 요청 후 인증 이메일도 함께 발송된다."
	)
	@ApiResponses({
		@ApiResponse(code = 200, message = "Success", response = String.class),
		@ApiResponse(code = 400, message = "Requested email already exists.", response = DuplicateDataException.class),
		@ApiResponse(code = 400, message = "Requested email already verified", response = VerificationFailedException.class),
		@ApiResponse(code = 404, message = "Requested email is not found", response = DataNotFoundException.class),
	})
	@PostMapping("/signup")
	public Mono<ResponseEntity<?>> signup(@RequestBody @Valid @ApiParam(value = "회원가입 시 필요한 정보", required = true) SignupDto signupDto) {
		return memberService.createMember(signupDto);
	}

	@ApiOperation(
		value = "비밀번호 초기화",
		httpMethod = "POST",
		response = String.class
	)
	@PostMapping("/password")
	public Mono<ResponseEntity<?>> findPassword(@RequestBody @ApiParam(value = "초기화된 비밀번호를 전송할 이메일", required = true) String email) {
		return null;
	}

	@ApiOperation(
		value = "중복된 이메일 여부 조회",
		httpMethod = "GET",
		notes = "회원 가입 시 중복된 이메일을 확인할 때 사용한다. 중복이면 OK(200), 중복이 아닐 경우 Not Found(404)을 반환한다."
	)
	@ApiResponses({
		@ApiResponse(code = 200, message = "Requested email already exists", response = String.class),
		@ApiResponse(code = 404, message = "Requested email doesn't exist", response = DataNotFoundException.class),
	})
	@GetMapping("/duplicate/{email}")
	public Mono<ResponseEntity<String>> checkDuplicateMember(@PathVariable String email) {
		return memberService.checkDuplicateMember(email);
	}

	@GetMapping("/verification")
	public Mono<ResponseEntity<?>> verifyEmail(@ModelAttribute @Valid VerificationDto verificationDto) {
		return emailService.verifyEmail(verificationDto);
	}
}
