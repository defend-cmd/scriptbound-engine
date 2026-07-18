package dev.scriptbound.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.scriptbound.ScriptBoundPaths;
import dev.scriptbound.entity.NpcEntity;
import dev.scriptbound.npc.NpcManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class NpcCommands
{
    private NpcCommands() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build()
    {
        return Commands.literal("npc")
            .then(Commands.literal("spawn")
                .then(Commands.argument("id", StringArgumentType.string())
                    .executes(ctx -> spawnFor(ctx.getSource(), ctx.getSource().getPlayer(), StringArgumentType.getString(ctx, "id")))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> spawnFor(
                            ctx.getSource(),
                            EntityArgument.getPlayer(ctx, "player"),
                            StringArgumentType.getString(ctx, "id")
                        )))))
            .then(Commands.literal("list")
                .executes(ctx -> {
                    try (Stream<java.nio.file.Path> files = Files.list(ScriptBoundPaths.npcsDir(ctx.getSource().getServer())))
                    {
                        long count = files
                            .filter(path -> path.toString().endsWith(".json"))
                            .peek(path -> ctx.getSource().sendSuccess(
                                () -> Component.literal("- " + path.getFileName().toString().replace(".json", "")),
                                false
                            ))
                            .count();

                        if (count == 0)
                        {
                            ctx.getSource().sendSuccess(() -> Component.literal("No NPC blueprints in scriptbound/npcs/"), false);
                        }

                        return (int) Math.max(1, count);
                    }
                    catch (IOException e)
                    {
                        ctx.getSource().sendFailure(Component.literal("Could not list NPCs: " + e.getMessage()));
                        return 0;
                    }
                }))
            .then(Commands.literal("despawn")
                .executes(ctx -> despawnNearest(ctx.getSource().getPlayer()))
                .then(Commands.argument("id", StringArgumentType.string())
                    .executes(ctx -> despawnBlueprint(ctx.getSource().getPlayer(), StringArgumentType.getString(ctx, "id")))))
            .then(Commands.literal("patrol")
                .then(Commands.literal("add")
                    .then(Commands.argument("id", StringArgumentType.string())
                        .executes(ctx -> patrolAdd(ctx.getSource().getPlayer(), StringArgumentType.getString(ctx, "id")))))
                .then(Commands.literal("clear")
                    .then(Commands.argument("id", StringArgumentType.string())
                        .executes(ctx -> patrolClear(ctx.getSource().getPlayer(), StringArgumentType.getString(ctx, "id"))))));
    }

    private static int patrolAdd(ServerPlayer player, String id)
    {
        if (player == null)
        {
            return 0;
        }

        com.google.gson.JsonObject root = dev.scriptbound.util.JsonFiles.readObject(ScriptBoundPaths.npcFile(player.server, id));
        com.google.gson.JsonArray patrol = root.has("patrol") && root.get("patrol").isJsonArray()
            ? root.getAsJsonArray("patrol")
            : new com.google.gson.JsonArray();

        com.google.gson.JsonArray point = new com.google.gson.JsonArray();
        point.add(player.blockPosition().getX());
        point.add(player.blockPosition().getY());
        point.add(player.blockPosition().getZ());
        patrol.add(point);
        root.add("patrol", patrol);

        dev.scriptbound.util.JsonFiles.writeObject(ScriptBoundPaths.npcFile(player.server, id), root);
        NpcManager.invalidateAll();
        refreshSpawned(player, id);
        player.sendSystemMessage(Component.literal("Patrol point #" + patrol.size() + " added to NPC '" + id + "' at " + player.blockPosition().toShortString()));
        return 1;
    }

    private static int patrolClear(ServerPlayer player, String id)
    {
        if (player == null)
        {
            return 0;
        }

        com.google.gson.JsonObject root = dev.scriptbound.util.JsonFiles.readObject(ScriptBoundPaths.npcFile(player.server, id));
        root.remove("patrol");
        dev.scriptbound.util.JsonFiles.writeObject(ScriptBoundPaths.npcFile(player.server, id), root);
        NpcManager.invalidateAll();
        refreshSpawned(player, id);
        player.sendSystemMessage(Component.literal("Patrol cleared for NPC '" + id + "'"));
        return 1;
    }

    private static void refreshSpawned(ServerPlayer player, String id)
    {
        List<NpcEntity> npcs = player.serverLevel().getEntitiesOfClass(
            NpcEntity.class,
            new AABB(player.blockPosition()).inflate(128D),
            entity -> id.equals(entity.getBlueprintId())
        );

        for (NpcEntity npc : npcs)
        {
            npc.applyBlueprint(NpcManager.get(player.server, id));
        }
    }

    private static int spawnFor(CommandSourceStack source, ServerPlayer player, String id)
    {
        if (player == null)
        {
            source.sendFailure(Component.literal("Only players can spawn NPCs here."));
            return 0;
        }

        NpcEntity npc = NpcManager.spawnNearPlayer(player, id);

        if (npc == null)
        {
            source.sendFailure(Component.literal("Failed to spawn NPC: " + id));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Spawned NPC '" + id + "'"), true);
        return 1;
    }

    private static int despawnNearest(ServerPlayer player)
    {
        if (player == null)
        {
            return 0;
        }

        List<NpcEntity> npcs = player.serverLevel().getEntitiesOfClass(
            NpcEntity.class,
            new AABB(player.blockPosition()).inflate(16D),
            entity -> true
        );

        if (npcs.isEmpty())
        {
            player.sendSystemMessage(Component.literal("No ScriptBound NPCs nearby."));
            return 0;
        }

        NpcEntity nearest = npcs.stream()
            .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)))
            .orElseThrow();

        String id = nearest.getBlueprintId();
        nearest.discard();
        player.sendSystemMessage(Component.literal("Despawned NPC '" + id + "'"));
        return 1;
    }

    private static int despawnBlueprint(ServerPlayer player, String id)
    {
        if (player == null)
        {
            return 0;
        }

        List<NpcEntity> npcs = player.serverLevel().getEntitiesOfClass(
            NpcEntity.class,
            new AABB(player.blockPosition()).inflate(128D),
            entity -> id.equals(entity.getBlueprintId())
        );

        if (npcs.isEmpty())
        {
            player.sendSystemMessage(Component.literal("No NPC '" + id + "' found nearby."));
            return 0;
        }

        for (NpcEntity npc : npcs)
        {
            npc.discard();
        }

        player.sendSystemMessage(Component.literal("Despawned " + npcs.size() + " NPC(s) '" + id + "'"));
        return npcs.size();
    }
}
