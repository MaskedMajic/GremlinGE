package com.maskedmajic.gremlinge.ge;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

public class ItemLimitService {
    private final Map<String, Integer> limits;

    public ItemLimitService() {
        this.limits = loadLimits();
    }

    public int getLimit(int itemId) {
        Integer value = limits.get(Integer.toString(itemId));
        return value != null ? value : 0;
    }

    private Map<String, Integer> loadLimits() {
        try {
            InputStream input = ItemLimitService.class.getClassLoader().getResourceAsStream("item_limits.json");
            if (input == null) {
                return Collections.emptyMap();
            }
            Type type = new TypeToken<Map<String, Integer>>() {}.getType();
            Map<String, Integer> map = new Gson().fromJson(new InputStreamReader(input, StandardCharsets.UTF_8), type);
            return map != null ? map : Collections.<String, Integer>emptyMap();
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
