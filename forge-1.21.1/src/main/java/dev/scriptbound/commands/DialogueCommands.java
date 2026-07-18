package dev.scriptbound.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.scriptbound.dialogue.DialogueManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class DialogueCommands
{
    private DialogueCommands() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build()
    {
        return Commands.literal("dialogue")
            .then(Commands.literal("open")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("id", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                            String id = StringArgumentType.getString(ctx, "id");
                            if (!DialogueManager.open(player, id))
                            {
                                ctx.getSource().sendFailure(Component.literal("Could not open dialogue '" + id + "'"));
                                return 0;
                            }

                            ctx.getSource().sendSuccess(() -> Component.literal("Opened dialogue '" + id + "'"), true);
                            return 1;
                        }))));
    }
}
