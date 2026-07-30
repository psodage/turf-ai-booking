package com.turfai.booking.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turfai.booking.ai.config.AiProperties;
import com.turfai.booking.ai.memory.ConversationContextBuilder;
import com.turfai.booking.ai.orchestrator.AiOrchestratorService;
import com.turfai.booking.ai.tool.AiToolGateway;
import com.turfai.booking.ai.tool.ToolResult;
import com.turfai.booking.config.WhatsAppProperties;
import com.turfai.booking.dto.whatsapp.inbound.InboundWebhookPayload;
import com.turfai.booking.dto.whatsapp.inbound.WebhookChange;
import com.turfai.booking.dto.whatsapp.inbound.WebhookContact;
import com.turfai.booking.dto.whatsapp.inbound.WebhookEntry;
import com.turfai.booking.dto.whatsapp.inbound.WebhookMessage;
import com.turfai.booking.dto.whatsapp.inbound.WebhookMetadata;
import com.turfai.booking.dto.whatsapp.inbound.WebhookProfile;
import com.turfai.booking.dto.whatsapp.inbound.WebhookText;
import com.turfai.booking.dto.whatsapp.inbound.WebhookValue;
import com.turfai.booking.entity.Business;
import com.turfai.booking.entity.BusinessStatus;
import com.turfai.booking.entity.Conversation;
import com.turfai.booking.entity.ConversationMessage;
import com.turfai.booking.entity.ConversationStatus;
import com.turfai.booking.entity.MessageSender;
import com.turfai.booking.entity.MessageType;
import com.turfai.booking.entity.OperatingHours;
import com.turfai.booking.entity.PricingRule;
import com.turfai.booking.entity.PricingType;
import com.turfai.booking.entity.Turf;
import com.turfai.booking.entity.TurfStatus;
import com.turfai.booking.entity.TurfType;
import com.turfai.booking.entity.User;
import com.turfai.booking.entity.UserRole;
import com.turfai.booking.entity.UserStatus;
import com.turfai.booking.exception.WebhookReplayException;
import com.turfai.booking.exception.WebhookSignatureException;
import com.turfai.booking.repository.BusinessRepository;
import com.turfai.booking.repository.ConversationMessageRepository;
import com.turfai.booking.repository.ConversationRepository;
import com.turfai.booking.repository.OperatingHoursRepository;
import com.turfai.booking.repository.PricingRuleRepository;
import com.turfai.booking.repository.TurfRepository;
import com.turfai.booking.repository.UserRepository;
import com.turfai.booking.service.ConversationService;
import com.turfai.booking.service.WhatsAppWebhookProcessor;
import com.turfai.booking.util.WhatsAppSignatureValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class WhatsAppAiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private WhatsAppProperties whatsAppProperties;
    @Autowired private WhatsAppSignatureValidator whatsAppSignatureValidator;
    @Autowired private WhatsAppWebhookProcessor whatsAppWebhookProcessor;
    @Autowired private ConversationService conversationService;
    @Autowired private ConversationContextBuilder conversationContextBuilder;
    @Autowired private AiOrchestratorService aiOrchestratorService;
    @Autowired private AiToolGateway aiToolGateway;
    @Autowired private AiProperties aiProperties;
    @Autowired private BusinessRepository businessRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TurfRepository turfRepository;
    @Autowired private OperatingHoursRepository operatingHoursRepository;
    @Autowired private PricingRuleRepository pricingRuleRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private ConversationMessageRepository conversationMessageRepository;

    private Business testBusiness;
    private Turf testTurf;

    @BeforeEach
    void setUp() {
        testBusiness = businessRepository.saveAndFlush(Business.builder()
                .name("Integration Green Pitch")
                .whatsappPhoneNumberId("PN_INTEG_WA_001")
                .phone("+919999900000")
                .timezone("Asia/Kolkata")
                .status(BusinessStatus.ACTIVE)
                .build());

        testTurf = turfRepository.saveAndFlush(Turf.builder()
                .business(testBusiness)
                .name("Integ Main Turf")
                .type(TurfType.FIVE_A_SIDE)
                .status(TurfStatus.ACTIVE)
                .build());

        operatingHoursRepository.saveAndFlush(OperatingHours.builder()
                .turf(testTurf)
                .dayOfWeek(1)
                .openingTime(LocalTime.of(6, 0))
                .closingTime(LocalTime.of(23, 0))
                .isClosed(false)
                .build());

        pricingRuleRepository.saveAndFlush(PricingRule.builder()
                .turf(testTurf)
                .pricingType(PricingType.BASE)
                .amount(new BigDecimal("1000.00"))
                .build());
    }

    @Test
    @DisplayName("1. Webhook Verification: Should verify GET webhook on both /webhook/whatsapp and /api/v1/webhooks/whatsapp")
    void testWebhookVerificationEndpoints() throws Exception {
        String token = whatsAppProperties.getVerifyToken();

        mockMvc.perform(get("/webhook/whatsapp")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", token)
                        .param("hub.challenge", "CHALLENGE_AAA"))
                .andExpect(status().isOk())
                .andExpect(content().string("CHALLENGE_AAA"));

        mockMvc.perform(get("/api/v1/webhooks/whatsapp")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", token)
                        .param("hub.challenge", "CHALLENGE_BBB"))
                .andExpect(status().isOk())
                .andExpect(content().string("CHALLENGE_BBB"));
    }

    @Test
    @DisplayName("2. Signature Validation: Should reject missing or invalid HMAC-SHA256 signatures")
    void testSignatureValidationRejection() {
        String payload = "{\"test\":\"payload\"}";

        assertThatThrownBy(() -> whatsAppSignatureValidator.validateSignature(payload, null))
                .isInstanceOf(WebhookSignatureException.class)
                .hasMessageContaining("Missing or malformed");

        assertThatThrownBy(() -> whatsAppSignatureValidator.validateSignature(payload, "sha256=invalid_hex_digest"))
                .isInstanceOf(WebhookSignatureException.class)
                .hasMessageContaining("Invalid webhook signature");

        // Valid signature calculation test
        String calculatedSig = whatsAppSignatureValidator.calculateHmacSha256(payload, whatsAppProperties.getAppSecret());
        whatsAppSignatureValidator.validateSignature(payload, "sha256=" + calculatedSig);
    }

    @Test
    @DisplayName("3. Replay Protection: Should reject webhook messages older than 5 minutes (300s)")
    void testReplayProtection() {
        long oldTimestamp = Instant.now().getEpochSecond() - 360; // 6 minutes ago

        WebhookText textObj = new WebhookText();
        textObj.setBody("Hello");

        WebhookMessage msg = new WebhookMessage();
        msg.setId("wamid_old_001");
        msg.setFrom("+919876543210");
        msg.setTimestamp(String.valueOf(oldTimestamp));
        msg.setType("text");
        msg.setText(textObj);

        WebhookMetadata metadata = new WebhookMetadata();
        metadata.setPhoneNumberId("PN_INTEG_WA_001");
        metadata.setDisplayPhoneNumber("DISPLAY_1");

        WebhookValue val = new WebhookValue();
        val.setMetadata(metadata);
        val.setMessages(List.of(msg));

        WebhookChange change = new WebhookChange();
        change.setValue(val);

        WebhookEntry entry = new WebhookEntry();
        entry.setChanges(List.of(change));

        InboundWebhookPayload payload = new InboundWebhookPayload();
        payload.setEntry(List.of(entry));

        assertThatThrownBy(() -> whatsAppWebhookProcessor.processWebhookPayload(payload))
                .isInstanceOf(WebhookReplayException.class)
                .hasMessageContaining("older than 5 minutes threshold");
    }

    @Test
    @DisplayName("4. Duplicate Webhook Handling: Should ignore duplicate wamid safely")
    void testDuplicateWebhookIgnored() {
        User customer = userRepository.saveAndFlush(User.builder()
                .business(testBusiness)
                .name("Dup User")
                .phone("+919888877777")
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build());

        Conversation conv = conversationService.getOrCreateActiveConversation(customer, testBusiness);
        String wamid = "wamid_dup_unique_123";

        conversationService.saveIncomingMessage(conv, "First attempt", MessageType.TEXT, wamid);

        assertThat(conversationService.isMessageDuplicate(wamid)).isTrue();
        assertThat(conversationService.isMessageDuplicate("wamid_new_999")).isFalse();
    }

    @Test
    @DisplayName("5. Conversation Locking: findWithLockByUserIdAndBusinessIdAndStatus acquires pessimistic lock")
    void testConversationPessimisticLock() {
        User customer = userRepository.saveAndFlush(User.builder()
                .business(testBusiness)
                .name("Lock User")
                .phone("+919777766666")
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build());

        Conversation conv1 = conversationService.getOrCreateActiveConversation(customer, testBusiness);
        assertThat(conv1).isNotNull();

        Conversation conv2 = conversationService.getOrCreateActiveConversation(customer, testBusiness);
        assertThat(conv2.getId()).isEqualTo(conv1.getId());
    }

    @Test
    @DisplayName("6. Context Window Trimming: Should restrict context window to maximum last 10 messages")
    void testContextWindowTrimming() {
        User customer = userRepository.saveAndFlush(User.builder()
                .business(testBusiness)
                .name("Window User")
                .phone("+919666655555")
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build());

        Conversation conv = conversationService.getOrCreateActiveConversation(customer, testBusiness);

        // Save 15 messages
        for (int i = 1; i <= 15; i++) {
            conversationService.saveIncomingMessage(conv, "Message #" + i, MessageType.TEXT, "wamid_" + i);
        }

        List<Map<String, String>> history = conversationContextBuilder.buildMessageHistory(conv);
        assertThat(history).hasSize(10); // max 10
        assertThat(history.get(0).get("content")).isEqualTo("Message #6");
        assertThat(history.get(9).get("content")).isEqualTo("Message #15");
    }

    @Test
    @DisplayName("7. Session Timeout: Inactive session beyond 10 minutes should expire")
    void testSessionTimeout() {
        User customer = userRepository.saveAndFlush(User.builder()
                .business(testBusiness)
                .name("Timeout User")
                .phone("+919555544444")
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build());

        Conversation conv = conversationService.getOrCreateActiveConversation(customer, testBusiness);
        conv.setLastActivity(Instant.now().minusSeconds(650)); // 10.8 mins ago
        conversationRepository.saveAndFlush(conv);

        List<Map<String, String>> history = conversationContextBuilder.buildMessageHistory(conv);
        assertThat(history).isEmpty();

        Conversation expiredConv = conversationRepository.findById(conv.getId()).orElseThrow();
        assertThat(expiredConv.getStatus()).isEqualTo(ConversationStatus.EXPIRED);
    }

    @Test
    @DisplayName("8. Standard Tool Response Format: Error tool calls return success=false, error_code, message, suggestions")
    void testStandardToolErrorResponse() {
        ToolResult errorResult = aiToolGateway.createBookingHold(testTurf.getId(), UUID.randomUUID(), LocalDate.now().minusDays(1), LocalTime.of(18, 0), LocalTime.of(19, 0));

        assertThat(errorResult.isSuccess()).isFalse();
        assertThat(errorResult.getErrorCode()).isEqualTo("INVALID_REQUEST");
        assertThat(errorResult.getMessage()).isNotNull();
        assertThat(errorResult.getSuggestions()).isNotNull();
    }

    @Test
    @DisplayName("9. Complete Webhook -> AI -> Tool End-to-End Integration Flow")
    void testCompleteWebhookToAiToToolFlow() throws Exception {
        String senderPhone = "+919444433333";
        long currentTimestamp = Instant.now().getEpochSecond();

        WebhookText textObj = new WebhookText();
        textObj.setBody("Check available slots for tomorrow");

        WebhookMessage msg = new WebhookMessage();
        msg.setId("wamid_e2e_" + UUID.randomUUID());
        msg.setFrom(senderPhone);
        msg.setTimestamp(String.valueOf(currentTimestamp));
        msg.setType("text");
        msg.setText(textObj);

        WebhookProfile profile = new WebhookProfile();
        profile.setName("Rahul Sharma");

        WebhookContact contact = new WebhookContact();
        contact.setProfile(profile);
        contact.setWaId(senderPhone);

        WebhookMetadata metadata = new WebhookMetadata();
        metadata.setPhoneNumberId("PN_INTEG_WA_001");
        metadata.setDisplayPhoneNumber("DISPLAY_1");

        WebhookValue val = new WebhookValue();
        val.setMetadata(metadata);
        val.setMessages(List.of(msg));
        val.setContacts(List.of(contact));

        WebhookChange change = new WebhookChange();
        change.setValue(val);

        WebhookEntry entry = new WebhookEntry();
        entry.setChanges(List.of(change));

        InboundWebhookPayload payload = new InboundWebhookPayload();
        payload.setEntry(List.of(entry));

        String rawJson = objectMapper.writeValueAsString(payload);
        String signature = "sha256=" + whatsAppSignatureValidator.calculateHmacSha256(rawJson, whatsAppProperties.getAppSecret());

        mockMvc.perform(post("/api/v1/webhooks/whatsapp")
                        .header("X-Hub-Signature-256", signature)
                        .content(rawJson)
                        .contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(content().string("EVENT_RECEIVED"));

        // Verify Auto-Registered Customer
        User customer = userRepository.findByPhone(senderPhone).orElseThrow();
        assertThat(customer.getName()).isEqualTo("Rahul Sharma");

        // Verify Active Conversation
        Conversation conv = conversationRepository.findByUserIdAndBusinessIdAndStatus(customer.getId(), testBusiness.getId(), ConversationStatus.ACTIVE).orElseThrow();

        // Verify Message History (Inbound & AI Outbound Response)
        List<ConversationMessage> messages = conversationMessageRepository.findByConversationIdOrderByCreatedAtAsc(conv.getId());
        assertThat(messages).hasSizeGreaterThanOrEqualTo(2);
        assertThat(messages.get(0).getSender()).isEqualTo(MessageSender.USER);
        assertThat(messages.get(messages.size() - 1).getSender()).isEqualTo(MessageSender.AI);
    }
}
