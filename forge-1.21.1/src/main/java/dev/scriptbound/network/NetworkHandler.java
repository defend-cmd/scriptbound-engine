package dev.scriptbound.network;

import dev.scriptbound.ScriptBoundMod;
import dev.scriptbound.network.packet.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.PacketDistributor;

public final class NetworkHandler {
    private static final int PROTOCOL = 1;
    private static Channel<CustomPacketPayload> CHANNEL;

    private NetworkHandler() {}

    public static void register(IEventBus modBus) {
        var builder = ChannelBuilder.named(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ScriptBoundMod.MOD_ID, "main"))
            .networkProtocolVersion(PROTOCOL)
            .payloadChannel();

        var protocol = builder.play()
            .clientbound(flow -> flow
                .add(DialogueLinePacket.TYPE, DialogueLinePacket.STREAM_CODEC, DialogueLinePacket::handle)
                .add(CloseDialoguePacket.TYPE, CloseDialoguePacket.STREAM_CODEC, CloseDialoguePacket::handle)
                .add(QuestJournalPacket.TYPE, QuestJournalPacket.STREAM_CODEC, QuestJournalPacket::handle)
                .add(DashboardDataPacket.TYPE, DashboardDataPacket.STREAM_CODEC, DashboardDataPacket::handle)
                .add(DashboardDocumentPacket.TYPE, DashboardDocumentPacket.STREAM_CODEC, DashboardDocumentPacket::handle)
                .add(DashboardNotificationPacket.TYPE, DashboardNotificationPacket.STREAM_CODEC, DashboardNotificationPacket::handle)
            )
            .serverbound(flow -> flow
                .add(DialogueAdvancePacket.TYPE, DialogueAdvancePacket.STREAM_CODEC, DialogueAdvancePacket::handle)
                .add(DialogueChoicePacket.TYPE, DialogueChoicePacket.STREAM_CODEC, DialogueChoicePacket::handle)
                .add(OpenJournalPacket.TYPE, OpenJournalPacket.STREAM_CODEC, OpenJournalPacket::handle)
                .add(OpenDashboardPacket.TYPE, OpenDashboardPacket.STREAM_CODEC, OpenDashboardPacket::handle)
                .add(DashboardActionPacket.TYPE, DashboardActionPacket.STREAM_CODEC, DashboardActionPacket::handle)
            );

        CHANNEL = ((net.minecraftforge.network.ChannelBuildable<CustomPacketPayload>) protocol).build();
    }

    public static void sendToServer(CustomPacketPayload packet) {
        CHANNEL.send(packet, PacketDistributor.SERVER.noArg());
    }

    public static void sendDialogueLine(ServerPlayer player, String dialogueId, int lineIndex, String speaker, String text, java.util.List<dev.scriptbound.dialogue.DialogueDefinition.Choice> choices) {
        java.util.List<String> choiceTexts = new java.util.ArrayList<>();
        for (dev.scriptbound.dialogue.DialogueDefinition.Choice choice : choices) choiceTexts.add(choice.text());
        CHANNEL.send(new DialogueLinePacket(dialogueId, lineIndex, speaker, text, choiceTexts), PacketDistributor.PLAYER.with(player));
    }

    public static void sendCloseDialogue(ServerPlayer player) {
        CHANNEL.send(new CloseDialoguePacket(), PacketDistributor.PLAYER.with(player));
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
        CHANNEL.send(new QuestJournalPacket(lines), PacketDistributor.PLAYER.with(player));
    }

    public static void sendDashboard(ServerPlayer player, dev.scriptbound.dashboard.DashboardSnapshot snapshot) {
        CHANNEL.send(new DashboardDataPacket(snapshot), PacketDistributor.PLAYER.with(player));
    }

    public static void sendDashboardDocument(ServerPlayer player, DashboardDocumentPacket packet) {
        CHANNEL.send(packet, PacketDistributor.PLAYER.with(player));
    }

    public static void sendNotification(ServerPlayer player, int type, String message) {
        CHANNEL.send(new DashboardNotificationPacket(type, message), PacketDistributor.PLAYER.with(player));
    }
}
