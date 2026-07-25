package com.turfai.booking.ai.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiResponse {
    private String content;
    private boolean isToolCall;
    private String toolName;
    private Map<String, Object> toolArguments;
    private int promptTokens;
    private int completionTokens;
}
