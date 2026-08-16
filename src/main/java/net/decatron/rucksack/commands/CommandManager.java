package net.decatron.rucksack.commands;

import net.decatron.rucksack.RucksackPlugin;
import net.decatron.rucksack.config.RenderSettings;
import net.decatron.rucksack.core.RucksackManager;
import net.decatron.rucksack.data.Backpack;
import net.decatron.rucksack.data.StorageManager;
import net.decatron.rucksack.gui.GuiManager;
import net.decatron.rucksack.util.BackpackItemUtil;
import net.decatron.rucksack.util.TierConfig;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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

            // /rucksack ajuste ... — colocacion de la mochila en vivo
            if (args.length >= 1 && args[0].equalsIgnoreCase("ajuste")) {
                handleAjuste(player, args);
                return true;
            }

            // /rucksack give [tier] — da el item fisico con PDC correcto
            if (args.length >= 1 && args[0].equalsIgnoreCase("give")) {
                TierConfig tier = TierConfig.LEATHER;
                if (args.length >= 2) {
                    try {
                        tier = TierConfig.fromId(args[1].toLowerCase());
                    } catch (Exception e) {
                        player.sendMessage("\u00a7cTier invalido. Usa: leather, iron, diamond, netherite");
                        return true;
                    }
                }
                ItemStack backpackItem = BackpackItemUtil.createBackpackItem(tier);
                player.getInventory().addItem(backpackItem);
                player.sendMessage("\u00a7aRecibiste: " + tier.getDisplayName());
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

    // -------------------------------------------------------------------------
    // /rucksack ajuste — afinar la colocacion mirando el personaje en F5
    // -------------------------------------------------------------------------

    /** Cuanto mueve un "+" o un "-", por jugador. */
    private final java.util.Map<java.util.UUID, Double> pasos = new java.util.HashMap<>();

    private static final String[] PARAMS = {"separacion", "altura", "lateral", "escala", "giro", "inclinacion"};

    private void handleAjuste(Player player, String[] args) {
        if (!player.hasPermission("rucksack.admin") && !player.isOp()) {
            player.sendMessage("§cNo tenes permiso para ajustar la mochila.");
            return;
        }

        RenderSettings settings = plugin.getPluginCore().getConfigManager().getRenderSettings();
        double paso = pasos.getOrDefault(player.getUniqueId(), 0.05);

        // /rucksack ajuste  → estado actual + ayuda
        if (args.length == 1) {
            mostrarEstado(player, settings, paso);
            return;
        }

        String sub = args[1].toLowerCase();

        switch (sub) {
            case "guardar" -> {
                settings.save();
                String linea = settings.toOneLine();
                player.sendMessage("§aGuardado en config.yml. Copia esta linea:");
                player.sendMessage("§e" + linea);
                plugin.getLogger().info("[ajuste] Valores guardados -> " + linea);
                return;
            }
            case "reset" -> {
                settings.reset();
                aplicar(player);
                player.sendMessage("§aValores restaurados a los de fabrica.");
                mostrarEstado(player, settings, paso);
                return;
            }
            case "paso" -> {
                if (args.length < 3) {
                    player.sendMessage("§cUso: /rucksack ajuste paso <valor>");
                    return;
                }
                try {
                    paso = Math.abs(Double.parseDouble(args[2]));
                    pasos.put(player.getUniqueId(), paso);
                    player.sendMessage("§aPaso de ajuste: §f" + paso);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cValor invalido: " + args[2]);
                }
                return;
            }
            default -> { /* sigue abajo: es un parametro */ }
        }

        if (Double.isNaN(settings.get(sub))) {
            player.sendMessage("§cParametro desconocido: §f" + sub);
            player.sendMessage("§7Validos: §f" + String.join(", ", PARAMS));
            return;
        }

        if (args.length < 3) {
            player.sendMessage("§cUso: /rucksack ajuste " + sub + " <valor | + | ->");
            return;
        }

        String valorArg = args[2];
        double nuevo;

        if (valorArg.equals("+")) {
            nuevo = settings.get(sub) + paso;
        } else if (valorArg.equals("-")) {
            nuevo = settings.get(sub) - paso;
        } else {
            try {
                nuevo = Double.parseDouble(valorArg);
            } catch (NumberFormatException e) {
                player.sendMessage("§cValor invalido: §f" + valorArg);
                return;
            }
        }

        settings.set(sub, nuevo);
        aplicar(player);

        // Feedback en la barra de accion para no tapar la pantalla mientras se mira el personaje
        player.sendActionBar(net.kyori.adventure.text.Component.text(
                "§6" + sub + " §f" + String.format("%.3f", settings.get(sub))
                        + " §8(paso " + paso + ")"));
    }

    private void aplicar(Player player) {
        plugin.getPluginCore().getBackpackDisplayManager().refresh(player);
    }

    private void mostrarEstado(Player player, RenderSettings s, double paso) {
        player.sendMessage("§6== Colocacion de la mochila ==");
        player.sendMessage("§7separacion §f" + String.format("%.3f", s.getSeparacion())
                + "   §7altura §f" + String.format("%.3f", s.getAltura())
                + "   §7lateral §f" + String.format("%.3f", s.getLateral()));
        player.sendMessage("§7escala §f" + String.format("%.3f", s.getEscala())
                + "   §7giro §f" + String.format("%.1f", s.getGiro())
                + "   §7inclinacion §f" + String.format("%.1f", s.getInclina()));
        player.sendMessage("§8Paso actual: §f" + paso);
        player.sendMessage("§7Entra en F5 y usa: §f/rucksack ajuste <param> + §7o §f-");
        player.sendMessage("§7Cuando te guste: §f/rucksack ajuste guardar");
    }
}
