package net.decatron.rucksack.data;

import net.decatron.rucksack.RucksackPlugin;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.sql.*;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class SQLiteStorage implements BackpackStorage {

    private static final int CURRENT_SCHEMA_VERSION = 1;

    private final RucksackPlugin plugin;
    private final Logger log;
    private Connection connection;

    public SQLiteStorage(RucksackPlugin plugin) {
        this.plugin = plugin;
        this.log    = plugin.getLogger();
    }

    // -------------------------------------------------------------------------
    // BackpackStorage — lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void initialize() throws Exception {
        // Asegurar que el directorio de datos existe
        File dataDir = new File(plugin.getDataFolder(), "data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        File dbFile = new File(dataDir, "backpacks.db");
        String url  = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        // Cargar driver relocado
        Class.forName("net.decatron.rucksack.libs.sqlite.JDBC");

        connection = DriverManager.getConnection(url);
        connection.setAutoCommit(true);

        // Habilitar WAL para mejor concurrencia de lectura/escritura
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL;");
        }

        createTables();
        runMigrations();

        log.info("[SQLiteStorage] Base de datos lista: " + dbFile.getAbsolutePath());
    }

    @Override
    public void shutdown() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                log.warning("[SQLiteStorage] Error al cerrar la conexion: " + e.getMessage());
            }
        }
        log.info("[SQLiteStorage] Conexion cerrada.");
    }

    // -------------------------------------------------------------------------
    // BackpackStorage — operaciones asíncronas
    // -------------------------------------------------------------------------

    @Override
    public CompletableFuture<Optional<Backpack>> loadBackpack(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT tier, contents FROM backpacks WHERE player_uuid = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String tier       = rs.getString("tier");
                        byte[] blob       = rs.getBytes("contents");
                        ItemStack[] items = deserializeContents(blob);
                        Backpack bp       = new Backpack(playerUuid, tier, items.length);
                        bp.setContents(items);
                        return Optional.of(bp);
                    }
                }
            } catch (Exception e) {
                log.severe("[SQLiteStorage] Error al cargar mochila de " + playerUuid + ": " + e.getMessage());
            }
            return Optional.empty();
        });
    }

    @Override
    public CompletableFuture<Void> saveBackpack(Backpack backpack) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO backpacks (player_uuid, tier, contents, updated_at) "
                       + "VALUES (?, ?, ?, CURRENT_TIMESTAMP) "
                       + "ON CONFLICT(player_uuid) DO UPDATE SET "
                       + "tier=excluded.tier, contents=excluded.contents, updated_at=excluded.updated_at";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, backpack.getPlayerUuid().toString());
                ps.setString(2, backpack.getTier());
                ps.setBytes(3, serializeContents(backpack.getContents()));
                ps.executeUpdate();
            } catch (Exception e) {
                log.severe("[SQLiteStorage] Error al guardar mochila de "
                        + backpack.getPlayerUuid() + ": " + e.getMessage());
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> deleteBackpack(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM backpacks WHERE player_uuid = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                log.severe("[SQLiteStorage] Error al eliminar mochila de " + playerUuid + ": " + e.getMessage());
            }
            return null;
        });
    }

    // -------------------------------------------------------------------------
    // Creación de tablas
    // -------------------------------------------------------------------------

    private void createTables() throws SQLException {
        // Tabla principal de mochilas
        String backpacksTable =
            "CREATE TABLE IF NOT EXISTS backpacks (" +
            "  player_uuid    VARCHAR(36) PRIMARY KEY NOT NULL," +
            "  tier           VARCHAR(32) NOT NULL DEFAULT 'leather'," +
            "  contents       BLOB," +
            "  schema_version INTEGER DEFAULT 1," +
            "  updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ");";

        // Tabla de migraciones de esquema
        String migrationsTable =
            "CREATE TABLE IF NOT EXISTS schema_migrations (" +
            "  version     INTEGER PRIMARY KEY NOT NULL," +
            "  applied_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ");";

        try (Statement st = connection.createStatement()) {
            st.execute(backpacksTable);
            st.execute(migrationsTable);
        }
    }

    // -------------------------------------------------------------------------
    // Migraciones versionadas
    // -------------------------------------------------------------------------

    private void runMigrations() throws SQLException {
        int currentVersion = getAppliedSchemaVersion();

        if (currentVersion < CURRENT_SCHEMA_VERSION) {
            log.info("[SQLiteStorage] Ejecutando migraciones desde version " + currentVersion
                   + " hasta " + CURRENT_SCHEMA_VERSION + "...");

            // Cada bloque if maneja una migración puntual.
            // Agregar nuevos bloques aquí en fases futuras.
            // if (currentVersion < 2) { ... applyMigration(2); }

            applyMigration(CURRENT_SCHEMA_VERSION);
            log.info("[SQLiteStorage] Migraciones completadas.");
        }
    }

    private int getAppliedSchemaVersion() throws SQLException {
        String sql = "SELECT MAX(version) AS v FROM schema_migrations";
        try (Statement st  = connection.createStatement();
             ResultSet rs  = st.executeQuery(sql)) {
            if (rs.next()) {
                int v = rs.getInt("v");
                return rs.wasNull() ? 0 : v;
            }
        }
        return 0;
    }

    private void applyMigration(int version) throws SQLException {
        String sql = "INSERT OR IGNORE INTO schema_migrations (version) VALUES (?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, version);
            ps.executeUpdate();
        }
    }

    // -------------------------------------------------------------------------
    // Serialización de ItemStack[] con BukkitObjectOutputStream + Base64
    // -------------------------------------------------------------------------

    private byte[] serializeContents(ItemStack[] items) throws Exception {
        try (ByteArrayOutputStream baos  = new ByteArrayOutputStream();
             BukkitObjectOutputStream boos = new BukkitObjectOutputStream(baos)) {

            boos.writeInt(items.length);
            for (ItemStack item : items) {
                boos.writeObject(item);
            }
            boos.flush();
            // Guardar como Base64 para facilitar depuración y portabilidad
            return Base64.getEncoder().encode(baos.toByteArray());
        }
    }

    private ItemStack[] deserializeContents(byte[] data) throws Exception {
        if (data == null || data.length == 0) {
            return new ItemStack[0];
        }
        byte[] decoded = Base64.getDecoder().decode(data);
        try (ByteArrayInputStream bais  = new ByteArrayInputStream(decoded);
             BukkitObjectInputStream bois = new BukkitObjectInputStream(bais)) {

            int length     = bois.readInt();
            ItemStack[] items = new ItemStack[length];
            for (int i = 0; i < length; i++) {
                items[i] = (ItemStack) bois.readObject();
            }
            return items;
        }
    }
}
