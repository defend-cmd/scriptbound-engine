package dev.scriptbound.quest;

import net.minecraft.world.entity.player.Player;

public final class QuestCapability {
    private QuestCapability() {}

    public static QuestProgress get(Player player) {
        return player.getData(dev.scriptbound.ScriptBoundAttachments.QUEST);
    }
}
