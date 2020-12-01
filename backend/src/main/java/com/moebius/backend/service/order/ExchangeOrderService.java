package com.moebius.backend.service.order;

import com.moebius.backend.domain.apikeys.ApiKey;
import com.moebius.backend.domain.orders.Order;
import com.moebius.backend.domain.orders.OrderPosition;
import com.moebius.backend.dto.order.OrderDto;
import com.moebius.backend.dto.trade.TradeDto;
import com.moebius.backend.service.exchange.ExchangeService;
import com.moebius.backend.service.exchange.ExchangeServiceFactory;
import com.moebius.backend.service.member.ApiKeyService;
import com.moebius.backend.service.order.factory.OrderFactoryManager;
import com.moebius.backend.utils.OrderUtil;
import com.moebius.backend.utils.Verifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeOrderService {
	private final ApiKeyService apiKeyService;
	private final OrderCacheService orderCacheService;
	private final ExchangeServiceFactory exchangeServiceFactory;
	private final OrderFactoryManager orderFactoryManager;
	private final TransactionalOperator transactionalOperator;
	private final OrderUtil orderUtil;

	public void order(ApiKey apiKey, Order order) {
		ExchangeService exchangeService = exchangeServiceFactory.getService(order.getExchange());

		exchangeService.requestOrder(apiKey, order).subscribe();
	}

	public void orderByTrade(TradeDto tradeDto) {
		Verifier.checkNullFields(tradeDto);

		orderCacheService.getReadyOrderCountByExchangeAndSymbol(tradeDto.getExchange(), tradeDto.getSymbol())
			.filter(orderCount -> orderCount == 0)
			.switchIfEmpty(Mono.defer(() -> processTransactionalOrder(tradeDto)))
			.subscribe();
	}

	public void cancelIfNeeded(ApiKey apiKey, OrderDto orderDto) {
		Verifier.checkNullFields(orderDto);

		ExchangeService exchangeService = exchangeServiceFactory.getService(orderDto.getExchange());
		exchangeService.getCurrentOrderStatus(apiKey, orderDto.getId())
			.filter(orderStatusDto -> orderUtil.isOrderCancelNeeded(orderDto.getOrderStatus(), orderStatusDto))
			.flatMap(orderStatusDto -> exchangeService.cancelOrder(apiKey, orderDto.getId()))
			.subscribe();
	}

	private Mono<Long> processTransactionalOrder(TradeDto tradeDto) {
		ExchangeService exchangeService = exchangeServiceFactory.getService(tradeDto.getExchange());

		return getAndUpdateOrders(tradeDto)
			.flatMap(order -> requestOrder(exchangeService, order))
			.count()
			.flatMap(count -> evictIfCountNotZero(tradeDto, count))
			.as(transactionalOperator::transactional)
			.onErrorReturn(UncategorizedMongoDbException.class, 0L);
	}

	private Flux<Order> getAndUpdateOrders(TradeDto tradeDto) {
		return Flux.concat(
			Flux.fromStream(Arrays.stream(OrderPosition.values())
				.map(orderFactoryManager::getOrdersFactory)
				.filter(Objects::nonNull)
				.map(ordersFactory -> ordersFactory.getAndUpdateOrdersToInProgress(tradeDto))
			)
		);
	}

	private Mono<ClientResponse> requestOrder(ExchangeService exchangeService, Order order) {
		return apiKeyService.getApiKeyById(order.getApiKeyId().toHexString())
			.flatMap(apiKey -> exchangeService.requestOrder(apiKey, order));
	}

	private Mono<Long> evictIfCountNotZero(TradeDto tradeDto, long count) {
		if (count != 0) {
			orderCacheService.evictReadyOrderCount(tradeDto.getExchange(), tradeDto.getSymbol());
		}
		return Mono.just(count);
	}
}
