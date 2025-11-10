package me.apocalipsis.disaster;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import me.apocalipsis.Apocalipsis;

/**
 * Sistema de rastreo de evasión de desastres.
 * Detecta jugadores que se desconectan durante desastres activos y aplica penalizaciones.
 * 
 * Requisitos para evitar penalización:
 * - Permanecer al menos 60 segundos en el desastre
 * - No desconectarse durante un desastre activo
 * 
 * Penalizaciones por evasión:
 * - Primera vez: -10% PS, advertencia
 * - Segunda vez: -20% PS, misión fallida aleatoria
 * - Tercera+ vez: -30% PS, todas las misiones fallidas, 10min cooldown
 */
public class DisasterEvasionTracker {
    
    private final Apocalipsis plugin;
    private final File dataFile;
    
    // UUID -> tiempo (ms) en que entró al desastre actual
    private final Map<UUID, Long> playerJoinTime = new HashMap<>();
    
    // UUID -> número de evasiones totales
    private final Map<UUID, Integer> evasionCount = new HashMap<>();
    
    // Tiempo mínimo requerido en el desastre (60 segundos)
    private static final long MIN_REQUIRED_TIME_MS = 60000;
    
    // Cooldown entre resets de evasiones (1 día = 86400000 ms)
    private static final long EVASION_RESET_TIME_MS = 86400000;
    
    // UUID -> último timestamp de evasión (para reset progresivo)
    private final Map<UUID, Long> lastEvasionTime = new HashMap<>();
    
    // UUID -> nivel de castigo pendiente al reconectar
    private final Map<UUID, Integer> pendingPunishment = new HashMap<>();
    
