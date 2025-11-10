package me.apocalipsis.listeners;

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
import me.apocalipsis.missions.MissionRank;
import me.apocalipsis.state.ServerState;
import me.apocalipsis.ui.ScoreboardManager;
import me.apocalipsis.ui.TablistManager;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PlayerListener implements Listener {

    private final Apocalipsis plugin;
    private final ScoreboardManager scoreboardManager;
    private final TablistManager tablistManager;
    
    // Sistema de castigos mejorado
    private FileConfiguration castigosConfig;
    private final Set<UUID> respawnImmunity = new HashSet<>();
    private final java.util.Random random = new java.util.Random();

    public PlayerListener(Apocalipsis plugin, ScoreboardManager scoreboardManager,
                         TablistManager tablistManager) {
        this.plugin = plugin;
        this.scoreboardManager = scoreboardManager;
        this.tablistManager = tablistManager;
        loadCastigosConfig();
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
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // [FIX DEFINITIVO] Forzar board compartido (crítico para que todos vean lo mismo)
        player.setScoreboard(org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard());
        
        // [EVASION PUNISHMENT] Aplicar castigos físicos pendientes
        plugin.getDisasterEvasionTracker().applyReconnectPunishment(player);
        
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
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // [RECONSTRUCCIÓN] Remover jugador del BossBar único del DisasterController
        plugin.getDisasterController().removePlayerFromBossBar(player);
        
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
        
        // Verificar si el sistema de castigos está habilitado
        if (!castigosConfig.getBoolean("enabled", true)) return;
        
        // Verificar si murió durante un desastre activo
        ServerState currentState = plugin.getStateManager().getCurrentState();
        if (currentState != ServerState.ACTIVO) return;
        
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
        int minPs = castigosConfig.getInt("general.ps_minimo", 0);
        int newPs = Math.max(minPs, currentPs - psLoss);
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
                Sound sound = Sound.valueOf(soundName);
                player.playSound(loc, sound, volumen, pitch);
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
                org.bukkit.Bukkit.broadcastMessage(anuncio);
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
            PotionEffectType effectType = PotionEffectType.getByName(tipo);
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
}
