package net.decatron.rucksack.data;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Stub de implementación MySQL — se completa en una fase posterior.
 */
public class MySQLStorage implements BackpackStorage {

    @Override
    public void initialize() throws Exception {
        throw new UnsupportedOperationException("MySQL support coming soon");
    }

    @Override
    public void shutdown() {
        throw new UnsupportedOperationException("MySQL support coming soon");
    }

    @Override
    public CompletableFuture<Optional<Backpack>> loadBackpack(UUID playerUuid) {
        throw new UnsupportedOperationException("MySQL support coming soon");
    }

    @Override
    public CompletableFuture<Void> saveBackpack(Backpack backpack) {
        throw new UnsupportedOperationException("MySQL support coming soon");
    }

    @Override
    public CompletableFuture<Void> deleteBackpack(UUID playerUuid) {
        throw new UnsupportedOperationException("MySQL support coming soon");
    }
}
