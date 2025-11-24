package me.apocalipsis.events.testing;

import java.util.*;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

import me.apocalipsis.Apocalipsis;

/**
 * Bot simulado que imita el comportamiento de un jugador real en eventos.
 * 
 * Capacidades:
 * - Navegación automática a ubicaciones
 * - Interacción con bloques y entidades
 * - Uso de items del inventario
 * - Evasión de peligros
 * - Muerte y respawn simulados
 * - Comportamientos aleatorios para diversidad
 */
public class EventTestBot {
    
    private final Apocalipsis plugin;
    private final String botName;
    private final UUID botUUID;
    private final BotBehaviorProfile profile;
    
    // Estado del bot
    private Location currentLocation;
    private Location targetLocation;
    private BotState state;
    private BotPersonality personality;
    
    // Inventario simulado
    private Map<Material, Integer> simulatedInventory;
    
    // Métricas de comportamiento
    private int actionsPerformed;
    private int itemsCollected;
    private int deaths;
    private long lastActionTime;
    
    // Configuración de comportamiento
    private double movementSpeed;
    private double reactionTime; // segundos
    private double errorRate; // 0.0 a 1.0 (probabilidad de cometer errores)
    
    public EventTestBot(Apocalipsis plugin, String name, BotBehaviorProfile profile) {
        this.plugin = plugin;
        this.botName = name;
        this.botUUID = UUID.randomUUID();
        this.profile = profile;
        this.simulatedInventory = new HashMap<>();
        this.state = BotState.IDLE;
        this.actionsPerformed = 0;
        this.itemsCollected = 0;
        this.deaths = 0;
        this.lastActionTime = System.currentTimeMillis();
        
        // Aplicar perfil de comportamiento
        applyBehaviorProfile(profile);
    }
    
    /**
     * Estados posibles del bot
     */
    public enum BotState {
        IDLE,               // Sin objetivo
        MOVING,             // Moviéndose a ubicación
        INTERACTING,        // Interactuando con algo
        COLLECTING,         // Recolectando items
        EVADING,            // Evadiendo peligro
        DEAD,               // Muerto
        WAITING             // Esperando evento/fase
    }
    
    /**
     * Personalidad del bot (afecta comportamiento)
     */
    public enum BotPersonality {
        AGGRESSIVE,     // Rápido, toma riesgos, muy activo
        CAUTIOUS,       // Lento, evita riesgos, más pasivo
        BALANCED,       // Equilibrado entre agresivo y cauteloso
        CHAOTIC,        // Comportamiento impredecible
        AFK             // Simula jugador AFK (hace muy poco)
    }
    
