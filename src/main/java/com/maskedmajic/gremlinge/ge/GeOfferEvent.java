package com.maskedmajic.gremlinge.ge;

public class GeOfferEvent {
    public OfferEventType type;
    public int slotIndex;
    public int itemId;
    public String itemName;
    public OfferType offerType;
    public int price;
    public int deltaFilled;
    public int newFilledQuantity;
    public long timestampEpochSeconds;

    public GeOfferEvent() {
        this.offerType = OfferType.UNKNOWN;
    }
}
