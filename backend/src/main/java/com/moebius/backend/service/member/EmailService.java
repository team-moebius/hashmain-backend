package com.moebius.backend.service.member;

import com.moebius.backend.domain.members.Member;
import com.moebius.backend.domain.members.MemberRepository;
import com.moebius.backend.dto.frontend.VerificationDto;
import com.moebius.backend.exception.DataNotFoundException;
import com.moebius.backend.exception.ExceptionTypes;
import com.moebius.backend.exception.WrongDataException;
import com.moebius.backend.utils.Verifier;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static com.moebius.backend.utils.ThreadScheduler.COMPUTE;
import static com.moebius.backend.utils.ThreadScheduler.IO;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
	private final JavaMailSender emailSender;
	private final MemberRepository memberRepository;
	private final TemplateEngine templateEngine;
	@Value("${moebius.protocol}")
	private String protocol;
	@Value("${moebius.host}")
	private String host;

	public Mono<ResponseEntity<?>> requestToVerifyEmail(@NonNull Member registeredMember) {
		return Mono.defer(() -> Mono.just(registeredMember))
			.filter(member -> !member.isActive() && StringUtils.isNoneBlank(member.getVerificationCode()))
			.switchIfEmpty(Mono.defer(() -> Mono.error(new WrongDataException(ExceptionTypes.ALREADY_VERIFIED_DATA.getMessage(registeredMember.getEmail())))))
			.doOnSuccess(member -> sendVerificationEmail(member).subscribe())
			.map(member -> ResponseEntity.ok(HttpStatus.OK.getReasonPhrase()));
	}

	public Mono<ResponseEntity<?>> verifyEmail(@NonNull VerificationDto verificationDto) {
		Verifier.checkNullFields(verificationDto);

		return memberRepository.findByEmail(verificationDto.getEmail())
			.subscribeOn(IO.scheduler())
			.publishOn(COMPUTE.scheduler())
			.switchIfEmpty(Mono.defer(() -> Mono.error(new DataNotFoundException(ExceptionTypes.NONEXISTENT_DATA.getMessage(verificationDto.getEmail())))))
			.filter(member -> !member.isActive() && member.getVerificationCode() != null)
			.switchIfEmpty(Mono.defer(() -> Mono.error(new WrongDataException(ExceptionTypes.ALREADY_VERIFIED_DATA.getMessage(verificationDto.getEmail())))))
			.filter(member -> member.getVerificationCode() != null && member.getVerificationCode().equals(verificationDto.getCode()))
			.switchIfEmpty(Mono.defer(() -> Mono.error(new WrongDataException(ExceptionTypes.WRONG_DATA.getMessage("Entered code")))))
			.flatMap(this::updateMember)
			.map(member -> ResponseEntity.ok(HttpStatus.OK.getReasonPhrase()));
	}

	private Mono<String> sendVerificationEmail(Member member) {
		log.info("[Email] Start to send verification mail. [{}]", member.getEmail());

		return Mono.fromCallable(() -> {
			MimeMessagePreparator messagePreparator = mimeMessage -> {
				MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, StandardCharsets.UTF_8.name());
				messageHelper.setFrom("cryptobox.master@gmail.com");
				messageHelper.setTo(member.getEmail());
				messageHelper.setSubject("Hashmain.pro 인증 메일입니다.");

				Context context = new Context();
				context.setVariable("protocol", protocol);
				context.setVariable("host", host);
				context.setVariable("code", member.getVerificationCode());
				context.setVariable("name", member.getName());
				context.setVariable("email", member.getEmail());

				String content = templateEngine.process("verificationForm", context);
				messageHelper.setText(content, true);
			};
			try {
				emailSender.send(messagePreparator);
				return HttpStatus.OK.getReasonPhrase();
			} catch (MailException me) {
				log.error("[Email] MailException occurred.", me);
				throw me;
			}
		}).doOnSuccess(stringResponseEntity -> log.info("[Email] Succeeded in sending verification email. [{}]", member.getEmail()))
			.subscribeOn(IO.scheduler());
	}

	private Mono<Member> updateMember(Member member) {
		return Mono.fromCallable(() -> {
			member.setActive(true);
			member.setVerificationCode(null);
			member.setUpdatedAt(LocalDateTime.now());
			return member;
		}).subscribeOn(COMPUTE.scheduler())
			.publishOn(IO.scheduler())
			.flatMap(memberRepository::save);
	}
}
