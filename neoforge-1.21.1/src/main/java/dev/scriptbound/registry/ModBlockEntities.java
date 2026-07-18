package dev.scriptbound.registry;

import dev.scriptbound.ScriptBoundMod;
import dev.scriptbound.block.entity.RegionBlockEntity;
import dev.scriptbound.block.entity.TriggerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModBlockEntities
{
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE, ScriptBoundMod.MOD_ID);

    public static final Supplier<BlockEntityType<TriggerBlockEntity>> TRIGGER_BLOCK = BLOCK_ENTITIES.register(
        "trigger_block",
        () -> BlockEntityType.Builder.of(TriggerBlockEntity::new, ModBlocks.TRIGGER_BLOCK.get()).build(null)
    );

    public static final Supplier<BlockEntityType<RegionBlockEntity>> REGION_BLOCK = BLOCK_ENTITIES.register(
        "region_block",
        () -> BlockEntityType.Builder.of(RegionBlockEntity::new, ModBlocks.REGION_BLOCK.get()).build(null)
    );

    private ModBlockEntities() {}

    public static void register(IEventBus modBus)
    {
        BLOCK_ENTITIES.register(modBus);
    }
}
