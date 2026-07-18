package dev.scriptbound.data;

import com.google.gson.JsonObject;
import dev.scriptbound.ScriptBoundMod;
import dev.scriptbound.ScriptBoundPaths;
import dev.scriptbound.util.JsonFiles;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.concurrent.atomic.AtomicBoolean;

public final class GlobalStateManager
{
    private static final StateStore GLOBAL = new StateStore();
    private static MinecraftServer server;
    private static final AtomicBoolean dirty = new AtomicBoolean(false);
    private static int saveCooldown;

    private GlobalStateManager() {}

    public static StateStore global()
    {
        return GLOBAL;
    }

    public static void onServerStarted(MinecraftServer minecraftServer)
    {
        server = minecraftServer;
        GLOBAL.loadFromJson(JsonFiles.readObject(ScriptBoundPaths.globalStatesFile(minecraftServer)));
        ScriptBoundMod.LOGGER.info("Loaded {} global state(s).", GLOBAL.keys().size());
    }

    public static void onServerStopping(MinecraftServer minecraftServer)
    {
        flush(minecraftServer);
        server = null;
    }

    public static void markDirty()
    {
        dirty.set(true);
    }

    @SubscribeEvent
    public static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event)
    {
        if (server == null || !dirty.get())
        {
            return;
        }

        if (--saveCooldown > 0)
        {
            return;
        }

        flush(server);
    }

    public static void flush(MinecraftServer minecraftServer)
    {
        JsonFiles.writeObject(ScriptBoundPaths.globalStatesFile(minecraftServer), GLOBAL.saveToJson());
        dirty.set(false);
        saveCooldown = 100;
    }
}
