package dev.scriptbound.data;

import com.google.gson.JsonObject;
import dev.scriptbound.ScriptBoundMod;
import dev.scriptbound.ScriptBoundPaths;
import dev.scriptbound.util.JsonFiles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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
public final class PlayerStateCapability
{
    public static final Capability<StateStore> STATE = CapabilityManager.get(new CapabilityToken<>() {});

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ScriptBoundMod.MOD_ID, "player_states");

    private PlayerStateCapability() {}

    public static StateStore get(Player player)
    {
        return player.getCapability(STATE).orElseThrow(() -> new IllegalStateException("Player is missing ScriptBound state capability."));
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

        event.getOriginal().getCapability(STATE).ifPresent(oldStore ->
            event.getEntity().getCapability(STATE).ifPresent(newStore -> newStore.copyFrom(oldStore))
        );
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }

        StateStore store = get(player);
        JsonObject file = JsonFiles.readObject(ScriptBoundPaths.playerStatesFile(player.server, player.getUUID()));

        if (store.keys().isEmpty() && file.has("states"))
        {
            store.loadFromJson(file);

            if (!store.keys().isEmpty())
            {
                ScriptBoundMod.LOGGER.debug("Imported {} player state(s) from file for {}", store.keys().size(), player.getGameProfile().getName());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }

        JsonFiles.writeObject(
            ScriptBoundPaths.playerStatesFile(player.server, player.getUUID()),
            get(player).saveToJson()
        );
    }

    private static final class Provider implements ICapabilityProvider, INBTSerializable<CompoundTag>
    {
        private final StateStore store = new StateStore();
        private final LazyOptional<StateStore> optional = LazyOptional.of(() -> this.store);

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable net.minecraft.core.Direction side)
        {
            return STATE.orEmpty(capability, this.optional);
        }

        @Override
        public CompoundTag serializeNBT()
        {
            return this.store.saveToNbt();
        }

        @Override
        public void deserializeNBT(CompoundTag tag)
        {
            this.store.loadFromNbt(tag);
        }
    }
}
