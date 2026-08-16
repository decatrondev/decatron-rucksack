package net.decatron.rucksack.render;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import net.decatron.rucksack.RucksackPlugin;
import net.decatron.rucksack.core.RucksackManager;
import net.decatron.rucksack.util.BackpackItemUtil;
import net.decatron.rucksack.util.TierConfig;
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
 * La entidad ItemDisplay se monta como PASAJERO del jugador: asi el cliente la
 * mueve junto a el de forma nativa, sin arrastre ni jitter por ping, y sin que
 * el servidor tenga que mandar un teletransporte por tick.
 *
 * Lo que el montaje NO resuelve:
 *
 * 1. El GIRO. El cliente alinea al pasajero con el yaw de la camara, pero la
 *    mochila va en el torso, que puede estar en diagonal respecto de la mirada.
 *    Sin compensar esa diferencia la mochila orbita alrededor del jugador al
 *    mover el mouse. Se corrige con getBodyYaw() via Transformation.
 *
 * 2. Las POSES. Un pasajero sigue la POSICION de la entidad, no la ANIMACION del
 *    modelo (que es puramente del cliente). Por eso las poses en las que el
 *    cuerpo deja de estar vertical — planear, nadar, agacharse — se aproximan a
 *    mano inclinando la mochila.
 *
 * Nota: no se puede usar el sistema nativo de equipment (que si sigue el
 * esqueleto y resuelve todas las poses gratis) porque solo admite texturas
 * sobre siluetas fijas, no geometria 3D propia.
 */
public class BackpackDisplayManager implements RucksackManager, Listener {

    private final RucksackPlugin plugin;
    private final Map<UUID, ItemDisplay> displays = new HashMap<>();
    private final Map<UUID, PoseState> lastState = new HashMap<>();
    private BukkitTask poseTask;

    /** Distancia hacia atras desde el punto de montaje (bloques). */
    private static final float BACK_OFFSET = 0.10f;
    /** Ajuste vertical desde el punto de montaje del pasajero. */
    private static final float HEIGHT_OFFSET = -0.35f;
    /** Escala del modelo sobre el cuerpo. */
    private static final float MODEL_SCALE = 0.85f;
    /** Giro fijo para que el frente del modelo apoye contra la espalda. */
    private static final float MODEL_FACING = 180f;
    /** Diferencia de giro (grados) a partir de la cual vale reenviar la transformacion. */
    private static final float YAW_EPSILON = 1.5f;

    public BackpackDisplayManager(RucksackPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        // La POSICION la resuelve el cliente por el montaje; este ciclo solo
        // corrige la ORIENTACION (torso vs camara) y la pose, y unicamente
        // reenvia cuando alguna de las dos cambio de verdad.
        poseTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
        plugin.getLogger().info("[BackpackDisplayManager] Inicializado (modo pasajero).");
    }

    @Override
    public void shutdown() {
        if (poseTask != null) {
            poseTask.cancel();
        }
        for (ItemDisplay display : displays.values()) {
            if (display != null && !display.isDead()) {
                display.remove();
            }
        }
        displays.clear();
        lastState.clear();
    }

    // -------------------------------------------------------------------------
    // Ciclo de poses
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
                lastState.remove(entry.getKey());
                it.remove();
                continue;
            }

            // Si algo lo desmonto (dimension, /ride, muerte), volver a engancharlo.
            if (!player.equals(display.getVehicle())) {
                player.addPassenger(display);
            }

            PoseState current = PoseState.of(player);
            PoseState previous = lastState.get(player.getUniqueId());
            if (previous == null || previous.differsFrom(current)) {
                applyTransformation(display, current);
                lastState.put(player.getUniqueId(), current);
            }
        }
    }

    /**
     * Coloca el modelo detras del torso y lo inclina segun la pose.
     *
     * El cliente ya gira al pasajero, pero lo hace con el yaw de la CAMARA, no
     * con el del cuerpo. Como el torso puede estar en diagonal respecto de la
     * mirada (mover el mouse quieto, o caminar de costado), hay que compensar la
     * diferencia entre ambos o la mochila orbita alrededor del jugador.
     *
     * Un yaw de Minecraft equivale a un rotateY negado en JOML, por eso la
     * compensacion es (camara - cuerpo) y no al reves.
     */
    private void applyTransformation(ItemDisplay display, PoseState state) {
        float height = HEIGHT_OFFSET + state.heightAdjust();
        float phi = (float) Math.toRadians(state.yawDelta());

        // La espalda del torso, girada desde el marco alineado a la camara.
        Vector3f offset = new Vector3f(0f, height, -BACK_OFFSET).rotateY(phi);

        Quaternionf rotation = new Quaternionf()
                .rotateY(phi + (float) Math.toRadians(MODEL_FACING))
                .rotateX((float) Math.toRadians(state.pitchAdjust()));

        display.setInterpolationDelay(0);
        display.setInterpolationDuration(3);
        display.setTransformation(new Transformation(
                offset,
                rotation,
                new Vector3f(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE),
                new Quaternionf()
        ));
    }

    /**
     * Estado visual del jugador que obliga a recolocar la mochila.
     *
     * yawDelta es cuanto esta girado el torso respecto de la camara: el montaje
     * alinea la mochila con la camara, asi que esta diferencia es lo que hay que
     * compensar para que quede pegada a la espalda y no orbitando.
     */
    private record PoseState(float yawDelta, boolean gliding, boolean swimming, boolean sneaking, boolean sleeping) {

        static PoseState of(Player player) {
            float headYaw = player.getLocation().getYaw();
            float bodyYaw = player.getBodyYaw();
            return new PoseState(
                    normalize(headYaw - bodyYaw),
                    player.isGliding(),
                    player.isSwimming(),
                    player.isSneaking(),
                    player.isSleeping()
            );
        }

        boolean differsFrom(PoseState other) {
            return gliding != other.gliding
                    || swimming != other.swimming
                    || sneaking != other.sneaking
                    || sleeping != other.sleeping
                    || Math.abs(normalize(yawDelta - other.yawDelta)) > YAW_EPSILON;
        }

        private static float normalize(float degrees) {
            while (degrees > 180f)  degrees -= 360f;
            while (degrees < -180f) degrees += 360f;
            return degrees;
        }

        /** Cuerpo horizontal (planear/nadar): la mochila se acuesta sobre la espalda. */
        float pitchAdjust() {
            if (gliding || swimming) return -90f;
            if (sneaking)            return -25f;
            return 0f;
        }

        float heightAdjust() {
            if (gliding || swimming) return 0.30f;
            if (sneaking)            return -0.15f;
            return 0f;
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
            player.addPassenger(display);
        } else {
            display.setItemStack(visual);
        }

        PoseState state = PoseState.of(player);
        applyTransformation(display, state);
        lastState.put(player.getUniqueId(), state);
    }

    public void remove(Player player) {
        ItemDisplay display = displays.remove(player.getUniqueId());
        lastState.remove(player.getUniqueId());
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

    /** Al reaparecer, si conserva la mochila puesta hay que volver a montarla. */
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
