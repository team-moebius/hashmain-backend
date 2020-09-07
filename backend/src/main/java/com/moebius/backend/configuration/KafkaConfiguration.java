package com.moebius.backend.configuration;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaConfiguration {
	private final KafkaProperties kafkaProperties;
	private static final String SECURITY_PROTOCOL = "SASL_PLAINTEXT";
	private static final String SASL_MECHANISM = "PLAIN";

	@Bean
	public Map<String, String> senderDefaultProperties() {
		Map<String, String> senderDefaultProperties = new HashMap<>();

		senderDefaultProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, String.join(",", kafkaProperties.getBootstrapServers()));
		senderDefaultProperties.put(ProducerConfig.CLIENT_ID_CONFIG, kafkaProperties.getProducer().getClientId());
		senderDefaultProperties.put(ProducerConfig.ACKS_CONFIG, kafkaProperties.getProducer().getAcks());
		senderDefaultProperties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SECURITY_PROTOCOL);
		senderDefaultProperties.put(SaslConfigs.SASL_MECHANISM, SASL_MECHANISM);
		senderDefaultProperties.put(SaslConfigs.SASL_JAAS_CONFIG, getJaasConfig());

		return senderDefaultProperties;
	}

	@Bean
	public Map<String, String> receiverDefaultProperties() {
		Map<String, String> receiverDefaultProperties = new HashMap<>();
		receiverDefaultProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, String.join(",", kafkaProperties.getBootstrapServers()));
		receiverDefaultProperties.put(ConsumerConfig.CLIENT_ID_CONFIG, kafkaProperties.getConsumer().getClientId());
		receiverDefaultProperties.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaProperties.getConsumer().getGroupId());
		receiverDefaultProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, kafkaProperties.getConsumer().getAutoOffsetReset());
		receiverDefaultProperties.put(JsonDeserializer.TRUSTED_PACKAGES, "com.moebius.backend.dto.trade");
		receiverDefaultProperties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SECURITY_PROTOCOL);
		receiverDefaultProperties.put(SaslConfigs.SASL_MECHANISM, SASL_MECHANISM);
		receiverDefaultProperties.put(SaslConfigs.SASL_JAAS_CONFIG, getJaasConfig());

		return receiverDefaultProperties;
	}

	private String getJaasConfig() {
		StringBuilder configBuilder = new StringBuilder();
		KafkaProperties.Jaas jaas = kafkaProperties.getJaas();

		configBuilder.append(jaas.getLoginModule());
		configBuilder.append(" required ");
		jaas.getOptions().forEach((key, value) -> {
			configBuilder.append(key);
			configBuilder.append("=\"");
			configBuilder.append(value);
			configBuilder.append("\" ");
		});
		configBuilder.replace(configBuilder.length() - 1, configBuilder.length(), ";");

		return configBuilder.toString();
	}
}
