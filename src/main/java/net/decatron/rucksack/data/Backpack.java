package net.decatron.rucksack.data;

import org.bukkit.inventory.ItemStack;
import java.util.UUID;

public class Backpack {

    private final UUID playerUuid;
    private String tier;
    private ItemStack[] contents;

    public Backpack(UUID playerUuid, String tier, int slots) {
        this.playerUuid = playerUuid;
        this.tier       = tier;
        this.contents   = new ItemStack[slots];
    }

    // --- Getters ---

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getTier() {
        return tier;
    }

    public ItemStack[] getContents() {
        return contents;
    }

    // --- Setters ---

    public void setTier(String tier) {
        this.tier = tier;
    }

    public void setContents(ItemStack[] contents) {
        this.contents = contents;
    }
}
