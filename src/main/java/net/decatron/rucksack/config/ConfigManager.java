package net.decatron.rucksack.config;

import net.decatron.rucksack.RucksackPlugin;
import net.decatron.rucksack.core.RucksackManager;

public class ConfigManager implements RucksackManager {

    private final RucksackPlugin plugin;

    public ConfigManager(RucksackPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        plugin.getLogger().info("[ConfigManager] Configuracion cargada — storage=" + getStorageType() + ", language=" + getLanguage());
    }

    @Override
    public void shutdown() {
        plugin.getLogger().info("[ConfigManager] Apagado.");
    }

    public String getStorageType() {
        return plugin.getConfig().getString("storage.type", "sqlite");
    }

    public String getLanguage() {
        return plugin.getConfig().getString("language", "es");
    }
}
