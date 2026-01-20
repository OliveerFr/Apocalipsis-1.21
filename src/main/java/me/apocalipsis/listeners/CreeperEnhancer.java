package me.apocalipsis.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import me.apocalipsis.Apocalipsis;

/**
 * Sistema de mejora de creepers para hacerlos extremadamente peligrosos
 * 
 * Características:
 * - Explosiones mucho más potentes (RADIO Y DAÑO)
 * - Mayor radio de explosión
 * - Velocidad aumentada
 * - Resistencia mejorada
 * - Creepers cargados más comunes
 */
public class CreeperEnhancer implements Listener {

    private final Apocalipsis plugin;
    private final double explosionRadiusMultiplier;  // Multiplicador de RADIO (alcance)
    private final double explosionDamageMultiplier;  // Multiplicador de DAÑO
    private final double chargedChance;
    private final boolean giveSpeed;
    private final boolean giveResistance;
    
    // Rastrear creepers que van a explotar para multiplicar su daño
    private final Map<UUID, Float> explodingCreepers = new HashMap<>();

    public CreeperEnhancer(Apocalipsis plugin) {
        this.plugin = plugin;
        
        // Cargar configuración - RADIO pequeño, DAÑO alto
        this.explosionRadiusMultiplier = plugin.getConfig().getDouble("creeper_mejoras.multiplicador_radio", 1.2);
        this.explosionDamageMultiplier = plugin.getConfig().getDouble("creeper_mejoras.multiplicador_dano", 4.5);
        this.chargedChance = plugin.getConfig().getDouble("creeper_mejoras.probabilidad_cargado", 0.35);
        this.giveSpeed = plugin.getConfig().getBoolean("creeper_mejoras.velocidad_extra", true);
        this.giveResistance = plugin.getConfig().getBoolean("creeper_mejoras.resistencia_extra", true);
        
        plugin.getLogger().info("[CreeperEnhancer] Creepers mejorados activados:");
        plugin.getLogger().info("  - Radio explosión: x" + explosionRadiusMultiplier);
        plugin.getLogger().info("  - Daño explosión: x" + explosionDamageMultiplier);
        plugin.getLogger().info("  - Probabilidad cargado: " + (chargedChance * 100) + "%");
        plugin.getLogger().info("  - Velocidad extra: " + giveSpeed);
        plugin.getLogger().info("  - Resistencia extra: " + giveResistance);
    }

    /**
     * Mejora los creepers cuando spawnean
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onCreeperSpawn(CreatureSpawnEvent event) {
        if (event.getEntityType() != EntityType.CREEPER) return;
        
        Creeper creeper = (Creeper) event.getEntity();
        
        // 35% de probabilidad de ser cargado (normalmente solo con rayos)
        if (Math.random() < chargedChance) {
            creeper.setPowered(true);
        }
        
        // Radio por defecto = 3.0, aplicamos multiplicador pequeño (1.2x = 3.6)
        creeper.setExplosionRadius(3); // Mantener radio vanilla para que no sea gigante
        
        // Dar efectos de poción permanentes
        if (giveSpeed) {
            // Speed II permanente para que sean más rápidos
            creeper.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED, 
                Integer.MAX_VALUE, 
                1, // Nivel II
                false, 
                false
            ));
        }
        
        if (giveResistance) {
            // Resistance II para que aguanten más golpes
            creeper.addPotionEffect(new PotionEffect(
                PotionEffectType.RESISTANCE, 
                Integer.MAX_VALUE, 
                1, // Nivel II
                false, 
                false
            ));
        }
        
        // Aumentar vida máxima usando AttributeInstance (método moderno)
        if (creeper.getAttribute(Attribute.MAX_HEALTH) != null) {
            creeper.getAttribute(Attribute.MAX_HEALTH).setBaseValue(30.0);
            creeper.setHealth(30.0);
        }
    }

    /**
     * Amplifica la explosión del creeper antes de que ocurra
     * REGISTRA el creeper para multiplicar el daño
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onCreeperExplode(ExplosionPrimeEvent event) {
        if (!(event.getEntity() instanceof Creeper)) return;
        
        Creeper creeper = (Creeper) event.getEntity();
        float currentRadius = event.getRadius();
        float newRadius = currentRadius * (float) explosionRadiusMultiplier; // Radio PEQUEÑO (1.2x)
        
        // Radio ligeramente aumentado (no gigante)
        event.setRadius(newRadius);
        event.setFire(true);
        
        // REGISTRAR con multiplicador de DAÑO alto (4.5x)
        explodingCreepers.put(creeper.getUniqueId(), (float) explosionDamageMultiplier);
        
        plugin.getLogger().fine("[CreeperEnhancer] Creeper explosivo - Radio: " + 
            currentRadius + " -> " + newRadius + " (x" + explosionRadiusMultiplier + ") | Daño: x" + explosionDamageMultiplier);
    }
    
    /**
     * MULTIPLICAR el daño de la explosión del creeper
     * Este evento se dispara cuando la explosión daña a una entidad
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onExplosionDamage(EntityDamageByEntityEvent event) {
        // Solo procesar daño por explosión
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) return;
        
        // Verificar si el daño viene de un creeper mejorado
        Entity damager = event.getDamager();
        if (!(damager instanceof Creeper)) return;
        
        Creeper creeper = (Creeper) damager;
        Float multiplier = explodingCreepers.remove(creeper.getUniqueId());
        
        if (multiplier != null && multiplier > 1.0) {
            double originalDamage = event.getDamage();
            double newDamage = originalDamage * multiplier;
            event.setDamage(newDamage);
            
            plugin.getLogger().fine("[CreeperEnhancer] Daño multiplicado: " + 
                String.format("%.1f -> %.1f (x%.1f)", originalDamage, newDamage, multiplier));
        }
    }
}
