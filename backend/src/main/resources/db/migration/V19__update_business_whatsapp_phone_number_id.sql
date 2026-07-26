-- Ensure active business has the correct WhatsApp Phone Number ID
UPDATE business 
SET whatsapp_phone_number_id = '1284997344689548' 
WHERE status = 'ACTIVE';
