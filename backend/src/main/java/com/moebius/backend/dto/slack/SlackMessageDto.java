package com.moebius.backend.dto.slack;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@Builder
@ToString
public class SlackMessageDto {
	private List<SlackAttachment> attachments;

	@Getter
	@Builder
	@ToString
	public static class SlackAttachment {
		private String color;
		@JsonProperty("author_name")
		private String authorName;
		@JsonProperty("author_link")
		private String authorLink;
		private String text;
		private List<Field> fields;
		private String footer;
		@JsonProperty("ts")
		private String timestamp;

		@Getter
		@Builder
		@ToString
		public static class Field {
			private String title;
			private String value;
			@JsonProperty("short")
			private boolean isShort;
		}
	}
}
