package com.wentory.ransom_in_minecraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import java.util.List;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class HoneyPotItem extends Item {
    private static final int FIREWALL_DURATION_TICKS = 10 * 60 * 20;

    public HoneyPotItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.ransom_in_minecraft.honey_pot.use")
                .withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            consume(serverPlayer, stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static void consume(ServerPlayer serverPlayer, ItemStack stack) {
        if (!stack.is(RansomItems.HONEY_POT.get()) || !serverPlayer.isAlive()) return;
        ServerLevel level = serverPlayer.serverLevel();
        serverPlayer.addEffect(new MobEffectInstance(RansomEffects.FIREWALL, FIREWALL_DURATION_TICKS,
                0, false, false, true));
        WormInfection.set(serverPlayer, 0);
        Vec3 burst = serverPlayer.getEyePosition().add(serverPlayer.getLookAngle().scale(0.45));
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.HONEY_BLOCK.defaultBlockState()),
                burst.x, burst.y, burst.z, 18, 0.18, 0.18, 0.18, 0.08);
        level.playSound(null, BlockPos.containing(burst), SoundEvents.HONEY_BLOCK_BREAK,
                SoundSource.PLAYERS, 1.0F, 1.0F);
        level.playSound(null, BlockPos.containing(burst), SoundEvents.HONEY_BLOCK_SLIDE,
                SoundSource.PLAYERS, 0.8F, 1.2F);
        if (!serverPlayer.getAbilities().instabuild) stack.shrink(1);
    }
}
