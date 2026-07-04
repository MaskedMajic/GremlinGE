package com.maskedmajic.gremlinge;

public class FlipRecommendation {
    public final FlipCandidate candidate;
    public final int remainingLimit;
    public final boolean alreadyActive;
    public final int recommendationScore;

    public FlipRecommendation(FlipCandidate candidate, int remainingLimit, boolean alreadyActive, int recommendationScore) {
        this.candidate = candidate;
        this.remainingLimit = remainingLimit;
        this.alreadyActive = alreadyActive;
        this.recommendationScore = recommendationScore;
    }
}
