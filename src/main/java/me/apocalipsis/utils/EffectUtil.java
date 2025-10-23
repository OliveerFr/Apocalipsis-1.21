package me.apocalipsis.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class EffectUtil {

    private static PotionEffectType cachedNauseaType = null;

    /**
     * Fix para NAUSEA/CONFUSION en Paper 1.20.x
     * Devuelve el PotionEffectType correcto con fallback seguro
     */
    @SuppressWarnings("deprecation")
    public static PotionEffectType nausea() {
        if (cachedNauseaType != null) {
            return cachedNauseaType;
        }

        try {
            // Intenta CONFUSION primero (más compatible)
            PotionEffectType type = PotionEffectType.getByName("CONFUSION");
            if (type != null) {
                cachedNauseaType = type;
                return type;
            }

            // Fallback a NAUSEA
            type = PotionEffectType.getByName("NAUSEA");
            if (type != null) {
                cachedNauseaType = type;
                return type;
            }

            // Último intento con NamespacedKey
            try {
                type = PotionEffectType.getByKey(NamespacedKey.minecraft("nausea"));
                if (type != null) {
                    cachedNauseaType = type;
                    return type;
                }
            } catch (Exception ignored) {}

        } catch (Exception e) {
            // Silenciar error
        }

        return null;
    }

    /**
     * Crea un efecto de náusea/confusión con parámetros
     */
    public static PotionEffect createNauseaEffect(int durationTicks, int amplifier) {
        PotionEffectType type = nausea();
        if (type == null) return null;
        return new PotionEffect(type, durationTicks, amplifier, false, false);
    }
}
