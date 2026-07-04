package com.maskedmajic.gremlinge.ge;

import java.util.ArrayList;
import java.util.List;

/**
 * Placeholder persistence layer for offer snapshots / events.
 *
 * For now we keep this minimal and local-first.
 */
public class OfferRepository {
    public void saveSnapshot(GeOfferSnapshot snapshot) {
        // TODO: persist snapshot locally if useful.
    }

    public void saveEvents(List<GeOfferEvent> events) {
        // TODO: persist event stream locally if useful.
    }

    public List<GeOfferEvent> loadEvents() {
        return new ArrayList<GeOfferEvent>();
    }
}
