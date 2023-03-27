package ru.whitebeef.beeflibrary.placeholderapi;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.intellij.lang.annotations.RegExp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.whitebeef.beeflibrary.BeefLibrary;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PAPIUtils {
    private static final TextReplacementConfig REMOVE_PLACEHOLDERS = TextReplacementConfig.builder().match("%(?!message\\b)[A-z0-9_]+%").replacement("").build();
    private static final Map<String, Function<CommandSender, Component>> registeredPlaceholders = new HashMap<>();

    /**
     * Плейсхолдер, который будет зарегистрирован будет иметь формат %pluginname_placeholder%
     *
     * @param plugin      Плагин будет регистрировать плейсхолдер, null - глобальный плейсхолдер
     * @param placeholder Плейсхолдер, без %%
     * @param function    Функцуия, в которую будет пердаваться CommandSender, для которого будет заменяться плейсхолдер, возвращаться должна компонента
     */
    public static void registerPlaceholder(@Nullable Plugin plugin, @NotNull String placeholder, @NotNull Function<CommandSender, Component> function) {
        String toRegister;
        if (plugin != null) {
            toRegister = "%" + plugin.getName().toLowerCase() + "_" + placeholder + "%";
        } else {
            toRegister = "%" + placeholder + "%";
        }
        if (placeholder.isEmpty()) {
            throw new IllegalArgumentException("Placeholder must be not empty!");
        }
        if (!registeredPlaceholders.containsKey(toRegister)) {
            registeredPlaceholders.put(toRegister, function);
        } else {
            throw new IllegalArgumentException("Placeholder " + toRegister + " already registered!");
        }
    }

    public static boolean isRegisteredPlaceholder(String placeholder) {
        return registeredPlaceholders.containsKey(placeholder);
    }

    public static Function<CommandSender, Component> getRegisteredPlaceHolder(String placeholder) {
        return registeredPlaceholders.getOrDefault(placeholder, commandSender -> PlainTextComponentSerializer.plainText().deserialize(placeholder));
    }

    public static Component setPlaceholders(@Nullable CommandSender sender, @NotNull Component component) {
        String line = GsonComponentSerializer.gson().serialize(component);
        if (sender == null) {
            return component;
        }
        component = replaceCustomPlaceHolders(sender, component);

        if (!(sender instanceof Player player)) {
            component = GsonComponentSerializer.gson().deserialize(line)
                    .replaceText(TextReplacementConfig.builder()
                            .match("%player_name%")
                            .replacement(sender.name()).build()
                    );
            component = component.replaceText(REMOVE_PLACEHOLDERS);
            return component;
        }

        return BeefLibrary.getInstance().isPlaceholderAPIHooked() ? GsonComponentSerializer.gson().deserialize(PlaceholderAPI.setPlaceholders(player, line)) : component;
    }

    public static String setPlaceholders(CommandSender sender, String text) {
        if (!(sender instanceof Player player)) {
            return text.replaceAll("%player-name-component%", "%player_name%").replaceAll("%player_name%", sender.getName()).replaceAll("%(?!message\\b)[A-z0-9_]+%", "");
        }
        return BeefLibrary.getInstance().isPlaceholderAPIHooked() ? PlaceholderAPI.setPlaceholders(player, text) : text;
    }

    private static Component replaceCustomPlaceHolders(CommandSender commandSender, Component component) {
        String gson = GsonComponentSerializer.gson().serialize(component);

        Set<String> toReplace = new HashSet<>();

        Pattern pattern = Pattern.compile("%([A-z0-9]+)_([A-z0-9]+)%");

        Matcher matcher = pattern.matcher(gson);
        int index = 0;
        while (matcher.find(index)) {
            String placeholder = gson.substring(matcher.start(), matcher.end());
            if (isRegisteredPlaceholder(placeholder)) {
                toReplace.add(placeholder);
            }
            index = matcher.end() + 1;
        }

        for (@RegExp String placeholder : toReplace) {
            component = component.replaceText(TextReplacementConfig.builder().match(placeholder).replacement(getRegisteredPlaceHolder(placeholder).apply(commandSender)).build());
        }

        return component;
    }
}
