package com.maskedmajic.gremlinge.runelite;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class GremlinGEPluginTest {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(GremlinGEPlugin.class);
        RuneLite.main(args);
    }
}
