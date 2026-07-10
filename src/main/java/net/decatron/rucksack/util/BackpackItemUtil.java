package net.decatron.rucksack.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

public final class BackpackItemUtil {

    private static final NamespacedKey TIER_KEY =
            new NamespacedKey("decatron", "backpack_tier");

    private BackpackItemUtil() {}

    /**
     * Crea el item placeholder (CHEST) con el PDC tag del tier correspondiente.
     */
    public static ItemStack createBackpackItem(TierConfig tier) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Nombre con colores usando legacy codes
        meta.setDisplayName(tier.getDisplayName());

        // Marcar el item con el tier en el PDC — esta es la "fuente de verdad"
        meta.getPersistentDataContainer().set(TIER_KEY, PersistentDataType.STRING, tier.getId());

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Retorna el TierConfig si el item es una mochila de Rucksack, empty si no lo es.
     * La identificacion se hace SOLO por PDC, nunca por nombre o material.
     */
    public static Optional<TierConfig> getBackpackTier(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return Optional.empty();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return Optional.empty();

        String tierId = meta.getPersistentDataContainer().get(TIER_KEY, PersistentDataType.STRING);
        if (tierId == null) return Optional.empty();

        return Optional.of(TierConfig.fromId(tierId));
    }

    public static NamespacedKey getTierKey() {
        return TIER_KEY;
    }
}
