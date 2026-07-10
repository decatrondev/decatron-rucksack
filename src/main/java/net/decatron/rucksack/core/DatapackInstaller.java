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

        try {
            // Instalar/actualizar archivos del datapack
            for (String resourcePath : DATAPACK_FILES) {
                String relativePath = resourcePath.substring("datapack/".length());
                File targetFile = new File(targetDir, relativePath);
                targetFile.getParentFile().mkdirs();

                try (InputStream in = plugin.getResource(resourcePath);
                     OutputStream out = new FileOutputStream(targetFile)) {
                    if (in == null) {
                        plugin.getLogger().warning("[DatapackInstaller] Recurso no encontrado: " + resourcePath);
                        continue;
                    }
                    in.transferTo(out);
                }
            }

            plugin.getLogger().info("[DatapackInstaller] Datapack instalado en: " + targetDir.getPath());

            // Ejecutar setup con 1 tick de delay para que el datapack esté cargado
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getServer().dispatchCommand(
                    plugin.getServer().getConsoleSender(),
                    "function decatron:setup"
                );
                plugin.getLogger().info("[DatapackInstaller] Setup ejecutado automaticamente.");
            }, 20L); // 1 segundo de delay

        } catch (IOException e) {
            plugin.getLogger().severe("[DatapackInstaller] Error al instalar datapack: " + e.getMessage());
        }
    }
}
