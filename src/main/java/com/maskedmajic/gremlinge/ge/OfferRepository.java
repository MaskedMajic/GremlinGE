package com.maskedmajic.gremlinge.ge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Local-first persistence for live GE snapshots and recent event history.
 */
public class OfferRepository {
    private static final int MAX_STORED_EVENTS = 200;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path snapshotPath;
    private final Path eventsPath;

    public OfferRepository() {
        this(Paths.get("data", "ge_snapshot.json"), Paths.get("data", "ge_events.json"));
    }

    public OfferRepository(Path snapshotPath, Path eventsPath) {
        this.snapshotPath = snapshotPath;
        this.eventsPath = eventsPath;
    }

    public void saveSnapshot(GeOfferSnapshot snapshot) throws IOException {
        if (snapshot == null) {
            return;
        }

        createParent(snapshotPath);
        try (Writer writer = Files.newBufferedWriter(snapshotPath, StandardCharsets.UTF_8)) {
            gson.toJson(snapshot, writer);
        }
    }

    public GeOfferSnapshot loadSnapshot() throws IOException {
        if (!Files.exists(snapshotPath)) {
            return null;
        }

        try (Reader reader = Files.newBufferedReader(snapshotPath, StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, GeOfferSnapshot.class);
        }
    }

    public void saveEvents(List<GeOfferEvent> events) throws IOException {
        createParent(eventsPath);
        try (Writer writer = Files.newBufferedWriter(eventsPath, StandardCharsets.UTF_8)) {
            gson.toJson(trimToRecent(events), writer);
        }
    }

    public void appendEvents(List<GeOfferEvent> newEvents) throws IOException {
        if (newEvents == null || newEvents.isEmpty()) {
            return;
        }

        List<GeOfferEvent> all = loadEvents();
        all.addAll(newEvents);
        saveEvents(all);
    }

    public List<GeOfferEvent> loadEvents() throws IOException {
        if (!Files.exists(eventsPath)) {
            return new ArrayList<GeOfferEvent>();
        }

        Type type = new TypeToken<List<GeOfferEvent>>() {}.getType();
        try (Reader reader = Files.newBufferedReader(eventsPath, StandardCharsets.UTF_8)) {
            List<GeOfferEvent> events = gson.fromJson(reader, type);
            return events != null ? events : new ArrayList<GeOfferEvent>();
        }
    }

    private List<GeOfferEvent> trimToRecent(List<GeOfferEvent> events) {
        if (events == null || events.isEmpty()) {
            return new ArrayList<GeOfferEvent>();
        }

        int start = Math.max(0, events.size() - MAX_STORED_EVENTS);
        return new ArrayList<GeOfferEvent>(events.subList(start, events.size()));
    }

    private void createParent(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}
