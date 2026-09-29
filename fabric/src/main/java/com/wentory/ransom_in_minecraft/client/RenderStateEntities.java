package com.wentory.ransom_in_minecraft.client;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import java.util.Map;
import java.util.WeakHashMap;

public final class RenderStateEntities {
    private static final Map<EntityRenderState, Entity> ENTITIES = new WeakHashMap<>();
    public static synchronized void remember(EntityRenderState state, Entity entity) { ENTITIES.put(state, entity); }
    public static synchronized Entity get(EntityRenderState state) { return ENTITIES.get(state); }
}
