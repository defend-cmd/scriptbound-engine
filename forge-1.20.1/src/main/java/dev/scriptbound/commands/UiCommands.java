package dev.scriptbound.commands;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.scriptbound.network.NetworkHandler;
import dev.scriptbound.ui.UiManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class UiCommands
{
    private UiCommands() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build()
    {
        return Commands.literal("ui")
            .then(Commands.literal("open")
                .then(Commands.argument("id", StringArgumentType.string())
                    .executes(ctx -> open(ctx, ctx.getSource().getPlayerOrException()))
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> open(ctx, EntityArgument.getPlayer(ctx, "target"))))))
            .then(Commands.literal("close")
                .then(Commands.argument("id", StringArgumentType.string())
                    .executes(ctx -> close(ctx, ctx.getSource().getPlayerOrException()))
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> close(ctx, EntityArgument.getPlayer(ctx, "target"))))))
            .then(Commands.literal("edit")
                .then(Commands.argument("id", StringArgumentType.string())
                    .executes(ctx -> edit(ctx))))
            .then(Commands.literal("list")
                .executes(ctx -> {
                    var ids = UiManager.list();
                    ctx.getSource().sendSuccess(
                        () -> Component.literal(ids.isEmpty() ? "No UI documents." : "UI: " + String.join(", ", ids)),
                        false
                    );
                    return 1;
                }));
    }

    private static int open(CommandContext<CommandSourceStack> ctx, ServerPlayer target) throws com.mojang.brigadier.exceptions.CommandSyntaxException
    {
        String id = StringArgumentType.getString(ctx, "id");
        JsonObject json = UiManager.getJson(id);

        if (json == null)
        {
            ctx.getSource().sendFailure(Component.literal("UI not found: scriptbound/ui/" + id + ".json"));
            return 0;
        }

        NetworkHandler.sendOpenUi(target, id, json.toString());
        ctx.getSource().sendSuccess(() -> Component.literal("Opened UI '" + id + "' for " + target.getGameProfile().getName()), false);
        return 1;
    }

    private static int edit(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException
    {
        String id = StringArgumentType.getString(ctx, "id");
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        JsonObject json = UiManager.getJson(id);

        if (json == null)
        {

            json = new JsonObject();
            json.addProperty("mode", "hud");
            json.add("elements", new com.google.gson.JsonArray());
        }

        dev.scriptbound.network.NetworkHandler.CHANNEL.send(
            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
            new dev.scriptbound.network.packet.EditUiPacket(id, json.toString())
        );
        return 1;
    }

    private static int close(CommandContext<CommandSourceStack> ctx, ServerPlayer target)
    {
        String id = StringArgumentType.getString(ctx, "id");
        NetworkHandler.sendCloseUi(target, id);
        ctx.getSource().sendSuccess(() -> Component.literal("Closed UI '" + id + "' for " + target.getGameProfile().getName()), false);
        return 1;
    }
}
