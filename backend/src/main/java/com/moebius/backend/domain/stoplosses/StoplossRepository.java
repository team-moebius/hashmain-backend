package com.moebius.backend.domain.stoplosses;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface StoplossRepository extends ReactiveMongoRepository<Stoploss, ObjectId> {
}