package me.apocalipsis.security;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import me.apocalipsis.Apocalipsis;

/**
 * Sistema avanzado de seguridad anti-farm y anti-autoclick
 * Detecta patrones sospechosos en acciones de jugadores:
 * - Intervalos de tiempo demasiado regulares (autoclick)
 * - Falta de movimiento (AFK farming)
 * - Alta frecuencia de acciones
 * - Patrones repetitivos
 */
public class AntiFarmSecurityManager {
    
    private final Apocalipsis plugin;
    private final Map<UUID, PlayerSecurityProfile> playerProfiles = new ConcurrentHashMap<>();
    private final Set<UUID> suspendedPlayers = ConcurrentHashMap.newKeySet();
    
    // Configuración
    private FileConfiguration config;
    private File configFile;
    
    // Parámetros de detección
    private boolean enabled;
    private int minActionsForAnalysis;
    private double regularityThreshold;
    private double movementRadiusThreshold;
    private int maxStrikes;
    private long suspensionDurationMinutes;
    private boolean notifyAdmins;
    private boolean verboseLogging;
    
    // Parámetros específicos para minado (más permisivos)
    private double miningRegularityThreshold;
    private double miningMovementRadiusThreshold;
    private int miningMinActionsForAnalysis;
    
    public AntiFarmSecurityManager(Apocalipsis plugin) {
        this.plugin = plugin;
        loadConfig();
        startCleanupTask();
    }
    
    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "anti_farm_security.yml");
        
        if (!configFile.exists()) {
            plugin.saveResource("anti_farm_security.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        
        enabled = config.getBoolean("enabled", true);
        minActionsForAnalysis = config.getInt("detection.min_actions_for_analysis", 10);
        regularityThreshold = config.getDouble("detection.regularity_threshold", 0.15);
        movementRadiusThreshold = config.getDouble("detection.movement_radius_threshold", 3.0);
        maxStrikes = config.getInt("strikes.max_strikes", 3);
        suspensionDurationMinutes = config.getLong("strikes.suspension_duration_minutes", 30);
        notifyAdmins = config.getBoolean("alerts.notify_admins", true);
        verboseLogging = config.getBoolean("alerts.verbose_logging", false);
        
        // Parámetros específicos para minado (más permisivos que los generales)
        miningRegularityThreshold = config.getDouble("detection.mining.regularity_threshold", 0.08);
        miningMovementRadiusThreshold = config.getDouble("detection.mining.movement_radius_threshold", 1.5);
        miningMinActionsForAnalysis = config.getInt("detection.mining.min_actions_for_analysis", 20);
        
        plugin.getLogger().info("[AntiAutoclick] Sistema de seguridad cargado. Enabled: " + enabled);
    }
    
    public void reload() {
        config = YamlConfiguration.loadConfiguration(configFile);
        loadConfig();
    }
    
    /**
     * Registra una acción del jugador y verifica si es sospechosa
     * @return true si la acción es legítima, false si está bloqueada
     */
    public SecurityCheckResult checkAction(Player player, ActionType actionType) {
        if (!enabled) {
            return SecurityCheckResult.allowed();
        }
        
        UUID uuid = player.getUniqueId();
        
        // Verificar suspensión
        if (suspendedPlayers.contains(uuid)) {
            PlayerSecurityProfile profile = playerProfiles.get(uuid);
            if (profile != null && profile.isSuspensionActive()) {
                return SecurityCheckResult.suspended(profile.getSuspensionTimeRemaining());
            } else {
                suspendedPlayers.remove(uuid);
            }
        }
        
        // Obtener o crear perfil
        PlayerSecurityProfile profile = playerProfiles.computeIfAbsent(uuid, 
            k -> new PlayerSecurityProfile(player));
        
        // Registrar acción
        profile.recordAction(actionType, player.getLocation());
        
        // Determinar umbrales según tipo de acción
        int requiredActions = actionType == ActionType.MINING ? miningMinActionsForAnalysis : minActionsForAnalysis;
        
        // Analizar patrones si hay suficientes datos
        if (profile.getActionCount(actionType) >= requiredActions) {
            AnalysisResult analysis = analyzePattern(profile, actionType);
            
            if (analysis.isSuspicious()) {
                return handleSuspiciousActivity(player, profile, analysis);
            }
        }
        
        return SecurityCheckResult.allowed();
    }
    
    /**
     * Analiza el patrón de acciones de un jugador
     */
    private AnalysisResult analyzePattern(PlayerSecurityProfile profile, ActionType actionType) {
        AnalysisResult result = new AnalysisResult();
        
        // Determinar umbrales según tipo de acción
        boolean isMining = actionType == ActionType.MINING;
        double regThreshold = isMining ? miningRegularityThreshold : regularityThreshold;
        double movThreshold = isMining ? miningMovementRadiusThreshold : movementRadiusThreshold;
        int requiredActions = isMining ? miningMinActionsForAnalysis : minActionsForAnalysis;
        
        List<Long> timestamps = profile.getRecentTimestamps(actionType, 30);
        if (timestamps.size() < requiredActions) {
            return result; // No hay suficientes datos
        }
        
        // 1. ANÁLISIS DE REGULARIDAD (Autoclick detection)
        List<Long> intervals = calculateIntervals(timestamps);
        double regularityScore = calculateRegularity(intervals);
        
        if (regularityScore < regThreshold) {
            result.addFlag(SuspiciousFlag.REGULAR_TIMING, 
                "Timing muy regular: " + String.format("%.3f", regularityScore) + 
                " (umbral: " + String.format("%.3f", regThreshold) + ")");
        }
        
        // 2. ANÁLISIS DE MOVIMIENTO (AFK detection)
        double movementRadius = profile.getMovementRadius(actionType);
        
        // Para minado, solo flagear si hay múltiples indicadores, no solo movimiento
        if (movementRadius < movThreshold) {
            if (!isMining || regularityScore < regThreshold * 0.5) {
                result.addFlag(SuspiciousFlag.NO_MOVEMENT, 
                    "Movimiento limitado: " + String.format("%.2f", movementRadius) + " bloques" +
                    " (umbral: " + String.format("%.2f", movThreshold) + ")");
            }
        }
        
        // 3. ANÁLISIS DE VELOCIDAD (Spam detection)
        double avgInterval = intervals.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(1000.0);
        
        // Ajustar umbral para minado (permitir hasta 50ms)
        int minInterval = isMining ? 50 : 100;
        if (avgInterval < minInterval) {
            result.addFlag(SuspiciousFlag.TOO_FAST, 
                "Acciones muy rápidas: " + String.format("%.0f", avgInterval) + "ms promedio");
        }
        
        // 4. ANÁLISIS DE VARIACIÓN (Humanidad check)
        double variance = calculateVariance(intervals);
        
        // Para minado, ser más permisivo con la variación
        double minVariance = isMining ? 20 : 50;
        double maxAvgInterval = isMining ? 1500 : 1000;
        
        if (variance < minVariance && avgInterval < maxAvgInterval) {
            // Solo flagear si es extremadamente robótico
            if (variance < 10 || regularityScore < regThreshold * 0.3) {
                result.addFlag(SuspiciousFlag.LOW_VARIANCE, 
                    "Variación sospechosa: " + String.format("%.0f", variance) + "ms");
            }
        }
        
        return result;
    }
    
    /**
     * Calcula intervalos entre timestamps
     */
    private List<Long> calculateIntervals(List<Long> timestamps) {
        List<Long> intervals = new ArrayList<>();
        for (int i = 1; i < timestamps.size(); i++) {
            intervals.add(timestamps.get(i) - timestamps.get(i - 1));
        }
        return intervals;
    }
    
    /**
     * Calcula la regularidad de los intervalos
     * Valores cercanos a 0 = muy regular (sospechoso)
     * Valores cercanos a 1 = muy irregular (normal)
     */
    private double calculateRegularity(List<Long> intervals) {
        if (intervals.isEmpty()) return 1.0;
        
        double mean = intervals.stream().mapToLong(Long::longValue).average().orElse(0);
        if (mean == 0) return 1.0;
        
        double variance = calculateVariance(intervals);
        double coefficientOfVariation = Math.sqrt(variance) / mean;
        
        // Normalizar a rango [0, 1]
        return Math.min(1.0, coefficientOfVariation);
    }
    
    /**
     * Calcula la varianza de los intervalos
     */
    private double calculateVariance(List<Long> intervals) {
        if (intervals.isEmpty()) return 0;
        
        double mean = intervals.stream().mapToLong(Long::longValue).average().orElse(0);
        
        double sumSquaredDiff = intervals.stream()
            .mapToDouble(interval -> Math.pow(interval - mean, 2))
            .sum();
        
        return sumSquaredDiff / intervals.size();
    }
    
    /**
     * Maneja actividad sospechosa
     */
    private SecurityCheckResult handleSuspiciousActivity(Player player, 
                                                        PlayerSecurityProfile profile,
                                                        AnalysisResult analysis) {
        profile.addStrike();
        
        String playerName = player.getName();
        int strikes = profile.getStrikes();
        
        // Log detallado
        if (verboseLogging) {
            plugin.getLogger().warning("[AntiAutoclick] Actividad sospechosa detectada:");
            plugin.getLogger().warning("  Jugador: " + playerName);
            plugin.getLogger().warning("  Strikes: " + strikes + "/" + maxStrikes);
            for (Map.Entry<SuspiciousFlag, String> flag : analysis.getFlags().entrySet()) {
                plugin.getLogger().warning("  - " + flag.getKey() + ": " + flag.getValue());
            }
        }
        
        // Notificar al jugador
        player.sendMessage("§c§l⚠ ALERTA DE SEGURIDAD");
        player.sendMessage("§e§lSe ha detectado actividad sospechosa en tu cuenta.");
        player.sendMessage("§7Strikes: §c" + strikes + "§7/§c" + maxStrikes);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
        
        // Notificar admins
        if (notifyAdmins) {
            String message = "§c[AntiAutoclick] §e" + playerName + " §7- Actividad sospechosa (" + 
                strikes + "/" + maxStrikes + " strikes)";
            
            for (Player admin : Bukkit.getOnlinePlayers()) {
                if (admin.hasPermission("apocalipsis.admin") || admin.isOp()) {
                    admin.sendMessage(message);
                    for (Map.Entry<SuspiciousFlag, String> flag : analysis.getFlags().entrySet()) {
                        admin.sendMessage("  §7- " + flag.getKey() + ": §f" + flag.getValue());
                    }
                }
            }
        }
        
        // Aplicar penalización según strikes
        if (strikes >= maxStrikes) {
            // Suspensión temporal
            profile.suspend(suspensionDurationMinutes);
            suspendedPlayers.add(player.getUniqueId());
            
            player.sendMessage("§c§l⚠ CUENTA SUSPENDIDA TEMPORALMENTE");
            player.sendMessage("§eDuración: §c" + suspensionDurationMinutes + " minutos");
            player.sendMessage("§7Razón: §fDetección de autoclick/macro");
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);
            
            return SecurityCheckResult.suspended(suspensionDurationMinutes * 60 * 1000);
        } else {
            // Penalización progresiva
            double penalty = 1.0 - (strikes * 0.3); // 30% menos por strike
            
            player.sendMessage("§eGanancias reducidas al §c" + (int)(penalty * 100) + "%");
            player.sendMessage("§7Tip: §fEvita patrones repetitivos y muévete más");
            
            return SecurityCheckResult.penalized(penalty, strikes);
        }
    }
    
    /**
     * Limpia datos antiguos cada 10 minutos
     */
    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                AtomicInteger cleaned = new AtomicInteger(0);
                
                // Limpiar perfiles inactivos (1 hora sin actividad)
                playerProfiles.entrySet().removeIf(entry -> {
                    if (entry.getValue().isInactive(60 * 60 * 1000)) {
                        cleaned.incrementAndGet();
                        return true;
                    }
                    return false;
                });
                
                // Limpiar suspensiones expiradas
                suspendedPlayers.removeIf(uuid -> {
                    PlayerSecurityProfile profile = playerProfiles.get(uuid);
                    return profile == null || !profile.isSuspensionActive();
                });
                
                if (cleaned.get() > 0 && verboseLogging) {
                    plugin.getLogger().info("[AntiAutoclick] Limpieza: " + cleaned.get() + " perfiles inactivos removidos");
                }
            }
        }.runTaskTimer(plugin, 20L * 60 * 10, 20L * 60 * 10); // Cada 10 minutos
    }
    
    /**
     * Limpia strikes de un jugador (comando admin)
     */
    public void clearStrikes(UUID uuid) {
        PlayerSecurityProfile profile = playerProfiles.get(uuid);
        if (profile != null) {
            profile.clearStrikes();
            suspendedPlayers.remove(uuid);
        }
    }
    
    /**
     * Obtiene información de seguridad de un jugador
     */
    public String getPlayerSecurityInfo(UUID uuid) {
        PlayerSecurityProfile profile = playerProfiles.get(uuid);
        if (profile == null) {
            return "Sin datos de seguridad";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("§e=== Perfil de Seguridad ===\n");
        sb.append("§7Strikes: §c").append(profile.getStrikes()).append("§7/§c").append(maxStrikes).append("\n");
        
        if (profile.isSuspensionActive()) {
            long remaining = profile.getSuspensionTimeRemaining();
            sb.append("§cSUSPENDIDO: §e").append(remaining / 60000).append(" min restantes\n");
        }
        
        for (ActionType type : ActionType.values()) {
            int count = profile.getActionCount(type);
            if (count > 0) {
                sb.append("§7").append(type).append(": §f").append(count).append(" acciones\n");
            }
        }
        
        return sb.toString();
    }
    
    // ═══════════════════════════════════════════════════════════════
    // CLASES INTERNAS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Tipos de acciones rastreables
     */
    public enum ActionType {
        XP_GAIN,
        TOKEN_DROP,
        MINING,
        COMBAT,
        FARMING
    }
    
    /**
     * Perfil de seguridad de un jugador
     */
    private static class PlayerSecurityProfile {
        private final UUID playerId;
        private final Map<ActionType, List<ActionRecord>> actionHistory = new EnumMap<>(ActionType.class);
        private int strikes = 0;
        private long suspensionEndTime = 0;
        private long lastActivity = System.currentTimeMillis();
        
        public PlayerSecurityProfile(Player player) {
            this.playerId = player.getUniqueId();
        }
        
        public void recordAction(ActionType type, Location location) {
            lastActivity = System.currentTimeMillis();
            
            List<ActionRecord> records = actionHistory.computeIfAbsent(type, k -> new ArrayList<>());
            records.add(new ActionRecord(System.currentTimeMillis(), location));
            
            // Mantener solo las últimas 50 acciones
            if (records.size() > 50) {
                records.remove(0);
            }
        }
        
        public List<Long> getRecentTimestamps(ActionType type, int count) {
            List<ActionRecord> records = actionHistory.get(type);
            if (records == null) return Collections.emptyList();
            
            return records.stream()
                .skip(Math.max(0, records.size() - count))
                .map(r -> r.timestamp)
                .toList();
        }
        
        public double getMovementRadius(ActionType type) {
            List<ActionRecord> records = actionHistory.get(type);
            if (records == null || records.size() < 3) return Double.MAX_VALUE;
            
            // Calcular centro de masa de las ubicaciones
            double avgX = records.stream().mapToDouble(r -> r.location.getX()).average().orElse(0);
            double avgY = records.stream().mapToDouble(r -> r.location.getY()).average().orElse(0);
            double avgZ = records.stream().mapToDouble(r -> r.location.getZ()).average().orElse(0);
            
            // Calcular radio máximo desde el centro
            double maxDistance = 0;
            for (ActionRecord record : records) {
                double dx = record.location.getX() - avgX;
                double dy = record.location.getY() - avgY;
                double dz = record.location.getZ() - avgZ;
                double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);
                maxDistance = Math.max(maxDistance, distance);
            }
            
            return maxDistance;
        }
        
        public int getActionCount(ActionType type) {
            List<ActionRecord> records = actionHistory.get(type);
            return records == null ? 0 : records.size();
        }
        
        public void addStrike() {
            strikes++;
        }
        
        public void clearStrikes() {
            strikes = 0;
            suspensionEndTime = 0;
        }
        
        public int getStrikes() {
            return strikes;
        }
        
        public void suspend(long minutes) {
            suspensionEndTime = System.currentTimeMillis() + (minutes * 60 * 1000);
        }
        
        public boolean isSuspensionActive() {
            return System.currentTimeMillis() < suspensionEndTime;
        }
        
        public long getSuspensionTimeRemaining() {
            if (!isSuspensionActive()) return 0;
            return suspensionEndTime - System.currentTimeMillis();
        }
        
        public boolean isInactive(long milliseconds) {
            return (System.currentTimeMillis() - lastActivity) > milliseconds;
        }
    }
    
    /**
     * Registro de una acción
     */
    private static class ActionRecord {
        final long timestamp;
        final Location location;
        
        public ActionRecord(long timestamp, Location location) {
            this.timestamp = timestamp;
            this.location = location.clone();
        }
    }
    
    /**
     * Resultado del análisis de patrones
     */
    private static class AnalysisResult {
        private final Map<SuspiciousFlag, String> flags = new EnumMap<>(SuspiciousFlag.class);
        
        public void addFlag(SuspiciousFlag flag, String details) {
            flags.put(flag, details);
        }
        
        public boolean isSuspicious() {
            return !flags.isEmpty();
        }
        
        public Map<SuspiciousFlag, String> getFlags() {
            return flags;
        }
    }
    
    /**
     * Banderas de comportamiento sospechoso
     */
    private enum SuspiciousFlag {
        REGULAR_TIMING,    // Intervalos muy regulares (autoclick)
        NO_MOVEMENT,       // Sin movimiento (AFK)
        TOO_FAST,          // Acciones muy rápidas (spam)
        LOW_VARIANCE       // Poca variación (bot/macro)
    }
    
    /**
     * Resultado de verificación de seguridad
     */
    public static class SecurityCheckResult {
        private final boolean allowed;
        private final double penaltyMultiplier;
        private final int strikes;
        private final long suspensionTime;
        
        private SecurityCheckResult(boolean allowed, double penaltyMultiplier, int strikes, long suspensionTime) {
            this.allowed = allowed;
            this.penaltyMultiplier = penaltyMultiplier;
            this.strikes = strikes;
            this.suspensionTime = suspensionTime;
        }
        
        public static SecurityCheckResult allowed() {
            return new SecurityCheckResult(true, 1.0, 0, 0);
        }
        
        public static SecurityCheckResult penalized(double multiplier, int strikes) {
            return new SecurityCheckResult(true, multiplier, strikes, 0);
        }
        
        public static SecurityCheckResult suspended(long timeMs) {
            return new SecurityCheckResult(false, 0, 0, timeMs);
        }
        
        public boolean isAllowed() {
            return allowed;
        }
        
        public double getPenaltyMultiplier() {
            return penaltyMultiplier;
        }
        
        public int getStrikes() {
            return strikes;
        }
        
        public long getSuspensionTime() {
            return suspensionTime;
        }
    }
}
