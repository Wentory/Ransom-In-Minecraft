package com.wentory.ransom_in_minecraft;

import com.mojang.brigadier.ParseResults;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class HijackSpeech {
    public static void initialize() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, player, type) -> allowMessage(player));
        ServerMessageEvents.ALLOW_COMMAND_MESSAGE.register((message, source, type) ->
                !(source.getEntity() instanceof ServerPlayer player) || allowMessage(player));
    }

    private static boolean allowMessage(ServerPlayer player) {
        if (!FullHijack.active(player)) return true;
        reject(player);
        return false;
    }

    public static boolean blockCommand(ParseResults<CommandSourceStack> parsed) {
        var context = parsed.getContext();
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)
                || !FullHijack.active(player) || !HijackChatCommands.blocks(context)) return false;
        reject(player);
        return true;
    }

    private static void reject(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("YOU CAN'T SPEAK").withStyle(ChatFormatting.RED));
    }

    private HijackSpeech() {}
}
