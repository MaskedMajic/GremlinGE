package com.maskedmajic.gremlinge.ge;

public class PurchaseRecord {
    public String itemName;
    public int quantity;
    public long timestampEpochSeconds;

    public PurchaseRecord() {}

    public PurchaseRecord(String itemName, int quantity, long timestampEpochSeconds) {
        this.itemName = itemName;
        this.quantity = quantity;
        this.timestampEpochSeconds = timestampEpochSeconds;
    }
}
