package com.moebius.backend.service.member;

import com.moebius.backend.assembler.MemberAssembler;
import com.moebius.backend.configuration.security.JwtUtil;
import com.moebius.backend.domain.members.Member;
import com.moebius.backend.domain.members.MemberRepository;
import com.moebius.backend.domain.members.MoebiusPrincipal;
import com.moebius.backend.dto.frontend.LoginDto;
import com.moebius.backend.dto.frontend.MemberDto;
import com.moebius.backend.dto.frontend.SignupDto;
import com.moebius.backend.dto.frontend.response.LoginResponseDto;
import com.moebius.backend.exception.*;
import com.moebius.backend.utils.Verifier;
import com.mongodb.DuplicateKeyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.moebius.backend.utils.ThreadScheduler.COMPUTE;
import static com.moebius.backend.utils.ThreadScheduler.IO;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {
	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final MemberAssembler memberAssembler;
	private final EmailService emailService;

	public Mono<ResponseEntity<String>> checkDuplicateMember(String email) {
		return memberRepository.findByEmail(email)
			.subscribeOn(IO.scheduler())
			.publishOn(COMPUTE.scheduler())
			.hasElement()
			.map(duplicated -> duplicated ?
				ResponseEntity.ok(email) :
				ResponseEntity.ok().build());
	}

	public Mono<ResponseEntity<?>> createMember(SignupDto signupDto) {
		log.info("[Member] Start to create member. [{}]", signupDto);

		return memberRepository.save(memberAssembler.assembleMember(signupDto))
			.subscribeOn(IO.scheduler())
			.publishOn(COMPUTE.scheduler())
			.onErrorMap(exception -> exception instanceof DuplicateKeyException ?
				new DuplicatedDataException(ExceptionTypes.DUPLICATED_DATA.getMessage(signupDto.getEmail())) :
				exception)
			.flatMap(member -> {
				log.info("[Member] Succeeded in creating member. [{}]", member);
				return emailService.requestToVerifyEmail(member);
			});
	}

	public Mono<ResponseEntity<?>> login(LoginDto loginDto) {
		return memberRepository.findByEmail(loginDto.getEmail())
			.subscribeOn(IO.scheduler())
			.publishOn(COMPUTE.scheduler())
			.switchIfEmpty(Mono.defer(() -> Mono.error(new DataNotFoundException(ExceptionTypes.NONEXISTENT_DATA.getMessage(loginDto.getEmail())))))
			.filter(member -> passwordEncoder.matches(loginDto.getPassword(), member.getPassword()))
			.switchIfEmpty(Mono.defer(() -> Mono.error(new WrongDataException(ExceptionTypes.WRONG_DATA.getMessage("Email or password")))))
			.filter(Member::isActive)
			.switchIfEmpty(Mono.defer(() -> Mono.error(new DataNotVerifiedException(ExceptionTypes.UNVERIFIED_DATA.getMessage(loginDto.getEmail())))))
			.map(member -> ResponseEntity.ok(new LoginResponseDto(JwtUtil.generateToken(new MoebiusPrincipal(member)))));
	}

	public Mono<ResponseEntity<MemberDto>> getMember(String id) {
		Verifier.checkBlankString(id);

		return memberRepository.findById(new ObjectId(id))
			.subscribeOn(IO.scheduler())
			.publishOn(COMPUTE.scheduler())
			.map(memberAssembler::assembleDto)
			.map(ResponseEntity::ok);
	}

	public Flux<Member> getValidMembers() {
		return memberRepository.findAll()
			.subscribeOn(IO.scheduler())
			.publishOn(COMPUTE.scheduler())
			.filter(Member::isActive);
	}
}
