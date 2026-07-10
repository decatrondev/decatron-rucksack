package net.decatron.rucksack.commands;

import net.decatron.rucksack.RucksackPlugin;
import net.decatron.rucksack.core.RucksackManager;
import net.decatron.rucksack.data.Backpack;
import net.decatron.rucksack.data.StorageManager;
import net.decatron.rucksack.gui.GuiManager;
import net.decatron.rucksack.util.TierConfig;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandManager implements RucksackManager {

    private final RucksackPlugin plugin;
    private final GuiManager     guiManager;
    private final StorageManager storageManager;

    public CommandManager(RucksackPlugin plugin, GuiManager guiManager, StorageManager storageManager) {
        this.plugin         = plugin;
        this.guiManager     = guiManager;
        this.storageManager = storageManager;
    }

    @Override
    public void initialize() {
        // Registrar el comando /rucksack definido en paper-plugin.yml
        var cmd = plugin.getServer().getPluginCommand("rucksack");
        if (cmd != null) {
            cmd.setExecutor(new RucksackCommand());
        } else {
            plugin.getLogger().warning("[CommandManager] El comando 'rucksack' no esta registrado en paper-plugin.yml.");
        }
        plugin.getLogger().info("[CommandManager] Inicializado.");
    }

    @Override
    public void shutdown() {
        plugin.getLogger().info("[CommandManager] Apagado.");
    }

    // -------------------------------------------------------------------------
    // Comando /rucksack — temporal, se migrara a Brigadier en Fase 10
    // -------------------------------------------------------------------------

    private class RucksackCommand implements CommandExecutor {

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("\u00a7cEste comando solo puede ejecutarlo un jugador.");
                return true;
            }

            // Buscar mochila existente, o crear una nueva de tier leather
            storageManager.getStorage()
                    .loadBackpack(player.getUniqueId())
                    .thenAccept(optional -> {
                        Backpack backpack = optional.orElseGet(() -> {
                            plugin.getLogger().info("[CommandManager] Creando mochila nueva para "
                                    + player.getName() + " (tier: leather).");
                            return new Backpack(player.getUniqueId(), TierConfig.LEATHER.getId(),
                                    TierConfig.LEATHER.getSlots());
                        });

                        // Abrir el GUI debe hacerse en el hilo principal
                        plugin.getServer().getScheduler().runTask(plugin, () ->
                                guiManager.openBackpack(player, backpack)
                        );
                    })
                    .exceptionally(ex -> {
                        plugin.getLogger().severe("[CommandManager] Error al cargar mochila de "
                                + player.getName() + ": " + ex.getMessage());
                        plugin.getServer().getScheduler().runTask(plugin, () ->
                                player.sendMessage("\u00a7cError al cargar tu mochila. Intenta de nuevo.")
                        );
                        return null;
                    });

            return true;
        }
    }
}
