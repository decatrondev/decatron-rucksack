package net.decatron.rucksack.render;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import net.decatron.rucksack.RucksackPlugin;
import net.decatron.rucksack.core.RucksackManager;
import net.decatron.rucksack.util.BackpackItemUtil;
import net.decatron.rucksack.util.TierConfig;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Muestra el modelo 3D real de la mochila enganchado a la espalda del jugador
 * usando un ItemDisplay que sigue su posicion/rotacion.
 *
 * Necesario porque el componente "equippable" de Minecraft NO soporta geometria
 * 3D custom en el cuerpo (solo re-texturiza siluetas fijas del juego: humanoid,
 * wings, wolf_body, etc). El "equippable" del item se deja solo para los stats
 * de armadura, no para el render visual.
 */
public class BackpackDisplayManager implements RucksackManager, Listener {

    private final RucksackPlugin plugin;
    private final Map<UUID, ItemDisplay> displays = new HashMap<>();
    private BukkitTask followTask;

    private static final double BACK_OFFSET = 0.32;
    private static final double HEIGHT_OFFSET = 1.25;
    private static final double SNEAK_HEIGHT_DROP = 0.30;

    public BackpackDisplayManager(RucksackPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        followTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
        plugin.getLogger().info("[BackpackDisplayManager] Inicializado.");
    }

    @Override
    public void shutdown() {
        if (followTask != null) {
            followTask.cancel();
        }
        for (ItemDisplay display : displays.values()) {
            if (display != null && !display.isDead()) {
                display.remove();
            }
        }
        displays.clear();
    }

    // -------------------------------------------------------------------------
    // Ciclo de seguimiento
    // -------------------------------------------------------------------------

    private int tickCounter = 0;

    private void tick() {
        tickCounter++;
        boolean logThisTick = (tickCounter % 100 == 0); // cada 5s aprox, solo mientras depuramos

        if (logThisTick) {
            plugin.getLogger().info("[BackpackDisplayManager] tick — mochilas activas: " + displays.size());
        }

        for (Map.Entry<UUID, ItemDisplay> entry : displays.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            ItemDisplay display = entry.getValue();
            if (player == null || !player.isOnline() || display == null || display.isDead()) {
                if (logThisTick) {
                    plugin.getLogger().warning("[BackpackDisplayManager] entrada invalida para " + entry.getKey()
                            + " (player null/offline: " + (player == null || !player.isOnline())
                            + ", display null/dead: " + (display == null || display.isDead()) + ")");
                }
                continue;
            }
            updateTransform(player, display);
            if (logThisTick) {
                plugin.getLogger().info("[BackpackDisplayManager] " + player.getName()
                        + " en " + formatLoc(player.getLocation())
                        + " -> display en " + formatLoc(display.getLocation()));
            }
        }
    }

    private String formatLoc(Location loc) {
        return String.format("(%.2f, %.2f, %.2f) yaw=%.1f", loc.getX(), loc.getY(), loc.getZ(), loc.getYaw());
    }

    private void updateTransform(Player player, ItemDisplay display) {
        Location loc = player.getLocation();
        float yaw = loc.getYaw();
        double yawRad = Math.toRadians(yaw);

        double backX = -Math.sin(yawRad) * BACK_OFFSET;
        double backZ = Math.cos(yawRad) * BACK_OFFSET;
        double height = player.isSneaking() ? (HEIGHT_OFFSET - SNEAK_HEIGHT_DROP) : HEIGHT_OFFSET;

        Location target = loc.clone().add(backX, height, backZ);
        target.setYaw(yaw);
        target.setPitch(0f);

        if (display.getInterpolationDuration() != 3) {
            display.setInterpolationDuration(3);
            display.setInterpolationDelay(0);
            display.setTeleportDuration(3);
        }
        display.teleport(target);

        // El giro visual del modelo se hace via Transformation, no via yaw de la entidad
        // (los Display entities ignoran su propio yaw/pitch para el render del item).
        Quaternionf rotation = new Quaternionf().rotateY((float) Math.toRadians(180 - yaw));
        display.setTransformation(new Transformation(
                new Vector3f(0f, 0f, 0f),
                rotation,
                new Vector3f(1f, 1f, 1f),
                new Quaternionf()
        ));
    }

    // -------------------------------------------------------------------------
    // Spawnear / actualizar / remover
    // -------------------------------------------------------------------------

    public void spawnOrUpdate(Player player, TierConfig tier) {
        ItemStack visual = BackpackItemUtil.createBackpackItem(tier);
        ItemDisplay display = displays.get(player.getUniqueId());

        if (display == null || display.isDead()) {
            display = player.getWorld().spawn(player.getLocation(), ItemDisplay.class, d -> {
                d.setItemStack(visual);
                d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                d.setBillboard(Display.Billboard.FIXED);
                d.setPersistent(false);
            });
            displays.put(player.getUniqueId(), display);
            plugin.getLogger().info("[BackpackDisplayManager] Display CREADO para " + player.getName()
                    + " (tier=" + tier.getId() + "), total activos: " + displays.size());
        } else {
            display.setItemStack(visual);
            plugin.getLogger().info("[BackpackDisplayManager] Display ACTUALIZADO para " + player.getName()
                    + " (tier=" + tier.getId() + ")");
        }
        updateTransform(player, display);
    }

    public void remove(Player player) {
        ItemDisplay display = displays.remove(player.getUniqueId());
        if (display != null && !display.isDead()) {
            display.remove();
        }
    }

    // -------------------------------------------------------------------------
    // Listeners
    // -------------------------------------------------------------------------

    @EventHandler
    public void onArmorChange(PlayerArmorChangeEvent event) {
        if (event.getSlotType() != PlayerArmorChangeEvent.SlotType.CHEST) {
            return;
        }
        BackpackItemUtil.getBackpackTier(event.getNewItem())
                .ifPresentOrElse(
                        tier -> spawnOrUpdate(event.getPlayer(), tier),
                        () -> remove(event.getPlayer())
                );
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        BackpackItemUtil.getBackpackTier(player.getInventory().getChestplate())
                .ifPresent(tier -> spawnOrUpdate(player, tier));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        remove(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        remove(event.getEntity());
    }
}
