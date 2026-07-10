package net.decatron.rucksack.util;

public enum TierConfig {

    LEATHER  ("leather",   27, "\u00a76Mochila de Cuero",      false, 4001, 3.0, 0.0),
    IRON     ("iron",      36, "\u00a77Mochila de Hierro",     true,  4002, 6.0, 0.0),
    DIAMOND  ("diamond",   45, "\u00a7bMochila de Diamante",   true,  4003, 8.0, 2.0),
    NETHERITE("netherite", 54, "\u00a78Mochila de Netherite",  true,  4004, 8.0, 3.0);

    private final String  id;
    private final int     slots;
    private final String  displayName;
    private final boolean premium;
    private final int     customModelData;
    private final double  armor;
    private final double  armorToughness;

    TierConfig(String id, int slots, String displayName, boolean premium,
               int customModelData, double armor, double armorToughness) {
        this.id              = id;
        this.slots           = slots;
        this.displayName     = displayName;
        this.premium         = premium;
        this.customModelData = customModelData;
        this.armor           = armor;
        this.armorToughness  = armorToughness;
    }

    public String  getId()              { return id; }
    public int     getSlots()           { return slots; }
    public String  getDisplayName()     { return displayName; }
    public boolean isPremium()          { return premium; }
    public int     getCustomModelData() { return customModelData; }
    public double  getArmor()           { return armor; }
    public double  getArmorToughness()  { return armorToughness; }

    /**
     * Busca un TierConfig por su id (case-insensitive).
     * Retorna LEATHER como fallback si no se encuentra.
     */
    public static TierConfig fromId(String id) {
        if (id == null) return LEATHER;
        for (TierConfig tier : values()) {
            if (tier.id.equalsIgnoreCase(id)) return tier;
        }
        return LEATHER;
    }
}
