package com.maskedmajic.gremlinge.ge;

public class LimitStatus {
    public final int itemId;
    public final String itemName;
    public final int buyLimit;
    public final int boughtInWindow;
    public final int remaining;
    public final String resetEta;

    public LimitStatus(int itemId, String itemName, int buyLimit, int boughtInWindow, int remaining, String resetEta) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.buyLimit = buyLimit;
        this.boughtInWindow = boughtInWindow;
        this.remaining = remaining;
        this.resetEta = resetEta;
    }
}
