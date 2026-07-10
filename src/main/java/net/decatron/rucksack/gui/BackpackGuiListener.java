package net.decatron.rucksack.gui;

import net.decatron.rucksack.data.Backpack;
import net.decatron.rucksack.data.StorageManager;
import net.decatron.rucksack.util.BackpackItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.logging.Logger;

public class BackpackGuiListener implements Listener {

    private final StorageManager storageManager;
    private final Logger log;

    public BackpackGuiListener(StorageManager storageManager, Logger log) {
        this.storageManager = storageManager;
        this.log = log;
    }

    // -------------------------------------------------------------------------
    // InventoryClickEvent
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        // Si el inventario superior no es una mochila de Rucksack, ignorar
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof BackpackHolder backpackHolder)) return;

        Inventory backpackInv = backpackHolder.getInventory();
        ItemStack cursor      = event.getCursor();
        ItemStack current     = event.getCurrentItem();

        // Bloquear colocar una mochila dentro de otra mochila (cursor)
        if (cursor != null && BackpackItemUtil.getBackpackTier(cursor).isPresent()) {
            // Solo bloquear si el click target es un slot de la mochila
            if (event.getClickedInventory() != null
                    && event.getClickedInventory().equals(backpackInv)) {
                event.setCancelled(true);
                return;
            }
        }

        // Bloquear shift-click desde el inventario del jugador que lleve una mochila
        if (event.isShiftClick()) {
            // Si el click esta en el inventario del jugador y el item es una mochila
            if (event.getClickedInventory() != null
                    && !event.getClickedInventory().equals(backpackInv)) {
                if (current != null && BackpackItemUtil.getBackpackTier(current).isPresent()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // Bloquear que el item en el ultimo slot del jugador (hotbar slot 8 = numero de slot 44
        // en un inventario full) pueda causar duplicacion por shift-click + cierre abruptp.
        // Prevencion conservadora: si el click es shift desde el inventario del jugador,
        // solo lo bloqueamos si el item es una mochila (ya manejado arriba).
        // El resto de interacciones se permiten normalmente.
    }

    // -------------------------------------------------------------------------
    // InventoryCloseEvent
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof BackpackHolder backpackHolder)) return;

        if (!(event.getPlayer() instanceof Player)) return;

        Inventory inv    = event.getInventory();
        Backpack backpack = backpackHolder.getBackpack();

        // Snapshot del contenido actual del inventario
        ItemStack[] contents = inv.getContents();

        // Actualizar el modelo de datos en memoria
        backpack.setContents(contents);

        // Guardar de forma asincrona — nunca bloquear el hilo principal
        storageManager.getStorage()
                .saveBackpack(backpack)
                .exceptionally(ex -> {
                    log.severe("[BackpackGuiListener] Error al guardar mochila de "
                            + backpack.getPlayerUuid() + ": " + ex.getMessage());
                    return null;
                });
    }

    // -------------------------------------------------------------------------
    // InventoryDragEvent
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof BackpackHolder backpackHolder)) return;

        // Bloquear si se intenta arrastrar una mochila dentro de otra mochila
        ItemStack dragged = event.getOldCursor();
        if (BackpackItemUtil.getBackpackTier(dragged).isPresent()) {
            Inventory backpackInv = backpackHolder.getInventory();
            int backpackSize = backpackInv.getSize();

            // Si alguno de los slots del drag pertenece a la mochila, cancelar
            for (int slot : event.getRawSlots()) {
                if (slot < backpackSize) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }
}
