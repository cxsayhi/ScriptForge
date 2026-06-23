ALTER TABLE script_result
    ADD COLUMN failed_episodes_json LONGTEXT NULL AFTER generation_message;
