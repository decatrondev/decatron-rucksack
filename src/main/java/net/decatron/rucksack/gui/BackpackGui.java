package net.decatron.rucksack.gui;

import net.decatron.rucksack.data.Backpack;
import net.decatron.rucksack.util.TierConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Construye y abre el inventario GUI de la mochila para un jugador.
 */
public class BackpackGui {

    /**
     * Crea el inventario correspondiente al tier de la mochila,
     * carga su contenido persistido y lo abre para el jugador.
     */
    public static void open(Player player, Backpack backpack) {
        TierConfig tier = TierConfig.fromId(backpack.getTier());

        BackpackHolder holder = new BackpackHolder(backpack, player);

        // Crear el inventario con el holder propio y el titulo con colores
        Inventory inv = Bukkit.createInventory(holder, tier.getSlots(), tier.getDisplayName());

        // Cargar el contenido guardado si existe
        ItemStack[] saved = backpack.getContents();
        if (saved != null) {
            // Copiar solo hasta el minimo entre slots del tier y slots guardados
            int limit = Math.min(saved.length, tier.getSlots());
            for (int i = 0; i < limit; i++) {
                if (saved[i] != null) {
                    inv.setItem(i, saved[i]);
                }
            }
        }

        // Enlazar el inventario con el holder para que el listener pueda accederlo
        holder.setInventory(inv);

        player.openInventory(inv);
    }
}
