package com.maskedmajic.gremlinge.runelite;

import com.maskedmajic.gremlinge.ge.GeOfferSnapshot;
import com.maskedmajic.gremlinge.ge.GeStateReader;
import com.maskedmajic.gremlinge.ge.GeStateTracker;

/**
 * Placeholder RuneLite plugin skeleton.
 *
 * This is NOT wired to the full RuneLite plugin lifecycle yet. It exists to
 * define the internal plugin shape before we bind the real annotations,
 * event bus, panel registration, and injected services.
 */
public class GremlinGEPlugin {
    private final GeStateReader stateReader = new GeStateReader();
    private final GeStateTracker stateTracker = new GeStateTracker();

    public void startUp() {
        // TODO: hook RuneLite startup lifecycle.
    }

    public void shutDown() {
        // TODO: hook RuneLite shutdown lifecycle.
    }

    public void onPoll(Object client, Object itemManager) {
        // TODO: replace Object types with RuneLite Client + ItemManager once the
        // plugin is bound to the actual RuneLite dependency injection lifecycle.
        GeOfferSnapshot snapshot = new GeOfferSnapshot();
        stateTracker.update(snapshot);
    }
}
