package me.apocalipsis.events.testing;

import me.apocalipsis.events.testing.EventTestBot.BotPersonality;

/**
 * Perfil de comportamiento para bots de testing.
 * Define cómo se comporta un bot (velocidad, precisión, personalidad).
 */
public class BotBehaviorProfile {
    
    public final BotPersonality personality;
    public final double movementSpeed;         // Bloques por tick
    public final double reactionTimeSeconds;   // Segundos entre acciones
    public final double errorRate;             // 0.0 a 1.0
    
    public BotBehaviorProfile(
        BotPersonality personality,
        double movementSpeed,
        double reactionTimeSeconds,
        double errorRate
    ) {
        this.personality = personality;
        this.movementSpeed = movementSpeed;
        this.reactionTimeSeconds = reactionTimeSeconds;
        this.errorRate = errorRate;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // PERFILES PREDEFINIDOS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Jugador pro: rápido, preciso, agresivo
     */
    public static BotBehaviorProfile PRO_PLAYER() {
        return new BotBehaviorProfile(
            BotPersonality.AGGRESSIVE,
            0.3,    // Rápido
            0.5,    // Reacción rápida
            0.05    // Muy pocos errores
        );
    }
    
    /**
     * Jugador casual: velocidad media, errores ocasionales
     */
    public static BotBehaviorProfile CASUAL_PLAYER() {
        return new BotBehaviorProfile(
            BotPersonality.BALANCED,
            0.2,    // Velocidad media
            1.5,    // Reacción media
            0.15    // Algunos errores
        );
    }
    
    /**
     * Jugador novato: lento, cauteloso, comete errores
     */
    public static BotBehaviorProfile NEWBIE_PLAYER() {
        return new BotBehaviorProfile(
            BotPersonality.CAUTIOUS,
            0.15,   // Lento
            3.0,    // Reacción lenta
            0.3     // Muchos errores
        );
    }
    
    /**
     * Jugador caótico: impredecible, errático
     */
    public static BotBehaviorProfile CHAOTIC_PLAYER() {
        return new BotBehaviorProfile(
            BotPersonality.CHAOTIC,
            0.25,
            2.0,
            0.25    // Bastantes errores
        );
    }
    
    /**
     * Jugador AFK: casi inactivo
     */
    public static BotBehaviorProfile AFK_PLAYER() {
        return new BotBehaviorProfile(
            BotPersonality.AFK,
            0.1,    // Muy lento
            10.0,   // Reacción muy lenta
            0.5     // 50% de errores
        );
    }
    
    /**
     * Mix aleatorio de perfiles
     */
    public static BotBehaviorProfile RANDOM() {
        BotBehaviorProfile[] profiles = {
            PRO_PLAYER(),
            CASUAL_PLAYER(),
            NEWBIE_PLAYER(),
            CHAOTIC_PLAYER()
        };
        return profiles[(int)(Math.random() * profiles.length)];
    }
    
    @Override
    public String toString() {
        return String.format(
            "Profile[personality=%s, speed=%.2f, reaction=%.2fs, error=%.1f%%]",
            personality, movementSpeed, reactionTimeSeconds, errorRate * 100
        );
    }
}
