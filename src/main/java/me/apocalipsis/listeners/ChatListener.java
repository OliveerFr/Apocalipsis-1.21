package me.apocalipsis.listeners;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.missions.MissionRank;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sistema de chat mejorado con formato visual basado en rangos
 * Cada rango tiene su propio estilo de chat único y colores distintivos
 */
public class ChatListener implements Listener {
    
    private final Apocalipsis plugin;
    private final Map<UUID, Long> lastMessageTime = new HashMap<>();
    private final Map<UUID, String> lastMessage = new HashMap<>();
    
    public ChatListener(Apocalipsis plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncChatEvent event) {
        if (event.isCancelled()) return;
        
        Player player = event.getPlayer();
        // Convertir Component a String plano para procesamiento
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        UUID uuid = player.getUniqueId();
        
        FileConfiguration config = plugin.getConfigManager().getChatConfig();
        
        // Verificar si el sistema está habilitado
        if (!config.getBoolean("enabled", true)) {
            return;
        }
        
        // Cancelar el evento y rebroadcastear manualmente para evitar formato por defecto
        event.setCancelled(true);
        
        // Obtener rango del jugador
        MissionRank rank = plugin.getRankService().getRank(player);
        
        // Verificar cooldown
        double cooldown = config.getDouble("general.message_cooldown." + rank.name(), 0.0);
        if (cooldown > 0.0) {
            long currentTime = System.currentTimeMillis();
            Long lastTime = lastMessageTime.get(uuid);
            
            if (lastTime != null) {
                double elapsed = (currentTime - lastTime) / 1000.0;
                if (elapsed < cooldown) {
                    event.setCancelled(true);
                    double remaining = cooldown - elapsed;
                    String cooldownMsg = config.getString("special_messages.cooldown_message", "&cEspera %seconds%s antes de enviar otro mensaje.")
                        .replace("%seconds%", String.format("%.1f", remaining));
                    player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(cooldownMsg));
                    return;
                }
            }
            lastMessageTime.put(uuid, currentTime);
        }
        
