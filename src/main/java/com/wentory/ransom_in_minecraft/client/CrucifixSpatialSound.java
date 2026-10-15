package com.wentory.ransom_in_minecraft.client;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

/** Positional mono playback with a full-volume inner radius and a linear outer fade. */
@EventBusSubscriber(modid = RansomInMinecraft.MODID, value = Dist.CLIENT)
public final class CrucifixSpatialSound extends AbstractTickableSoundInstance {
    private static final float INNER_RADIUS = 16;
    private static final float OUTER_RADIUS = 32;

    private CrucifixSpatialSound(SoundInstance original) {
        super(SoundEvent.createVariableRangeEvent(original.getLocation()), original.getSource(), RandomSource.create());
        x = original.getX();
        y = original.getY();
        z = original.getZ();
        relative = false;
        attenuation = Attenuation.NONE;
        updateVolume();
    }

    @SubscribeEvent
    public static void onSound(PlaySoundEvent event) {
        SoundInstance original = event.getSound();
        if (original == null || original instanceof CrucifixSpatialSound
                || !original.getLocation().getNamespace().equals(RansomInMinecraft.MODID)) return;
        String path = original.getLocation().getPath();
        if (path.equals("crucifix_success") || path.equals("ransom_scream") || path.equals("hijack_crucifixed")) {
            event.setSound(new CrucifixSpatialSound(original));
        }
    }

    @Override
    public void tick() {
        if (Minecraft.getInstance().level == null) {
            stop();
            return;
        }
        updateVolume();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    private void updateVolume() {
        Vec3 listener = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double distance = listener.distanceTo(new Vec3(x, y, z));
        volume = Mth.clamp((float) ((OUTER_RADIUS - distance) / (OUTER_RADIUS - INNER_RADIUS)), 0, 1);
    }
}
