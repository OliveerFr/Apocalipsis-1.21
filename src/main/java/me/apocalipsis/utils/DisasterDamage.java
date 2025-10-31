package me.apocalipsis.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import me.apocalipsis.ui.MessageBus;
import me.apocalipsis.ui.SoundUtil;

public class DisasterDamage {

    private static final Random RANDOM = new Random();
    private static final Map<UUID, Long> lastActionBarTime = new HashMap<>();
    private static final long ACTIONBAR_COOLDOWN_MS = 200; // 200ms debounce

    /**
     * Aplica daño aleatorio al jugador según configuración del desastre.
     * [FIX] Ahora usa damage() en lugar de setHealth() para respetar armadura y encantamientos.
     */
    public static void maybeDamage(Player player, String disasterId, ConfigurationSection disasterConfig, 
                                   MessageBus messageBus, SoundUtil soundUtil) {
        
        if (player.isDead() || player.getHealth() <= 0) return;

        ConfigurationSection commonDamage = disasterConfig.getConfigurationSection("common.random_damage");
        if (commonDamage == null || !commonDamage.getBoolean("enabled", true)) return;

        // Obtener config específica del desastre
        ConfigurationSection specificDamage = disasterConfig.getConfigurationSection("desastres." + disasterId + ".random_damage");
        
        double chance = commonDamage.getDouble("chance", 0.15);
        if (specificDamage != null && specificDamage.contains("chance")) {
            chance = specificDamage.getDouble("chance");
        }

        if (RANDOM.nextDouble() > chance) return;

        double minHearts = commonDamage.getDouble("min_hearts", 0.5);
        double maxHearts = commonDamage.getDouble("max_hearts", 1.5);
        double hearts = minHearts + RANDOM.nextDouble() * (maxHearts - minHearts);
        double damage = hearts * 2.0; // 1 corazón = 2.0 HP

        // [FIX] Usar damage() en lugar de setHealth() para respetar armadura
        // damage() aplica el daño correctamente considerando:
        // - Armadura y su durabilidad
        // - Encantamientos (Protection, Blast Protection, etc.)
        // - Efectos de pociones (Resistance)
        // - Dificultad del mundo
        player.damage(damage);

        // Sonido [1.21+] Usar Registry en lugar de valueOf (deprecated)
        String soundName = commonDamage.getString("sound", "ENTITY_PLAYER_HURT");
        try {
            String keyStr = soundName.toLowerCase(java.util.Locale.ROOT).replace("_", ".");
            if (!keyStr.contains(":")) {
                keyStr = "minecraft:" + keyStr;
            }
            org.bukkit.NamespacedKey key = org.bukkit.NamespacedKey.fromString(keyStr);
            if (key != null) {
                Sound sound = org.bukkit.Registry.SOUNDS.get(key);
                if (sound != null) {
                    soundUtil.playSound(player, sound, 0.7f, 1.0f);
                }
            }
        } catch (Exception ignored) {}

        // Mensaje en ActionBar con debounce (200ms)
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastTime = lastActionBarTime.get(uuid);
        
        if (lastTime == null || (now - lastTime) >= ACTIONBAR_COOLDOWN_MS) {
            String disasterName = disasterId.toUpperCase().replace("_", " ");
            messageBus.sendActionBar(player, String.format("§c-❤ %.1f §7(%s)", hearts, disasterName));
            lastActionBarTime.put(uuid, now);
        }
    }
}
