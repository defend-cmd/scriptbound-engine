package dev.scriptbound.registry;

import dev.scriptbound.ScriptBoundMod;
import dev.scriptbound.entity.MovingBlockEntity;
import dev.scriptbound.entity.NpcEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities
{
    public static final DeferredRegister<EntityType<?>> ENTITIES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ScriptBoundMod.MOD_ID);

    public static final RegistryObject<EntityType<NpcEntity>> NPC = ENTITIES.register(
        "npc",
        () -> EntityType.Builder.of(NpcEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.8F)
            .clientTrackingRange(10)
            .build("npc")
    );

    public static final RegistryObject<EntityType<MovingBlockEntity>> MOVING_BLOCK = ENTITIES.register(
        "moving_block",
        () -> EntityType.Builder.of(MovingBlockEntity::new, MobCategory.MISC)
            .sized(1.0F, 1.0F)
            .clientTrackingRange(10)
            .updateInterval(1)
            .build("moving_block")
    );

    private ModEntities() {}

    public static void register(IEventBus modBus)
    {
        ENTITIES.register(modBus);
    }
}
