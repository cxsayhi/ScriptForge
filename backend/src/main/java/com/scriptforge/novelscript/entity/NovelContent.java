package com.scriptforge.novelscript.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class NovelContent {

    private String originalText = "";
    private List<Chapter> chapters = new ArrayList<>();
    private Instant updatedAt = Instant.now();

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
        this.updatedAt = Instant.now();
    }

    public List<Chapter> getChapters() {
        return chapters;
    }

    public void setChapters(List<Chapter> chapters) {
        this.chapters = chapters;
        this.updatedAt = Instant.now();
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
