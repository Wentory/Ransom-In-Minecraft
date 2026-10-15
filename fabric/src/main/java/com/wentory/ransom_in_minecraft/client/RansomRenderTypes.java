package com.wentory.ransom_in_minecraft.client;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
final class RansomRenderTypes {
 private RansomRenderTypes() {}
 static RenderType crucifixShine(Identifier texture,boolean compatible){return CrucifixPipelines.moonlight(texture);}
 static RenderType crucifixCircle(Identifier texture){return CrucifixPipelines.moonlight(texture);}
 static RenderType banishmentSprite(Identifier texture){return CrucifixPipelines.unlit(texture);}
 static RenderType crucifixUnlit(Identifier texture,boolean translucent){return CrucifixPipelines.unlit(texture);}
 static RenderType crucifixBlockLight(){return CrucifixPipelines.blockLight();}
 static RenderType crucifixChestLight(Identifier texture){return CrucifixPipelines.chestLight(texture);}

 static RenderType caveCloud(boolean owner){return CaveRansomPipelines.cloud(owner);}
 static RenderType caveSprite(Identifier texture,boolean owner){return CaveRansomPipelines.sprite(texture,owner);}
 static RenderType infectedGlitch(Identifier texture) { return RenderTypes.entityTranslucent(texture); }
 static RenderType boundaryGlitch(Identifier texture) { return RenderTypes.entityTranslucent(texture); }
 static RenderType boundaryFragments() { return RenderTypes.debugQuads(); }
}
