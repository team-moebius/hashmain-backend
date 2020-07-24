package com.moebius.backend.service.member

import com.moebius.backend.domain.members.Member
import com.moebius.backend.domain.members.MemberRepository
import com.moebius.backend.dto.frontend.VerificationDto
import org.springframework.http.HttpStatus
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessagePreparator
import org.thymeleaf.TemplateEngine
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import spock.lang.Specification
import spock.lang.Subject

class EmailServiceTest extends Specification {
	def emailSender = Mock(JavaMailSender)
	def memberRepository = Mock(MemberRepository)
	def templateEngine = Mock(TemplateEngine)
	def email = "somebody@dummy.com"

	@Subject
	def emailService = new EmailService(
			emailSender,
			memberRepository,
			templateEngine
	)

	def "Should request to verify email"() {
		given:
		def member = Stub(Member) {
			isActive() >> false
			getVerificationCode() >> "720321"
		}
		1 * memberRepository.findByEmail(_ as String) >> Mono.just(member)
		1 * emailSender.send(_ as MimeMessagePreparator)

		expect:
		StepVerifier.create(emailService.requestToVerifyEmail(email))
				.assertNext({
					it != null
					it.getStatusCode() == HttpStatus.OK
				})
				.verifyComplete()
	}

	def "Should verify email"() {
		given:
		def member = Stub(Member) {
			isActive() >> false
			getVerificationCode() >> "180712"
		}
		def verificationDto = Stub(VerificationDto) {
			getEmail() >> email
			getCode() >> "180712"
		}
		1 * memberRepository.findByEmail(_ as String) >> Mono.just(member)
		1 * memberRepository.save(_ as Member) >> Mono.just(member)

		expect:
		StepVerifier.create(emailService.verifyEmail(verificationDto))
				.assertNext({
					it != null
					it.getStatusCode() == HttpStatus.OK
				})
				.verifyComplete()
	}

	def "Should update member status"() {
		given:
		def member = new Member()
		1 * memberRepository.save(_ as Member) >> Mono.just(member)

		expect:
		StepVerifier.create(emailService.updateMember(member))
				.assertNext({
					it != null
					it instanceof Member
					it.isActive()
				})
				.verifyComplete()
	}
}
