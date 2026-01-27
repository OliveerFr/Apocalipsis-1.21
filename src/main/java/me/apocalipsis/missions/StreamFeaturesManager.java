package me.apocalipsis.missions;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.apocalipsis.Apocalipsis;

/**
 * Gestiona las características exclusivas de stream:
 * - Drops especiales cuando el streamer está online
 * - Sistema de tokens de stream
 * - Eventos automáticos de stream
 * - Misiones exclusivas de stream
 */
public class StreamFeaturesManager {
    
    private final Apocalipsis plugin;
    private File configFile;
    private FileConfiguration config;
    
    // Base de datos de tokens
    private final TokenDatabase tokenDatabase;
    
    // Estado del streamer
    private boolean streamerOnline = false;
    private String streamerUsername;
    
    // Task IDs
    private int eventTaskId = -1;
    private int reminderTaskId = -1;
    
    public StreamFeaturesManager(Apocalipsis plugin) {
        this.plugin = plugin;
        this.tokenDatabase = new TokenDatabase(plugin);
        loadConfig();
        startTasks();
    }
    
    /**
     * Carga la configuración del archivo
     */
    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "stream_features.yml");
        
        if (!configFile.exists()) {
            plugin.saveResource("stream_features.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        streamerUsername = config.getString("streamer.username", "OliveerF");
        updateStreamerStatus();
    }
    
    /**
     * Recarga la configuración
     */
    public void reload() {
        config = YamlConfiguration.loadConfiguration(configFile);
        streamerUsername = config.getString("streamer.username", "OliveerF");
        updateStreamerStatus();
        
        plugin.getLogger().info("Stream Features recargado. Streamer: " + streamerUsername);
    }
    
    /**
     * Actualiza el estado del streamer (online/offline)
     */
    public void updateStreamerStatus() {
        Player streamer = Bukkit.getPlayerExact(streamerUsername);
        boolean wasOnline = streamerOnline;
        streamerOnline = (streamer != null && streamer.isOnline());
        
        // Si cambió de estado, notificar
        if (wasOnline != streamerOnline) {
            if (streamerOnline) {
                broadcastStreamerOnline();
            } else {
                broadcastStreamerOffline();
            }
        }
    }
    
    /**
     * Tarea periódica que verifica y anuncia cambios de estado
     * Solo se usa para broadcasts automáticos, no para verificaciones en tiempo real
     */
    private void checkAndBroadcastStreamerStatus() {
        Player streamer = Bukkit.getPlayerExact(streamerUsername);
        boolean currentStatus = (streamer != null && streamer.isOnline());
        boolean wasOnline = streamerOnline;
        
        streamerOnline = currentStatus;
        
        // Solo broadcast si cambió de estado
        if (wasOnline != currentStatus) {
            if (currentStatus) {
                broadcastStreamerOnline();
            } else {
                broadcastStreamerOffline();
            }
        }
    }
    
    /**
     * Verifica si el streamer está online (en tiempo real)
     * Este método verifica en tiempo real en vez de usar la caché
     */
    public boolean isStreamerOnline() {
        // Verificación en tiempo real para precisión en UIs
        Player streamer = Bukkit.getPlayerExact(streamerUsername);
        boolean currentStatus = (streamer != null && streamer.isOnline());
        
        // Actualizar caché si es diferente (sin broadcast para evitar spam)
        if (currentStatus != streamerOnline) {
            streamerOnline = currentStatus;
        }
        
        return currentStatus;
    }
    
    /**
     * Obtiene el username del streamer
     */
    public String getStreamerUsername() {
        return streamerUsername;
    }
    
    /**
     * Notifica a todos que el streamer está online
     */
    private void broadcastStreamerOnline() {
        if (!config.getBoolean("drops_stream.enabled", true)) return;
        
        String mensajeOnline = config.getString("mensajes.streamer_online", 
            "&6&l[STREAM] &e¡" + streamerUsername + " está online!");
        String mensajeDrops = config.getString("mensajes.drops_activados", 
            "&7&o¡Drops especiales activados! Mata mobs para obtener tokens.");
        
        mensajeOnline = mensajeOnline.replace("&", "§").replace("%streamer%", streamerUsername);
        mensajeDrops = mensajeDrops.replace("&", "§");
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(mensajeOnline);
            player.sendMessage(mensajeDrops);
            player.sendMessage("§aXP normal restaurado.");
        }
    }
    
    /**
     * Notifica a todos que el streamer está offline
     */
    private void broadcastStreamerOffline() {
        String mensajeOffline = config.getString("mensajes.streamer_offline", 
            "&6&l[STREAM] &7" + streamerUsername + " está offline.");
        mensajeOffline = mensajeOffline.replace("&", "§").replace("%streamer%", streamerUsername);
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(mensajeOffline);
            player.sendMessage("§7XP reducido al §c20% §7hasta que vuelva.");
        }
    }
    
    /**
     * Procesa un drop especial de stream
     * @return true si se otorgó un drop, false si no
     */
    public boolean processStreamDrop(Player player) {
        if (!streamerOnline) return false;
        if (!config.getBoolean("drops_stream.enabled", true)) return false;
        
        // Detectar si el jugador es el streamer
        boolean isStreamer = player.getName().equalsIgnoreCase(streamerUsername);
        
        // Aplicar multiplicador de chance si es el streamer
        double chanceBase = config.getDouble("drops_stream.chance_base", 0.15);
        if (isStreamer) {
            double multiplicador = config.getDouble("streamer.chance_multiplicador", 2.5);
            chanceBase *= multiplicador;
            // Asegurar que no exceda 100%
            chanceBase = Math.min(chanceBase, 1.0);
        }
        
        if (Math.random() > chanceBase) return false;
        
        // Obtener lista de items posibles
        List<Map<?, ?>> items = config.getMapList("drops_stream.items");
        if (items.isEmpty()) return false;
        
        // Seleccionar un item basado en su chance (con bonus para streamer si aplica)
        Map<?, ?> selectedItem = selectItemByChance(items, isStreamer);
        if (selectedItem == null) return false;
        
        // Crear el item
        ItemStack item = createStreamItem(selectedItem);
        if (item == null) return false;
        
        // Detectar si es un token y registrarlo SOLO en la base de datos
        boolean isToken = item.getType() == Material.NETHER_STAR;
        if (isToken) {
            // Añadir tokens a la base de datos (NO al inventario)
            addPlayerTokens(player.getUniqueId(), item.getAmount(), "Drop de mob hostil");
            
            // Notificar al jugador con mensaje especial para tokens
            String mensaje = config.getString("drops_stream.mensaje_token", 
                "§6§l[STREAM DROP] §e¡Has obtenido %cantidad% Token(s) de Stream!");
            mensaje = mensaje.replace("&", "§");
            mensaje = mensaje.replace("%cantidad%", String.valueOf(item.getAmount()));
            player.sendMessage(mensaje);
            player.sendMessage("§7Los tokens se han añadido automáticamente a tu cuenta.");
            player.sendMessage("§7Usa §e/avo canjear §7para ver tus tokens y recompensas.");
        } else {
            // Dar items NO-token al inventario normalmente
            player.getInventory().addItem(item);
            
            // Enviar mensaje normal para items físicos
            String mensaje = config.getString("drops_stream.mensaje", "§6§l[STREAM DROP] §e¡Has obtenido %item%!");
            mensaje = mensaje.replace("&", "§");
            mensaje = mensaje.replace("%item%", item.getItemMeta().getDisplayName());
            player.sendMessage(mensaje);
        }
        
        // Reproducir sonido (aplicable para ambos casos)
        String sonido = config.getString("drops_stream.sonido", "ENTITY_PLAYER_LEVELUP");
        try {
            player.playSound(player.getLocation(), org.bukkit.Sound.valueOf(sonido), 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Sonido inválido: " + sonido);
        }
        
        return true;
    }
    
    /**
     * Selecciona un item de la lista basado en su chance
     * @param items Lista de items posibles
     * @param isStreamer Si el jugador es el streamer (aplica multiplicador a tokens)
     */
    private Map<?, ?> selectItemByChance(List<Map<?, ?>> items, boolean isStreamer) {
        double totalChance = 0.0;
        
        // Calcular chance total, aplicando multiplicador a tokens si es streamer
        for (Map<?, ?> item : items) {
            double chance = getDouble(item, "chance", 0.0);
            
            // Si es el streamer y el item es un token (NETHER_STAR), aplicar multiplicador
            if (isStreamer && "NETHER_STAR".equals(item.get("tipo"))) {
                double tokenMultiplicador = config.getDouble("streamer.token_multiplicador", 3.0);
                chance *= tokenMultiplicador;
            }
            
            totalChance += chance;
        }
        
        double roll = Math.random() * totalChance;
        double cumulative = 0.0;
        
        for (Map<?, ?> item : items) {
            double chance = getDouble(item, "chance", 0.0);
            
            // Aplicar el mismo multiplicador al seleccionar
            if (isStreamer && "NETHER_STAR".equals(item.get("tipo"))) {
                double tokenMultiplicador = config.getDouble("streamer.token_multiplicador", 3.0);
                chance *= tokenMultiplicador;
            }
            
            cumulative += chance;
            if (roll <= cumulative) {
                return item;
            }
        }
        
        return null;
    }
    
    /**
     * Crea un ItemStack a partir de la configuración
     */
    private ItemStack createStreamItem(Map<?, ?> itemConfig) {
        String tipo = getString(itemConfig, "tipo", "IRON_INGOT");
        int cantidad = getInt(itemConfig, "cantidad", 1);
        
        Material material;
        try {
            material = Material.valueOf(tipo);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Material inválido: " + tipo);
            return null;
        }
        
        ItemStack item = new ItemStack(material, cantidad);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Nombre
            String nombre = getString(itemConfig, "nombre", null);
            if (nombre != null) {
                meta.setDisplayName(nombre.replace("&", "§"));
            }
            
            // Lore
            Object loreObj = itemConfig.get("lore");
            if (loreObj instanceof List) {
                List<String> lore = new ArrayList<>();
                for (Object line : (List<?>) loreObj) {
                    lore.add(line.toString().replace("&", "§"));
                }
                meta.setLore(lore);
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Obtiene los tokens de un jugador desde la base de datos
     */
    public int getPlayerTokens(UUID uuid) {
        return tokenDatabase.getTokens(uuid);
    }
    
    /**
     * Añade tokens a un jugador de forma asíncrona
     */
    public void addPlayerTokens(UUID uuid, int amount, String reason) {
        tokenDatabase.addTokens(uuid, amount, reason).thenAccept(success -> {
            if (!success) {
                plugin.getLogger().warning("Error añadiendo " + amount + " tokens a " + uuid);
            }
        });
    }
    
    /**
     * Quita tokens a un jugador de forma síncrona
     * @return true si tenía suficientes tokens, false si no
     */
    public boolean removePlayerTokens(UUID uuid, int amount, String reason) {
        try {
            return tokenDatabase.removeTokens(uuid, amount, reason).get();
        } catch (Exception e) {
            plugin.getLogger().severe("Error quitando tokens: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Inicia las tareas periódicas
     */
    private void startTasks() {
        // Tarea de verificación y broadcast de estado del streamer (cada 5 segundos)
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkAndBroadcastStreamerStatus, 100L, 100L);
        
        // Tarea de recordatorio periódico
        if (config.getBoolean("mensajes.recordatorio_periodico.enabled", true)) {
            int intervalo = config.getInt("mensajes.recordatorio_periodico.intervalo_minutos", 20);
            reminderTaskId = Bukkit.getScheduler().runTaskTimer(plugin, this::sendReminder, 
                intervalo * 1200L, intervalo * 1200L).getTaskId();
        }
    }
    
    /**
     * Envía recordatorio periódico
     */
    private void sendReminder() {
        if (!streamerOnline) return;
        
        String mensaje = config.getString("mensajes.recordatorio_periodico.mensaje", 
            "§6§l[STREAM] §e¡Recuerda que estás ganando §a§lx3 XP §emientras el streamer está online!");
        mensaje = mensaje.replace("&", "§"); // Convertir códigos de color
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(mensaje);
        }
    }
    
    /**
     * Canjea tokens por una recompensa
     */
    public boolean redeemReward(Player player, String rewardId) {
        if (!config.getBoolean("canje_tokens.enabled", true)) {
            player.sendMessage("§cEl sistema de canje está deshabilitado.");
            return false;
        }
        
        ConfigurationSection recompensas = config.getConfigurationSection("canje_tokens.recompensas");
        if (recompensas == null) {
            player.sendMessage("§cNo hay recompensas configuradas.");
            return false;
        }
        
        ConfigurationSection reward = recompensas.getConfigurationSection(rewardId);
        if (reward == null) {
            player.sendMessage("§cRecompensa no encontrada: §e" + rewardId);
            return false;
        }
        
        int costo = reward.getInt("costo_tokens", 0);
        
        // Contar tokens en base de datos + inventario
        int tokensDatabase = getPlayerTokens(player.getUniqueId());
        int tokensInventory = countTokensInInventory(player);
        int totalTokens = tokensDatabase + tokensInventory;
        
        plugin.getLogger().info("[StreamTokens] Canje para " + player.getName() + 
            " | BD: " + tokensDatabase + " | Inv: " + tokensInventory + " | Total: " + totalTokens + " | Costo: " + costo);
        
        if (totalTokens < costo) {
            player.sendMessage("§cNo tienes suficientes tokens. Necesitas: §e" + costo + " §c| Tienes: §e" + totalTokens);
            return false;
        }
        
        // Primero quitar tokens del inventario
        int remaining = costo;
        if (tokensInventory > 0) {
            int toRemove = Math.min(tokensInventory, remaining);
            removeTokensFromInventory(player, toRemove);
            remaining -= toRemove;
            plugin.getLogger().info("[StreamTokens] Removidos " + toRemove + " tokens del inventario");
        }
        
        // Luego quitar de la base de datos si es necesario
        if (remaining > 0) {
            if (!removePlayerTokens(player.getUniqueId(), remaining, "Canje: " + rewardId)) {
                player.sendMessage("§cError al procesar los tokens.");
                return false;
            }
            plugin.getLogger().info("[StreamTokens] Removidos " + remaining + " tokens de la base de datos");
        }
        
        // Dar recompensas
        List<String> comandos = reward.getStringList("items");
        for (String comando : comandos) {
            comando = comando.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), comando);
        }
        
        String nombre = reward.getString("nombre", rewardId);
        player.sendMessage("§a§l¡Canje exitoso!");
        player.sendMessage("§7Has canjeado: " + nombre.replace("&", "§"));
        player.sendMessage("§7Tokens restantes: §e" + (getPlayerTokens(player.getUniqueId()) + countTokensInInventory(player)));
        
        return true;
    }
    
    /**
     * Muestra el menú de canje a un jugador
     */
    public void showRedeemMenu(Player player) {
        if (!config.getBoolean("canje_tokens.enabled", true)) {
            player.sendMessage("§cEl sistema de canje está deshabilitado.");
            return;
        }
        
        ConfigurationSection recompensas = config.getConfigurationSection("canje_tokens.recompensas");
        if (recompensas == null) {
            player.sendMessage("§cNo hay recompensas configuradas.");
            return;
        }
        
        int tokens = getPlayerTokens(player.getUniqueId());
        
        player.sendMessage("§6§l═══════════════════════════════");
        player.sendMessage("§6§l    CANJE DE TOKENS DE STREAM");
        player.sendMessage("§6§l═══════════════════════════════");
        player.sendMessage("");
        player.sendMessage("§7Tus tokens: §e§l" + tokens);
        player.sendMessage("");
        player.sendMessage("§eRecompensas disponibles:");
        player.sendMessage("");
        
        for (String rewardId : recompensas.getKeys(false)) {
            ConfigurationSection reward = recompensas.getConfigurationSection(rewardId);
            if (reward == null) continue;
            
            String nombre = reward.getString("nombre", rewardId);
            String descripcion = reward.getString("descripcion", "");
            int costo = reward.getInt("costo_tokens", 0);
            
            String disponible = tokens >= costo ? "§a✓" : "§c✗";
            
            player.sendMessage(disponible + " " + nombre.replace("&", "§"));
            player.sendMessage("   §7" + descripcion);
            player.sendMessage("   §7Costo: §e" + costo + " tokens");
            player.sendMessage("   §7Comando: §e/avo canjear " + rewardId);
            player.sendMessage("");
        }
        
        player.sendMessage("§6§l═══════════════════════════════");
    }
    
    /**
     * Cuenta cuántos tokens físicos tiene un jugador en su inventario
     */
    private int countTokensInInventory(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isTokenItem(item)) {
                count += item.getAmount();
            }
        }
        return count;
    }
    
    /**
     * Verifica si un item es un token de stream
     */
    private boolean isTokenItem(ItemStack item) {
        if (item == null || item.getType() != Material.NETHER_STAR) return false;
        if (!item.hasItemMeta()) return false;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        
        String name = meta.getDisplayName();
        return name.contains("Token de Stream");
    }
    
    /**
     * Quita una cantidad específica de tokens del inventario del jugador
     */
    private void removeTokensFromInventory(Player player, int amount) {
        int remaining = amount;
        
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            if (remaining <= 0) break;
            
            ItemStack item = player.getInventory().getItem(i);
            if (!isTokenItem(item)) continue;
            
            int stackAmount = item.getAmount();
            
            if (stackAmount <= remaining) {
                // Remover todo el stack
                player.getInventory().setItem(i, null);
                remaining -= stackAmount;
            } else {
                // Reducir el stack
                item.setAmount(stackAmount - remaining);
                remaining = 0;
            }
        }
    }
    
    /**
     * Detiene todas las tareas y cierra la base de datos
     */
    public void shutdown() {
        tokenDatabase.close();
        
        if (eventTaskId != -1) {
            Bukkit.getScheduler().cancelTask(eventTaskId);
        }
        if (reminderTaskId != -1) {
            Bukkit.getScheduler().cancelTask(reminderTaskId);
        }
    }
    
    // Métodos auxiliares para leer Maps
    private String getString(Map<?, ?> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    private int getInt(Map<?, ?> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
    
    private double getDouble(Map<?, ?> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }
}