    /**
     * Aplica perfil de comportamiento al bot
     */
    private void applyBehaviorProfile(BotBehaviorProfile profile) {
        this.personality = profile.personality;
        this.movementSpeed = profile.movementSpeed;
        this.reactionTime = profile.reactionTimeSeconds;
        this.errorRate = profile.errorRate;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // TICK - Actualización del bot
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Actualiza el comportamiento del bot (llamar cada tick)
     */
    public void tick() {
        if (state == BotState.DEAD) {
            handleDeadState();
            return;
        }
        
        // Aplicar tiempo de reacción
        long now = System.currentTimeMillis();
        if (now - lastActionTime < reactionTime * 1000) {
            return;
        }
        
        // Comportamiento según estado
        switch (state) {
            case IDLE:
                handleIdleState();
                break;
            case MOVING:
                handleMovingState();
                break;
            case INTERACTING:
                handleInteractingState();
                break;
            case COLLECTING:
                handleCollectingState();
                break;
            case EVADING:
                handleEvadingState();
                break;
            case WAITING:
                handleWaitingState();
                break;
        }
        
        lastActionTime = now;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // MANEJADORES DE ESTADO
    // ═══════════════════════════════════════════════════════════════════
    
    private void handleIdleState() {
        // Bot idle: explorar o esperar
        if (personality == BotPersonality.AFK) {
            // AFK: casi no hacer nada
            if (Math.random() < 0.01) { // 1% de hacer algo
                wanderRandomly();
            }
        } else {
            // Decidir qué hacer basado en personalidad
            double roll = Math.random();
            
            if (personality == BotPersonality.AGGRESSIVE && roll < 0.7) {
                wanderRandomly();
            } else if (personality == BotPersonality.CAUTIOUS && roll < 0.3) {
                wanderRandomly();
            } else if (roll < 0.5) {
                wanderRandomly();
            }
        }
    }
    
    private void handleMovingState() {
        if (targetLocation == null) {
            state = BotState.IDLE;
            return;
        }
        
        // Simular movimiento hacia target
        double distance = currentLocation.distance(targetLocation);
        
        // Aplicar error de navegación
        if (shouldMakeError()) {
            // Desviarse ligeramente
            targetLocation.add(
                (Math.random() - 0.5) * 2,
                0,
                (Math.random() - 0.5) * 2
            );
        }
        
        if (distance < 1.0) {
            // Llegó al destino
            currentLocation = targetLocation.clone();
            state = BotState.IDLE;
            actionsPerformed++;
        } else {
            // Continuar moviéndose
            Vector direction = targetLocation.toVector().subtract(currentLocation.toVector()).normalize();
            currentLocation.add(direction.multiply(movementSpeed));
        }
    }
    
    private void handleInteractingState() {
        // Interacción completada
        actionsPerformed++;
        state = BotState.IDLE;
    }
    
    private void handleCollectingState() {
        // Recolección completada
        itemsCollected++;
        state = BotState.IDLE;
    }
    
    private void handleEvadingState() {
        // Evadir: moverse aleatoriamente
        if (targetLocation == null || currentLocation.distance(targetLocation) < 5.0) {
            // Encontró lugar seguro
            state = BotState.IDLE;
        } else {
            // Continuar evadiendo
            handleMovingState();
        }
    }
    
    private void handleWaitingState() {
        // Esperando: no hacer nada
        if (Math.random() < 0.05) { // 5% de volver a idle
            state = BotState.IDLE;
        }
    }
    
    private void handleDeadState() {
        // Simular respawn después de tiempo
        if (Math.random() < 0.02) { // ~2% por tick = respawn en ~50 ticks
            respawn();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // ACCIONES DEL BOT
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Mueve el bot a una ubicación específica
     */
    public void moveTo(Location target) {
        this.targetLocation = target.clone();
        this.state = BotState.MOVING;
    }
    
    /**
     * Hace que el bot deambule aleatoriamente
     */
    public void wanderRandomly() {
        if (currentLocation == null) return;
        
        double angle = Math.random() * Math.PI * 2;
        double distance = 5 + Math.random() * 15; // 5-20 bloques
        
        Location newTarget = currentLocation.clone().add(
            Math.cos(angle) * distance,
            0,
            Math.sin(angle) * distance
        );
        
        moveTo(newTarget);
    }
    
    /**
     * Interactuar con un bloque en ubicación
     */
    public void interactWithBlock(Location location) {
        // Primero moverse allí
        moveTo(location);
        // Después de llegar, cambiar a interacción
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (state == BotState.IDLE && currentLocation.distance(location) < 2.0) {
                state = BotState.INTERACTING;
            }
        }, 40L); // 2 segundos
    }
    
    /**
     * Recolectar item en ubicación
     */
    public void collectItem(Location location, Material material, int amount) {
        moveTo(location);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (state == BotState.IDLE && currentLocation.distance(location) < 3.0) {
                addToInventory(material, amount);
                state = BotState.COLLECTING;
            }
        }, 20L);
    }
    
    /**
     * Evadir peligro (huir de ubicación)
     */
    public void evadeFrom(Location dangerLocation) {
        if (currentLocation == null) return;
        
        // Calcular dirección opuesta
        Vector away = currentLocation.toVector().subtract(dangerLocation.toVector()).normalize();
        Location safeSpot = currentLocation.clone().add(away.multiply(20));
        
        targetLocation = safeSpot;
        state = BotState.EVADING;
    }
    
