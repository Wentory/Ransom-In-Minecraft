package com.wentory.ransom_in_minecraft.mixin;

import com.mojang.brigadier.ParseResults;
import com.wentory.ransom_in_minecraft.HijackSpeech;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public abstract class HijackChatCommandMixin {
    @Inject(method = "performCommand", at = @At("HEAD"), cancellable = true)
    private void ransom$blockSpeechCommand(ParseResults<CommandSourceStack> parsed, String command, CallbackInfo ci) {
        if (HijackSpeech.blockCommand(parsed)) ci.cancel();
    }
}
