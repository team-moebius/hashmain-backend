package com.moebius.backend.service.order;

import com.moebius.backend.assembler.order.OrderAssembler;
import com.moebius.backend.domain.apikeys.ApiKey;
import com.moebius.backend.domain.commons.EventType;
import com.moebius.backend.domain.commons.Exchange;
import com.moebius.backend.domain.orders.Order;
import com.moebius.backend.domain.orders.OrderRepository;
import com.moebius.backend.domain.orders.OrderStatus;
import com.moebius.backend.domain.orders.OrderStatusCondition;
import com.moebius.backend.dto.OrderAssetDto;
import com.moebius.backend.dto.OrderDto;
import com.moebius.backend.dto.OrderStatusDto;
import com.moebius.backend.dto.TradeDto;
import com.moebius.backend.dto.exchange.AssetDto;
import com.moebius.backend.dto.frontend.response.OrderAssetResponseDto;
import com.moebius.backend.dto.frontend.response.OrderResponseDto;
import com.moebius.backend.exception.DataNotFoundException;
import com.moebius.backend.exception.ExceptionTypes;
import com.moebius.backend.exception.WrongDataException;
import com.moebius.backend.service.asset.AssetService;
import com.moebius.backend.service.market.MarketService;
import com.moebius.backend.service.member.ApiKeyService;
import com.moebius.backend.service.order.validator.OrderValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.moebius.backend.domain.commons.EventType.*;
import static com.moebius.backend.utils.ThreadScheduler.COMPUTE;
import static com.moebius.backend.utils.ThreadScheduler.IO;

@Slf4j
@Service
@RequiredArgsConstructor
public class InternalOrderService {
	private final OrderRepository orderRepository;
	private final OrderAssembler orderAssembler;
	private final OrderValidator orderValidator;
	private final ApiKeyService apiKeyService;
	private final OrderCacheService orderCacheService;
	private final AssetService assetService;
	private final MarketService marketService;

	public Mono<ResponseEntity<OrderResponseDto>> processOrders(String memberId, Exchange exchange, List<OrderDto> orderDtos) {
		orderValidator.validate(orderDtos);

		return apiKeyService.getApiKeyByMemberIdAndExchange(memberId, exchange)
			.subscribeOn(COMPUTE.scheduler())
			.flatMapIterable(apiKey -> orderDtos.stream()
				.map(orderDto -> processOrder(apiKey, orderDto))
				.collect(Collectors.toList()))
			.collectList()
			.doOnSuccess(this::evictOrderCaches)
			.map(orderAssembler::toResponseDto)
			.map(ResponseEntity::ok);
	}

	public Mono<ResponseEntity<OrderResponseDto>> getOrdersByExchange(String memberId, Exchange exchange) {
		return getOrders(memberId, exchange)
			.subscribeOn(COMPUTE.scheduler())
			.map(orderAssembler::toResponseDto)
			.map(ResponseEntity::ok);
	}

	public Mono<ResponseEntity<OrderResponseDto>> getOrdersByExchangeAndSymbol(String memberId, Exchange exchange, String symbol) {
		return getOrders(memberId, exchange).map(orders -> filterOrdersBySymbol(orders, symbol))
			.subscribeOn(COMPUTE.scheduler())
			.map(orderAssembler::toResponseDto)
			.map(ResponseEntity::ok);
	}

	public Mono<ResponseEntity<OrderAssetResponseDto>> getOrderAssets(String memberId, Exchange exchange) {
		return Mono.zip(
			getOrdersExceptDone(memberId, exchange).map(orderAssembler::toCurrencyOrderDtos),
			assetService.getCurrencyAssets(memberId, exchange),
			marketService.getCurrencyMarketPrices(exchange)
		).subscribeOn(COMPUTE.scheduler())
			.map(tuple -> extractOrderStatuses(tuple.getT1(), tuple.getT2(), tuple.getT3()))
			.map(orderAssembler::toStatusResponseDto)
			.map(ResponseEntity::ok);
	}

	@Cacheable(value = "readyOrderCount", key = "{#tradeDto.exchange, #tradeDto.symbol, 'READY'}")
	public Mono<Long> findOrderCountByTradeDto(TradeDto tradeDto) {
		log.info("[Order] [{}/{}] Start to get count of orders from repository because not found in cache.", tradeDto.getExchange(),
			tradeDto.getSymbol());
		return orderRepository.countBySymbolAndExchangeAndOrderStatus(tradeDto.getSymbol(), tradeDto.getExchange(), OrderStatus.READY)
			.subscribeOn(IO.scheduler())
			.publishOn(COMPUTE.scheduler())
			.cache();
	}

