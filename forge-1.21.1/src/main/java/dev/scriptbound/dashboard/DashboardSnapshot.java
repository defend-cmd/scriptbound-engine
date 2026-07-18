package dev.scriptbound.dashboard;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DashboardSnapshot
{
    private final Map<String, String> globalStates;
    private final Map<String, String> playerStates;
    private final List<String> triggers;
    private final Map<String, Integer> triggerActionCounts;
    private final List<String> scripts;
    private final List<String> dialogues;
    private final List<String> quests;
    private final List<String> npcs;
    private final List<String> flows;
    private final Map<String, String> npcMeta;
    private final Map<String, String> globalTriggers;

    public DashboardSnapshot(
        Map<String, String> globalStates,
        Map<String, String> playerStates,
        List<String> triggers,
        Map<String, Integer> triggerActionCounts,
        List<String> scripts,
        List<String> dialogues,
        List<String> quests,
        List<String> npcs,
        List<String> flows,
        Map<String, String> npcMeta,
        Map<String, String> globalTriggers
    )
    {
        this.globalStates = globalStates;
        this.playerStates = playerStates;
        this.triggers = triggers;
        this.triggerActionCounts = triggerActionCounts;
        this.scripts = scripts;
        this.dialogues = dialogues;
        this.quests = quests;
        this.npcs = npcs;
        this.flows = flows;
        this.npcMeta = npcMeta;
        this.globalTriggers = globalTriggers;
    }

    public Map<String, String> globalStates()
    {
        return this.globalStates;
    }

    public Map<String, String> playerStates()
    {
        return this.playerStates;
    }

    public List<String> triggers()
    {
        return this.triggers;
    }

    public Map<String, Integer> triggerActionCounts()
    {
        return this.triggerActionCounts;
    }

    public List<String> scripts()
    {
        return this.scripts;
    }

    public List<String> dialogues()
    {
        return this.dialogues;
    }

    public List<String> quests()
    {
        return this.quests;
    }

    public List<String> npcs()
    {
        return this.npcs;
    }

    public List<String> flows()
    {
        return this.flows;
    }

    public Map<String, String> npcMeta()
    {
        return this.npcMeta;
    }

    public Map<String, String> globalTriggers()
    {
        return this.globalTriggers;
    }

    public static void encode(DashboardSnapshot snapshot, FriendlyByteBuf buf)
    {
        writeStringMap(buf, snapshot.globalStates);
        writeStringMap(buf, snapshot.playerStates);
        writeStringList(buf, snapshot.triggers);
        writeIntMap(buf, snapshot.triggerActionCounts);
        writeStringList(buf, snapshot.scripts);
        writeStringList(buf, snapshot.dialogues);
        writeStringList(buf, snapshot.quests);
        writeStringList(buf, snapshot.npcs);
        writeStringList(buf, snapshot.flows);
        writeStringMap(buf, snapshot.npcMeta);
        writeStringMap(buf, snapshot.globalTriggers);
    }

    public static DashboardSnapshot decode(FriendlyByteBuf buf)
    {
        return new DashboardSnapshot(
            readStringMap(buf),
            readStringMap(buf),
            readStringList(buf),
            readIntMap(buf),
            readStringList(buf),
            readStringList(buf),
            readStringList(buf),
            readStringList(buf),
            readStringList(buf),
            readStringMap(buf),
            readStringMap(buf)
        );
    }

    private static void writeStringMap(FriendlyByteBuf buf, Map<String, String> map)
    {
        buf.writeVarInt(map.size());

        for (Map.Entry<String, String> entry : map.entrySet())
        {
            buf.writeUtf(entry.getKey(), 32767);
            buf.writeUtf(entry.getValue(), 32767);
        }
    }

    private static Map<String, String> readStringMap(FriendlyByteBuf buf)
    {
        int count = buf.readVarInt();
        Map<String, String> map = new LinkedHashMap<>();

        for (int i = 0; i < count; i++)
        {
            map.put(buf.readUtf(), buf.readUtf());
        }

        return map;
    }

    private static void writeIntMap(FriendlyByteBuf buf, Map<String, Integer> map)
    {
        buf.writeVarInt(map.size());

        for (Map.Entry<String, Integer> entry : map.entrySet())
        {
            buf.writeUtf(entry.getKey(), 32767);
            buf.writeVarInt(entry.getValue());
        }
    }

    private static Map<String, Integer> readIntMap(FriendlyByteBuf buf)
    {
        int count = buf.readVarInt();
        Map<String, Integer> map = new LinkedHashMap<>();

        for (int i = 0; i < count; i++)
        {
            map.put(buf.readUtf(), buf.readVarInt());
        }

        return map;
    }

    private static void writeStringList(FriendlyByteBuf buf, List<String> list)
    {
        buf.writeVarInt(list.size());

        for (String value : list)
        {
            buf.writeUtf(value, 32767);
        }
    }

    private static List<String> readStringList(FriendlyByteBuf buf)
    {
        int count = buf.readVarInt();
        List<String> list = new ArrayList<>(count);

        for (int i = 0; i < count; i++)
        {
            list.add(buf.readUtf());
        }

        return list;
    }
}
