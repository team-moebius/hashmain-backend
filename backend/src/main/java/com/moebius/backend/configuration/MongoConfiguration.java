package com.moebius.backend.configuration;

import com.moebius.backend.domain.Repositories;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(MongoProperties.class)
@EnableReactiveMongoRepositories(basePackageClasses = Repositories.class)
public class MongoConfiguration {
	private final MongoProperties mongoProperties;

	@Bean
	public MongoClient mongoClient() {
		return MongoClients.create(mongoProperties.getUri());
	}

	@Bean
	public SimpleReactiveMongoDatabaseFactory reactiveMongoDatabaseFactory(MongoClient mongoClient) {
		return new SimpleReactiveMongoDatabaseFactory(mongoClient, mongoProperties.getDatabase());
	}

	@Bean
	public ReactiveMongoTemplate reactiveMongoTemplate(ReactiveMongoDatabaseFactory mongoDatabaseFactory) {
		ReactiveMongoTemplate template = new ReactiveMongoTemplate(mongoDatabaseFactory);
		MappingMongoConverter converter = (MappingMongoConverter) template.getConverter();
		converter.setTypeMapper(new DefaultMongoTypeMapper(null));

		return template;
	}

	@Bean
	public TransactionOptions transactionalOptions() {
		return TransactionOptions.builder()
			.readConcern(ReadConcern.LOCAL)
			.writeConcern(WriteConcern.MAJORITY)
			.readPreference(ReadPreference.primary())
			.build();
	}

	@Bean
	public ReactiveTransactionManager transactionManager(ReactiveMongoDatabaseFactory mongoDatabaseFactory, TransactionOptions transactionOptions) {
		return new ReactiveMongoTransactionManager(mongoDatabaseFactory, transactionOptions);
	}

	@Bean
	public TransactionalOperator transactionalOperator(ReactiveTransactionManager transactionManager) {
		return TransactionalOperator.create(transactionManager);
	}
}
