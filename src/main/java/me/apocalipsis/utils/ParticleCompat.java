package me.apocalipsis.utils;

import org.bukkit.Particle;

/**
 * Compatibilidad de partículas para Paper API 1.17+
 * Evita errores NoSuchFieldError con partículas renombradas/removidas
 */
public class ParticleCompat {

    /**
     * SMOKE_NORMAL (1.17+) - antes era SMOKE
     */
    public static Particle smokeNormal() {
        try {
            return Particle.valueOf("SMOKE_NORMAL");
        } catch (IllegalArgumentException e) {
            return Particle.CLOUD; // Fallback
        }
    }

    /**
     * CAMPFIRE_SIGNAL_SMOKE - humo de campamento
     */
    public static Particle campfireSignal() {
        try {
            return Particle.valueOf("CAMPFIRE_SIGNAL_SMOKE");
        } catch (IllegalArgumentException e) {
            return Particle.CLOUD;
        }
    }

    /**
     * CLOUD - nube básica
     */
    public static Particle cloud() {
        return Particle.CLOUD;
    }

    /**
     * LAVA - partículas de lava
     */
    public static Particle lava() {
        return Particle.LAVA;
    }

    /**
     * FLAME - llama
     */
    public static Particle flame() {
        return Particle.FLAME;
    }

    /**
     * EXPLOSION_NORMAL (1.17+)
     */
    public static Particle explosionNormal() {
        try {
            return Particle.valueOf("EXPLOSION_NORMAL");
        } catch (IllegalArgumentException e) {
            return Particle.EXPLOSION;
        }
    }

    /**
     * BLOCK - partículas de bloque (1.21+ unificado, reemplaza BLOCK_CRACK)
     * REQUIERE BlockData como extra data
     */
    public static Particle blockCrack() {
        return Particle.BLOCK;
    }

    /**
     * BLOCK - partículas de bloque (1.21+ unificado, reemplaza BLOCK_DUST)
     * REQUIERE BlockData como extra data
     */
    public static Particle blockDust() {
        return Particle.BLOCK;
    }
}
