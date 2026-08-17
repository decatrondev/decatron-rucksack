package net.decatron.rucksack.render;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import net.decatron.rucksack.RucksackPlugin;
import net.decatron.rucksack.config.ConfigManager;
import net.decatron.rucksack.config.RenderSettings;
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
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Muestra el modelo 3D de la mochila en la espalda del jugador.
 *
 * La entidad se posiciona a mano, NO montada como pasajero. El montaje parecia
 * mejor (el cliente movia la mochila solo, sin arrastre) pero alineaba todo con
 * el yaw de la CAMARA, no el del torso: al mover el mouse la mochila orbitaba
 * alrededor del jugador. Esa rotacion la aplica el cliente y no es observable
 * desde el servidor, o sea que solo se podia corregir a ciegas.
 *
 * Aca en cambio todo es explicito: la posicion se calcula con getBodyYaw() y la
 * orientacion entera vive en la Transformation, con la entidad siempre en yaw 0
 * para que el espacio local coincida con el del mundo.
 *
 * El costo es un teletransporte por jugador por tick y algo de arrastre al
 * correr, mitigado con interpolacion del lado del cliente.
 *
 * Los offsets no estan hardcodeados: se ajustan en vivo con /rucksack ajuste,
 * porque dependen de como se ve en pantalla y no de un calculo.
 */
public class BackpackDisplayManager implements RucksackManager, Listener {

    private final RucksackPlugin plugin;
    private final ConfigManager  configManager;

    private final Map<UUID, ItemDisplay> displays = new HashMap<>();
    /** Ultima posicion conocida, para estimar hacia donde se esta moviendo. */
    private final Map<UUID, Location> lastLocations = new HashMap<>();
    private BukkitTask followTask;

    /** Tope de correccion por prediccion, para que un teletransporte no la dispare lejos. */
    private static final double MAX_PREDICTION = 0.6;

    public BackpackDisplayManager(RucksackPlugin plugin, ConfigManager configManager) {
        this.plugin        = plugin;
        this.configManager = configManager;
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
        lastLocations.clear();
    }

    // -------------------------------------------------------------------------
    // Seguimiento
    // -------------------------------------------------------------------------

    private void tick() {
        Iterator<Map.Entry<UUID, ItemDisplay>> it = displays.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ItemDisplay> entry = it.next();
            Player player = plugin.getServer().getPlayer(entry.getKey());
            ItemDisplay display = entry.getValue();

            if (player == null || !player.isOnline() || display == null || display.isDead()) {
                if (display != null && !display.isDead()) {
                    display.remove();
                }
                lastLocations.remove(entry.getKey());
                it.remove();
                continue;
            }

            follow(player, display);
        }
    }

    /**
     * Coloca la mochila detras del torso del jugador.
     *
     * Se usa el yaw del CUERPO y no el de la camara: son distintos (mover el
     * mouse quieto gira solo la cabeza, y caminar de costado pone el torso en
     * diagonal), y la mochila cuelga del torso.
     */
    private void follow(Player player, ItemDisplay display) {
        RenderSettings s = configManager.getRenderSettings();

        Location playerLoc = player.getLocation();
        float bodyYaw = player.getBodyYaw();
        double yawRad = Math.toRadians(bodyYaw);

        // Con yaw 0 el jugador mira al sur (+Z), asi que adelante = (-sin, cos).
        double forwardX = -Math.sin(yawRad);
        double forwardZ =  Math.cos(yawRad);
        // La derecha del jugador es adelante girado -90 grados.
        double rightX = -forwardZ;
        double rightZ =  forwardX;

        double sep = s.getSeparacion();
        double lat = s.getLateral();

        Vector3f offset = new Vector3f(
                (float) (-forwardX * sep + rightX * lat),
                (float) s.getAltura(),
                (float) (-forwardZ * sep + rightZ * lat)
        );

        float scale = (float) s.getEscala();

        Quaternionf rotation = new Quaternionf()
                .rotateY((float) Math.toRadians(s.getGiro() - bodyYaw))
                .rotateX((float) Math.toRadians(s.getInclina()));

        // La entidad se mantiene en yaw/pitch 0: toda la orientacion va en la
        // Transformation, asi el espacio local coincide con el del mundo y no
        // hay dos rotaciones distintas peleandose.
        Location target = playerLoc.clone();
        target.setYaw(0f);
        target.setPitch(0f);

        // El servidor siempre ve al jugador un paso atras de donde su cliente ya
        // lo dibujo, y por eso al correr la mochila queda colgando atras. Se
        // compensa adelantandola en la direccion en la que se venia moviendo.
        Location previous = lastLocations.get(player.getUniqueId());
        double pred = s.getPrediccion();
        if (previous != null && pred > 0 && previous.getWorld() == playerLoc.getWorld()) {
            double dx = clampPrediction((playerLoc.getX() - previous.getX()) * pred);
            double dy = clampPrediction((playerLoc.getY() - previous.getY()) * pred);
            double dz = clampPrediction((playerLoc.getZ() - previous.getZ()) * pred);
            target.add(dx, dy, dz);
        }
        lastLocations.put(player.getUniqueId(), playerLoc.clone());

        // El suavizado tiene que coincidir con cada cuanto actualizamos (1 tick).
        // Si es mayor, la mochila nunca llega a destino: queda despegada al correr
        // y trabada de costado al girar rapido.
        int smoothing = s.getSuavizado();
        if (display.getTeleportDuration() != smoothing) {
            display.setTeleportDuration(smoothing);
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(smoothing);
        }

        display.teleport(target);
        display.setTransformation(new Transformation(
                offset,
                rotation,
                new Vector3f(scale, scale, scale),
                new Quaternionf()
        ));
    }

    private static double clampPrediction(double value) {
        return Math.max(-MAX_PREDICTION, Math.min(MAX_PREDICTION, value));
    }

    /** Reaplica la colocacion ya mismo (usado tras cambiar un valor en vivo). */
    public void refresh(Player player) {
        ItemDisplay display = displays.get(player.getUniqueId());
        if (display != null && !display.isDead()) {
            follow(player, display);
        }
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
                d.setViewRange(0.6f);
            });
            displays.put(player.getUniqueId(), display);
        } else {
            display.setItemStack(visual);
        }

        follow(player, display);
    }

    public void remove(Player player) {
        lastLocations.remove(player.getUniqueId());
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            BackpackItemUtil.getBackpackTier(player.getInventory().getChestplate())
                    .ifPresent(tier -> spawnOrUpdate(player, tier));
        }, 5L);
    }
}
