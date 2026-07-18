package dev.scriptbound.ui;

import dev.scriptbound.ScriptBoundMod;
import dev.scriptbound.data.GlobalStateManager;
import dev.scriptbound.data.PlayerStateCapability;
import dev.scriptbound.data.StateValue;
import dev.scriptbound.network.NetworkHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = ScriptBoundMod.MOD_ID)
public final class UiSession
{
    private static final Map<UUID, Set<String>> OPEN = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, String>> LAST_SENT = new ConcurrentHashMap<>();
    private static int counter;

    private UiSession() {}

    public static void open(ServerPlayer player, String id)
    {
        OPEN.computeIfAbsent(player.getUUID(), k -> ConcurrentHashMap.newKeySet()).add(id);
    }

    public static void close(ServerPlayer player, String id)
    {
        Set<String> set = OPEN.get(player.getUUID());

        if (set == null)
        {
            return;
        }

        if ("*".equals(id))
        {
            set.clear();
        }
        else
        {
            set.remove(id);
        }

        LAST_SENT.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        OPEN.remove(event.getEntity().getUUID());
        LAST_SENT.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        if (++counter % 5 != 0)
        {
            return;
        }

        MinecraftServer server = event.getServer();

        if (server == null)
        {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            pushFor(player);
        }
    }

    private static void pushFor(ServerPlayer player)
    {
        Set<String> openIds = OPEN.get(player.getUUID());

        if (openIds == null || openIds.isEmpty())
        {
            return;
        }

        Map<String, String> values = new HashMap<>();

        for (String id : openIds)
        {
            UiDefinition def = UiManager.get(id);

            if (def == null)
            {
                continue;
            }

            for (String boundKey : UiBindings.collect(def))
            {
                values.put(boundKey, readValue(player, boundKey));
            }
        }

        if (values.isEmpty() || values.equals(LAST_SENT.get(player.getUUID())))
        {
            return;
        }

        LAST_SENT.put(player.getUUID(), values);
        NetworkHandler.sendUiState(player, values);
    }

    private static String readValue(ServerPlayer player, String scopeKey)
    {
        int colon = scopeKey.indexOf(':');

        if (colon < 0)
        {
            return "";
        }

        String scope = scopeKey.substring(0, colon);
        String key = scopeKey.substring(colon + 1);

        StateValue value = "global".equals(scope)
            ? GlobalStateManager.global().get(key)
            : PlayerStateCapability.get(player).get(key);

        if (value == null)
        {
            return "";
        }

        if (value.isNumber())
        {
            double n = value.asNumber();
            return n == Math.rint(n) && !Double.isInfinite(n) ? String.valueOf((long) n) : String.valueOf(n);
        }

        return value.asString();
    }
}
