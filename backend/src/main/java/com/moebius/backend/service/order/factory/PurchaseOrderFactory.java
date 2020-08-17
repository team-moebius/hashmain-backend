package com.moebius.backend.service.order.factory;

import com.moebius.backend.domain.orders.Order;
import com.moebius.backend.domain.orders.OrderPosition;
import com.moebius.backend.domain.orders.OrderRepository;
import com.moebius.backend.dto.trade.TradeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import static com.moebius.backend.utils.ThreadScheduler.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseOrderFactory implements OrderFactory {
	private final OrderRepository orderRepository;

	@Override
	public OrderPosition getPosition() {
		return OrderPosition.PURCHASE;
	}

	@Override
	public Flux<Order> getAndUpdateOrdersToInProgress(TradeDto tradeDto) {
		return orderRepository.findAndUpdateAllByBidCondition(tradeDto.getExchange(), tradeDto.getSymbol(), OrderPosition.PURCHASE, tradeDto.getPrice())
			.subscribeOn(IO.scheduler())
			.publishOn(COMPUTE.scheduler());
	}
}
