package net.decatron.rucksack.core;

import net.decatron.rucksack.RucksackPlugin;
import org.bukkit.World;

import java.io.*;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;

public class DatapackInstaller {

    private final RucksackPlugin plugin;

    // Todos los archivos del datapack dentro del jar
    private static final List<String> DATAPACK_FILES = Arrays.asList(
        "datapack/pack.mcmeta",
        "datapack/data/decatron/functions/setup.mcfunction",
        "datapack/data/decatron/recipes/backpack_leather.json",
        "datapack/data/decatron/advancements/get_backpack.json"
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
            // Instalar/actualizar archivos del datapack
            for (String resourcePath : DATAPACK_FILES) {
                String relativePath = resourcePath.substring("datapack/".length());
                File targetFile = new File(targetDir, relativePath);
                targetFile.getParentFile().mkdirs();

                try (InputStream in = plugin.getResource(resourcePath);
                     OutputStream out = new FileOutputStream(targetFile)) {
                    if (in == null) {
                        plugin.getLogger().warning("[DatapackInstaller] Resource not found: " + resourcePath);
                        continue;
                    }
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
}
