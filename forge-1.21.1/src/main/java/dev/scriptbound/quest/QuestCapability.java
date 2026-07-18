package dev.scriptbound.quest;

import dev.scriptbound.ScriptBoundMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Mod.EventBusSubscriber(modid = ScriptBoundMod.MOD_ID)
public final class QuestCapability
{
    public static final Capability<QuestProgress> QUEST = CapabilityManager.get(new CapabilityToken<>() {});

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ScriptBoundMod.MOD_ID, "quests");

    private QuestCapability() {}

    public static QuestProgress get(Player player)
    {
        return player.getCapability(QUEST).orElseThrow(() -> new IllegalStateException("Missing quest capability"));
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event)
    {
        if (event.getObject() instanceof Player)
        {
            event.addCapability(ID, new Provider());
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event)
    {
        if (!event.isWasDeath())
        {
            return;
        }

        event.getOriginal().getCapability(QUEST).ifPresent(old ->
            event.getEntity().getCapability(QUEST).ifPresent(neu -> neu.load(old.save()))
        );
    }

    private static final class Provider implements ICapabilityProvider, INBTSerializable<CompoundTag>
    {
        private final QuestProgress progress = new QuestProgress();
        private final LazyOptional<QuestProgress> optional = LazyOptional.of(() -> this.progress);

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable net.minecraft.core.Direction side)
        {
            return QUEST.orEmpty(capability, this.optional);
        }

        @Override
        public CompoundTag serializeNBT(net.minecraft.core.HolderLookup.Provider provider)
        {
            return this.progress.save();
        }

        @Override
        public void deserializeNBT(net.minecraft.core.HolderLookup.Provider provider, CompoundTag tag)
        {
            this.progress.load(tag);
        }
    }
}
