package net.decatron.rucksack.data;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface BackpackStorage {

    /**
     * Inicializa el backend de storage (crea tablas, abre conexión, etc.).
     * Se llama una sola vez al arrancar el plugin.
     */
    void initialize() throws Exception;

    /**
     * Cierra conexiones y libera recursos.
     * Se llama una sola vez al apagar el plugin.
     */
    void shutdown();

    /**
     * Carga la mochila de un jugador de forma asíncrona.
     * Retorna Optional.empty() si el jugador no tiene mochila guardada.
     */
    CompletableFuture<Optional<Backpack>> loadBackpack(UUID playerUuid);

    /**
     * Guarda (inserta o actualiza) la mochila de un jugador de forma asíncrona.
     */
    CompletableFuture<Void> saveBackpack(Backpack backpack);

    /**
     * Elimina la mochila de un jugador de forma asíncrona.
     */
    CompletableFuture<Void> deleteBackpack(UUID playerUuid);
}