    public DisasterEvasionTracker(Apocalipsis plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "evasion_data.yml");
        loadData();
    }
    
    /**
     * Registra que un jugador ha entrado en un desastre activo
     */
    public void onDisasterStart(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        
        playerJoinTime.put(uuid, now);
        
        if (plugin.getConfigManager().isDebugCiclo()) {
            plugin.getLogger().info("[EvasionTracker] Jugador " + player.getName() + " entró al desastre");
        }
    }
    
    /**
     * Registra que un jugador está saliendo del servidor durante un desastre.
     * Evalúa si es evasión y aplica penalizaciones.
     * 
     * @return true si fue evasión, false si salida legítima
     */
    public boolean onPlayerQuitDuringDisaster(Player player) {
        UUID uuid = player.getUniqueId();
        Long joinTime = playerJoinTime.get(uuid);
        
        // Si no tenía registro, no estaba en el desastre (o ya había terminado)
        if (joinTime == null) {
            return false;
        }
        
        long now = System.currentTimeMillis();
        long timeInDisaster = now - joinTime;
        
        // Si estuvo más del tiempo mínimo, no es evasión
        if (timeInDisaster >= MIN_REQUIRED_TIME_MS) {
            playerJoinTime.remove(uuid);
            return false;
        }
        
        // EVASIÓN DETECTADA
        applyEvasionPenalty(player, timeInDisaster);
        playerJoinTime.remove(uuid);
        return true;
    }
    
    /**
     * Limpia el registro cuando el desastre termina naturalmente
     */
    public void onDisasterEnd() {
        playerJoinTime.clear();
        
        if (plugin.getConfigManager().isDebugCiclo()) {
            plugin.getLogger().info("[EvasionTracker] Desastre finalizado - registros limpiados");
        }
    }
    
    /**
     * Aplica penalizaciones progresivas por evasión (PS + castigos físicos)
     */
    private void applyEvasionPenalty(Player player, long timeInDisaster) {
        UUID uuid = player.getUniqueId();
        
        // Resetear contador si pasó más de 1 día desde última evasión
        Long lastEvasion = lastEvasionTime.get(uuid);
        if (lastEvasion != null) {
            long timeSinceLastEvasion = System.currentTimeMillis() - lastEvasion;
            if (timeSinceLastEvasion > EVASION_RESET_TIME_MS) {
                evasionCount.put(uuid, 0);
            }
        }
        
        // Incrementar contador de evasiones
        int evasions = evasionCount.getOrDefault(uuid, 0) + 1;
        evasionCount.put(uuid, evasions);
        lastEvasionTime.put(uuid, System.currentTimeMillis());
        
        // Obtener PS actual
        int currentPs = plugin.getMissionService().getPlayerPs(player);
        int psLoss = 0;
        
        // Programar castigos físicos para cuando el jugador vuelva a conectarse
        scheduleReconnectPunishment(uuid, evasions);
        
        // Aplicar penalización según número de evasiones
        if (evasions == 1) {
            // NIVEL 1: Advertencia suave
            // -10% PS, advertencia, 3 rayos al reconectar
            psLoss = (int) (currentPs * 0.10);
            sendWarningMessage(player, 1, psLoss, timeInDisaster);
            
        } else if (evasions == 2) {
            // NIVEL 2: Castigo moderado
            // -20% PS, misión fallida, 5 rayos + daño al reconectar
            psLoss = (int) (currentPs * 0.20);
            failRandomMission(player);
            sendWarningMessage(player, 2, psLoss, timeInDisaster);
            
        } else if (evasions == 3) {
            // NIVEL 3: Castigo severo
            // -30% PS, todas las misiones fallidas, 10 rayos + lluvia de fuego al reconectar
            psLoss = (int) (currentPs * 0.30);
            failAllMissions(player);
            sendWarningMessage(player, 3, psLoss, timeInDisaster);
            
        } else {
            // NIVEL 4+: CASTIGO EXTREMO
            // -50% PS, todas las misiones fallidas, SUPER METEORITO destruye su base
            psLoss = (int) (currentPs * 0.50);
            failAllMissions(player);
            sendWarningMessage(player, 4, psLoss, timeInDisaster);
        }
        
        // Aplicar pérdida de PS
        if (psLoss > 0) {
            int newPs = Math.max(0, currentPs - psLoss);
            plugin.getMissionService().setPlayerPs(uuid, newPs);
        }
        
        // Log
        plugin.getLogger().warning(String.format(
            "[EvasionTracker] EVASIÓN DETECTADA - Jugador: %s, Tiempo en desastre: %.1fs, Evasiones totales: %d, PS perdidos: %d",
            player.getName(),
            timeInDisaster / 1000.0,
            evasions,
            psLoss
        ));
    }
    
    /**
     * Envía mensaje de advertencia personalizado según nivel de evasión
     */
    private void sendWarningMessage(Player player, int evasionLevel, int psLoss, long timeInDisaster) {
        int secondsInDisaster = (int) (timeInDisaster / 1000);
        int requiredSeconds = (int) (MIN_REQUIRED_TIME_MS / 1000);
        
        player.sendMessage("");
        player.sendMessage("§c§l⚠ ═══════════════════════════════════════ ⚠");
        player.sendMessage("§c§l         EVASIÓN DE DESASTRE DETECTADA");
        player.sendMessage("§c§l⚠ ═══════════════════════════════════════ ⚠");
        player.sendMessage("");
        player.sendMessage("§7Tiempo mínimo requerido: §e" + requiredSeconds + "s");
        player.sendMessage("§7Tiempo que estuviste: §c" + secondsInDisaster + "s");
        player.sendMessage("");
        player.sendMessage("§c§lPENALIZACIONES INMEDIATAS:");
        player.sendMessage("§7  • Pérdida de PS: §c-" + psLoss + " PS");
        
        if (evasionLevel == 2) {
            player.sendMessage("§7  • Una misión marcada como §cFALLIDA");
        } else if (evasionLevel >= 3) {
            player.sendMessage("§7  • §cTODAS§7 tus misiones marcadas como §cFALLIDAS");
        }
        
        // Advertencias de castigos físicos
        player.sendMessage("");
        player.sendMessage("§4§l⚡ CASTIGO AL RECONECTAR:");
        switch (evasionLevel) {
            case 1:
                player.sendMessage("§e  • §73 rayos de advertencia");
                break;
            case 2:
                player.sendMessage("§6  • §75 rayos con daño real");
                player.sendMessage("§6  • §7Efectos de debilidad temporal");
                break;
            case 3:
                player.sendMessage("§c  • §710 rayos devastadores");
                player.sendMessage("§c  • §7Lluvia de bolas de fuego");
                player.sendMessage("§c  • §7Efectos debilitantes prolongados");
                break;
            default:
                player.sendMessage("§4§l  • SUPER METEORITO DESTRUCTOR");
                player.sendMessage("§4§l  • 15 explosiones en tu base");
                player.sendMessage("§4§l  • Efectos devastadores por 1 minuto");
                player.sendMessage("§4§l  • §cTU BASE SERÁ DESTRUIDA");
                break;
        }
        
        player.sendMessage("");
        player.sendMessage("§e§l⚠ ADVERTENCIA:");
        player.sendMessage("§7No evadas los desastres desconectándote.");
        player.sendMessage("§7Las penalizaciones §c§laumentan§7 con cada evasión.");
        player.sendMessage("§7Evasiones totales: §c" + evasionLevel);
        player.sendMessage("");
        player.sendMessage("§c§l⚠ ═══════════════════════════════════════ ⚠");
        player.sendMessage("");
    }
    
    /**
     * Falla una misión aleatoria del jugador
     */
    private void failRandomMission(Player player) {
        plugin.getMissionService().failRandomMission(player.getUniqueId());
    }
    
    /**
     * Falla todas las misiones del jugador
     */
    private void failAllMissions(Player player) {
        plugin.getMissionService().failAllMissions(player.getUniqueId());
    }
    
    /**
     * Obtiene el número de evasiones de un jugador
     */
    public int getEvasionCount(UUID uuid) {
        return evasionCount.getOrDefault(uuid, 0);
    }
    
    /**
     * Resetea las evasiones de un jugador (admin command)
     */
    public void resetEvasions(UUID uuid) {
        evasionCount.remove(uuid);
        lastEvasionTime.remove(uuid);
        playerJoinTime.remove(uuid);
        pendingPunishment.remove(uuid);
    }
    
    /**
     * Limpia todos los datos (llamar al desactivar plugin)
     */
    public void clearAll() {
        playerJoinTime.clear();
        evasionCount.clear();
        lastEvasionTime.clear();
        pendingPunishment.clear();
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // PERSISTENCIA DE DATOS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Carga datos persistidos de evasiones y castigos pendientes
     */
    private void loadData() {
        if (!dataFile.exists()) {
            return;
        }
        
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
            
            // Cargar contador de evasiones
            if (config.contains("evasions")) {
                ConfigurationSection evasionsSection = config.getConfigurationSection("evasions");
                if (evasionsSection != null) {
                    for (String key : evasionsSection.getKeys(false)) {
                        try {
                            UUID uuid = UUID.fromString(key);
                            int count = config.getInt("evasions." + key + ".count", 0);
                            long lastTime = config.getLong("evasions." + key + ".lastTime", 0);
                            
                            evasionCount.put(uuid, count);
                            if (lastTime > 0) {
                                lastEvasionTime.put(uuid, lastTime);
                            }
                        } catch (IllegalArgumentException e) {
                            // UUID inválido, ignorar
                        }
                    }
                }
            }
            
            // Cargar castigos pendientes
            if (config.contains("pending_punishments")) {
                ConfigurationSection punishmentsSection = config.getConfigurationSection("pending_punishments");
                if (punishmentsSection != null) {
                    for (String key : punishmentsSection.getKeys(false)) {
                        try {
                            UUID uuid = UUID.fromString(key);
                            int level = config.getInt("pending_punishments." + key);
                            pendingPunishment.put(uuid, level);
                        } catch (IllegalArgumentException e) {
                            // UUID inválido, ignorar
                        }
                    }
                }
            }
            
            plugin.getLogger().info("[EvasionTracker] Cargados " + evasionCount.size() + " registros de evasión y " + pendingPunishment.size() + " castigos pendientes");
        } catch (Exception e) {
            plugin.getLogger().warning("[EvasionTracker] Error cargando datos: " + e.getMessage());
        }
    }
    
    /**
     * Guarda datos de evasiones y castigos pendientes (llamar al desactivar plugin)
     */
    public void saveData() {
        try {
            FileConfiguration config = new YamlConfiguration();
            
            // Guardar contador de evasiones
            for (Map.Entry<UUID, Integer> entry : evasionCount.entrySet()) {
                String key = entry.getKey().toString();
                config.set("evasions." + key + ".count", entry.getValue());
                
                Long lastTime = lastEvasionTime.get(entry.getKey());
                if (lastTime != null) {
                    config.set("evasions." + key + ".lastTime", lastTime);
                }
            }
            
            // Guardar castigos pendientes
            for (Map.Entry<UUID, Integer> entry : pendingPunishment.entrySet()) {
                config.set("pending_punishments." + entry.getKey().toString(), entry.getValue());
            }
            
            config.save(dataFile);
            plugin.getLogger().info("[EvasionTracker] Guardados " + evasionCount.size() + " registros de evasión y " + pendingPunishment.size() + " castigos pendientes");
        } catch (IOException e) {
            plugin.getLogger().warning("[EvasionTracker] Error guardando datos: " + e.getMessage());
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE CASTIGOS FÍSICOS AL RECONECTAR
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Programa un castigo físico para cuando el jugador se reconecte
     */
    private void scheduleReconnectPunishment(UUID uuid, int evasionLevel) {
        pendingPunishment.put(uuid, evasionLevel);
        saveData(); // Guardar inmediatamente para persistir el castigo
        
        if (plugin.getConfigManager().isDebugCiclo()) {
            plugin.getLogger().info("[EvasionTracker] Castigo nivel " + evasionLevel + " programado y guardado para UUID: " + uuid);
        }
    }
    
    /**
     * Aplica el castigo físico cuando el jugador se reconecta
     * Este método debe ser llamado desde PlayerJoinEvent
     */
    public void applyReconnectPunishment(Player player) {
        UUID uuid = player.getUniqueId();
        Integer punishmentLevel = pendingPunishment.get(uuid);
        
        if (punishmentLevel == null) {
            return; // No tiene castigos pendientes
        }
        
        // Remover el castigo pendiente
        pendingPunishment.remove(uuid);
        
        // Mensaje de advertencia INMEDIATO al conectarse
        player.sendMessage("");
        player.sendMessage("§4§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");
        player.sendMessage("  §c§lTIENES UN CASTIGO PENDIENTE");
        player.sendMessage("  §7Por evadir un desastre al desconectarte");
        player.sendMessage("");
        player.sendMessage("  §7Nivel de evasión: §c" + punishmentLevel);
        player.sendMessage("  §7Los dioses aplicarán tu castigo en §e3 segundos§7...");
        player.sendMessage("");
        player.sendMessage("§4§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");
        
        // Sonido de advertencia
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);
        
        // Aplicar castigo según nivel
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            
            switch (punishmentLevel) {
                case 1:
                    applyLevel1Punishment(player);
                    break;
                case 2:
                    applyLevel2Punishment(player);
                    break;
                case 3:
                    applyLevel3Punishment(player);
                    break;
                default:
                    applyLevel4Punishment(player);
                    break;
            }
        }, 60L); // 3 segundos después de conectarse
    }
    
    /**
     * NIVEL 1: Advertencia suave - 3 rayos
     */
    private void applyLevel1Punishment(Player player) {
        player.sendMessage("");
        player.sendMessage("§e§l⚠ ════════════════════════════════════ ⚠");
        player.sendMessage("§e§l      CASTIGO POR EVASIÓN - NIVEL 1");
        player.sendMessage("§e§l⚠ ════════════════════════════════════ ⚠");
        player.sendMessage("§7Los dioses te recuerdan que §cNO debes evadir desastres§7.");
        player.sendMessage("");
        
        org.bukkit.Location loc = player.getLocation();
        
        // 3 rayos con intervalos
        for (int i = 0; i < 3; i++) {
            final int delay = i * 10; // 0.5s entre rayos
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.getWorld().strikeLightningEffect(loc);
                    player.playSound(loc, org.bukkit.Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
                }
            }, delay);
        }
        
        plugin.getLogger().info("[EvasionTracker] Castigo NIVEL 1 aplicado a " + player.getName());
    }
    
    /**
     * NIVEL 2: Castigo moderado - 5 rayos + daño + efecto
     */
    private void applyLevel2Punishment(Player player) {
        player.sendMessage("");
        player.sendMessage("§6§l⚠ ════════════════════════════════════ ⚠");
        player.sendMessage("§6§l      CASTIGO POR EVASIÓN - NIVEL 2");
        player.sendMessage("§6§l⚠ ════════════════════════════════════ ⚠");
        player.sendMessage("§7Los dioses están §cenojados§7. Segunda advertencia.");
        player.sendMessage("");
        
        org.bukkit.Location loc = player.getLocation();
        
        // 5 rayos con daño progresivo
        for (int i = 0; i < 5; i++) {
            final int delay = i * 8; // Más rápido
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.getWorld().strikeLightning(loc); // Rayo REAL con daño
                    player.damage(4.0); // 2 corazones
                    player.playSound(loc, org.bukkit.Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.8f);
                }
            }, delay);
        }
        
        // Efecto de ralentización
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SLOWNESS, 200, 1)); // 10s
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.WEAKNESS, 200, 0)); // 10s
            }
        }, 40L);
        
        plugin.getLogger().info("[EvasionTracker] Castigo NIVEL 2 aplicado a " + player.getName());
    }
    
    /**
     * NIVEL 3: Castigo severo - 10 rayos + lluvia de fuego
     */
    private void applyLevel3Punishment(Player player) {
        player.sendMessage("");
        player.sendMessage("§c§l⚠ ════════════════════════════════════ ⚠");
        player.sendMessage("§c§l      CASTIGO POR EVASIÓN - NIVEL 3");
        player.sendMessage("§c§l⚠ ════════════════════════════════════ ⚠");
        player.sendMessage("§7Los dioses están §4§lFURIOSOS§7. Última advertencia.");
        player.sendMessage("");
        
        org.bukkit.Location loc = player.getLocation();
        org.bukkit.World world = player.getWorld();
        
        // 10 rayos devastadores
        for (int i = 0; i < 10; i++) {
            final int delay = i * 5; // Muy rápido
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    world.strikeLightning(loc);
                    player.damage(6.0); // 3 corazones
                    player.playSound(loc, org.bukkit.Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.5f);
                }
            }, delay);
        }
        
        // Lluvia de bolas de fuego en un radio de 5 bloques
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 * i) / 8;
                    double x = Math.cos(angle) * 5;
                    double z = Math.sin(angle) * 5;
                    org.bukkit.Location fireLoc = loc.clone().add(x, 10, z);
                    
                    org.bukkit.entity.Fireball fireball = world.spawn(
                        fireLoc, org.bukkit.entity.Fireball.class);
                    fireball.setDirection(new org.bukkit.util.Vector(0, -1, 0));
                    fireball.setYield(2.0f); // Explosión moderada
                }
            }
        }, 60L);
        
        // Efectos debilitantes
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SLOWNESS, 400, 2)); // 20s
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.WEAKNESS, 400, 1)); // 20s
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.MINING_FATIGUE, 400, 1)); // 20s
                player.setFireTicks(100); // 5 segundos en fuego
            }
        }, 70L);
        
        plugin.getLogger().warning("[EvasionTracker] Castigo NIVEL 3 aplicado a " + player.getName());
    }
    
    /**
     * NIVEL 4+: CASTIGO EXTREMO - Super meteorito destructor
     */
    private void applyLevel4Punishment(Player player) {
        player.sendMessage("");
        player.sendMessage("§4§l⚠⚠⚠ ═══════════════════════════════ ⚠⚠⚠");
        player.sendMessage("§4§l    CASTIGO POR EVASIÓN - NIVEL MÁXIMO");
        player.sendMessage("§4§l⚠⚠⚠ ═══════════════════════════════ ⚠⚠⚠");
        player.sendMessage("§c§lLOS DIOSES HAN PERDIDO LA PACIENCIA.");
        player.sendMessage("§7Un §4§lSUPER METEORITO§7 destruirá tu base.");
        player.sendMessage("");
        
        org.bukkit.Location loc = player.getLocation();
        org.bukkit.World world = player.getWorld();
        
        // Advertencia sonora dramática
        for (int i = 0; i < 5; i++) {
            final int delay = i * 20;
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.playSound(loc, org.bukkit.Sound.ENTITY_WITHER_SPAWN, 2.0f, 0.5f);
                    player.sendTitle("§4§l⚠", "§cMETEORITO ENTRANTE", 0, 20, 10);
                }
            }, delay);
        }
        
        // SUPER METEORITO después de 5 segundos
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                org.bukkit.Location currentLoc = player.getLocation();
                
                // Anuncio global
                org.bukkit.Bukkit.broadcastMessage("§4§l⚠⚠⚠ Un SUPER METEORITO impactará en la base de §f" + player.getName() + " §4§l⚠⚠⚠");
                
                // Impacto del meteorito - múltiples explosiones
                for (int i = 0; i < 15; i++) {
                    final int delay = i * 2;
                    final double offsetX = (Math.random() - 0.5) * 10;
                    final double offsetZ = (Math.random() - 0.5) * 10;
                    
                    org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        org.bukkit.Location explosionLoc = currentLoc.clone().add(offsetX, 0, offsetZ);
                        
                        // Explosión grande
                        world.createExplosion(explosionLoc, 4.0f, true, true);
                        
                        // Rayo en cada explosión
                        world.strikeLightning(explosionLoc);
                        
                        // Partículas dramáticas
                        world.spawnParticle(org.bukkit.Particle.EXPLOSION_EMITTER, 
                            explosionLoc, 3, 1, 1, 1, 0.1);
                        world.spawnParticle(org.bukkit.Particle.LAVA, 
                            explosionLoc, 50, 2, 2, 2, 0.1);
                        world.spawnParticle(org.bukkit.Particle.FLAME, 
                            explosionLoc, 100, 3, 3, 3, 0.2);
                    }, delay);
                }
                
                // Daño masivo al jugador
                player.damage(15.0); // 7.5 corazones
                
                // Efectos devastadores permanentes por 1 minuto
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SLOWNESS, 1200, 3));
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.WEAKNESS, 1200, 2));
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.MINING_FATIGUE, 1200, 2));
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.HUNGER, 1200, 2));
                player.setFireTicks(400); // 20 segundos en fuego
            }
        }, 100L);
        
        plugin.getLogger().severe("[EvasionTracker] CASTIGO NIVEL MÁXIMO aplicado a " + player.getName() + " - Super Meteorito lanzado");
    }
    
    /**
     * Verifica si un jugador tiene castigos pendientes
     */
    public boolean hasPendingPunishment(UUID uuid) {
        return pendingPunishment.containsKey(uuid);
    }
    
    /**
     * Obtiene el nivel de castigo pendiente
     */
    public int getPendingPunishmentLevel(UUID uuid) {
        return pendingPunishment.getOrDefault(uuid, 0);
    }
}
