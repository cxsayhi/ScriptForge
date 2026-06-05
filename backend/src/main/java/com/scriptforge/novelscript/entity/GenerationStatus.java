package com.scriptforge.novelscript.entity;

import java.time.Instant;

public class GenerationStatus {

    private String status = "idle";
    private String message = "尚未生成剧本";
    private Instant updatedAt = Instant.now();

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
        this.updatedAt = Instant.now();
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
