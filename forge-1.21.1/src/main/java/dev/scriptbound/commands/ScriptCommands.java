package dev.scriptbound.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.scriptbound.script.ScriptManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class ScriptCommands
{
    private ScriptCommands() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build()
    {
        return Commands.literal("script")
            .then(Commands.literal("run")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("id", StringArgumentType.string())
                        .executes(ctx -> {
                            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                            String id = StringArgumentType.getString(ctx, "id");
                            ScriptManager.Result result = ScriptManager.run(player.server, id, player, "main");

                            if (!result.success())
                            {
                                ctx.getSource().sendFailure(Component.literal(result.message()));
                                player.sendSystemMessage(Component.literal(result.message()));
                                return 0;
                            }

                            ctx.getSource().sendSuccess(() -> Component.literal(result.message()), true);
                            player.sendSystemMessage(Component.literal(result.message()));
                            return 1;
                        })
                        .then(Commands.argument("function", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                                String id = StringArgumentType.getString(ctx, "id");
                                String function = StringArgumentType.getString(ctx, "function");
                                ScriptManager.Result result = ScriptManager.run(player.server, id, player, function);

                                if (!result.success())
                                {
                                    ctx.getSource().sendFailure(Component.literal(result.message()));
                                    player.sendSystemMessage(Component.literal(result.message()));
                                    return 0;
                                }

                                ctx.getSource().sendSuccess(() -> Component.literal(result.message()), true);
                                player.sendSystemMessage(Component.literal(result.message()));
                                return 1;
                            })))));
    }
}
