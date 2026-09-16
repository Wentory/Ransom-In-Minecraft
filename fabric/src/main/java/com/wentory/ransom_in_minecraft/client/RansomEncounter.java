package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wentory.ransom_in_minecraft.ClientConfig;
import com.wentory.ransom_in_minecraft.RansomFabric;
import com.wentory.ransom_in_minecraft.network.RansomStatePayload;
import com.wentory.ransom_in_minecraft.network.RansomProgressPayload;
import com.wentory.ransom_in_minecraft.network.ClientRansomResumeTracker;
import com.wentory.ransom_in_minecraft.network.RansomFailureArmedPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import com.wentory.ransom_in_minecraft.mixin.InventorySelectionAccessor;
import com.wentory.ransom_in_minecraft.mixin.SoundEngineAccessor;
import com.wentory.ransom_in_minecraft.mixin.SoundManagerAccessor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * First playable version of the Ransom encounter. The encounter is deliberately
 * client-driven while its look and timing are being play-tested.
 */
public final class RansomEncounter {
    private static final Identifier RANSOM = texture("ransom.png");
    private static final Identifier BLOCK = texture("block.png");
    private static final Identifier JUMPSCARE_1 = texture("jumpscare1.png");
    private static final Identifier JUMPSCARE_2 = texture("jumpscare2.png");
    private static final Identifier POPUP_1 = texture("popup1.png");
    private static final Identifier POPUP_2 = texture("popup2.png");
    private static final Identifier WIN = texture("win.png");
    private static final Identifier INFECTED_GLITCH = texture("glitch.png");
    private static final RenderType INFECTED_GLITCH_LAYER = RansomRenderTypes.infectedGlitch(INFECTED_GLITCH);
    private static final Identifier NUGGET = Identifier.withDefaultNamespace("textures/item/gold_nugget.png");
    private static final Identifier[] WEIRD_TEXTURES = {
            Identifier.withDefaultNamespace("textures/block/command_block_front.png"),
            Identifier.withDefaultNamespace("textures/block/crying_obsidian.png"),
            Identifier.withDefaultNamespace("textures/block/nether_portal.png"),
            Identifier.withDefaultNamespace("textures/entity/end_portal.png")
    };

    private static final int WARNING_TICKS = 48;
    private static final int WARNING_TELEPORT_TICK = 8;
    private static final int WARNING_STOP_TICK = 14;
    private static final int WARNING_REACTION_GRACE_TICKS = 10;
    private static final int RANSOM_TICKS = 78 * 20;
    private static final int WIN_TICKS = 70;
    private static final int FAILED_TICKS = 20;
    private static final int RANSOM_INTRO_TICKS = 12;
    private static final int TARGET_COINS = 100;
    private static final int EXTRACTION_TICKS = 18;
    private static final int ESCAPE_SCARE_TICKS = 20;
    private static final int EMPTY_AREA_EXIT_TICKS = 50;
    private static final int MINIMUM_INFECTIONS = 12;
    private static final int WINDOW_COLOR = 0xFFFF3A24;
    private static final RandomSource RANDOM = RandomSource.create();
    private static final int[] COIN_VALUES = {5, 10, 15};
    private static int nextCoinValueIndex = COIN_VALUES.length;
    private static final List<InfectedBlock> INFECTED = new ArrayList<>();
    private static final List<Popup> POPUPS = new ArrayList<>();

    private static volatile Phase phase = Phase.IDLE;
    private static int phaseTicks;
    private static int coins;
    private static int targetCoins = TARGET_COINS;
    private static boolean respawnRansomPending;
    private static int respawnCoins;
    private static int respawnTargetCoins;
    private static int respawnPhaseTicks;
    private static boolean respawnRecoveryScare;
    private static boolean soundtrackPaused;
    private static int automaticSpawnTicks = -1;
    private static int lockedSlot;
    private static boolean forbiddenInput;
    private static double lastMouseX;
    private static double lastMouseY;
    private static BlockPos extracting;
    private static int extractionTicks;
    private static float mainWindowX = 0.5F;
    private static float mainWindowY = 0.42F;
    private static int nextWindowMove;
    private static int nextPopup;
    private static int popupSerial;
    private static int nextInfectedSpawn;
    private static int pendingFailureCoins;
    private static boolean pendingFailureDeleteHotbar;
    private static int escapeResumePhaseTicks;
    private static int suppressDeathDebtTicks;
    private static BlockPos infectionCenter;
    private static boolean infectionPreflightDone;
    private static SimpleSoundInstance soundtrack;
    private static SimpleSoundInstance jumpscareSound;
    private static Double savedMusicVolume;
    private static float warningStartX;
    private static float warningStartY;
    private static float successStartX;
    private static float successStartY;

    private RansomEncounter() {
    }

    public static void refreshNaturalSpawnTimer() {
        automaticSpawnTicks = randomNaturalSpawnDelayTicks();
    }

    /**
     * Clear only the client session when leaving a world. The server keeps the
     * encounter in that world's player data and explicitly sends it back when
     * the player rejoins the same world.
     */
    public static void handleDisconnect(Minecraft minecraft) {
        respawnCoins = 0;
        respawnTargetCoins = TARGET_COINS;
        respawnPhaseTicks = 0;
        respawnRansomPending = false;
        respawnRecoveryScare = false;
        pendingFailureCoins = 0;
        pendingFailureDeleteHotbar = false;
        phase = Phase.IDLE;
        phaseTicks = 0;
        coins = 0;
        targetCoins = TARGET_COINS;
        POPUPS.clear();
        INFECTED.clear();
        infectionCenter = null;
        extractionTicks = 0;
        extracting = null;
        stopJumpscare(minecraft);
        stopSoundtrack(minecraft);
        restoreMinecraftMusic(minecraft);
    }

    public static void clientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        var serverResume = ClientRansomResumeTracker.consume();
        if (serverResume != null) {
            respawnCoins = serverResume.coins();
            respawnTargetCoins = serverResume.targetCoins();
            respawnPhaseTicks = serverResume.phaseTicks();
            respawnRansomPending = true;
        }
        if (minecraft.player != null && ClientRansomResumeTracker.consumeSummon() && phase == Phase.IDLE) {
            startWarning(minecraft);
            return;
        }
        var failureScare = minecraft.player != null ? ClientRansomResumeTracker.consumeFailureScare() : null;
        if (failureScare != null) {
            respawnRansomPending = false;
            pendingFailureCoins = failureScare.coins();
            pendingFailureDeleteHotbar = failureScare.deleteHotbar();
            phase = Phase.FAILED;
            phaseTicks = 0;
            minecraft.setScreenAndShow(null);
            stopJumpscare(minecraft);
            jumpscareSound = SimpleSoundInstance.forUI(RansomFabric.JUMPSCARE2.get(), 1.0F, 1.0F);
            minecraft.getSoundManager().play(jumpscareSound);
            return;
        }
        if (suppressDeathDebtTicks > 0) suppressDeathDebtTicks--;
        if (minecraft.player != null && minecraft.player.isDeadOrDying()
                && (phase == Phase.RANSOM || phase == Phase.ESCAPE_SCARE)) {
            respawnCoins = coins;
            respawnTargetCoins = targetCoins + (suppressDeathDebtTicks > 0 ? 0 : 30);
            respawnPhaseTicks = phase == Phase.ESCAPE_SCARE
                    ? escapeResumePhaseTicks + phaseTicks : phaseTicks;
            respawnRansomPending = true;
            phase = Phase.IDLE;
            phaseTicks = 0;
            POPUPS.clear();
            INFECTED.clear();
            // LivingDeathEvent releases the temporary hand lock without clearing
            // the persisted ransom.  The event must survive the death screen.
            if (soundtrack != null && !soundtrackPaused) {
                pauseSoundtrack(minecraft);
                soundtrackPaused = true;
            }
            return;
        }
        if (minecraft.player == null || minecraft.level == null) {
            reset();
            return;
        }
        if (minecraft.isPaused()) return;

