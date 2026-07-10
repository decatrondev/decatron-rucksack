package net.decatron.rucksack.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.EquippableComponent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

public final class BackpackItemUtil {

    private static final NamespacedKey TIER_KEY =
            new NamespacedKey("decatron", "backpack_tier");

    private BackpackItemUtil() {}

    /**
     * Crea el item de mochila (PAPER) con custom_model_data, atributos de armadura,
     * componente equippable y el PDC tag del tier correspondiente.
     */
    public static ItemStack createBackpackItem(TierConfig tier) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Nombre con colores usando legacy codes
        meta.setDisplayName(tier.getDisplayName());

        // custom_model_data unico por tier para el resource pack
        meta.setCustomModelData(tier.getCustomModelData());

        // Marcar el item con el tier en el PDC — esta es la "fuente de verdad"
        meta.getPersistentDataContainer().set(TIER_KEY, PersistentDataType.STRING, tier.getId());

        // Atributo de armadura en el slot de pechera
        meta.addAttributeModifier(
            Attribute.ARMOR,
            new AttributeModifier(
                new NamespacedKey("decatron", "backpack_armor_" + tier.getId()),
                tier.getArmor(),
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.CHEST
            )
        );

        // Atributo de toughness en el slot de pechera (solo si > 0)
        if (tier.getArmorToughness() > 0.0) {
            meta.addAttributeModifier(
                Attribute.ARMOR_TOUGHNESS,
                new AttributeModifier(
                    new NamespacedKey("decatron", "backpack_toughness_" + tier.getId()),
                    tier.getArmorToughness(),
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.CHEST
                )
            );
        }

        // Componente equippable: permite equipar en slot de pechera como elytra
        EquippableComponent equippable = meta.getEquippable();
        equippable.setSlot(EquipmentSlot.CHEST);
        equippable.setDispensable(true);
        equippable.setSwappable(true);
        meta.setEquippable(equippable);

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
