package com.wentory.ransom_in_minecraft.client;

import com.wentory.ransom_in_minecraft.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class RansomConfigScreen extends Screen {
    private final Screen parent;
    private Button deletionButton;
    private Button biomeWhitelistButton;
    private EditBox belowField;
    private EditBox aboveField;
    private EditBox radiusField;
    private EditBox spawnMinField;
    private EditBox spawnMaxField;
    private EditBox biomeWhitelistField;

    public RansomConfigScreen(Screen parent) {
        super(Component.literal("Ransom In Minecraft"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int fieldX = width / 2 + 42;
        int buttonsY = Math.min(height - 24, 216);
        belowField = numberField(fieldX, 35, ClientConfig.INFECTION_VERTICAL_BELOW.get());
        aboveField = numberField(fieldX, 55, ClientConfig.INFECTION_VERTICAL_ABOVE.get());
        radiusField = numberField(fieldX, 75, ClientConfig.INFECTION_RADIUS.get());
        spawnMinField = numberField(fieldX, 95, ClientConfig.NATURAL_SPAWN_MIN_SECONDS.get());
        spawnMaxField = numberField(fieldX, 115, ClientConfig.NATURAL_SPAWN_MAX_SECONDS.get());

        biomeWhitelistField = addRenderableWidget(new EditBox(font, width / 2 - 110, buttonsY - 23, 220, 18, Component.empty()));
        biomeWhitelistField.setHint(Component.literal("minecraft:plains, minecraft:forest").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        biomeWhitelistField.setValue(ClientConfig.BIOME_WHITELIST.get());
        biomeWhitelistButton = addRenderableWidget(Button.builder(biomeWhitelistLabel(), button -> {
            ClientConfig.BIOME_WHITELIST_ENABLED.set(!ClientConfig.BIOME_WHITELIST_ENABLED.get());
            button.setMessage(biomeWhitelistLabel());
        }).bounds(width / 2 - 110, buttonsY - 48, 220, 20).build());
        deletionButton = addRenderableWidget(Button.builder(deletionLabel(), button -> {
            ClientConfig.DELETE_HOTBAR_ON_FAILURE.set(!ClientConfig.DELETE_HOTBAR_ON_FAILURE.get());
            button.setMessage(deletionLabel());
        }).bounds(width / 2 - 110, buttonsY - 74, 220, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset to Defaults"), button -> resetDefaults())
                .bounds(width / 2 - 110, buttonsY, 106, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(width / 2 + 4, buttonsY, 106, 20).build());
    }

    private EditBox numberField(int x, int y, int value) {
        EditBox field = addRenderableWidget(new EditBox(font, x, y, 80, 18, Component.empty()));
        field.setValue(Integer.toString(value));
        field.setFilter(text -> text.isEmpty() || text.chars().allMatch(Character::isDigit));
        return field;
    }

    private Component deletionLabel() {
        return Component.literal("Delete hotbar on failure: "
                + (ClientConfig.DELETE_HOTBAR_ON_FAILURE.get() ? "ON" : "OFF"));
    }

    private Component biomeWhitelistLabel() {
        return Component.literal("Biome whitelist: " + (ClientConfig.BIOME_WHITELIST_ENABLED.get() ? "ON" : "OFF"));
    }

    private void resetDefaults() {
        ClientConfig.JUMPSCARE_ENDS_AT.set(1.0);
        ClientConfig.DOWNLOAD_STARTS_AT.set(1.0);
        ClientConfig.DOWNLOAD_ENDS_AT.set(2.0);
        ClientConfig.DELETE_HOTBAR_ON_FAILURE.set(true);
        ClientConfig.INFECTION_VERTICAL_BELOW.set(5);
        ClientConfig.INFECTION_VERTICAL_ABOVE.set(5);
        ClientConfig.INFECTION_RADIUS.set(64);
        ClientConfig.NATURAL_SPAWN_MIN_SECONDS.set(180);
        ClientConfig.NATURAL_SPAWN_MAX_SECONDS.set(360);
        ClientConfig.BIOME_WHITELIST_ENABLED.set(false);
        ClientConfig.BIOME_WHITELIST.set("");

        belowField.setValue("5");
        aboveField.setValue("5");
        radiusField.setValue("64");
        spawnMinField.setValue("180");
        spawnMaxField.setValue("360");
        deletionButton.setMessage(deletionLabel());
    }

    @Override
    public void onClose() {
        int below = parse(belowField, ClientConfig.INFECTION_VERTICAL_BELOW.get(), 0, 128);
        int above = parse(aboveField, ClientConfig.INFECTION_VERTICAL_ABOVE.get(), 0, 128);
        int radius = parse(radiusField, ClientConfig.INFECTION_RADIUS.get(), 8, 256);
        int spawnMin = parse(spawnMinField, ClientConfig.NATURAL_SPAWN_MIN_SECONDS.get(), 1, 86400);
        int spawnMax = parse(spawnMaxField, ClientConfig.NATURAL_SPAWN_MAX_SECONDS.get(), 1, 86400);
        spawnMax = Math.max(spawnMin, spawnMax);
        ClientConfig.INFECTION_VERTICAL_BELOW.set(below);
        ClientConfig.INFECTION_VERTICAL_ABOVE.set(above);
        ClientConfig.INFECTION_RADIUS.set(radius);
        ClientConfig.NATURAL_SPAWN_MIN_SECONDS.set(spawnMin);
        ClientConfig.NATURAL_SPAWN_MAX_SECONDS.set(spawnMax);
        ClientConfig.BIOME_WHITELIST.set(biomeWhitelistField.getValue().trim());
        ClientConfig.SPEC.save();
        RansomEncounter.refreshNaturalSpawnTimer();
        minecraft.setScreen(parent);
    }

    private static int parse(EditBox field, int fallback, int minimum, int maximum) {
        try {
            return Math.max(minimum, Math.min(maximum, Integer.parseInt(field.getValue())));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 14, 0xFFFFFFFF);
        int labelX = width / 2 - 128;
        graphics.drawString(font, "Infection below player", labelX, 40, 0xFFFFFFFF, false);
        graphics.drawString(font, "Infection above player", labelX, 60, 0xFFFFFFFF, false);
        graphics.drawString(font, "Game radius (blocks)", labelX, 80, 0xFFFFFFFF, false);
        graphics.drawString(font, "Spawn interval from (sec)", labelX, 100, 0xFFFFFFFF, false);
        graphics.drawString(font, "Spawn interval to (sec)", labelX, 120, 0xFFFFFFFF, false);
    }
}
