package net.decatron.rucksack.config;

import net.decatron.rucksack.RucksackPlugin;
import net.decatron.rucksack.core.RucksackManager;

public class ConfigManager implements RucksackManager {

    private final RucksackPlugin plugin;
    private final RenderSettings renderSettings;

    public ConfigManager(RucksackPlugin plugin) {
        this.plugin = plugin;
        this.renderSettings = new RenderSettings(plugin);
    }

    @Override
    public void initialize() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        renderSettings.load();
        plugin.getLogger().info("[ConfigManager] Configuracion cargada — storage=" + getStorageType() + ", language=" + getLanguage());
        plugin.getLogger().info("[ConfigManager] Colocacion en espalda: " + renderSettings.toOneLine());
    }

    public RenderSettings getRenderSettings() {
        return renderSettings;
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

    /**
     * Retorna la configuracion del plugin (delegada al plugin principal).
     */
    public org.bukkit.configuration.file.FileConfiguration getConfig() {
        return plugin.getConfig();
    }
}
