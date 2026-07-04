package com.maskedmajic.gremlinge;

public class FlipCandidate {
    public final String name;
    public final int buy;
    public final int sell;
    public final int margin;
    public final String volumeTag;
    public final int buyLimit;
    public final int volume5m;
    public final int score;

    public FlipCandidate(String name, int buy, int sell, int margin, String volumeTag, int buyLimit, int volume5m) {
        this.name = name;
        this.buy = buy;
        this.sell = sell;
        this.margin = margin;
        this.volumeTag = volumeTag;
        this.buyLimit = buyLimit;
        this.volume5m = volume5m;
        this.score = computeScore();
    }

    private int computeScore() {
        int volumeBonus = 1;
        if ("high".equals(volumeTag)) {
            volumeBonus = 3;
        } else if ("med".equals(volumeTag)) {
            volumeBonus = 2;
        }

        int limitBonus = Math.max(1, buyLimit / 1000);
        return margin * volumeBonus + Math.min(volume5m / 1000, 200) + limitBonus;
    }
}
