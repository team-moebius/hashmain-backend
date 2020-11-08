package com.moebius.backend.service.message

import com.moebius.backend.domain.commons.Exchange
import com.moebius.backend.dto.message.MessageDedupStrategy
import com.moebius.backend.dto.message.MessageRecipientType
import com.moebius.backend.dto.slack.TradeSlackDto
import com.moebius.backend.service.kafka.producer.MessageKafkaProducer
import com.moebius.backend.utils.OrderUtil
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kafka.sender.SenderResult
import reactor.test.StepVerifier
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

import java.time.LocalTime

class TradeSlackMessageSenderTest extends Specification {
    def orderUtil = Mock(OrderUtil)
    def messageKafkaProducer = Mock(MessageKafkaProducer)

    @Shared
    def kafkaSendResult = Mock(SenderResult)

    @Subject
    def sut = new TradeSlackMessageSender(messageKafkaProducer, orderUtil)

    def "Test message send"() {
        given:
        def symbol = "KRW-BTC"
        def parameter = TradeSlackDto.builder()
                .symbol(symbol)
                .exchange(Exchange.UPBIT)
                .totalAskVolume(0.0f)
                .totalBidVolume(0.0f)
                .totalValidPrice(0)
                .price(0)
                .priceChangeRate(0.0f)
                .from(LocalTime.now())
                .to(LocalTime.now())
                .referenceLink("")
                .build()

        1 * orderUtil.getUnitCurrencyBySymbol(symbol) >> "KRW"
        1 * orderUtil.getTargetCurrencyBySymbol(symbol) >> "BTC"
        1 * messageKafkaProducer.produceMessages({
            it.dedupStrategy == MessageDedupStrategy.LEAVE_LAST_ARRIVAL.name() &&  \
             it.recipientType == MessageRecipientType.SLACK.name() &&  \
             it.body.parameters.from != null && it.body.parameters.to != null &&  \
             it.body.parameters.unitCurrency == "KRW" && it.body.parameters.targetCurrency == "BTC"
        }) >> Flux.just(kafkaSendResult)

        expect:
        StepVerifier.create(sut.sendMessage(parameter))
                .expectNext(true)
                .verifyComplete()

    }
}
