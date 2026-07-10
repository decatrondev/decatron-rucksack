package net.decatron.rucksack.data;

import net.decatron.rucksack.RucksackPlugin;
import net.decatron.rucksack.core.RucksackManager;

public class StorageManager implements RucksackManager {

    private final RucksackPlugin plugin;

    public StorageManager(RucksackPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        plugin.getLogger().info("[StorageManager] Inicializado.");
    }

    @Override
    public void shutdown() {
        plugin.getLogger().info("[StorageManager] Apagado.");
    }
}
