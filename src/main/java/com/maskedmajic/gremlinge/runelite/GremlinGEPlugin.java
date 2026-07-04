package com.maskedmajic.gremlinge.runelite;

import com.google.inject.Provides;
import com.maskedmajic.gremlinge.ge.GeOfferEvent;
import com.maskedmajic.gremlinge.ge.GeOfferSnapshot;
import com.maskedmajic.gremlinge.ge.GeStateReader;
import com.maskedmajic.gremlinge.ge.GeStateTracker;
import com.maskedmajic.gremlinge.ge.LimitStatus;
import com.maskedmajic.gremlinge.ge.LimitUsageService;
import com.maskedmajic.gremlinge.ge.OfferRepository;
import com.maskedmajic.gremlinge.profit.ProfitSummary;
import com.maskedmajic.gremlinge.profit.ProfitTrackerService;
import java.awt.image.BufferedImage;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ClientShutdown;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@PluginDescriptor(
    name = "GremlinGE",
    description = "High-volume GE flip assistant with offer-state tracking",
    tags = {"ge", "grandexchange", "flipping", "money"}
)
public class GremlinGEPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private ItemManager itemManager;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private GremlinGEConfig config;

    private final GeStateReader stateReader = new GeStateReader();
    private final GeStateTracker stateTracker = new GeStateTracker();
    private final OfferRepository offerRepository = new OfferRepository();
    private final LimitUsageService limitUsageService = new LimitUsageService(Paths.get("data", "purchases.json"));
    private final ProfitTrackerService profitTrackerService = new ProfitTrackerService(Paths.get("data", "fills.json"));
    private final GremlinGEPanel panel = new GremlinGEPanel();
    private final List<GeOfferEvent> recentEvents = new ArrayList<GeOfferEvent>();

    private NavigationButton navigationButton;

    @Override
    protected void startUp() {
        BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        navigationButton = NavigationButton.builder()
            .tooltip("GremlinGE")
            .icon(icon)
            .priority(5)
            .panel(panel)
            .build();

        clientToolbar.addNavigation(navigationButton);
        loadRecentEvents();

        GeOfferSnapshot snapshot = stateReader.readCurrentSnapshot(client, itemManager);
        stateTracker.update(snapshot);
        persistSnapshot(snapshot);
        refreshPanel(snapshot);
    }

    @Override
    protected void shutDown() {
        persistSnapshot(stateReader.readCurrentSnapshot(client, itemManager));

        if (navigationButton != null) {
            clientToolbar.removeNavigation(navigationButton);
            navigationButton = null;
        }
    }

    @Subscribe
    public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged event) {
        if (client == null || itemManager == null || event == null) {
            return;
        }

        GeOfferSnapshot snapshot = stateReader.readCurrentSnapshot(client, itemManager);
        List<GeOfferEvent> events = stateTracker.update(snapshot);
        persistSnapshot(snapshot);

        if (!events.isEmpty()) {
            recentEvents.addAll(events);
            trimRecentEvents();
            persistEvents(events);
        }

        for (GeOfferEvent geEvent : events) {
            handleGeEvent(geEvent);
        }

        refreshPanel(snapshot);
    }

    @Subscribe
    public void onClientShutdown(ClientShutdown event) {
        persistSnapshot(stateReader.readCurrentSnapshot(client, itemManager));
    }

    @Provides
    GremlinGEConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(GremlinGEConfig.class);
    }

    private void handleGeEvent(GeOfferEvent geEvent) {
        try {
            List<GeOfferEvent> one = Collections.singletonList(geEvent);
            limitUsageService.consumeEvents(one);
            profitTrackerService.consumeEvents(one);
        } catch (Exception ignored) {
        }
    }

    private void refreshPanel(GeOfferSnapshot snapshot) {
        List<com.maskedmajic.gremlinge.ge.GeOfferState> offers = snapshot != null
            ? snapshot.slots
            : Collections.<com.maskedmajic.gremlinge.ge.GeOfferState>emptyList();

        panel.updateSummary(offers);
        panel.updateOffers(offers);
        panel.updateEvents(recentEvents);

        try {
            List<LimitStatus> statuses = limitUsageService.buildLimitStatuses(offers);
            panel.updateLimits(statuses);
        } catch (Exception e) {
            panel.updateLimits(Collections.<LimitStatus>emptyList());
        }

        try {
            ProfitSummary summary = profitTrackerService.summarize();
            panel.updateProfit(summary);
        } catch (Exception e) {
            panel.updateProfit(null);
        }
    }

    private void loadRecentEvents() {
        recentEvents.clear();
        try {
            recentEvents.addAll(offerRepository.loadEvents());
            trimRecentEvents();
        } catch (Exception ignored) {
        }
    }

    private void persistSnapshot(GeOfferSnapshot snapshot) {
        try {
            offerRepository.saveSnapshot(snapshot);
        } catch (Exception ignored) {
        }
    }

    private void persistEvents(List<GeOfferEvent> events) {
        try {
            offerRepository.appendEvents(events);
        } catch (Exception ignored) {
        }
    }

    private void trimRecentEvents() {
        int overflow = recentEvents.size() - 50;
        if (overflow > 0) {
            recentEvents.subList(0, overflow).clear();
        }
    }
}
