package com.wentory.ransom_in_minecraft;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.arguments.MessageArgument;

public final class HijackChatCommandsTest {
    public static void main(String[] args) {
        var dispatcher = new CommandDispatcher<String>();
        for (String name : new String[]{"msg", "w", "tell", "minecraft:msg", "teammsg", "me", "say", "tellraw", "reply"}) {
            dispatcher.register(com.mojang.brigadier.builder.LiteralArgumentBuilder.<String>literal(name)
                    .then(com.mojang.brigadier.builder.RequiredArgumentBuilder.<String,String>argument("text", StringArgumentType.greedyString()).executes(c -> 1)));
            check(dispatcher, name+" hello", true);
        }
        var message = dispatcher.getRoot().getChild("msg");
        dispatcher.register(com.mojang.brigadier.builder.LiteralArgumentBuilder.<String>literal("alias").redirect(message));
        check(dispatcher, "alias hello", true);
        dispatcher.register(com.mojang.brigadier.builder.LiteralArgumentBuilder.<String>literal("custom")
                .then(com.mojang.brigadier.builder.RequiredArgumentBuilder.<String,MessageArgument.Message>argument("message", MessageArgument.message()).executes(c -> 1)));
        check(dispatcher, "custom hello", true);
        dispatcher.register(com.mojang.brigadier.builder.LiteralArgumentBuilder.<String>literal("customprivate")
                .then(com.mojang.brigadier.builder.RequiredArgumentBuilder.<String,String>argument("message", StringArgumentType.greedyString()).executes(c -> 1)));
        check(dispatcher, "customprivate hello", true);
        dispatcher.register(com.mojang.brigadier.builder.LiteralArgumentBuilder.<String>literal("execute")
                .then(com.mojang.brigadier.builder.LiteralArgumentBuilder.<String>literal("run").redirect(dispatcher.getRoot())));
        check(dispatcher, "execute run minecraft:msg hello", true);
        for (String name : new String[]{"ransom", "effect", "tp", "gamemode", "give"}) {
            dispatcher.register(com.mojang.brigadier.builder.LiteralArgumentBuilder.<String>literal(name)
                    .then(com.mojang.brigadier.builder.RequiredArgumentBuilder.<String,String>argument("arguments", StringArgumentType.greedyString()).executes(c -> 1)));
            check(dispatcher, name+" test", false);
        }
        check(dispatcher, "execute run effect test", false);
        check(dispatcher, "ransom hijack set Player 0", false);
        System.out.println("Hijack speech: chat commands, namespaces, redirects, custom message arguments, execute and technical commands passed");
    }

    private static void check(CommandDispatcher<String> dispatcher, String command, boolean blocked) {
        var parsed = dispatcher.parse(command, "test");
        if (parsed.getReader().canRead()) throw new AssertionError("Incomplete test parse: "+command);
        if (HijackChatCommands.blocks(parsed.getContext()) != blocked) throw new AssertionError(command);
    }
}
