package dev.scriptbound.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public sealed interface StateValue permits StateValue.NumberValue, StateValue.StringValue
{
    static StateValue fromJson(JsonElement element)
    {
        if (element == null || element.isJsonNull())
        {
            return null;
        }

        if (element.isJsonPrimitive())
        {
            JsonPrimitive primitive = element.getAsJsonPrimitive();

            if (primitive.isNumber())
            {
                return new NumberValue(primitive.getAsDouble());
            }

            if (primitive.isString())
            {
                return new StringValue(primitive.getAsString());
            }
        }

        throw new IllegalArgumentException("State value must be a number or string.");
    }

    static StateValue parse(String raw)
    {
        if (raw == null)
        {
            throw new IllegalArgumentException("Value cannot be null.");
        }

        try
        {
            if (raw.contains(".") || raw.contains("e") || raw.contains("E"))
            {
                return new NumberValue(Double.parseDouble(raw));
            }

            return new NumberValue(Long.parseLong(raw));
        }
        catch (NumberFormatException ignored)
        {
            if (raw.length() >= 2 && raw.startsWith("\"") && raw.endsWith("\""))
            {
                return new StringValue(raw.substring(1, raw.length() - 1));
            }

            return new StringValue(raw);
        }
    }

    JsonElement toJson();

    boolean isNumber();

    boolean isString();

    double asNumber();

    String asString();

    record NumberValue(double value) implements StateValue
    {
        @Override
        public JsonElement toJson()
        {
            double v = this.value;

            if (v == Math.rint(v) && !Double.isInfinite(v))
            {
                return new JsonPrimitive((long) v);
            }

            return new JsonPrimitive(v);
        }

        @Override
        public boolean isNumber()
        {
            return true;
        }

        @Override
        public boolean isString()
        {
            return false;
        }

        @Override
        public double asNumber()
        {
            return this.value;
        }

        @Override
        public String asString()
        {
            return String.valueOf(this.value);
        }
    }

    record StringValue(String value) implements StateValue
    {
        @Override
        public JsonElement toJson()
        {
            return new JsonPrimitive(this.value);
        }

        @Override
        public boolean isNumber()
        {
            return false;
        }

        @Override
        public boolean isString()
        {
            return true;
        }

        @Override
        public double asNumber()
        {
            throw new IllegalStateException("State value is not a number.");
        }

        @Override
        public String asString()
        {
            return this.value;
        }
    }
}
