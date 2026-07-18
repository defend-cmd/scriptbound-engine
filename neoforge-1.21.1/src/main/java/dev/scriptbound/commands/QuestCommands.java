package dev.scriptbound.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.scriptbound.quest.QuestManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class QuestCommands
{
    private QuestCommands() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build()
    {
        return Commands.literal("quest")
            .then(Commands.literal("give")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("id", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                            String id = StringArgumentType.getString(ctx, "id");

                            if (!QuestManager.give(player, id))
                            {
                                ctx.getSource().sendFailure(Component.literal("Player already has quest: " + id));
                                return 0;
                            }

                            ctx.getSource().sendSuccess(() -> Component.literal("Gave quest '" + id + "'"), true);
                            return 1;
                        }))))
            .then(Commands.literal("complete")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("id", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                            String id = StringArgumentType.getString(ctx, "id");

                            if (!QuestManager.complete(player, id))
                            {
                                ctx.getSource().sendFailure(Component.literal("Could not complete quest: " + id));
                                return 0;
                            }

                            ctx.getSource().sendSuccess(() -> Component.literal("Completed quest '" + id + "'"), true);
                            return 1;
                        }))))
            .then(Commands.literal("journal")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayer();

                    if (player == null)
                    {
                        ctx.getSource().sendFailure(Component.literal("Only players can open the journal."));
                        return 0;
                    }

                    QuestManager.openJournal(player);
                    return 1;
                })
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> {
                        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                        QuestManager.openJournal(player);
                        ctx.getSource().sendSuccess(() -> Component.literal("Opened journal for " + player.getGameProfile().getName()), true);
                        return 1;
                    })));
    }
}
