package com.maskedmajic.gremlinge;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class LimitTrackerService {
    private static final long RESET_WINDOW_SECONDS = 4 * 60 * 60;
    private final Gson gson = new Gson();
    private final Path purchasesPath;

    public LimitTrackerService(Path purchasesPath) {
        this.purchasesPath = purchasesPath;
    }

    public List<PurchaseRecord> loadRecords() throws IOException {
        if (!Files.exists(purchasesPath)) {
            return new ArrayList<PurchaseRecord>();
        }
        String json = new String(Files.readAllBytes(purchasesPath), StandardCharsets.UTF_8);
        Type type = new TypeToken<List<PurchaseRecord>>() {}.getType();
        List<PurchaseRecord> records = gson.fromJson(json, type);
        return records != null ? records : new ArrayList<PurchaseRecord>();
    }

    public void saveRecords(List<PurchaseRecord> records) throws IOException {
        Files.write(purchasesPath, gson.toJson(records).getBytes(StandardCharsets.UTF_8));
    }

    public void addPurchase(String itemName, int quantity) throws IOException {
        List<PurchaseRecord> records = loadRecords();
        records.add(new PurchaseRecord(itemName, quantity, Instant.now().getEpochSecond()));
        saveRecords(records);
    }

    public int getBoughtInWindow(String itemName) throws IOException {
        long now = Instant.now().getEpochSecond();
        int total = 0;
        for (PurchaseRecord record : loadRecords()) {
            if (record.itemName.equalsIgnoreCase(itemName) && now - record.timestampEpochSeconds < RESET_WINDOW_SECONDS) {
                total += record.quantity;
            }
        }
        return total;
    }

    public String getNextResetEta(String itemName) throws IOException {
        long now = Instant.now().getEpochSecond();
        List<PurchaseRecord> relevant = loadRecords().stream()
            .filter(r -> r.itemName.equalsIgnoreCase(itemName) && now - r.timestampEpochSeconds < RESET_WINDOW_SECONDS)
            .sorted(Comparator.comparingLong(r -> r.timestampEpochSeconds))
            .collect(Collectors.toList());

        if (relevant.isEmpty()) {
            return "ready";
        }

        long resetAt = relevant.get(0).timestampEpochSeconds + RESET_WINDOW_SECONDS;
        long remaining = Math.max(0, resetAt - now);
        long hours = remaining / 3600;
        long minutes = (remaining % 3600) / 60;
        return String.format("%dh %02dm", hours, minutes);
    }
}
