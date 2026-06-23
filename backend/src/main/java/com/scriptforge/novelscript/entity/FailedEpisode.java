package com.scriptforge.novelscript.entity;

/**
 * A failed LLM episode kept outside the formal YAML script so users can review
 * and edit the original response without treating it as a valid episode.
 */
public record FailedEpisode(
        int episodeId,
        String status,
        String reason,
        String rawResponse
) {

    public static final String NEEDS_REVIEW = "needs_review";
    public static final String EDITED = "edited";

    public static FailedEpisode needsReview(int episodeId, String reason, String rawResponse) {
        return new FailedEpisode(
                episodeId,
                NEEDS_REVIEW,
                reason == null ? "LLM 生成失败" : reason,
                rawResponse == null ? "" : rawResponse
        );
    }

    public FailedEpisode withEditedRawResponse(String rawResponse) {
        return new FailedEpisode(episodeId, EDITED, reason, rawResponse == null ? "" : rawResponse);
    }
}
