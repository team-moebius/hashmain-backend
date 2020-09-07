package com.moebius.backend.service.order;

import com.moebius.backend.assembler.order.OrderAssembler;
import com.moebius.backend.assembler.order.OrderAssetAssembler;
import com.moebius.backend.utils.OrderUtil;
import com.moebius.backend.domain.apikeys.ApiKey;
import com.moebius.backend.domain.commons.EventType;
import com.moebius.backend.domain.commons.Exchange;
import com.moebius.backend.domain.orders.Order;
import com.moebius.backend.domain.orders.OrderRepository;
import com.moebius.backend.domain.orders.OrderStatus;
import com.moebius.backend.dto.order.OrderAssetDto;
import com.moebius.backend.dto.order.OrderDto;
import com.moebius.backend.dto.exchange.AssetDto;
import com.moebius.backend.dto.frontend.response.OrderAssetResponseDto;
import com.moebius.backend.dto.frontend.response.OrderResponseDto;
import com.moebius.backend.exception.DataNotFoundException;
import com.moebius.backend.exception.ExceptionTypes;
import com.moebius.backend.service.asset.AssetService;
import com.moebius.backend.service.market.MarketService;
import com.moebius.backend.service.member.ApiKeyService;
import com.moebius.backend.service.order.validator.OrderValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.moebius.backend.domain.commons.EventType.CREATE;
import static com.moebius.backend.domain.commons.EventType.DELETE;
import static com.moebius.backend.utils.ThreadScheduler.COMPUTE;
import static com.moebius.backend.utils.ThreadScheduler.IO;

@Slf4j
@Service
@RequiredArgsConstructor
public class InternalOrderService {
	private final OrderRepository orderRepository;
	private final OrderAssembler orderAssembler;
	private final OrderAssetAssembler orderAssetAssembler;
	private final OrderValidator orderValidator;
	private final OrderUtil orderUtil;
	private final ApiKeyService apiKeyService;
	private final AssetService assetService;
	private final MarketService marketService;
	private final OrderCacheService orderCacheService;
	private final ExchangeOrderService exchangeOrderService;

	public Mono<ResponseEntity<OrderResponseDto>> processOrders(String memberId, Exchange exchange, List<OrderDto> orderDtos) {
		orderValidator.validate(orderDtos);

		return apiKeyService.getApiKeyByMemberIdAndExchange(memberId, exchange)
			.subscribeOn(COMPUTE.scheduler())
			.flatMapIterable(apiKey -> orderDtos.stream()
				.map(orderDto -> processOrder(apiKey, orderDto))
				.collect(Collectors.toList()))
			.collectList()
			.doOnSuccess(this::evictOrderCaches)
			.map(orderAssembler::assembleResponseDto)
			.map(ResponseEntity::ok);
	}

	public Mono<ResponseEntity<OrderResponseDto>> getOrdersByExchange(String memberId, Exchange exchange) {
		return getOrders(memberId, exchange, apiKey -> orderRepository.findAllByApiKeyId(apiKey.getId()))
			.subscribeOn(COMPUTE.scheduler())
			.map(orderAssembler::assembleResponseDto)
			.map(ResponseEntity::ok);
	}

	public Mono<ResponseEntity<OrderResponseDto>> getOrdersByExchangeAndSymbol(String memberId, Exchange exchange, String symbol) {
		return getOrders(memberId, exchange, apiKey -> orderRepository.findAllByApiKeyId(apiKey.getId()))
			.map(orders -> orderUtil.filterOrdersBySymbol(orders, symbol))
			.subscribeOn(COMPUTE.scheduler())
			.map(orderAssembler::assembleResponseDto)
			.map(ResponseEntity::ok);
	}

	public Mono<ResponseEntity<OrderAssetResponseDto>> getOrderAssets(String memberId, Exchange exchange) {
		return Mono.zip(
			getOrders(memberId, exchange, apiKey -> orderRepository.findAllByApiKeyIdAndOrderStatusNot(apiKey.getId(), OrderStatus.DONE))
				.map(orderAssetAssembler::assembleCurrencyToOrderDtos),
			assetService.getCurrencyAssetMap(memberId, exchange),
			marketService.getCurrencyMarketPriceMap(exchange)
		).subscribeOn(COMPUTE.scheduler())
			.map(tuple -> extractOrderStatuses(tuple.getT1(), tuple.getT2(), tuple.getT3()))
			.map(orderAssetAssembler::assembleOrderAssetResponse)
			.map(ResponseEntity::ok);
	}

	private OrderDto processOrder(ApiKey apiKey, OrderDto orderDto) {
		EventType eventType = orderDto.getEventType();

		if (eventType == CREATE) {
			Mono.zip(
				createOrder(apiKey, orderDto),
				marketService.getCurrentPrice(orderDto.getExchange(), orderDto.getSymbol())
			).subscribe(tuple -> requestOrderIfNeeded(apiKey, tuple.getT1(), tuple.getT2()));
		} else if (eventType == DELETE) {
			deleteOrder(orderDto.getId())
				.subscribe(nothing -> exchangeOrderService.cancelIfNeeded(apiKey, orderDto));
		}

		return orderDto;
	}

	private void requestOrderIfNeeded(ApiKey apiKey, Order order, double price) {
		if (orderUtil.isOrderRequestNeeded(order, price)) {
			exchangeOrderService.order(apiKey, order);
		}
	}

	private Mono<Order> createOrder(ApiKey apiKey, OrderDto orderDto) {
		return orderRepository.save(orderAssembler.assembleReadyOrder(apiKey, orderDto))
			.subscribeOn(IO.scheduler())
			.publishOn(COMPUTE.scheduler());
	}

	private Mono<Void> deleteOrder(String id) {
		return orderRepository.deleteById(new ObjectId(id))
			.subscribeOn(IO.scheduler())
			.publishOn(COMPUTE.scheduler());
	}

	private void evictOrderCaches(List<OrderDto> orderDtos) {
		orderDtos.stream()
			.collect(Collectors.groupingBy(OrderDto::getSymbol))
			.keySet()
			.forEach(symbol -> orderCacheService.evictReadyOrderCount(orderDtos.get(0).getExchange(), symbol));
	}

	private Mono<List<OrderDto>> getOrders(String memberId, Exchange exchange, Function<ApiKey, Flux<Order>> getOrdersFunction) {
		return apiKeyService.getApiKeyByMemberIdAndExchange(memberId, exchange)
			.flatMapMany(getOrdersFunction)
			.subscribeOn(IO.scheduler())
			.publishOn(COMPUTE.scheduler())
			.switchIfEmpty(Mono.defer(() -> Mono.error(new DataNotFoundException(
				ExceptionTypes.NONEXISTENT_DATA.getMessage("[Order] order information based on memberId(" + memberId + ")")))))
			.map(order -> orderAssembler.assembleDto(order, EventType.READ))
			.collectList();
	}

	private List<OrderAssetDto> extractOrderStatuses(Map<String, List<OrderDto>> currencyToSameSymbolOrders,
		Map<String, AssetDto> currencyAssets,
		Map<String, Double> currencyMarketPrices) {
		return currencyToSameSymbolOrders.entrySet().stream()
			.map(orderEntry -> orderAssetAssembler.assembleOrderAssetDto(orderEntry.getValue(),
				currencyAssets.get(orderEntry.getKey()),
				currencyMarketPrices.get(orderEntry.getKey())))
			.collect(Collectors.toList());
	}
}
