package com.wentory.ransom_in_minecraft.mixin;
import com.wentory.ransom_in_minecraft.client.WormEffects;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Hud.class)
public abstract class FabricHudMixin {
    @Inject(method="extractHearts",at=@At("HEAD"),cancellable=true)
    private void ransom$hearts(GuiGraphicsExtractor graphics,Player player,int x,int y,int rowHeight,int offset,
                               float maxHealth,int health,int oldHealth,int absorption,boolean blink,CallbackInfo ci){
        if(WormEffects.hasFalseHud()) {WormEffects.falseHud(true,graphics,x,y);ci.cancel();}
    }
    @Inject(method="extractFood",at=@At("HEAD"),cancellable=true)
    private void ransom$food(GuiGraphicsExtractor graphics,Player player,int y,int x,CallbackInfo ci){
        if(WormEffects.hasFalseHud()) {WormEffects.falseHud(false,graphics,x,y);ci.cancel();}
    }
}
