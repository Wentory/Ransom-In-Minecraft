package com.wentory.ransom_in_minecraft.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
/** Positional mono sound, full volume within 16 blocks and silent at 32. */
public final class CrucifixSpatialSound extends AbstractTickableSoundInstance {
    private CrucifixSpatialSound(SoundInstance original){
        super(SoundEvent.createVariableRangeEvent(original.getIdentifier()),original.getSource(),RandomSource.create());
        x=original.getX();y=original.getY();z=original.getZ();relative=false;attenuation=Attenuation.NONE;updateVolume();
    }
    public static SoundInstance wrap(SoundInstance original){
        if(original instanceof CrucifixSpatialSound || !original.getIdentifier().getNamespace().equals("ransom_in_minecraft"))return original;
        String name=original.getIdentifier().getPath();
        return name.equals("crucifix_success")||name.equals("ransom_scream")||name.equals("hijack_crucifixed")?new CrucifixSpatialSound(original):original;
    }
    @Override public void tick(){if(Minecraft.getInstance().level==null){stop();return;}updateVolume();}
    @Override public boolean canStartSilent(){return true;}
    private void updateVolume(){
        double distance=Minecraft.getInstance().gameRenderer.mainCamera().position().distanceTo(new Vec3(x,y,z));
        volume=Math.clamp((float)((32-distance)/16),0,1);
    }
}
