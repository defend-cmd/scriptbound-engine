package dev.scriptbound.trigger.actions;

import com.google.gson.JsonObject;
import dev.scriptbound.trigger.TriggerContext;
import dev.scriptbound.trigger.TriggerExecutor;
import dev.scriptbound.util.ServerScheduler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public record DelayAction(int ticks, String trigger) implements TriggerAction
{
    public static DelayAction fromJson(JsonObject json)
    {
        int ticks = json.has("ticks") ? json.get("ticks").getAsInt() : 20;
        String trigger = json.has("trigger") ? json.get("trigger").getAsString() : "";
        return new DelayAction(ticks, trigger);
    }

    @Override
    public String type()
    {
        return "delay";
    }

    @Override
    public void execute(TriggerContext context)
    {
        if (this.trigger.isBlank())
        {
            return;
        }

        MinecraftServer server = context.player().server;
        UUID playerId = context.player().getUUID();
        String triggerId = this.trigger;

        ServerScheduler.schedule(this.ticks, () -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);

            if (player != null)
            {
                TriggerContext newContext = new TriggerContext(
                    player,
                    context.level(),
                    context.blockPos(),
                    context.chatMessage(),
                    context.npc(),
                    0
                );
                TriggerExecutor.run(server, triggerId, newContext);
            }
        });
    }
}
