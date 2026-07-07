package com.maskedmajic.gremlinge;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MarketDataService {
    private static final String UA = "GremlinGE prototype (local dev)";
    private static final String LATEST_URL = "https://prices.runescape.wiki/api/v1/osrs/latest";
    private static final String FIVE_MIN_URL = "https://prices.runescape.wiki/api/v1/osrs/5m";
    private static final String MAPPING_URL = "https://prices.runescape.wiki/api/v1/osrs/mapping";
    private static final long DEFAULT_CACHE_MILLIS = 60_000L;

    private final HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(20))
        .build();
    private final Gson gson = new Gson();

    // Unfiltered: every tradeable item with a live high/low price, regardless of margin/volume/price thresholds.
    // fetchCandidates() filters this down per Settings; search/autocomplete reads it directly.
    private List<FlipCandidate> cachedAllCandidates = new ArrayList<FlipCandidate>();
    private long cacheLoadedAtMillis;
    private long cacheTtlMillis = DEFAULT_CACHE_MILLIS;

    public List<FlipCandidate> fetchCandidates(Settings settings) throws IOException, InterruptedException {
        return fetchCandidates(settings, false);
    }

    public List<FlipCandidate> fetchCandidates(Settings settings, boolean forceRefresh) throws IOException, InterruptedException {
        List<FlipCandidate> all = fetchAllCandidates(forceRefresh);
        List<FlipCandidate> results = new ArrayList<FlipCandidate>();
        for (FlipCandidate candidate : all) {
            if (candidate.margin < settings.minMargin) {
                continue;
            }
            if (candidate.buy < settings.minPrice || candidate.buy > settings.maxPrice) {
                continue;
            }
            if (settings.highVolumeOnly && candidate.volume5m < settings.minVolume5m) {
                continue;
            }
            results.add(candidate);
        }
        return results;
    }

    /**
     * All tradeable items with a live price, unfiltered by margin/volume/price thresholds.
     * Backs item-name autocomplete and single-item lookups.
     */
    public synchronized List<FlipCandidate> fetchAllCandidates(boolean forceRefresh) throws IOException, InterruptedException {
        long now = System.currentTimeMillis();
        if (!forceRefresh && !cachedAllCandidates.isEmpty() && now - cacheLoadedAtMillis < cacheTtlMillis) {
            return new ArrayList<FlipCandidate>(cachedAllCandidates);
        }

        JsonObject latest = fetchJsonObject(LATEST_URL).getAsJsonObject("data");
        JsonObject fiveMin = fetchJsonObject(FIVE_MIN_URL).getAsJsonObject("data");
        JsonArray mapping = fetchJsonArray(MAPPING_URL);

        List<FlipCandidate> results = new ArrayList<FlipCandidate>();
        for (JsonElement el : mapping) {
            JsonObject item = el.getAsJsonObject();
            int id = item.get("id").getAsInt();
            String idKey = Integer.toString(id);
            if (!latest.has(idKey) || !fiveMin.has(idKey)) {
                continue;
            }

            JsonObject latestRow = latest.getAsJsonObject(idKey);
            JsonObject fiveMinRow = fiveMin.getAsJsonObject(idKey);
            if (!latestRow.has("high") || !latestRow.has("low")) {
                continue;
            }

            int high = latestRow.get("high").getAsInt();
            int low = latestRow.get("low").getAsInt();
            int grossMargin = high - low;
            int tax = estimateGeTax(high);
            int netMargin = high - tax - low;

            int highVol = fiveMinRow.has("highPriceVolume") ? fiveMinRow.get("highPriceVolume").getAsInt() : 0;
            int lowVol = fiveMinRow.has("lowPriceVolume") ? fiveMinRow.get("lowPriceVolume").getAsInt() : 0;
            int totalVol = highVol + lowVol;

            int limit = item.has("limit") && !item.get("limit").isJsonNull() ? item.get("limit").getAsInt() : 0;
            String name = item.get("name").getAsString();
            String volumeTag = classifyVolume(totalVol);
            results.add(new FlipCandidate(id, name, low, high, grossMargin, tax, netMargin, volumeTag, limit, totalVol));
        }

        cachedAllCandidates = new ArrayList<FlipCandidate>(results);
        cacheLoadedAtMillis = now;
        return new ArrayList<FlipCandidate>(cachedAllCandidates);
    }

    /** Sorted, de-duplicated item names for search/autocomplete. Triggers a fetch if the cache is stale. */
    public List<String> fetchItemNames() throws IOException, InterruptedException {
        List<FlipCandidate> all = fetchAllCandidates(false);
        List<String> names = new ArrayList<String>(all.size());
        for (FlipCandidate candidate : all) {
            names.add(candidate.name);
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    /** Exact (case-insensitive) name lookup, bypassing all Settings thresholds. Null if not found. */
    public FlipCandidate findByName(String itemName) throws IOException, InterruptedException {
        if (itemName == null || itemName.trim().isEmpty()) {
            return null;
        }
        for (FlipCandidate candidate : fetchAllCandidates(false)) {
            if (candidate.name.equalsIgnoreCase(itemName.trim())) {
                return candidate;
            }
        }
        return null;
    }

    public synchronized void setCacheTtlMillis(long cacheTtlMillis) {
        this.cacheTtlMillis = Math.max(5_000L, cacheTtlMillis);
    }

    public synchronized long getCacheAgeMillis() {
        if (cacheLoadedAtMillis <= 0) {
            return Long.MAX_VALUE;
        }
        return Math.max(0L, System.currentTimeMillis() - cacheLoadedAtMillis);
    }

    public synchronized void clearCache() {
        cachedAllCandidates = new ArrayList<FlipCandidate>();
        cacheLoadedAtMillis = 0L;
    }

    private String classifyVolume(int totalVol) {
        if (totalVol >= 25000) return "high";
        if (totalVol >= 8000) return "med";
        return "low";
    }

    private int estimateGeTax(int sellPrice) {
        return (int) Math.floor(sellPrice * 0.02d);
    }

    private JsonObject fetchJsonObject(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .header("User-Agent", UA)
            .GET()
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(response.body(), JsonObject.class);
    }

    private JsonArray fetchJsonArray(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .header("User-Agent", UA)
            .GET()
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(response.body(), JsonArray.class);
    }
}
