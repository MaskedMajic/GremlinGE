package com.maskedmajic.gremlinge;

import com.maskedmajic.gremlinge.ge.GeOfferState;
import com.maskedmajic.gremlinge.ge.LimitStatus;
import com.maskedmajic.gremlinge.ge.LimitUsageService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FlipRecommendationService {
    private final MarketDataService marketDataService;
    private final LimitUsageService limitUsageService;

    public FlipRecommendationService() {
        this(new MarketDataService(), null);
    }

    public FlipRecommendationService(LimitUsageService limitUsageService) {
        this(new MarketDataService(), limitUsageService);
    }

    public FlipRecommendationService(MarketDataService marketDataService) {
        this(marketDataService, null);
    }

    public FlipRecommendationService(MarketDataService marketDataService, LimitUsageService limitUsageService) {
        this.marketDataService = marketDataService;
        this.limitUsageService = limitUsageService;
    }

    public void setCacheTtlMillis(long cacheTtlMillis) {
        marketDataService.setCacheTtlMillis(cacheTtlMillis);
    }

    public void clearCache() {
        marketDataService.clearCache();
    }

    public List<FlipRecommendation> recommend(
        Settings settings,
        List<GeOfferState> activeOffers,
        List<LimitStatus> limitStatuses,
        int maxResults
    ) throws IOException, InterruptedException {
        return recommend(settings, activeOffers, limitStatuses, maxResults, "All");
    }

    public List<FlipRecommendation> recommend(
        Settings settings,
        List<GeOfferState> activeOffers,
        List<LimitStatus> limitStatuses,
        int maxResults,
        String selectedBand
    ) throws IOException, InterruptedException {
        List<FlipCandidate> candidates = marketDataService.fetchCandidates(settings);
        Set<String> activeNames = buildActiveNameSet(activeOffers);
        Map<String, LimitStatus> limitsByName = buildLimitMap(limitStatuses);

        List<FlipRecommendation> recommendations = new ArrayList<FlipRecommendation>();
        for (FlipCandidate candidate : candidates) {
            boolean alreadyActive = activeNames.contains(normalize(candidate.name));
            LimitStatus status = limitsByName.get(normalize(candidate.name));
            int remainingLimit = resolveRemainingLimit(candidate, status);
            String priceBand = classifyPriceBand(candidate.buy);

            if (!matchesBand(priceBand, selectedBand)) {
                continue;
            }
            if (alreadyActive) {
                continue;
            }
            if (remainingLimit <= 0) {
                continue;
            }

            int recommendationScore = candidate.score
                + Math.min(remainingLimit / 100, 100)
                + volumeTagBonus(candidate.volumeTag)
                + priceBandBonus(priceBand)
                + recentActivityBonus(candidate.volume5m);

            recommendations.add(new FlipRecommendation(candidate, remainingLimit, false, recommendationScore, priceBand));
        }

        recommendations.sort(new Comparator<FlipRecommendation>() {
            @Override
            public int compare(FlipRecommendation a, FlipRecommendation b) {
                return Integer.compare(b.recommendationScore, a.recommendationScore);
            }
        });

        return diversify(recommendations, maxResults, selectedBand);
    }

    /**
     * Looks up a single item by exact (case-insensitive) name, bypassing the normal
     * margin/volume/price-band filters. Used for on-demand search results.
     */
    public FlipRecommendation search(
        String itemName,
        List<GeOfferState> activeOffers,
        List<LimitStatus> limitStatuses
    ) throws IOException, InterruptedException {
        FlipCandidate candidate = marketDataService.findByName(itemName);
        if (candidate == null) {
            return null;
        }

        Set<String> activeNames = buildActiveNameSet(activeOffers);
        Map<String, LimitStatus> limitsByName = buildLimitMap(limitStatuses);

        boolean alreadyActive = activeNames.contains(normalize(candidate.name));
        LimitStatus status = limitsByName.get(normalize(candidate.name));
        int remainingLimit = resolveRemainingLimit(candidate, status);
        String priceBand = classifyPriceBand(candidate.buy);
        int recommendationScore = candidate.score
            + Math.min(remainingLimit / 100, 100)
            + volumeTagBonus(candidate.volumeTag)
            + priceBandBonus(priceBand)
            + recentActivityBonus(candidate.volume5m);

        return new FlipRecommendation(candidate, remainingLimit, alreadyActive, recommendationScore, priceBand);
    }

    /**
     * Remaining buy limit for a candidate that isn't sitting in an active GE slot right now.
     * Falls back to raw purchase history so items don't appear to have a full limit again
     * just because their offer was collected/cleared.
     */
    private int resolveRemainingLimit(FlipCandidate candidate, LimitStatus status) {
        if (status != null) {
            return status.remaining;
        }
        if (limitUsageService == null || candidate.buyLimit <= 0) {
            return candidate.buyLimit;
        }
        try {
            int bought = limitUsageService.getBoughtInWindow(candidate.name);
            return Math.max(0, candidate.buyLimit - bought);
        } catch (IOException e) {
            return candidate.buyLimit;
        }
    }

    private List<FlipRecommendation> diversify(List<FlipRecommendation> recommendations, int maxResults, String selectedBand) {
        if (recommendations.size() <= maxResults) {
            return recommendations;
        }

        boolean useDiversification = selectedBand == null || "All".equals(selectedBand) || "0-100k".equals(selectedBand);
        if (!useDiversification) {
            return new ArrayList<FlipRecommendation>(recommendations.subList(0, maxResults));
        }

        List<FlipRecommendation> result = new ArrayList<FlipRecommendation>();
        Set<String> seenPrefixes = new LinkedHashSet<String>();

        for (FlipRecommendation recommendation : recommendations) {
            String prefix = normalize(prefixOf(recommendation.candidate.name));
            if (!seenPrefixes.contains(prefix) || result.size() + 3 >= maxResults) {
                result.add(recommendation);
                seenPrefixes.add(prefix);
            }
            if (result.size() >= maxResults) {
                return result;
            }
        }

        for (FlipRecommendation recommendation : recommendations) {
            if (!result.contains(recommendation)) {
                result.add(recommendation);
            }
            if (result.size() >= maxResults) {
                break;
            }
        }
        return result;
    }

    private Set<String> buildActiveNameSet(List<GeOfferState> activeOffers) {
        Set<String> names = new HashSet<String>();
        if (activeOffers == null) {
            return names;
        }

        for (GeOfferState offer : activeOffers) {
            if (offer == null || offer.isEmpty() || offer.itemName == null) {
                continue;
            }
            names.add(normalize(offer.itemName));
        }
        return names;
    }

    private Map<String, LimitStatus> buildLimitMap(List<LimitStatus> limitStatuses) {
        Map<String, LimitStatus> limits = new HashMap<String, LimitStatus>();
        if (limitStatuses == null) {
            return limits;
        }

        for (LimitStatus status : limitStatuses) {
            if (status == null || status.itemName == null) {
                continue;
            }
            limits.put(normalize(status.itemName), status);
        }
        return limits;
    }

    private int volumeTagBonus(String volumeTag) {
        if ("high".equalsIgnoreCase(volumeTag)) {
            return 25;
        }
        if ("med".equalsIgnoreCase(volumeTag)) {
            return 10;
        }
        return 0;
    }

    private int recentActivityBonus(int volume5m) {
        return Math.min(40, Math.max(0, volume5m / 1000));
    }

    private int priceBandBonus(String priceBand) {
        if ("0-100k".equals(priceBand)) {
            return 0;
        }
        if ("100k-1m".equals(priceBand)) {
            return 10;
        }
        if ("1m-10m".equals(priceBand)) {
            return 18;
        }
        if ("10m-50m".equals(priceBand)) {
            return 24;
        }
        if ("50m+".equals(priceBand)) {
            return 28;
        }
        return 0;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String classifyPriceBand(int price) {
        if (price <= 100_000) {
            return "0-100k";
        }
        if (price <= 1_000_000) {
            return "100k-1m";
        }
        if (price <= 10_000_000) {
            return "1m-10m";
        }
        if (price <= 50_000_000) {
            return "10m-50m";
        }
        return "50m+";
    }

    private boolean matchesBand(String candidateBand, String selectedBand) {
        return selectedBand == null || "All".equals(selectedBand) || selectedBand.equals(candidateBand);
    }

    private String prefixOf(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "";
        }
        String[] parts = name.trim().split("\\s+");
        return parts.length == 0 ? name : parts[0];
    }
}
