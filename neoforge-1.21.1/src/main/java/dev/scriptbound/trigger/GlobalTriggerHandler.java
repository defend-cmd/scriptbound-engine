package dev.scriptbound.trigger;

import net.neoforged.fml.common.EventBusSubscriber;

import dev.scriptbound.block.entity.TriggerBlockEntity;
import dev.scriptbound.data.ServerSettings;
import dev.scriptbound.entity.NpcEntity;
import dev.scriptbound.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import dev.scriptbound.ScriptBoundMod;

public final class GlobalTriggerHandler
{
    private GlobalTriggerHandler() {}

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }

        String triggerId = ServerSettings.globalTrigger("player_join");

        if (triggerId != null && !triggerId.isBlank())
        {
            TriggerExecutor.run(player.getServer(), triggerId, player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChat(ServerChatEvent event)
    {
        ServerPlayer player = event.getPlayer();

        String triggerId = ServerSettings.globalTrigger("player_chat");

        if (triggerId == null || triggerId.isBlank())
        {
            return;
        }

        TriggerExecutor.run(
            player.getServer(),
            triggerId,
            new TriggerContext(player, player.serverLevel(), null, event.getRawText(), null, 0)
        );
    }

    @SubscribeEvent
    public static void onPlayerDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }

        String triggerId = ServerSettings.globalTrigger("player_death");

        if (triggerId != null && !triggerId.isBlank())
        {
            TriggerExecutor.run(player.getServer(), triggerId, player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }

        String triggerId = ServerSettings.globalTrigger("player_respawn");

        if (triggerId != null && !triggerId.isBlank())
        {
            TriggerExecutor.run(player.getServer(), triggerId, player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event)
    {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level))
        {
            return;
        }

        BlockPos pos = event.getPos();

        if (!level.getBlockState(pos).is(ModBlocks.TRIGGER_BLOCK.get()))
        {
            return;
        }

        if (level.getBlockEntity(pos) instanceof TriggerBlockEntity blockEntity)
        {
            String triggerId = blockEntity.getLeftTrigger();

            if (triggerId != null && !triggerId.isBlank())
            {
                boolean ok = TriggerExecutor.runAtBlock(player.server, triggerId, player, level, pos);

                if (ok)
                {
                    event.setCanceled(true);
                }
                else
                {
                    player.sendSystemMessage(Component.literal("[SBE] Left trigger '" + triggerId + "' not found or empty."));
                }
            }
            else
            {
                player.sendSystemMessage(Component.literal("[SBE] No left trigger bound. Use /sbe trigger block set left <id>"));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event)
    {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }

        if (!(event.getTarget() instanceof NpcEntity npc))
        {
            return;
        }

        if (!npc.level().isClientSide())
        {
            npc.handlePlayerInteract(player);
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        }
    }
}
