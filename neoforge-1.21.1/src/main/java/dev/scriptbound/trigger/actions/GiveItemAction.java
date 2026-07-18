package dev.scriptbound.trigger.actions;

import com.google.gson.JsonObject;
import dev.scriptbound.trigger.TriggerContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public record GiveItemAction(String item, int count) implements TriggerAction
{
    public static GiveItemAction fromJson(JsonObject json)
    {
        String item = json.has("item") ? json.get("item").getAsString() : "";
        int count = json.has("count") ? json.get("count").getAsInt() : 1;
        return new GiveItemAction(item, Math.max(1, count));
    }

    @Override
    public String type()
    {
        return "give_item";
    }

    @Override
    public void execute(TriggerContext context)
    {
        ResourceLocation id = ResourceLocation.tryParse(this.item);

        if (id == null)
        {
            return;
        }

        Item resolved = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);

        if (resolved == null || resolved == Items.AIR)
        {
            return;
        }

        ItemStack stack = new ItemStack(resolved, this.count);

        if (!context.player().getInventory().add(stack))
        {
            context.player().drop(stack, false);
        }
    }
}
