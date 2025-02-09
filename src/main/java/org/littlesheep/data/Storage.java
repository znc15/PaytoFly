package org.littlesheep.data;

import java.util.Map;
import java.util.UUID;

public interface Storage {
    void init();
    void close();
    void setPlayerFlightTime(UUID uuid, long endTime);
    Long getPlayerFlightTime(UUID uuid);
    Map<UUID, Long> getAllPlayerData();
    void removePlayerFlightTime(UUID uuid);
} 