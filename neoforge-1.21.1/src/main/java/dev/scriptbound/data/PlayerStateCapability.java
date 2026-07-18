package dev.scriptbound.data;

import dev.scriptbound.ScriptBoundMod;
import dev.scriptbound.ScriptBoundPaths;
import dev.scriptbound.util.JsonFiles;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ScriptBoundMod.MOD_ID)
public final class PlayerStateCapability {
    private PlayerStateCapability() {}

    public static StateStore get(Player player) {
        return player.getData(dev.scriptbound.ScriptBoundAttachments.PLAYER_STATE);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        StateStore store = get(player);
        com.google.gson.JsonObject file = JsonFiles.readObject(ScriptBoundPaths.playerStatesFile(player.server, player.getUUID()));
        if (store.keys().isEmpty() && file.has("states")) {
            store.loadFromJson(file);
            if (!store.keys().isEmpty()) {
                ScriptBoundMod.LOGGER.debug("Imported {} player state(s) from file for {}", store.keys().size(), player.getGameProfile().getName());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        JsonFiles.writeObject(ScriptBoundPaths.playerStatesFile(player.server, player.getUUID()), get(player).saveToJson());
    }
}
