package dev.scriptbound.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.scriptbound.block.entity.RegionBlockEntity;
import dev.scriptbound.region.RegionTracker;
import dev.scriptbound.registry.ModBlocks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class RegionCommands
{
    private static final double REACH = 6D;

    private RegionCommands() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build()
    {
        return Commands.literal("region")
            .then(Commands.literal("set")
                .then(Commands.argument("side", StringArgumentType.word())
                    .then(Commands.argument("trigger", StringArgumentType.greedyString())
                        .executes(ctx -> bindTrigger(ctx.getSource(), StringArgumentType.getString(ctx, "side"), StringArgumentType.getString(ctx, "trigger"))))))
            .then(Commands.literal("radius")
                .then(Commands.argument("x", IntegerArgumentType.integer(1, 64))
                    .then(Commands.argument("y", IntegerArgumentType.integer(1, 64))
                        .then(Commands.argument("z", IntegerArgumentType.integer(1, 64))
                            .executes(ctx -> setRadius(
                                ctx.getSource(),
                                IntegerArgumentType.getInteger(ctx, "x"),
                                IntegerArgumentType.getInteger(ctx, "y"),
                                IntegerArgumentType.getInteger(ctx, "z")
                            ))))));
    }

    private static int bindTrigger(CommandSourceStack source, String side, String trigger)
    {
        ServerPlayer player = source.getPlayer();

        if (player == null)
        {
            source.sendFailure(Component.literal("Only players can configure regions."));
            return 0;
        }

        RegionBlockEntity region = getLookedAtRegion(player);

        if (region == null)
        {
            source.sendFailure(Component.literal("Look at a ScriptBound region block."));
            return 0;
        }

        if ("enter".equalsIgnoreCase(side))
        {
            region.setOnEnter(trigger);
        }
        else if ("exit".equalsIgnoreCase(side))
        {
            region.setOnExit(trigger);
        }
        else
        {
            source.sendFailure(Component.literal("Side must be 'enter' or 'exit'."));
            return 0;
        }

        region.setChanged();
        RegionTracker.resetPlayerAt(player, region.getBlockPos());
        ((ServerLevel) player.level()).sendBlockUpdated(region.getBlockPos(), region.getBlockState(), region.getBlockState(), 3);
        source.sendSuccess(() -> Component.literal("Region " + side + " -> " + trigger + " (re-enter zone to test enter)"), true);
        return 1;
    }

    private static int setRadius(CommandSourceStack source, int x, int y, int z)
    {
        ServerPlayer player = source.getPlayer();

        if (player == null)
        {
            source.sendFailure(Component.literal("Only players can configure regions."));
            return 0;
        }

        RegionBlockEntity region = getLookedAtRegion(player);

        if (region == null)
        {
            source.sendFailure(Component.literal("Look at a ScriptBound region block."));
            return 0;
        }

        region.setRadius(x, y, z);
        region.setChanged();
        source.sendSuccess(() -> Component.literal("Region radius set to " + x + "/" + y + "/" + z), true);
        return 1;
    }

    private static RegionBlockEntity getLookedAtRegion(ServerPlayer player)
    {
        HitResult hit = player.pick(REACH, 0F, false);

        if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult blockHit))
        {
            return null;
        }

        BlockPos pos = blockHit.getBlockPos();

        if (!player.level().getBlockState(pos).is(ModBlocks.REGION_BLOCK.get()))
        {
            return null;
        }

        BlockEntity blockEntity = player.level().getBlockEntity(pos);

        if (blockEntity instanceof RegionBlockEntity regionBlockEntity)
        {
            return regionBlockEntity;
        }

        return null;
    }
}
