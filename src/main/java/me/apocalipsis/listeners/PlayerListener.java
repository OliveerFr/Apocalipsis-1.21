package me.apocalipsis.listeners;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.disaster.Disaster;
import me.apocalipsis.disaster.DisasterBase;
import me.apocalipsis.disaster.DisasterController;
import me.apocalipsis.missions.MissionRank;
import me.apocalipsis.state.ServerState;
import me.apocalipsis.ui.ScoreboardManager;
import me.apocalipsis.ui.TablistManager;

public class PlayerListener implements Listener {

    private final Apocalipsis plugin;
    private final ScoreboardManager scoreboardManager;
    private final TablistManager tablistManager;
    
    // Sistema de castigos mejorado
    private FileConfiguration castigosConfig;
    private final Set<UUID> respawnImmunity = new HashSet<>();
    private final java.util.Random random = new java.util.Random();
    
    // [PERFORMANCE] Cache de configuración frecuentemente accedida
    private boolean castigosEnabled = true;
    private int psMinimo = 0;
    private boolean anuncioPublicoEnabled = true;
    
    // Sistema de puntos por tiempo jugado
    private final Map<UUID, Long> playerJoinTime = new HashMap<>();
    private static final long PS_TIME_INTERVAL = 30 * 60 * 1000; // 30 minutos en ms
    private static final int PS_PER_INTERVAL = 1; // 1 PS cada 30 minutos

    public PlayerListener(Apocalipsis plugin, ScoreboardManager scoreboardManager,
                         TablistManager tablistManager) {
        this.plugin = plugin;
        this.scoreboardManager = scoreboardManager;
        this.tablistManager = tablistManager;
        loadCastigosConfig();
        startPlaytimeRewardTask();
    }
    
