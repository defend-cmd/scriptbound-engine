package dev.scriptbound.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.scriptbound.ScriptBoundPaths;
import dev.scriptbound.block.entity.TriggerBlockEntity;
import dev.scriptbound.data.ServerSettings;
import dev.scriptbound.dialogue.DialogueManager;
import dev.scriptbound.npc.NpcManager;
import dev.scriptbound.quest.QuestManager;
import dev.scriptbound.registry.ModBlocks;
import dev.scriptbound.trigger.TriggerExecutor;
import dev.scriptbound.trigger.TriggerManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;

public final class TriggerCommands
{
    private static final double BLOCK_REACH = 6.0D;

    private TriggerCommands() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build()
    {
        return Commands.literal("trigger")
            .then(Commands.literal("run")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("id", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                            String id = StringArgumentType.getString(ctx, "id");
                            boolean ok = TriggerExecutor.run(player.server, id, player);

                            if (!ok)
                            {
                                ctx.getSource().sendFailure(Component.literal("Trigger not found or empty: " + id));
                                return 0;
                            }

                            ctx.getSource().sendSuccess(
                                () -> Component.literal("Ran trigger '" + id + "' on " + player.getGameProfile().getName()),
                                true
                            );
                            return 1;
                        }))))
            .then(Commands.literal("reload")
                .executes(ctx -> {
                    TriggerManager.invalidateAll();
                    ServerSettings.load(ctx.getSource().getServer());
                    DialogueManager.invalidateAll();
                    NpcManager.invalidateAll();
                    QuestManager.invalidateAll();
                    ctx.getSource().sendSuccess(() -> Component.literal("Reloaded ScriptBound triggers and settings."), true);
                    return 1;
                }))
            .then(Commands.literal("list")
                .executes(ctx -> {
                    try (Stream<java.nio.file.Path> files = Files.list(ScriptBoundPaths.triggersDir(ctx.getSource().getServer())))
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
                            ctx.getSource().sendSuccess(
                                () -> Component.literal("No triggers in scriptbound/triggers/"),
                                false
                            );
                        }

                        return (int) Math.max(1, count);
                    }
                    catch (IOException e)
                    {
                        ctx.getSource().sendFailure(Component.literal("Could not list triggers: " + e.getMessage()));
                        return 0;
                    }
                }))
            .then(Commands.literal("block")
                .then(Commands.literal("set")
                    .then(Commands.argument("side", StringArgumentType.word())
                        .then(Commands.argument("id", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                ServerPlayer player = ctx.getSource().getPlayer();

                                if (player == null)
                                {
                                    ctx.getSource().sendFailure(Component.literal("Only players can bind trigger blocks."));
                                    return 0;
                                }

                                String side = StringArgumentType.getString(ctx, "side").toLowerCase();
                                String id = StringArgumentType.getString(ctx, "id");

                                if (!side.equals("left") && !side.equals("right"))
                                {
                                    ctx.getSource().sendFailure(Component.literal("Side must be 'left' or 'right'."));
                                    return 0;
                                }

                                TriggerBlockEntity blockEntity = getLookedAtTriggerBlock(player);

                                if (blockEntity == null)
                                {
                                    ctx.getSource().sendFailure(Component.literal("Look at a ScriptBound trigger block."));
                                    return 0;
                                }

                                if (side.equals("left"))
                                {
                                    blockEntity.setLeftTrigger(id);
                                }
                                else
                                {
                                    blockEntity.setRightTrigger(id);
                                }

                                blockEntity.setChanged();
                                ((ServerLevel) player.level()).sendBlockUpdated(
                                    blockEntity.getBlockPos(),
                                    blockEntity.getBlockState(),
                                    blockEntity.getBlockState(),
                                    3
                                );

                                ctx.getSource().sendSuccess(
                                    () -> Component.literal("Bound " + side + " trigger to '" + id + "'"),
                                    true
                                );
                                return 1;
                            })))));
    }

    private static TriggerBlockEntity getLookedAtTriggerBlock(ServerPlayer player)
    {
        HitResult hit = player.pick(BLOCK_REACH, 0.0F, false);

        if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult blockHit))
        {
            return null;
        }

        BlockPos pos = blockHit.getBlockPos();

        if (!player.level().getBlockState(pos).is(ModBlocks.TRIGGER_BLOCK.get()))
        {
            return null;
        }

        BlockEntity blockEntity = player.level().getBlockEntity(pos);

        if (blockEntity instanceof TriggerBlockEntity triggerBlockEntity)
        {
            return triggerBlockEntity;
        }

        return null;
    }
}
