package net.decatron.rucksack.license;

import net.decatron.rucksack.RucksackPlugin;
import net.decatron.rucksack.core.RucksackManager;

public class LicenseManager implements RucksackManager {

    private final RucksackPlugin plugin;

    public LicenseManager(RucksackPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        plugin.getLogger().info("[LicenseManager] Inicializado.");
    }

    @Override
    public void shutdown() {
        plugin.getLogger().info("[LicenseManager] Apagado.");
    }
}
