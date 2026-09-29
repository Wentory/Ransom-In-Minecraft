package com.wentory.ransom_in_minecraft.mixin;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(GuiGraphicsExtractor.class)
public interface GuiGraphicsStateAccessor {
    @Accessor("guiRenderState") GuiRenderState ransom$guiState();
    @Accessor("scissorStack") GuiGraphicsExtractor.ScissorStack ransom$scissors();
}
