package com.maskedmajic.gremlinge.ge;

public class GeOfferState {
    public int slotIndex;
    public int itemId;
    public String itemName;
    public OfferType offerType;
    public int price;
    public int totalQuantity;
    public int filledQuantity;
    public int buyLimit;
    public OfferState state;
    public long firstSeenAtEpochSeconds;
    public long lastUpdatedAtEpochSeconds;

    public GeOfferState() {
        this.offerType = OfferType.UNKNOWN;
        this.state = OfferState.UNKNOWN;
    }

    public boolean isEmpty() {
        return state == OfferState.EMPTY;
    }
}
