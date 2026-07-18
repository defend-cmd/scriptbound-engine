package dev.scriptbound.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.scriptbound.ScriptBoundConstants;
import dev.scriptbound.events.StateChangedEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@AutoRegisterCapability
public class StateStore
{
    private final Map<String, StateValue> states = new LinkedHashMap<>();

    public StateValue get(String key)
    {
        return this.states.get(key);
    }

    public boolean has(String key)
    {
        return this.states.containsKey(key);
    }

    public Set<String> keys()
    {
        return Collections.unmodifiableSet(this.states.keySet());
    }

    public Map<String, StateValue> snapshot()
    {
        return Collections.unmodifiableMap(new LinkedHashMap<>(this.states));
    }

    public boolean set(String key, StateValue value, StateScope scope, String holderId)
    {
        if (value == null)
        {
            return this.remove(key, scope, holderId);
        }

        StateValue previous = this.states.put(key, value);
        boolean changed = previous == null || !previous.equals(value);

        if (changed)
        {
            MinecraftForge.EVENT_BUS.post(new StateChangedEvent(scope, holderId, key, previous, value));
        }

        return changed;
    }

    public boolean remove(String key, StateScope scope, String holderId)
    {
        StateValue previous = this.states.remove(key);

        if (previous != null)
        {
            MinecraftForge.EVENT_BUS.post(new StateChangedEvent(scope, holderId, key, previous, null));
            return true;
        }

        return false;
    }

    public boolean add(String key, double delta, StateScope scope, String holderId)
    {
        StateValue current = this.states.get(key);
        double base = current != null && current.isNumber() ? current.asNumber() : 0D;
        return this.set(key, new StateValue.NumberValue(base + delta), scope, holderId);
    }

    public int clear(String pattern, StateScope scope, String holderId)
    {
        if (pattern == null || pattern.isEmpty() || "*".equals(pattern))
        {
            int count = this.states.size();
            Map<String, StateValue> copy = new HashMap<>(this.states);

            this.states.clear();

            for (Map.Entry<String, StateValue> entry : copy.entrySet())
            {
                MinecraftForge.EVENT_BUS.post(new StateChangedEvent(scope, holderId, entry.getKey(), entry.getValue(), null));
            }

            return count;
        }

        String regex = pattern.replace(".", "\\.").replace("*", ".*");
        int removed = 0;

        for (String key : this.states.keySet().stream().collect(Collectors.toList()))
        {
            if (key.matches(regex))
            {
                if (this.remove(key, scope, holderId))
                {
                    removed++;
                }
            }
        }

        return removed;
    }

    public void loadFromJson(JsonObject root)
    {
        this.states.clear();

        if (root == null)
        {
            return;
        }

        JsonObject statesObject = root.has("states") && root.get("states").isJsonObject()
            ? root.getAsJsonObject("states")
            : root;

        for (Map.Entry<String, JsonElement> entry : statesObject.entrySet())
        {
            StateValue value = StateValue.fromJson(entry.getValue());

            if (value != null)
            {
                this.states.put(entry.getKey(), value);
            }
        }
    }

    public JsonObject saveToJson()
    {
        JsonObject root = new JsonObject();
        root.addProperty("version", ScriptBoundConstants.DATA_VERSION);

        JsonObject statesObject = new JsonObject();

        for (Map.Entry<String, StateValue> entry : this.states.entrySet())
        {
            statesObject.add(entry.getKey(), entry.getValue().toJson());
        }

        root.add("states", statesObject);
        return root;
    }

    public void loadFromNbt(CompoundTag tag)
    {
        this.states.clear();

        if (tag == null || !tag.contains("entries", Tag.TAG_LIST))
        {
            return;
        }

        ListTag entries = tag.getList("entries", Tag.TAG_COMPOUND);

        for (int i = 0; i < entries.size(); i++)
        {
            CompoundTag entry = entries.getCompound(i);
            String key = entry.getString("key");

            if (entry.contains("number"))
            {
                this.states.put(key, new StateValue.NumberValue(entry.getDouble("number")));
            }
            else if (entry.contains("string"))
            {
                this.states.put(key, new StateValue.StringValue(entry.getString("string")));
            }
        }
    }

    public CompoundTag saveToNbt()
    {
        CompoundTag tag = new CompoundTag();
        ListTag entries = new ListTag();

        for (Map.Entry<String, StateValue> entry : this.states.entrySet())
        {
            CompoundTag item = new CompoundTag();
            item.putString("key", entry.getKey());

            if (entry.getValue().isNumber())
            {
                item.putDouble("number", entry.getValue().asNumber());
            }
            else
            {
                item.putString("string", entry.getValue().asString());
            }

            entries.add(item);
        }

        tag.put("entries", entries);
        return tag;
    }

    public void copyFrom(StateStore other)
    {
        this.states.clear();
        this.states.putAll(other.states);
    }
}
