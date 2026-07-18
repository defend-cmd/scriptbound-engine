package dev.scriptbound.script;

import dev.scriptbound.data.StateService;
import dev.scriptbound.data.StateValue;
import dev.scriptbound.trigger.TriggerExecutor;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ScriptContext
{
    private final ServerPlayer player;

    public ScriptContext(ServerPlayer player)
    {
        this.player = player;
    }

    public void send(String message)
    {
        this.player.sendSystemMessage(Component.literal(message));
    }

    public void command(String command)
    {
        this.player.server.getCommands().performPrefixedCommand(this.player.createCommandSourceStack().withPermission(2), command);
    }

    public double getState(String scope, String key)
    {
        StateValue value = StateService.resolveStore(this.player.createCommandSourceStack(), normalizeScope(scope)).get(key);
        return value != null && value.isNumber() ? value.asNumber() : 0D;
    }

    public String getStateString(String scope, String key)
    {
        StateValue value = StateService.resolveStore(this.player.createCommandSourceStack(), normalizeScope(scope)).get(key);
        return value != null ? value.asString() : "";
    }

    public void setState(String scope, String key, double value)
    {
        StateService.Target target = StateService.resolveTarget(this.player.createCommandSourceStack(), normalizeScope(scope));
        StateService.set(target, key, new StateValue.NumberValue(value));
    }

    public void setStateString(String scope, String key, String value)
    {
        StateService.Target target = StateService.resolveTarget(this.player.createCommandSourceStack(), normalizeScope(scope));
        StateService.set(target, key, new StateValue.StringValue(value));
    }

    public void addState(String scope, String key, double delta)
    {
        StateService.Target target = StateService.resolveTarget(this.player.createCommandSourceStack(), normalizeScope(scope));
        StateService.add(target, key, delta);
    }

    public void runTrigger(String triggerId)
    {
        TriggerExecutor.run(this.player.server, triggerId, this.player);
    }

    public void teleport(double x, double y, double z)
    {
        this.player.teleportTo(x, y, z);
    }

    public void removeState(String scope, String key)
    {
        StateService.Target target = StateService.resolveTarget(this.player.createCommandSourceStack(), normalizeScope(scope));
        StateService.remove(target, key);
    }

    public boolean hasState(String scope, String key)
    {
        return StateService.resolveStore(this.player.createCommandSourceStack(), normalizeScope(scope)).get(key) != null;
    }

    public void actionbar(String message)
    {
        this.player.displayClientMessage(Component.literal(message), true);
    }

    public void giveItem(String itemId, int count)
    {
        net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(itemId);

        if (id == null)
        {
            return;
        }

        net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(id);

        if (item == null || item == net.minecraft.world.item.Items.AIR)
        {
            return;
        }

        net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item, Math.max(1, count));

        if (!this.player.getInventory().add(stack))
        {
            this.player.drop(stack, false);
        }
    }

    public void playSound(String soundId, double volume)
    {
        net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(soundId);

        if (id == null)
        {
            return;
        }

        net.minecraft.sounds.SoundEvent event = net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(id);

        if (event == null)
        {
            return;
        }

        this.player.serverLevel().playSound(
            null,
            this.player.getX(),
            this.player.getY(),
            this.player.getZ(),
            event,
            net.minecraft.sounds.SoundSource.MASTER,
            (float) volume,
            1.0F
        );
    }

    public double getFaction(String factionId)
    {
        return dev.scriptbound.faction.FactionManager.score(this.player, factionId);
    }

    public void addFaction(String factionId, double delta)
    {
        dev.scriptbound.faction.FactionManager.addScore(this.player, factionId, delta);
    }

    public void setFaction(String factionId, double score)
    {
        dev.scriptbound.faction.FactionManager.setScore(this.player, factionId, score);
    }

    public String getFactionAttitude(String factionId)
    {
        return dev.scriptbound.faction.FactionManager.attitude(this.player, factionId).name().toLowerCase();
    }

    public void giveQuest(String questId)
    {
        dev.scriptbound.quest.QuestManager.give(this.player, questId);
    }

    public void completeQuest(String questId)
    {
        dev.scriptbound.quest.QuestManager.complete(this.player, questId);
    }

    public void spawnNpc(String npcId)
    {
        dev.scriptbound.npc.NpcManager.spawnNearPlayer(this.player, npcId);
    }

    public void dialogue(String dialogueId)
    {
        dev.scriptbound.dialogue.DialogueManager.open(this.player, dialogueId);
    }

    public void delayTrigger(int ticks, String triggerId)
    {
        net.minecraft.server.MinecraftServer server = this.player.server;
        java.util.UUID playerId = this.player.getUUID();

        dev.scriptbound.util.ServerScheduler.schedule(ticks, () -> {
            ServerPlayer target = server.getPlayerList().getPlayer(playerId);

            if (target != null)
            {
                TriggerExecutor.run(server, triggerId, target);
            }
        });
    }

    public void openUI(String id)
    {
        com.google.gson.JsonObject json = dev.scriptbound.ui.UiManager.getJson(id);

        if (json != null)
        {
            dev.scriptbound.network.NetworkHandler.sendOpenUi(this.player, id, json.toString());
        }
    }

    public void closeUI(String id)
    {
        dev.scriptbound.network.NetworkHandler.sendCloseUi(this.player, id == null || id.isBlank() ? "*" : id);
    }

    public void uiSet(String ui, String element, String field, String value)
    {
        dev.scriptbound.network.NetworkHandler.sendUiSet(this.player, ui, element, field, value);
    }

    public void uiText(String ui, String element, String text)
    {
        dev.scriptbound.network.NetworkHandler.sendUiSet(this.player, ui, element, "text", text);
    }

    public void uiValue(String ui, String element, double value)
    {
        dev.scriptbound.network.NetworkHandler.sendUiSet(this.player, ui, element, "value", String.valueOf(value));
    }

    public void uiVisible(String ui, String element, boolean visible)
    {
        dev.scriptbound.network.NetworkHandler.sendUiSet(this.player, ui, element, "visible", String.valueOf(visible));
    }

    public void log(String message)
    {
        dev.scriptbound.ScriptBoundMod.LOGGER.info("[Script:{}] {}", this.player.getGameProfile().getName(), message);
    }

    private static String normalizeScope(String scope)
    {
        if (scope == null || scope.isBlank() || "player".equalsIgnoreCase(scope) || "p".equalsIgnoreCase(scope))
        {
            return "@p";
        }

        if ("global".equalsIgnoreCase(scope))
        {
            return "~";
        }

        return scope;
    }
}
