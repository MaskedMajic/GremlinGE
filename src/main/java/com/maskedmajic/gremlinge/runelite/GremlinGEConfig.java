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
}
