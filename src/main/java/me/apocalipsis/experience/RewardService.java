package me.apocalipsis.experience;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.missions.MissionCatalog;
import me.apocalipsis.missions.MissionDifficulty;
import me.apocalipsis.missions.MissionRank;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.*;

/**
 * Servicio que entrega recompensas cuando un jugador sube de rango
 * o completa misiones diarias.
 * Las recompensas se configuran en recompensas.yml
 */
public class RewardService {
    
    private final Apocalipsis plugin;
    
    // Caché de recompensas por rango
    private final Map<MissionRank, RankReward> rewardsByRank = new HashMap<>();
    
    // Registro de recompensas entregadas (para evitar duplicados)
    private final Set<String> deliveredRewards = new HashSet<>();
    
    private final Random random = new Random();
    
    public RewardService(Apocalipsis plugin) {
        this.plugin = plugin;
        loadRewards();
    }
    
    /**
     * Carga las recompensas desde recompensas.yml
     */
    public void loadRewards() {
        rewardsByRank.clear();
        
        FileConfiguration config = plugin.getConfigManager().getRecompensasConfig();
        ConfigurationSection section = config.getConfigurationSection("recompensas_por_rango");
        
        if (section == null) {
            plugin.getLogger().warning("[Rewards] No se encontró sección 'recompensas_por_rango' en recompensas.yml");
            return;
        }
        
        for (MissionRank rank : MissionRank.values()) {
            if (rank == MissionRank.NOVATO) continue; // Novato no tiene recompensas
            
            String rankKey = rank.name();
            ConfigurationSection rankSection = section.getConfigurationSection(rankKey);
            
            if (rankSection == null) continue;
            
            List<String> commands = rankSection.getStringList("comandos");
            String message = rankSection.getString("mensaje", "");
            
            if (!commands.isEmpty()) {
                rewardsByRank.put(rank, new RankReward(commands, message));
            }
        }
        
        plugin.getLogger().info("[Rewards] Recompensas cargadas para " + rewardsByRank.size() + " rangos");
    }
    
