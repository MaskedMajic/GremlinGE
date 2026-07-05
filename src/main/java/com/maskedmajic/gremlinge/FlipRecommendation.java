package com.maskedmajic.gremlinge;

public class FlipRecommendation {
    public final FlipCandidate candidate;
    public final int remainingLimit;
    public final boolean alreadyActive;
    public final int recommendationScore;
    public final String priceBand;

    public FlipRecommendation(FlipCandidate candidate, int remainingLimit, boolean alreadyActive, int recommendationScore, String priceBand) {
        this.candidate = candidate;
        this.remainingLimit = remainingLimit;
        this.alreadyActive = alreadyActive;
        this.recommendationScore = recommendationScore;
        this.priceBand = priceBand;
    }
}