    /**
     * Carga configuración de castigos desde castigos.yml
     */
    private void loadCastigosConfig() {
        File castigosFile = new File(plugin.getDataFolder(), "castigos.yml");
        if (!castigosFile.exists()) {
            plugin.saveResource("castigos.yml", false);
        }
        castigosConfig = YamlConfiguration.loadConfiguration(castigosFile);
        
        // [PERFORMANCE] Cachear valores frecuentemente accedidos
        castigosEnabled = castigosConfig.getBoolean("general.enabled", true);
        psMinimo = castigosConfig.getInt("general.ps_minimo", 0);
        anuncioPublicoEnabled = castigosConfig.getBoolean("general.anuncio_publico.enabled", true);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // [PERFORMANCE] Early return si player inválido
        if (player == null || !player.isOnline()) {
            return;
        }
        
        // [BLOCK TRACKER] Actualizar última conexión para protección de bloques
        plugin.getBlockTracker().updatePlayerLastSeen(player);
        
        // [TIEMPO JUGADO] Registrar hora de conexión para PS por tiempo
        trackPlayerJoin(player);
        
        // [FIX DEFINITIVO] Forzar board compartido (crítico para que todos vean lo mismo)
        player.setScoreboard(org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard());
        
        // [RANGOS PERMANENTES] Actualizar tab con rango permanente si tiene uno
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (plugin.getPermRankManager() != null) {
                plugin.getPermRankManager().updatePlayerTab(player);
            }
        }, 20L); // 1 segundo después para dar tiempo a que cargue todo
        
        // [v2.0 CASTIGOS PENDIENTES] Aplicar castigos por misiones fallidas de días anteriores
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (plugin.getMissionService() != null) {
                plugin.getMissionService().applyPendingPenalty(player);
            }
        }, 30L); // 1.5 segundos después de conectarse (para que vea el título de bienvenida primero)
        
        // [EVASION PUNISHMENT] Aplicar castigos físicos pendientes
        plugin.getDisasterEvasionTracker().applyReconnectPunishment(player);
        
        // [EVASION TRACKING] Si hay un desastre activo Y el tracking está activado, registrar entrada del jugador
        // IMPORTANTE: Verificar disasterActive para evitar tracking prematuro durante preparación
        ServerState currentState = plugin.getStateManager().getCurrentState();
        if (currentState == ServerState.ACTIVO && 
            plugin.getDisasterEvasionTracker().isDisasterActive() && 
            !player.hasPermission("apocalipsis.exempt")) {
            
            plugin.getDisasterEvasionTracker().onDisasterStart(player);
            
            if (plugin.getConfigManager().isDebugCiclo()) {
                plugin.getLogger().info("[EvasionTracker] Jugador " + player.getName() + 
                    " se unió durante desastre activo - tracking iniciado");
            }
        }
        
        // [HABILIDADES DE RANGO] Aplicar habilidades pasivas del rango actual
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (plugin.getAbilityService() != null) {
                plugin.getAbilityService().applyAbilities(player, true);
            }
        }, 20L); // 1 segundo después de conectarse
        
        // [RECOMPENSAS] Las recompensas SOLO se entregan al subir de rango, 
        // NO al reconectar (comentado para evitar duplicados)
        // org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
        //     if (plugin.getRewardService() != null) {
        //         plugin.getRewardService().checkAndDeliverPendingRewards(player);
        //     }
        // }, 40L); // 2 segundos después de conectarse
        
        // [AUTOASIGNACIÓN] Late-join: asignar misiones si el jugador no tiene misiones activas
        // Esto permite que jugadores que entren durante un día activo reciban misiones
        // Si ya tienen misiones (por reconexión), assignMissionsToPlayer() las respeta y no reasigna
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getMissionService().assignMissionsToPlayer(player);
        }, 2L);

        // Actualizar UI
        scoreboardManager.updatePlayer(player);
        
        // [RECONSTRUCCIÓN] Agregar jugador al BossBar único del DisasterController
        plugin.getDisasterController().addPlayerToBossBar(player);
        
        // [FIX DEFINITIVO] Aplicar TAB prefix al jugador que entra
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            tablistManager.applyTabPrefix(player);
            
            // Reaplicar a los demás también (para que te vean con prefijo al instante)
            for (Player other : org.bukkit.Bukkit.getOnlinePlayers()) {
                if (other.equals(player)) continue;
                tablistManager.applyTabPrefix(other);
            }
            
            // Actualizar header/footer para todos
            tablistManager.updateAll();
        }, 10L);
        
        // ═══════════════════════════════════════════════════════════════════
        // [v2.0] SISTEMA DE BIENVENIDA PARA NUEVOS JUGADORES
        // ═══════════════════════════════════════════════════════════════════
        if (!player.hasPlayedBefore()) {
            // Nuevo jugador - mostrar tutorial de bienvenida
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                sendWelcomeTutorial(player);
            }, 60L); // 3 segundos después de entrar
        }
        
        // ═══════════════════════════════════════════════════════════════════
        // [v2.0] NOTIFICACIÓN DE RECOMPENSAS PENDIENTES
        // ═══════════════════════════════════════════════════════════════════
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (plugin.getRewardClaimSystem() != null) {
                int pendingRewards = plugin.getRewardClaimSystem().getTotalPendingItems(player.getUniqueId());
                if (pendingRewards > 0) {
                    player.sendMessage("");
                    player.sendMessage("§6§l🎁 §e¡Tienes §a" + pendingRewards + " §erecompensa(s) pendiente(s)!");
                    player.sendMessage("§7   Usa §a/recompensa §7o §a/avo menu §7para reclamarlas.");
                    player.sendMessage("");
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.8f);
                }
            }
        }, 80L); // 4 segundos después de entrar
    }
    
    /**
     * Envía tutorial de bienvenida a nuevos jugadores
     */
    private void sendWelcomeTutorial(Player player) {
        player.sendTitle(
            "§5§l¡BIENVENIDO AL APOCALIPSIS!",
            "§7Un mundo donde la supervivencia es todo...",
            20, 100, 20
        );
        
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        
        // Mensaje de tutorial después del título
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.sendMessage("");
            player.sendMessage("§5§l═══════════════════════════════════════════");
            player.sendMessage("§d§l    ¡BIENVENIDO AL SERVIDOR APOCALIPSIS!");
            player.sendMessage("§5§l═══════════════════════════════════════════");
            player.sendMessage("");
            player.sendMessage("§e§lGuía Rápida de Supervivencia:");
            player.sendMessage("");
            player.sendMessage("§a▸ §f/avo menu §7- Menú principal con todo");
            player.sendMessage("§a▸ §f/recompensa §7- Reclamar recompensas");
            player.sendMessage("§a▸ §f/avo status §7- Ver tu estado");
            player.sendMessage("§a▸ §f/avo protecciones §7- Cómo sobrevivir");
            player.sendMessage("");
            player.sendMessage("§c§l¡IMPORTANTE!");
            player.sendMessage("§7• Completa tus §emisiones diarias §7para ganar XP y PS");
            player.sendMessage("§7• Sobrevive a los §cdesastres §7(terremotos, huracanes, fuego)");
            player.sendMessage("§7• Sube de §drango §7para desbloquear recompensas");
            player.sendMessage("");
            player.sendMessage("§6Consejo: §eUsa §a/avo menu §epara empezar.");
            player.sendMessage("");
            player.sendMessage("§5§l═══════════════════════════════════════════");
            player.sendMessage("");
        }, 80L); // 4 segundos después del título
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // [TIEMPO JUGADO] Limpiar tracking al desconectarse
        trackPlayerQuit(player);
        
        // [XP] Resetear combos al desconectarse
        if (plugin.getExperienceListener() != null && plugin.getExperienceListener().getXPManager() != null) {
            plugin.getExperienceListener().getXPManager().resetPlayerCombos(player);
            // [NUEVO v1.22.68] Limpiar trackers de XP para prevenir memory leaks
            plugin.getExperienceListener().getXPManager().cleanupPlayer(playerId);
        }
        
        // [HABILIDADES v1.22.68] Limpiar cooldowns de habilidades para prevenir memory leaks
        if (plugin.getAbilityService() != null) {
            plugin.getAbilityService().cleanupPlayer(playerId);
        }
        
        // [RECONSTRUCCIÓN] Remover jugador del BossBar único del DisasterController
        plugin.getDisasterController().removePlayerFromBossBar(player);
        
        // ============ MEJORADO: Detectar tipo de desconexión ============
        
        // 1. Detectar si fue expulsado por timeout del servidor (conexión perdida)
        boolean timeout = player.getPing() > 5000;
        
        // 2. Detectar si el servidor está bajo
        boolean serverLow = plugin.getPerformanceAdapter() != null && 
                           plugin.getPerformanceAdapter().getLastTPS() < 10.0;
        
        // 3. Marcar como desconexión involuntaria si hay problemas
        if (timeout) {
            plugin.getDisasterEvasionTracker().flagServerDisconnect(player, "timeout_conexion");
            if (plugin.getConfigManager().isDebugCiclo()) {
                plugin.getLogger().info("[PlayerListener] " + player.getName() + 
                    " desconectado por timeout (ping alto)");
            }
        } else if (serverLow) {
            plugin.getDisasterEvasionTracker().flagServerDisconnect(player, "servidor_bajo_tps");
            if (plugin.getConfigManager().isDebugCiclo()) {
                plugin.getLogger().info("[PlayerListener] " + player.getName() + 
                    " desconectado con TPS bajo - no contar como evasión");
            }
        }
        
        // ============ Fin de mejoras de detección ============
        
        // [EVASIÓN] Detectar si el jugador se desconecta durante un desastre activo
        ServerState currentState = plugin.getStateManager().getCurrentState();
        if (currentState == ServerState.ACTIVO) {
            plugin.getDisasterEvasionTracker().onPlayerQuitDuringDisaster(player);
        }
        
        scoreboardManager.clearPlayer(player);
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // SISTEMA DE CASTIGOS MEJORADO
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Maneja muerte del jugador con sistema de penalizaciones mejorado
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();
        
        // [NUEVO] Registrar muerte en DeathTracker
        if (plugin.getDeathTracker() != null) {
            plugin.getDeathTracker().addDeath(uuid);
            
            // Actualizar TAB inmediatamente para mostrar nueva muerte
            if (plugin.getTablistManager() != null) {
                plugin.getTablistManager().applyTabPrefix(player);
            }
        }
        
        // Verificar si el sistema de castigos está habilitado
        if (!castigosConfig.getBoolean("enabled", true)) return;
        
        // Verificar si murió durante un desastre activo
        ServerState currentState = plugin.getStateManager().getCurrentState();
        if (currentState != ServerState.ACTIVO) return;
        
        // [v1.19.0] Notificar al desastre activo sobre la muerte
        DisasterController controller = plugin.getDisasterController();
        if (controller != null && controller.getCurrentDisaster() != null) {
            Disaster activeDisaster = controller.getCurrentDisaster();
            if (activeDisaster instanceof DisasterBase) {
                ((DisasterBase) activeDisaster).handlePlayerDeathInDisaster(player);
            }
        }
        
        ConfigurationSection muerteConfig = castigosConfig.getConfigurationSection("muerte_en_desastre");
        if (muerteConfig == null || !muerteConfig.getBoolean("enabled", true)) return;
        
        // Aplicar inmunidad temporal al respawn
        int inmunidadSec = castigosConfig.getInt("general.inmunidad_respawn_segundos", 10);
        respawnImmunity.add(uuid);
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> respawnImmunity.remove(uuid), inmunidadSec * 20L);
        
        // Aplicar penalizaciones después del respawn (1 segundo delay)
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) return;
            
            applyDeathPenalties(p, muerteConfig);
        }, 20L);
    }
    
    /**
     * Aplica penalizaciones por muerte durante desastre
     */
    private void applyDeathPenalties(Player player, ConfigurationSection config) {
        // [PERFORMANCE] Early return si sistema deshabilitado
        if (!castigosEnabled) {
            return;
        }
        
        Location loc = player.getLocation();
        StringBuilder efectosAplicados = new StringBuilder();
        
        // 1. PENALIZACIÓN BASE (siempre se aplica)
        ConfigurationSection baseSection = config.getConfigurationSection("penalizacion_base");
        if (baseSection != null) {
            for (String key : baseSection.getKeys(false)) {
                ConfigurationSection effectSection = baseSection.getConfigurationSection(key);
                if (effectSection != null) {
                    applyEffectFromSection(player, effectSection);
                    efectosAplicados.append(extractEffectNameFromType(effectSection.getString("tipo", "UNKNOWN"))).append(", ");
                }
            }
        }
        
        // 2. PENALIZACIÓN ADICIONAL (probabilidad)
        double probAdicional = config.getDouble("penalizacion_adicional.probabilidad", 0.40);
        if (random.nextDouble() < probAdicional) {
            ConfigurationSection adicionalSection = config.getConfigurationSection("penalizacion_adicional.efectos");
            if (adicionalSection != null) {
                for (String key : adicionalSection.getKeys(false)) {
                    ConfigurationSection effectSection = adicionalSection.getConfigurationSection(key);
                    if (effectSection != null) {
                        applyEffectFromSection(player, effectSection);
                        efectosAplicados.append(extractEffectNameFromType(effectSection.getString("tipo", "UNKNOWN"))).append(", ");
                    }
                }
            }
        }
        
        // 3. PENALIZACIÓN SEVERA (baja probabilidad)
        double probSevera = config.getDouble("penalizacion_severa.probabilidad", 0.08);
        boolean severaAplicada = false;
        if (random.nextDouble() < probSevera) {
            severaAplicada = true;
            ConfigurationSection severaSection = config.getConfigurationSection("penalizacion_severa.efectos");
            if (severaSection != null) {
                for (String key : severaSection.getKeys(false)) {
                    ConfigurationSection effectSection = severaSection.getConfigurationSection(key);
                    if (effectSection != null) {
                        applyEffectFromSection(player, effectSection);
                        efectosAplicados.append(extractEffectNameFromType(effectSection.getString("tipo", "UNKNOWN"))).append(", ");
                    }
                }
            }
        }
        
        // 4. PÉRDIDA DE PS (escalado por rango)
        MissionRank rank = plugin.getRankService().getRank(player);
        int psLoss = config.getInt("perdida_ps." + rank.name(), 10);
        int currentPs = plugin.getMissionService().getPS(player.getUniqueId());
        int newPs = Math.max(psMinimo, currentPs - psLoss);
        plugin.getMissionService().setPS(player.getUniqueId(), newPs);
        
        // 5. MENSAJES
        String efectosStr = efectosAplicados.length() > 0 ? efectosAplicados.substring(0, efectosAplicados.length() - 2) : "Ninguno";
        plugin.getMessageBus().sendMessage(player, config.getString("mensajes.muerte", "&c&l✗ Has muerto durante el desastre!"));
        plugin.getMessageBus().sendMessage(player, config.getString("mensajes.efectos_aplicados", "&7Penalizaciones: &c%efectos%")
            .replace("%efectos%", efectosStr));
        plugin.getMessageBus().sendMessage(player, config.getString("mensajes.ps_perdidos", "&7Has perdido &c-%ps% PS")
            .replace("%ps%", String.valueOf(psLoss)));
        
        // 6. SONIDOS Y PARTÍCULAS
        ConfigurationSection sonidoConfig = config.getConfigurationSection("sonidos");
        if (sonidoConfig != null) {
            String soundName = sonidoConfig.getString("muerte", "ENTITY_WITHER_HURT");
            float volumen = (float) sonidoConfig.getDouble("volumen", 1.0);
            float pitch = (float) sonidoConfig.getDouble("pitch", 0.8);
            try {
                // Migrado a Registry API
                org.bukkit.NamespacedKey soundKey = org.bukkit.NamespacedKey.fromString(soundName.toLowerCase());
                if (soundKey == null) soundKey = org.bukkit.NamespacedKey.minecraft(soundName.toLowerCase());
                Sound sound = org.bukkit.Registry.SOUNDS.get(soundKey);
                if (sound != null) {
                    player.playSound(loc, sound, volumen, pitch);
                }
            } catch (IllegalArgumentException ignored) {}
        }
        
        ConfigurationSection particulaConfig = config.getConfigurationSection("particulas");
        if (particulaConfig != null) {
            String particleName = particulaConfig.getString("tipo", "SMOKE_LARGE");
            int cantidad = particulaConfig.getInt("cantidad", 30);
            double radio = particulaConfig.getDouble("radio", 1.5);
            try {
                Particle particle = Particle.valueOf(particleName);
                player.getWorld().spawnParticle(particle, loc, cantidad, radio, radio, radio, 0.1);
            } catch (IllegalArgumentException ignored) {}
        }
        
        // 7. ANUNCIO PÚBLICO (solo si fue penalización severa)
        if (severaAplicada && castigosConfig.getBoolean("general.anuncio_publico.enabled", true)) {
            if (castigosConfig.getBoolean("general.anuncio_publico.solo_muerte_severa", true)) {
                String anuncio = castigosConfig.getString("general.anuncio_publico.mensaje", "&c%jugador% &7ha recibido penalizaciones severas")
                    .replace("%jugador%", player.getName())
                    .replace("%razon%", "muerte en desastre");
                org.bukkit.Bukkit.getServer().broadcast(net.kyori.adventure.text.Component.text(anuncio));
            }
        }
        
        // 8. DEBUG
        if (castigosConfig.getBoolean("general.debug", false)) {
            plugin.getLogger().info("[CASTIGOS] " + player.getName() + " penalizado por muerte: " + efectosStr + " | -" + psLoss + " PS");
        }
        
        // 9. Actualizar scoreboard y tablist
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().updatePlayer(player);
        }
        if (plugin.getTablistManager() != null) {
            plugin.getTablistManager().updatePlayer(player);
        }
    }
    
    /**
     * Aplica efecto de poción desde ConfigurationSection
     */
    private void applyEffectFromSection(Player player, ConfigurationSection section) {
        String tipo = section.getString("tipo");
        int duracionSec = section.getInt("duracion_segundos", 30);
        int amplificador = section.getInt("amplificador", 0);
        
        if (tipo == null) return;
        
        try {
            // Migrado a Registry API
            org.bukkit.NamespacedKey effectKey = org.bukkit.NamespacedKey.fromString(tipo.toLowerCase());
            if (effectKey == null) effectKey = org.bukkit.NamespacedKey.minecraft(tipo.toLowerCase());
            PotionEffectType effectType = org.bukkit.Registry.EFFECT.get(effectKey);
            if (effectType != null) {
                player.addPotionEffect(new PotionEffect(effectType, duracionSec * 20, amplificador));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[CASTIGOS] Error aplicando efecto " + tipo + ": " + e.getMessage());
        }
    }
    
    /**
     * Extrae nombre del efecto para mostrar en mensaje
     */
    private String extractEffectNameFromType(String effectType) {
        if (effectType.contains("WEAKNESS")) return "Debilidad";
        if (effectType.contains("SLOW_DIGGING")) return "Fatiga";
        if (effectType.contains("HUNGER")) return "Hambre";
        if (effectType.contains("SLOW")) return "Lentitud";
        if (effectType.contains("CONFUSION")) return "Náusea";
        if (effectType.contains("BLINDNESS")) return "Ceguera";
        if (effectType.contains("UNLUCK")) return "Mala Suerte";
        return "Desconocido";
    }
    
    // ═══════════════════════════════════════════════════════════════════
    //                    SISTEMA DE PS POR TIEMPO JUGADO
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Inicia tarea periódica para dar PS por tiempo jugado
     */
    private void startPlaytimeRewardTask() {
        // Ejecutar cada 30 minutos (36000 ticks = 30 minutos)
        org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                
                // Verificar si el jugador ha estado online por al menos 30 minutos
                if (playerJoinTime.containsKey(uuid)) {
                    long joinTime = playerJoinTime.get(uuid);
                    long currentTime = System.currentTimeMillis();
                    long timeOnline = currentTime - joinTime;
                    
                    // Si ha estado online por al menos 30 minutos
                    if (timeOnline >= PS_TIME_INTERVAL) {
                        // Dar PS
                        plugin.getMissionService().addPS(uuid, PS_PER_INTERVAL, "Tiempo jugado (30min)");
                        
                        // Notificar discretamente solo si debug está activo
                        if (plugin.getConfigManager().isDebugCiclo()) {
                            player.sendMessage("§7[§ePS§7] §a+1 PS §7por tiempo jugado");
                            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 1.5f);
                        }
                        
                        // Actualizar tiempo base para próxima recompensa
                        playerJoinTime.put(uuid, currentTime);
                    }
                }
            }
        }, 36000L, 36000L); // 30 minutos inicial, repetir cada 30 minutos
    }
    
    /**
     * Registra cuando un jugador se conecta (para tracking de tiempo)
     */
    private void trackPlayerJoin(Player player) {
        playerJoinTime.put(player.getUniqueId(), System.currentTimeMillis());
    }
    
    /**
     * Limpia tracking cuando un jugador se desconecta
     */
    private void trackPlayerQuit(Player player) {
        playerJoinTime.remove(player.getUniqueId());
    }
}
