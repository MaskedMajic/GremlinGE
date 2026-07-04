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

            // EMPTY -> non-empty = placed
            if (previous.state == OfferState.EMPTY && current.state != OfferState.EMPTY) {
                events.add(makeEvent(OfferEventType.PLACED, current, current.filledQuantity));
            }

            // Fill progress increased
            if (current.filledQuantity > previous.filledQuantity) {
                int delta = current.filledQuantity - previous.filledQuantity;

                if (current.state == OfferState.COMPLETE) {
                    events.add(makeEvent(OfferEventType.COMPLETED, current, delta));
                } else {
                    events.add(makeEvent(OfferEventType.PARTIAL_FILL, current, delta));
                }
            }

            // Cancelled state transition
            if (previous.state != OfferState.CANCELLED && current.state == OfferState.CANCELLED) {
                events.add(makeEvent(OfferEventType.CANCELLED, current, 0));
            }

            // Cleared slot transition
            if (previous.state != OfferState.EMPTY && current.state == OfferState.EMPTY) {
                events.add(makeEvent(OfferEventType.CLEARED, previous, 0));
            }

            // Generic material update (price/qty/state change) if not already covered cleanly
            if (isMateriallyDifferent(previous, current)
                && !alreadyRepresented(events, current.slotIndex, OfferEventType.PLACED)
                && !alreadyRepresented(events, current.slotIndex, OfferEventType.PARTIAL_FILL)
                && !alreadyRepresented(events, current.slotIndex, OfferEventType.COMPLETED)
                && !alreadyRepresented(events, current.slotIndex, OfferEventType.CANCELLED)
                && !alreadyRepresented(events, current.slotIndex, OfferEventType.CLEARED)) {
                events.add(makeEvent(OfferEventType.UPDATED, current, 0));
            }
        }

        previousSnapshot = currentSnapshot;
        return events;
    }

    private GeOfferEvent makeEvent(OfferEventType type, GeOfferState state, int deltaFilled) {
        GeOfferEvent event = new GeOfferEvent();
        event.type = type;
        event.slotIndex = state.slotIndex;
        event.itemId = state.itemId;
        event.itemName = state.itemName;
        event.offerType = state.offerType;
        event.deltaFilled = deltaFilled;
        event.newFilledQuantity = state.filledQuantity;
        event.timestampEpochSeconds = System.currentTimeMillis() / 1000L;
        return event;
    }

    private boolean isMateriallyDifferent(GeOfferState previous, GeOfferState current) {
        if (previous == null || current == null) {
            return false;
        }

        return previous.itemId != current.itemId
            || previous.offerType != current.offerType
            || previous.price != current.price
            || previous.totalQuantity != current.totalQuantity
            || previous.filledQuantity != current.filledQuantity
            || previous.state != current.state;
    }

    private boolean alreadyRepresented(List<GeOfferEvent> events, int slotIndex, OfferEventType type) {
        for (GeOfferEvent event : events) {
            if (event.slotIndex == slotIndex && event.type == type) {
                return true;
            }
        }
        return false;
    }
}
