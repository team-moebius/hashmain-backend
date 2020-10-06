package com.moebius.backend.dto.trade;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.moebius.backend.domain.commons.Exchange;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@Builder(builderClassName = "AggregatedTradeHistoriesDtoBuilder")
@ToString
@JsonDeserialize(builder = AggregatedTradeHistoriesDto.AggregatedTradeHistoriesDtoBuilder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AggregatedTradeHistoriesDto {
	private List<AggregatedTradeHistoryDto> aggregatedTradeHistories;

	@JsonPOJOBuilder(withPrefix = "")
	public static class AggregatedTradeHistoriesDtoBuilder {
	}
}
