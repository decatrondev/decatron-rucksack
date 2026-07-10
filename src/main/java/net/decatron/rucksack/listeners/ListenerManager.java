package net.decatron.rucksack.listeners;

import net.decatron.rucksack.RucksackPlugin;
import net.decatron.rucksack.core.RucksackManager;

public class ListenerManager implements RucksackManager {

    private final RucksackPlugin plugin;

    public ListenerManager(RucksackPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        plugin.getLogger().info("[ListenerManager] Inicializado.");
    }

    @Override
    public void shutdown() {
        plugin.getLogger().info("[ListenerManager] Apagado.");
    }
}
