package com.maskedmajic.gremlinge.ge;

import java.util.ArrayList;
import java.util.List;

/**
 * Compares snapshots and emits meaningful GE offer events.
 */
public class GeStateTracker {
    private GeOfferSnapshot previousSnapshot;

    public List<GeOfferEvent> update(GeOfferSnapshot currentSnapshot) {
        List<GeOfferEvent> events = new ArrayList<GeOfferEvent>();

        if (previousSnapshot == null) {
            previousSnapshot = currentSnapshot;
            return events;
        }

        for (GeOfferState current : currentSnapshot.slots) {
            GeOfferState previous = previousSnapshot.getSlot(current.slotIndex);
            if (previous == null) {
                continue;
            }

            // Planned comparisons later:
            // 1. EMPTY -> ACTIVE  => PLACED
            // 2. filled qty increased but not done => PARTIAL_FILL
            // 3. not complete -> complete => COMPLETED
            // 4. active/partial -> cancelled => CANCELLED
            // 5. non-empty -> empty => CLEARED
            // 6. other material changes => UPDATED
        }

        previousSnapshot = currentSnapshot;
        return events;
    }
}