	public Flux<Order> findInProgressOrders(TradeDto tradeDto) {
		OrderStatusCondition inProgressStatusCondition = orderAssembler.assembleInProgressStatusCondition(tradeDto);

		return orderRepository.findAllByOrderStatusCondition(inProgressStatusCondition)
			.subscribeOn(IO.scheduler())
			.publishOn(COMPUTE.scheduler());
	}

	public Mono<Order> updateOrderStatus(Order order, OrderStatusDto orderStatusDto) {
		return orderRepository.save(orderAssembler.assembleUpdatedStatusOrder(order, orderStatusDto))
			.subscribeOn(IO.scheduler())
			.publishOn(COMPUTE.scheduler());
	}

	private OrderDto processOrder(ApiKey apiKey, OrderDto orderDto) {
		EventType eventType = orderDto.getEventType();

		if (eventType == CREATE) {
			createOrder(apiKey, orderDto).subscribe();
		} else if (eventType == UPDATE) {
			updateOrder(orderDto).subscribe();
		} else if (eventType == DELETE) {
			deleteOrder(orderDto.getId()).subscribe();
		}

		return orderDto;
	}

	private Mono<Order> createOrder(ApiKey apiKey, OrderDto orderDto) {
		return orderRepository.save(orderAssembler.assembleOrderWhenCreate(apiKey, orderDto))
			.subscribeOn(IO.scheduler())
			.publishOn(COMPUTE.scheduler());
	}

	private Mono<Order> updateOrder(OrderDto orderDto) {
		if (orderDto.getId() == null) {
			throw new WrongDataException("[Order] There isn't id in UPDATE or DELETE event dto. [" + orderDto + "]");
		}

		return orderRepository.findById(new ObjectId(orderDto.getId()))
			.map(order -> orderAssembler.assembleOrderWhenUpdate(order, orderDto))
			.flatMap(orderRepository::save)
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
			.forEach(symbol -> orderCacheService.evictOrderCount(orderDtos.get(0).getExchange(), symbol));
	}

	private Mono<List<OrderDto>> getOrders(String memberId, Exchange exchange) {
		return apiKeyService.getApiKeyByMemberIdAndExchange(memberId, exchange)
			.map(apiKey -> orderRepository.findAllByApiKeyId(apiKey.getId()))
			.subscribeOn(IO.scheduler())
			.publishOn(COMPUTE.scheduler())
			.switchIfEmpty(Mono.defer(() -> Mono.error(new DataNotFoundException(
				ExceptionTypes.NONEXISTENT_DATA.getMessage("[Order] order information based on memberId(" + memberId + ")")))))
			.flatMap(orders -> orders.map(order -> orderAssembler.toDto(order, EventType.READ))
				.collectList());
	}

	private Mono<List<OrderDto>> getOrdersExceptDone(String memberId, Exchange exchange) {
		return apiKeyService.getApiKeyByMemberIdAndExchange(memberId, exchange)
			.map(apiKey -> orderRepository.findAllByApiKeyIdAndOrderStatusNot(apiKey.getId(), OrderStatus.DONE))
			.subscribeOn(IO.scheduler())
			.publishOn(COMPUTE.scheduler())
			.switchIfEmpty(Mono.defer(() -> Mono.error(new DataNotFoundException(
				ExceptionTypes.NONEXISTENT_DATA.getMessage("[Order] order information based on memberId(" + memberId + ")")))))
			.flatMap(orders -> orders.map(order -> orderAssembler.toDto(order, EventType.READ))
				.collectList());
	}

	private List<OrderDto> filterOrdersBySymbol(List<OrderDto> orderDtos, String symbol) {
		return orderDtos.stream()
			.filter(orderDto -> orderDto.getSymbol().equals(symbol.toUpperCase()))
			.collect(Collectors.toList());
	}

	private List<OrderAssetDto> extractOrderStatuses(Map<String, List<OrderDto>> currencyOrders,
		Map<String, AssetDto> currencyAssets,
		Map<String, Double> currencyMarketPrices) {
		return currencyOrders.entrySet().stream()
			.map(orderEntry -> orderAssembler.toOrderAssetDto(orderEntry.getValue(),
				currencyAssets.get(orderEntry.getKey()),
				currencyMarketPrices.get(orderEntry.getKey())))
			.collect(Collectors.toList());
	}
}