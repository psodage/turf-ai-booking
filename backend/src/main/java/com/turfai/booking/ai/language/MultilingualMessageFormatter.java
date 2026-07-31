package com.turfai.booking.ai.language;

import com.turfai.booking.dto.whatsapp.outbound.OutboundRow;
import com.turfai.booking.dto.whatsapp.outbound.OutboundSection;
import com.turfai.booking.entity.UserRole;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MultilingualMessageFormatter {

    public record MenuConfig(String header, String body, String buttonText, List<OutboundSection> sections) {}

    public MenuConfig getMenuConfig(String lang) {
        return getMenuConfig(UserRole.CUSTOMER, lang);
    }

    public MenuConfig getMenuConfig(UserRole role, String lang) {
        String language = normalizeLang(lang);
        boolean isOwnerOrManager = (role == UserRole.OWNER || role == UserRole.MANAGER);

        String header = switch (language) {
            case "HI" -> isOwnerOrManager 
                    ? "👔 Owner Dashboard — Green Pitch Kolhapur"
                    : "👋 Green Pitch Kolhapur में आपका स्वागत है";
            case "MR" -> isOwnerOrManager 
                    ? "👔 Owner Dashboard — Green Pitch Kolhapur"
                    : "👋 Green Pitch Kolhapur मध्ये आपले स्वागत आहे";
            default -> isOwnerOrManager 
                    ? "👔 Owner Dashboard — Green Pitch Kolhapur"
                    : "👋 Welcome to Green Pitch Kolhapur";
        };

        String body = switch (language) {
            case "HI" -> isOwnerOrManager 
                    ? "मालिक/प्रबंधक विकल्प चुनें:"
                    : "कृपया नीचे दिए गए विकल्पों में से चुनें या अपना प्रश्न लिखें।";
            case "MR" -> isOwnerOrManager 
                    ? "मालक/व्यवस्थापक पर्याय निवडा:"
                    : "कृपया खालील पर्यायांपैकी निवडा किंवा तुमचा प्रश्न टाईप करा.";
            case "HINGLISH" -> isOwnerOrManager 
                    ? "Owner/Manager action select karein:"
                    : "Kripya neeche diye gaye options mein se select karein ya apna query type karein.";
            case "MINGLISH" -> isOwnerOrManager 
                    ? "Owner/Manager action select kara:"
                    : "Krupaya khalil options pakin select kara kiwa tumcha query type kara.";
            default -> isOwnerOrManager 
                    ? "Select an owner dashboard action below:"
                    : "Please select an option below or type your query.";
        };

        String buttonText = switch (language) {
            case "HI" -> isOwnerOrManager ? "मालिक विकल्प" : "मेनू विकल्प";
            case "MR" -> isOwnerOrManager ? "मालक पर्याय" : "मेनू पर्याय";
            default -> isOwnerOrManager ? "Owner Actions" : "Menu Options";
        };

        String secTitle = switch (language) {
            case "HI" -> isOwnerOrManager ? "ऑपरेशनल टूल" : "मुख्य विकल्प";
            case "MR" -> isOwnerOrManager ? "ऑपरेशनल साधने" : "मुख्य पर्याय";
            default -> isOwnerOrManager ? "Owner Tools" : "Menu Options";
        };

        List<OutboundRow> rows;

        if (isOwnerOrManager) {
            rows = switch (language) {
                case "HI" -> List.of(
                        OutboundRow.builder().id("get_business_summary").title("📊 आज का सारांश और राजस्व").description("कुल बुकिंग, पुष्ट स्थिति और राजस्व देखें").build(),
                        OutboundRow.builder().id("get_today_bookings").title("📋 आज का पूरा शेड्यूल").description("आज की सभी ग्राहक बुकिंग देखें").build(),
                        OutboundRow.builder().id("block_slot").title("🚫 स्लॉट ब्लॉक करें (रखरखाव)").description("रखरखाव या ऑफ़लाइन बुकिंग के लिए स्लॉट ब्लॉक करें").build(),
                        OutboundRow.builder().id("unblock_slot").title("🔓 स्लॉट अनब्लॉक करें").description("अवरुद्ध स्लॉट को अनब्लॉक करें").build(),
                        OutboundRow.builder().id("excel_report").title("📥 एक्सेल रिपोर्ट डाउनलोड करें").description("दैनिक/मासिक एक्सेल रिपोर्ट प्राप्त करें").build()
                );
                case "MR" -> List.of(
                        OutboundRow.builder().id("get_business_summary").title("📊 आजचा सारांश आणि महसूल").description("एकूण बुकिंग, कन्फर्म संख्या आणि महसूल पहा").build(),
                        OutboundRow.builder().id("get_today_bookings").title("📋 आजचे पूर्ण वेळापत्रक").description("आजच्या सर्व ग्राहकांच्या बुकिंग पहा").build(),
                        OutboundRow.builder().id("block_slot").title("🚫 स्लॉट ब्लॉक करा").description("मेंटेनन्स किंवा ऑफलाइन बुकिंगसाठी स्लॉट ब्लॉक करा").build(),
                        OutboundRow.builder().id("unblock_slot").title("🔓 स्लॉट अनब्लॉक करा").description("ब्लॉक केलेला स्लॉट अनब्लॉक करा").build(),
                        OutboundRow.builder().id("excel_report").title("📥 एक्सेल रिपोर्ट डाउनलोड करा").description("दैनिक/मासिक एक्सेल रिपोर्ट मिळवा").build()
                );
                case "HINGLISH" -> List.of(
                        OutboundRow.builder().id("get_business_summary").title("📊 Today's Summary & Revenue").description("Total bookings and revenue dekhein").build(),
                        OutboundRow.builder().id("get_today_bookings").title("📋 Today's Schedule").description("Aaj ki sabhi customer bookings dekhein").build(),
                        OutboundRow.builder().id("block_slot").title("🚫 Block Slot (Maintenance)").description("Slot block karein offline/maintenance ke liye").build(),
                        OutboundRow.builder().id("unblock_slot").title("🔓 Unblock Slot").description("Blocked slot ko unblock karein").build(),
                        OutboundRow.builder().id("excel_report").title("📥 Download Excel Report").description("Daily/monthly Excel report paayein").build()
                );
                case "MINGLISH" -> List.of(
                        OutboundRow.builder().id("get_business_summary").title("📊 Today's Summary & Revenue").description("Total bookings aani revenue paha").build(),
                        OutboundRow.builder().id("get_today_bookings").title("📋 Today's Schedule").description("Aajchya sarv customer bookings paha").build(),
                        OutboundRow.builder().id("block_slot").title("🚫 Block Slot (Maintenance)").description("Slot block kara offline/maintenance sathi").build(),
                        OutboundRow.builder().id("unblock_slot").title("🔓 Unblock Slot").description("Blocked slot unblock kara").build(),
                        OutboundRow.builder().id("excel_report").title("📥 Download Excel Report").description("Daily/monthly Excel report milwa").build()
                );
                default -> List.of(
                        OutboundRow.builder().id("get_business_summary").title("📊 Today's Summary & Revenue").description("View total bookings, confirmed count & revenue").build(),
                        OutboundRow.builder().id("get_today_bookings").title("📋 Today's Schedule").description("View all customer bookings for today").build(),
                        OutboundRow.builder().id("block_slot").title("🚫 Block Slot (Maintenance)").description("Block slot for maintenance or offline booking").build(),
                        OutboundRow.builder().id("unblock_slot").title("🔓 Unblock Slot").description("Unblock a previously blocked slot").build(),
                        OutboundRow.builder().id("excel_report").title("📥 Download Excel Report").description("Generate and receive daily/monthly Excel report").build()
                );
            };
        } else {
            rows = switch (language) {
                case "HI" -> List.of(
                        OutboundRow.builder().id("check_availability").title("📅 उपलब्धता जांचें").description("उपलब्ध स्लॉट देखें").build(),
                        OutboundRow.builder().id("pricing").title("💰 मूल्य सूची").description("बुकिंग दरें देखें").build(),
                        OutboundRow.builder().id("location_map").title("📍 स्थान और मानचित्र").description("हमारा पता और लोकेशन प्राप्त करें").build(),
                        OutboundRow.builder().id("view_booking").title("📖 मेरी बुकिंग देखें").description("अपनी मौजूदा बुकिंग का विवरण देखें").build(),
                        OutboundRow.builder().id("cancel_booking").title("❌ बुकिंग रद्द करें").description("अपनी मौजूदा बुकिंग रद्द करें").build()
                );
                case "MR" -> List.of(
                        OutboundRow.builder().id("check_availability").title("📅 उपलब्धता तपासा").description("उपलब्ध स्लॉट तपासा").build(),
                        OutboundRow.builder().id("pricing").title("💰 दरपत्रक").description("बुकिंगचे दर पहा").build(),
                        OutboundRow.builder().id("location_map").title("📍 ठिकाण आणि नकाशा").description("आमचे लोकेशन मिळवा").build(),
                        OutboundRow.builder().id("view_booking").title("📖 माझी बुकिंग पहा").description("तुमच्या बुकिंगचा तपशील पहा").build(),
                        OutboundRow.builder().id("cancel_booking").title("❌ बुकिंग रद्द करा").description("तुमची बुकिंग रद्द करा").build()
                );
                case "HINGLISH" -> List.of(
                        OutboundRow.builder().id("check_availability").title("📅 Check Availability").description("Available slots check karein").build(),
                        OutboundRow.builder().id("pricing").title("💰 Pricing").description("Booking rates dekhein").build(),
                        OutboundRow.builder().id("location_map").title("📍 Location & Map").description("Humari location paayein").build(),
                        OutboundRow.builder().id("view_booking").title("📖 View My Booking").description("Apni booking detail dekhein").build(),
                        OutboundRow.builder().id("cancel_booking").title("❌ Cancel Booking").description("Existing booking cancel karein").build()
                );
                case "MINGLISH" -> List.of(
                        OutboundRow.builder().id("check_availability").title("📅 Check Availability").description("Available slots check kara").build(),
                        OutboundRow.builder().id("pricing").title("💰 Pricing").description("Booking rates paha").build(),
                        OutboundRow.builder().id("location_map").title("📍 Location & Map").description("Amche location milwa").build(),
                        OutboundRow.builder().id("view_booking").title("📖 View My Booking").description("Tumchi booking details paha").build(),
                        OutboundRow.builder().id("cancel_booking").title("❌ Cancel Booking").description("Existing booking cancel kara").build()
                );
                default -> List.of(
                        OutboundRow.builder().id("check_availability").title("📅 Check Availability").description("Check available slots for today & upcoming days").build(),
                        OutboundRow.builder().id("pricing").title("💰 Pricing").description("View slot prices & dynamic hourly rates").build(),
                        OutboundRow.builder().id("location_map").title("📍 Location & Directions").description("Get turf address & Google Maps navigation").build(),
                        OutboundRow.builder().id("view_booking").title("📖 View My Bookings").description("Look up your active & past bookings").build(),
                        OutboundRow.builder().id("cancel_booking").title("❌ Cancel Booking").description("Cancel an existing active booking").build()
                );
            };
        }

        OutboundSection section = OutboundSection.builder()
                .title(secTitle)
                .rows(rows)
                .build();

        return new MenuConfig(header, body, buttonText, List.of(section));
    }

    public String formatBookingDetails(String lang, List<Map<String, Object>> bookings) {
        String language = normalizeLang(lang);
        if (bookings == null || bookings.isEmpty()) {
            return formatNoBookingFound(language);
        }

        StringBuilder sb = new StringBuilder();
        switch (language) {
            case "HI" -> sb.append("📖 *आपकी बुकिंग का विवरण:*\n\n");
            case "MR" -> sb.append("📖 *तुमच्या बुकिंगचा तपशील:*\n\n");
            case "HINGLISH" -> sb.append("📖 *Aapki Booking Details:*\n\n");
            case "MINGLISH" -> sb.append("📖 *Tumchi Booking Details:*\n\n");
            default -> sb.append("📖 *Your Booking Details:*\n\n");
        }

        for (Map<String, Object> b : bookings) {
            sb.append("• *Booking ID:* ").append(b.get("booking_id")).append("\n");
            sb.append("• *Turf Name:* ").append(b.get("turf_name")).append("\n");
            sb.append("• *Date:* ").append(b.get("date")).append("\n");
            sb.append("• *Time Slot:* ").append(b.get("time_slot")).append("\n");
            sb.append("• *Booking Status:* ").append(b.get("status")).append("\n");
            sb.append("• *Amount Paid:* ₹").append(b.get("amount_paid")).append("\n\n");
        }

        return sb.toString().trim();
    }

    public String formatNoBookingFound(String lang) {
        return switch (normalizeLang(lang)) {
            case "HI" -> "ℹ️ आपके पंजीकृत नंबर पर कोई बुकिंग नहीं मिली।\n\nयदि आपकी बुकिंग किसी अन्य नंबर से की गई है, तो कृपया खोज करने के लिए अपना पंजीकृत 10-अंकीय मोबाइल नंबर भेजें।";
            case "MR" -> "ℹ️ तुमच्या नोंदणीकृत नंबरवर कोणतीही बुकिंग आढळली नाही.\n\nतुमची बुकिंग दुसऱ्या नंबरवरून असल्यास, शोधण्यासाठी कृपया तुमचा १० अंकी नोंदणीकृत मोबाईल नंबर पाठवा.";
            case "HINGLISH" -> "ℹ️ Aapke registered number par koi booking nahi mili.\n\nAgar aapki booking kisi doosre number se hai, toh kripya search karne ke liye apna 10-digit mobile number bheinjiye.";
            case "MINGLISH" -> "ℹ️ Tumchya registered number var kontihi booking nahi mili.\n\nJar tumchi booking dusrya number varun asel, tar search karnyasathi tumcha 10-digit mobile number pathva.";
            default -> "ℹ️ No bookings found for your registered account.\n\nIf your booking was created under a different number, please reply with your registered 10-digit mobile number to search.";
        };
    }

    public String formatLocationSummary(String lang, String name, String address) {
        return switch (normalizeLang(lang)) {
            case "HI" -> String.format("📍 *%s*\nपता: %s\nगूगल मैप्स: https://maps.google.com/?q=Rankala+Kolhapur", name, address);
            case "MR" -> String.format("📍 *%s*\nपत्ता: %s\nगूगल मॅप्स: https://maps.google.com/?q=Rankala+Kolhapur", name, address);
            case "HINGLISH" -> String.format("📍 *%s*\nAddress: %s\nGoogle Maps: https://maps.google.com/?q=Rankala+Kolhapur", name, address);
            case "MINGLISH" -> String.format("📍 *%s*\nAddress: %s\nGoogle Maps: https://maps.google.com/?q=Rankala+Kolhapur", name, address);
            default -> String.format("📍 *%s*\nAddress: %s\nGoogle Maps: https://maps.google.com/?q=Rankala+Kolhapur", name, address);
        };
    }

    public String formatPricing(String lang) {
        return switch (normalizeLang(lang)) {
            case "HI" -> "⚽ *Green Pitch Kolhapur मूल्य सूची (Pricing):*\n\n• सामान्य घंटे (6 AM - 5 PM): ₹800/घंटा\n• पीक घंटे (5 PM - 11 PM): ₹1,000/घंटा\n\nउपलब्ध स्लॉट की जांच के लिए अपनी पसंदीदा तारीख और समय भेजें!";
            case "MR" -> "⚽ *Green Pitch Kolhapur दरपत्रक (Pricing):*\n\n• नियमित वेळ (6 AM - 5 PM): ₹800/तास\n• पीक वेळ (5 PM - 11 PM): ₹1,000/तास\n\nउपलब्ध स्लॉट तपासण्यासाठी तुमची आवडती तारीख आणि वेळ पाठवा!";
            case "HINGLISH" -> "⚽ *Green Pitch Kolhapur Rates:*\n\n• Regular Hours (6 AM - 5 PM): ₹800/hr\n• Peak Hours (5 PM - 11 PM): ₹1,000/hr\n\nAvailable slots check karne ke liye apni date aur time bheinjiye!";
            case "MINGLISH" -> "⚽ *Green Pitch Kolhapur Rates:*\n\n• Regular Hours (6 AM - 5 PM): ₹800/hr\n• Peak Hours (5 PM - 11 PM): ₹1,000/hr\n\nAvailable slots check karnyasathi tumchi date aani time pathva!";
            default -> "⚽ *Green Pitch Kolhapur Pricing:*\n\n• Standard Hours (6 AM - 5 PM): ₹800/hr\n• Peak Hours (5 PM - 11 PM): ₹1,000/hr\n\nReply with your preferred date & time to check available slots!";
        };
    }

    public String formatAvailability(String lang) {
        return switch (normalizeLang(lang)) {
            case "HI" -> "📅 *कल के लिए उपलब्ध स्लॉट:*\n• 06:00 PM - 07:00 PM (₹800)\n• 07:00 PM - 08:00 PM (₹1,000 पीक)\n\n10-मिनट की बुकिंग होल्ड के लिए अपना पसंदीदा स्लॉट भेजें (जैसे 'Book 6 to 7')!";
            case "MR" -> "📅 *उद्यासाठी उपलब्ध स्लॉट:*\n• 06:00 PM - 07:00 PM (₹800)\n• 07:00 PM - 08:00 PM (₹1,000 पीक)\n\n१० मिनिटांची होल्ड बुकिंग करण्यासाठी तुमचा स्लॉट पाठवा (उदा. 'Book 6 to 7')!";
            case "HINGLISH" -> "📅 *Kal ke liye Available Slots:*\n• 06:00 PM - 07:00 PM (₹800)\n• 07:00 PM - 08:00 PM (₹1,000 PEAK)\n\n10-minute booking hold ke liye apna preferred slot bheinjiye (e.g. 'Book 6 to 7')!";
            case "MINGLISH" -> "📅 *Udya sathi Available Slots:*\n• 06:00 PM - 07:00 PM (₹800)\n• 07:00 PM - 08:00 PM (₹1,000 PEAK)\n\n10-minute booking hold sathi tumcha slot pathva (e.g. 'Book 6 to 7')!";
            default -> "📅 *Available Slots for Tomorrow:*\n• 06:00 PM - 07:00 PM (₹800)\n• 07:00 PM - 08:00 PM (₹1,000 PEAK)\n\nReply with your preferred slot (e.g., 'Book 6 to 7') to place a 10-minute hold!";
        };
    }

    public String formatCancellation(String lang) {
        return switch (normalizeLang(lang)) {
            case "HI" -> "❌ आपकी बुकिंग सफलतापूर्वक रद्द कर दी गई है। यदि लागू हो, तो रिफंड नीति के अनुसार 2-3 कार्य दिवसों में प्रोसेस किया जाएगा।";
            case "MR" -> "❌ तुमची बुकिंग यशस्वीरित्या रद्द करण्यात आली आहे. लागू असल्यास, परतावा २-३ कामकाजाच्या दिवसांत जमा केला जाईल.";
            case "HINGLISH" -> "❌ Aapki booking successfully cancel ho gayi hai. Refund policy ke according 2-3 business days mein process hoga.";
            case "MINGLISH" -> "❌ Tumchi booking successfully cancel zali aahe. Refund policy nusar 2-3 business days madhe process hoiil.";
            default -> "❌ Your booking has been successfully cancelled. Refund, if applicable, will be processed according to our policy within 2-3 business days.";
        };
    }

    public String formatHoldCreated(String lang, String bookingRef, String paymentUrl, Object price) {
        return switch (normalizeLang(lang)) {
            case "HI" -> String.format(
                    "⏳ *बुकिंग होल्ड बन गया!*\n\n• *बुकिंग संदर्भ:* %s\n• *स्लॉट:* कल 06:00 PM - 07:00 PM\n• *देय राशि:* ₹%s\n• *होल्ड अवधि:* 7.5 मिनट\n\n💳 *भुगतान और पुष्टि के लिए लिंक पर क्लिक करें:* \n%s\n\n*(स्लॉट लॉक करने के लिए 5 मिनट के भीतर UPI / कार्ड द्वारा भुगतान करें!)*",
                    bookingRef, price, paymentUrl);
            case "MR" -> String.format(
                    "⏳ *बुकिंग होल्ड तयार झाले!*\n\n• *बुकिंग संदर्भ:* %s\n• *स्लॉट:* उद्या 06:00 PM - 07:00 PM\n• *देय रक्कम:* ₹%s\n• *होल्ड वेळ:* 7.5 मिनिटे\n\n💳 *पेमेंट आणि कन्फर्मेशनसाठी लिंकवर क्लिक करा:* \n%s\n\n*(५ मिनिटांत UPI / कार्डद्वारे पेमेंट पूर्ण करा!)*",
                    bookingRef, price, paymentUrl);
            case "HINGLISH" -> String.format(
                    "⏳ *Booking Hold Created!*\n\n• *Booking Ref:* %s\n• *Slot:* Kal 06:00 PM - 07:00 PM\n• *Payable Amount:* ₹%s\n• *Hold Duration:* 7.5 Minutes\n\n💳 *Payment & Confirm karne ke liye click karein:* \n%s\n\n*(5 mins ke andar payment complete karein!)*",
                    bookingRef, price, paymentUrl);
            case "MINGLISH" -> String.format(
                    "⏳ *Booking Hold Created!*\n\n• *Booking Ref:* %s\n• *Slot:* Udya 06:00 PM - 07:00 PM\n• *Payable Amount:* ₹%s\n• *Hold Duration:* 7.5 Minutes\n\n💳 *Payment & Confirm karnyasathi click kara:* \n%s\n\n*(5 mins madhe payment complete kara!)*",
                    bookingRef, price, paymentUrl);
            default -> String.format(
                    "⏳ *Booking Hold Created!*\n\n• *Booking Ref:* %s\n• *Slot:* Tomorrow 06:00 PM - 07:00 PM\n• *Amount Payable:* ₹%s\n• *Hold Duration:* 7.5 Minutes\n\n💳 *Click Link to Pay & Confirm:* \n%s\n\n*(Complete payment within 5 mins via UPI / Card / NetBanking to lock your slot!)*",
                    bookingRef, price, paymentUrl);
        };
    }

    private String normalizeLang(String lang) {
        if (lang == null) return "EN";
        String u = lang.toUpperCase();
        if (u.equals("HI") || u.equals("MR") || u.equals("HINGLISH") || u.equals("MINGLISH")) {
            return u;
        }
        return "EN";
    }
}
