package com.maskedmajic.gremlinge;

import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;

public class GremlinGE {
    public static void main(String[] args) {
        Settings settings = loadSettings();
        MarketDataService service = new MarketDataService();

        try {
            List<FlipCandidate> candidates = service.fetchCandidates(settings);
            candidates.sort(Comparator.comparingInt((FlipCandidate c) -> c.score).reversed());
            render(candidates, settings.topN);
            System.out.println("\nLive market data wired in. Next step: buy-limit tracking.");
        } catch (Exception e) {
            System.err.println("Failed to fetch live market data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Settings loadSettings() {
        InputStream input = GremlinGE.class.getClassLoader().getResourceAsStream("settings.json");
        if (input == null) {
            return new Settings();
        }
        return new Gson().fromJson(new InputStreamReader(input, StandardCharsets.UTF_8), Settings.class);
    }

    private static void render(List<FlipCandidate> rows, int topN) {
        System.out.println("\nGremlinGE Java v0.2 — Live High-Volume Flip Candidates");
        System.out.println("----------------------------------------------------------------------------------------------------------------");
        System.out.printf("%-28s %10s %10s %10s %8s %10s %12s %8s%n", "Item", "Buy", "Sell", "Margin", "Vol", "Limit", "5m Volume", "Score");
        System.out.println("----------------------------------------------------------------------------------------------------------------");

        for (int i = 0; i < Math.min(rows.size(), topN); i++) {
            FlipCandidate row = rows.get(i);
            System.out.printf(
                "%-28s %10s %10s %10s %8s %10s %12s %8s%n",
                truncate(row.name, 28),
                String.format("%,d", row.buy),
                String.format("%,d", row.sell),
                String.format("%,d", row.margin),
                row.volumeTag,
                String.format("%,d", row.buyLimit),
                String.format("%,d", row.volume5m),
                String.format("%,d", row.score)
            );
        }

        System.out.println("----------------------------------------------------------------------------------------------------------------");
    }

    private static String truncate(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 3) + "...";
    }
}
