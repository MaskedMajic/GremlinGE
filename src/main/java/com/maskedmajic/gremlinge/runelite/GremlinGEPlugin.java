package com.maskedmajic.gremlinge.runelite;

import com.maskedmajic.gremlinge.ge.GeOfferEvent;
import com.maskedmajic.gremlinge.ge.GeOfferSnapshot;
import com.maskedmajic.gremlinge.ge.GeStateReader;
import com.maskedmajic.gremlinge.ge.GeStateTracker;
import com.maskedmajic.gremlinge.ge.LimitUsageService;
import net.runelite.api.Client;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.client.game.ItemManager;

import java.nio.file.Paths;
import java.util.List;

/**
 * RuneLite plugin skeleton with real event/data flow.
 */
public class GremlinGEPlugin {
    private final GeStateReader stateReader = new GeStateReader();
    private final GeStateTracker stateTracker = new GeStateTracker();
    private final LimitUsageService limitUsageService = new LimitUsageService(Paths.get("data", "purchases.json"));

    private Client client;
    private ItemManager itemManager;

    public void startUp(Client client, ItemManager itemManager) {
        this.client = client;
        this.itemManager = itemManager;

        GeOfferSnapshot snapshot = stateReader.readCurrentSnapshot(client, itemManager);
        stateTracker.update(snapshot);
    }

    public void shutDown() {
        this.client = null;
        this.itemManager = null;
    }

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
        try {
            java.util.List<GeOfferEvent> one = java.util.Collections.singletonList(geEvent);
            limitUsageService.consumeEvents(one);
        } catch (Exception ignored) {
            // TODO: add proper plugin logging once RuneLite plugin lifecycle is fully wired.
        }

        // TODO next steps:
        // - persist richer event history if useful
        // - trigger side-panel refresh
        // - trigger notifications (offer complete, slot free, etc.)
    }
}
