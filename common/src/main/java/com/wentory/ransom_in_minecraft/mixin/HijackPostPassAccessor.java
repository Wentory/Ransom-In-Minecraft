package com.wentory.ransom_in_minecraft.mixin;
import java.util.Map;
import com.mojang.blaze3d.buffers.GpuBuffer;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(PostPass.class)
public interface HijackPostPassAccessor {
    @Accessor("customUniforms") Map<String,GpuBuffer> ransom$uniforms();
}