        if (phase == Phase.IDLE && respawnRansomPending && !minecraft.player.isDeadOrDying()) {
            respawnRansomPending = false;
            targetCoins = respawnTargetCoins;
            coins = respawnCoins;
            respawnRecoveryScare = true;
            phase = Phase.JUMPSCARE;
            phaseTicks = 0;
            minecraft.setScreenAndShow(null);
            setServerRansomState(minecraft, true, false, 0);
            jumpscareSound = SimpleSoundInstance.forUI(RansomFabric.JUMPSCARE.get(), 1.0F, 1.0F);
            minecraft.getSoundManager().play(jumpscareSound);
            return;
        }

        if (phase == Phase.IDLE) {
            if (automaticSpawnTicks < 0) automaticSpawnTicks = randomNaturalSpawnDelayTicks();
            if (--automaticSpawnTicks <= 0) startWarning(minecraft);
            return;
        }

        phaseTicks++;
        switch (phase) {
            case WARNING -> tickWarning(minecraft);
            case JUMPSCARE -> {
                if (respawnRecoveryScare && phaseTicks >= jumpscareEndTick()) {
                    respawnRecoveryScare = false;
                    stopJumpscare(minecraft);
                    if (soundtrack != null && soundtrackPaused) {
                        resumeSoundtrack(minecraft);
                        soundtrackPaused = false;
                    }
                    startRansom(minecraft, true);
                } else if (!respawnRecoveryScare && !infectionPreflightDone
                        && phaseTicks >= jumpscareEndTick()) {
                    infectionPreflightDone = true;
                    infectNearbyBlocks(minecraft);
                    if (!hasViableInfection()) startEmptyAreaExit(minecraft);
                } else if (!respawnRecoveryScare && phaseTicks >= downloadEndTick()) {
                    startRansom(minecraft);
                }
            }
            case RANSOM -> tickRansom(minecraft);
            case ESCAPE_SCARE -> {
                if (phaseTicks >= ESCAPE_SCARE_TICKS) finishEscapePenalty(minecraft);
            }
            case EMPTY_AREA_EXIT -> {
                if (phaseTicks >= EMPTY_AREA_EXIT_TICKS) reset();
            }
            case WIN, FAILED -> {
                int endingTicks = phase == Phase.FAILED ? FAILED_TICKS : WIN_TICKS;
                if (phaseTicks >= endingTicks) {
                    if (phase == Phase.FAILED) {
                        setServerRansomState(minecraft, false, true, pendingFailureCoins, 0,
                                pendingFailureDeleteHotbar);
                    }
                    reset();
                }
            }
            default -> {
            }
        }
    }

    private static void tickWarning(Minecraft minecraft) {
        double mouseX = minecraft.mouseHandler.xpos();
        double mouseY = minecraft.mouseHandler.ypos();
        boolean mouseMoved = Math.abs(mouseX - lastMouseX) > 0.01D
                || Math.abs(mouseY - lastMouseY) > 0.01D;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        int movementLockTick = WARNING_STOP_TICK + WARNING_REACTION_GRACE_TICKS;
        if (phaseTicks >= movementLockTick && (forbiddenInput || mouseMoved || stopInputHeld(minecraft))) {
            startJumpscare(minecraft);
        } else if (phaseTicks >= WARNING_TICKS) {
            reset();
        }
        forbiddenInput = false;
    }

    private static void tickRansom(Minecraft minecraft) {
        // Index 9 is deliberately outside the nine real hotbar slots. Inventory#getSelected
        // therefore exposes an empty main hand without moving or replacing any item stack.
        ((InventorySelectionAccessor) minecraft.player.getInventory()).ransom$setSelectedSlot(Inventory.getSelectionSize());
        ClientPlayNetworking.send(new RansomProgressPayload(coins, targetCoins, phaseTicks));

        int infectionRadius = infectionRadius();
        if (infectionCenter != null && minecraft.player.position().distanceToSqr(Vec3.atCenterOf(infectionCenter))
                > (double) infectionRadius * infectionRadius) {
            escapeResumePhaseTicks = phaseTicks;
            phase = Phase.ESCAPE_SCARE;
            phaseTicks = 0;
            minecraft.setScreenAndShow(null);
            stopJumpscare(minecraft);
            jumpscareSound = SimpleSoundInstance.forUI(RansomFabric.JUMPSCARE.get(), 1.0F, 1.0F);
            minecraft.getSoundManager().play(jumpscareSound);
            return;
        }

        if (phaseTicks >= RANSOM_TICKS) {
            pendingFailureCoins = coins;
            pendingFailureDeleteHotbar = ClientConfig.DELETE_HOTBAR_ON_FAILURE.get();
            phase = Phase.FAILED;
            phaseTicks = 0;
            minecraft.setScreenAndShow(null);
            ClientPlayNetworking.send(new RansomFailureArmedPayload(
                    coins, ClientConfig.DELETE_HOTBAR_ON_FAILURE.get()));
            jumpscareSound = SimpleSoundInstance.forUI(RansomFabric.JUMPSCARE2.get(), 1.0F, 1.0F);
            minecraft.getSoundManager().play(jumpscareSound);
            return;
        }

        collectBrokenBlocks(minecraft);
        if (phase != Phase.RANSOM) return;
        if (--nextInfectedSpawn <= 0) {
            spawnAdditionalInfected(minecraft);
            nextInfectedSpawn = 15 * 20;
        }
        POPUPS.removeIf(popup -> phaseTicks - popup.bornTick >= popup.lifetime);
        if (--nextPopup <= 0) {
            if (POPUPS.size() < 3) spawnPopup(false);
            nextPopup = 55 + RANDOM.nextInt(91);
        }
        if (--nextWindowMove <= 0) {
            mainWindowX = 0.08F + RANDOM.nextFloat() * 0.84F;
            mainWindowY = 0.08F + RANDOM.nextFloat() * 0.68F;
            nextWindowMove = 80 + RANDOM.nextInt(121);
        }
        tickExtraction(minecraft);
    }

    private static void tickExtraction(Minecraft minecraft) {
        BlockPos target = null;
        if (minecraft.hitResult instanceof BlockHitResult blockHit
                && minecraft.hitResult.getType() == HitResult.Type.BLOCK
                && isInfected(blockHit.getBlockPos())
                && minecraft.options.keyUse.isDown()) {
            target = blockHit.getBlockPos();
        }

        if (target == null) {
            extracting = null;
            extractionTicks = 0;
            return;
        }
        if (!target.equals(extracting)) {
            extracting = target;
            extractionTicks = 0;
        }
        extractionTicks++;
        minecraft.gui.hud.setOverlayMessage(Component.translatable("ransom.hold",
                Math.min(100, extractionTicks * 100 / EXTRACTION_TICKS)), false);
        if (extractionTicks < EXTRACTION_TICKS) return;

        collectCoinsAt(minecraft, target);
        extracting = null;
        extractionTicks = 0;
    }

    private static void collectBrokenBlocks(Minecraft minecraft) {
        BlockPos playerPos = minecraft.player.blockPosition();
        List<BlockPos> broken = INFECTED.stream()
                .map(InfectedBlock::pos)
                // An unloaded client chunk looks like air. Never interpret that as the player
                // breaking the block, and only award a break that happened within reach.
                .filter(pos -> minecraft.level.hasChunkAt(pos))
                .filter(pos -> pos.distSqr(playerPos) <= 64.0D)
                .filter(pos -> minecraft.level.getBlockState(pos).isAir())
                .toList();
        for (BlockPos pos : broken) {
            if (phase != Phase.RANSOM) break;
            collectCoinsAt(minecraft, pos);
        }
    }

    private static void collectCoinsAt(Minecraft minecraft, BlockPos pos) {
        Iterator<InfectedBlock> iterator = INFECTED.iterator();
        while (iterator.hasNext()) {
            InfectedBlock infected = iterator.next();
            if (!infected.pos.equals(pos)) continue;
            coins += infected.value;
            minecraft.gui.hud.setOverlayMessage(Component.translatable("ransom.found", infected.value), false);
            play(minecraft, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.45F, 1.65F);
            iterator.remove();
            break;
        }
        if (coins >= targetCoins) {
            stopSoundtrack(minecraft);
            restoreMinecraftMusic(minecraft);
            setServerRansomState(minecraft, false, false, 0);
            if (!Inventory.isHotbarSlot(minecraft.player.getInventory().getSelectedSlot())) {
                minecraft.player.getInventory().setSelectedSlot(lockedSlot);
            }
            successStartX = mainWindowX;
            successStartY = mainWindowY;
            phase = Phase.WIN;
            phaseTicks = 0;
            play(minecraft, RansomFabric.SUCCESS.get(), 1.0F, 1.0F);
        }
    }

    private static void startWarning(Minecraft minecraft) {
        phase = Phase.WARNING;
        phaseTicks = 0;
        forbiddenInput = false;
        coins = 0;
        targetCoins = TARGET_COINS;
        POPUPS.clear();
        INFECTED.clear();
        infectionCenter = null;
        infectionPreflightDone = false;
        lockedSlot = minecraft.player.getInventory().getSelectedSlot();
        warningStartX = 0.08F + RANDOM.nextFloat() * 0.84F;
        warningStartY = 0.08F + RANDOM.nextFloat() * 0.70F;
        lastMouseX = minecraft.mouseHandler.xpos();
        lastMouseY = minecraft.mouseHandler.ypos();
        ClientPlayNetworking.send(new RansomProgressPayload(coins, targetCoins, 0));
        play(minecraft, RansomFabric.SPAWN.get(), 1.0F, 1.0F);
    }

    private static void startJumpscare(Minecraft minecraft) {
        phase = Phase.JUMPSCARE;
        phaseTicks = 0;
        minecraft.setScreenAndShow(null);
        ClientPlayNetworking.send(new RansomProgressPayload(coins, targetCoins, 0));
        stopJumpscare(minecraft);
        jumpscareSound = SimpleSoundInstance.forUI(RansomFabric.JUMPSCARE.get(), 1.0F, 1.0F);
        minecraft.getSoundManager().play(jumpscareSound);
    }

    private static void startEmptyAreaExit(Minecraft minecraft) {
        phase = Phase.EMPTY_AREA_EXIT;
        phaseTicks = 0;
        INFECTED.clear();
        infectionCenter = null;
        stopJumpscare(minecraft);
        setServerRansomState(minecraft, false, false, 0);
        play(minecraft, RansomFabric.BOWOMP.get(), 1.0F, 1.0F);
    }

    private static void startRansom(Minecraft minecraft) {
        startRansom(minecraft, false);
    }

    private static void startRansom(Minecraft minecraft, boolean resume) {
        phase = Phase.RANSOM;
        muteMinecraftMusic(minecraft);
        if (resume) {
            phaseTicks = respawnPhaseTicks;
        } else {
            phaseTicks = 0;
            coins = 0;
        }
        POPUPS.clear();
        popupSerial = 0;
        for (int i = 0; i < 6; i++) spawnPopup(true);
        nextPopup = 55 + RANDOM.nextInt(46);
        mainWindowX = 0.5F;
        mainWindowY = 0.42F;
        nextWindowMove = 80 + RANDOM.nextInt(81);
        nextInfectedSpawn = 15 * 20;
        if (resume || INFECTED.isEmpty()) infectNearbyBlocks(minecraft);
        if (!resume || soundtrack == null) {
            soundtrack = new SimpleSoundInstance(
                    RansomFabric.SOUNDTRACK.get().location(), SoundSource.MASTER, 0.8F, 1.0F,
                    RANDOM, false, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true);
            minecraft.getSoundManager().play(soundtrack);
            soundtrackPaused = false;
        }
        setServerRansomState(minecraft, true, false, 0);
        ClientPlayNetworking.send(new RansomProgressPayload(coins, targetCoins, phaseTicks));
    }

    private static void finishEscapePenalty(Minecraft minecraft) {
        stopJumpscare(minecraft);
        targetCoins += 30;
        phase = Phase.RANSOM;
        phaseTicks = escapeResumePhaseTicks + ESCAPE_SCARE_TICKS;
        suppressDeathDebtTicks = 40;
        setServerRansomState(minecraft, true, false, coins, 4);
        infectNearbyBlocks(minecraft);
    }

    private static void reset() {
        Phase previousPhase = phase;
        Minecraft minecraft = Minecraft.getInstance();
        setServerRansomState(minecraft, false, false, 0);
        if (minecraft.player != null && !Inventory.isHotbarSlot(minecraft.player.getInventory().getSelectedSlot())) {
            minecraft.player.getInventory().setSelectedSlot(lockedSlot);
        }
        phase = Phase.IDLE;
        phaseTicks = 0;
        coins = 0;
        POPUPS.clear();
        extractionTicks = 0;
        extracting = null;
        INFECTED.clear();
        infectionCenter = null;
        if (previousPhase != Phase.FAILED) stopJumpscare(Minecraft.getInstance());
        if (!respawnRansomPending) stopSoundtrack(Minecraft.getInstance());
        restoreMinecraftMusic(minecraft);
        automaticSpawnTicks = randomNaturalSpawnDelayTicks();
    }

    private static void infectNearbyBlocks(Minecraft minecraft) {
        INFECTED.clear();
        infectionCenter = minecraft.player.blockPosition().immutable();
        for (int i = 0; i < 48; i++) spawnAdditionalInfected(minecraft);
    }

    private static void spawnAdditionalInfected(Minecraft minecraft) {
        if (INFECTED.size() >= 48) return;
        BlockPos center = infectionCenter != null ? infectionCenter : minecraft.player.blockPosition();
        int radius = infectionRadius();
        int below = ClientConfig.INFECTION_VERTICAL_BELOW.get();
        int above = ClientConfig.INFECTION_VERTICAL_ABOVE.get();
        for (int attempt = 0; attempt < 500; attempt++) {
            int dx = RANDOM.nextInt(radius * 2 + 1) - radius;
            int dz = RANDOM.nextInt(radius * 2 + 1) - radius;
            int dy = RANDOM.nextInt(below + above + 1) - below;
            BlockPos pos = center.offset(dx, dy, dz);
            if (pos.distSqr(center) < 9 || pos.distSqr(center) > (double) radius * radius || isInfected(pos)) continue;
            BlockState state = minecraft.level.getBlockState(pos);
            if (state.isAir() || state.getDestroySpeed(minecraft.level, pos) < 0
                    || !state.getFluidState().isEmpty() || state.getRenderShape() == RenderShape.INVISIBLE
                    || !net.minecraft.world.level.block.Block.isShapeFullBlock(state.getCollisionShape(minecraft.level, pos))) continue;
            boolean exposed = false;
            for (Direction direction : Direction.values()) {
                if (minecraft.level.getBlockState(pos.relative(direction)).isAir()) {
                    exposed = true;
                    break;
                }
            }
            if (!exposed) continue;
            INFECTED.add(new InfectedBlock(pos.immutable(), nextCoinValue()));
            return;
        }
    }

    private static int nextCoinValue() {
        if (nextCoinValueIndex == COIN_VALUES.length) {
            for (int i = COIN_VALUES.length - 1; i > 0; i--) {
                int swap = RANDOM.nextInt(i + 1);
                int value = COIN_VALUES[i];
                COIN_VALUES[i] = COIN_VALUES[swap];
                COIN_VALUES[swap] = value;
            }
            nextCoinValueIndex = 0;
        }
        return COIN_VALUES[nextCoinValueIndex++];
    }

    private static boolean isInfected(BlockPos pos) {
        return INFECTED.stream().anyMatch(block -> block.pos.equals(pos));
    }

    private static boolean hasViableInfection() {
        if (INFECTED.size() < MINIMUM_INFECTIONS) return false;
        int availableCoins = INFECTED.stream().mapToInt(InfectedBlock::value).sum();
        return availableCoins >= Math.max(1, targetCoins - coins);
    }


    public static void keyInput(int key, int action) {
        if (phase == Phase.WARNING && action == GLFW.GLFW_PRESS
                && key != GLFW.GLFW_KEY_ESCAPE) forbiddenInput = true;
    }

    public static void mouseInput(int action) {
        if (phase == Phase.WARNING && action == GLFW.GLFW_PRESS) forbiddenInput = true;
    }

    public static boolean shouldBlockScroll() {
        return handsLocked();
    }

    public static void renderInventoryHotbarLock(Screen screen, GuiGraphicsExtractor graphics) {
        if (handsLocked() && screen instanceof InventoryScreen) {
            int left = (screen.width - 176) / 2;
            int top = (screen.height - 166) / 2;
            graphics.pose().pushMatrix();
            graphics.nextStratum();
            for (int slot = 0; slot < 9; slot++) {
                int x = left + 8 + slot * 18;
                int y = top + 142;
                graphics.fill(x, y, x + 16, y + 16, 0x78C00000);
                graphics.fill(x, y, x + 16, y + 1, 0xFFFF2525);
                graphics.fill(x, y + 15, x + 16, y + 16, 0xFFFF2525);
                graphics.fill(x, y, x + 1, y + 16, 0xFFFF2525);
                graphics.fill(x + 15, y, x + 16, y + 16, 0xFFFF2525);
                blitScaled(graphics, BLOCK, x + 2, y + 2, 12, 12, 200, 200);
            }
            graphics.pose().popMatrix();
        }
        renderEncounterOverlay(graphics);
    }

    public static boolean shouldBlockPickBlock() {
        return handsLocked();
    }

    public static boolean shouldHideHeldItem() {
        return handsLocked();
    }

    public static void renderInfectedBlocks(LevelRenderContext context) {
        if (phase != Phase.RANSOM || INFECTED.isEmpty()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        PoseStack poseStack = context.poseStack();
        Vec3 camera = context.levelState().cameraRenderState.pos;
        int glitchFrame = (phaseTicks / 2) % 6;

        for (InfectedBlock infected : INFECTED) {
            BlockState state = minecraft.level.getBlockState(infected.pos);
            if (net.minecraft.world.level.block.Block.isShapeFullBlock(state.getCollisionShape(minecraft.level, infected.pos))) {
                List<BlockStateModelPart> parts = new ArrayList<>();
                minecraft.getModelManager().getBlockStateModelSet().get(state)
                        .collectParts(RandomSource.create(state.getSeed(infected.pos)), parts);
                List<BakedQuad> faces = new ArrayList<>();
                for (Direction direction : Direction.values()) {
                    if (!minecraft.level.getBlockState(infected.pos.relative(direction)).isAir()) continue;
                    for (BlockStateModelPart part : parts) faces.addAll(part.getQuads(direction));
                }
                if (faces.isEmpty()) continue;
                poseStack.pushPose();
                poseStack.translate(
                        infected.pos.getX() - camera.x,
                        infected.pos.getY() - camera.y,
                        infected.pos.getZ() - camera.z);
                context.submitNodeCollector().submitCustomGeometry(poseStack, INFECTED_GLITCH_LAYER,
                        (pose, vertices) -> drawInfectedFaces(pose, vertices, faces, glitchFrame));
                poseStack.popPose();
            }
        }
    }

    private static void drawInfectedFaces(PoseStack.Pose pose, VertexConsumer vertices,
                                          List<BakedQuad> faces, int frame) {
        VertexConsumer animated = new AnimatedSheetVertexConsumer(vertices, frame, 6, 128);
        for (BakedQuad face : faces) {
            Direction side = face.direction();
            for (int i = 0; i < 4; i++) {
                var point = face.position(i);
                float x = (point.x() - 0.5F) * 1.003F + 0.5F;
                float y = (point.y() - 0.5F) * 1.003F + 0.5F;
                float z = (point.z() - 0.5F) * 1.003F + 0.5F;
                float u = side.getAxis() == Direction.Axis.X ? z : x;
                float v = side.getAxis() == Direction.Axis.Y ? z : 1.0F - y;
                animated.addVertex(pose, x, y, z)
                        .setColor(255, 255, 255, 255)
                        .setUv(u, v)
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(0x00F000F0)
                        .setNormal(pose, side.getStepX(), side.getStepY(), side.getStepZ());
            }
        }
    }

    public static void renderHud(GuiGraphicsExtractor graphics) {
        if (Minecraft.getInstance().gui.screen() != null) return;
        renderEncounterOverlay(graphics);
    }

    private static void renderEncounterOverlay(GuiGraphicsExtractor graphics) {
        if (phase == Phase.IDLE) return;
        Minecraft minecraft = Minecraft.getInstance();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        graphics.pose().pushMatrix();
        graphics.nextStratum();
        switch (phase) {
            case WARNING -> renderWarning(graphics, width, height);
            case JUMPSCARE -> renderInfectionSequence(graphics, minecraft.font, width, height);
            case RANSOM -> renderRansom(graphics, minecraft.font, width, height);
            case ESCAPE_SCARE -> renderJumpscare(graphics, width, height, true);
            case EMPTY_AREA_EXIT -> renderEmptyAreaExit(graphics, width, height);
            case WIN -> renderThanksWindow(graphics, width, height);
            case FAILED -> renderJumpscare(graphics, width, height, true);
            default -> {
            }
        }
        graphics.pose().popMatrix();
    }

    private static void renderWarning(GuiGraphicsExtractor graphics, int width, int height) {
        int size = Math.min(width, height) * 2 / 5;
        Identifier image = phaseTicks < WARNING_STOP_TICK ? RANSOM : BLOCK;
        int x;
        int y;
        if (phaseTicks < WARNING_TELEPORT_TICK) {
            x = (int) (warningStartX * (width - size));
            y = (int) (warningStartY * (height - size - 24));
        } else {
            x = (width - size) / 2;
            y = (height - size - 24) / 2;
        }
        blitScaled(graphics, image, x, y, size, size, 200, 200);
    }

    private static void renderEmptyAreaExit(GuiGraphicsExtractor graphics, int width, int height) {
        int size = Math.min(width, height) * 2 / 5;
        int waitTicks = 20;
        float progress = Mth.clamp((phaseTicks - waitTicks) / (float) (EMPTY_AREA_EXIT_TICKS - waitTicks), 0.0F, 1.0F);
        float eased = progress * progress * (3.0F - 2.0F * progress);
        int startX = (width - size) / 2;
        int x = Math.round(Mth.lerp(eased, startX, -size - 4));
        int y = (height - size - 24) / 2;
        blitScaled(graphics, RANSOM, x, y, size, size, 200, 200);
    }

    private static void renderJumpscare(GuiGraphicsExtractor graphics, int width, int height) {
        renderJumpscare(graphics, width, height, false);
    }

    private static void renderJumpscare(GuiGraphicsExtractor graphics, int width, int height, boolean forceLarge) {
        renderNoise(graphics, width, height, true);
        boolean firstFrame = !forceLarge && phaseTicks < 6;
        Identifier texture = firstFrame ? JUMPSCARE_1 : JUMPSCARE_2;
        int textureWidth = texture == JUMPSCARE_1 ? 200 : 564;
        int textureHeight = texture == JUMPSCARE_1 ? 200 : 647;
        int availableHeight = height - 26;
        int maxWidth = firstFrame ? width * 2 / 5 : width * 94 / 100;
        int maxHeight = firstFrame ? availableHeight * 2 / 5 : availableHeight * 94 / 100;
        int drawWidth = Math.min(maxWidth, maxHeight * textureWidth / textureHeight);
        int drawHeight = drawWidth * textureHeight / textureWidth;
        int shakeX = RANDOM.nextInt(5) - 2;
        int shakeY = RANDOM.nextInt(5) - 2;
        blitScaled(graphics, texture, (width - drawWidth) / 2 + shakeX, (availableHeight - drawHeight) / 2 + shakeY,
                drawWidth, drawHeight, textureWidth, textureHeight);
        renderLockedHotbar(graphics, width, height);
    }

    private static void renderInfectionSequence(GuiGraphicsExtractor graphics, Font font, int width, int height) {
        if (phaseTicks < jumpscareEndTick()) {
            renderJumpscare(graphics, width, height);
        } else if (phaseTicks >= downloadStartTick() && phaseTicks < downloadEndTick()) {
            renderDownload(graphics, font, width, height);
        }
    }

    private static void renderDownload(GuiGraphicsExtractor graphics, Font font, int width, int height) {
        renderNoise(graphics, width, height, true);
        int playHeight = height - 24;
        int panelWidth = Math.min(280, width - 24);
        int panelHeight = 58;
        int x = (width - panelWidth) / 2;
        int y = (playHeight - panelHeight) / 2;
        int shakeX = RANDOM.nextInt(5) - 2;
        int shakeY = RANDOM.nextInt(5) - 2;
        graphics.pose().pushMatrix();
        graphics.pose().translate(shakeX, shakeY);
        drawCentered(graphics, font, Component.literal("DOWNLOADING..."), width / 2, y + 16, 0xFFFFFFFF, 1.35F);

        int barX = x + 14;
        int barY = y + 34;
        int barWidth = panelWidth - 28;
        int barHeight = 13;
        graphics.fill(barX - 2, barY - 2, barX + barWidth + 2, barY + barHeight + 2, 0xFF5A0909);
        graphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF050505);
        int duration = Math.max(1, downloadEndTick() - downloadStartTick());
        float progress = Mth.clamp((phaseTicks - downloadStartTick()) / (float) duration, 0.0F, 1.0F);
        int filledSegments = Math.min(10, (int) Math.ceil(progress * 10.0F));
        int gap = 2;
        int segmentWidth = (barWidth - gap * 9) / 10;
        for (int segment = 0; segment < 10; segment++) {
            int segmentX = barX + segment * (segmentWidth + gap);
            int segmentRight = segment == 9 ? barX + barWidth : segmentX + segmentWidth;
            if (segment < filledSegments) {
                graphics.fill(segmentX, barY, segmentRight, barY + barHeight, 0xFFFF4A4A);
            }
        }
        String hint = "HOLD RMB ON INFECTED BLOCKS TO COLLECT COINS";
        float hintScale = Math.min(0.72F, (barWidth - 4.0F) / font.width(hint));
        drawCentered(graphics, font, Component.literal(hint), width / 2,
                barY + barHeight + 8, 0xFF090909, hintScale);
        graphics.pose().popMatrix();
        renderLockedHotbar(graphics, width, height);
    }

    private static void renderRansom(GuiGraphicsExtractor graphics, Font font, int width, int height) {
        float zoneIntensity = zoneEdgeIntensity(Minecraft.getInstance());
        if (phaseTicks < RANSOM_INTRO_TICKS) {
            int color = phaseTicks < 4 || phaseTicks >= 8 ? 0xFFF02020 : 0xFFFFFFFF;
            graphics.fill(0, 0, width, height, color);
        } else {
            renderNoise(graphics, width, height, false, zoneIntensity);
        }
        renderZoneBoundaryWarning(graphics, font, width, height, zoneIntensity);
        renderPopups(graphics, width, height);
        

        int windowWidth = Math.min(152, width - 24);
        int windowHeight = 112;
        int x = Mth.clamp((int) (mainWindowX * (width - windowWidth)), 4, Math.max(4, width - windowWidth - 4));
        int y = Mth.clamp((int) (mainWindowY * (height - windowHeight - 26)), 4, Math.max(4, height - windowHeight - 26));
        int windowSecond = phaseTicks / 4;
        x = Mth.clamp(x + windowJitterX(windowSecond, 101), 4, Math.max(4, width - windowWidth - 4));
        y = Mth.clamp(y + windowJitterY(windowSecond, 137), 4, Math.max(4, height - windowHeight - 26));
        graphics.pose().pushMatrix();
        graphics.nextStratum();
        window(graphics, x, y, windowWidth, windowHeight, "RANSOM");
        graphics.fill(x + 3, y + 12, x + windowWidth - 3, y + windowHeight - 3, WINDOW_COLOR);
        float headlineScale = 1.0F;
        int headlineWidth = Math.max(font.width("YOUR HOTBAR"),
                Math.max(font.width("HAVE BEEN"), font.width("ENCRYPTED")));
        int headerGroupWidth = 38 + 4 + Math.round(headlineWidth * headlineScale);
        int headerGroupX = x + (windowWidth - headerGroupWidth) / 2;
        blitScaled(graphics, RANSOM, headerGroupX, y + 14, 38, 38, 200, 200);
        int headlineX = headerGroupX + 42;
        drawScaledString(graphics, font, "YOUR HOTBAR", headlineX, y + 18, 0xFFFFFFFF, headlineScale, true);
        drawScaledString(graphics, font, "HAVE BEEN", headlineX, y + 28, 0xFFFFFFFF, headlineScale, true);
        drawScaledString(graphics, font, "ENCRYPTED", headlineX, y + 38, 0xFFFFFFFF, headlineScale, true);
        graphics.fill(x + 6, y + 52, x + windowWidth - 6, y + 80, 0xFFFFFFFF);
        graphics.fill(x + 7, y + 53, x + windowWidth - 7, y + 79, 0xFF000000);
        float demandScale = 0.74F;
        String demandLineOne = "FIND INFECTED BLOCKS";
        String demandLineTwo = "RETURN MY COINS";
        int demandCenter = x + windowWidth / 2;
        drawScaledString(graphics, font, demandLineOne,
                demandCenter - Math.round(font.width(demandLineOne) * demandScale / 2.0F), y + 55,
                0xFFFFFFFF, demandScale, false);
        drawScaledString(graphics, font, demandLineTwo,
                demandCenter - Math.round(font.width(demandLineTwo) * demandScale / 2.0F), y + 63,
                0xFFFFFFFF, demandScale, false);
        String threatPrefix = "OR YOUR ITEMS WILL BE ";
        String threatEnding = "DESTROYED";
        int threatWidth = Math.round((font.width(threatPrefix) + font.width(threatEnding)) * demandScale);
        int threatX = demandCenter - threatWidth / 2;
        drawScaledString(graphics, font, threatPrefix, threatX, y + 71,
                0xFFFFFFFF, demandScale, false);
        int destroyedX = threatX + Math.round(font.width(threatPrefix) * demandScale);
        drawScaledString(graphics, font, threatEnding, destroyedX, y + 71,
                0xFFFF2020, demandScale, false);

        int remaining = Math.max(0, targetCoins - coins);
        int bottomY = y + 85;
        int bottomEdge = y + 108;
        int split = x + 63;
        graphics.fill(x + 7, bottomY, split, bottomEdge, 0xFFFFD83D);
        graphics.fill(x + 8, bottomY + 1, split - 1, bottomEdge - 1, 0xFF090909);
        blitScaled(graphics, NUGGET, x + 13, bottomY + 5, 12, 12, 16, 16);
        graphics.text(font, Integer.toString(remaining), x + 30, bottomY + 8, 0xFFFFD83D, true);
        graphics.fill(split + 4, bottomY, x + windowWidth - 7, bottomEdge, 0xFFFFFFFF);
        graphics.fill(split + 5, bottomY + 1, x + windowWidth - 8, bottomEdge - 1, 0xFFB31318);
        int seconds = Math.max(0, (RANSOM_TICKS - phaseTicks + 19) / 20);
        graphics.text(font, String.format("TIME %02d:%02d", seconds / 60, seconds % 60),
                split + 9, bottomY + 8, 0xFF000000, true);

        renderLockedHotbar(graphics, width, height);
        
        graphics.pose().popMatrix();
    }

    private static void renderPopups(GuiGraphicsExtractor graphics, int width, int height) {
        for (int layer = 0; layer < POPUPS.size(); layer++) {
            Popup popup = POPUPS.get(layer);
            int fullWidth = popup.width;
            int fullHeight = popup.height;
            int age = phaseTicks - popup.bornTick;
            int remaining = popup.lifetime - age;
            int animationTicks = 6;
            float opening = Mth.clamp(age / (float) animationTicks, 0.0F, 1.0F);
            float closing = Mth.clamp(remaining / (float) animationTicks, 0.0F, 1.0F);
            float scale = Math.min(opening, closing);
            if (scale <= 0.02F) continue;

            int fullX = (int) (popup.x * Math.max(1, width - fullWidth));
            int fullY = (int) (popup.y * Math.max(1, height - fullHeight - 25));
            int popupWidth = Math.max(12, Math.round(fullWidth * scale));
            int popupHeight = Math.max(18, Math.round(fullHeight * scale));
            int x = fullX + (fullWidth - popupWidth) / 2;
            int y = fullY + (fullHeight - popupHeight) / 2;
            int windowSecond = phaseTicks / 4;
            x += windowJitterX(windowSecond, layer * 17 + popup.variant);
            y += windowJitterY(windowSecond, layer * 31 + popup.variant);
            boolean whiteTransition = opening < 1.0F || closing < 1.0F;

            graphics.pose().pushMatrix();
            graphics.nextStratum();
            window(graphics, x, y, popupWidth, popupHeight, popupTitle(popup.variant));
            graphics.enableScissor(x + 3, y + 12, x + popupWidth - 3, y + popupHeight - 3);
            if (popupHeight > 20) {
                graphics.fill(x + 3, y + 12, x + popupWidth - 3, y + popupHeight - 3,
                        whiteTransition ? 0xFFFFFFFF : WINDOW_COLOR);
            }
            if (!whiteTransition && popup.variant == 0) {
                blitScaled(graphics, POPUP_1, x + 3, y + 12, popupWidth - 6, popupHeight - 15, 200, 165);
            } else if (!whiteTransition && popup.variant == 1) {
                blitScaled(graphics, POPUP_2, x + 3, y + 12, popupWidth - 6, popupHeight - 15, 200, 165);
            } else if (!whiteTransition && popup.variant >= 3) {
                Identifier texture = WEIRD_TEXTURES[popup.variant % WEIRD_TEXTURES.length];
                int size = Math.min(popupWidth - 8, popupHeight - 17);
                if (size > 0) {
                    blitScaled(graphics, texture, x + (popupWidth - size) / 2, y + 14, size, size, 16, 16);
                }
            }
            graphics.disableScissor();
            
            graphics.pose().popMatrix();
        }
    }

    private static void renderLockedHotbar(GuiGraphicsExtractor graphics, int width, int height) {
        if (Minecraft.getInstance().gui.hud.isHidden()) return;
        int left = width / 2 - 91;
        int top = height - 22;
        graphics.pose().pushMatrix();
        graphics.nextStratum();
        for (int slot = 0; slot < 9; slot++) {
            int x = left + 1 + slot * 20;
            graphics.fill(x, top, x + 19, top + 21, 0x99C00000);
            graphics.fill(x, top, x + 19, top + 1, 0xFFFF2525);
            graphics.fill(x, top + 20, x + 19, top + 21, 0xFFFF2525);
            graphics.fill(x, top, x + 1, top + 21, 0xFFFF2525);
            graphics.fill(x + 18, top, x + 19, top + 21, 0xFFFF2525);
            blitScaled(graphics, BLOCK, x + 2, top + 3, 15, 15, 200, 200);
        }
        graphics.pose().popMatrix();
    }

    private static void renderNoise(GuiGraphicsExtractor graphics, int width, int height, boolean fullScreen) {
        renderNoise(graphics, width, height, fullScreen, 0.0F);
    }

    private static void renderNoise(GuiGraphicsExtractor graphics, int width, int height, boolean fullScreen, float intensity) {
        int playHeight = height;
        if (fullScreen) {
            graphics.fill(0, 0, width, playHeight, 0xD06C0000);
        } else {
            int fade = Math.max(10, Math.min(width, playHeight) / 18)
                    + Math.round(intensity * Math.min(width, playHeight) / 12.0F);
            for (int inset = 0; inset < fade; inset++) {
                float strength = 1.0F - inset / (float) fade;
                int alpha = Mth.clamp(Math.round((105.0F + 120.0F * intensity) * strength * strength), 0, 255);
                int color = alpha << 24 | 0x00A00000;
                graphics.fill(inset, inset, width - inset, inset + 1, color);
                graphics.fill(inset, playHeight - inset - 1, width - inset, playHeight - inset, color);
                graphics.fill(inset, inset + 1, inset + 1, playHeight - inset - 1, color);
                graphics.fill(width - inset - 1, inset + 1, width - inset, playHeight - inset - 1, color);
            }
        }
        int border = fullScreen ? Math.max(width, height)
                : Math.max(18, Math.min(width, height) / 9) + Math.round(intensity * Math.min(width, height) / 12.0F);
        int count = fullScreen ? 280 : 140 + Math.round(160.0F * intensity);
        for (int i = 0; i < count; i++) {
            int x;
            int y;
            if (fullScreen || RANDOM.nextBoolean()) {
                x = RANDOM.nextInt(Math.max(1, width));
                y = fullScreen ? RANDOM.nextInt(playHeight)
                        : (RANDOM.nextBoolean() ? RANDOM.nextInt(Math.min(border, playHeight)) : playHeight - RANDOM.nextInt(Math.min(border, playHeight)) - 1);
            } else {
                x = RANDOM.nextBoolean() ? RANDOM.nextInt(Math.min(border, width)) : width - RANDOM.nextInt(Math.min(border, width)) - 1;
                y = RANDOM.nextInt(playHeight);
            }
            int color = switch (RANDOM.nextInt(5)) {
                case 0 -> 0xAAFFFFFF;
                case 1 -> 0xCC160000;
                default -> 0xBBFF0000;
            };
            graphics.fill(x, y, Math.min(width, x + 2 + RANDOM.nextInt(13)), Math.min(playHeight, y + 1 + RANDOM.nextInt(4)), color);
        }
    }

    private static float zoneEdgeIntensity(Minecraft minecraft) {
        if (infectionCenter == null || minecraft.player == null) return 0.0F;
        double distance = Math.sqrt(minecraft.player.position().distanceToSqr(Vec3.atCenterOf(infectionCenter)));
        int radius = infectionRadius();
        double effectStart = Math.max(0.0D, radius - 32.0D);
        return Mth.clamp((float) ((distance - effectStart)
                / Math.max(1.0D, radius - effectStart)), 0.0F, 1.0F);
    }

    private static void renderZoneBoundaryWarning(GuiGraphicsExtractor graphics, Font font, int width, int height, float intensity) {
        if (intensity < 0.01F || infectionCenter == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        double distance = Math.sqrt(minecraft.player.position().distanceToSqr(Vec3.atCenterOf(infectionCenter)));
        int remaining = Math.max(0, (int) Math.ceil(infectionRadius() - distance));
        float pulse = 0.72F + 0.28F * (float) Math.sin(phaseTicks * 0.8F);
        int alpha = Mth.clamp(Math.round((100.0F + 155.0F * intensity) * pulse), 50, 255);
        int color = alpha << 24 | 0x00FF2020;
        int thickness = 1 + Math.round(intensity * 3.0F);
        graphics.fill(0, 0, width, thickness, color);
        graphics.fill(0, height - thickness, width, height, color);
        graphics.fill(0, 0, thickness, height, color);
        graphics.fill(width - thickness, 0, width, height, color);

        if (remaining > 15) return;
        String warning = "WARNING: " + remaining + " BLOCKS TO ZONE LIMIT";
        int boxWidth = font.width(warning) + 12;
        int x = (width - boxWidth) / 2;
        graphics.fill(x, 4, x + boxWidth, 18, 0xD0000000);
        graphics.fill(x, 4, x + boxWidth, 5, color);
        graphics.text(font, warning, x + 6, 7,
                phaseTicks / 4 % 2 == 0 ? 0xFFFFFFFF : 0xFFFF3030, true);
    }

    private static void spawnPopup(boolean initial) {
        int variant = popupSerial++ % 7;
        int width = 82 + RANDOM.nextInt(50);
        int height = 49 + RANDOM.nextInt(29);
        int lifetime = initial ? 45 + RANDOM.nextInt(36) : 35 + RANDOM.nextInt(66);
        POPUPS.add(new Popup(phaseTicks, lifetime, RANDOM.nextFloat(), RANDOM.nextFloat(), width, height, variant));
    }

    private static void window(GuiGraphicsExtractor graphics, int x, int y, int width, int height, String title) {
        graphics.fill(x, y, x + width, y + height, 0xFF34343B);
        graphics.fill(x + 2, y + 2, x + width - 2, y + 12, 0xFF686873);
        graphics.fill(x + 3, y + 3, x + width - 3, y + 11, 0xFF3B3B43);
        graphics.fill(x + 2, y + 12, x + width - 2, y + height - 2, 0xFFA7A7AE);
        Font font = Minecraft.getInstance().font;
        float titleScale = 0.70F;
        String clippedTitle = font.plainSubstrByWidth(title, Math.max(1, (int) ((width - 10) / titleScale)));
        drawScaledString(graphics, font, clippedTitle, x + 5, y + 4, 0xFFFFFFFF, titleScale, true);
    }

    private static String popupTitle(int index) {
        return switch (index % 6) {
            case 0 -> "RANSOM";
            case 1 -> "I FOUND YOU";
            case 2 -> "YOU ARE AN IDIOT";
            case 3 -> "PAY UP";
            case 4 -> "YOUR COMPUTER";
            default -> "ERROR_90";
        };
    }

    private static void drawWrapped(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int maxWidth) {
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(text), maxWidth);
        for (int i = 0; i < Math.min(3, lines.size()); i++) {
            graphics.text(font, lines.get(i), x, y + i * 10, 0xFFFFFFFF, false);
        }
    }

    private static void drawCentered(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int color, float scale) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.text(font, text, -font.width(text) / 2, -font.lineHeight / 2, color, true);
        graphics.pose().popMatrix();
    }

    private static int windowJitterX(int second, int salt) {
        return jitterValue(second, salt * 31 + 7);
    }

    private static int windowJitterY(int second, int salt) {
        return jitterValue(second, salt * 47 + 13);
    }

    private static int jitterValue(int second, int salt) {
        int value = second * 1103515245 + salt * 12345;
        value ^= value >>> 16;
        return Math.floorMod(value, 5) - 2;
    }

    private static void drawScaledString(GuiGraphicsExtractor graphics, Font font, String text,
                                         int x, int y, int color, float scale, boolean shadow) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.text(font, text, 0, 0, color, shadow);
        graphics.pose().popMatrix();
    }

    private static void drawScaledStringRight(GuiGraphicsExtractor graphics, Font font, String text,
                                              int right, int y, int color, float scale, boolean shadow) {
        int x = right - Math.round(font.width(text) * scale);
        drawScaledString(graphics, font, text, x, y, color, scale, shadow);
    }

    private static void drawWrappedScaled(GuiGraphicsExtractor graphics, Font font, String text,
                                          int x, int y, int maxWidth, float scale) {
        int unscaledWidth = Math.max(1, (int) (maxWidth / scale));
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(text), unscaledWidth);
        int lineStep = Math.max(7, Math.round((font.lineHeight + 1) * scale));
        for (int i = 0; i < Math.min(3, lines.size()); i++) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(x, y + i * lineStep);
            graphics.pose().scale(scale, scale);
            graphics.text(font, lines.get(i), 0, 0, 0xFFFFFFFF, false);
            graphics.pose().popMatrix();
        }
    }

    private static void renderThanksWindow(GuiGraphicsExtractor graphics, int width, int height) {
        int windowWidth = Math.min(140, width - 24);
        int windowHeight = 104;
        float movement = Math.min(1.0F, phaseTicks / 12.0F);
        float xRatio = Mth.lerp(movement, successStartX, 0.5F);
        float yRatio = Mth.lerp(movement, successStartY, 0.5F);
        int x = Mth.clamp((int) (xRatio * (width - windowWidth)), 4, Math.max(4, width - windowWidth - 4));
        int y = Mth.clamp((int) (yRatio * (height - windowHeight - 24)), 4, Math.max(4, height - windowHeight - 24));
        window(graphics, x, y, windowWidth, windowHeight, "RANSOM");
        graphics.fill(x + 3, y + 12, x + windowWidth - 3, y + windowHeight - 3, 0xFF000000);

        if (phaseTicks < 28) {
            int faceSize = Math.min(72, windowHeight - 19);
            blitScaled(graphics, RANSOM, x + (windowWidth - faceSize) / 2, y + 15,
                    faceSize, faceSize, 200, 200);
        } else {
            int contentWidth = windowWidth - 8;
            int contentHeight = windowHeight - 18;
            int fittedWidth = Math.min(contentWidth, contentHeight * 962 / 633);
            int fittedHeight = fittedWidth * 633 / 962;
            blitScaled(graphics, WIN, x + (windowWidth - fittedWidth) / 2,
                    y + 14 + (contentHeight - fittedHeight) / 2,
                    fittedWidth, fittedHeight, 962, 633);
        }
    }

    private static void blitScaled(GuiGraphicsExtractor graphics, Identifier texture, int x, int y,
                                   int width, int height, int textureWidth, int textureHeight) {
        graphics.blit(texture, x, y, x + width, y + height, 0, 0, 1, 1);
    }

    private static void play(Minecraft minecraft, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        minecraft.player.playSound(sound, volume, pitch);
    }

    private static void stopSoundtrack(Minecraft minecraft) {
        if (soundtrack != null) {
            minecraft.getSoundManager().stop(soundtrack);
            soundtrack = null;
        }
        soundtrackPaused = false;
    }

    private static void pauseSoundtrack(Minecraft minecraft) {
        if (soundtrack == null) return;
        var engine = ((SoundManagerAccessor) minecraft.getSoundManager()).ransom$getSoundEngine();
        var handle = ((SoundEngineAccessor) engine).ransom$getInstanceToChannel().get(soundtrack);
        if (handle != null) handle.execute(com.mojang.blaze3d.audio.Channel::pause);
    }

    private static void resumeSoundtrack(Minecraft minecraft) {
        if (soundtrack == null) return;
        var engine = ((SoundManagerAccessor) minecraft.getSoundManager()).ransom$getSoundEngine();
        var handle = ((SoundEngineAccessor) engine).ransom$getInstanceToChannel().get(soundtrack);
        if (handle != null) handle.execute(com.mojang.blaze3d.audio.Channel::unpause);
    }

    private static void muteMinecraftMusic(Minecraft minecraft) {
        if (savedMusicVolume != null) return;
        var musicVolume = minecraft.options.getSoundSourceOptionInstance(SoundSource.MUSIC);
        savedMusicVolume = musicVolume.get();
        musicVolume.set(0.0D);
    }

    private static void restoreMinecraftMusic(Minecraft minecraft) {
        if (savedMusicVolume == null) return;
        minecraft.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(savedMusicVolume);
        savedMusicVolume = null;
    }

    private static void stopJumpscare(Minecraft minecraft) {
        if (jumpscareSound != null) {
            minecraft.getSoundManager().stop(jumpscareSound);
            jumpscareSound = null;
        }
    }

    private static void setServerRansomState(Minecraft minecraft, boolean active, boolean failure, int coins) {
        setServerRansomState(minecraft, active, failure, coins, 0, false);
    }

    private static void setServerRansomState(Minecraft minecraft, boolean active, boolean failure, int coins, int penaltyDamage) {
        setServerRansomState(minecraft, active, failure, coins, penaltyDamage, false);
    }

    private static void setServerRansomState(Minecraft minecraft, boolean active, boolean failure, int coins,
                                              int penaltyDamage, boolean deleteHotbar) {
        if (minecraft.getConnection() != null) {
            ClientPlayNetworking.send(new RansomStatePayload(active, failure, coins, penaltyDamage, deleteHotbar));
        }
    }

    private static int jumpscareEndTick() {
        return Math.max(1, secondsToTicks(ClientConfig.JUMPSCARE_ENDS_AT.get()));
    }

    private static int downloadStartTick() {
        return Math.max(jumpscareEndTick(), secondsToTicks(ClientConfig.DOWNLOAD_STARTS_AT.get()));
    }

    private static int downloadEndTick() {
        return Math.max(downloadStartTick() + 1, secondsToTicks(ClientConfig.DOWNLOAD_ENDS_AT.get()));
    }

    private static int secondsToTicks(double seconds) {
        return (int) Math.round(seconds * 20.0);
    }

    private static boolean stopInputHeld(Minecraft minecraft) {
        var options = minecraft.options;
        return options.keyUp.isDown() || options.keyDown.isDown()
                || options.keyLeft.isDown() || options.keyRight.isDown()
                || options.keyJump.isDown() || options.keyShift.isDown()
                || options.keySprint.isDown() || options.keyAttack.isDown()
                || options.keyUse.isDown();
    }

    private static int infectionRadius() {
        return ClientConfig.INFECTION_RADIUS.get();
    }

    private static int randomNaturalSpawnDelayTicks() {
        int minimum = ClientConfig.NATURAL_SPAWN_MIN_SECONDS.get();
        int maximum = Math.max(minimum, ClientConfig.NATURAL_SPAWN_MAX_SECONDS.get());
        int seconds = minimum + RANDOM.nextInt(maximum - minimum + 1);
        return seconds * 20;
    }

    private static boolean handsLocked() {
        return phase == Phase.RANSOM || phase == Phase.ESCAPE_SCARE;
    }

    private static Identifier texture(String file) {
        return Identifier.fromNamespaceAndPath(RansomFabric.MODID, "textures/" + file);
    }

    private record InfectedBlock(BlockPos pos, int value) {
    }

    private record Popup(int bornTick, int lifetime, float x, float y, int width, int height, int variant) {
    }

    private enum Phase {
        IDLE,
        WARNING,
        JUMPSCARE,
        RANSOM,
        WIN,
        FAILED,
        ESCAPE_SCARE,
        EMPTY_AREA_EXIT
    }
}
