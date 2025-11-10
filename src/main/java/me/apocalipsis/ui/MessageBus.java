package me.apocalipsis.ui;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.apocalipsis.Apocalipsis;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class MessageBus {

    private final Apocalipsis plugin;
    private final Map<String, Long> debounceMap = new HashMap<>();
    private static final long DEBOUNCE_MS = 3000; // 3 segundos

    public MessageBus(Apocalipsis plugin) {
        this.plugin = plugin;
    }

    /**
     * Envía un mensaje broadcast con debounce
     */
    public void broadcast(String message, String debounceKey) {
        if (shouldDebounce(debounceKey)) return;
        // Convert legacy color codes (§ or &) into Adventure Components
        Component comp = LegacyComponentSerializer.legacySection().deserialize(message);
        Bukkit.broadcast(comp);
        if (plugin.getConfigManager().isDebugCiclo()) {
            plugin.getLogger().info("[MessageBus] Broadcast: " + message);
        }
    }

    /**
     * Envía mensaje en chat a un jugador
     */
    public void sendMessage(Player player, String message) {
        Component comp = LegacyComponentSerializer.legacySection().deserialize(message);
        // Use Adventure-aware sendMessage when available
        try {
            player.sendMessage(comp);
        } catch (NoSuchMethodError e) {
            // Fallback to legacy string
            player.sendMessage(LegacyComponentSerializer.legacySection().serialize(comp));
        }
    }

    /**
     * Envía título a todos los jugadores con debounce
     */
    public void sendTitleAll(String title, String subtitle, int fadeIn, int stay, int fadeOut, String debounceKey) {
        if (shouldDebounce(debounceKey)) return;
        // Deserialize legacy formatting into components
        Component titleComp = LegacyComponentSerializer.legacySection().deserialize(title);
        Component subComp = LegacyComponentSerializer.legacySection().deserialize(subtitle);
        Title titleObj = Title.title(
            titleComp,
            subComp,
            Title.Times.times(
                Duration.ofMillis(fadeIn * 50L),
                Duration.ofMillis(stay * 50L),
                Duration.ofMillis(fadeOut * 50L)
            )
        );

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(titleObj);
        }
    }

    /**
     * Envía título a un jugador específico
     */
    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Component titleComp = LegacyComponentSerializer.legacySection().deserialize(title);
        Component subComp = LegacyComponentSerializer.legacySection().deserialize(subtitle);
        Title titleObj = Title.title(
            titleComp,
            subComp,
            Title.Times.times(
                Duration.ofMillis(fadeIn * 50L),
                Duration.ofMillis(stay * 50L),
                Duration.ofMillis(fadeOut * 50L)
            )
        );
        player.showTitle(titleObj);
    }

    /**
     * Envía ActionBar a un jugador
     */
    public void sendActionBar(Player player, String message) {
        Component comp = LegacyComponentSerializer.legacySection().deserialize(message);
        player.sendActionBar(comp);
    }

    /**
     * Envía ActionBar a todos con debounce
     */
    public void sendActionBarAll(String message, String debounceKey) {
        if (shouldDebounce(debounceKey)) return;
        Component comp = LegacyComponentSerializer.legacySection().deserialize(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendActionBar(comp);
        }
    }

    /**
     * Control de debounce: evita spam de mensajes repetidos
     */
    private boolean shouldDebounce(String key) {
        if (key == null || key.isEmpty()) return false;
        
        long now = System.currentTimeMillis();
        Long lastSent = debounceMap.get(key);
        
        if (lastSent != null && (now - lastSent) < DEBOUNCE_MS) {
            return true; // Demasiado pronto, ignorar
        }
        
        debounceMap.put(key, now);
        return false;
    }

    /**
     * Limpia el debounce (útil al cambiar de fase)
     */
    public void clearDebounce() {
        debounceMap.clear();
        if (plugin.getConfigManager().isDebugCiclo()) {
            plugin.getLogger().info("[MessageBus] Debounce map cleared");
        }
    }
}
