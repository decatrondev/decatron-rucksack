package net.decatron.rucksack.render;

import net.decatron.rucksack.RucksackPlugin;
import net.decatron.rucksack.config.ConfigManager;
import net.decatron.rucksack.core.RucksackManager;
import net.decatron.rucksack.gui.GuiManager;
import net.decatron.rucksack.data.StorageManager;
import net.decatron.rucksack.data.Backpack;
import net.decatron.rucksack.util.BackpackItemUtil;
import net.decatron.rucksack.util.TierConfig;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

public class RenderManager implements RucksackManager {

    private final RucksackPlugin  plugin;
    private final ConfigManager   configManager;
    private final GuiManager      guiManager;
    private final StorageManager  storageManager;

    // UUID fijo para el resource pack de Rucksack
    private static final UUID RP_UUID = UUID.fromString("a1b2c3d4-0000-0000-0000-ba5e40000001");

    public RenderManager(RucksackPlugin plugin,
                         ConfigManager configManager,
                         GuiManager guiManager,
                         StorageManager storageManager) {
        this.plugin         = plugin;
        this.configManager  = configManager;
        this.guiManager     = guiManager;
        this.storageManager = storageManager;
    }

    @Override
    public void initialize() {
        // Registrar todos los listeners de render
        plugin.getServer().getPluginManager().registerEvents(new RenderListener(), plugin);
        plugin.getLogger().info("[RenderManager] Inicializado — listeners registrados.");

        // Extraer datapack a plugins/Rucksack/datapack/
        extractDatapack();

        // Verificar configuracion del resource pack
        boolean rpEnabled = configManager.getConfig().getBoolean("resourcepack.enabled", true);
        String rpUrl = configManager.getConfig().getString("resourcepack.url", "");
        if (rpEnabled && (rpUrl == null || rpUrl.isBlank())) {
            plugin.getLogger().warning("[RenderManager] Resource pack habilitado pero 'resourcepack.url' esta vacio. " +
                    "El resource pack NO se enviara a los jugadores hasta que se configure la URL publica.");
        }
    }

    @Override
    public void shutdown() {
        plugin.getLogger().info("[RenderManager] Apagado.");
    }

    // -------------------------------------------------------------------------
    // Listeners internos
    // -------------------------------------------------------------------------

    private class RenderListener implements Listener {

        /**
         * RIGHT_CLICK con mochila en mano:
         *  - Si el slot de pechera esta vacio: equipar la mochila.
         *  - Si el jugador ya tiene la mochila equipada en pechera: abrir el GUI.
         */
        @EventHandler(priority = EventPriority.LOWEST)
        public void onPlayerInteract(PlayerInteractEvent event) {
            // Solo click derecho (aire o bloque), y solo con la mano principal
            if (event.getHand() != EquipmentSlot.HAND) return;
            if (event.getAction() != Action.RIGHT_CLICK_AIR
                    && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

            Player player = event.getPlayer();
            ItemStack inHand = event.getItem();
            ItemStack chestSlot = player.getInventory().getChestplate();

            // CASO 1: Shift + click derecho con mano vacia o cualquier item
            // Si tiene mochila equipada en pechera → abrir GUI
            if (player.isSneaking()) {
                Optional<TierConfig> equippedOpt = BackpackItemUtil.getBackpackTier(chestSlot);
                if (equippedOpt.isPresent()) {
                    event.setCancelled(true);
                    openBackpackGui(player, equippedOpt.get());
                    return;
                }
            }

            // CASO 2: Click derecho con mochila EN MANO
            if (inHand == null) return;
            Optional<TierConfig> tierOpt = BackpackItemUtil.getBackpackTier(inHand);
            if (tierOpt.isEmpty()) return;

            TierConfig tier = tierOpt.get();
            event.setCancelled(true);

            if (player.isSneaking()) {
                // Shift + click derecho con mochila en mano → abrir GUI directamente
                openBackpackGui(player, tier);
            } else if (chestSlot == null || chestSlot.getType().isAir()) {
                // Click derecho normal + pechera vacia → equipar
                player.getInventory().setChestplate(inHand.clone());
                inHand.subtract(1);
                player.sendMessage("\u00a7aEquipaste tu " + tier.getDisplayName() + "\u00a7a.");
            } else {
                Optional<TierConfig> equippedOpt = BackpackItemUtil.getBackpackTier(chestSlot);
                if (equippedOpt.isPresent()) {
                    // Tiene mochila equipada → abrir GUI
                    openBackpackGui(player, equippedOpt.get());
                } else {
                    // Tiene otra pechera puesta
                    player.sendMessage("\u00a7cDebes quitar tu pechera antes de equipar la mochila.");
                }
            }
        }

        /**
         * Detecta /trigger mochila (con o sin valor).
         * Completamente invisible en el chat — el jugador asigna su tecla
         * favorita en Minecraft > Controles a este comando.
         */
        @EventHandler(priority = EventPriority.LOWEST)
        public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
            String msg = event.getMessage().toLowerCase().trim();
            if (!msg.equals("/trigger mochila") && !msg.startsWith("/trigger mochila ")) return;

            Player player = (Player) event.getPlayer();

            // Cancelar para que no se procese como comando normal
            event.setCancelled(true);

            // Verificar que tiene mochila equipada
            ItemStack chestSlot = player.getInventory().getChestplate();
            Optional<TierConfig> tierOpt = BackpackItemUtil.getBackpackTier(chestSlot);

            if (tierOpt.isEmpty()) {
                // No tiene mochila equipada — silencioso
                return;
            }

            // Abrir el GUI
            openBackpackGui(player, tierOpt.get());
        }

