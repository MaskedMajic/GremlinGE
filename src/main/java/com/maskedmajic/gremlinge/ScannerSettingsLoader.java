package com.maskedmajic.gremlinge;

import com.google.gson.Gson;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class ScannerSettingsLoader {
    private ScannerSettingsLoader() {}

    public static Settings load() {
        InputStream input = GremlinGE.class.getClassLoader().getResourceAsStream("settings.json");
        if (input == null) {
            return new Settings();
        }
        return new Gson().fromJson(new InputStreamReader(input, StandardCharsets.UTF_8), Settings.class);
    }
}
