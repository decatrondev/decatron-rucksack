package net.decatron.rucksack.gui;

import net.decatron.rucksack.RucksackPlugin;
import net.decatron.rucksack.core.RucksackManager;

public class GuiManager implements RucksackManager {

    private final RucksackPlugin plugin;

    public GuiManager(RucksackPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        plugin.getLogger().info("[GuiManager] Inicializado.");
    }

    @Override
    public void shutdown() {
        plugin.getLogger().info("[GuiManager] Apagado.");
    }
}
