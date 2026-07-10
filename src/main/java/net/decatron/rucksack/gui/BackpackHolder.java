package net.decatron.rucksack.gui;

import net.decatron.rucksack.data.Backpack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * InventoryHolder propio para inventarios de mochila.
 * Permite identificar de forma inequivoca si un inventario abierto
 * pertenece al sistema Rucksack, sin depender del titulo ni del contenido.
 */
public class BackpackHolder implements InventoryHolder {

    private final Backpack backpack;
    private final Player   player;
    private Inventory inventory;

    public BackpackHolder(Backpack backpack, Player player) {
        this.backpack = backpack;
        this.player   = player;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public Backpack getBackpack() { return backpack; }
    public Player   getPlayer()   { return player; }
}