        // Anti-spam
        if (config.getBoolean("moderation.anti_spam_enabled", true)) {
            String last = lastMessage.get(uuid);
            if (last != null && last.equalsIgnoreCase(message)) {
                event.setCancelled(true);
                player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    config.getString("moderation.spam_warning", "&cNo envíes el mismo mensaje repetidamente.")));
                return;
            }
            lastMessage.put(uuid, message);
        }
        
        // Verificar mayúsculas
        int maxCaps = config.getInt("moderation.max_caps_percentage", 70);
        if (maxCaps > 0 && message.length() > 5) {
            long capsCount = message.chars().filter(Character::isUpperCase).count();
            double capsPercentage = (capsCount * 100.0) / message.length();
            
            if (capsPercentage > maxCaps) {
                player.sendMessage(config.getString("moderation.caps_warning", "&eEvita escribir todo en MAYÚSCULAS."));
            }
        }
        
        // Limpiar colores si el jugador no tiene permiso
        boolean canUseColors = config.getBoolean("general.allow_colors." + rank.name(), false);
        if (!canUseColors && !player.hasPermission("avo.chat.colors")) {
            message = message.replaceAll("&[0-9a-fk-or]", "");
        }
        
        // Limpiar formato si el jugador no tiene permiso
        boolean canUseFormatting = config.getBoolean("general.allow_formatting." + rank.name(), false);
        if (!canUseFormatting && !player.hasPermission("avo.chat.format")) {
            message = message.replaceAll("&[l-or]", "");
        }
        
        // Obtener nivel del jugador
        int level = plugin.getExperienceService().getLevel(player);
        
        // Formatear mensaje según rango y config
        String formattedMessage = formatChatMessage(player, rank, level, message, config);
        
        // Sistema de menciones
        if (config.getBoolean("mentions.enabled", true)) {
            message = applyMentions(message, player, config);
        }
        
        // Reemplazar %1$s con el mensaje procesado
        formattedMessage = formattedMessage.replace("%1$s", message);
        
        // Enviar mensaje formateado a todos los jugadores
        Component formattedComponent = LegacyComponentSerializer.legacySection().deserialize(formattedMessage);
        for (Player recipient : plugin.getServer().getOnlinePlayers()) {
            recipient.sendMessage(formattedComponent);
        }
        
        // Log a consola (sin colores)
        plugin.getLogger().info(PlainTextComponentSerializer.plainText().serialize(formattedComponent));
    }
    
    /**
     * Formatea el mensaje de chat según el rango del jugador usando chat.yml
     */
    private String formatChatMessage(Player player, MissionRank rank, int level, String message, FileConfiguration config) {
        String rankName = rank.name();
        String basePath = "formats." + rankName + ".";
        
        // Obtener componentes del formato desde config
        String badge = config.getString(basePath + "badge", "§8[" + rankName + "§8]");
        String playerName = config.getString(basePath + "player_name", "§f%player%");
        String levelBadge = config.getString(basePath + "level_badge", "§8[§7Lv.%level%§8]");
        String separator = config.getString(basePath + "separator", "§8»");
        String messageColor = config.getString(basePath + "message_color", "§f");
        
        // Reemplazar variables
        playerName = playerName.replace("%player%", player.getName());
        levelBadge = levelBadge.replace("%level%", String.valueOf(level));
        
        // Traducir códigos de color
        badge = translateColors(badge);
        playerName = translateColors(playerName);
        levelBadge = translateColors(levelBadge);
        separator = translateColors(separator);
        messageColor = translateColors(messageColor);
        
        // 🔧 DEBUG: Log del formato generado (comentar después de testear)
        // plugin.getLogger().info("[Chat-DEBUG] Rank: " + rank.name());
        // plugin.getLogger().info("[Chat-DEBUG] Badge: " + badge);
        
        // Construir formato final con placeholder %1$s para el mensaje
        // %1$s es reemplazado por Minecraft con el mensaje del jugador
        return badge + " " + playerName + " " + levelBadge + " " + separator + " " + messageColor + "%1$s";
    }
    
    /**
     * Aplica el sistema de menciones y retorna el mensaje procesado
     */
    private String applyMentions(String message, Player sender, FileConfiguration config) {
        String mentionColor = config.getString("mentions.mention_color", "&e&l");
        
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (message.toLowerCase().contains(online.getName().toLowerCase())) {
                // Notificar al jugador mencionado
                if (!online.equals(sender)) {
                    String soundName = config.getString("mentions.mention_sound", "BLOCK_NOTE_BLOCK_PLING");
                    float volume = (float) config.getDouble("mentions.mention_volume", 0.8);
                    float pitch = (float) config.getDouble("mentions.mention_pitch", 1.5);
                    
                    try {
                        // 🔧 FIX: Usar Registry API de Paper 1.21 correctamente
                        String soundKeyString = soundName.toLowerCase().replace("_", ".");
                        if (!soundKeyString.contains(":")) {
                            soundKeyString = "minecraft:" + soundKeyString;
                        }
                        org.bukkit.NamespacedKey soundKey = org.bukkit.NamespacedKey.fromString(soundKeyString);
                        
                        if (soundKey != null) {
                            Sound sound = org.bukkit.Registry.SOUNDS.get(soundKey);
                            
                            if (sound != null) {
                                online.playSound(online.getLocation(), sound, volume, pitch);
                                // 🔧 DEBUG: Confirmar que se reproduce
                                // plugin.getLogger().info("[Mention] Sonido reproducido para " + online.getName());
                            } else {
                                plugin.getLogger().warning("[Mention] Sonido no encontrado en Registry: " + soundName);
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("[Mention] Error al reproducir sonido: " + e.getMessage());
                    }
                }
                
                // Resaltar el nombre en el mensaje
                String highlighted = translateColors(mentionColor) + online.getName() + "§r";
                message = message.replaceAll("(?i)" + online.getName(), highlighted);
            }
        }
        
        return message;
    }
    
    /**
     * Traduce códigos de color (&) a códigos de Minecraft (§)
     */
    @SuppressWarnings("deprecation")
    private String translateColors(String text) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
    }
    
    /**
     * FORMATOS DEPRECADOS - Mantenidos por compatibilidad
     * Estos métodos ya no se usan, todo se lee desde chat.yml
     */
    
    private String formatNovato(String name, int level, String message) {
        return "§8[§aNovato§8] §a" + name + " §8[§7Nv." + level + "§8] §8» §f" + message;
    }
    
    private String formatExplorador(String name, int level, String message) {
        return "§8[§b✦ Explorador§8] §b" + name + " §8[§3Nv." + level + "§8] §8» §f" + message;
    }
    
    private String formatSobreviviente(String name, int level, String message) {
        return "§8[§e⚔ Sobreviviente§8] §e" + name + " §8[§6Nv." + level + "§8] §8» §f" + message;
    }
    
    private String formatVeterano(String name, int level, String message) {
        return "§8[§6★ Veterano§8] §6" + name + " §8[§eLv." + level + "§8] §7» §f" + message;
    }
    
    private String formatLeyenda(String name, int level, String message) {
        return "§8[§c★★ §l§cLEYENDA§8] §c§l" + name + " §r§8[§cLv." + level + "§8] §c» §f" + message;
    }
    
    private String formatMaestro(String name, int level, String message) {
        return "§8[§5♛ §l§5MAESTRO§8] §5§l" + name + " §r§8[§dLv." + level + "§8] §5» §f" + message;
    }
    
    private String formatTitan(String name, int level, String message) {
        return "§8[§4✦§l§4TITÁN§r§4✦§8] §4§l" + name + " §r§8[§cLv." + level + "§8] §4» §f" + message;
    }
    
    private String formatAbsoluto(String name, int level, String message) {
        return "§8[§f§l◈ ABSOLUTO ◈§8] §f§l" + name + " §r§8[§fLv." + level + "§8] §f» §7" + message;
    }
}
