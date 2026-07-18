package dev.scriptbound.registry;

import dev.scriptbound.ScriptBoundMod;
import dev.scriptbound.block.TriggerBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModBlocks
{
    public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(net.minecraft.core.registries.BuiltInRegistries.BLOCK, ScriptBoundMod.MOD_ID);

    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(net.minecraft.core.registries.BuiltInRegistries.ITEM, ScriptBoundMod.MOD_ID);

    public static final Supplier<Block> TRIGGER_BLOCK = BLOCKS.register(
        "trigger_block",
        () -> new TriggerBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_PURPLE)
            .strength(0.2F)
            .sound(SoundType.WOOL)
            .noOcclusion()
        )
    );

    public static final Supplier<Item> TRIGGER_BLOCK_ITEM = ITEMS.register(
        "trigger_block",
        () -> new BlockItem(TRIGGER_BLOCK.get(), new Item.Properties())
    );

    public static final Supplier<Block> REGION_BLOCK = BLOCKS.register(
        "region_block",
        () -> new dev.scriptbound.block.RegionBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_LIGHT_BLUE)
            .strength(0.2F)
            .sound(SoundType.GLASS)
            .noOcclusion()
        )
    );

    public static final Supplier<Item> REGION_BLOCK_ITEM = ITEMS.register(
        "region_block",
        () -> new BlockItem(REGION_BLOCK.get(), new Item.Properties())
    );

    private ModBlocks() {}

    public static void register(IEventBus modBus)
    {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }
}
