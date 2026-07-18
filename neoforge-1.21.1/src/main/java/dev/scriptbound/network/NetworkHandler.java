package dev.scriptbound.network;

import dev.scriptbound.ScriptBoundMod;
import dev.scriptbound.network.packet.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class NetworkHandler {
    private static final String PROTOCOL = "1";

    private NetworkHandler() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
            var registrar = event.registrar(ScriptBoundMod.MOD_ID).versioned(PROTOCOL);

            registrar.playToClient(DialogueLinePacket.TYPE, DialogueLinePacket.STREAM_CODEC, DialogueLinePacket::handle);
            registrar.playToClient(CloseDialoguePacket.TYPE, CloseDialoguePacket.STREAM_CODEC, CloseDialoguePacket::handle);
            registrar.playToClient(QuestJournalPacket.TYPE, QuestJournalPacket.STREAM_CODEC, QuestJournalPacket::handle);
            registrar.playToClient(DashboardDataPacket.TYPE, DashboardDataPacket.STREAM_CODEC, DashboardDataPacket::handle);
            registrar.playToClient(DashboardDocumentPacket.TYPE, DashboardDocumentPacket.STREAM_CODEC, DashboardDocumentPacket::handle);
            registrar.playToClient(DashboardNotificationPacket.TYPE, DashboardNotificationPacket.STREAM_CODEC, DashboardNotificationPacket::handle);

            registrar.playToServer(DialogueAdvancePacket.TYPE, DialogueAdvancePacket.STREAM_CODEC, DialogueAdvancePacket::handle);
            registrar.playToServer(DialogueChoicePacket.TYPE, DialogueChoicePacket.STREAM_CODEC, DialogueChoicePacket::handle);
            registrar.playToServer(OpenJournalPacket.TYPE, OpenJournalPacket.STREAM_CODEC, OpenJournalPacket::handle);
            registrar.playToServer(OpenDashboardPacket.TYPE, OpenDashboardPacket.STREAM_CODEC, OpenDashboardPacket::handle);
            registrar.playToServer(DashboardActionPacket.TYPE, DashboardActionPacket.STREAM_CODEC, DashboardActionPacket::handle);
        });
    }

    public static void sendToServer(CustomPacketPayload packet) {
        PacketDistributor.sendToServer(packet);
    }

    public static void sendDialogueLine(ServerPlayer player, String dialogueId, int lineIndex, String speaker, String text, java.util.List<dev.scriptbound.dialogue.DialogueDefinition.Choice> choices) {
        java.util.List<String> choiceTexts = new java.util.ArrayList<>();
        for (dev.scriptbound.dialogue.DialogueDefinition.Choice choice : choices) choiceTexts.add(choice.text());
        PacketDistributor.sendToPlayer(player, new DialogueLinePacket(dialogueId, lineIndex, speaker, text, choiceTexts));
    }

    public static void sendCloseDialogue(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new CloseDialoguePacket());
    }

    public static void sendQuestJournal(ServerPlayer player) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        dev.scriptbound.quest.QuestProgress progress = dev.scriptbound.quest.QuestCapability.get(player);
        if (progress.ids().isEmpty()) lines.add("No active quests.");
        else {
            for (String questId : progress.ids()) {
                dev.scriptbound.quest.QuestDefinition definition = dev.scriptbound.quest.QuestManager.get(player.server, questId);
                boolean complete = dev.scriptbound.quest.QuestManager.isComplete(player, definition, progress.get(questId));
                lines.add(definition.title() + (complete ? " [ready]" : ""));
                lines.add(definition.description());
                if (complete) lines.add("/sbe quest complete @p " + questId);
            }
        }
        PacketDistributor.sendToPlayer(player, new QuestJournalPacket(lines));
    }

    public static void sendDashboard(ServerPlayer player, dev.scriptbound.dashboard.DashboardSnapshot snapshot) {
        PacketDistributor.sendToPlayer(player, new DashboardDataPacket(snapshot));
    }

    public static void sendDashboardDocument(ServerPlayer player, DashboardDocumentPacket packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendNotification(ServerPlayer player, int type, String message) {
        PacketDistributor.sendToPlayer(player, new DashboardNotificationPacket(type, message));
    }
}
