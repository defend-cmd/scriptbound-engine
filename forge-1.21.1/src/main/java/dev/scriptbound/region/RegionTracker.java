package dev.scriptbound.region;

import dev.scriptbound.block.entity.RegionBlockEntity;
import dev.scriptbound.trigger.TriggerExecutor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import dev.scriptbound.ScriptBoundMod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ScriptBoundMod.MOD_ID)
public final class RegionTracker
{
    private static final Map<UUID, Set<BlockPos>> INSIDE = new HashMap<>();

    private RegionTracker() {}

    public static void resetPlayer(ServerPlayer player)
    {
        INSIDE.remove(player.getUUID());
    }

    public static void resetPlayerAt(ServerPlayer player, BlockPos pos)
    {
        Set<BlockPos> inside = INSIDE.get(player.getUUID());

        if (inside != null)
        {
            inside.remove(pos);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide)
        {
            return;
        }

        if (event.player.tickCount % 5 != 0 || !(event.player instanceof ServerPlayer player))
        {
            return;
        }

        ServerLevel level = player.serverLevel();
        Set<BlockPos> current = new HashSet<>();
        BlockPos center = player.blockPosition();
        int range = 24;

        for (BlockPos pos : BlockPos.betweenClosed(
            center.offset(-range, -range, -range),
            center.offset(range, range, range)
        ))
        {
            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (!(blockEntity instanceof RegionBlockEntity region))
            {
                continue;
            }

            if (region.getBounds().contains(player.getX(), player.getY(), player.getZ()))
            {
                current.add(region.getBlockPos());
            }
        }

        Set<BlockPos> previous = INSIDE.computeIfAbsent(player.getUUID(), id -> new HashSet<>());

        for (BlockPos entered : current)
        {
            if (!previous.contains(entered))
            {
                BlockEntity blockEntity = level.getBlockEntity(entered);

                if (blockEntity instanceof RegionBlockEntity region)
                {
                    String trigger = region.getOnEnter();

                    if (trigger != null && !trigger.isBlank())
                    {
                        TriggerExecutor.runAtBlock(player.server, trigger, player, level, entered);
                    }
                }
            }
        }

        for (BlockPos exited : previous)
        {
            if (!current.contains(exited))
            {
                BlockEntity blockEntity = level.getBlockEntity(exited);

                if (blockEntity instanceof RegionBlockEntity region)
                {
                    String trigger = region.getOnExit();

                    if (trigger != null && !trigger.isBlank())
                    {
                        TriggerExecutor.runAtBlock(player.server, trigger, player, level, exited);
                    }
                }
            }
        }

        INSIDE.put(player.getUUID(), current);
    }
}
