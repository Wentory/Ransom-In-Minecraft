package com.wentory.ransom_in_minecraft.client;

import com.wentory.ransom_in_minecraft.ClientConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class RansomConfigScreen extends Screen {
    private final Screen parent;
    private Button deletionButton;
    private EditBox belowField;
    private EditBox aboveField;
    private EditBox radiusField;
    private EditBox spawnMinField;
    private EditBox spawnMaxField;

    public RansomConfigScreen(Screen parent) {
        super(Component.literal("Ransom In Minecraft"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int fieldX = width / 2 + 42;
        belowField = numberField(fieldX, 40, ClientConfig.INFECTION_VERTICAL_BELOW.get());
        aboveField = numberField(fieldX, 64, ClientConfig.INFECTION_VERTICAL_ABOVE.get());
        radiusField = numberField(fieldX, 88, ClientConfig.INFECTION_RADIUS.get());
        spawnMinField = numberField(fieldX, 112, ClientConfig.NATURAL_SPAWN_MIN_SECONDS.get());
        spawnMaxField = numberField(fieldX, 136, ClientConfig.NATURAL_SPAWN_MAX_SECONDS.get());

        deletionButton = addRenderableWidget(Button.builder(deletionLabel(), button -> {
            ClientConfig.DELETE_HOTBAR_ON_FAILURE.set(!ClientConfig.DELETE_HOTBAR_ON_FAILURE.get());
            button.setMessage(deletionLabel());
        }).bounds(width / 2 - 110, 164, 220, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset to Defaults"), button -> resetDefaults())
                .bounds(width / 2 - 110, 196, 106, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(width / 2 + 4, 196, 106, 20).build());
    }

    private EditBox numberField(int x, int y, int value) {
        EditBox field = addRenderableWidget(new EditBox(font, x, y, 80, 18, Component.empty()));
        field.setValue(Integer.toString(value));
        return field;
    }

    private Component deletionLabel() {
        return Component.literal("Delete hotbar on failure: "
                + (ClientConfig.DELETE_HOTBAR_ON_FAILURE.get() ? "ON" : "OFF"));
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
        ClientConfig.SPEC.save();
        RansomEncounter.refreshNaturalSpawnTimer();
        minecraft.setScreenAndShow(parent);
    }

    private static int parse(EditBox field, int fallback, int minimum, int maximum) {
        try {
            return Math.max(minimum, Math.min(maximum, Integer.parseInt(field.getValue())));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractBackground(graphics, mouseX, mouseY, partialTick);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 14, 0xFFFFFFFF);
        int labelX = width / 2 - 128;
        graphics.text(font, "Infection below player", labelX, 45, 0xFFFFFFFF, false);
        graphics.text(font, "Infection above player", labelX, 69, 0xFFFFFFFF, false);
        graphics.text(font, "Game radius (blocks)", labelX, 93, 0xFFFFFFFF, false);
        graphics.text(font, "Spawn interval from (sec)", labelX, 117, 0xFFFFFFFF, false);
        graphics.text(font, "Spawn interval to (sec)", labelX, 141, 0xFFFFFFFF, false);
    }
}
