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

    private static final java.util.Map<ResourceLocation, RenderType> CIRCLE_TRANSLUCENT_TYPES = new java.util.HashMap<>();
    private static final java.util.Map<ResourceLocation, RenderType> BANISHMENT_TYPES = new java.util.HashMap<>();
    private static final java.util.Map<ResourceLocation, RenderType> SHINE_SHADER_TYPES = new java.util.HashMap<>();
    private static final java.util.Map<ResourceLocation, RenderType> SHINE_COMPAT_TYPES = new java.util.HashMap<>();
    private static final java.util.Map<ResourceLocation, RenderType> UNLIT_TYPES = new java.util.HashMap<>();
    private static final java.util.Map<ResourceLocation, RenderType> UNLIT_FADE_TYPES = new java.util.HashMap<>();
    private static final java.util.Map<ResourceLocation, RenderType> CHEST_LIGHT_TYPES = new java.util.HashMap<>();
    private static final java.util.Map<ResourceLocation, RenderType> HIJACKED_SURFACE_TYPES = new java.util.HashMap<>();
    private static final java.util.Map<ResourceLocation, RenderType> CAVE_THROUGH_TYPES = new java.util.HashMap<>();
    private static final java.util.Map<ResourceLocation, RenderType> CAVE_VISIBLE_TYPES = new java.util.HashMap<>();
    // Vanilla NO_DEPTH_TEST is a no-op in 1.21.1; it does not disable inherited depth testing.
    private static final DepthTestStateShard CAVE_THROUGH_DEPTH = new DepthTestStateShard("ransom_cave_through_depth", 519) {
        @Override public void setupRenderState() {
            com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
        }
        @Override public void clearRenderState() {
            com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
            com.mojang.blaze3d.systems.RenderSystem.depthFunc(515);
        }
    };
    private static final RenderType CAVE_CLOUD_OWNER = caveCloudType(true);
    private static final RenderType CAVE_CLOUD_OTHER = caveCloudType(false);

    static RenderType caveCloud(boolean owner) { return owner ? CAVE_CLOUD_OWNER : CAVE_CLOUD_OTHER; }

    private static RenderType caveCloudType(boolean owner) {
        return RenderType.create(owner ? "ransom_cave_cloud_owner" : "ransom_cave_cloud_other",
                DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 8192, false, true,
                RenderType.CompositeState.builder().setShaderState(POSITION_COLOR_SHADER)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL)
                        .setDepthTestState(owner ? CAVE_THROUGH_DEPTH : LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    }

    static RenderType caveSprite(ResourceLocation texture, boolean throughWalls) {
        var cache = throughWalls ? CAVE_THROUGH_TYPES : CAVE_VISIBLE_TYPES;
        return cache.computeIfAbsent(texture, key -> RenderType.create(
                "ransom_cave_owner", DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS,
                1536, false, true, RenderType.CompositeState.builder()
                        .setShaderState(new ShaderStateShard(net.minecraft.client.renderer.GameRenderer::getPositionTexColorShader))
                        .setTextureState(new TextureStateShard(key, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL)
                        .setDepthTestState(throughWalls ? CAVE_THROUGH_DEPTH : LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false)));
    }

    static RenderType hijackedSurface(ResourceLocation texture) {
        return HIJACKED_SURFACE_TYPES.computeIfAbsent(texture,
                key -> translucentGlitch("ransom_hijacked_surface", key));
    }

    static RenderType crucifixChestLight(ResourceLocation texture) {
        return CHEST_LIGHT_TYPES.computeIfAbsent(texture, key -> RenderType.create(
                "ransom_crucifix_chest_light", DefaultVertexFormat.POSITION_TEX_COLOR,
                VertexFormat.Mode.QUADS, 1536, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(new ShaderStateShard(CrucifixCircleRenderer::lightShader))
                        .setTextureState(new TextureStateShard(key, false, false))
                        .setTransparencyState(LIGHTNING_TRANSPARENCY)
                        .setCullState(CULL)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setLayeringState(POLYGON_OFFSET_LAYERING)
                        .setOutputState(ITEM_ENTITY_TARGET)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false)));
    }
    private static final RenderType CRUCIFIX_BLOCK_LIGHT = RenderType.create(
            "ransom_crucifix_block_light", DefaultVertexFormat.POSITION_TEX_COLOR,
            VertexFormat.Mode.QUADS, 4096, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(new ShaderStateShard(CrucifixCircleRenderer::lightShader))
                    .setTextureState(new TextureStateShard(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS, false, false))
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setLayeringState(POLYGON_OFFSET_LAYERING)
                    .setOutputState(ITEM_ENTITY_TARGET)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    static RenderType crucifixBlockLight() { return CRUCIFIX_BLOCK_LIGHT; }

    static RenderType crucifixUnlit(ResourceLocation texture, boolean fading) {
        var cache = fading ? UNLIT_FADE_TYPES : UNLIT_TYPES;
        return cache.computeIfAbsent(texture, key -> RenderType.create(
                "ransom_crucifix_unlit", DefaultVertexFormat.POSITION_TEX_COLOR,
                VertexFormat.Mode.QUADS, 1536, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(new ShaderStateShard(net.minecraft.client.renderer.GameRenderer::getPositionTexColorShader))
                        .setTextureState(new TextureStateShard(key, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setOutputState(ITEM_ENTITY_TARGET)
                        .setWriteMaskState(fading ? COLOR_WRITE : COLOR_DEPTH_WRITE)
                        .createCompositeState(false)));
    }

    static RenderType crucifixShine(ResourceLocation texture, boolean compatible) {
        var cache = compatible ? SHINE_COMPAT_TYPES : SHINE_SHADER_TYPES;
        return cache.computeIfAbsent(texture, key -> RenderType.create(
                "ransom_crucifix_shine", compatible ? DefaultVertexFormat.NEW_ENTITY : DefaultVertexFormat.POSITION_TEX_COLOR,
                VertexFormat.Mode.QUADS, 1536, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(compatible ? RENDERTYPE_ENTITY_TRANSLUCENT_SHADER
                                : new ShaderStateShard(CrucifixCircleRenderer::shader))
                        .setTextureState(new TextureStateShard(key, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setOverlayState(OVERLAY)
                        .setLightmapState(LIGHTMAP)
                        .setOutputState(ITEM_ENTITY_TARGET)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false)));
    }
    private static final RenderType BANISHMENT_FLASH = RenderType.create(
            "ransom_banishment_flash", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 1536, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setOutputState(ITEM_ENTITY_TARGET)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));
    static RenderType banishmentFlash() { return BANISHMENT_FLASH; }
    static RenderType banishmentSprite(ResourceLocation texture) {
        return BANISHMENT_TYPES.computeIfAbsent(texture, key -> RenderType.create(
                "ransom_banishment_sprite", DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS, 1536, false, true,
                RenderType.CompositeState.builder()
                        // Iris maps the emissive shader to SpiderEyes, whose pack-defined
                        // additive blending loses the dark parts of Ransom's face.
                        // Full-bright vertex light supplies visibility through the ordinary entity pass.
                        .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                        .setTextureState(new TextureStateShard(key, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setOverlayState(OVERLAY)
                        .setLightmapState(LIGHTMAP)
                        .setOutputState(ITEM_ENTITY_TARGET)
                        .setWriteMaskState(COLOR_DEPTH_WRITE)
                        .createCompositeState(false)));
    }
    static RenderType crucifixCircle(ResourceLocation texture) {
        return CIRCLE_TRANSLUCENT_TYPES.computeIfAbsent(texture, key -> RenderType.create(
                "ransom_crucifix_translucent",
                DefaultVertexFormat.POSITION_TEX_COLOR,
                VertexFormat.Mode.QUADS, 1536, false, false,
                RenderType.CompositeState.builder()
                        .setShaderState(new ShaderStateShard(CrucifixCircleRenderer::shader))
                        .setTextureState(new TextureStateShard(key, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setOutputState(ITEM_ENTITY_TARGET)
                        .setWriteMaskState(COLOR_DEPTH_WRITE)
                        .createCompositeState(false)));
    }

    static RenderType infectedGlitch(ResourceLocation texture) {
        return translucentGlitch("ransom_infected_glitch", texture);
    }

    static RenderType boundaryGlitch(ResourceLocation texture) {
        return translucentGlitch("ransom_boundary_glitch", texture);
    }

    static RenderType boundaryFragments() {
        return RenderType.create(
                "ransom_boundary_fragments",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(POSITION_COLOR_SHADER)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setOutputState(ITEM_ENTITY_TARGET)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false));
    }

    private static RenderType translucentGlitch(String name, ResourceLocation texture) {
        return RenderType.create(
                name,
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
                        .setOutputState(ITEM_ENTITY_TARGET)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false));
    }
}
