package net.decatron.rucksack.util;

public enum TierConfig {

    LEATHER("leather",   27, "\u00a76Mochila de Cuero",      false),
    IRON   ("iron",      36, "\u00a77Mochila de Hierro",     true),
    DIAMOND("diamond",   45, "\u00a7bMochila de Diamante",   true),
    NETHERITE("netherite", 54, "\u00a78Mochila de Netherite", true);

    private final String  id;
    private final int     slots;
    private final String  displayName;
    private final boolean premium;

    TierConfig(String id, int slots, String displayName, boolean premium) {
        this.id          = id;
        this.slots       = slots;
        this.displayName = displayName;
        this.premium     = premium;
    }

    public String  getId()          { return id; }
    public int     getSlots()       { return slots; }
    public String  getDisplayName() { return displayName; }
    public boolean isPremium()      { return premium; }

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
