package com.wentory.ransom_in_minecraft.mixin;
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(ClientInput.class)
public interface HijackedInputAccessor {
    @Accessor("moveVector") void ransom$movement(Vec2 vector);
}