    /**
     * Usar item del inventario
     */
    public boolean useItem(Material material) {
        if (!hasItem(material)) {
            return false;
        }
        
        // Aplicar error
        if (shouldMakeError()) {
            // Fallo al usar item
            return false;
        }
        
        removeFromInventory(material, 1);
        actionsPerformed++;
        return true;
    }
    
    /**
     * Atacar entidad (simulado)
     */
    public void attackEntity(Location entityLocation) {
        if (currentLocation.distance(entityLocation) > 3.0) {
            // Primero acercarse
            moveTo(entityLocation);
        } else {
            // Ya está cerca, atacar
            state = BotState.INTERACTING;
            actionsPerformed++;
        }
    }
    
    /**
     * Mata al bot (simulado)
     */
    public void die() {
        state = BotState.DEAD;
        deaths++;
        simulatedInventory.clear();
    }
    
    /**
     * Respawnea al bot
     */
    public void respawn() {
        state = BotState.IDLE;
        // Volver a spawn point
        if (currentLocation != null && currentLocation.getWorld() != null) {
            currentLocation = currentLocation.getWorld().getSpawnLocation();
        }
    }
    
    /**
     * Hacer que el bot espere
     */
    public void wait(int ticks) {
        state = BotState.WAITING;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (state == BotState.WAITING) {
                state = BotState.IDLE;
            }
        }, ticks);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // INVENTARIO SIMULADO
    // ═══════════════════════════════════════════════════════════════════
    
    public void addToInventory(Material material, int amount) {
        simulatedInventory.put(material, simulatedInventory.getOrDefault(material, 0) + amount);
    }
    
    public void removeFromInventory(Material material, int amount) {
        int current = simulatedInventory.getOrDefault(material, 0);
        int newAmount = Math.max(0, current - amount);
        if (newAmount == 0) {
            simulatedInventory.remove(material);
        } else {
            simulatedInventory.put(material, newAmount);
        }
    }
    
    public boolean hasItem(Material material) {
        return simulatedInventory.containsKey(material) && simulatedInventory.get(material) > 0;
    }
    
    public int getItemCount(Material material) {
        return simulatedInventory.getOrDefault(material, 0);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // UTILIDADES
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Determina si el bot comete un error según su errorRate
     */
    private boolean shouldMakeError() {
        return Math.random() < errorRate;
    }
    
    /**
     * Calcula distancia a una ubicación
     */
    public double distanceTo(Location location) {
        if (currentLocation == null) return Double.MAX_VALUE;
        return currentLocation.distance(location);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // GETTERS / SETTERS
    // ═══════════════════════════════════════════════════════════════════
    
    public String getName() {
        return botName;
    }
    
    public UUID getUUID() {
        return botUUID;
    }
    
    public Location getLocation() {
        return currentLocation;
    }
    
    public void setLocation(Location location) {
        this.currentLocation = location.clone();
    }
    
    public BotState getState() {
        return state;
    }
    
    public void setState(BotState state) {
        this.state = state;
    }
    
    public BotPersonality getPersonality() {
        return personality;
    }
    
    public int getActionsPerformed() {
        return actionsPerformed;
    }
    
    public int getItemsCollected() {
        return itemsCollected;
    }
    
    public int getDeaths() {
        return deaths;
    }
    
    public boolean isAlive() {
        return state != BotState.DEAD;
    }
    
    public boolean isIdle() {
        return state == BotState.IDLE;
    }
    
    public Map<Material, Integer> getInventory() {
        return new HashMap<>(simulatedInventory);
    }
    
    /**
     * Genera un reporte de estadísticas del bot
     */
    public String getStatsReport() {
        return String.format(
            "§e%s §7[%s]\n" +
            "  §7Estado: §f%s\n" +
            "  §7Acciones: §f%d\n" +
            "  §7Items: §f%d\n" +
            "  §7Muertes: §f%d\n" +
            "  §7Inventario: §f%d tipos",
            botName,
            personality.name(),
            state.name(),
            actionsPerformed,
            itemsCollected,
            deaths,
            simulatedInventory.size()
        );
    }
}
