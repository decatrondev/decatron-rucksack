package net.decatron.rucksack.listeners;

import net.decatron.rucksack.RucksackPlugin;
import net.decatron.rucksack.core.RucksackManager;
import net.decatron.rucksack.util.BackpackItemUtil;
import net.decatron.rucksack.util.TierConfig;

import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ListenerManager implements RucksackManager, Listener {

    private final RucksackPlugin plugin;

    public ListenerManager(RucksackPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("[ListenerManager] Inicializado.");
    }

    @Override
    public void shutdown() {
        plugin.getLogger().info("[ListenerManager] Apagado.");
    }

    @EventHandler
    public void onCraftBackpack(CraftItemEvent event) {
        ItemStack result = event.getRecipe().getResult();

        // Solo nos interesa PAPER
        if (result.getType() != Material.PAPER) return;
        if (!result.hasItemMeta()) return;

        ItemMeta meta = result.getItemMeta();
        if (meta == null) return;

        // Verificar custom_model_data en rango 4001-4004
        if (!meta.hasCustomModelData()) return;
        int cmd = meta.getCustomModelData();

        // Buscar el tier que corresponde a este custom_model_data
        TierConfig tier = null;
        for (TierConfig t : TierConfig.values()) {
            if (t.getCustomModelData() == cmd) {
                tier = t;
                break;
            }
        }
        if (tier == null) return;

        // Reemplazar el resultado con el item correcto que tiene PDC
        event.setCurrentItem(BackpackItemUtil.createBackpackItem(tier));

        HumanEntity crafter = event.getWhoClicked();
        plugin.getLogger().info("[ListenerManager] Mochila crafteada por " + crafter.getName() + ": " + tier.getId());
    }
}
