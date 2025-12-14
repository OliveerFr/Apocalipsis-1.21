package me.apocalipsis.experience;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.missions.MissionDifficulty;
import me.apocalipsis.missions.MissionRank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;

/**
 * Servicio que entrega recompensas cuando un jugador sube de rango
 * o completa misiones diarias.
 * Las recompensas se configuran en recompensas.yml
 */
public class RewardService {
    
    private final Apocalipsis plugin;
    private final File dataFile;
    
    // Caché de recompensas por rango
    private final Map<MissionRank, RankReward> rewardsByRank = new HashMap<>();
    
    // Registro de recompensas entregadas (para evitar duplicados) - PERSISTENTE
    private final Set<String> deliveredRewards = new HashSet<>();
    
    private final Random random = new Random();
    
    public RewardService(Apocalipsis plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "rewards_delivered.yml");
        loadDeliveredRewards();
        loadRewards();
    }
    
    /**
     * Carga el registro de recompensas ya entregadas desde archivo
     */
    private void loadDeliveredRewards() {
        if (!dataFile.exists()) {
            return;
        }
        
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
            List<String> delivered = config.getStringList("delivered_rewards");
            deliveredRewards.addAll(delivered);
            plugin.getLogger().info("[Rewards] Cargadas " + deliveredRewards.size() + " recompensas ya entregadas");
        } catch (Exception e) {
            plugin.getLogger().warning("[Rewards] Error cargando rewards_delivered.yml: " + e.getMessage());
        }
    }
    
    /**
     * Guarda el registro de recompensas entregadas a archivo
     */
    private void saveDeliveredRewards() {
        try {
            FileConfiguration config = new YamlConfiguration();
            config.set("delivered_rewards", new ArrayList<>(deliveredRewards));
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("[Rewards] Error guardando rewards_delivered.yml: " + e.getMessage());
        }
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
     * Las recompensas se añaden al sistema de reclamación (/recompensa)
     * @return true si se añadieron recompensas, false si ya las había recibido
     */
    public boolean deliverRewards(Player player, MissionRank rank) {
        // Verificar si ya recibió esta recompensa
        String key = player.getUniqueId().toString() + ":" + rank.name();
        if (deliveredRewards.contains(key)) {
            plugin.getLogger().info("[Rewards] " + player.getName() + " ya recibió recompensas de " + rank.name());
            return false; // Ya recibió esta recompensa
        }
        
        RankReward reward = rewardsByRank.get(rank);
        if (reward == null) {
            plugin.getLogger().warning("[Rewards] No hay recompensas configuradas para rango: " + rank.name());
            return false; // No hay recompensas para este rango
        }
        
        plugin.getLogger().info("[Rewards] Procesando recompensas de " + rank.name() + " para " + player.getName());
        plugin.getLogger().info("[Rewards] Comandos configurados: " + reward.getCommands().size());
        
        // Convertir comandos give a ItemStacks y separar comandos especiales
        List<ItemStack> items = new ArrayList<>();
        List<String> specialCommands = new ArrayList<>();
        
        for (String command : reward.getCommands()) {
            String processedCommand = command.replace("%player%", player.getName());
            ItemStack item = parseGiveCommand(processedCommand);
            
            if (item != null) {
                items.add(item);
                plugin.getLogger().info("[Rewards] Item parseado: " + item.getType() + " x" + item.getAmount());
            } else {
                // Comandos especiales (ps give) van a ejecutarse inmediatamente
                specialCommands.add(processedCommand);
                plugin.getLogger().info("[Rewards] Comando especial detectado: " + processedCommand);
            }
        }
        
        plugin.getLogger().info("[Rewards] Items totales parseados: " + items.size() + " | Comandos especiales: " + specialCommands.size());
        
        // Ejecutar comandos especiales inmediatamente
        int protectionBlocksTotal = 0;
        for (String cmd : specialCommands) {
            final String finalCmd = cmd;
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
                    plugin.getLogger().info("[Rewards] ✓ Comando ejecutado: " + finalCmd);
                } catch (Exception e) {
                    plugin.getLogger().severe("[Rewards] ✗ Error ejecutando comando: " + finalCmd);
                    e.printStackTrace();
                }
            });
        }
        
        // Añadir items al sistema de reclamación
        if (!items.isEmpty()) {
            if (plugin.getRewardClaimSystem() == null) {
                plugin.getLogger().severe("[Rewards] ERROR: RewardClaimSystem es NULL!");
                // Intentar entregar items directamente como fallback
                for (ItemStack item : items) {
                    player.getInventory().addItem(item);
                }
                player.sendMessage("§c⚠ Sistema de recompensas no disponible. Items entregados directamente.");
            } else {
                String displayName = "§6⬆ Ascenso a " + rank.getDisplayName();
                plugin.getRewardClaimSystem().addRewards(
                    player.getUniqueId(),
                    "RANGO_" + rank.name(),
                    displayName,
                    items,
                    1440, // 24 horas para reclamar
                    rank.name(),
                    0
                );
                
                plugin.getLogger().info("[Rewards] ✓ Items agregados al sistema de reclamación: " + items.size());
                
                // Notificar sobre reclamación
                player.sendMessage("");
                player.sendMessage("§6§l════════════════════════════════════════");
                player.sendMessage("§e§l  🎁 RECOMPENSAS DE RANGO DISPONIBLES");
                if (protectionBlocksTotal > 0) {
                    player.sendMessage("§a§l  🛡 +" + protectionBlocksTotal + " Bloque(s) de Protección");
                }
                player.sendMessage("§7  Usa §f/recompensa §7para reclamar tus items.");
                player.sendMessage("§7  Tienes §e24 horas §7para reclamarlas.");
                player.sendMessage("§6§l════════════════════════════════════════");
                player.sendMessage("");
            }
        } else {
            plugin.getLogger().warning("[Rewards] No se generaron items para " + rank.name() + " (solo comandos especiales)");
        }
        
        // Enviar mensaje del rango
        if (!reward.getMessage().isEmpty()) {
            Component message = LegacyComponentSerializer.legacyAmpersand().deserialize(reward.getMessage());
            player.sendMessage(message);
        }
        
        // Marcar como entregado y guardar
        deliveredRewards.add(key);
        saveDeliveredRewards();
        
        // Efectos visuales
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        
        plugin.getLogger().info("[Rewards] ✓ Recompensas de " + rank.name() + " añadidas para " + player.getName());
        
        return true;
    }
    
    /**
     * Verifica y entrega recompensas pendientes SOLO del rango actual
     * (cuando el jugador sube de rango mientras está offline)
     * Ya NO entrega recompensas de rangos anteriores al reconectar
     */
    public void checkAndDeliverPendingRewards(Player player) {
        MissionRank currentRank = plugin.getRankService().getRank(player);
        
        // Solo verificar el rango actual (por si subió mientras estaba offline)
        if (currentRank != MissionRank.NOVATO) {
            // Intentar entregar recompensas del rango actual (solo si no las ha recibido)
            deliverRewards(player, currentRank);
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
     * Guarda los datos de recompensas entregadas
     * Llamar al desactivar el plugin
     */
    public void saveData() {
        saveDeliveredRewards();
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // PARSEO DE COMANDOS A ITEMSTACK
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Patrón para comandos "give" de Minecraft
     * Formato: give <player> minecraft:<item> [cantidad]
     */
    private static final Pattern GIVE_PATTERN = Pattern.compile(
        "^give\\s+\\S+\\s+(?:minecraft:)?(\\w+)(?:\\s+(\\d+))?$",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Parsea un comando "give" y lo convierte en ItemStack
     * @param command El comando a parsear (ej: "give Steve minecraft:diamond 5")
     * @return ItemStack si es un comando give válido, null si no lo es
     */
    private ItemStack parseGiveCommand(String command) {
        Matcher matcher = GIVE_PATTERN.matcher(command.trim());
        
        if (!matcher.matches()) {
            return null; // No es un comando give
        }
        
        String materialName = matcher.group(1).toUpperCase();
        String amountStr = matcher.group(2);
        int amount = (amountStr != null) ? Integer.parseInt(amountStr) : 1;
        
        try {
            Material material = Material.valueOf(materialName);
            if (material.isItem()) {
                return new ItemStack(material, amount);
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[Rewards] Material desconocido en comando: " + materialName);
        }
        
        return null;
    }
    
    /**
     * Crea un ItemStack especial que representa un bloque de protección.
     * Este item se muestra en el menú de recompensas y al hacer click
     * ejecuta el comando para dar el bloque de protección.
     * 
     * @param command El comando completo (ej: "ps give Player 15 2")
     * @param amount La cantidad de bloques
     * @return ItemStack con el bloque de esmeralda y metadata especial
     */
    private ItemStack createProtectionBlockItem(String command, int amount) {
        ItemStack item = new ItemStack(Material.EMERALD_BLOCK, amount);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§a§l🛡 Bloque de Protección");
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Cantidad: §e" + amount);
            lore.add("");
            lore.add("§7Este bloque te protege de");
            lore.add("§7los desastres naturales.");
            lore.add("");
            lore.add("§e▶ Click para reclamar");
            lore.add("");
            // Guardar el comando en el lore (oculto)
            lore.add("§8§oPS_CMD:" + command);
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // RECOMPENSAS POR MISIONES
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Entrega recompensas al completar una misión individual (bonus aleatorio)
     * Las recompensas van al sistema de reclamación (/recompensa)
     */
    public void deliverMissionReward(Player player, MissionDifficulty difficulty) {
        FileConfiguration config = plugin.getConfigManager().getRecompensasConfig();
        
        plugin.getLogger().info("[Rewards] Procesando recompensa de misión para " + player.getName() + " (dificultad: " + difficulty.name() + ")");
        
        if (!config.getBoolean("recompensas_por_mision.enabled", true)) {
            plugin.getLogger().info("[Rewards] Sistema de recompensas por misión deshabilitado");
            return;
        }
        
        String path = "recompensas_por_mision.por_dificultad." + difficulty.name();
        ConfigurationSection section = config.getConfigurationSection(path);
        
        if (section == null) {
            plugin.getLogger().warning("[Rewards] No existe configuración para: " + path);
            return;
        }
        
        double probability = section.getDouble("probabilidad", 0.0);
        double roll = random.nextDouble();
        plugin.getLogger().info("[Rewards] Probabilidad: " + probability + " | Roll: " + roll);
        
        if (roll > probability) {
            plugin.getLogger().info("[Rewards] No hay recompensa esta vez (roll no pasó)");
            return; // No hay recompensa esta vez
        }
        
        // Convertir comandos give a ItemStacks
        List<String> commands = section.getStringList("items");
        plugin.getLogger().info("[Rewards] Comandos configurados: " + commands.size());
        
        List<ItemStack> items = new ArrayList<>();
        
        for (String command : commands) {
            String processedCommand = command.replace("%player%", player.getName());
            ItemStack item = parseGiveCommand(processedCommand);
            if (item != null) {
                items.add(item);
                plugin.getLogger().info("[Rewards] Item parseado: " + item.getType() + " x" + item.getAmount());
            } else {
                plugin.getLogger().warning("[Rewards] No se pudo parsear comando: " + command);
            }
        }
        
        plugin.getLogger().info("[Rewards] Items totales parseados: " + items.size());
        
        // Añadir al sistema de reclamación si hay items
        if (!items.isEmpty() && plugin.getRewardClaimSystem() != null) {
            String displayName = "§e✦ Bonus Misión " + getDifficultyDisplay(difficulty);
            plugin.getRewardClaimSystem().addRewards(
                player.getUniqueId(),
                "MISSION_BONUS_" + difficulty.name(),
                displayName,
                items,
                60, // 1 hora para reclamar
                null,
                0
            );
            plugin.getLogger().info("[Rewards] ✓ Items agregados al sistema de reclamación: " + items.size());
        } else if (items.isEmpty()) {
            plugin.getLogger().warning("[Rewards] ⚠ No se generaron items para agregar");
        } else if (plugin.getRewardClaimSystem() == null) {
            plugin.getLogger().severe("[Rewards] ✗ RewardClaimSystem es NULL!");
        }
        
        // Enviar mensaje
        String message = section.getString("mensaje", "");
        if (message != null && !message.isEmpty()) {
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
            player.sendMessage("§7Usa §f/recompensa §7para reclamar.");
        }
        
        // Sonido sutil
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
    }
    
    /**
     * Obtiene el nombre de dificultad formateado
     */
    private String getDifficultyDisplay(MissionDifficulty difficulty) {
        switch (difficulty) {
            case FACIL: return "§aFácil";
            case MEDIA: return "§eMedia";
            case DIFICIL: return "§cDifícil";
            default: return "§7Desconocida";
        }
    }
    
    /**
     * Entrega recompensas por completar todas las misiones del día
     * Las recompensas van al sistema de reclamación (/recompensa)
     */
    public void deliverDailyCompletionReward(Player player) {
        FileConfiguration config = plugin.getConfigManager().getRecompensasConfig();
        
        if (!config.getBoolean("recompensas_diarias_completas.enabled", true)) {
            return;
        }
        
        Location loc = player.getLocation();
        MissionRank rank = plugin.getRankService().getRank(player);
        
        // Recopilar todos los items
        List<ItemStack> allItems = new ArrayList<>();
        
        // 1. Recompensas base
        List<String> baseCommands = config.getStringList("recompensas_diarias_completas.recompensas_base.comandos");
        for (String command : baseCommands) {
            String processedCommand = command.replace("%player%", player.getName());
            ItemStack item = parseGiveCommand(processedCommand);
            if (item != null) {
                allItems.add(item);
            }
        }
        
        // 2. Bonus por rango
        String rankPath = "recompensas_diarias_completas.bonus_por_rango." + rank.name();
        ConfigurationSection rankSection = config.getConfigurationSection(rankPath);
        
        if (rankSection != null) {
            List<String> rankCommands = rankSection.getStringList("comandos");
            for (String command : rankCommands) {
                String processedCommand = command.replace("%player%", player.getName());
                ItemStack item = parseGiveCommand(processedCommand);
                if (item != null) {
                    allItems.add(item);
                }
            }
        }
        
        // Añadir todos los items al sistema de reclamación
        if (!allItems.isEmpty() && plugin.getRewardClaimSystem() != null) {
            String displayName = "§6★ Misiones Diarias Completadas";
            plugin.getRewardClaimSystem().addRewards(
                player.getUniqueId(),
                "DAILY_COMPLETE",
                displayName,
                allItems,
                120, // 2 horas para reclamar
                rank.name(),
                0
            );
            
            // Mensaje sobre reclamación
            String baseMessage = config.getString("recompensas_diarias_completas.recompensas_base.mensaje", "");
            if (baseMessage != null && !baseMessage.isEmpty()) {
                player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(baseMessage));
            }
            
            String rankMessage = rankSection != null ? rankSection.getString("mensaje", "") : "";
            if (rankMessage != null && !rankMessage.isEmpty()) {
                player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(rankMessage));
            }
            
            player.sendMessage("§7Usa §f/recompensa §7para reclamar tus items.");
        }
        
        // 3. Título épico
        String titulo = config.getString("recompensas_diarias_completas.titulo", "&6&l¡COMPLETADO!");
        String subtitulo = config.getString("recompensas_diarias_completas.subtitulo", "&eHas terminado todas las misiones");
        
        if (titulo == null) titulo = "&6&l¡COMPLETADO!";
        if (subtitulo == null) subtitulo = "&eHas terminado todas las misiones";
        
        Component titleComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(titulo);
        Component subtitleComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(subtitulo);
        
        Title title = Title.title(
            titleComponent,
            subtitleComponent,
            Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000))
        );
        player.showTitle(title);
        
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
        
        plugin.getLogger().info("[Rewards] " + player.getName() + " completó todas las misiones diarias - recompensas en /recompensa");
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
