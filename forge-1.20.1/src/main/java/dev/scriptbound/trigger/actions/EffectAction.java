package dev.scriptbound.trigger.actions;

import com.google.gson.JsonObject;
import dev.scriptbound.trigger.TriggerContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.registries.ForgeRegistries;

public record EffectAction(String effectId, int duration, int amplifier, boolean showParticles) implements TriggerAction
{
    public static EffectAction fromJson(JsonObject json)
    {
        String effectId = json.has("effect") ? json.get("effect").getAsString() : "minecraft:glowing";
        int duration = json.has("duration") ? json.get("duration").getAsInt() : 200;
        int amplifier = json.has("amplifier") ? json.get("amplifier").getAsInt() : 0;
        boolean showParticles = !json.has("particles") || json.get("particles").getAsBoolean();
        return new EffectAction(effectId, duration, amplifier, showParticles);
    }

    @Override
    public String type()
    {
        return "effect";
    }

    @Override
    public void execute(TriggerContext context)
    {
        if (context.player() == null) return;

        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(this.effectId));

        if (effect != null)
        {
            if (this.duration <= 0)
            {
                context.player().removeEffect(effect);
            }
            else
            {
                context.player().addEffect(new MobEffectInstance(effect, this.duration, this.amplifier, false, this.showParticles));
            }
        }
    }
}
