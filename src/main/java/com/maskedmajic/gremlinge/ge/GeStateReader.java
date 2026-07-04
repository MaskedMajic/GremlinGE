package com.maskedmajic.gremlinge.ge;

/**
 * RuneLite-facing GE state reader skeleton.
 *
 * This remains dependency-free for now so the project can compile without
 * RuneLite jars present. The method signatures and comments reflect the exact
 * binding plan for when we add RuneLite API dependencies.
 */
public class GeStateReader {
    /**
     * Future RuneLite binding entrypoint.
     *
     * Planned signature later:
     *   readCurrentSnapshot(Client client, ItemManager itemManager)
     *
     * It will:
     * - call client.getGrandExchangeOffers()
     * - iterate all 8 slots
     * - map each RuneLite offer into GeOfferState
     * - return a GeOfferSnapshot
     */
    public GeOfferSnapshot readCurrentSnapshot() {
        GeOfferSnapshot snapshot = new GeOfferSnapshot();
        snapshot.capturedAtEpochSeconds = System.currentTimeMillis() / 1000L;

        // TODO RuneLite binding plan:
        // GrandExchangeOffer[] offers = client.getGrandExchangeOffers();
        // for (int slotIndex = 0; slotIndex < offers.length; slotIndex++) {
        //     GrandExchangeOffer offer = offers[slotIndex];
        //     GeOfferState state = mapOffer(slotIndex, offer, itemManager);
        //     snapshot.slots.add(state);
        // }

        return snapshot;
    }

    /**
     * Planned mapping method shape once RuneLite APIs are wired in.
     *
     * Planned signature later:
     *   private GeOfferState mapOffer(int slotIndex, GrandExchangeOffer offer, ItemManager itemManager)
     *
     * Mapping plan:
     * - slot index -> state.slotIndex
     * - offer item id -> state.itemId
     * - item manager lookup -> state.itemName
     * - offer price -> state.price
     * - offer total quantity -> state.totalQuantity
     * - offer quantity sold/bought -> state.filledQuantity
     * - RuneLite buy/sell flag -> state.offerType
     * - RuneLite offer state -> state.state
     * - first seen / updated timestamps assigned locally
     */
    public GeOfferState mapPlaceholder(int slotIndex) {
        GeOfferState state = new GeOfferState();
        state.slotIndex = slotIndex;
        state.firstSeenAtEpochSeconds = System.currentTimeMillis() / 1000L;
        state.lastUpdatedAtEpochSeconds = state.firstSeenAtEpochSeconds;
        return state;
    }

    /**
     * Planned state normalization rules.
     *
     * RuneLite state -> GremlinGE state:
     * - empty -> EMPTY
     * - active/in progress -> ACTIVE
     * - partial progress -> PARTIAL
     * - completed -> COMPLETE
     * - cancelled/aborted -> CANCELLED
     */
    public OfferState normalizePlaceholderState() {
        return OfferState.UNKNOWN;
    }

    /**
     * Planned offer-type mapping rules.
     *
     * RuneLite offer -> BUY / SELL / UNKNOWN.
     */
    public OfferType normalizePlaceholderType() {
        return OfferType.UNKNOWN;
    }
}
