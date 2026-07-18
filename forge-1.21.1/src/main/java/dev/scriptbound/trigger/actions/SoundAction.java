package dev.scriptbound.trigger.actions;

import com.google.gson.JsonObject;
import dev.scriptbound.trigger.TriggerContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.registries.ForgeRegistries;

public record SoundAction(String sound, float volume) implements TriggerAction
{
    public static SoundAction fromJson(JsonObject json)
    {
        String sound = json.has("sound") ? json.get("sound").getAsString() : "";
        float volume = json.has("volume") ? json.get("volume").getAsFloat() : 1.0F;
        return new SoundAction(sound, volume);
    }

    @Override
    public String type()
    {
        return "sound";
    }

    @Override
    public void execute(TriggerContext context)
    {
        ResourceLocation id = ResourceLocation.tryParse(this.sound);

        if (id == null)
        {
            return;
        }

        SoundEvent event = ForgeRegistries.SOUND_EVENTS.getValue(id);

        if (event == null)
        {
            return;
        }

        context.player().serverLevel().playSound(
            null,
            context.player().getX(),
            context.player().getY(),
            context.player().getZ(),
            event,
            SoundSource.MASTER,
            this.volume,
            1.0F
        );
    }
}
