package com.turfai.booking.ai.provider;

import com.turfai.booking.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
@RequiredArgsConstructor
public class OpenAiProvider implements AiProvider {

    private final AiProperties aiProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    @SuppressWarnings("unchecked")
    public AiResponse generateResponse(AiRequest request) {
        log.info("Sending request to OpenAI API using model {}", aiProperties.getModel());

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", aiProperties.getModel());
        payload.put("max_tokens", aiProperties.getMaxTokens());

        List<Map<String, String>> messagesList = new ArrayList<>();
        if (request.getSystemPrompt() != null) {
            messagesList.add(Map.of("role", "system", "content", request.getSystemPrompt()));
        }
        if (request.getMessages() != null) {
            messagesList.addAll(request.getMessages());
        }
        payload.put("messages", messagesList);

        if (request.getTools() != null && !request.getTools().isEmpty()) {
            payload.put("tools", request.getTools());
            payload.put("tool_choice", "auto");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiProperties.getApiKey());

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);

        try {
            Map<String, Object> responseMap = restTemplate.postForObject(aiProperties.getApiUrl(), requestEntity, Map.class);
            return parseOpenAiResponse(responseMap);
        } catch (Exception ex) {
            log.error("OpenAI API call failed", ex);
            return AiResponse.builder()
                    .isToolCall(false)
                    .content("I'm sorry, I am currently experiencing technical difficulties processing your request. Please try again shortly.")
                    .build();
        }
    }

    @SuppressWarnings("unchecked")
    private AiResponse parseOpenAiResponse(Map<String, Object> responseMap) {
        if (responseMap == null || !responseMap.containsKey("choices")) {
            return AiResponse.builder().isToolCall(false).content("No response from AI provider.").build();
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
        if (choices.isEmpty()) {
            return AiResponse.builder().isToolCall(false).content("Empty choice from AI provider.").build();
        }

        Map<String, Object> firstChoice = choices.get(0);
        Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");

        Map<String, Object> usage = (Map<String, Object>) responseMap.get("usage");
        int promptTokens = usage != null && usage.get("prompt_tokens") != null ? ((Number) usage.get("prompt_tokens")).intValue() : 0;
        int completionTokens = usage != null && usage.get("completion_tokens") != null ? ((Number) usage.get("completion_tokens")).intValue() : 0;

        if (message != null && message.containsKey("tool_calls")) {
            List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) message.get("tool_calls");
            if (!toolCalls.isEmpty()) {
                Map<String, Object> firstTool = toolCalls.get(0);
                Map<String, Object> function = (Map<String, Object>) firstTool.get("function");
                String name = (String) function.get("name");
                String argumentsJson = (String) function.get("arguments");

                return AiResponse.builder()
                        .isToolCall(true)
                        .toolName(name)
                        .toolArguments(Map.of("raw_arguments", argumentsJson))
                        .promptTokens(promptTokens)
                        .completionTokens(completionTokens)
                        .build();
            }
        }

        String textContent = message != null ? (String) message.get("content") : "No content";
        return AiResponse.builder()
                .isToolCall(false)
                .content(textContent)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .build();
    }
}
