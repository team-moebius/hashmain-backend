package com.moebius.backend.dto.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.moebius.backend.domain.commons.EventType;
import com.moebius.backend.domain.commons.Exchange;
import com.moebius.backend.domain.orders.OrderPosition;
import com.moebius.backend.domain.orders.OrderStatus;
import com.moebius.backend.domain.orders.OrderType;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.lang.Nullable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderDto {
	@Id
	@ApiModelProperty(notes = "Order Id, 이미 생성된 order일 경우 삭제 또는 수정 시 반드시 넣어서 보내줘야 한다.")
	@Nullable
	private String id;
	@NotNull
	private EventType eventType;
	@NotNull
	private Exchange exchange;
	@NotNull
	private String symbol;
	@NotNull
	private OrderPosition orderPosition;
	@NotNull
	private OrderStatus orderStatus;
	@NotNull
	private OrderType orderType;
	@PositiveOrZero
	private double price;
	@PositiveOrZero
	private double volume;
	@Positive
	private int level;
}
