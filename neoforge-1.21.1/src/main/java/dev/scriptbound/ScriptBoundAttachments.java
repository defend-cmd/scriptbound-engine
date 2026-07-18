package dev.scriptbound;

import dev.scriptbound.data.StateStore;
import dev.scriptbound.quest.QuestProgress;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ScriptBoundAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ScriptBoundMod.MOD_ID);

    public static final java.util.function.Supplier<AttachmentType<StateStore>> PLAYER_STATE = ATTACHMENTS.register(
        "player_states", () -> AttachmentType.builder(StateStore::new)
            .serialize(new IAttachmentSerializer<CompoundTag, StateStore>() {
                @Override
                public StateStore read(net.neoforged.neoforge.attachment.IAttachmentHolder holder, CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
                    StateStore store = new StateStore();
                    store.loadFromNbt(tag);
                    return store;
                }

                @Override
                public CompoundTag write(StateStore attachment, net.minecraft.core.HolderLookup.Provider provider) {
                    return attachment.saveToNbt();
                }
            })
            .copyOnDeath()
            .build()
    );

    public static final java.util.function.Supplier<AttachmentType<QuestProgress>> QUEST = ATTACHMENTS.register(
        "quests", () -> AttachmentType.builder(QuestProgress::new)
            .serialize(new IAttachmentSerializer<CompoundTag, QuestProgress>() {
                @Override
                public QuestProgress read(net.neoforged.neoforge.attachment.IAttachmentHolder holder, CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
                    QuestProgress p = new QuestProgress();
                    p.load(tag);
                    return p;
                }

                @Override
                public CompoundTag write(QuestProgress attachment, net.minecraft.core.HolderLookup.Provider provider) {
                    return attachment.save();
                }
            })
            .copyOnDeath()
            .build()
    );
}
