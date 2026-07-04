package com.maskedmajic.gremlinge;

import com.maskedmajic.gremlinge.ge.GeOfferState;
import com.maskedmajic.gremlinge.ge.LimitStatus;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FlipRecommendationService {
    private final MarketDataService marketDataService;

    public FlipRecommendationService() {
        this(new MarketDataService());
    }

    public FlipRecommendationService(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    public List<FlipRecommendation> recommend(
        Settings settings,
        List<GeOfferState> activeOffers,
        List<LimitStatus> limitStatuses,
        int maxResults
    ) throws IOException, InterruptedException {
        List<FlipCandidate> candidates = marketDataService.fetchCandidates(settings);
        Set<String> activeNames = buildActiveNameSet(activeOffers);
        Map<String, LimitStatus> limitsByName = buildLimitMap(limitStatuses);

        List<FlipRecommendation> recommendations = new ArrayList<FlipRecommendation>();
        for (FlipCandidate candidate : candidates) {
            boolean alreadyActive = activeNames.contains(normalize(candidate.name));
            LimitStatus status = limitsByName.get(normalize(candidate.name));
            int remainingLimit = status != null ? status.remaining : candidate.buyLimit;

            if (alreadyActive) {
                continue;
            }
            if (remainingLimit <= 0) {
                continue;
            }

            int recommendationScore = candidate.score
                + Math.min(remainingLimit / 100, 100)
                + volumeTagBonus(candidate.volumeTag);

            recommendations.add(new FlipRecommendation(candidate, remainingLimit, false, recommendationScore));
        }

        recommendations.sort(new Comparator<FlipRecommendation>() {
            @Override
            public int compare(FlipRecommendation a, FlipRecommendation b) {
                return Integer.compare(b.recommendationScore, a.recommendationScore);
            }
        });

        if (recommendations.size() <= maxResults) {
            return recommendations;
        }
        return new ArrayList<FlipRecommendation>(recommendations.subList(0, maxResults));
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

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
