package com.wentory.ransom_in_minecraft;

import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.MessageArgument;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Checks the parsed command tree, including redirects and execute subcommands. */
public final class HijackChatCommands {
    private static final Set<String> SPEECH_COMMANDS = Set.of(
            "msg", "w", "tell", "whisper", "pm", "dm", "reply", "r",
            "me", "say", "teammsg", "tm", "tellraw", "broadcast", "announce",
            "chat", "global", "local", "shout", "mail");
    private static final Set<String> MESSAGE_ARGUMENTS = Set.of("message", "msg", "text", "content", "body");

    public static boolean blocks(CommandContextBuilder<?> context) {
        for (CommandContextBuilder<?> current = context; current != null; current = current.getChild()) {
            // A redirect target can identify an alias even when its typed name is unrelated.
            if (speechNode(current.getRootNode(), new HashSet<>())) return true;
            for (var parsed : current.getNodes()) {
                if (speechNode(parsed.getNode(), new HashSet<>())) return true;
            }
        }
        return false;
    }

    private static boolean speechNode(CommandNode<?> node, Set<CommandNode<?>> visited) {
        if (node == null || !visited.add(node)) return false;
        if (node instanceof ArgumentCommandNode<?, ?> argument) {
            if (argument.getType() instanceof MessageArgument
                    || argument.getType() instanceof ComponentArgument) return true;
            if (argument.getType() instanceof StringArgumentType
                    && MESSAGE_ARGUMENTS.contains(argument.getName().toLowerCase(Locale.ROOT))) return true;
        } else {
            String name = node.getName().toLowerCase(Locale.ROOT);
            int namespace = name.lastIndexOf(':');
            if (SPEECH_COMMANDS.contains(name.substring(namespace + 1))) return true;
        }
        return speechNode(node.getRedirect(), visited);
    }

    private HijackChatCommands() {}
}
