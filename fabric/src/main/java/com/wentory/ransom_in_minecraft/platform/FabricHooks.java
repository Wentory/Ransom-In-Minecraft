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
        CommandRegistrationCallback.EVENT.register((dispatcher,lookup,environment) ->
                RansomServerState.registerCommands(new RegisterCommandsEvent(dispatcher)));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> FabricNetwork.server=server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {FabricNetwork.server=null; DIMENSIONS.clear();});
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            RansomChestGame.tick(new ServerTickEvent.Post(server));
            for(var player:server.getPlayerList().getPlayers()) {
                var previous=DIMENSIONS.put(player.getUUID(),player.level().dimension());
                if(previous!=null && !previous.equals(player.level().dimension()))
                    WormInfection.dimension(new PlayerEvent.PlayerChangedDimensionEvent(player));
                var event=new PlayerTickEvent.Post(player);
                RansomServerState.naturalSpawnTick(event); RansomServerState.keepMainHandEmpty(event); WormInfection.tick(event);
            }
        });
        ServerEntityEvents.ENTITY_LOAD.register((entity,level) -> {
            var event=new EntityJoinLevelEvent(entity,level);
            InfectedZombies.onJoin(event); InfectedCreepers.join(event);
        });
        EntityTrackingEvents.START_TRACKING.register((entity,player) -> {
            var event=new PlayerEvent.StartTracking(player,entity);
            InfectedZombies.onStartTracking(event); InfectedCreepers.tracking(event);
        });
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server) -> {
            var event=new PlayerEvent.PlayerLoggedInEvent(handler.player);
            RansomServerState.playerLoggedIn(event); WormInfection.login(event); CommonSettingsPayload.login(event);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server) -> {
            DIMENSIONS.remove(handler.player.getUUID());
            var event=new PlayerEvent.PlayerLoggedOutEvent(handler.player);
            RansomChestGame.logout(event); RansomServerState.playerLoggedOut(event);
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
            WormInfection.hit(event); InfectedZombies.onHurt(event);
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity,source) -> {
            var event=new LivingDeathEvent(entity,source);
            RansomServerState.playerDied(event); WormInfection.death(event);
        });
        ServerLivingEntityEvents.MOB_CONVERSION.register((oldMob,newMob,params) ->
                InfectedZombies.onConversion(new LivingConversionEvent.Post(oldMob,newMob)));
        UseBlockCallback.EVENT.register((player,level,hand,hit) -> {
            var event=new PlayerInteractEvent.RightClickBlock(player,hand,hit.getBlockPos());
            RansomServerState.blockBlockUse(event);
            if(!event.isCanceled()) RansomChestGame.openChest(event);
            return event.isCanceled()?InteractionResult.FAIL:InteractionResult.PASS;
        });
        UseItemCallback.EVENT.register((player,level,hand) -> {
            var event=new PlayerInteractEvent.RightClickItem(player);
            RansomServerState.blockItemUse(event);
            return event.isCanceled()?InteractionResult.FAIL:InteractionResult.PASS;
        });
        UseEntityCallback.EVENT.register((player,level,hand,entity,hit) -> {
            var event=new PlayerInteractEvent.EntityInteract(player,player.getItemInHand(hand));
            RansomServerState.blockEntityUse(event);
            return event.isCanceled()?InteractionResult.FAIL:InteractionResult.PASS;
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
