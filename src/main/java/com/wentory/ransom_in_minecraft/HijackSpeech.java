package com.wentory.ransom_in_minecraft;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.ServerChatEvent;

/** Server-authoritative silence while the player is fully Hijacked; chat UI stays usable. */
@EventBusSubscriber(modid = RansomInMinecraft.MODID)
public final class HijackSpeech {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void chat(ServerChatEvent event) {
        if (!FullHijack.active(event.getPlayer())) return;
        event.setCanceled(true);
        reject(event.getPlayer());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void command(CommandEvent event) {
        var context = event.getParseResults().getContext();
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)
                || !FullHijack.active(player) || !HijackChatCommands.blocks(context)) return;
        event.setCanceled(true);
        reject(player);
    }

    private static void reject(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("YOU CAN'T SPEAK").withStyle(ChatFormatting.RED));
    }

    private HijackSpeech() {}
}
