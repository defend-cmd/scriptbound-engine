package dev.scriptbound.network;

import dev.scriptbound.ScriptBoundMod;
import dev.scriptbound.dashboard.DashboardSnapshot;
import dev.scriptbound.network.packet.CloseDialoguePacket;
import dev.scriptbound.network.packet.DialogueAdvancePacket;
import dev.scriptbound.network.packet.DialogueChoicePacket;
import dev.scriptbound.network.packet.DialogueLinePacket;
import dev.scriptbound.network.packet.DashboardActionPacket;
import dev.scriptbound.network.packet.DashboardDataPacket;
import dev.scriptbound.network.packet.DashboardDocumentPacket;
import dev.scriptbound.network.packet.DashboardNotificationPacket;
import dev.scriptbound.dialogue.DialogueDefinition;
import dev.scriptbound.network.packet.OpenDashboardPacket;
import dev.scriptbound.network.packet.OpenJournalPacket;
import dev.scriptbound.network.packet.QuestJournalPacket;
import dev.scriptbound.quest.QuestCapability;
import dev.scriptbound.quest.QuestDefinition;
import dev.scriptbound.quest.QuestManager;
import dev.scriptbound.quest.QuestProgress;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.ArrayList;
import java.util.List;

public final class NetworkHandler
{
    private static final String PROTOCOL = "1";
    private static int packetId = 0;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        ResourceLocation.fromNamespaceAndPath(ScriptBoundMod.MOD_ID, "main"),
        () -> PROTOCOL,
        PROTOCOL::equals,
        PROTOCOL::equals
    );

    private NetworkHandler() {}

    public static void register(IEventBus modBus)
    {
        modBus.addListener(NetworkHandler::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event)
    {
        event.enqueueWork(() -> {
            CHANNEL.messageBuilder(DialogueLinePacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DialogueLinePacket::encode)
                .decoder(DialogueLinePacket::decode)
                .consumerMainThread(DialogueLinePacket::handle)
                .add();

            CHANNEL.messageBuilder(CloseDialoguePacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CloseDialoguePacket::encode)
                .decoder(CloseDialoguePacket::decode)
                .consumerMainThread(CloseDialoguePacket::handle)
                .add();

            CHANNEL.messageBuilder(DialogueAdvancePacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(DialogueAdvancePacket::encode)
                .decoder(DialogueAdvancePacket::decode)
                .consumerMainThread(DialogueAdvancePacket::handle)
                .add();

            CHANNEL.messageBuilder(DialogueChoicePacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(DialogueChoicePacket::encode)
                .decoder(DialogueChoicePacket::decode)
                .consumerMainThread(DialogueChoicePacket::handle)
                .add();

            CHANNEL.messageBuilder(QuestJournalPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(QuestJournalPacket::encode)
                .decoder(QuestJournalPacket::decode)
                .consumerMainThread(QuestJournalPacket::handle)
                .add();

            CHANNEL.messageBuilder(OpenJournalPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(OpenJournalPacket::encode)
                .decoder(OpenJournalPacket::decode)
                .consumerMainThread(OpenJournalPacket::handle)
                .add();

            CHANNEL.messageBuilder(OpenDashboardPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(OpenDashboardPacket::encode)
                .decoder(OpenDashboardPacket::decode)
                .consumerMainThread(OpenDashboardPacket::handle)
                .add();

            CHANNEL.messageBuilder(DashboardDataPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DashboardDataPacket::encode)
                .decoder(DashboardDataPacket::decode)
                .consumerMainThread(DashboardDataPacket::handle)
                .add();

            CHANNEL.messageBuilder(DashboardActionPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(DashboardActionPacket::encode)
                .decoder(DashboardActionPacket::decode)
                .consumerMainThread(DashboardActionPacket::handle)
                .add();

            CHANNEL.messageBuilder(DashboardDocumentPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DashboardDocumentPacket::encode)
                .decoder(DashboardDocumentPacket::decode)
                .consumerMainThread(DashboardDocumentPacket::handle)
                .add();

            CHANNEL.messageBuilder(DashboardNotificationPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DashboardNotificationPacket::encode)
                .decoder(DashboardNotificationPacket::decode)
                .consumerMainThread(DashboardNotificationPacket::handle)
                .add();

            CHANNEL.messageBuilder(dev.scriptbound.network.packet.OpenUiPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(dev.scriptbound.network.packet.OpenUiPacket::encode)
                .decoder(dev.scriptbound.network.packet.OpenUiPacket::decode)
                .consumerMainThread(dev.scriptbound.network.packet.OpenUiPacket::handle)
                .add();

            CHANNEL.messageBuilder(dev.scriptbound.network.packet.CloseUiPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(dev.scriptbound.network.packet.CloseUiPacket::encode)
                .decoder(dev.scriptbound.network.packet.CloseUiPacket::decode)
                .consumerMainThread(dev.scriptbound.network.packet.CloseUiPacket::handle)
                .add();

            CHANNEL.messageBuilder(dev.scriptbound.network.packet.EditUiPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(dev.scriptbound.network.packet.EditUiPacket::encode)
                .decoder(dev.scriptbound.network.packet.EditUiPacket::decode)
                .consumerMainThread(dev.scriptbound.network.packet.EditUiPacket::handle)
                .add();

            CHANNEL.messageBuilder(dev.scriptbound.network.packet.SaveUiPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(dev.scriptbound.network.packet.SaveUiPacket::encode)
                .decoder(dev.scriptbound.network.packet.SaveUiPacket::decode)
                .consumerMainThread(dev.scriptbound.network.packet.SaveUiPacket::handle)
                .add();

            CHANNEL.messageBuilder(dev.scriptbound.network.packet.UiClickPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(dev.scriptbound.network.packet.UiClickPacket::encode)
                .decoder(dev.scriptbound.network.packet.UiClickPacket::decode)
                .consumerMainThread(dev.scriptbound.network.packet.UiClickPacket::handle)
                .add();

            CHANNEL.messageBuilder(dev.scriptbound.network.packet.UiStatePacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(dev.scriptbound.network.packet.UiStatePacket::encode)
                .decoder(dev.scriptbound.network.packet.UiStatePacket::decode)
                .consumerMainThread(dev.scriptbound.network.packet.UiStatePacket::handle)
                .add();

            CHANNEL.messageBuilder(dev.scriptbound.network.packet.UiSetPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(dev.scriptbound.network.packet.UiSetPacket::encode)
                .decoder(dev.scriptbound.network.packet.UiSetPacket::decode)
                .consumerMainThread(dev.scriptbound.network.packet.UiSetPacket::handle)
                .add();

            CHANNEL.messageBuilder(dev.scriptbound.network.packet.RequestUiListPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(dev.scriptbound.network.packet.RequestUiListPacket::encode)
                .decoder(dev.scriptbound.network.packet.RequestUiListPacket::decode)
                .consumerMainThread(dev.scriptbound.network.packet.RequestUiListPacket::handle)
                .add();

            CHANNEL.messageBuilder(dev.scriptbound.network.packet.UiListPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(dev.scriptbound.network.packet.UiListPacket::encode)
                .decoder(dev.scriptbound.network.packet.UiListPacket::decode)
                .consumerMainThread(dev.scriptbound.network.packet.UiListPacket::handle)
                .add();

            CHANNEL.messageBuilder(dev.scriptbound.network.packet.OpenUiEditorPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(dev.scriptbound.network.packet.OpenUiEditorPacket::encode)
                .decoder(dev.scriptbound.network.packet.OpenUiEditorPacket::decode)
                .consumerMainThread(dev.scriptbound.network.packet.OpenUiEditorPacket::handle)
                .add();

            CHANNEL.messageBuilder(dev.scriptbound.network.packet.DeleteUiPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(dev.scriptbound.network.packet.DeleteUiPacket::encode)
                .decoder(dev.scriptbound.network.packet.DeleteUiPacket::decode)
                .consumerMainThread(dev.scriptbound.network.packet.DeleteUiPacket::handle)
                .add();
        });
    }

    private static int nextId()
    {
        return packetId++;
    }

    public static void sendDialogueLine(ServerPlayer player, String dialogueId, int lineIndex, String speaker, String text, List<DialogueDefinition.Choice> choices)
    {
        List<String> choiceTexts = new ArrayList<>();

        for (DialogueDefinition.Choice choice : choices)
        {
            choiceTexts.add(choice.text());
        }

        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new DialogueLinePacket(dialogueId, lineIndex, speaker, text, choiceTexts));
    }

    public static void sendCloseDialogue(ServerPlayer player)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new CloseDialoguePacket());
    }

    public static void sendQuestJournal(ServerPlayer player)
    {
        List<String> lines = new ArrayList<>();
        QuestProgress progress = QuestCapability.get(player);

        if (progress.ids().isEmpty())
        {
            lines.add("No active quests.");
        }
        else
        {
            for (String questId : progress.ids())
            {
                QuestDefinition definition = QuestManager.get(player.server, questId);
                boolean complete = QuestManager.isComplete(player, definition, progress.get(questId));
                lines.add(definition.title() + (complete ? " [ready]" : ""));
                lines.add(definition.description());

                if (complete)
                {
                    lines.add("/sbe quest complete @p " + questId);
                }
            }
        }

        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new QuestJournalPacket(lines));
    }

    public static void sendDashboard(ServerPlayer player, DashboardSnapshot snapshot)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new DashboardDataPacket(snapshot));
    }

    public static void sendDashboardDocument(ServerPlayer player, DashboardDocumentPacket packet)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendNotification(ServerPlayer player, int type, String message)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new DashboardNotificationPacket(type, message));
    }

    public static void sendOpenUi(ServerPlayer player, String id, String json)
    {
        dev.scriptbound.ui.UiSession.open(player, id);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new dev.scriptbound.network.packet.OpenUiPacket(id, json));
    }

    public static void sendCloseUi(ServerPlayer player, String id)
    {
        dev.scriptbound.ui.UiSession.close(player, id);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new dev.scriptbound.network.packet.CloseUiPacket(id));
    }

    public static void sendUiState(ServerPlayer player, java.util.Map<String, String> values)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new dev.scriptbound.network.packet.UiStatePacket(values));
    }

    public static void sendUiSet(ServerPlayer player, String uiId, String elementId, String field, String value)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new dev.scriptbound.network.packet.UiSetPacket(uiId, elementId, field, value));
    }
}
