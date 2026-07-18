package dev.scriptbound.registry;

import dev.scriptbound.ScriptBoundMod;
import dev.scriptbound.block.entity.RegionBlockEntity;
import dev.scriptbound.block.entity.TriggerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities
{
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ScriptBoundMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<TriggerBlockEntity>> TRIGGER_BLOCK = BLOCK_ENTITIES.register(
        "trigger_block",
        () -> BlockEntityType.Builder.of(TriggerBlockEntity::new, ModBlocks.TRIGGER_BLOCK.get()).build(null)
    );

    public static final RegistryObject<BlockEntityType<RegionBlockEntity>> REGION_BLOCK = BLOCK_ENTITIES.register(
        "region_block",
        () -> BlockEntityType.Builder.of(RegionBlockEntity::new, ModBlocks.REGION_BLOCK.get()).build(null)
    );

    private ModBlockEntities() {}

    public static void register(IEventBus modBus)
    {
        BLOCK_ENTITIES.register(modBus);
    }
}
