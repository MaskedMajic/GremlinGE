package com.maskedmajic.gremlinge.ge;

import net.runelite.api.Client;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemManager;

/**
 * RuneLite-facing GE state reader.
 *
 * Reads the client's current GE slot state and normalizes it into GremlinGE's
 * internal snapshot model.
 */
public class GeStateReader {
    public GeOfferSnapshot readCurrentSnapshot(Client client, ItemManager itemManager) {
        GeOfferSnapshot snapshot = new GeOfferSnapshot();
        snapshot.capturedAtEpochSeconds = System.currentTimeMillis() / 1000L;

        if (client == null) {
            return snapshot;
        }

        GrandExchangeOffer[] offers = client.getGrandExchangeOffers();
        if (offers == null) {
            return snapshot;
        }

        for (int slotIndex = 0; slotIndex < offers.length; slotIndex++) {
            GrandExchangeOffer offer = offers[slotIndex];
            GeOfferState state = mapOffer(slotIndex, offer, itemManager);
            snapshot.slots.add(state);
        }

        return snapshot;
    }

    private GeOfferState mapOffer(int slotIndex, GrandExchangeOffer offer, ItemManager itemManager) {
        GeOfferState state = new GeOfferState();
        state.slotIndex = slotIndex;
        state.firstSeenAtEpochSeconds = System.currentTimeMillis() / 1000L;
        state.lastUpdatedAtEpochSeconds = state.firstSeenAtEpochSeconds;

        if (offer == null) {
            state.state = OfferState.EMPTY;
            return state;
        }

        state.itemId = offer.getItemId();
        state.itemName = lookupItemName(offer.getItemId(), itemManager);
        state.offerType = normalizeType(offer.getState());
        state.price = offer.getPrice();
        state.totalQuantity = offer.getTotalQuantity();
        state.filledQuantity = offer.getQuantitySold();
        state.state = normalizeState(offer.getState(), offer.getTotalQuantity(), offer.getQuantitySold());

        return state;
    }

    private String lookupItemName(int itemId, ItemManager itemManager) {
        if (itemManager == null || itemId <= 0) {
            return "Unknown";
        }

        try {
            ItemComposition item = itemManager.getItemComposition(itemId);
            if (item != null && item.getName() != null) {
                return item.getName();
            }
        } catch (Exception ignored) {
            // Keep fallback below; we do not want item-name lookup failures to kill state reading.
        }

        return "Item " + itemId;
    }

    private OfferState normalizeState(GrandExchangeOfferState rlState, int total, int filled) {
        if (rlState == null) {
            return OfferState.UNKNOWN;
        }

        switch (rlState) {
            case EMPTY:
                return OfferState.EMPTY;
            case CANCELLED_BUY:
            case CANCELLED_SELL:
                return OfferState.CANCELLED;
            case BOUGHT:
            case SOLD:
                return OfferState.COMPLETE;
            case BUYING:
            case SELLING:
                if (filled > 0 && filled < total) {
                    return OfferState.PARTIAL;
                }
                return OfferState.ACTIVE;
            default:
                return OfferState.UNKNOWN;
        }
    }

    private OfferType normalizeType(GrandExchangeOfferState rlState) {
        if (rlState == null) {
            return OfferType.UNKNOWN;
        }

        switch (rlState) {
            case BUYING:
            case BOUGHT:
            case CANCELLED_BUY:
                return OfferType.BUY;
            case SELLING:
            case SOLD:
            case CANCELLED_SELL:
                return OfferType.SELL;
            default:
                return OfferType.UNKNOWN;
        }
    }
}
