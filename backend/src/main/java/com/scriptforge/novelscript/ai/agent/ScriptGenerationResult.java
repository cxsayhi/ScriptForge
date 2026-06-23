package com.scriptforge.novelscript.ai.agent;

import com.scriptforge.novelscript.entity.FailedEpisode;

import java.util.List;

/**
 * Agent generation outcome. A review-required result preserves the best available
 * structured draft and the raw LLM output instead of replacing it with a synthetic fallback.
 */
public record ScriptGenerationResult(
        String yaml,
        String rawLlmResponse,
        String status,
        String message,
        List<FailedEpisode> failedEpisodes
) {

    public static final String COMPLETED = "completed";
    public static final String NEEDS_REVIEW = "needs_review";

    public static ScriptGenerationResult completed(String yaml) {
        return new ScriptGenerationResult(yaml, "", COMPLETED, "剧本初稿已生成", List.of());
    }

    public static ScriptGenerationResult needsReview(String yaml, String rawLlmResponse, String message) {
        return needsReview(yaml, rawLlmResponse, message, List.of());
    }

    public static ScriptGenerationResult needsReview(String yaml,
                                                     String rawLlmResponse,
                                                     String message,
                                                     List<FailedEpisode> failedEpisodes) {
        return new ScriptGenerationResult(
                yaml == null ? "" : yaml,
                rawLlmResponse == null ? "" : rawLlmResponse,
                NEEDS_REVIEW,
                message == null || message.isBlank() ? "LLM 输出需要人工审核" : message,
                failedEpisodes == null ? List.of() : List.copyOf(failedEpisodes)
        );
    }

    public boolean requiresReview() {
        return NEEDS_REVIEW.equals(status);
    }
}
