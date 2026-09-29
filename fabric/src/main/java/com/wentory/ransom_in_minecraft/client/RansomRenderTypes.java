package com.wentory.ransom_in_minecraft.client;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
final class RansomRenderTypes {
 private RansomRenderTypes() {}
 static RenderType infectedGlitch(Identifier texture) { return RenderTypes.entityTranslucent(texture); }
 static RenderType boundaryGlitch(Identifier texture) { return RenderTypes.entityTranslucent(texture); }
 static RenderType boundaryFragments() { return RenderTypes.debugQuads(); }
}
