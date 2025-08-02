package org.littlesheep.data;

import java.io.Closeable;
import java.util.Map;
import java.util.UUID;

public interface Storage extends Closeable {
    void init();
    void close();
    void setPlayerFlightTime(UUID uuid, long endTime);
    Long getPlayerFlightTime(UUID uuid);
    Map<UUID, Long> getAllPlayerData();
    void removePlayerFlightTime(UUID uuid);
} 