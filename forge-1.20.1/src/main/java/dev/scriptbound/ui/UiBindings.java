package dev.scriptbound.ui;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UiBindings
{
    private static final Pattern PLACEHOLDER = Pattern.compile("%([a-zA-Z0-9_.]+)%");

    private UiBindings() {}

    public static String scopeOf(String raw)
    {
        return raw != null && raw.equalsIgnoreCase("global") ? "global" : "player";
    }

    public static String key(String scope, String name)
    {
        return scopeOf(scope) + ":" + name;
    }

    private static String[] split(String token)
    {
        int dot = token.indexOf('.');

        if (dot > 0)
        {
            String prefix = token.substring(0, dot);

            if (prefix.equalsIgnoreCase("player") || prefix.equalsIgnoreCase("global"))
            {
                return new String[] { scopeOf(prefix), token.substring(dot + 1) };
            }
        }

        return new String[] { "player", token };
    }

    public static Set<String> collect(UiDefinition def)
    {
        Set<String> keys = new HashSet<>();

        for (UiElement element : def.elements)
        {
            collect(element, keys);
        }

        return keys;
    }

    private static void collect(UiElement element, Set<String> keys)
    {
        if (element.text != null)
        {
            Matcher matcher = PLACEHOLDER.matcher(element.text);

            while (matcher.find())
            {
                String[] sk = split(matcher.group(1));
                keys.add(key(sk[0], sk[1]));
            }
        }

        if (element.bindKey != null && !element.bindKey.isBlank())
        {
            keys.add(key(element.bindScope, element.bindKey));
        }

        for (UiElement child : element.children)
        {
            collect(child, keys);
        }
    }

    public static String apply(String text, Function<String, String> lookup)
    {
        if (text == null || text.indexOf('%') < 0)
        {
            return text == null ? "" : text;
        }

        Matcher matcher = PLACEHOLDER.matcher(text);
        StringBuilder out = new StringBuilder();

        while (matcher.find())
        {
            String[] sk = split(matcher.group(1));
            String value = lookup.apply(key(sk[0], sk[1]));
            matcher.appendReplacement(out, Matcher.quoteReplacement(value == null ? "" : value));
        }

        matcher.appendTail(out);
        return out.toString();
    }
}
