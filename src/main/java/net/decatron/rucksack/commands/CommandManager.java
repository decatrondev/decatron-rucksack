package net.decatron.rucksack.commands;

import net.decatron.rucksack.RucksackPlugin;
import net.decatron.rucksack.core.RucksackManager;

public class CommandManager implements RucksackManager {

    private final RucksackPlugin plugin;

    public CommandManager(RucksackPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        plugin.getLogger().info("[CommandManager] Inicializado.");
    }

    @Override
    public void shutdown() {
        plugin.getLogger().info("[CommandManager] Apagado.");
    }
}
