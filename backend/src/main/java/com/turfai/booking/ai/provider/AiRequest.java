package com.turfai.booking.ai.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRequest {
    private String systemPrompt;
    private List<Map<String, String>> messages; // role: user/assistant, content
    private List<Map<String, Object>> tools;
}
