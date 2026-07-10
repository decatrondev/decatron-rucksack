package net.decatron.rucksack.core;

import net.decatron.rucksack.RucksackPlugin;
import org.bukkit.World;

import java.io.*;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;

public class DatapackInstaller {

    private final RucksackPlugin plugin;

    // Todos los archivos del datapack dentro del jar (MC 1.21+ usa directorios en singular)
    private static final List<String> DATAPACK_FILES = Arrays.asList(
        "datapack/pack.mcmeta",
        "datapack/data/decatron/function/setup.mcfunction",
        "datapack/data/decatron/recipe/backpack_leather.json",
        "datapack/data/decatron/advancement/get_backpack.json"
    );

    public DatapackInstaller(RucksackPlugin plugin) {
        this.plugin = plugin;
    }

    public void installAndSetup() {
        World world = plugin.getServer().getWorlds().get(0);
        File datapacksDir = new File(world.getWorldFolder(), "datapacks");
        File targetDir = new File(datapacksDir, "decatron-rucksack");

        boolean firstInstall = !targetDir.exists();

        try {
            // Crear carpetas base si no existen
            if (!datapacksDir.exists()) datapacksDir.mkdirs();
            if (!targetDir.exists() && !targetDir.mkdirs()) {
                plugin.getLogger().severe("[DatapackInstaller] Could not create directory: " + targetDir.getAbsolutePath());
                return;
            }

            // Limpiar archivos sueltos dejados por versiones anteriores del plugin
            cleanupLooseFiles(datapacksDir);

            // Eliminar carpetas con nombres viejos (plural) si existen
            deleteDir(new File(targetDir, "data/decatron/functions"));
            deleteDir(new File(targetDir, "data/decatron/recipes"));
            deleteDir(new File(targetDir, "data/decatron/advancements"));

            // Instalar/actualizar archivos del datapack
            for (String resourcePath : DATAPACK_FILES) {
                String relativePath = resourcePath.substring("datapack/".length());
                File targetFile = new File(targetDir, relativePath);
                targetFile.getParentFile().mkdirs();

                InputStream in = plugin.getResource(resourcePath);
                if (in == null) {
                    plugin.getLogger().warning("[DatapackInstaller] Resource not found: " + resourcePath);
                    continue;
                }
                try (in; OutputStream out = new FileOutputStream(targetFile)) {
                    in.transferTo(out);
                }
            }

            if (firstInstall) {
                // Primera instalacion — el server debe reiniciarse para cargar el datapack
                plugin.getLogger().warning("************************************************************");
                plugin.getLogger().warning("* Decatron Rucksack: DATAPACK INSTALLED FOR THE FIRST TIME");
                plugin.getLogger().warning("* Please RESTART the server to activate recipes and crafting.");
                plugin.getLogger().warning("************************************************************");
            } else {
                // Ya estaba instalado — solo ejecutar setup
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    plugin.getServer().dispatchCommand(
                        plugin.getServer().getConsoleSender(),
                        "function decatron:setup"
                    );
                    plugin.getLogger().info("[DatapackInstaller] Setup executed automatically.");
                }, 20L);
            }

        } catch (IOException e) {
            plugin.getLogger().severe("[DatapackInstaller] Error installing datapack: " + e.getMessage());
        }
    }

    /** Elimina archivos sueltos en datapacks/ que no son carpetas de datapacks (pack.mcmeta, data/, etc.) */
    private void cleanupLooseFiles(File datapacksDir) {
        File[] entries = datapacksDir.listFiles();
        if (entries == null) return;
        for (File entry : entries) {
            String name = entry.getName();
            // Solo borramos lo que sabemos que dejamos nosotros por error
            if (name.equals("pack.mcmeta") || name.equals("data")) {
                if (entry.isDirectory()) {
                    deleteDir(entry);
                } else {
                    entry.delete();
                }
                plugin.getLogger().info("[DatapackInstaller] Cleaned up loose entry: " + name);
            }
        }
    }

    private void deleteDir(File dir) {
        if (!dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDir(f);
                else f.delete();
            }
        }
        dir.delete();
    }
}
