package ru.whitebeef.beeflibrary.handlers;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import ru.whitebeef.beeflibrary.BeefLibrary;
import ru.whitebeef.beeflibrary.chat.MessageType;
import ru.whitebeef.beeflibrary.commands.AbstractCommand;
import ru.whitebeef.beeflibrary.database.abstractions.Database;
import ru.whitebeef.beeflibrary.inventory.InventoryGUIManager;
import ru.whitebeef.beeflibrary.placeholderapi.PAPIUtils;
import ru.whitebeef.beeflibrary.utils.JedisUtils;
import ru.whitebeef.beeflibrary.utils.SoundType;

public class PluginHandler implements Listener {

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        Plugin plugin = event.getPlugin();

        if (!plugin.getDescription().getDepend().contains("BeefLibrary") &&
                !plugin.getDescription().getSoftDepend().contains("BeefLibrary")) {
            return;
        }

        MessageType.registerTypesSection(plugin, "messages");
        SoundType.registerTypesSection(plugin, "sounds");
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        Plugin plugin = event.getPlugin();
        if (!plugin.getDescription().getDepend().contains("BeefLibrary") &&
                !plugin.getDescription().getSoftDepend().contains("BeefLibrary")) {
            return;
        }

        BeefLibrary.getInstance().unregisterPlaceholders(plugin);
        InventoryGUIManager.getInstance().unregisterTemplates(plugin);
        SoundType.unregisterTypesSection(plugin);
        AbstractCommand.unregisterAllCommands(plugin);
        PAPIUtils.unregisterPlaceholders(plugin);
        MessageType.unregisterTypesSection(plugin);
        if (JedisUtils.isJedisEnabled()) {
            JedisUtils.unSubscribe(plugin);
        }
        for (Database database : Database.getDatabases(event.getPlugin())) {
            database.close();
        }
    }
}
