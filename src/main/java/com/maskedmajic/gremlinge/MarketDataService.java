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

    private List<FlipCandidate> cachedCandidates = new ArrayList<FlipCandidate>();
    private long cacheLoadedAtMillis;
    private long cacheTtlMillis = DEFAULT_CACHE_MILLIS;

    public List<FlipCandidate> fetchCandidates(Settings settings) throws IOException, InterruptedException {
        return fetchCandidates(settings, false);
    }

    public synchronized List<FlipCandidate> fetchCandidates(Settings settings, boolean forceRefresh) throws IOException, InterruptedException {
        long now = System.currentTimeMillis();
        if (!forceRefresh && !cachedCandidates.isEmpty() && now - cacheLoadedAtMillis < cacheTtlMillis) {
            return new ArrayList<FlipCandidate>(cachedCandidates);
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
            int margin = high - low;
            if (margin < settings.minMargin) {
                continue;
            }
            if (low < settings.minPrice || low > settings.maxPrice) {
                continue;
            }

            int highVol = fiveMinRow.has("highPriceVolume") ? fiveMinRow.get("highPriceVolume").getAsInt() : 0;
            int lowVol = fiveMinRow.has("lowPriceVolume") ? fiveMinRow.get("lowPriceVolume").getAsInt() : 0;
            int totalVol = highVol + lowVol;
            if (settings.highVolumeOnly && totalVol < settings.minVolume5m) {
                continue;
            }

            int limit = item.has("limit") && !item.get("limit").isJsonNull() ? item.get("limit").getAsInt() : 0;
            String name = item.get("name").getAsString();
            String volumeTag = classifyVolume(totalVol);
            results.add(new FlipCandidate(name, low, high, margin, volumeTag, limit, totalVol));
        }

        cachedCandidates = new ArrayList<FlipCandidate>(results);
        cacheLoadedAtMillis = now;
        return new ArrayList<FlipCandidate>(cachedCandidates);
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

    private String classifyVolume(int totalVol) {
        if (totalVol >= 25000) return "high";
        if (totalVol >= 8000) return "med";
        return "low";
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
