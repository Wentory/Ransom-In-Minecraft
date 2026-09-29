package com.wentory.ransom_in_minecraft.mixin;
import com.wentory.ransom_in_minecraft.WormInfection;
import com.wentory.ransom_in_minecraft.platform.FabricEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(LivingEntity.class)
public abstract class FabricLivingLifecycleMixin {
    @Redirect(method="completeUsingItem",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack ransom$finishFood(ItemStack stack,Level level,LivingEntity entity){
        ItemStack eaten=stack.copy();
        ItemStack result=stack.finishUsingItem(level,entity);
        if(!level.isClientSide()) WormInfection.eat(new FabricEvents.LivingEntityUseItemEvent.Finish(entity,eaten));
        return result;
    }
}
