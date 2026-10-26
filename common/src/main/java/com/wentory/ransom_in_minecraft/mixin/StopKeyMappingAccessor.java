package com.wentory.ransom_in_minecraft.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads the assigned control without consulting a toggle action's state. */
@Mixin(KeyMapping.class)
public interface StopKeyMappingAccessor {
    @Accessor("key") InputConstants.Key ransom$physicalKey();
}
