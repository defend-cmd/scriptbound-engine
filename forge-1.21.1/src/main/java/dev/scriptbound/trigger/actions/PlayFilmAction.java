package dev.scriptbound.trigger.actions;

import com.google.gson.JsonObject;
import dev.scriptbound.compat.BBSCompat;
import dev.scriptbound.trigger.TriggerContext;

public record PlayFilmAction(String filmId) implements TriggerAction
{
    public static PlayFilmAction fromJson(JsonObject json)
    {
        return new PlayFilmAction(json.has("film") ? json.get("film").getAsString() : "");
    }

    @Override
    public String type()
    {
        return "play_film";
    }

    @Override
    public void execute(TriggerContext context)
    {
        BBSCompat.playFilm(context.player(), this.filmId);
    }
}
