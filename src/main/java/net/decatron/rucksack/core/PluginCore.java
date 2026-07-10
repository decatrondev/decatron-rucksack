package net.decatron.rucksack.core;

import net.decatron.rucksack.RucksackPlugin;
import net.decatron.rucksack.commands.CommandManager;
import net.decatron.rucksack.config.ConfigManager;
import net.decatron.rucksack.data.StorageManager;
import net.decatron.rucksack.gui.GuiManager;
import net.decatron.rucksack.license.LicenseManager;
import net.decatron.rucksack.listeners.ListenerManager;
import net.decatron.rucksack.render.ResourcePackManager;
import net.decatron.rucksack.render.RenderManager;

public class PluginCore {

    private final RucksackPlugin plugin;

    private final ConfigManager       configManager;
    private final StorageManager      storageManager;
    private final LicenseManager      licenseManager;
    private final ResourcePackManager resourcePackManager;
    private final GuiManager          guiManager;
    private final RenderManager       renderManager;
    private final ListenerManager     listenerManager;
    private final CommandManager      commandManager;

    public PluginCore(RucksackPlugin plugin) {
        this.plugin = plugin;

        // ConfigManager debe crearse primero — todo lo demas lo necesita
        this.configManager       = new ConfigManager(plugin);
        this.storageManager      = new StorageManager(plugin, configManager);
        this.licenseManager      = new LicenseManager(plugin);
        this.resourcePackManager = new ResourcePackManager(plugin, configManager);
        this.guiManager          = new GuiManager(plugin, storageManager);

        // RenderManager recibe configManager, resourcePackManager, guiManager y storageManager
        this.renderManager   = new RenderManager(plugin, configManager, resourcePackManager, guiManager, storageManager);

        this.listenerManager = new ListenerManager(plugin);
        this.commandManager  = new CommandManager(plugin, guiManager, storageManager);
    }

    public void initialize() {
        plugin.getLogger().info("Inicializando PluginCore...");
        configManager.initialize();
        storageManager.initialize();
        licenseManager.initialize();
        resourcePackManager.initialize();
        guiManager.initialize();
        renderManager.initialize();
        listenerManager.initialize();
        commandManager.initialize();
        plugin.getLogger().info("PluginCore inicializado correctamente.");
    }

    public void shutdown() {
        plugin.getLogger().info("Apagando PluginCore...");
        commandManager.shutdown();
        listenerManager.shutdown();
        renderManager.shutdown();
        resourcePackManager.shutdown();
        guiManager.shutdown();
        licenseManager.shutdown();
        storageManager.shutdown();
        configManager.shutdown();
        plugin.getLogger().info("PluginCore apagado correctamente.");
    }

    // Getters para inyeccion en fases siguientes
    public ConfigManager       getConfigManager()       { return configManager; }
    public StorageManager      getStorageManager()      { return storageManager; }
    public LicenseManager      getLicenseManager()      { return licenseManager; }
    public ResourcePackManager getResourcePackManager() { return resourcePackManager; }
    public GuiManager          getGuiManager()          { return guiManager; }
    public RenderManager       getRenderManager()       { return renderManager; }
    public ListenerManager     getListenerManager()     { return listenerManager; }
    public CommandManager      getCommandManager()      { return commandManager; }
}
