package net.decatron.rucksack.data;

import net.decatron.rucksack.RucksackPlugin;
import net.decatron.rucksack.config.ConfigManager;
import net.decatron.rucksack.core.RucksackManager;

public class StorageManager implements RucksackManager {

    private final RucksackPlugin plugin;
    private final ConfigManager  configManager;
    private BackpackStorage      storage;

    public StorageManager(RucksackPlugin plugin, ConfigManager configManager) {
        this.plugin        = plugin;
        this.configManager = configManager;
    }

    @Override
    public void initialize() {
        String type = configManager.getStorageType().toLowerCase().trim();
        plugin.getLogger().info("[StorageManager] Tipo de storage configurado: " + type);

        switch (type) {
            case "mysql" -> storage = new MySQLStorage();
            case "sqlite" -> storage = new SQLiteStorage(plugin);
            default -> {
                plugin.getLogger().warning("[StorageManager] Tipo de storage desconocido '" + type
                        + "'. Usando SQLite por defecto.");
                storage = new SQLiteStorage(plugin);
            }
        }

        try {
            storage.initialize();
            plugin.getLogger().info("[StorageManager] Storage inicializado correctamente.");
        } catch (UnsupportedOperationException e) {
            plugin.getLogger().severe("[StorageManager] " + e.getMessage()
                    + " — el plugin no puede continuar con este backend.");
            throw new RuntimeException(e);
        } catch (Exception e) {
            plugin.getLogger().severe("[StorageManager] Error al inicializar storage: " + e.getMessage());
            throw new RuntimeException("Error fatal al inicializar storage", e);
        }
    }

    @Override
    public void shutdown() {
        if (storage != null) {
            try {
                storage.shutdown();
            } catch (Exception e) {
                plugin.getLogger().warning("[StorageManager] Error al apagar storage: " + e.getMessage());
            }
        }
        plugin.getLogger().info("[StorageManager] Apagado.");
    }

    /**
     * Expone el backend de storage para que otros managers puedan usarlo.
     */
    public BackpackStorage getStorage() {
        return storage;
    }
}
