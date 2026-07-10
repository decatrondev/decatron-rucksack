package net.decatron.rucksack.gui;

import net.decatron.rucksack.RucksackPlugin;
import net.decatron.rucksack.core.RucksackManager;
import net.decatron.rucksack.data.Backpack;
import net.decatron.rucksack.data.StorageManager;
import org.bukkit.entity.Player;

public class GuiManager implements RucksackManager {

    private final RucksackPlugin plugin;
    private final StorageManager storageManager;

    public GuiManager(RucksackPlugin plugin, StorageManager storageManager) {
        this.plugin         = plugin;
        this.storageManager = storageManager;
    }

    @Override
    public void initialize() {
        BackpackGuiListener listener = new BackpackGuiListener(
                storageManager,
                plugin.getLogger()
        );
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        plugin.getLogger().info("[GuiManager] Inicializado — BackpackGuiListener registrado.");
    }

    @Override
    public void shutdown() {
        plugin.getLogger().info("[GuiManager] Apagado.");
    }

    /**
     * Abre el inventario GUI de la mochila para el jugador.
     */
    public void openBackpack(Player player, Backpack backpack) {
        BackpackGui.open(player, backpack);
    }
}
