package com.moebius.backend.domain.apikeys;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ApiKeyRepository extends ReactiveMongoRepository<ApiKey, ObjectId> {
	Flux<ApiKey> findAllByMemberId(ObjectId memberId);

	Mono<ApiKey> findByIdAndMemberId(ObjectId id, ObjectId memberId);

	Mono<Long> deleteByIdAndMemberId(ObjectId id, ObjectId memberId);
}
