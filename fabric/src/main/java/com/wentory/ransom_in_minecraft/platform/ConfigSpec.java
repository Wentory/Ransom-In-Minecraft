package com.wentory.ransom_in_minecraft.platform;

import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.function.Function;

public final class ConfigSpec {
    private final Map<String, ConfigValue<?>> values;
    private Path file;
    private ConfigSpec(Map<String, ConfigValue<?>> values) { this.values = Map.copyOf(values); }
    public synchronized void load(String filename) {
        file = FabricLoader.getInstance().getConfigDir().resolve(filename);
        Properties properties = new Properties();
        if (Files.exists(file)) try (InputStream input = Files.newInputStream(file)) { properties.load(input); }
        catch (IOException e) { throw new UncheckedIOException("Cannot read Ransom settings", e); }
        values.forEach((key,value) -> value.load(properties.getProperty(key)));
        if (!Files.exists(file)) save();
    }
    public synchronized void save() {
        if (file == null) throw new IllegalStateException("Config has not been loaded");
        Properties properties = new Properties();
        values.forEach((key,value) -> properties.setProperty(key, value.get().toString()));
        try { Files.createDirectories(file.getParent());
            try (OutputStream out = Files.newOutputStream(file)) {
                properties.store(out, "Ransom settings. 1 second = 20 ticks.");
            }
        } catch (IOException e) { throw new UncheckedIOException("Cannot save Ransom settings", e); }
    }
    public boolean isLoaded() { return file != null; }
    public static class ConfigValue<T> {
        private final T fallback;
        private final Function<String,T> parse;
        private volatile T value;
        ConfigValue(T fallback, Function<String,T> parse) { this.fallback=fallback; this.value=fallback; this.parse=parse; }
        public T get() { return value; }
        public T getDefault() { return fallback; }
        public void set(T value) { this.value=parse.apply(value.toString()); }
        void load(String raw) { try { value = raw == null ? fallback : parse.apply(raw); } catch (RuntimeException e) { value=fallback; } }
    }
    public static final class IntValue extends ConfigValue<Integer> {
        IntValue(int value, int min, int max) { super(value, s -> Math.clamp(Integer.parseInt(s),min,max)); }
    }
    public static final class DoubleValue extends ConfigValue<Double> {
        DoubleValue(double value, double min, double max) { super(value, s -> {
            double v=Double.parseDouble(s); return Double.isFinite(v) ? Math.clamp(v,min,max) : value;
        }); }
    }
    public static final class BooleanValue extends ConfigValue<Boolean> {
        BooleanValue(boolean value) { super(value, Boolean::parseBoolean); }
    }
    public static final class Builder {
        private final Map<String,ConfigValue<?>> values=new LinkedHashMap<>();
        public Builder comment(String... comment) { return this; }
        private <T extends ConfigValue<?>> T add(String key,T value) { values.put(key,value); return value; }
        public IntValue defineInRange(String key,int value,int min,int max) { return add(key,new IntValue(value,min,max)); }
        public DoubleValue defineInRange(String key,double value,double min,double max) { return add(key,new DoubleValue(value,min,max)); }
        public BooleanValue define(String key,boolean value) { return add(key,new BooleanValue(value)); }
        public ConfigValue<String> define(String key,String value) { return add(key,new ConfigValue<>(value, s -> s)); }
        public ConfigSpec build() { return new ConfigSpec(values); }
    }
}
