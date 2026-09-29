package com.wentory.ransom_in_minecraft;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.wentory.ransom_in_minecraft.network.WormInfectionPayload;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = RansomInMinecraft.MODID)
public final class WormInfection {
    private static final String VALUE = "ransom_worm_infection";
    private static final String RECOVERY = "ransom_worm_recovery_ticks";
    private WormInfection() {}
    public static int get(ServerPlayer player) { return Math.clamp(player.getPersistentData().getInt(VALUE), 0, 100); }
    public static int set(ServerPlayer player, int value) {
        int next = StealerConfig.WORM_ENABLED.get() && player.isAlive() ? Math.clamp(value, 0, 100) : 0;
        player.getPersistentData().putInt(VALUE, next);
        PacketDistributor.sendToPlayer(player, new WormInfectionPayload(next));
        return next;
    }
    public static void add(ServerPlayer player, int amount) { set(player, get(player) + amount); }

    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        int value = get(player);
        if (!StealerConfig.WORM_ENABLED.get() || !player.isAlive()) {
            if (value != 0) set(player, 0);
            player.getPersistentData().remove(RECOVERY);
            return;
        }
        if (value == 0) { player.getPersistentData().remove(RECOVERY); return; }
        int ticks = player.getPersistentData().getInt(RECOVERY) + 1;
        if (ticks >= StealerConfig.WORM_HEAL_TICKS.get()) { add(player, -1); ticks = 0; }
        player.getPersistentData().putInt(RECOVERY, ticks);
    }
    @SubscribeEvent public static void hit(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getNewDamage() > 0
                && event.getSource().getEntity() instanceof Zombie zombie && InfectedZombies.isInfected(zombie)) add(player, 2);
    }
    @SubscribeEvent public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { set(player, 0); player.getPersistentData().remove(RECOVERY); }
        else if (event.getSource().getEntity() instanceof ServerPlayer killer
                && (event.getEntity() instanceof Zombie zombie && InfectedZombies.isInfected(zombie)
                || event.getEntity() instanceof Creeper creeper && InfectedCreepers.isInfected(creeper))) add(killer, -2);
    }
    @SubscribeEvent public static void eat(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getItem().is(Items.ENCHANTED_GOLDEN_APPLE)) add(player, -50);
        else if (event.getItem().is(Items.GOLDEN_APPLE)) add(player, -20);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) set(player, get(player));
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) set(player, get(player));
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getPersistentData().putInt(VALUE, event.isWasDeath() ? 0 : event.getOriginal().getPersistentData().getInt(VALUE));
        }
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) set(player, get(player));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("hijack")
                .then(Commands.literal("get")
                        .executes(c -> report(c.getSource(), c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> report(c.getSource(), EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("set").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100)).executes(c -> {
                            ServerPlayer player = EntityArgument.getPlayer(c, "player");
                            set(player, IntegerArgumentType.getInteger(c, "value"));
                            return report(c.getSource(), player);
                        }))))
                .then(Commands.literal("add").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(-100, 100)).executes(c -> {
                            ServerPlayer player = EntityArgument.getPlayer(c, "player");
                            add(player, IntegerArgumentType.getInteger(c, "amount"));
                            return report(c.getSource(), player);
                        }))));
    }
    private static int report(CommandSourceStack source, ServerPlayer player) {
        int value = get(player);
        source.sendSuccess(() -> Component.literal(player.getGameProfile().getName() + ": " + value + "/100"), false);
        return value;
    }
}
