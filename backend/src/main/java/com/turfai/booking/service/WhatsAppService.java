package com.turfai.booking.service;

import com.turfai.booking.config.WhatsAppProperties;
import com.turfai.booking.dto.whatsapp.outbound.OutboundButton;
import com.turfai.booking.dto.whatsapp.outbound.OutboundSection;
import com.turfai.booking.dto.whatsapp.outbound.TemplateComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppService {

    private final WhatsAppProperties whatsappProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    public boolean sendTextMessage(String toPhone, String text) {
        Map<String, Object> body = new HashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", sanitizePhone(toPhone));
        body.put("type", "text");
        body.put("text", Map.of("body", text));

        return executeOutboundPost(body);
    }

    public boolean sendInteractiveButtons(String toPhone, String bodyText, List<OutboundButton> buttons) {
        Map<String, Object> interactive = new HashMap<>();
        interactive.put("type", "button");
        interactive.put("body", Map.of("text", bodyText));

        List<Map<String, Object>> buttonList = new ArrayList<>();
        for (OutboundButton b : buttons) {
            buttonList.add(Map.of(
                    "type", "reply",
                    "reply", Map.of("id", b.getId(), "title", b.getTitle())
            ));
        }
        interactive.put("action", Map.of("buttons", buttonList));

        Map<String, Object> body = new HashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", sanitizePhone(toPhone));
        body.put("type", "interactive");
        body.put("interactive", interactive);

        return executeOutboundPost(body);
    }

    public boolean sendListMessage(String toPhone, String bodyText, String buttonText, List<OutboundSection> sections) {
        return sendListMessage(toPhone, null, bodyText, buttonText, sections);
    }

    public boolean sendListMessage(String toPhone, String headerText, String bodyText, String buttonText, List<OutboundSection> sections) {
        Map<String, Object> interactive = new HashMap<>();
        interactive.put("type", "list");

        if (headerText != null && !headerText.isBlank()) {
            interactive.put("header", Map.of("type", "text", "text", headerText));
        }

        interactive.put("body", Map.of("text", bodyText));

        List<Map<String, Object>> sectionList = new ArrayList<>();
        for (OutboundSection sec : sections) {
            List<Map<String, Object>> rowList = new ArrayList<>();
            sec.getRows().forEach(r -> {
                Map<String, Object> rowMap = new HashMap<>();
                rowMap.put("id", r.getId());
                rowMap.put("title", r.getTitle());
                if (r.getDescription() != null) {
                    rowMap.put("description", r.getDescription());
                }
                rowList.add(rowMap);
            });
            sectionList.add(Map.of("title", sec.getTitle(), "rows", rowList));
        }

        interactive.put("action", Map.of("button", buttonText, "sections", sectionList));

        Map<String, Object> body = new HashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", sanitizePhone(toPhone));
        body.put("type", "interactive");
        body.put("interactive", interactive);

        return executeOutboundPost(body);
    }

    public boolean sendLocationMessage(String toPhone, double latitude, double longitude, String name, String address) {
        Map<String, Object> location = new HashMap<>();
        location.put("latitude", latitude);
        location.put("longitude", longitude);
        if (name != null && !name.isBlank()) {
            location.put("name", name);
        }
        if (address != null && !address.isBlank()) {
            location.put("address", address);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", sanitizePhone(toPhone));
        body.put("type", "location");
        body.put("location", location);

        return executeOutboundPost(body);
    }

    public boolean sendTemplateMessage(String toPhone, String templateName, String languageCode, List<TemplateComponent> components) {
        Map<String, Object> templateMap = new HashMap<>();
        templateMap.put("name", templateName);
        templateMap.put("language", Map.of("code", languageCode != null ? languageCode : "en"));
        if (components != null && !components.isEmpty()) {
            templateMap.put("components", components);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", sanitizePhone(toPhone));
        body.put("type", "template");
        body.put("template", templateMap);

        return executeOutboundPost(body);
    }

    public boolean sendDocumentMessage(String toPhone, String documentUrl, String filename, String caption) {
        Map<String, Object> docMap = new HashMap<>();
        docMap.put("link", documentUrl);
        if (filename != null) docMap.put("filename", filename);
        if (caption != null) docMap.put("caption", caption);

        Map<String, Object> body = new HashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", sanitizePhone(toPhone));
        body.put("type", "document");
        body.put("document", docMap);

        return executeOutboundPost(body);
    }

    private boolean executeOutboundPost(Map<String, Object> payload) {
        String endpointUrl = String.format("%s/%s/messages", whatsappProperties.getApiUrl(), whatsappProperties.getPhoneNumberId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(whatsappProperties.getAccessToken());

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);

        int maxRetries = 2;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                restTemplate.postForEntity(endpointUrl, requestEntity, String.class);
                log.info("Successfully sent WhatsApp message to {}", payload.get("to"));
                return true;
            } catch (Exception ex) {
                log.warn("Attempt {}/{} to send WhatsApp message failed: {}", attempt, maxRetries, ex.getMessage());
                if (attempt == maxRetries) {
                    log.error("Failed to deliver WhatsApp message after {} retries", maxRetries, ex);
                }
            }
        }
        return false;
    }

    private String sanitizePhone(String phone) {
        if (phone == null) return "";
        return phone.replaceAll("[^0-9]", "");
    }
}
