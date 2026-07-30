-- Ensure active business has the correct Meta WhatsApp Phone Number ID matching .env
UPDATE business 
SET whatsapp_phone_number_id = '1174774225727644' 
WHERE status = 'ACTIVE';
