package com.turfai.booking.ai.language;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LanguageDetectorTest {

    private LanguageDetector languageDetector;

    @BeforeEach
    void setUp() {
        languageDetector = new LanguageDetector();
    }

    @Test
    @DisplayName("Should detect English language")
    void testEnglishDetection() {
        String lang = languageDetector.detectLanguage("Book slot for tomorrow", "EN");
        assertEquals("EN", lang);
    }

    @Test
    @DisplayName("Should detect Hindi language from Devanagari script")
    void testHindiDetection() {
        String lang = languageDetector.detectLanguage("कल का स्लॉट बुक करना है", "EN");
        assertEquals("HI", lang);
    }

    @Test
    @DisplayName("Should detect Marathi language from Devanagari script")
    void testMarathiDetection() {
        String lang = languageDetector.detectLanguage("उद्यासाठी स्लॉट बुक करायचा आहे", "EN");
        assertEquals("MR", lang);
    }

    @Test
    @DisplayName("Should detect Hinglish language from Romanized text")
    void testHinglishDetection() {
        String lang1 = languageDetector.detectLanguage("Book slot kal ke liye", "EN");
        assertEquals("HINGLISH", lang1);

        String lang2 = languageDetector.detectLanguage("Price kya hai?", "EN");
        assertEquals("HINGLISH", lang2);
    }

    @Test
    @DisplayName("Should detect Minglish language from mixed/Romanized text")
    void testMinglishDetection() {
        String lang1 = languageDetector.detectLanguage("Book slot उद्यासाठी", "EN");
        assertEquals("MINGLISH", lang1);

        String lang2 = languageDetector.detectLanguage("Rate काय आहे?", "EN");
        assertEquals("MINGLISH", lang2);

        String lang3 = languageDetector.detectLanguage("Udya sathi slot pahije", "EN");
        assertEquals("MINGLISH", lang3);
    }

    @Test
    @DisplayName("Should preserve session language for neutral input")
    void testNeutralInputPreservesSessionLanguage() {
        String lang = languageDetector.detectLanguage("6 to 7", "MR");
        assertEquals("MR", lang);
    }
}
