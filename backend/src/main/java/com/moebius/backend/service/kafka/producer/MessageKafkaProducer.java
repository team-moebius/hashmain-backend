package com.moebius.backend.service.kafka.producer;

import com.moebius.backend.dto.message.MessageSendRequestDto;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.time.Instant;
import java.util.Map;

public class MessageKafkaProducer extends KafkaProducer<String, MessageSendRequestDto, String> {
    private static final String MESSAGE_SEND_TOPIC = "moebius.message.send";
    private static final String MESSAGE_KEY_FORMAT = "%s.%s.%s";
    private static final String CORRELATION_META_DATA_FORMAT = "%s.%s.%d";

    @Override
    protected String getKey(MessageSendRequestDto message) {
        return String.format(MESSAGE_KEY_FORMAT,
            message.getDedupStrategy(), message.getRecipientType(), message.getTitle()
        );
    }

    public MessageKafkaProducer(Map<String, String> senderDefaultProperties) {
        super(senderDefaultProperties);
    }

    @Override
    public String getTopic() {
        return MESSAGE_SEND_TOPIC;
    }

    @Override
    protected String getCorrelationMetadata(MessageSendRequestDto message) {
        long epochSecond = Instant.now().getEpochSecond();

        return String.format(CORRELATION_META_DATA_FORMAT,
                MESSAGE_SEND_TOPIC, getKey(message), epochSecond
        );
    }

    @Override
    protected Class<?> getKeySerializerClass() {
        return StringSerializer.class;
    }

    @Override
    protected Class<?> getValueSerializerClass() {
        return JsonSerializer.class;
    }
}
