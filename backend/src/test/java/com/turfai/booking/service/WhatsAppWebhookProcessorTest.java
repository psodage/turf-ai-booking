package com.turfai.booking.service;

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
import com.turfai.booking.entity.User;
import com.turfai.booking.entity.UserRole;
import com.turfai.booking.exception.WebhookReplayException;
import com.turfai.booking.repository.BusinessRepository;
import com.turfai.booking.util.WhatsAppSignatureValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WhatsAppWebhookProcessorTest {

    @Mock private BusinessRepository businessRepository;
    @Mock private CustomerRegistrationService customerRegistrationService;
    @Mock private ConversationService conversationService;
    @Mock private WhatsAppProperties whatsappProperties;

    private WhatsAppWebhookProcessor whatsAppWebhookProcessor;
    private WhatsAppSignatureValidator signatureValidator;

    private Business testBusiness;
    private User testCustomer;

    @BeforeEach
    void setUp() {
        whatsAppWebhookProcessor = new WhatsAppWebhookProcessor(businessRepository, customerRegistrationService, conversationService, null);
        signatureValidator = new WhatsAppSignatureValidator(whatsappProperties);

        testBusiness = Business.builder()
                .name("Green Pitch Kolhapur")
                .whatsappPhoneNumberId("PHONE_NUM_ID_001")
                .timezone("Asia/Kolkata")
                .status(BusinessStatus.ACTIVE)
                .build();

        testCustomer = User.builder()
                .name("Test User")
                .phone("+919876543210")
                .role(UserRole.CUSTOMER)
                .business(null)
                .build();
    }

    @Test
    @DisplayName("Should process valid incoming WhatsApp text message payload")
    void testProcessValidTextMessagePayload() {
        long currentTimestamp = Instant.now().getEpochSecond();

        InboundWebhookPayload payload = createSamplePayload("PHONE_NUM_ID_001", "wamid.123", "919876543210", "Book slot tomorrow", String.valueOf(currentTimestamp));

        when(businessRepository.findByWhatsappPhoneNumberId("PHONE_NUM_ID_001")).thenReturn(Optional.of(testBusiness));
        when(conversationService.isMessageDuplicate("wamid.123")).thenReturn(false);
        when(customerRegistrationService.getOrCreateCustomer(eq("919876543210"), any())).thenReturn(testCustomer);

        Conversation mockConversation = Conversation.builder().id(UUID.randomUUID()).user(testCustomer).business(testBusiness).build();
        when(conversationService.getOrCreateActiveConversation(testCustomer, testBusiness)).thenReturn(mockConversation);

        whatsAppWebhookProcessor.processWebhookPayload(payload);

        verify(customerRegistrationService).getOrCreateCustomer(eq("919876543210"), any());
        verify(conversationService).saveIncomingMessage(eq(mockConversation), eq("Book slot tomorrow"), any(), eq("wamid.123"));
    }

    @Test
    @DisplayName("Should throw WebhookReplayException when payload timestamp is older than 5 minutes")
    void testReplayProtectionOldTimestamp() {
        long oldTimestamp = Instant.now().getEpochSecond() - 600; // 10 minutes ago (> 5 min)

        InboundWebhookPayload payload = createSamplePayload("PHONE_NUM_ID_001", "wamid.old", "919876543210", "Old message", String.valueOf(oldTimestamp));

        when(businessRepository.findByWhatsappPhoneNumberId("PHONE_NUM_ID_001")).thenReturn(Optional.of(testBusiness));

        assertThatThrownBy(() -> whatsAppWebhookProcessor.processWebhookPayload(payload))
                .isInstanceOf(WebhookReplayException.class)
                .hasMessageContaining("older than 5 minutes");
    }

    @Test
    @DisplayName("Should ignore duplicate message if wamid already processed")
    void testIgnoreDuplicateMessage() {
        long currentTimestamp = Instant.now().getEpochSecond();

        InboundWebhookPayload payload = createSamplePayload("PHONE_NUM_ID_001", "wamid.duplicate", "919876543210", "Duplicate message", String.valueOf(currentTimestamp));

        when(businessRepository.findByWhatsappPhoneNumberId("PHONE_NUM_ID_001")).thenReturn(Optional.of(testBusiness));
        when(conversationService.isMessageDuplicate("wamid.duplicate")).thenReturn(true);

        whatsAppWebhookProcessor.processWebhookPayload(payload);

        verify(customerRegistrationService, never()).getOrCreateCustomer(any(), any());
    }

    @Test
    @DisplayName("HMAC-SHA256 signature validator calculation check")
    void testHmacSha256Calculation() {
        when(whatsappProperties.getAppSecret()).thenReturn("test_secret");

        String payload = "{\"test\":\"data\"}";
        String signature = signatureValidator.calculateHmacSha256(payload, "test_secret");

        assertThat(signature).isNotNull().hasSize(64);
    }

    private InboundWebhookPayload createSamplePayload(String phoneNumId, String wamid, String fromPhone, String textBody, String timestamp) {
        WebhookMetadata metadata = new WebhookMetadata();
        metadata.setPhoneNumberId(phoneNumId);

        WebhookProfile profile = new WebhookProfile();
        profile.setName("Test User");

        WebhookContact contact = new WebhookContact();
        contact.setWaId(fromPhone);
        contact.setProfile(profile);

        WebhookText text = new WebhookText();
        text.setBody(textBody);

        WebhookMessage message = new WebhookMessage();
        message.setId(wamid);
        message.setFrom(fromPhone);
        message.setType("text");
        message.setText(text);
        message.setTimestamp(timestamp);

        WebhookValue value = new WebhookValue();
        value.setMetadata(metadata);
        value.setContacts(List.of(contact));
        value.setMessages(List.of(message));

        WebhookChange change = new WebhookChange();
        change.setField("messages");
        change.setValue(value);

        WebhookEntry entry = new WebhookEntry();
        entry.setId("ENTRY_01");
        entry.setChanges(List.of(change));

        InboundWebhookPayload payload = new InboundWebhookPayload();
        payload.setObject("whatsapp_business_account");
        payload.setEntry(List.of(entry));

        return payload;
    }
}
