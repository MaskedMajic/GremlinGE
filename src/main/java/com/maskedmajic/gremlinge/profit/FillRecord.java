package com.maskedmajic.gremlinge.profit;

public class FillRecord {
    public String itemName;
    public int itemId;
    public TradeSide side;
    public int quantity;
    public int price;
    public long timestampEpochSeconds;

    public FillRecord() {}

    public FillRecord(String itemName, int itemId, TradeSide side, int quantity, int price, long timestampEpochSeconds) {
        this.itemName = itemName;
        this.itemId = itemId;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.timestampEpochSeconds = timestampEpochSeconds;
    }
}
