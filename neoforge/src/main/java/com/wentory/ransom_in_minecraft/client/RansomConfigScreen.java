package com.wentory.ransom_in_minecraft.client;

import com.wentory.ransom_in_minecraft.RansomwareConfig;
import com.wentory.ransom_in_minecraft.StealerConfig;
import com.wentory.ransom_in_minecraft.network.RansomwareSettingsPayload;
import com.wentory.ransom_in_minecraft.network.RansomwareSyncPayload;
import com.wentory.ransom_in_minecraft.network.StealerSettingsPayload;
import com.wentory.ransom_in_minecraft.network.WormSettingsPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.LinkedHashMap;

public final class RansomConfigScreen extends Screen {
    private final Screen parent;
    private Button deletionButton;
    private Button biomeWhitelistButton;
    private Button stealerButton;
    private boolean stealerEnabled;
    private boolean canEditStealer;
    private boolean deleteItems;
    private boolean biomeWhitelistEnabled;
    private boolean resetRansomwareTiming;
    private EditBox belowField;
    private EditBox aboveField;
    private EditBox radiusField;
    private EditBox spawnMinField;
    private EditBox spawnMaxField;
    private EditBox biomeWhitelistField;
    private EditBox stealerChanceField;
    private EditBox wormHealField;
    private EditBox zombieChanceField;
    private EditBox drownedChanceField;
    private EditBox creeperChanceField;
    private Button wormButton;
    private boolean wormEnabled;
    private Button resetButton;
    private Button doneButton;
    private int scroll;
    private final Map<AbstractWidget, Integer> rows = new LinkedHashMap<>();

