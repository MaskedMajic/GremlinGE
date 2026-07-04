package com.maskedmajic.gremlinge.runelite;

import com.maskedmajic.gremlinge.ge.GeOfferEvent;
import com.maskedmajic.gremlinge.ge.GeOfferSnapshot;
import com.maskedmajic.gremlinge.ge.GeStateReader;
import com.maskedmajic.gremlinge.ge.GeStateTracker;
import net.runelite.api.Client;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.client.game.ItemManager;

import java.util.List;

/**
 * RuneLite plugin skeleton with the real event/data flow documented in code.
 *
 * Still not a full RuneLite Plugin subclass yet, but this now reflects the
 * actual method shapes we want once we wire annotations + DI + panel hookup.
 */
public class GremlinGEPlugin {
    private final GeStateReader stateReader = new GeStateReader();
    private final GeStateTracker stateTracker = new GeStateTracker();

    private Client client;
    private ItemManager itemManager;

    public void startUp(Client client, ItemManager itemManager) {
        this.client = client;
        this.itemManager = itemManager;

        // Initial truth snapshot on startup/login/plugin enable.
        GeOfferSnapshot snapshot = stateReader.readCurrentSnapshot(client, itemManager);
        stateTracker.update(snapshot);
    }

    public void shutDown() {
        this.client = null;
        this.itemManager = null;
    }

    /**
     * Planned RuneLite event hook shape.
     *
     * Real plugin version later will subscribe with RuneLite's event bus:
     *   @Subscribe
     *   public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged event)
     */
    public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged event) {
        if (client == null || itemManager == null || event == null) {
            return;
        }

        GeOfferSnapshot snapshot = stateReader.readCurrentSnapshot(client, itemManager);
        List<GeOfferEvent> events = stateTracker.update(snapshot);

        for (GeOfferEvent geEvent : events) {
            handleGeEvent(geEvent);
        }
    }

    private void handleGeEvent(GeOfferEvent geEvent) {
        // TODO next steps:
        // - feed buy-fill events into automatic limit usage tracking
        // - persist event history if useful
        // - trigger side-panel refresh
        // - trigger notifications (offer complete, slot free, etc.)
    }
}
