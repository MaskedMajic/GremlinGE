package com.maskedmajic.gremlinge.ge;

import java.util.ArrayList;
import java.util.List;

public class GeOfferSnapshot {
    public long capturedAtEpochSeconds;
    public List<GeOfferState> slots = new ArrayList<GeOfferState>();

    public GeOfferState getSlot(int slotIndex) {
        for (GeOfferState slot : slots) {
            if (slot.slotIndex == slotIndex) {
                return slot;
            }
        }
        return null;
    }
}