    public RansomConfigScreen(Screen parent) {
        super(Component.literal("Ransom In Minecraft"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        canEditStealer = minecraft.player == null
                || minecraft.getSingleplayerServer() != null || minecraft.player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
        deleteItems = ransomValue(RansomwareConfig.DELETE_HOTBAR_ON_FAILURE);
        biomeWhitelistEnabled = ransomValue(RansomwareConfig.BIOME_WHITELIST_ENABLED);
        stealerEnabled = serverValue(StealerConfig.ENABLED);
        wormEnabled = serverValue(StealerConfig.WORM_ENABLED);
        rows.clear();
        int fieldX = width / 2 + 42;
        belowField = numberField(fieldX, 24, ransomValue(RansomwareConfig.INFECTION_VERTICAL_BELOW));
        aboveField = numberField(fieldX, 42, ransomValue(RansomwareConfig.INFECTION_VERTICAL_ABOVE));
        radiusField = numberField(fieldX, 60, ransomValue(RansomwareConfig.INFECTION_RADIUS));
        spawnMinField = numberField(fieldX, 78, ransomValue(RansomwareConfig.NATURAL_SPAWN_MIN_SECONDS));
        spawnMaxField = numberField(fieldX, 96, ransomValue(RansomwareConfig.NATURAL_SPAWN_MAX_SECONDS));
        for (EditBox field : new EditBox[]{belowField, aboveField, radiusField, spawnMinField, spawnMaxField}) {
            field.setEditable(canEditStealer);
        }

        biomeWhitelistField = addRenderableWidget(new EditBox(font, width / 2 - 128, 151, 256, 18,
                Component.literal("Biome whitelist")) {
            @Override
            public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
                super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTick);
                if (getValue().isEmpty() && !isFocused()) {
                    graphics.text(font,
                            font.plainSubstrByWidth("minecraft:plains, minecraft:forest", getInnerWidth()),
                            getX() + 4, getY() + (getHeight() - 8) / 2, 0x80666666, false);
                }
            }
        });
        biomeWhitelistField.setValue(ransomValue(RansomwareConfig.BIOME_WHITELIST));
        biomeWhitelistField.setEditable(canEditStealer);

        deletionButton = addRenderableWidget(Button.builder(deletionLabel(), button -> {
            deleteItems = !deleteItems;
            button.setMessage(deletionLabel());
        }).bounds(width / 2 - 128, 117, 124, 18).build());
        deletionButton.active = canEditStealer;
        biomeWhitelistButton = addRenderableWidget(Button.builder(biomeWhitelistLabel(), button -> {
            biomeWhitelistEnabled = !biomeWhitelistEnabled;
            button.setMessage(biomeWhitelistLabel());
        }).bounds(width / 2 + 4, 117, 124, 18).build());
        biomeWhitelistButton.active = canEditStealer;
        stealerButton = addRenderableWidget(Button.builder(stealerLabel(), button -> {
            stealerEnabled = !stealerEnabled;
            button.setMessage(stealerLabel());
        }).bounds(width / 2 - 128, 173, 256, 18).build());
        stealerButton.active = canEditStealer;
        stealerChanceField = numberField(fieldX, 194, serverValue(StealerConfig.INFECTION_CHANCE_PERCENT));
        stealerChanceField.setEditable(canEditStealer);
        wormButton = addRenderableWidget(Button.builder(wormLabel(), button -> {
            wormEnabled = !wormEnabled;
            button.setMessage(wormLabel());
        }).bounds(width / 2 - 128, 216, 256, 18).build());
        wormButton.active = canEditStealer;
        wormHealField = numberField(fieldX, 238, serverValue(StealerConfig.WORM_HEAL_TICKS));
        wormHealField.setEditable(canEditStealer);
        wormHealField.setTooltip(Tooltip.create(Component.literal("1 second = 20 ticks. Default: 60 ticks (3 seconds) per Hijack point.")));
        zombieChanceField = numberField(fieldX, 280, serverValue(StealerConfig.HIJACK_ZOMBIE_CHANCE));
        drownedChanceField = numberField(fieldX, 302, serverValue(StealerConfig.HIJACK_DROWNED_CHANCE));
        creeperChanceField = numberField(fieldX, 324, serverValue(StealerConfig.HIJACK_CREEPER_CHANCE));
        for (EditBox field : new EditBox[]{zombieChanceField, drownedChanceField, creeperChanceField}) {
            field.setEditable(canEditStealer);
            field.setTooltip(Tooltip.create(Component.literal("Natural spawns only. 0–100%. Default: 5%.")));
        }
        for (var child : children()) if (child instanceof AbstractWidget widget) rows.put(widget, widget.getY());
        resetButton = addRenderableWidget(Button.builder(Component.literal("Reset to Defaults"), button -> resetDefaults())
                .bounds(width / 2 - 110, height - 24, 106, 20).build());
        doneButton = addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(width / 2 + 4, height - 24, 106, 20).build());
        updateScroll();
    }

    private static <T> T serverValue(net.neoforged.neoforge.common.ModConfigSpec.ConfigValue<T> value) {
        if (net.minecraft.client.Minecraft.getInstance().player != null
                && net.minecraft.client.Minecraft.getInstance().getSingleplayerServer() == null) {
            return com.wentory.ransom_in_minecraft.network.CommonSettingsPayload.remoteValue(value);
        }
        return StealerConfig.SPEC.isLoaded() ? value.get() : value.getDefault();
    }
    private static <T> T ransomValue(net.neoforged.neoforge.common.ModConfigSpec.ConfigValue<T> value) {
        if (net.minecraft.client.Minecraft.getInstance().player != null
                && net.minecraft.client.Minecraft.getInstance().getSingleplayerServer() == null) {
            return RansomwareSyncPayload.value(value);
        }
        return RansomwareConfig.SPEC.isLoaded() ? value.get() : value.getDefault();
    }
    private Component wormLabel() { return Component.literal("Ransom Hijack: " + (wormEnabled ? "ON" : "OFF")); }

    private void updateScroll() {
        scroll = Math.clamp(scroll, 0, Math.max(0, 350 - (height - 32)));
        rows.forEach((widget, y) -> {
            widget.setY(y - scroll);
            widget.visible = widget.getY() >= 20 && widget.getY() + widget.getHeight() <= height - 32;
        });
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        scroll -= (int) (deltaY * 20);
        updateScroll();
        return true;
    }

    private EditBox numberField(int x, int y, int value) {
        EditBox field = addRenderableWidget(new EditBox(font, x, y, 80, 18, Component.empty()));
        field.setValue(Integer.toString(value));

        return field;
    }

    private Component deletionLabel() {
        return Component.literal("Delete items: "
                + (deleteItems ? "ON" : "OFF"));
    }

    private Component biomeWhitelistLabel() {
        return Component.literal("Biome whitelist: "
                + (biomeWhitelistEnabled ? "ON" : "OFF"));
    }

    private Component stealerLabel() {
        return Component.literal("RANSOM: STEALER: " + (stealerEnabled ? "ON" : "OFF"));
    }

    private void resetDefaults() {
        if (canEditStealer) {
            resetRansomwareTiming = true;
            deleteItems = true;
            biomeWhitelistEnabled = false;
            belowField.setValue("5");
            aboveField.setValue("5");
            radiusField.setValue("64");
            spawnMinField.setValue("180");
            spawnMaxField.setValue("360");
            biomeWhitelistField.setValue("");
            deletionButton.setMessage(deletionLabel());
            biomeWhitelistButton.setMessage(biomeWhitelistLabel());
        }
        if (canEditStealer) {
            stealerEnabled = true;
            stealerButton.setMessage(stealerLabel());
            stealerChanceField.setValue("30");
            wormEnabled = true;
            wormButton.setMessage(wormLabel());
            wormHealField.setValue("60");
            zombieChanceField.setValue("5");
            drownedChanceField.setValue("5");
            creeperChanceField.setValue("5");
        }
    }

    @Override
    public void onClose() {
        if (canEditStealer) {
            int below = parse(belowField, ransomValue(RansomwareConfig.INFECTION_VERTICAL_BELOW), 0, 128);
            int above = parse(aboveField, ransomValue(RansomwareConfig.INFECTION_VERTICAL_ABOVE), 0, 128);
            int radius = parse(radiusField, ransomValue(RansomwareConfig.INFECTION_RADIUS), 8, 256);
            int spawnMin = parse(spawnMinField, ransomValue(RansomwareConfig.NATURAL_SPAWN_MIN_SECONDS), 1, 86400);
            int spawnMax = parse(spawnMaxField, ransomValue(RansomwareConfig.NATURAL_SPAWN_MAX_SECONDS), 1, 86400);
            spawnMax = Math.max(spawnMin, spawnMax);
            double jumpscareEnd = resetRansomwareTiming ? 1.0 : ransomValue(RansomwareConfig.JUMPSCARE_ENDS_AT);
            double downloadStart = resetRansomwareTiming ? 1.0 : ransomValue(RansomwareConfig.DOWNLOAD_STARTS_AT);
            double downloadEnd = resetRansomwareTiming ? 2.0 : ransomValue(RansomwareConfig.DOWNLOAD_ENDS_AT);
            String biomes = biomeWhitelistField.getValue().trim();
            if (minecraft.player == null) {
                RansomwareConfig.JUMPSCARE_ENDS_AT.set(jumpscareEnd);
                RansomwareConfig.DOWNLOAD_STARTS_AT.set(downloadStart);
                RansomwareConfig.DOWNLOAD_ENDS_AT.set(downloadEnd);
                RansomwareConfig.DELETE_HOTBAR_ON_FAILURE.set(deleteItems);
                RansomwareConfig.INFECTION_VERTICAL_BELOW.set(below);
                RansomwareConfig.INFECTION_VERTICAL_ABOVE.set(above);
                RansomwareConfig.INFECTION_RADIUS.set(radius);
                RansomwareConfig.NATURAL_SPAWN_MIN_SECONDS.set(spawnMin);
                RansomwareConfig.NATURAL_SPAWN_MAX_SECONDS.set(spawnMax);
                RansomwareConfig.BIOME_WHITELIST_ENABLED.set(biomeWhitelistEnabled);
                RansomwareConfig.BIOME_WHITELIST.set(biomes);
                RansomwareConfig.SPEC.save();
            } else {
                net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new RansomwareSettingsPayload(jumpscareEnd, downloadStart,
                        downloadEnd, deleteItems, below, above, radius, spawnMin, spawnMax,
                        biomeWhitelistEnabled, biomes));
            }
        }
        if (canEditStealer) {
            int chance = parse(stealerChanceField, serverValue(StealerConfig.INFECTION_CHANCE_PERCENT), 0, 100);
            if (minecraft.player == null || minecraft.getSingleplayerServer() != null) StealerConfig.ENABLED.set(stealerEnabled);
            if (minecraft.player == null || minecraft.getSingleplayerServer() != null) StealerConfig.INFECTION_CHANCE_PERCENT.set(chance);
            if (minecraft.player != null) net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new StealerSettingsPayload(stealerEnabled, chance));
            int healingTicks = parse(wormHealField, serverValue(StealerConfig.WORM_HEAL_TICKS), 1, 72000);
            StealerConfig.WORM_ENABLED.set(wormEnabled);
            StealerConfig.WORM_HEAL_TICKS.set(healingTicks);
            int zombieChance = parse(zombieChanceField, serverValue(StealerConfig.HIJACK_ZOMBIE_CHANCE), 0, 100);
            int drownedChance = parse(drownedChanceField, serverValue(StealerConfig.HIJACK_DROWNED_CHANCE), 0, 100);
            int creeperChance = parse(creeperChanceField, serverValue(StealerConfig.HIJACK_CREEPER_CHANCE), 0, 100);
            StealerConfig.HIJACK_ZOMBIE_CHANCE.set(zombieChance);
            StealerConfig.HIJACK_DROWNED_CHANCE.set(drownedChance);
            StealerConfig.HIJACK_CREEPER_CHANCE.set(creeperChance);
            if (minecraft.player != null) net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new WormSettingsPayload(wormEnabled, healingTicks,
                    zombieChance, drownedChance, creeperChance));
            if (minecraft.player == null || minecraft.getSingleplayerServer() != null) StealerConfig.SPEC.save();
        }
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
        graphics.fill(0, 0, width, height, 0xC0101010);
        graphics.enableScissor(0, 20, width, height - 32);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.pose().pushMatrix();
        graphics.pose().translate(0, -scroll);
        int labelX = width / 2 - 128;
        graphics.text(font, "Infection below player", labelX, 29, 0xFFFFFFFF, false);
        graphics.text(font, "Infection above player", labelX, 47, 0xFFFFFFFF, false);
        graphics.text(font, "Game radius (blocks)", labelX, 65, 0xFFFFFFFF, false);
        graphics.text(font, "Spawn interval from (sec)", labelX, 83, 0xFFFFFFFF, false);
        graphics.text(font, "Spawn interval to (sec)", labelX, 101, 0xFFFFFFFF, false);
        graphics.text(font, "Allowed biomes", labelX, 140, 0xFFFFFFFF, false);
        graphics.text(font, "Chest infection chance (%)", labelX, 199, 0xFFFFFFFF, false);
        graphics.text(font, "Hijack recovery interval (ticks)", labelX, 243, 0xFFFFFFFF, false);
        graphics.text(font, "1 second = 20 ticks", labelX, 260, 0xFFAAAAAA, false);
        graphics.text(font, "Hijacked zombie chance (%)", labelX, 285, 0xFFFFFFFF, false);
        graphics.text(font, "Hijacked drowned chance (%)", labelX, 307, 0xFFFFFFFF, false);
        graphics.text(font, "Hijacked creeper chance (%)", labelX, 329, 0xFFFFFFFF, false);
        graphics.pose().popMatrix();
        graphics.disableScissor();
        resetButton.extractRenderState(graphics, mouseX, mouseY, partialTick);
        doneButton.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 8, 0xFFFFFFFF);
    }
}
