package net.decatron.rucksack;

import org.bukkit.plugin.java.JavaPlugin;

public final class RucksackPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("Decatron Rucksack v" + getDescription().getVersion() + " habilitado.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Decatron Rucksack deshabilitado.");
    }
}
