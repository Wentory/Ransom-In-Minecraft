package com.wentory.ransom_in_minecraft.client;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import com.wentory.ransom_in_minecraft.network.ChestQteAdvancePayload;
import com.wentory.ransom_in_minecraft.network.ChestQteAbortPayload;
import com.wentory.ransom_in_minecraft.network.ChestQteFinishPayload;
import com.wentory.ransom_in_minecraft.network.ChestQteResultPayload;
import com.wentory.ransom_in_minecraft.network.ChestQteProgressPayload;
import com.wentory.ransom_in_minecraft.network.ChestQteStartPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.mojang.math.Axis;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.entity.player.Inventory;
import com.wentory.ransom_in_minecraft.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class RansomChestScreen extends ContainerScreen {
    private static final ResourceLocation RANSOM = new ResourceLocation(
            RansomInMinecraft.MODID, "textures/ransom.png");
    private static final ResourceLocation LOCK = new ResourceLocation(
            RansomInMinecraft.MODID, "textures/block.png");
    private static final ResourceLocation CHEST_GLITCH = new ResourceLocation(
            RansomInMinecraft.MODID, "textures/glitch2.png");
    private static final ResourceLocation JUMPSCARE = new ResourceLocation(
            RansomInMinecraft.MODID, "textures/jumpscare1.png");
    private static final ResourceLocation SCAN = new ResourceLocation(
            RansomInMinecraft.MODID, "textures/scan.png");
    private static final String[] REPLACEMENT_KEYS = {"A", "S", "D", "F", "G", "H", "J", "K", "L", "W", "E", "R"};
    private static final String[] WELL_DONE_ART = {
            "$$\\      $$\\ $$$$$$$$\\ $$\\       $$\\             $$$$$$$\\   $$$$$$\\  $$\\   $$\\ $$$$$$$$\\",
            "$$ | $\\  $$ |$$  _____|$$ |      $$ |            $$  __$$\\ $$  __$$\\ $$$\\  $$ |$$  _____|",
            "$$ |$$$\\ $$ |$$ |      $$ |      $$ |            $$ |  $$ |$$ /  $$ |$$$$\\ $$ |$$ |",
            "$$ $$ $$\\$$ |$$$$$\\    $$ |      $$ |            $$ |  $$ |$$ |  $$ |$$ $$\\$$ |$$$$$\\",
            "$$$$  _$$$$ |$$  __|   $$ |      $$ |            $$ |  $$ |$$ |  $$ |$$ \\$$$$ |$$  __|",
            "$$$  / \\$$$ |$$ |      $$ |      $$ |            $$ |  $$ |$$ |  $$ |$$ |\\$$$ |$$ |",
            "$$  /   \\$$ |$$$$$$$$\\ $$$$$$$$\\ $$$$$$$$\\       $$$$$$$  | $$$$$$  |$$ | \\$$ |$$$$$$$$\\",
            "\\__/     \\__|\\________|\\________|\\________|      \\_______/  \\______/ \\__|  \\__|\\________|"
    };
    private static RansomChestScreen active;

    private final UUID sessionId;
    private final BlockPos chestPos;
    private final int totalRounds;
    private List<String> sequence;
    private int round;
    private static final int SCAN_DURATION = 38;
    private static final int WIN_JUMPSCARE_FADE_TICKS = 10;
    private int scanTicks = SCAN_DURATION;
    private int openingTicks;
    private int mainOpeningTicks;
    private boolean initialRoundPending;
    private int keyIndex;
    private int roundTicks;
    private long roundDeadlineMillis;
    private int feedbackTicks;
    private int finalTicks;
    private int finalOutcome;
    private FadingJumpscareSound interruptedJumpscare;
    private SimpleSoundInstance endingLaugh;
    private SimpleSoundInstance scanningSound;
    private SimpleSoundInstance soundtrack;
    private static final int LOST_ITEM_FLIGHT_TICKS = 20;
    private static final int LOST_ITEM_GLITCH_TICKS = 10;
    private static final int LOST_ITEM_TOTAL_TICKS = LOST_ITEM_FLIGHT_TICKS + LOST_ITEM_GLITCH_TICKS;
    private int replacementTarget = -1;
    private int replacementTrigger = -1;
    private int replacementGlitchTicks;
    private int replacementGlitchIndex = -1;
    private String pendingSequence = "";
    private int pendingRound;
    private boolean sentResult;
    private boolean finishSent;
    private boolean lastRoundSuccess;
    private boolean completedAltTrap;
    private boolean lastRoundWasAltTrap;
    private ItemStack lostStack = ItemStack.EMPTY;
    private int lostCount;
    private int visualTicks;
    private int chestLossTicks;
    private final ArrayDeque<String> terminalQueue = new ArrayDeque<>();
    private final List<String> terminalLines = new ArrayList<>();
    private String typingLine = "";
    private int typingProgress;
    private final PreviewChest previewChest = new PreviewChest();
    private final MovingWindow stealerWindow = new MovingWindow(0.50F, 0.14F, 100);
    private final MovingWindow terminalWindow = new MovingWindow(0.13F, 0.65F, 145);
    private final MovingWindow chestWindow = new MovingWindow(0.76F, 0.65F, 180);

    private RansomChestScreen(ChestMenu menu, Inventory inventory, ChestQteStartPayload payload) {
        super(menu, inventory, Component.translatable(
                menu.getRowCount() == 6 ? "container.chestDouble" : "container.chest"));
        this.sessionId = payload.sessionId();
        this.chestPos = BlockPos.of(payload.chestPos());
        this.round = payload.round();
        this.totalRounds = payload.totalRounds();
        this.sequence = decode(payload.sequence());
        prepareRound();
        this.keyIndex = payload.keyIndex();
        this.roundDeadlineMillis = System.currentTimeMillis() + payload.remainingMillis();
        this.roundTicks = Math.max(0, (int) ((roundDeadlineMillis - System.currentTimeMillis() + 49L) / 50L));
        this.initialRoundPending = !payload.resumed();
        if (payload.resumed()) {
            this.openingTicks = 7;
            this.scanTicks = 0;
            terminalQueue.add("m:/chest>: resume ENTRY_" + String.format(Locale.ROOT, "%02d", round));
        }
    }

    public static void open(ChestQteStartPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !(minecraft.player.containerMenu instanceof ChestMenu menu)) return;
        active = new RansomChestScreen(menu, minecraft.player.getInventory(), payload);
        minecraft.setScreen(active);
    }

    public static void advance(ChestQteAdvancePayload payload) {
        if (active == null || !active.sessionId.equals(payload.sessionId())) return;
        active.receiveAdvance(payload);
    }

    public static void abort(ChestQteAbortPayload payload) {
        if (active != null && active.sessionId.equals(payload.sessionId())) clear();
    }

    public static void clear() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof RansomChestScreen) minecraft.setScreen(null);
        active = null;
    }

    public static boolean isActive() {
        return active != null;
    }

    private void receiveAdvance(ChestQteAdvancePayload payload) {
        lastRoundSuccess = payload.roundSuccess();
        lastRoundWasAltTrap = payload.roundSuccess() && completedAltTrap;
        completedAltTrap = false;
        pendingSequence = payload.nextSequence();
        pendingRound = payload.round();
        finalOutcome = payload.finalOutcome();
        terminalQueue.add(payload.roundSuccess() ? "ACCESS DENIED / ITEM SAVED" : "TRANSFER COMPLETE");
        sentResult = false;
        if (!payload.roundSuccess() && !payload.lostItem().isEmpty()) {
            ResourceLocation id = ResourceLocation.tryParse(payload.lostItem());
            lostStack = id == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.get(id));
            lostCount = payload.lostCount();
            chestLossTicks = LOST_ITEM_TOTAL_TICKS;
            feedbackTicks = LOST_ITEM_TOTAL_TICKS;
        } else if (lastRoundWasAltTrap) {
            feedbackTicks = 14;
        } else if (finalOutcome == ChestQteAdvancePayload.NOT_FINISHED) {
            feedbackTicks = 8;
        } else {
            beginEnding();
        }
        if (!pendingSequence.isEmpty()) terminalQueue.add("m:/chest>: steal ENTRY_" + String.format(Locale.ROOT, "%02d", pendingRound));
        else terminalQueue.add("m:/chest>: exit");
    }

    @Override
    protected void containerTick() {
        visualTicks++;
        if (finalOutcome == ChestQteAdvancePayload.NOT_FINISHED || feedbackTicks > 0) {
            stealerWindow.tick();
            terminalWindow.tick();
            chestWindow.tick();
        }
        if (chestLossTicks > 0 && --chestLossTicks == LOST_ITEM_GLITCH_TICKS)
            playUiSound(RansomInMinecraft.GLITCH.get());
        if (openingTicks < 7) {
            openingTicks++;
            if (openingTicks == 4) {
                playUiSound(RansomInMinecraft.WINDOW_OPEN.get());
                scanningSound = SimpleSoundInstance.forUI(RansomInMinecraft.SCANNING.get(), 1.0F, 1.0F);
                minecraft.getSoundManager().play(scanningSound);
            }
            if (openingTicks < 4) return;
        }
        if (replacementGlitchTicks > 0 && --replacementGlitchTicks == 0) replacementGlitchIndex = -1;
        if (scanTicks > 0) {
            scanTicks--;
            if (scanTicks == 0) {
                stopScanningSound();
                playUiSound(RansomInMinecraft.WINDOW_CLOSE.get());
                terminalQueue.add("m:/chest>: steal ENTRY_01");
            }
            return;
        }
        if (mainOpeningTicks < 4) {
            mainOpeningTicks++;
            if (mainOpeningTicks == 1) playUiSound(RansomInMinecraft.WINDOW_OPEN.get());
            if (mainOpeningTicks == 4) {
                startSoundtrack();
                if (initialRoundPending) {
                    roundDeadlineMillis = System.currentTimeMillis() + timeForRound(sequence.size()) * 50L;
                    initialRoundPending = false;
                }
            }
            return;
        }
        tickTerminal();
        if (feedbackTicks > 0) {
            if (--feedbackTicks == 0) {
                if (pendingSequence.isEmpty()) beginEnding();
                else startNextRound();
            }
            return;
        }
        if (finalOutcome != ChestQteAdvancePayload.NOT_FINISHED) {
            if (finalOutcome == ChestQteAdvancePayload.SAVED_NONE && finalTicks == 4) {
                endingLaugh = SimpleSoundInstance.forUI(RansomInMinecraft.LAUGH.get(), 1.0F);
                minecraft.getSoundManager().play(endingLaugh);
            }
            if (finalOutcome == ChestQteAdvancePayload.SAVED_SOME
                    && finalTicks >= 8 && finalTicks < 32 && (finalTicks & 1) == 0) {
                playTerminalClick(0.25F);
            }
            if (finalOutcome == ChestQteAdvancePayload.SAVED_ALL && finalTicks == 16) {
                interruptedJumpscare = new FadingJumpscareSound();
                minecraft.getSoundManager().play(interruptedJumpscare);
            }
            if (finalTicks == finalCollapseStart()) playUiSound(RansomInMinecraft.WINDOW_CLOSE.get());
            if (++finalTicks >= endingLength()) finishAndClose();
            return;
        }
        maybeReplaceKey();
        if (sentResult || sequence.isEmpty()) return;
        roundTicks = Math.max(0, (int) ((roundDeadlineMillis - System.currentTimeMillis() + 49L) / 50L));
        if (roundTicks <= 0) {
            if (isAltTrap()) {
                completedAltTrap = true;
                passCurrentKey();
            }
            else submit(false);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (!canAcceptInput()) return true;
        if (!isQteInputKey(keyCode)) return true;
        playUiSound(SoundEvents.STONE_BUTTON_CLICK_ON);
        if (isAltTrap()) {
            submit(false);
            return true;
        }
        String pressed = keyName(keyCode);
        if (sequence.get(keyIndex).equals(pressed)) passCurrentKey();
        else submit(false);
        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean canAcceptInput() {
        return scanTicks <= 0 && mainOpeningTicks >= 4 && feedbackTicks <= 0
                && finalOutcome == ChestQteAdvancePayload.NOT_FINISHED && !sentResult
                && System.currentTimeMillis() < roundDeadlineMillis;
    }

    private void passCurrentKey() {
        keyIndex++;
        if (keyIndex >= sequence.size()) {
            submit(true);
            return;
        }
        maybeReplaceKey();
        PacketDistributor.sendToServer(new ChestQteProgressPayload(sessionId, round, keyIndex,
                String.join("|", sequence)));
    }

    private void submit(boolean success) {
        if (sentResult) return;
        sentResult = true;
        if (success) playUiSound(RansomInMinecraft.STEALER_SUCCESS.get());
        else playUiSound(RansomInMinecraft.FAILTURE.get());
        PacketDistributor.sendToServer(new ChestQteResultPayload(sessionId, round, success));
    }

    private void playUiSound(net.minecraft.sounds.SoundEvent sound) {
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F, 1.0F));
    }

    private void startNextRound() {
        round = pendingRound;
        sequence = decode(pendingSequence);
        roundDeadlineMillis = System.currentTimeMillis() + timeForRound(sequence.size()) * 50L;
        pendingSequence = "";
        lostStack = ItemStack.EMPTY;
        lostCount = 0;
        lastRoundWasAltTrap = false;
        keyIndex = 0;
        sentResult = false;
        prepareRound();
    }

    private void prepareRound() {
        keyIndex = 0;
        roundTicks = timeForRound(sequence.size());
        replacementGlitchTicks = 0;
        replacementGlitchIndex = -1;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int lastEligible = sequence.size() - 1;
        if (lastEligible >= 0 && "ALT+F4".equals(sequence.get(lastEligible))) lastEligible--;
        if (lastEligible >= 2 && random.nextFloat() < 0.38F) {
            replacementTarget = random.nextInt(2, lastEligible + 1);
            replacementTrigger = replacementTarget - 1;
        } else {
            replacementTarget = -1;
            replacementTrigger = -1;
        }
    }

    private void maybeReplaceKey() {
        if (replacementTarget < 0 || keyIndex != replacementTrigger) return;
        String old = sequence.get(replacementTarget);
        String replacement;
        do {
            replacement = REPLACEMENT_KEYS[ThreadLocalRandom.current().nextInt(REPLACEMENT_KEYS.length)];
        } while (replacement.equals(old));
        sequence.set(replacementTarget, replacement);
        replacementGlitchIndex = replacementTarget;
        replacementGlitchTicks = 2;
        playUiSound(RansomInMinecraft.GLITCH2.get());
        PacketDistributor.sendToServer(new ChestQteProgressPayload(sessionId, round, keyIndex,
                String.join("|", sequence)));
        replacementTarget = -1;
    }

    private boolean isAltTrap() {
        return keyIndex < sequence.size() && "ALT+F4".equals(sequence.get(keyIndex));
    }

    private static int timeForKey(int index) {
        if (index < 5) return 10;
        return 12 + (index - 5) * 2;
    }

    private static int timeForRound(int keyCount) {
        int total = 0;
        for (int index = 0; index < keyCount; index++) total += timeForKey(index);
        return total;
    }

    private void beginEnding() {
        feedbackTicks = 0;
        finalTicks = 0;
        playUiSound(RansomInMinecraft.WINDOW_CLOSE.get());
        stopSoundtrack();
        terminalQueue.add(finalOutcome == ChestQteAdvancePayload.SAVED_ALL ? "FAILURE / NO ITEMS STOLEN"
                : finalOutcome == ChestQteAdvancePayload.SAVED_SOME ? "PARTIAL TRANSFER" : "TRANSFER COMPLETE");
    }

    private void tickTerminal() {
        if (typingLine.isEmpty() && !terminalQueue.isEmpty()) {
            typingLine = terminalQueue.removeFirst();
            typingProgress = 0;
        }
        if (typingLine.isEmpty()) return;
        typingProgress = Math.min(typingLine.length(), typingProgress + 4);
        playTerminalClick(0.45F);
        if (typingProgress == typingLine.length()) {
            terminalLines.add(typingLine);
            if (terminalLines.size() > 10) terminalLines.remove(0);
            typingLine = "";
        }
    }

    private void playTerminalClick(float volume) {
        float semitones = ThreadLocalRandom.current().nextFloat(-2.0F, 2.0F);
        float pitch = (float) Math.pow(2.0D, semitones / 12.0D);
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                RansomInMinecraft.TERMINAL_CLICK.get(), pitch, volume));
    }

    private int endingLength() {
        return finalOutcome == ChestQteAdvancePayload.SAVED_ALL ? 26
                : finalOutcome == ChestQteAdvancePayload.SAVED_SOME ? 42
                : finalOutcome == ChestQteAdvancePayload.SAVED_NONE ? 42 : 22;
    }

    private int finalCollapseStart() {
        return finalOutcome == ChestQteAdvancePayload.SAVED_ALL ? 16
                : finalOutcome == ChestQteAdvancePayload.SAVED_SOME ? 38
                : finalOutcome == ChestQteAdvancePayload.SAVED_NONE ? 38 : 18;
    }

    private void finishAndClose() {
        if (finishSent) return;
        finishSent = true;
        stopSoundtrack();
        stopInterruptedJumpscare();
        stopEndingLaugh();
        PacketDistributor.sendToServer(new ChestQteFinishPayload(sessionId));
        sentResult = true;
    }

    private void stopInterruptedJumpscare() {
        if (interruptedJumpscare == null || minecraft == null) return;
        minecraft.getSoundManager().stop(interruptedJumpscare);
        interruptedJumpscare = null;
    }

    private void stopEndingLaugh() {
        if (endingLaugh == null || minecraft == null) return;
        minecraft.getSoundManager().stop(endingLaugh);
        endingLaugh = null;
    }

    private void startSoundtrack() {
        if (soundtrack != null || minecraft == null) return;
        soundtrack = new SimpleSoundInstance(RansomInMinecraft.SOUNDTRACK2.get().getLocation(),
                SoundSource.MASTER, 0.8F, 1.0F, RandomSource.create(), true, 0,
                SoundInstance.Attenuation.NONE, 0, 0, 0, true);
        minecraft.getSoundManager().play(soundtrack);
    }

    private void stopSoundtrack() {
        if (soundtrack == null || minecraft == null) return;
        minecraft.getSoundManager().stop(soundtrack);
        soundtrack = null;
    }

    private void stopScanningSound() {
        if (scanningSound == null || minecraft == null) return;
        minecraft.getSoundManager().stop(scanningSound);
        scanningSound = null;
    }

    @Override
    public void onClose() {
        stopScanningSound();
        stopSoundtrack();
        stopInterruptedJumpscare();
        stopEndingLaugh();
        active = null;
        super.onClose();
    }

    @Override
    public void removed() {
        stopScanningSound();
        stopSoundtrack();
        stopInterruptedJumpscare();
        stopEndingLaugh();
        super.removed();
        if (active == this) {
            active = null;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderSealedChest(graphics);
        if (openingTicks < 4) return;
        float fit = Math.min(1.0F, Math.min((width - 8) / 332.0F, (height - 8) / 224.0F));
        if (scanTicks > 0) {
            renderScanWindow(graphics, fit, Math.min(1.0F, (openingTicks - 3 + partialTick) / 4.0F), partialTick);
            return;
        }
        float grow = Math.min(1.0F, (mainOpeningTicks + partialTick) / 4.0F);
        if (finalOutcome != ChestQteAdvancePayload.NOT_FINISHED && feedbackTicks <= 0) {
            float closing = Math.max(0.0F, 1.0F - (finalTicks + partialTick) / 4.0F);
            if (closing > 0.0F) {
                renderMovingWindow(graphics, terminalWindow, 196, 100, fit, closing, 250, () -> {
                    window(graphics, 0, 0, 196, 100, "TERMINAL");
                    renderTerminal(graphics);
                });
                renderMovingWindow(graphics, chestWindow, 126, 100, fit, closing, 250, () -> {
                    window(graphics, 0, 0, 126, 100, "CHEST");
                    renderChestPreview(graphics, partialTick);
                });
            }
            renderEndingWindow(graphics, fit, partialTick);
            return;
        }
        renderMovingWindow(graphics, terminalWindow, 196, 100, fit, grow, 250, () -> {
            window(graphics, 0, 0, 196, 100, "TERMINAL");
            renderTerminal(graphics);
        });
        renderMovingWindow(graphics, chestWindow, 126, 100, fit, grow, 250, () -> {
            window(graphics, 0, 0, 126, 100, "CHEST");
            renderChestPreview(graphics, partialTick);
        });
        renderMovingWindow(graphics, stealerWindow, 228, 110, fit, grow, 450, () -> {
            window(graphics, 0, 0, 228, 110, "RANSOM: STEALER");
            graphics.blit(RANSOM, 12, 25, 64, 64, 0, 0, 200, 200, 200, 200);
            graphics.drawString(font, "INPUT", 87, 19, 0xFFFFFFFF, false);
            if (feedbackTicks <= 0 && finalOutcome == ChestQteAdvancePayload.NOT_FINISHED)
                renderQte(graphics, 86, 42, 130, partialTick);
        });
    }

    private void renderSealedChest(GuiGraphics graphics) {
        int x = leftPos + 7;
        int y = topPos + 17;
        int panelWidth = 162;
        int panelHeight = menu.getRowCount() * 18;
        int frame = visualTicks / 2 % 6;
        graphics.fill(x, y, x + panelWidth, y + panelHeight, 0xFF080508);
        graphics.enableScissor(x, y, x + panelWidth, y + panelHeight);
        for (int tileX = 0; tileX < panelWidth; tileX += 54) {
            for (int tileY = 0; tileY < panelHeight; tileY += 54) {
                int tileWidth = Math.min(54, panelWidth - tileX);
                int tileHeight = Math.min(54, panelHeight - tileY);
                graphics.blit(CHEST_GLITCH, x + tileX, y + tileY, tileWidth, tileHeight,
                        tileX * 37 % 75, frame * 128 + tileY * 31 % 75,
                        tileWidth, tileHeight, 128, 768);
            }
        }
        graphics.disableScissor();
        graphics.fill(x, y, x + panelWidth, y + 2, 0xFFFF2424);
        graphics.fill(x, y + panelHeight - 2, x + panelWidth, y + panelHeight, 0xFFFF2424);
        graphics.fill(x, y, x + 2, y + panelHeight, 0xFFFF2424);
        graphics.fill(x + panelWidth - 2, y, x + panelWidth, y + panelHeight, 0xFFFF2424);
        int stopSize = 64;
        int stopX = x + (panelWidth - stopSize) / 2;
        int stopY = y + (panelHeight - stopSize) / 2;
        graphics.blit(LOCK, stopX, stopY, stopSize, stopSize,
                0, 0, 200, 200, 200, 200);
    }

    private void renderEndingWindow(GuiGraphics graphics, float fit, float partialTick) {
        float transition = Math.min(1.0F, (finalTicks + partialTick) / 4.0F);
        float startX = stealerWindow.x * (width - 228 * fit) + 114 * fit;
        float startY = stealerWindow.y * (height - 110 * fit) + 55 * fit;
        float centerX = startX + (width / 2.0F - startX) * transition;
        float centerY = startY + (height / 2.0F - startY) * transition;
        boolean wideArtWindow = finalOutcome == ChestQteAdvancePayload.SAVED_SOME;
        int windowWidth = wideArtWindow ? Math.round(228 + 92 * transition) : 228;
        int windowHeight = wideArtWindow ? Math.round(110 - 40 * transition) : 110;
        float targetScale = wideArtWindow
                ? Math.min(fit * 1.15F, Math.min((width - 8.0F) / 320.0F, (height - 8.0F) / 70.0F))
                : fit * 1.45F;
        float scale = fit + (targetScale - fit) * transition;
        int collapseStart = finalCollapseStart();
        if (finalTicks > collapseStart) {
            int duration = Math.max(1, endingLength() - collapseStart);
            scale *= Math.max(0.04F, 1.0F - (finalTicks - collapseStart + partialTick) / duration);
        }
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 450);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.pose().translate(-windowWidth / 2.0F, -windowHeight / 2.0F, 0);
        window(graphics, 0, 0, windowWidth, windowHeight, "RANSOM: STEALER");
        int jitterX = finalOutcome == ChestQteAdvancePayload.SAVED_ALL && finalTicks > 9
                ? ThreadLocalRandom.current().nextInt(-3, 4) : 0;
        if (finalOutcome != ChestQteAdvancePayload.SAVED_SOME || finalTicks < 8) {
            int faceSize = wideArtWindow ? Math.round(64 - 16 * transition) : 64;
            graphics.blit(RANSOM, windowWidth / 2 - faceSize / 2 + jitterX,
                    wideArtWindow ? (windowHeight - faceSize) / 2 + 6 : 23,
                    faceSize, faceSize, 0, 0, 200, 200, 200, 200);
        }
        if (finalOutcome == ChestQteAdvancePayload.SAVED_ALL) {
            if (finalTicks >= 17) {
                int size = 76 + (finalTicks - 17) * 8;
                graphics.blit(JUMPSCARE, 114 - size / 2, 59 - size / 2,
                        size, size, 0, 0, 200, 200, 200, 200);
            }
        } else if (finalOutcome == ChestQteAdvancePayload.SAVED_SOME) {
            if (finalTicks >= 8) renderWellDonePrint(graphics, partialTick);
        } else if (finalOutcome == ChestQteAdvancePayload.SAVED_NONE) {
            for (int i = 0; i < 9; i++) {
                int xx = 7 + Math.floorMod(i * 73 + finalTicks * 17, 163);
                int yy = 16 + Math.floorMod(i * 29 + finalTicks * 11, 79);
                graphics.drawString(font, "HAHAHAH", xx, yy, 0xFFFF2424, false);
            }
        }
        graphics.pose().popPose();
    }

    private void renderWellDonePrint(GuiGraphics graphics, float partialTick) {
        int columns = 89;
        int totalCells = columns * WELL_DONE_ART.length;
        float progress = Math.min(1.0F, (finalTicks - 8 + partialTick) / 24.0F);
        int printed = Math.min(totalCells, (int) (progress * totalCells));
        graphics.pose().pushPose();
        graphics.pose().translate(10, 20, 0);
        graphics.pose().scale(0.55F, 0.55F, 1.0F);
        for (int row = 0; row < WELL_DONE_ART.length; row++) {
            String line = WELL_DONE_ART[row];
            for (int column = 0; column < line.length() && row * columns + column < printed; column++) {
                if (line.charAt(column) != ' ') {
                    graphics.drawString(font, String.valueOf(line.charAt(column)),
                            column * 6, row * 9, 0xFFF2F2F2, false);
                }
            }
        }
        if (printed > 0 && printed < totalCells) {
            int x = printed % columns * 6;
            int y = printed / columns * 9;
            graphics.fill(x, y, x + 5, y + 8, 0xFFFF3030);
        }
        graphics.pose().popPose();
    }

    private void renderScanWindow(GuiGraphics graphics, float fit, float grow, float partialTick) {
        int windowWidth = 190;
        int windowHeight = 72;
        graphics.pose().pushPose();
        graphics.pose().translate(width / 2.0F, height / 2.0F, 450);
        graphics.pose().scale(fit * grow, fit * grow, 1.0F);
        graphics.pose().translate(-windowWidth / 2.0F, -windowHeight / 2.0F, 0);
        window(graphics, 0, 0, windowWidth, windowHeight, "RANSOM: SCAN");
        float progress = Math.min(1.0F, (SCAN_DURATION - scanTicks + partialTick) / SCAN_DURATION);
        renderScanDisc(graphics, 79, 18, progress);
        graphics.drawCenteredString(font, "Scanning" + ".".repeat(1 + visualTicks / 4 % 3),
                windowWidth / 2, 55, 0xFFFFFFFF);
        graphics.pose().popPose();
    }

    private void renderScanDisc(GuiGraphics graphics, int x, int y, float progress) {
        double sweep = progress * Math.PI * 2.0D;
        for (int row = 0; row < 16; row++) {
            int runStart = -1;
            for (int column = 0; column <= 16; column++) {
                double angle = column < 16 ? Math.atan2(row - 7.5D, column - 7.5D) : 0.0D;
                if (angle < 0.0D) angle += Math.PI * 2.0D;
                boolean visible = column < 16 && (progress >= 1.0F || angle <= sweep);
                if (visible && runStart < 0) runStart = column;
                if (!visible && runStart >= 0) {
                    int length = column - runStart;
                    graphics.blit(SCAN, x + runStart * 2, y + row * 2, length * 2, 2,
                            runStart, row, length, 1, 16, 16);
                    runStart = -1;
                }
            }
        }
    }

    private void renderMovingWindow(GuiGraphics graphics, MovingWindow motion, int windowWidth, int windowHeight,
                                    float fit, float grow, int depth, Runnable contents) {
        int actualWidth = Math.round(windowWidth * fit);
        int actualHeight = Math.round(windowHeight * fit);
        int x = Math.max(4, Math.min(width - actualWidth - 4,
                Math.round(motion.x * (width - actualWidth)) + motion.jitterX(visualTicks)));
        int y = Math.max(4, Math.min(height - actualHeight - 4,
                Math.round(motion.y * (height - actualHeight)) + motion.jitterY(visualTicks)));
        graphics.pose().pushPose();
        graphics.pose().translate(x + actualWidth / 2.0F, y + actualHeight / 2.0F, depth);
        graphics.pose().scale(fit * grow, fit * grow, 1.0F);
        graphics.pose().translate(-windowWidth / 2.0F, -windowHeight / 2.0F, 0);
        contents.run();
        graphics.pose().popPose();
    }

    private void renderQte(GuiGraphics graphics, int x, int y, int width, float partialTick) {
        int left = x;
        int available = width;
        int box = 20;
        int keyY = y;
        for (int i = 0; i < sequence.size(); i++) {
            int keyX = left + (i % 5) * 26;
            int cellY = keyY + (i / 5) * 22;
            boolean current = i == keyIndex;
            boolean passed = i < keyIndex;
            boolean glitching = replacementGlitchTicks > 0 && i == replacementGlitchIndex;
            int color = passed ? 0xFF44110D : current ? 0xFFFF3626 : 0xFF1A1A1A;
            if (glitching) {
                keyX += ThreadLocalRandom.current().nextInt(-2, 3);
                color = 0xFFB50016;
            }
            String key = sequence.get(i);
            int cellWidth = "ALT+F4".equals(key) ? 42 : box;
            graphics.fill(keyX, cellY, keyX + cellWidth, cellY + box, 0xFFD0D0D0);
            graphics.fill(keyX + 1, cellY + 1, keyX + cellWidth - 1, cellY + box - 1, color);
            if (glitching) {
                graphics.fill(keyX + 2, cellY + 4, keyX + cellWidth - 2, cellY + 6, 0xFF070707);
                graphics.fill(keyX + 1, cellY + 10, keyX + cellWidth - 1, cellY + 12, 0xFFFFFFFF);
                graphics.fill(keyX + 1, cellY + 15, keyX + cellWidth - 1, cellY + 17, 0xFFFF3030);
            } else if ("ALT+F4".equals(key)) {
                graphics.drawCenteredString(font, "ALT+F4", keyX + cellWidth / 2, cellY + 6,
                        current ? 0xFFFFFFFF : 0xFFBBBBBB);
            } else {
                graphics.drawCenteredString(font, key, keyX + box / 2, cellY + 6,
                        current ? 0xFFFFFFFF : 0xFFBBBBBB);
            }
        }
        int max = timeForRound(sequence.size());
        int timerWidth = Math.max(0, available * roundTicks / Math.max(1, max));
        graphics.fill(left, keyY + 44, left + available, keyY + 51, 0xFFDDDDDD);
        graphics.fill(left + 1, keyY + 45, left + available - 1, keyY + 50, 0xFF1A0000);
        graphics.fill(left + 1, keyY + 45, left + Math.max(1, timerWidth - 1), keyY + 50, 0xFFE00000);
    }

    private void window(GuiGraphics graphics, int x, int y, int width, int height, String title) {
        graphics.fill(x, y, x + width, y + height, 0xFF34343B);
        graphics.fill(x + 2, y + 2, x + width - 2, y + 12, 0xFF686873);
        graphics.fill(x + 3, y + 3, x + width - 3, y + 11, 0xFF3B3B43);
        graphics.fill(x + 2, y + 12, x + width - 2, y + height - 2, 0xFFA7A7AE);
        graphics.fill(x + 4, y + 14, x + width - 4, y + height - 4, 0xFF050505);
        graphics.drawString(font, title, x + 5, y + 3, 0xFFFFFFFF, false);
    }

    private void renderTerminal(GuiGraphics graphics) {
        int first = Math.max(0, terminalLines.size() - 7);
        int line = 0;
        for (int i = first; i < terminalLines.size(); i++) {
            String value = font.plainSubstrByWidth(terminalLines.get(i), 180);
            graphics.drawString(font, value, 8, 18 + line++ * 10, 0xFFFFFFFF, false);
        }
        if (!typingLine.isEmpty() && line < 8) {
            graphics.drawString(font, font.plainSubstrByWidth(typingLine.substring(0, typingProgress), 180),
                    8, 18 + line++ * 10, 0xFFFFFFFF, false);
        }
        if (line < 8) graphics.drawString(font, "m:/chest>:_", 8, 18 + line * 10, 0xFFAAAAAA, false);
    }

    private void renderChestPreview(GuiGraphics graphics, float partialTick) {
        float open = chestLossTicks > 0
                ? Math.min(1.0F, (LOST_ITEM_TOTAL_TICKS - chestLossTicks + partialTick) / 7.0F) : 0.0F;
        previewChest.openness = open;
        graphics.flush();
        graphics.pose().pushPose();
        graphics.pose().translate(63, 57, 100);
        graphics.pose().scale(44.0F, -44.0F, 44.0F);
        graphics.pose().mulPose(Axis.XP.rotationDegrees(15));
        graphics.pose().mulPose(Axis.YP.rotationDegrees((visualTicks + partialTick) * 2.2F));
        graphics.pose().translate(-0.5F, -0.5F, -0.5F);
        minecraft.getBlockEntityRenderDispatcher().renderItem(previewChest, graphics.pose(),
                graphics.bufferSource(), 0xF000F0, OverlayTexture.NO_OVERLAY);
        graphics.flush();
        graphics.pose().popPose();
        if (chestLossTicks > 0 && !lostStack.isEmpty()) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 150);
            float age = LOST_ITEM_TOTAL_TICKS - chestLossTicks + partialTick;
            float flight = Math.min(1.0F, age / LOST_ITEM_FLIGHT_TICKS);
            float easeOut = 1.0F - (1.0F - flight) * (1.0F - flight) * (1.0F - flight);
            int yy = 50 - Math.round(24.0F * easeOut);
            int glitchAge = Math.max(0, (int) age - LOST_ITEM_FLIGHT_TICKS);
            int itemX = 55 + (glitchAge > 0 ? ThreadLocalRandom.current().nextInt(-3, 4) : 0);
            if (glitchAge == 0 || (glitchAge < 8 && visualTicks % 3 != 0))
                graphics.renderItem(lostStack, itemX, yy);
            if (glitchAge == 0) {
                graphics.drawCenteredString(font, "x" + lostCount, 63, 76, 0xFFFFD45A);
                graphics.drawCenteredString(font, font.plainSubstrByWidth(lostStack.getHoverName().getString(), 112),
                        63, 85, 0xFFFFFFFF);
            }
            if (glitchAge > 0) {
                int fragments = 2 + glitchAge / 2;
                for (int i = 0; i < fragments; i++) {
                    int stripY = yy + ThreadLocalRandom.current().nextInt(16);
                    int shift = ThreadLocalRandom.current().nextInt(-6, 7);
                    int length = 4 + ThreadLocalRandom.current().nextInt(9);
                    graphics.fill(itemX + shift, stripY, itemX + shift + length, stripY + 2,
                            i % 3 == 0 ? 0xFFFFFFFF : i % 2 == 0 ? 0xFFFF2424 : 0xFF050505);
                }
            }
            graphics.pose().popPose();
        }
    }

    private static final class FadingJumpscareSound extends AbstractTickableSoundInstance {
        private int age;

        private FadingJumpscareSound() {
            super(RansomInMinecraft.JUMPSCARE2.get(), SoundSource.MASTER, RandomSource.create());
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }

        @Override
        public void tick() {
            age++;
            volume = Math.max(0.0F, 1.0F - age / (float) WIN_JUMPSCARE_FADE_TICKS);
            if (age >= WIN_JUMPSCARE_FADE_TICKS) stop();
        }
    }

    private static final class PreviewChest extends ChestBlockEntity {
        private float openness;

        private PreviewChest() {
            super(BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
        }

        @Override
        public float getOpenNess(float partialTicks) {
            return openness;
        }
    }

    private static final class MovingWindow {
        private float x;
        private float y;
        private int nextMove;
        private final int salt;

        private MovingWindow(float x, float y, int initialDelay) {
            this.x = x;
            this.y = y;
            this.nextMove = initialDelay;
            this.salt = initialDelay;
        }

        private void tick() {
            if (--nextMove > 0) return;
            ThreadLocalRandom random = ThreadLocalRandom.current();
            x = 0.04F + random.nextFloat() * 0.88F;
            y = 0.04F + random.nextFloat() * 0.88F;
            nextMove = 80 + random.nextInt(121);
        }

        private int jitterX(int ticks) {
            return jitter(ticks / 4, salt * 31 + 7);
        }

        private int jitterY(int ticks) {
            return jitter(ticks / 4, salt * 47 + 13);
        }

        private static int jitter(int beat, int salt) {
            int value = beat * 1103515245 + salt * 12345;
            value ^= value >>> 16;
            return Math.floorMod(value, 5) - 2;
        }
    }

    private static List<String> decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(encoded.split("\\|")));
    }

    private static String keyName(int keyCode) {
        if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
            return Character.toString((char) keyCode).toUpperCase(Locale.ROOT);
        }
        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
            return Character.toString((char) keyCode);
        }
        if (keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_9) {
            return Integer.toString(keyCode - GLFW.GLFW_KEY_KP_0);
        }
        return "KEY_" + keyCode;
    }

    private static boolean isQteInputKey(int keyCode) {
        return keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z
                || keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9
                || keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_9;
    }
}
