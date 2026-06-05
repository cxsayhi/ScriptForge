CREATE DATABASE IF NOT EXISTS novel_script 
DEFAULT CHARACTER SET utf8mb4 
DEFAULT COLLATE utf8mb4_unicode_ci;
USE novel_script;
CREATE TABLE project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(80) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(40) NOT NULL DEFAULT 'draft',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE novel_content (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    original_text LONGTEXT NOT NULL,
    chapters_json LONGTEXT NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_novel_project FOREIGN KEY (project_id) REFERENCES project(id)
);

CREATE TABLE adaptation_setting (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    script_type VARCHAR(40) NOT NULL,
    target_episodes INT NOT NULL,
    episode_duration_minutes INT NOT NULL,
    style VARCHAR(80) NOT NULL,
    language VARCHAR(20) NOT NULL,
    adaptation_intensity VARCHAR(40) NOT NULL,
    dialogue_style VARCHAR(40) NOT NULL,
    budget_preference VARCHAR(40) NOT NULL,
    keep_original_dialogues BOOLEAN NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_setting_project FOREIGN KEY (project_id) REFERENCES project(id)
);

CREATE TABLE script_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    yaml LONGTEXT NOT NULL,
    validation_status VARCHAR(40) NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_script_project FOREIGN KEY (project_id) REFERENCES project(id)
);
