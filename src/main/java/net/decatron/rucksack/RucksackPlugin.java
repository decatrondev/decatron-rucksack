package net.decatron.rucksack;

import net.decatron.rucksack.core.PluginCore;
import org.bukkit.plugin.java.JavaPlugin;

public final class RucksackPlugin extends JavaPlugin {

    private PluginCore pluginCore;

    @Override
    public void onEnable() {
        getLogger().info("Decatron Rucksack v" + getDescription().getVersion() + " — iniciando...");
        pluginCore = new PluginCore(this);
        pluginCore.initialize();
        // Instalar datapack automaticamente en la carpeta del mundo
        new net.decatron.rucksack.core.DatapackInstaller(this).installAndSetup();
        getLogger().info("Decatron Rucksack habilitado correctamente.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Decatron Rucksack — apagando...");
        if (pluginCore != null) {
            pluginCore.shutdown();
        }
        getLogger().info("Decatron Rucksack deshabilitado.");
    }

    public PluginCore getPluginCore() {
        return pluginCore;
    }
}
