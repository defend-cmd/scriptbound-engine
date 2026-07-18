package dev.scriptbound.checker;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.scriptbound.data.StateService;
import dev.scriptbound.data.StateValue;
import dev.scriptbound.quest.QuestCapability;
import dev.scriptbound.quest.QuestDefinition;
import dev.scriptbound.quest.QuestManager;
import dev.scriptbound.trigger.TriggerContext;

import java.util.ArrayList;
import java.util.List;

public final class CheckerParser
{
    private CheckerParser() {}

    public static Checker parse(JsonElement element)
    {
        if (element == null || !element.isJsonObject())
        {
            return context -> true;
        }

        JsonObject json = element.getAsJsonObject();
        String type = json.has("type") ? json.get("type").getAsString().toLowerCase() : "state";

        return switch (type)
        {
            case "and" -> parseComposite(json, true);
            case "or" -> parseComposite(json, false);
            case "not" -> parseNot(json);
            case "talk" -> parseTalk(json);
            case "quest" -> parseQuest(json);
            case "faction" -> parseFaction(json);
            case "state_absent" -> parseState(json, "absent");
            case "state_present" -> parseState(json, "present");
            default -> parseState(json, json.has("op") ? json.get("op").getAsString() : "gte");
        };
    }

    private static Checker parseComposite(JsonObject json, boolean and)
    {
        List<Checker> checks = new ArrayList<>();

        if (json.has("checks") && json.get("checks").isJsonArray())
        {
            for (JsonElement child : json.getAsJsonArray("checks"))
            {
                checks.add(parse(child));
            }
        }

        if (checks.isEmpty())
        {
            return context -> true;
        }

        return and ? new AndChecker(checks) : new OrChecker(checks);
    }

    private static Checker parseNot(JsonObject json)
    {
        Checker inner = json.has("check") ? parse(json.get("check")) : context -> false;
        return context -> !inner.test(context);
    }

    private static Checker parseTalk(JsonObject json)
    {
        String npc = json.has("npc") ? json.get("npc").getAsString()
            : (json.has("npcId") ? json.get("npcId").getAsString() : "");
        return new TalkChecker(npc);
    }

    private static Checker parseQuest(JsonObject json)
    {
        String questId = json.has("quest") ? json.get("quest").getAsString()
            : (json.has("id") ? json.get("id").getAsString() : "");
        String status = json.has("status") ? json.get("status").getAsString() : "active";
        return new QuestChecker(questId, status);
    }

    private static Checker parseFaction(JsonObject json)
    {
        String faction = json.has("faction") ? json.get("faction").getAsString() : "";
        String attitude = json.has("attitude") ? json.get("attitude").getAsString() : "friendly";
        return new FactionChecker(faction, attitude);
    }

    private static Checker parseState(JsonObject json, String op)
    {
        String scope = json.has("scope") ? json.get("scope").getAsString() : "player";
        String key = json.has("key") ? json.get("key").getAsString() : "";
        double value = json.has("value") ? json.get("value").getAsDouble() : 1D;
        return new StateChecker(scope, key, op, value);
    }

    public static final class AndChecker implements Checker
    {
        private final List<Checker> checks;

        public AndChecker(List<Checker> checks)
        {
            this.checks = List.copyOf(checks);
        }

        @Override
        public boolean test(TriggerContext context)
        {
            for (Checker check : this.checks)
            {
                if (!check.test(context))
                {
                    return false;
                }
            }

            return true;
        }
    }

    public static final class OrChecker implements Checker
    {
        private final List<Checker> checks;

        public OrChecker(List<Checker> checks)
        {
            this.checks = List.copyOf(checks);
        }

        @Override
        public boolean test(TriggerContext context)
        {
            for (Checker check : this.checks)
            {
                if (check.test(context))
                {
                    return true;
                }
            }

            return false;
        }
    }

    public static final class TalkChecker implements Checker
    {
        private final String npcId;

