package com.turfai.booking.ai.language;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Component
public class LanguageDetector {

    private static final Pattern DEVANAGARI_PATTERN = Pattern.compile("[\\u0900-\\u097F]");
    private static final Pattern LATIN_PATTERN = Pattern.compile("[a-zA-Z]");

    // Specific Marathi Devanagari Words
    private static final Set<String> MARATHI_DEVANAGARI_WORDS = Set.of(
            "उद्या", "काय", "आहे", "करायचा", "पाहिजे", "करा", "दाखवा", "रद्द", "माझे", "माझी", "माझा", "दर", "स्थान", "वेळ"
    );

    // Specific Hindi Devanagari Words
    private static final Set<String> HINDI_DEVANAGARI_WORDS = Set.of(
            "कल", "क्या", "है", "करना", "चाहिए", "दिखाओ", "रद्द", "मेरा", "मेरी", "मेरे", "रेट", "लोकेशन", "समय"
    );

    // Romanized Marathi Words / Patterns
    private static final Set<String> MINGLISH_WORDS = Set.of(
            "udya", "kay", "aahe", "pahije", "karaycha", "kuthe", "majhe", "majhi", "majha", "sathi", "sundar"
    );

    // Romanized Hindi Words / Patterns
    private static final Set<String> HINGLISH_WORDS = Set.of(
            "kal", "kya", "hai", "chahiye", "karo", "karna", "dikhao", "kitna", "mera", "meri", "mere", "ke", "liye", "bhai"
    );

    /**
     * Detects language from message text, preserving existing language for neutral messages.
     * Supported codes: EN, HI, MR, HINGLISH, MINGLISH.
     */
    public String detectLanguage(String text, String currentLanguage) {
        if (text == null || text.isBlank()) {
            return (currentLanguage != null && !currentLanguage.isBlank()) ? currentLanguage : "EN";
        }

        String cleaned = text.trim();
        String lower = cleaned.toLowerCase(Locale.ROOT);

        boolean hasDevanagari = DEVANAGARI_PATTERN.matcher(cleaned).find();
        boolean hasLatin = LATIN_PATTERN.matcher(cleaned).find();

        if (hasDevanagari) {
            // Check for Marathi vs Hindi Devanagari
            boolean isMarathiDev = containsAnyWord(lower, MARATHI_DEVANAGARI_WORDS);
            boolean isHindiDev = containsAnyWord(lower, HINDI_DEVANAGARI_WORDS);

            if (hasLatin) {
                // Mixed Latin + Devanagari
                if (isMarathiDev || lower.contains("उद्या") || lower.contains("काय")) {
                    log.info("Detected MINGLISH (Mixed English + Marathi Devanagari) for input: {}", text);
                    return "MINGLISH";
                }
                log.info("Detected HINGLISH (Mixed English + Hindi Devanagari) for input: {}", text);
                return "HINGLISH";
            }

            if (isMarathiDev) {
                log.info("Detected MR (Marathi) for input: {}", text);
                return "MR";
            }
            if (isHindiDev) {
                log.info("Detected HI (Hindi) for input: {}", text);
                return "HI";
            }
            // Default Devanagari fallback: Hindi
            log.info("Detected HI (Devanagari fallback) for input: {}", text);
            return "HI";
        }

        if (hasLatin) {
            // Check for Minglish (English + Marathi in Latin script)
            if (containsAnyWord(lower, MINGLISH_WORDS) || lower.contains("udya") || lower.contains("aahe") || lower.contains("kay")) {
                log.info("Detected MINGLISH for input: {}", text);
                return "MINGLISH";
            }

            // Check for Hinglish (English + Hindi in Latin script)
            if (containsAnyWord(lower, HINGLISH_WORDS) || lower.contains("kya") || lower.contains("hai") || lower.contains("kal") || lower.contains("chahiye")) {
                log.info("Detected HINGLISH for input: {}", text);
                return "HINGLISH";
            }

            // Check mixed explicit keywords requested by prompt like "Book slot उद्यासाठी" (covered above) or "Rate काय आहे?"
            if (lower.contains("rate kay") || lower.contains("kay aahe") || lower.contains("udya")) {
                log.info("Detected MINGLISH for input: {}", text);
                return "MINGLISH";
            }
            if (lower.contains("price kya") || lower.contains("kya hai") || lower.contains("kal ke liye")) {
                log.info("Detected HINGLISH for input: {}", text);
                return "HINGLISH";
            }
        }

        // Neutral inputs like numbers, single words, or pure English
        if (isPureEnglish(lower)) {
            log.info("Detected EN (English) for input: {}", text);
            return "EN";
        }

        return (currentLanguage != null && !currentLanguage.isBlank()) ? currentLanguage : "EN";
    }

    private boolean containsAnyWord(String text, Set<String> words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPureEnglish(String text) {
        // Simple heuristic: contains common English words or basic alphanumeric
        return text.contains("book") || text.contains("slot") || text.contains("tomorrow")
                || text.contains("price") || text.contains("location") || text.contains("cancel")
                || text.contains("availability") || text.contains("view") || text.contains("my booking")
                || text.contains("yes") || text.contains("no") || text.matches("^[a-zA-Z0-9\\s.,!?'-]+$");
    }
}
