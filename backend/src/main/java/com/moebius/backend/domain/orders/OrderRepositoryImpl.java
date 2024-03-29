package com.moebius.backend.domain.orders;

import com.moebius.backend.domain.commons.Exchange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepositoryCustom {
	private final ReactiveMongoTemplate mongoTemplate;

	@Override
	public Flux<Order> findAndUpdateAllByAskCondition(Exchange exchange, String symbol, OrderPosition orderPosition, double price) {
		Query query = new Query(Criteria.where("price").lte(price)
			.and("symbol").is(symbol)
			.and("orderPosition").is(orderPosition)
			.and("orderStatus").is(OrderStatus.IN_PROGRESS)
			.and("exchange").is(exchange));

		return mongoTemplate.find(query, Order.class)
			.flatMap(this::updateOrderStatusToDone);
	}

	@Override
	public Flux<Order> findAndUpdateAllByBidCondition(Exchange exchange, String symbol, OrderPosition orderPosition, double price) {
		Query query = new Query(Criteria.where("price").gte(price)
			.and("symbol").is(symbol)
			.and("orderPosition").is(orderPosition)
			.and("orderStatus").is(OrderStatus.IN_PROGRESS)
			.and("exchange").is(exchange));

		return mongoTemplate.find(query, Order.class)
			.flatMap(this::updateOrderStatusToDone);
	}

	@Override
	public Flux<Order> findAllByOrderStatusCondition(OrderStatusCondition orderStatusCondition) {
		Query query = new Query(Criteria.where("symbol").is(orderStatusCondition.getSymbol())
			.and("exchange").is(orderStatusCondition.getExchange())
			.and("orderStatus").is(orderStatusCondition.getOrderStatus()));

		return mongoTemplate.find(query, Order.class);
	}

	private Mono<Order> updateOrderStatusToDone(Order order) {
		order.setOrderStatus(OrderStatus.DONE);
		order.setUpdatedAt(LocalDateTime.now());
		return mongoTemplate.save(order);
	}
}