        public TalkChecker(String npcId)
        {
            this.npcId = npcId;
        }

        @Override
        public boolean test(TriggerContext context)
        {
            if (this.npcId.isBlank())
            {
                return false;
            }

            StateValue value = StateService.playerTarget(context.player()).store().get("talked." + this.npcId);
            return value != null;
        }
    }

    public static final class QuestChecker implements Checker
    {
        private final String questId;
        private final String status;

        public QuestChecker(String questId, String status)
        {
            this.questId = questId;
            this.status = status == null ? "active" : status.toLowerCase();
        }

        @Override
        public boolean test(TriggerContext context)
        {
            if (this.questId.isBlank())
            {
                return false;
            }

            boolean active = QuestCapability.get(context.player()).has(this.questId);

            return switch (this.status)
            {
                case "active" -> active;
                case "not_started", "absent" -> !active && !isCompleted(context);
                case "ready" -> active && isReady(context);
                case "complete", "completed" -> isCompleted(context) || (active && isReady(context));
                default -> active;
            };
        }

        private boolean isCompleted(TriggerContext context)
        {
            StateValue value = StateService.playerTarget(context.player()).store().get("quests." + this.questId);
            return value != null;
        }

        private boolean isReady(TriggerContext context)
        {
            if (!QuestCapability.get(context.player()).has(this.questId))
            {
                return false;
            }

            QuestDefinition definition = QuestManager.get(context.player().server, this.questId);
            return QuestManager.isComplete(context.player(), definition, QuestCapability.get(context.player()).get(this.questId));
        }
    }

    public static final class FactionChecker implements Checker
    {
        private final String factionId;
        private final String attitude;

        public FactionChecker(String factionId, String attitude)
        {
            this.factionId = factionId;
            this.attitude = attitude == null ? "friendly" : attitude.toLowerCase();
        }

        @Override
        public boolean test(TriggerContext context)
        {
            if (this.factionId.isBlank())
            {
                return false;
            }

            dev.scriptbound.faction.FactionManager.Attitude current =
                dev.scriptbound.faction.FactionManager.attitude(context.player(), this.factionId);

            return switch (this.attitude)
            {
                case "hostile" -> current == dev.scriptbound.faction.FactionManager.Attitude.HOSTILE;
                case "neutral" -> current == dev.scriptbound.faction.FactionManager.Attitude.NEUTRAL;
                default -> current == dev.scriptbound.faction.FactionManager.Attitude.FRIENDLY;
            };
        }
    }

    public static final class StateChecker implements Checker
    {
        private final String scope;
        private final String key;
        private final String op;
        private final double value;

        public StateChecker(String scope, String key, String op, double value)
        {
            this.scope = scope;
            this.key = key;
            this.op = op == null ? "gte" : op.toLowerCase();
            this.value = value;
        }

        @Override
        public boolean test(TriggerContext context)
        {
            if (this.key.isBlank())
            {
                return true;
            }

            String target = "global".equalsIgnoreCase(this.scope) || "~".equals(this.scope) ? "~" : "@p";
            StateService.Target resolved = StateService.resolveTarget(context.asSource(), target);
            StateValue current = resolved.store().get(this.key);

            return switch (this.op)
            {
                case "absent", "missing", "not" -> current == null;
                case "present", "exists" -> current != null;
                case "eq", "==", "equals" -> current != null && compare(current) == 0;
                case "lt", "<" -> current != null && compare(current) < 0;
                case "lte", "<=" -> current != null && compare(current) <= 0;
                case "gt", ">" -> current != null && compare(current) > 0;
                default -> current != null && compare(current) >= 0;
            };
        }

        private int compare(StateValue current)
        {
            if (current.isNumber())
            {
                return Double.compare(current.asNumber(), this.value);
            }

            try
            {
                return Double.compare(Double.parseDouble(current.asString()), this.value);
            }
            catch (NumberFormatException e)
            {
                return current.asString().compareTo(String.valueOf(this.value));
            }
        }
    }
}
