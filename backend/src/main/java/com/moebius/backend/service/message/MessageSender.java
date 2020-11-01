package com.moebius.backend.service.message;

import com.moebius.backend.dto.message.MessageBodyDto;
import com.moebius.backend.dto.message.MessageDedupStrategy;
import com.moebius.backend.dto.message.MessageSendRequestDto;
import com.moebius.backend.dto.message.MessageRecipientType;
import com.moebius.backend.service.kafka.producer.MessageKafkaProducer;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Optional;

@RequiredArgsConstructor
public abstract class MessageSender<PARAM, BODY> {
    private final MessageKafkaProducer messageKafkaProducer;

    Mono<Boolean> sendMessage(PARAM parameter) {
        String recipientType = getRecipientType().name();
        MessageSendRequestDto<BODY> sendRequest = new MessageSendRequestDto<>(
                getDedupStrategyValue(), getDedupPeriod(), getTitle(parameter),
                getMessageBody(parameter),
                recipientType, getRecipientId(parameter)

        );

        return Mono.from(messageKafkaProducer.produceMessages(sendRequest)
                .map(senderResult -> Objects.isNull(senderResult.exception())));

    }

    private MessageBodyDto<BODY> getMessageBody(PARAM parameter) {
        return new MessageBodyDto<>(getTemplateName(parameter), getBody(parameter));
    }

    private String getDedupStrategyValue() {
        return Optional.ofNullable(getDedupStrategy())
                .map(Enum::name)
                .orElse(MessageDedupStrategy.NO_DEDUP.name());
    }

    protected abstract long getDedupPeriod();

    protected abstract MessageDedupStrategy getDedupStrategy();

    protected abstract String getTitle(PARAM message);

    protected abstract String getTemplateName(PARAM message);

    protected abstract BODY getBody(PARAM message);

    protected abstract MessageRecipientType getRecipientType();

    protected abstract String getRecipientId(PARAM message);
}
