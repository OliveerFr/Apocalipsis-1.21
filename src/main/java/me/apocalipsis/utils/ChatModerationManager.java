package me.apocalipsis.utils;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor simple de moderación de chat: mute global y por jugador.
 */
public class ChatModerationManager {
    private volatile boolean globalMuted = false;
    private final Set<UUID> mutedPlayers = ConcurrentHashMap.newKeySet();

    public boolean isGlobalMuted() {
        return globalMuted;
    }

    public void setGlobalMuted(boolean muted) {
        this.globalMuted = muted;
    }

    public boolean toggleGlobalMuted() {
        this.globalMuted = !this.globalMuted;
        return this.globalMuted;
    }

    public void mute(UUID uuid) {
        if (uuid != null) mutedPlayers.add(uuid);
    }

    public void unmute(UUID uuid) {
        if (uuid != null) mutedPlayers.remove(uuid);
    }

    public boolean isPlayerMuted(UUID uuid) {
        return uuid != null && mutedPlayers.contains(uuid);
    }

    public Set<UUID> getMutedPlayers() {
        return Collections.unmodifiableSet(mutedPlayers);
    }
}
