package dev.scriptbound.quest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class QuestProgress
{
    private final Map<String, ActiveQuest> active = new LinkedHashMap<>();

    public boolean has(String questId)
    {
        return this.active.containsKey(questId);
    }

    public ActiveQuest get(String questId)
    {
        return this.active.get(questId);
    }

    public Set<String> ids()
    {
        return Collections.unmodifiableSet(this.active.keySet());
    }

    public void add(String questId)
    {
        this.active.putIfAbsent(questId, new ActiveQuest(questId));
    }

    public void remove(String questId)
    {
        this.active.remove(questId);
    }

    public Map<String, ActiveQuest> snapshot()
    {
        return Collections.unmodifiableMap(this.active);
    }

    public CompoundTag save()
    {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();

        for (ActiveQuest quest : this.active.values())
        {
            list.add(quest.save());
        }

        tag.put("quests", list);
        return tag;
    }

    public void load(CompoundTag tag)
    {
        this.active.clear();

        if (tag == null || !tag.contains("quests", Tag.TAG_LIST))
        {
            return;
        }

        ListTag list = tag.getList("quests", Tag.TAG_COMPOUND);

        for (int i = 0; i < list.size(); i++)
        {
            ActiveQuest quest = ActiveQuest.load(list.getCompound(i));
            this.active.put(quest.questId(), quest);
        }
    }

    public static final class ActiveQuest
    {
        private final String questId;
        private final Map<String, Integer> killCounts = new LinkedHashMap<>();
        private boolean completed;

        public ActiveQuest(String questId)
        {
            this.questId = questId;
        }

        public String questId()
        {
            return this.questId;
        }

        public boolean isCompleted()
        {
            return this.completed;
        }

        public void setCompleted(boolean completed)
        {
            this.completed = completed;
        }

        public int getKillCount(String entityId)
        {
            return this.killCounts.getOrDefault(entityId, 0);
        }

        public void addKill(String entityId)
        {
            this.killCounts.put(entityId, this.getKillCount(entityId) + 1);
        }

        public CompoundTag save()
        {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", this.questId);
            tag.putBoolean("completed", this.completed);

            CompoundTag kills = new CompoundTag();

            for (Map.Entry<String, Integer> entry : this.killCounts.entrySet())
            {
                kills.putInt(entry.getKey(), entry.getValue());
            }

            tag.put("kills", kills);
            return tag;
        }

        public static ActiveQuest load(CompoundTag tag)
        {
            ActiveQuest quest = new ActiveQuest(tag.getString("id"));
            quest.completed = tag.getBoolean("completed");

            if (tag.contains("kills", Tag.TAG_COMPOUND))
            {
                CompoundTag kills = tag.getCompound("kills");

                for (String key : kills.getAllKeys())
                {
                    quest.killCounts.put(key, kills.getInt(key));
                }
            }

            return quest;
        }
    }
}
