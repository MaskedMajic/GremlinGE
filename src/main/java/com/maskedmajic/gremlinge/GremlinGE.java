package com.maskedmajic.gremlinge;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

public class GremlinGE {
    public static void main(String[] args) {
        Settings settings = ScannerSettingsLoader.load();
        MarketDataService service = new MarketDataService();
        LimitTrackerService limitTracker = new LimitTrackerService(Paths.get("data", "purchases.json"));

        try {
            if (args.length >= 3 && "buy".equalsIgnoreCase(args[0])) {
                String itemName = args[1].replace('_', ' ');
                int quantity = Integer.parseInt(args[2]);
                limitTracker.addPurchase(itemName, quantity);
                System.out.println("Logged purchase: " + itemName + " x" + quantity);
                return;
            }

            List<FlipCandidate> candidates = service.fetchCandidates(settings);
            candidates.sort(Comparator.comparingInt((FlipCandidate c) -> c.score).reversed());
            render(candidates, settings.topN, limitTracker);
            System.out.println("\nCommands:");
            System.out.println("  gradle -q run --args='buy Death_rune 5000'");
            System.out.println("\nNext step: smarter filtering + RuneLite-side offer state later.");
        } catch (Exception e) {
            System.err.println("Failed to run GremlinGE: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void render(List<FlipCandidate> rows, int topN, LimitTrackerService limitTracker) throws Exception {
        System.out.println("\nGremlinGE Java v0.3 — Live High-Volume Flip Candidates");
        System.out.println("----------------------------------------------------------------------------------------------------------------------------------------");
        System.out.printf("%-24s %10s %10s %8s %8s %10s %12s %8s %12s %10s%n", "Item", "Buy", "Sell", "Margin", "Vol", "Limit", "5m Volume", "Score", "Limit Left", "Reset ETA");
        System.out.println("----------------------------------------------------------------------------------------------------------------------------------------");

        for (int i = 0; i < Math.min(rows.size(), topN); i++) {
            FlipCandidate row = rows.get(i);
            int bought = limitTracker.getBoughtInWindow(row.name);
            int left = Math.max(0, row.buyLimit - bought);
            String eta = row.buyLimit > 0 ? limitTracker.getNextResetEta(row.name) : "n/a";

            System.out.printf(
                "%-24s %10s %10s %8s %8s %10s %12s %8s %12s %10s%n",
                truncate(row.name, 24),
                String.format("%,d", row.buy),
                String.format("%,d", row.sell),
                String.format("%,d", row.margin),
                row.volumeTag,
                String.format("%,d", row.buyLimit),
                String.format("%,d", row.volume5m),
                String.format("%,d", row.score),
                row.buyLimit > 0 ? String.format("%,d", left) : "n/a",
                eta
            );
        }

        System.out.println("----------------------------------------------------------------------------------------------------------------------------------------");
    }

    private static String truncate(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 3) + "...";
    }
}
