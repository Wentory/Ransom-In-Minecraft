package com.wentory.ransom_in_minecraft.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.client.model.EntityModel;

/** Small event arguments shared by the loader-independent encounter implementations. */
public final class FabricEvents {
    public static class Cancelable {
        private boolean canceled;
        public void setCanceled(boolean canceled) { this.canceled=canceled; }
        public boolean isCanceled() { return canceled; }
    }
    public record EntityJoinLevelEvent(Entity getEntity,Level getLevel) {}
    public record EntityLeaveLevelEvent(Entity getEntity,Level getLevel) {}
    public record FinalizeSpawnEvent(Mob getEntity,EntitySpawnReason getSpawnType) {}
    public static class LivingConversionEvent { public record Post(LivingEntity getEntity,LivingEntity getOutcome) {} }
    public record LivingDeathEvent(LivingEntity getEntity,DamageSource getSource) {}
    public static class LivingDamageEvent { public record Post(LivingEntity getEntity,DamageSource getSource,float getHealthDamage) {} }
    public static class LivingEntityUseItemEvent { public record Finish(LivingEntity getEntity,ItemStack getItem) {} }
    public static class EntityTickEvent { public record Post(Entity getEntity) {} }
    public static class PlayerTickEvent { public record Post(Player getEntity) {} }
    public static class ServerTickEvent { public record Post(MinecraftServer getServer) {} }
    public static class ClientTickEvent { public record Pre() {} public record Post() {} }
    public static class ClientPlayerNetworkEvent { public record LoggingIn() {} public record LoggingOut() {} }
    public static class PlayerEvent {
        public record PlayerLoggedInEvent(Player getEntity) {}
        public record PlayerLoggedOutEvent(Player getEntity) {}
        public record PlayerRespawnEvent(Player getEntity) {}
        public record PlayerChangedDimensionEvent(Player getEntity) {}
        public record Clone(Player getOriginal,Player getEntity,boolean isWasDeath) {}
        public record StartTracking(Player getEntity,Entity getTarget) {}
    }
    public static class PlayerInteractEvent {
        public static class LeftClickBlock extends RightClickBlock {
            public LeftClickBlock(Player player, InteractionHand hand, BlockPos pos) { super(player, hand, pos); }
        }
        public static class RightClickBlock extends Cancelable {
            private final Player player; private final InteractionHand hand; private final BlockPos pos;
            public RightClickBlock(Player player,InteractionHand hand,BlockPos pos) {this.player=player;this.hand=hand;this.pos=pos;}
            public Player getEntity(){return player;} public Level getLevel(){return player.level();}
            public InteractionHand getHand(){return hand;} public BlockPos getPos(){return pos;}
            public ItemStack getItemStack(){return player.getItemInHand(hand);}
        }
        public static class RightClickItem extends Cancelable {
            private final Player player;
            private final InteractionHand hand;
            public RightClickItem(Player player) { this(player, InteractionHand.MAIN_HAND); }
            public RightClickItem(Player player, InteractionHand hand) { this.player=player; this.hand=hand; }
            public Player getEntity(){return player;}
            public InteractionHand getHand(){return hand;}
            public ItemStack getItemStack(){return player.getItemInHand(hand);}
        }
        public static class EntityInteract extends RightClickItem {
            private final ItemStack item;
            private final Entity target;
            private net.minecraft.world.InteractionResult result = net.minecraft.world.InteractionResult.FAIL;
            public EntityInteract(Player p, ItemStack item) { this(p, item, null, InteractionHand.MAIN_HAND); }
            public EntityInteract(Player p, ItemStack item, Entity target, InteractionHand hand) { super(p,hand);this.item=item;this.target=target; }
            public ItemStack getItemStack(){return item;}
            public Entity getTarget(){return target;}
            public void setCancellationResult(net.minecraft.world.InteractionResult result){this.result=result;}
            public net.minecraft.world.InteractionResult getCancellationResult(){return result;}
        }
    }
    public static class AttackEntityEvent extends Cancelable {
        private final Player player;
        private final Entity target;
        public AttackEntityEvent(Player player, Entity target) { this.player=player;this.target=target; }
        public Player getEntity(){return player;}
        public Entity getTarget(){return target;}
    }
    public record ServerStoppedEvent(MinecraftServer getServer) {}
    public record RegisterCommandsEvent(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> getDispatcher) {}
    public static class RegisterPayloadHandlersEvent { public PayloadRegistrar registrar(String version){return new PayloadRegistrar();} }
    public static class ChunkWatchEvent { public record Sent(ServerPlayer getPlayer,ServerLevel getLevel,LevelChunk getChunk) {} }
    public static class BlockEvent {}
    public static class BreakBlockEvent extends Cancelable {
        private final Level level; private final BlockPos pos; private final Player player;
        public BreakBlockEvent(ServerLevel level,BlockPos pos){this(level,pos,null);}
        public BreakBlockEvent(Level level,BlockPos pos,Player player){this.level=level;this.pos=pos;this.player=player;}
        public Level getLevel(){return level;}
        public BlockPos getPos(){return pos;}
        public Player getPlayer(){return player;}
    }
    public static class ExplosionEvent {public record Detonate(Level getLevel,java.util.List<BlockPos> getAffectedBlocks) {}}
    public static class InputEvent {
        public record Key(int getKey,int getAction) {}
        public static class MouseButton {public record Pre(int getAction) {}}
        public static class InteractionKeyMappingTriggered extends Cancelable {
            private final int action; public InteractionKeyMappingTriggered(int action){this.action=action;}
            public boolean isAttack(){return action==0;} public boolean isUseItem(){return action==1;} public boolean isPickBlock(){return action==2;}
        }
    }
    public record MovementInputUpdateEvent(net.minecraft.client.player.LocalPlayer getEntity,net.minecraft.client.player.ClientInput getInput) {}
    public static class ScreenEvent {
        public static class Opening extends Cancelable {
            private final Screen screen;
            public Opening(Screen screen){this.screen=screen;}
            public Screen getNewScreen(){return screen;}
        }
        public static class Render {public record Post(Screen getScreen,GuiGraphicsExtractor getGuiGraphics) {}}
        public static class MouseButtonPressed {public static class Pre extends Cancelable {
            private final Screen screen; private final int button;
            public Pre(Screen screen,int button){this.screen=screen;this.button=button;}
            public Screen getScreen(){return screen;}public int getButton(){return button;}
        }}
        public static class KeyPressed {public static class Pre extends Cancelable {
            private final Screen screen;private final int key,scan;
            public Pre(Screen screen,int key,int scan){this.screen=screen;this.key=key;this.scan=scan;}
            public Screen getScreen(){return screen;}public int getKeyCode(){return key;}public int getScanCode(){return scan;}
        }}
    }
    public static class RenderGuiEvent {public record Post(GuiGraphicsExtractor getGuiGraphics) {}}
    public record SubmitCustomGeometryEvent(PoseStack getPoseStack,SubmitNodeCollector getSubmitNodeCollector,LevelRenderState getLevelRenderState) {}
    public static class RenderLivingEvent<T extends LivingEntity,S extends LivingEntityRenderState,M extends EntityModel<? super S>> extends Cancelable {
        private final LivingEntityRenderer<T,S,M> renderer; private final S state; private final PoseStack pose;
        private final SubmitNodeCollector collector; private final float partial;
        public RenderLivingEvent(LivingEntityRenderer<T,S,M> r,S s,PoseStack p,SubmitNodeCollector c,float partial){renderer=r;state=s;pose=p;collector=c;this.partial=partial;}
        public LivingEntityRenderer<T,S,M> getRenderer(){return renderer;} public S getRenderState(){return state;}
        public PoseStack getPoseStack(){return pose;} public SubmitNodeCollector getSubmitNodeCollector(){return collector;}
        public float getPartialTick(){return partial;} public int getPackedLight(){return state.lightCoords;}
        public static class Pre<T extends LivingEntity,S extends LivingEntityRenderState,M extends EntityModel<? super S>> extends RenderLivingEvent<T,S,M>{
            public Pre(LivingEntityRenderer<T,S,M> r,S s,PoseStack p,SubmitNodeCollector c,float t){super(r,s,p,c,t);}}
        public static class Post<T extends LivingEntity,S extends LivingEntityRenderState,M extends EntityModel<? super S>> extends RenderLivingEvent<T,S,M>{
            public Post(LivingEntityRenderer<T,S,M> r,S s,PoseStack p,SubmitNodeCollector c,float t){super(r,s,p,c,t);}}
    }
    public static class RenderPlayerEvent extends Cancelable {
        private final AvatarRenderer<?> renderer;private final AvatarRenderState state;private final PoseStack pose;private final SubmitNodeCollector collector;private final float partial;
        public RenderPlayerEvent(AvatarRenderer<?> r,AvatarRenderState s,PoseStack p,SubmitNodeCollector c,float t){renderer=r;state=s;pose=p;collector=c;partial=t;}
        public AvatarRenderer<?> getRenderer(){return renderer;} public AvatarRenderState getRenderState(){return state;}
        public PoseStack getPoseStack(){return pose;} public SubmitNodeCollector getSubmitNodeCollector(){return collector;}
        public float getPartialTick(){return partial;}public int getPackedLight(){return state.lightCoords;}
        public static class Pre extends RenderPlayerEvent {public Pre(AvatarRenderer<?> r,AvatarRenderState s,PoseStack p,SubmitNodeCollector c,float t){super(r,s,p,c,t);}}
        public static class Post extends RenderPlayerEvent {public Post(AvatarRenderer<?> r,AvatarRenderState s,PoseStack p,SubmitNodeCollector c,float t){super(r,s,p,c,t);}}
    }
    private FabricEvents() {}
}
