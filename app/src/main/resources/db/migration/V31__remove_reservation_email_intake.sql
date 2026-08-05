DELETE FROM integration_sync_jobs
WHERE job_type IN ('RESERVATION_MAIL_SYNC', 'INBOUND_EMAIL_IMPORT');

DELETE FROM webhook_receipts
WHERE provider = 'inbound-email'
   OR job_id IN (
       SELECT id FROM operational_jobs WHERE job_type = 'INBOUND_EMAIL_WEBHOOK'
   );

DELETE FROM dead_letter_events
WHERE event_type = 'INBOUND_EMAIL_WEBHOOK'
   OR job_id IN (
       SELECT id FROM operational_jobs WHERE job_type = 'INBOUND_EMAIL_WEBHOOK'
   );

DELETE FROM operational_jobs
WHERE job_type = 'INBOUND_EMAIL_WEBHOOK';

DROP TABLE IF EXISTS inbound_email_aliases;