        /**
         * Al morir: la mochila cae como item con su PDC intacto.
         * El contenido sigue guardado en DB — al recoger el item y abrirlo
         * con /rucksack se cargan los datos automaticamente.
         */
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onPlayerDeath(PlayerDeathEvent event) {
            Player player = event.getEntity();
            ItemStack chestSlot = player.getInventory().getChestplate();
            if (chestSlot == null) return;

            Optional<TierConfig> tierOpt = BackpackItemUtil.getBackpackTier(chestSlot);
            if (tierOpt.isEmpty()) return;

            // La mochila dropea normalmente (como cualquier item de pechera).
            // Su PDC esta intacto — al recogerla y abrirla carga el contenido de DB.
            plugin.getLogger().info("[RenderManager] " + player.getName()
                    + " murio con mochila equipada (" + tierOpt.get().getId() + ") — dropa normalmente.");
        }

        /**
         * Al conectarse: enviar el resource pack si esta configurado.
         */
        @EventHandler(priority = EventPriority.MONITOR)
        public void onPlayerJoin(PlayerJoinEvent event) {
            boolean rpEnabled = configManager.getConfig().getBoolean("resourcepack.enabled", true);
            if (!rpEnabled) return;

            String rpUrl = configManager.getConfig().getString("resourcepack.url", "");
            if (rpUrl == null || rpUrl.isBlank()) return;

            boolean force = configManager.getConfig().getBoolean("resourcepack.force", true);
            Player player = event.getPlayer();

            try {
                Component kickMessage = force
                        ? Component.text("\u00a7cNecesitas aceptar el resource pack de Decatron Rucksack para jugar en este servidor.")
                        : null;

                // SHA vacio — el servidor usara el URL tal cual.
                // En produccion, configurar un hash SHA-1 real para evitar redescargas.
                player.setResourcePack(RP_UUID, rpUrl, (String) null, kickMessage, force);

            } catch (Exception e) {
                plugin.getLogger().warning("[RenderManager] Error al enviar resource pack a "
                        + player.getName() + ": " + e.getMessage());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void extractDatapack() {
        java.io.File datapackDir = new java.io.File(plugin.getDataFolder(), "datapack");
        if (!datapackDir.exists()) {
            datapackDir.mkdirs();
        }

        String[] datapackFiles = {
            "datapack/pack.mcmeta",
            "datapack/data/decatron/function/setup.mcfunction"
        };

        for (String resourcePath : datapackFiles) {
            java.io.File dest = new java.io.File(plugin.getDataFolder().getPath() + "/" + resourcePath);
            if (!dest.getParentFile().exists()) {
                dest.getParentFile().mkdirs();
            }
            if (!dest.exists()) {
                try (java.io.InputStream in = plugin.getResource(resourcePath)) {
                    if (in == null) {
                        plugin.getLogger().warning("[RenderManager] No se encontro el recurso: " + resourcePath);
                        continue;
                    }
                    java.nio.file.Files.copy(in, dest.toPath());
                } catch (Exception e) {
                    plugin.getLogger().warning("[RenderManager] Error al extraer " + resourcePath + ": " + e.getMessage());
                }
            }
        }

        plugin.getLogger().info("[RenderManager] Datapack extraido en plugins/Rucksack/datapack/ " +
                "— copialo a la carpeta datapacks/ de tu mundo y ejecuta /reload para activar el trigger.");
    }

    private void openBackpackGui(Player player, TierConfig tier) {
        storageManager.getStorage()
                .loadBackpack(player.getUniqueId())
                .thenAccept(optional -> {
                    Backpack backpack = optional.orElseGet(() ->
                            new Backpack(player.getUniqueId(), tier.getId(), tier.getSlots())
                    );
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            guiManager.openBackpack(player, backpack)
                    );
                })
                .exceptionally(ex -> {
                    plugin.getLogger().severe("[RenderManager] Error al cargar mochila de "
                            + player.getName() + ": " + ex.getMessage());
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            player.sendMessage("\u00a7cError al abrir tu mochila. Intenta de nuevo.")
                    );
                    return null;
                });
    }
}
