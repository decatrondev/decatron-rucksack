package net.decatron.rucksack.render;

import net.decatron.rucksack.RucksackPlugin;
import net.decatron.rucksack.config.ConfigManager;
import net.decatron.rucksack.core.RucksackManager;

import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.concurrent.Executors;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ResourcePackManager implements RucksackManager {

    private static final String RESOURCEPACK_PREFIX = "resourcepack/";

    private final RucksackPlugin plugin;
    private final ConfigManager  configManager;

    private HttpServer httpServer;
    private String     url;
    private byte[]     sha1Bytes;

    public ResourcePackManager(RucksackPlugin plugin, ConfigManager configManager) {
        this.plugin        = plugin;
        this.configManager = configManager;
    }

    @Override
    public void initialize() {
        boolean enabled = configManager.getConfig().getBoolean("resourcepack.enabled", true);
        if (!enabled) {
            plugin.getLogger().info("[ResourcePackManager] Resource pack deshabilitado en config. Omitiendo.");
            return;
        }

        try {
            // 1. Extraer archivos del resource pack al disco
            File rpDir = new File(plugin.getDataFolder(), "resourcepack");
            extractResourcePack(rpDir);

            // 2. Empaquetar en zip
            File zipFile = new File(plugin.getDataFolder(), "resourcepack.zip");
            packResourcePack(rpDir, zipFile);

            // 3. Calcular SHA-1
            sha1Bytes = calculateSha1(zipFile.toPath());
            String sha1Hex = bytesToHex(sha1Bytes);

            // 4. Iniciar servidor HTTP embebido
            int port = configManager.getConfig().getInt("resourcepack.port", 28765);
            String publicIp = configManager.getConfig().getString("resourcepack.public-ip", "");
            if (publicIp == null || publicIp.isBlank()) {
                publicIp = InetAddress.getLocalHost().getHostAddress();
                plugin.getLogger().info("[ResourcePackManager] public-ip vacio, usando IP auto-detectada: " + publicIp);
            }

            startHttpServer(port, zipFile.toPath());
            url = "http://" + publicIp + ":" + port + "/rucksack-resourcepack.zip";

            plugin.getLogger().info("[ResourcePackManager] Servidor HTTP iniciado en puerto " + port);
            plugin.getLogger().info("[ResourcePackManager] URL del resource pack: " + url);
            plugin.getLogger().info("[ResourcePackManager] SHA-1: " + sha1Hex);

        } catch (Exception e) {
            plugin.getLogger().severe("[ResourcePackManager] Error al inicializar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void shutdown() {
        if (httpServer != null) {
            httpServer.stop(0);
            plugin.getLogger().info("[ResourcePackManager] Servidor HTTP detenido.");
        }
    }

    // -------------------------------------------------------------------------
    // Getters para RenderManager
    // -------------------------------------------------------------------------

    /** Devuelve la URL publica del resource pack, o null si no esta disponible. */
    public String getUrl() {
        return url;
    }

    /** Devuelve los bytes del SHA-1, o null si no esta disponible. */
    public byte[] getSha1Bytes() {
        return sha1Bytes;
    }

    // -------------------------------------------------------------------------
    // Internos
    // -------------------------------------------------------------------------

    /**
     * Extrae TODO lo que haya bajo resourcepack/ dentro del jar del plugin, sin lista hardcodeada.
     * Asi cualquier modelo/textura nueva que se agregue a src/main/resources/resourcepack/
     * se empaqueta automaticamente sin tocar este archivo.
     */
    private void extractResourcePack(File rpDir) throws IOException, URISyntaxException {
        File jarFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        int extracted = 0;

        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || !name.startsWith(RESOURCEPACK_PREFIX)) {
                    continue;
                }

                File dest = new File(plugin.getDataFolder(), name);
                dest.getParentFile().mkdirs();
                try (InputStream in = jar.getInputStream(entry)) {
                    Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                extracted++;
            }
        }

        if (extracted == 0) {
            plugin.getLogger().warning("[ResourcePackManager] No se encontro ningun archivo bajo " + RESOURCEPACK_PREFIX + " en el jar.");
        }
        plugin.getLogger().info("[ResourcePackManager] " + extracted + " archivos del resource pack extraidos en " + rpDir.getAbsolutePath());
    }

    private void packResourcePack(File rpDir, File zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            zipDirectory(rpDir, rpDir, zos);
        }
        plugin.getLogger().info("[ResourcePackManager] Resource pack empaquetado: " + zipFile.getAbsolutePath());
    }

    private void zipDirectory(File rootDir, File currentDir, ZipOutputStream zos) throws IOException {
        File[] files = currentDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                zipDirectory(rootDir, file, zos);
            } else {
                // Ruta relativa dentro del zip (separador /)
                String relativePath = rootDir.toURI().relativize(file.toURI()).getPath();
                ZipEntry entry = new ZipEntry(relativePath);
                zos.putNextEntry(entry);
                Files.copy(file.toPath(), zos);
                zos.closeEntry();
            }
        }
    }

    private byte[] calculateSha1(Path filePath) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] fileBytes = Files.readAllBytes(filePath);
        return digest.digest(fileBytes);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void startHttpServer(int port, Path zipPath) throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.createContext("/rucksack-resourcepack.zip", exchange -> {
            try {
                byte[] data = Files.readAllBytes(zipPath);
                exchange.sendResponseHeaders(200, data.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(data);
                }
            } catch (IOException e) {
                plugin.getLogger().warning("[ResourcePackManager] Error sirviendo resource pack: " + e.getMessage());
            }
        });
        httpServer.setExecutor(Executors.newSingleThreadExecutor());
        httpServer.start();
    }
}
