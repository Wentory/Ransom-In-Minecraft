package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** Render layers used by the infection overlay. */
final class RansomRenderTypes extends RenderStateShard {
    private RansomRenderTypes() {
        super("ransom_render_types", () -> {}, () -> {});
    }

    static RenderType infectedGlitch(ResourceLocation texture) {
        return RenderType.create(
                "ransom_infected_glitch",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setLightmapState(LIGHTMAP)
                        .setOverlayState(OVERLAY)
                        .setLayeringState(POLYGON_OFFSET_LAYERING)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false));
    }
}
