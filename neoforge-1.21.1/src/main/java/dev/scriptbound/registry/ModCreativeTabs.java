package dev.scriptbound.registry;

import dev.scriptbound.ScriptBoundMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

public final class ModCreativeTabs
{
    public static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ScriptBoundMod.MOD_ID);

    public static final Supplier<CreativeModeTab> SBE_TAB = TABS.register(
        "main",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.scriptbound"))
            .icon(() -> new ItemStack(ModBlocks.TRIGGER_BLOCK_ITEM.get()))
            .displayItems((params, output) -> {
                output.accept(ModBlocks.TRIGGER_BLOCK_ITEM.get());
                output.accept(ModBlocks.REGION_BLOCK_ITEM.get());
            })
            .build()
    );

    private ModCreativeTabs() {}

    public static void register(IEventBus modBus)
    {
        TABS.register(modBus);
    }
}
