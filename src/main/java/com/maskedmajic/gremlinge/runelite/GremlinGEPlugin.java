package com.maskedmajic.gremlinge.runelite;

import com.google.inject.Provides;
import com.maskedmajic.gremlinge.FlipRecommendation;
import com.maskedmajic.gremlinge.FlipRecommendationService;
import com.maskedmajic.gremlinge.ScannerSettingsLoader;
import com.maskedmajic.gremlinge.Settings;
import com.maskedmajic.gremlinge.ge.GeOfferEvent;
import com.maskedmajic.gremlinge.ge.GeOfferSnapshot;
import com.maskedmajic.gremlinge.ge.GeOfferState;
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
import javax.swing.JComboBox;
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
    private final FlipRecommendationService flipRecommendationService = new FlipRecommendationService();
    private final GremlinGEPanel panel = new GremlinGEPanel();
    private final List<GeOfferEvent> recentEvents = new ArrayList<GeOfferEvent>();

    private NavigationButton navigationButton;
    private boolean forceFlipRefresh;

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
        wirePanelActions();
        loadRecentEvents();
        loadPreviousSnapshot();

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

    private void wirePanelActions() {
        panel.getResetProfitButton().addActionListener(e -> {
            try {
                profitTrackerService.reset();
            } catch (Exception ignored) {
            }
            refreshPanel(stateReader.readCurrentSnapshot(client, itemManager));
        });

        panel.getRefreshFlipsButton().addActionListener(e -> {
            flipRecommendationService.clearCache();
            forceFlipRefresh = true;
            refreshPanel(stateReader.readCurrentSnapshot(client, itemManager));
        });

        panel.getTierDropdown().addActionListener(e -> {
            flipRecommendationService.clearCache();
            forceFlipRefresh = true;
            refreshPanel(stateReader.readCurrentSnapshot(client, itemManager));
        });
    }

    private void refreshPanel(GeOfferSnapshot snapshot) {
        List<GeOfferState> offers = snapshot != null
            ? snapshot.slots
            : Collections.<GeOfferState>emptyList();

        panel.updateSummary(offers);
        panel.updateOffers(offers);

        List<LimitStatus> statuses = Collections.emptyList();
        try {
            statuses = limitUsageService.buildLimitStatuses(offers);
            panel.updateLimits(statuses);
        } catch (Exception e) {
            panel.updateLimits(Collections.<LimitStatus>emptyList());
        }

        if (config.recommendationsEnabled()) {
            try {
                Settings settings = filteredSettings(ScannerSettingsLoader.load(), panel.getTierDropdown());
                flipRecommendationService.setCacheTtlMillis(config.marketRefreshSeconds() * 1000L);
                if (forceFlipRefresh) {
                    flipRecommendationService.clearCache();
                }
                List<FlipRecommendation> recommendations = flipRecommendationService.recommend(
                    settings,
                    offers,
                    statuses,
                    Math.max(1, config.recommendationCount())
                );
                forceFlipRefresh = false;
                panel.updateRecommendations(recommendations);
            } catch (Exception e) {
                forceFlipRefresh = false;
                panel.updateRecommendations(Collections.<FlipRecommendation>emptyList());
            }
        } else {
            panel.updateRecommendations(Collections.<FlipRecommendation>emptyList());
        }

        try {
            ProfitSummary summary = profitTrackerService.summarize();
            panel.updateProfit(summary);
        } catch (Exception e) {
            panel.updateProfit(null);
        }
    }

    private Settings filteredSettings(Settings base, JComboBox<String> dropdown) {
        Settings settings = new Settings();
        settings.topN = base.topN;
        settings.minMargin = base.minMargin;
        settings.minVolume5m = base.minVolume5m;
        settings.minPrice = base.minPrice;
        settings.maxPrice = base.maxPrice;
        settings.highVolumeOnly = base.highVolumeOnly;

        Object selected = dropdown.getSelectedItem();
        String tier = selected != null ? selected.toString() : "All";
        switch (tier) {
            case "0-100k":
                settings.minPrice = 0;
                settings.maxPrice = 100_000;
                break;
            case "100k-1m":
                settings.minPrice = 100_001;
                settings.maxPrice = 1_000_000;
                break;
            case "1m-10m":
                settings.minPrice = 1_000_001;
                settings.maxPrice = 10_000_000;
                break;
            case "10m-50m":
                settings.minPrice = 10_000_001;
                settings.maxPrice = 50_000_000;
                break;
            case "50m+":
                settings.minPrice = 50_000_001;
                settings.maxPrice = Integer.MAX_VALUE;
                break;
            default:
                break;
        }
        return settings;
    }

    private void loadRecentEvents() {
        recentEvents.clear();
        try {
            recentEvents.addAll(offerRepository.loadEvents());
            trimRecentEvents();
        } catch (Exception ignored) {
        }
    }

    private void loadPreviousSnapshot() {
        try {
            GeOfferSnapshot snapshot = offerRepository.loadSnapshot();
            if (snapshot != null) {
                stateTracker.seed(snapshot);
            }
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
