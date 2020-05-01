package com.moebius.backend.service.order.factory;

import com.moebius.backend.domain.orders.Order;
import com.moebius.backend.domain.orders.OrderPosition;
import com.moebius.backend.domain.orders.OrderRepository;
import com.moebius.backend.dto.TradeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import static com.moebius.backend.utils.ThreadScheduler.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class SaleOrderFactory implements OrderFactory {
	private final OrderRepository orderRepository;

	@Override
	public OrderPosition getPosition() {
		return OrderPosition.SALE;
	}

	@Override
	public Flux<Order> getAndUpdateOrdersToDone(TradeDto tradeDto) {
		return orderRepository.findAndUpdateAllByAskCondition(tradeDto.getExchange(), tradeDto.getSymbol(), OrderPosition.SALE, tradeDto.getPrice())
			.subscribeOn(IO.scheduler())
			.publishOn(COMPUTE.scheduler());
	}
}