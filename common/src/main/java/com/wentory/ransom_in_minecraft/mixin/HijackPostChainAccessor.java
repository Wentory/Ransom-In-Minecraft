package com.wentory.ransom_in_minecraft.mixin;
import java.util.List;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(PostChain.class)
public interface HijackPostChainAccessor {
    @Accessor("passes") List<PostPass> ransom$passes();
}