    /**
     * Entrega las recompensas de un rango a un jugador
     * @return true si se entregaron recompensas, false si ya las había recibido
     */
    public boolean deliverRewards(Player player, MissionRank rank) {
        // Verificar si ya recibió esta recompensa
        String key = player.getUniqueId().toString() + ":" + rank.name();
        if (deliveredRewards.contains(key)) {
            return false; // Ya recibió esta recompensa
        }
        
        RankReward reward = rewardsByRank.get(rank);
        if (reward == null) {
            return false; // No hay recompensas para este rango
        }
        
        // Ejecutar comandos
        for (String command : reward.getCommands()) {
            String processedCommand = command.replace("%player%", player.getName());
            
            // Ejecutar en el siguiente tick para evitar problemas de sincronización
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
            });
        }
        
        // Enviar mensaje
        if (!reward.getMessage().isEmpty()) {
            String message = org.bukkit.ChatColor.translateAlternateColorCodes('&', reward.getMessage());
            player.sendMessage(message);
        }
        
        // Marcar como entregado
        deliveredRewards.add(key);
        
        // Efectos visuales
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        
        plugin.getLogger().info("[Rewards] Recompensas de " + rank.name() + " entregadas a " + player.getName());
        
        return true;
    }
    
    /**
     * Verifica y entrega todas las recompensas pendientes para un jugador
     * (útil para cuando un jugador se une y tiene múltiples rangos sin reclamar)
     */
    public void checkAndDeliverPendingRewards(Player player) {
        MissionRank currentRank = plugin.getRankService().getRank(player);
        
        // Verificar cada rango desde EXPLORADOR hasta el rango actual
        for (MissionRank rank : MissionRank.values()) {
            if (rank == MissionRank.NOVATO) continue;
            if (rank.ordinal() > currentRank.ordinal()) break;
            
            // Intentar entregar recompensas (solo entregará si no las ha recibido)
            deliverRewards(player, rank);
        }
    }
    
    /**
     * Fuerza la entrega de recompensas de un rango (ignora si ya las recibió)
     * Solo para uso administrativo
     */
    public void forceDeliverRewards(Player player, MissionRank rank) {
        String key = player.getUniqueId().toString() + ":" + rank.name();
        deliveredRewards.remove(key); // Remover el registro
        deliverRewards(player, rank);
    }
    
    /**
     * Reinicia las recompensas entregadas a un jugador
     * Solo para uso administrativo
     */
    public void resetPlayerRewards(UUID uuid) {
        deliveredRewards.removeIf(key -> key.startsWith(uuid.toString()));
    }
    
    /**
     * Obtiene la lista de recompensas para un rango
     */
    public RankReward getRankReward(MissionRank rank) {
        return rewardsByRank.get(rank);
    }
    
    /**
     * Verifica si un jugador ya recibió las recompensas de un rango
     */
    public boolean hasReceivedRewards(Player player, MissionRank rank) {
        String key = player.getUniqueId().toString() + ":" + rank.name();
        return deliveredRewards.contains(key);
    }
    
    /**
     * Recarga las recompensas desde la configuración
     */
    public void reload() {
        loadRewards();
    }
    
    /**
     * Entrega recompensas al completar una misión individual (bonus aleatorio)
     */
    public void deliverMissionReward(Player player, MissionDifficulty difficulty) {
        FileConfiguration config = plugin.getConfigManager().getRecompensasConfig();
        
        if (!config.getBoolean("recompensas_por_mision.enabled", true)) {
            return;
        }
        
        String path = "recompensas_por_mision.por_dificultad." + difficulty.name();
        ConfigurationSection section = config.getConfigurationSection(path);
        
        if (section == null) return;
        
        double probability = section.getDouble("probabilidad", 0.0);
        if (random.nextDouble() > probability) {
            return; // No hay recompensa esta vez
        }
        
        // Ejecutar comandos de recompensa
        List<String> commands = section.getStringList("items");
        for (String command : commands) {
            String processedCommand = command.replace("%player%", player.getName());
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
            });
        }
        
        // Enviar mensaje
        String message = section.getString("mensaje", "");
        if (!message.isEmpty()) {
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
        }
        
        // Sonido sutil
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
    }
    
    /**
     * Entrega recompensas por completar todas las misiones del día
     */
    public void deliverDailyCompletionReward(Player player) {
        FileConfiguration config = plugin.getConfigManager().getRecompensasConfig();
        
        if (!config.getBoolean("recompensas_diarias_completas.enabled", true)) {
            return;
        }
        
        Location loc = player.getLocation();
        MissionRank rank = plugin.getRankService().getRank(player);
        
        // 1. Recompensas base
        List<String> baseCommands = config.getStringList("recompensas_diarias_completas.recompensas_base.comandos");
        for (String command : baseCommands) {
            String processedCommand = command.replace("%player%", player.getName());
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
            });
        }
        
        String baseMessage = config.getString("recompensas_diarias_completas.recompensas_base.mensaje", "");
        if (!baseMessage.isEmpty()) {
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', baseMessage));
        }
        
        // 2. Bonus por rango
        String rankPath = "recompensas_diarias_completas.bonus_por_rango." + rank.name();
        ConfigurationSection rankSection = config.getConfigurationSection(rankPath);
        
        if (rankSection != null) {
            List<String> rankCommands = rankSection.getStringList("comandos");
            for (String command : rankCommands) {
                String processedCommand = command.replace("%player%", player.getName());
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
                });
            }
            
            String rankMessage = rankSection.getString("mensaje", "");
            if (!rankMessage.isEmpty()) {
                player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', rankMessage));
            }
        }
        
        // 3. Título épico
        String titulo = config.getString("recompensas_diarias_completas.titulo", "&6&l¡COMPLETADO!");
        String subtitulo = config.getString("recompensas_diarias_completas.subtitulo", "&eHas terminado todas las misiones");
        titulo = org.bukkit.ChatColor.translateAlternateColorCodes('&', titulo);
        subtitulo = org.bukkit.ChatColor.translateAlternateColorCodes('&', subtitulo);
        player.sendTitle(titulo, subtitulo, 10, 60, 20);
        
        // 4. Efectos especiales
        if (config.getBoolean("recompensas_diarias_completas.efectos.sonidos", true)) {
            player.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.5f, 1.0f);
            player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.2f, 1.0f);
            player.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 2.0f);
        }
        
        if (config.getBoolean("recompensas_diarias_completas.efectos.particulas", true)) {
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.add(0, 2, 0), 50, 1.0, 1.0, 1.0, 0.1);
            player.getWorld().spawnParticle(Particle.FIREWORK, loc, 30, 0.8, 0.8, 0.8, 0.15);
            player.getWorld().spawnParticle(Particle.END_ROD, loc, 25, 0.6, 0.6, 0.6, 0.1);
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 40, 1.2, 1.2, 1.2, 0.05);
        }
        
        // 5. Fuegos artificiales
        int fireworkCount = config.getInt("recompensas_diarias_completas.efectos.fuegos_artificiales", 5);
        for (int i = 0; i < fireworkCount; i++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                spawnRandomFirework(player.getLocation());
            }, i * 10L); // Escalonados cada 0.5s
        }
        
        plugin.getLogger().info("[Rewards] " + player.getName() + " completó todas las misiones diarias y recibió recompensas");
    }
    
    /**
     * Genera un fuego artificial aleatorio en una ubicación
     */
    private void spawnRandomFirework(Location loc) {
        Firework fw = (Firework) loc.getWorld().spawnEntity(loc.add(0, 1, 0), EntityType.FIREWORK_ROCKET);
        FireworkMeta meta = fw.getFireworkMeta();
        
        FireworkEffect.Type[] types = FireworkEffect.Type.values();
        FireworkEffect.Type type = types[random.nextInt(types.length)];
        
        Color[] colors = {Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.AQUA, Color.BLUE, Color.PURPLE, Color.FUCHSIA, Color.WHITE};
        Color c1 = colors[random.nextInt(colors.length)];
        Color c2 = colors[random.nextInt(colors.length)];
        Color fade = colors[random.nextInt(colors.length)];
        
        FireworkEffect effect = FireworkEffect.builder()
            .with(type)
            .withColor(c1, c2)
            .withFade(fade)
            .trail(random.nextBoolean())
            .flicker(random.nextBoolean())
            .build();
        
        meta.addEffect(effect);
        meta.setPower(1);
        fw.setFireworkMeta(meta);
    }
    
    /**
     * Clase que representa una recompensa de rango
     */
    public static class RankReward {
        private final List<String> commands;
        private final String message;
        
        public RankReward(List<String> commands, String message) {
            this.commands = commands;
            this.message = message;
        }
        
        public List<String> getCommands() {
            return commands;
        }
        
        public String getMessage() {
            return message;
        }
    }
}
