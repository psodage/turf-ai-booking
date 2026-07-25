package com.turfai.booking.ai.tool;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToolResult {

    private boolean success;

    @JsonProperty("error_code")
    private String errorCode;

    private String message;

    private Object data;

    private List<String> suggestions;

    public static ToolResult success(Object data) {
        return ToolResult.builder()
                .success(true)
                .data(data)
                .build();
    }

    public static ToolResult success(String message, Object data) {
        return ToolResult.builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static ToolResult error(String errorCode, String message, List<String> suggestions) {
        return ToolResult.builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .suggestions(suggestions)
                .build();
    }
}
