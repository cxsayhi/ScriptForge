ALTER TABLE script_result
    ADD COLUMN raw_llm_response LONGTEXT NULL AFTER validation_status,
    ADD COLUMN generation_status VARCHAR(40) NOT NULL DEFAULT 'completed' AFTER raw_llm_response,
    ADD COLUMN generation_message VARCHAR(1000) NULL AFTER generation_status;
