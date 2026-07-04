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

        if (currentSnapshot == null) {
            return events;
        }

        if (previousSnapshot == null) {
            previousSnapshot = copySnapshot(currentSnapshot);
            return events;
        }

        for (GeOfferState current : currentSnapshot.slots) {
            if (current == null) {
                continue;
            }

            GeOfferState previous = previousSnapshot.getSlot(current.slotIndex);
            if (previous == null) {
                previous = emptyStateForSlot(current.slotIndex);
            }

            if (isReplacement(previous, current)) {
                if (!previous.isEmpty()) {
                    events.add(makeEvent(OfferEventType.CLEARED, previous, 0));
                }
                if (!current.isEmpty()) {
                    events.add(makeEvent(OfferEventType.PLACED, current, current.filledQuantity));
                }
                previous = emptyStateForSlot(current.slotIndex);
            }

            if (previous.isEmpty() && !current.isEmpty()) {
                events.add(makeEvent(OfferEventType.PLACED, current, current.filledQuantity));
            }

            int delta = current.filledQuantity - previous.filledQuantity;
            if (!current.isEmpty() && delta > 0) {
                if (current.state == OfferState.COMPLETE) {
                    events.add(makeEvent(OfferEventType.COMPLETED, current, delta));
                } else {
                    events.add(makeEvent(OfferEventType.PARTIAL_FILL, current, delta));
                }
            }

            if (previous.state != OfferState.CANCELLED && current.state == OfferState.CANCELLED) {
                events.add(makeEvent(OfferEventType.CANCELLED, current, 0));
            }

            if (!previous.isEmpty() && current.isEmpty()) {
                events.add(makeEvent(OfferEventType.CLEARED, previous, 0));
            }

            if (shouldEmitUpdated(previous, current, events)) {
                events.add(makeEvent(OfferEventType.UPDATED, current, 0));
            }
        }

        previousSnapshot = copySnapshot(currentSnapshot);
        return events;
    }

    private boolean shouldEmitUpdated(GeOfferState previous, GeOfferState current, List<GeOfferEvent> events) {
        if (previous == null || current == null) {
            return false;
        }

        if (current.isEmpty()) {
            return false;
        }

        if (!isMateriallyDifferent(previous, current)) {
            return false;
        }

        return !hasTerminalEvent(events, current.slotIndex);
    }

    private boolean hasTerminalEvent(List<GeOfferEvent> events, int slotIndex) {
        for (GeOfferEvent event : events) {
            if (event.slotIndex != slotIndex) {
                continue;
            }

            if (event.type == OfferEventType.PLACED
                || event.type == OfferEventType.PARTIAL_FILL
                || event.type == OfferEventType.COMPLETED
                || event.type == OfferEventType.CANCELLED
                || event.type == OfferEventType.CLEARED) {
                return true;
            }
        }
        return false;
    }

    private boolean isReplacement(GeOfferState previous, GeOfferState current) {
        if (previous == null || current == null) {
            return false;
        }

        if (previous.isEmpty() || current.isEmpty()) {
            return false;
        }

        return previous.itemId != current.itemId
            || previous.offerType != current.offerType
            || current.filledQuantity < previous.filledQuantity;
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

    private GeOfferSnapshot copySnapshot(GeOfferSnapshot source) {
        GeOfferSnapshot copy = new GeOfferSnapshot();
        copy.capturedAtEpochSeconds = source.capturedAtEpochSeconds;

        for (GeOfferState state : source.slots) {
            if (state != null) {
                copy.slots.add(copyState(state));
            }
        }

        return copy;
    }

    private GeOfferState copyState(GeOfferState source) {
        GeOfferState copy = new GeOfferState();
        copy.slotIndex = source.slotIndex;
        copy.itemId = source.itemId;
        copy.itemName = source.itemName;
        copy.offerType = source.offerType;
        copy.price = source.price;
        copy.totalQuantity = source.totalQuantity;
        copy.filledQuantity = source.filledQuantity;
        copy.buyLimit = source.buyLimit;
        copy.state = source.state;
        copy.firstSeenAtEpochSeconds = source.firstSeenAtEpochSeconds;
        copy.lastUpdatedAtEpochSeconds = source.lastUpdatedAtEpochSeconds;
        return copy;
    }

    private GeOfferState emptyStateForSlot(int slotIndex) {
        GeOfferState state = new GeOfferState();
        state.slotIndex = slotIndex;
        state.state = OfferState.EMPTY;
        return state;
    }

    private GeOfferEvent makeEvent(OfferEventType type, GeOfferState state, int deltaFilled) {
        GeOfferEvent event = new GeOfferEvent();
        event.type = type;
        event.slotIndex = state.slotIndex;
        event.itemId = state.itemId;
        event.itemName = state.itemName;
        event.offerType = state.offerType;
        event.price = state.price;
        event.deltaFilled = deltaFilled;
        event.newFilledQuantity = state.filledQuantity;
        event.timestampEpochSeconds = System.currentTimeMillis() / 1000L;
        return event;
    }
}
