package com.wentory.ransom_in_minecraft.platform;

import com.wentory.ransom_in_minecraft.*;
import com.wentory.ransom_in_minecraft.network.*;
import com.wentory.ransom_in_minecraft.platform.FabricEvents.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;

public final class FabricHooks {
    private static final java.util.Map<java.util.UUID,net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>> DIMENSIONS=new java.util.HashMap<>();
    public static void initialize() {
        RansomServerState.registerPayloads(new RegisterPayloadHandlersEvent());
        FullHijack.register(new RegisterPayloadHandlersEvent());
        CaveRansom.register(new RegisterPayloadHandlersEvent());
        CommandRegistrationCallback.EVENT.register((dispatcher,lookup,environment) ->
                RansomServerState.registerCommands(new RegisterCommandsEvent(dispatcher)));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> FabricNetwork.server=server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            var event = new ServerStoppedEvent(server);
            FullHijack.stopped(event);
            CaveRansom.stopped(event);
            CrucifixActions.stopped(event);
            CrucifixCircleTest.stopped(event);
            FabricNetwork.server=null;
            DIMENSIONS.clear();
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            RansomChestGame.tick(new ServerTickEvent.Post(server));
            CrucifixActions.tick(new ServerTickEvent.Post(server));
            CrucifixCircleTest.expire(new ServerTickEvent.Post(server));
            for(var player:server.getPlayerList().getPlayers()) {
                var previous=DIMENSIONS.put(player.getUUID(),player.level().dimension());
                if(previous!=null && !previous.equals(player.level().dimension())) {
                    WormInfection.dimension(new PlayerEvent.PlayerChangedDimensionEvent(player));
                    CrucifixCircleTest.changedDimension(new PlayerEvent.PlayerChangedDimensionEvent(player));
                }
                var event=new PlayerTickEvent.Post(player);
                RansomServerState.naturalSpawnTick(event); RansomServerState.keepMainHandEmpty(event); WormInfection.tick(event);
                FullHijack.tick(event);
                CaveRansom.tick(event);
            }
        });
        ServerEntityEvents.ENTITY_LOAD.register((entity,level) -> {
            var event=new EntityJoinLevelEvent(entity,level);
            InfectedZombies.onJoin(event); InfectedCreepers.join(event);
            CrucifixActions.joined(event);
        });
        EntityTrackingEvents.START_TRACKING.register((entity,player) -> {
            var event=new PlayerEvent.StartTracking(player,entity);
            InfectedZombies.onStartTracking(event); InfectedCreepers.tracking(event);
            FullHijack.tracking(event);
            CrucifixActions.tracking(event);
        });
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server) -> {
            var event=new PlayerEvent.PlayerLoggedInEvent(handler.player);
            RansomServerState.playerLoggedIn(event); WormInfection.login(event); CommonSettingsPayload.login(event);
            CrucifixCircleTest.joined(event);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server) -> {
            DIMENSIONS.remove(handler.player.getUUID());
            var event=new PlayerEvent.PlayerLoggedOutEvent(handler.player);
            RansomChestGame.logout(event); RansomServerState.playerLoggedOut(event);
            FullHijack.logout(event);
            CaveRansom.logout(event);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer,newPlayer,alive) -> {
            PersistentData.copy(oldPlayer,newPlayer);
            var clone=new PlayerEvent.Clone(oldPlayer,newPlayer,!alive);
            RansomServerState.clonePlayer(clone); WormInfection.clone(clone);
            WormInfection.respawn(new PlayerEvent.PlayerRespawnEvent(newPlayer));
            RansomServerState.playerLoggedIn(new PlayerEvent.PlayerLoggedInEvent(newPlayer));
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity,source,base,taken,blocked) -> {
            var event=new LivingDamageEvent.Post(entity,source,blocked ? 0 : taken);
            WormInfection.hit(event); InfectedZombies.onHurt(event); InfectedCreepers.onHurt(event);
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity,source) -> {
            var event=new LivingDeathEvent(entity,source);
            RansomServerState.playerDied(event); WormInfection.death(event);
        });
        ServerLivingEntityEvents.MOB_CONVERSION.register((oldMob,newMob,params) ->
                InfectedZombies.onConversion(new LivingConversionEvent.Post(oldMob,newMob)));
        UseBlockCallback.EVENT.register((player,level,hand,hit) -> {
            var event=new PlayerInteractEvent.RightClickBlock(player,hand,hit.getBlockPos());
            FullHijack.interact(event);
            RansomServerState.blockBlockUse(event);
            if(!event.isCanceled()) RansomChestGame.openChest(event);
            return event.isCanceled()?InteractionResult.FAIL:InteractionResult.PASS;
        });
        UseItemCallback.EVENT.register((player,level,hand) -> {
            var event=new PlayerInteractEvent.RightClickItem(player,hand);
            FullHijack.interact(event);
            RansomServerState.blockItemUse(event);
            return event.isCanceled()?InteractionResult.FAIL:InteractionResult.PASS;
        });
        UseEntityCallback.EVENT.register((player,level,hand,entity,hit) -> {
            var event=new PlayerInteractEvent.EntityInteract(player,player.getItemInHand(hand),entity,hand);
            FullHijack.interact(event);
            if (!event.isCanceled()) CrucifixActions.useOnPlayer(event);
            RansomServerState.blockEntityUse(event);
            return event.isCanceled()?event.getCancellationResult():InteractionResult.PASS;
        });
        AttackEntityCallback.EVENT.register((player,level,hand,entity,hit) -> {
            var event = new AttackEntityEvent(player,entity);
            FullHijack.attack(event);
            if (!event.isCanceled()) CrucifixActions.banishMob(event);
            return event.isCanceled() ? InteractionResult.FAIL : InteractionResult.PASS;
        });
        AttackBlockCallback.EVENT.register((player,level,hand,pos,direction) -> {
            var event = new PlayerInteractEvent.LeftClickBlock(player,hand,pos);
            FullHijack.interact(event);
            if (!event.isCanceled()) RansomChestGame.strikeWithCrucifix(event);
            return event.isCanceled() ? InteractionResult.FAIL : InteractionResult.PASS;
        });
        PlayerBlockBreakEvents.BEFORE.register((level,player,pos,state,blockEntity) -> {
            var event = new BreakBlockEvent(level,pos,player);
            FullHijack.breakBlock(event);
            return !event.isCanceled();
        });
    }
    public static void naturalSpawn(Mob mob,EntitySpawnReason reason) {
        var event=new FinalizeSpawnEvent(mob,reason);
        InfectedZombies.onSpawn(event); InfectedCreepers.spawn(event);
    }
    public static void entityTick(Entity entity) {
        var event=new EntityTickEvent.Post(entity);
        InfectedZombies.onTick(event); InfectedCreepers.tick(event);
    }
    private FabricHooks() {}
}
