package dev.scriptbound.quest;

import com.google.gson.JsonObject;
import dev.scriptbound.ScriptBoundPaths;
import dev.scriptbound.data.StateService;
import dev.scriptbound.data.StateValue;
import dev.scriptbound.network.NetworkHandler;
import dev.scriptbound.trigger.TriggerExecutor;
import dev.scriptbound.util.JsonFiles;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import dev.scriptbound.ScriptBoundMod;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = ScriptBoundMod.MOD_ID)
public final class QuestManager
{
    private static final Map<String, QuestDefinition> CACHE = new HashMap<>();

    private QuestManager() {}

    public static QuestDefinition get(MinecraftServer server, String id)
    {
        String key = normalize(id);

        if (CACHE.containsKey(key))
        {
            return CACHE.get(key);
        }

        JsonObject root = JsonFiles.readObject(ScriptBoundPaths.questFile(server, key));
        QuestDefinition definition = QuestDefinition.fromJson(key, root);
        CACHE.put(key, definition);
        return definition;
    }

    public static void invalidateAll()
    {
        CACHE.clear();
    }

    public static boolean give(ServerPlayer player, String questId)
    {
        QuestDefinition definition = get(player.server, questId);
        QuestProgress progress = QuestCapability.get(player);

        if (progress.has(questId))
        {
            return false;
        }

        progress.add(questId);
        player.sendSystemMessage(Component.literal("Quest started: " + definition.title()));
        return true;
    }

    public static boolean complete(ServerPlayer player, String questId)
    {
        QuestProgress progress = QuestCapability.get(player);

        if (!progress.has(questId))
        {
            return false;
        }

        QuestDefinition definition = get(player.server, questId);

        if (!isComplete(player, definition, progress.get(questId)))
        {
            player.sendSystemMessage(Component.literal("Quest objectives not complete."));
            return false;
        }

        progress.remove(questId);
        StateService.set(StateService.playerTarget(player), "quests." + questId, new StateValue.NumberValue(1));

        if (definition.rewardTrigger() != null && !definition.rewardTrigger().isBlank())
        {
            TriggerExecutor.run(player.server, definition.rewardTrigger(), player);
        }

        player.sendSystemMessage(Component.literal("Quest completed: " + definition.title()));
        return true;
    }

    public static boolean isComplete(ServerPlayer player, QuestDefinition definition, QuestProgress.ActiveQuest active)
    {
        for (QuestDefinition.Objective objective : definition.objectives())
        {
            if (!isObjectiveComplete(player, objective, active))
            {
                return false;
            }
        }

        return true;
    }

    private static boolean isObjectiveComplete(ServerPlayer player, QuestDefinition.Objective objective, QuestProgress.ActiveQuest active)
    {
        return switch (objective.type().toLowerCase())
        {
            case "kill" -> active.getKillCount(objective.entity()) >= objective.count();
            case "state" -> {
                StateValue value = StateService.playerTarget(player).store().get(objective.key());
                yield value != null && value.isNumber() && value.asNumber() >= objective.value();
            }
            case "talk" -> {
                StateValue value = StateService.playerTarget(player).store().get("talked." + objective.npcId());
                yield value != null;
            }
            default -> false;
        };
    }

    @SubscribeEvent
    public static void onEntityKill(LivingDeathEvent event)
    {
        if (event.getEntity().level().isClientSide)
        {
            return;
        }

        Entity source = event.getSource().getEntity();

        if (!(source instanceof ServerPlayer player))
        {
            return;
        }

        LivingEntity victim = event.getEntity();
        String entityId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).toString();
        QuestProgress progress = QuestCapability.get(player);

        for (String questId : progress.ids())
        {
            QuestDefinition definition = get(player.server, questId);
            QuestProgress.ActiveQuest active = progress.get(questId);

            for (QuestDefinition.Objective objective : definition.objectives())
            {
                if ("kill".equalsIgnoreCase(objective.type()) && objective.entity().equals(entityId))
                {
                    active.addKill(entityId);

                    if (isComplete(player, definition, active))
                    {
                        player.sendSystemMessage(Component.literal("Quest ready to complete: " + definition.title()));
                    }
                }
            }
        }
    }

    public static void openJournal(ServerPlayer player)
    {
        NetworkHandler.sendQuestJournal(player);
    }

    private static String normalize(String id)
    {
        String trimmed = id.trim();

        if (trimmed.endsWith(".json"))
        {
            return trimmed.substring(0, trimmed.length() - 5);
        }

        return trimmed;
    }
}
