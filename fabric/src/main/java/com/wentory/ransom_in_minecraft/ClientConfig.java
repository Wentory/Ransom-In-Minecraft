package com.wentory.ransom_in_minecraft;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ClientConfig {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("ransom_in_minecraft.properties");
    private static final Properties VALUES = new Properties();

    public static final Value<Double> JUMPSCARE_ENDS_AT = new Value<>("timing.jumpscareEndsAtSeconds", 1.0, Double::parseDouble);
    public static final Value<Double> DOWNLOAD_STARTS_AT = new Value<>("timing.downloadStartsAtSeconds", 1.0, Double::parseDouble);
    public static final Value<Double> DOWNLOAD_ENDS_AT = new Value<>("timing.downloadEndsAtSeconds", 2.0, Double::parseDouble);
    public static final Value<Boolean> DELETE_HOTBAR_ON_FAILURE = new Value<>("punishment.deleteHotbarOnFailure", true, Boolean::parseBoolean);
    public static final Value<Integer> INFECTION_VERTICAL_BELOW = new Value<>("game.infectionVerticalBelow", 5, Integer::parseInt);
    public static final Value<Integer> INFECTION_VERTICAL_ABOVE = new Value<>("game.infectionVerticalAbove", 5, Integer::parseInt);
    public static final Value<Integer> INFECTION_RADIUS = new Value<>("game.infectionRadius", 64, Integer::parseInt);
    public static final Value<Integer> NATURAL_SPAWN_MIN_SECONDS = new Value<>("spawn.minimumSeconds", 180, Integer::parseInt);
    public static final Value<Integer> NATURAL_SPAWN_MAX_SECONDS = new Value<>("spawn.maximumSeconds", 360, Integer::parseInt);
    public static final Spec SPEC = new Spec();

    private ClientConfig() {}

    public static void load() {
        if (!Files.exists(FILE)) return;
        try (InputStream stream = Files.newInputStream(FILE)) {
            VALUES.load(stream);
            JUMPSCARE_ENDS_AT.load();
            DOWNLOAD_STARTS_AT.load();
            DOWNLOAD_ENDS_AT.load();
            DELETE_HOTBAR_ON_FAILURE.load();
            INFECTION_VERTICAL_BELOW.load();
            INFECTION_VERTICAL_ABOVE.load();
            INFECTION_RADIUS.load();
            NATURAL_SPAWN_MIN_SECONDS.load();
            NATURAL_SPAWN_MAX_SECONDS.load();
        } catch (IOException ignored) {
        }
    }

    public static final class Value<T> {
        private final String key;
        private final T fallback;
        private final java.util.function.Function<String, T> parser;
        private T value;
        private Value(String key, T fallback, java.util.function.Function<String, T> parser) {
            this.key = key;
            this.fallback = fallback;
            this.parser = parser;
            this.value = fallback;
        }
        public T get() { return value; }
        public void set(T value) {
            this.value = value;
            VALUES.setProperty(key, value.toString());
        }
        private void load() {
            try { value = parser.apply(VALUES.getProperty(key, fallback.toString())); }
            catch (RuntimeException ignored) { value = fallback; }
        }
    }

    public static final class Spec {
        public void save() {
            try {
                Files.createDirectories(FILE.getParent());
                try (OutputStream stream = Files.newOutputStream(FILE)) {
                    VALUES.store(stream, "Ransom In Minecraft settings");
                }
            } catch (IOException ignored) {
            }
        }
    }
}