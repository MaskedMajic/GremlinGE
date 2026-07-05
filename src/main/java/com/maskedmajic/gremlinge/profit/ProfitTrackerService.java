package com.maskedmajic.gremlinge.profit;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.maskedmajic.gremlinge.ge.GeOfferEvent;
import com.maskedmajic.gremlinge.ge.OfferEventType;
import com.maskedmajic.gremlinge.ge.OfferType;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight profit-tracker skeleton.
 *
 * v1 scope:
 * - record fill events (buy/sell)
 * - summarize gross buy/sell value using offer price snapshots
 * - approximate realized P/L from filled value difference
 *
 * Later we can improve pairing logic and per-item summaries.
 */
public class ProfitTrackerService {
    private final Gson gson = new Gson();
    private final Path fillsPath;

    public ProfitTrackerService(Path fillsPath) {
        this.fillsPath = fillsPath;
    }

    public List<FillRecord> loadFills() throws IOException {
        if (!Files.exists(fillsPath)) {
            return new ArrayList<FillRecord>();
        }
        String json = new String(Files.readAllBytes(fillsPath), StandardCharsets.UTF_8);
        Type type = new TypeToken<List<FillRecord>>() {}.getType();
        List<FillRecord> fills = gson.fromJson(json, type);
        return fills != null ? fills : new ArrayList<FillRecord>();
    }

    public void saveFills(List<FillRecord> fills) throws IOException {
        Files.createDirectories(fillsPath.getParent());
        Files.write(fillsPath, gson.toJson(fills).getBytes(StandardCharsets.UTF_8));
    }

    public void consumeEvents(List<GeOfferEvent> events) throws IOException {
        if (events == null || events.isEmpty()) {
            return;
        }

        List<FillRecord> fills = loadFills();
        boolean changed = false;

        for (GeOfferEvent event : events) {
            if (event.type != OfferEventType.PARTIAL_FILL && event.type != OfferEventType.COMPLETED) {
                continue;
            }
            if (event.deltaFilled <= 0) {
                continue;
            }

            TradeSide side;
            if (event.offerType == OfferType.BUY) {
                side = TradeSide.BUY;
            } else if (event.offerType == OfferType.SELL) {
                side = TradeSide.SELL;
            } else {
                continue;
            }

            fills.add(new FillRecord(
                event.itemName,
                event.itemId,
                side,
                event.deltaFilled,
                Math.max(0, event.price),
                event.timestampEpochSeconds > 0 ? event.timestampEpochSeconds : Instant.now().getEpochSecond()
            ));
            changed = true;
        }

        if (changed) {
            saveFills(fills);
        }
    }

    public ProfitSummary summarize() throws IOException {
        ProfitSummary summary = new ProfitSummary();
        for (FillRecord fill : loadFills()) {
            long value = (long) fill.quantity * (long) fill.price;
            if (fill.side == TradeSide.BUY) {
                summary.totalBuys += fill.quantity;
                summary.grossBuyValue += value;
            } else {
                summary.totalSells += fill.quantity;
                summary.grossSellValue += value;
            }
        }
        summary.realizedProfit = summary.grossSellValue - summary.grossBuyValue;
        return summary;
    }

    public void reset() throws IOException {
        saveFills(new ArrayList<FillRecord>());
    }
}
