package net.decatron.rucksack.render;

import net.decatron.rucksack.RucksackPlugin;
import net.decatron.rucksack.core.RucksackManager;

public class RenderManager implements RucksackManager {

    private final RucksackPlugin plugin;

    public RenderManager(RucksackPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        plugin.getLogger().info("[RenderManager] Inicializado.");
    }

    @Override
    public void shutdown() {
        plugin.getLogger().info("[RenderManager] Apagado.");
    }
}
