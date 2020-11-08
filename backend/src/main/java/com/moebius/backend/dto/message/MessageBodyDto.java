package com.moebius.backend.dto.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class MessageBodyDto<T> {
    private String templateId;
    private T parameters;
}
