package com.wentory.ransom_in_minecraft.mixin;
import com.wentory.ransom_in_minecraft.RansomEffects;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import java.util.Collection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(Hud.class)
public abstract class FirewallHudMixin {
    @Redirect(method="extractEffects",at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;getActiveEffects()Ljava/util/Collection;"))
    private Collection<MobEffectInstance> ransom$inventoryOnly(LocalPlayer player){
        return player.getActiveEffects().stream().filter(effect->!effect.is(RansomEffects.FIREWALL)).toList();
    }
}
