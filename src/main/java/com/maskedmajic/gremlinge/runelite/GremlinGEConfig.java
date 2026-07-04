package com.maskedmajic.gremlinge.runelite;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("gremlinge")
public interface GremlinGEConfig extends Config {
    @ConfigItem(
        keyName = "notificationsEnabled",
        name = "Notifications enabled",
        description = "Enable future GremlinGE notifications"
    )
    default boolean notificationsEnabled() {
        return true;
    }

    @ConfigItem(
        keyName = "recommendationsEnabled",
        name = "Show next flips",
        description = "Display scanner-backed next-flip recommendations in the panel"
    )
    default boolean recommendationsEnabled() {
        return true;
    }

    @ConfigItem(
        keyName = "recommendationCount",
        name = "Next flips count",
        description = "How many scanner-backed recommendations to show"
    )
    default int recommendationCount() {
        return 5;
    }

    @ConfigItem(
        keyName = "marketRefreshSeconds",
        name = "Market refresh seconds",
        description = "Minimum seconds between scanner market refreshes"
    )
    default int marketRefreshSeconds() {
        return 60;
    }
}
