package me.apocalipsis.ciclos;

import me.apocalipsis.Apocalipsis;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Listener que protege contra comandos que puedan usarse
 * para transferir items o recursos entre mundos
 */
public class CommandProtectionListener implements Listener {
    
    private final Apocalipsis plugin;
    private final CicloManager cicloManager;
    
    // Comandos bloqueados (prefijos)
    private final Set<String> blockedCommands = new HashSet<>(Arrays.asList(
        "/give",
        "/minecraft:give",
        "/item",
        "/summon minecraft:item",
        "/clear",          // Podría usarse para eliminar y dar en otro mundo
        "/replaceitem"     // Podría usarse para intercambiar items
    ));
    
    // Comandos de economía (si tienes plugins de economía)
    private final Set<String> economyCommands = new HashSet<>(Arrays.asList(
        "/eco give",
        "/economy give",
        "/bal",
        "/balance",
        "/pay",
        "/money"
    ));
    
    public CommandProtectionListener(Apocalipsis plugin, CicloManager cicloManager) {
        this.plugin = plugin;
        this.cicloManager = cicloManager;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase();
        String currentWorld = player.getWorld().getName();
        
        // Solo aplicar protección si el jugador está en un mundo de ciclo
        if (!cicloManager.isCycleWorld(currentWorld)) {
            return;
        }
        
        // Permitir si el jugador tiene el permiso de bypass
        if (player.hasPermission("apocalipsis.ciclo.bypass")) {
            return;
        }
        
        // Verificar comandos bloqueados
        for (String blockedCmd : blockedCommands) {
            if (command.startsWith(blockedCmd)) {
                event.setCancelled(true);
                player.sendMessage("§c✖ Ese comando está bloqueado en mundos de ciclo.");
                player.sendMessage("§7Razón: Prevención de transferencia de items entre mundos.");
                
                plugin.getLogger().warning("[CommandProtection] Bloqueado comando '" + 
                    command + "' de " + player.getName() + " en mundo: " + currentWorld);
                return;
            }
        }
        
        // Verificar comandos de economía (advertencia, no bloqueo total)
        for (String ecoCmd : economyCommands) {
            if (command.startsWith(ecoCmd)) {
                player.sendMessage("§e⚠ Advertencia: Los recursos de economía pueden estar separados por mundo.");
                // No cancelar, solo advertir
                return;
            }
        }
        
        // Comandos adicionales sospechosos (lógica expandible)
        if (command.contains("shulker") && command.contains("give")) {
            event.setCancelled(true);
            player.sendMessage("§c✖ No puedes usar comandos relacionados con Shulker Boxes en ciclos.");
            return;
        }
    }
    
    /**
     * Permite agregar un comando bloqueado dinámicamente
     */
    public void addBlockedCommand(String command) {
        blockedCommands.add(command.toLowerCase());
        plugin.getLogger().info("[CommandProtection] Comando bloqueado añadido: " + command);
    }
    
    /**
     * Permite remover un comando bloqueado
     */
    public void removeBlockedCommand(String command) {
        blockedCommands.remove(command.toLowerCase());
        plugin.getLogger().info("[CommandProtection] Comando desbloqueado: " + command);
    }
    
    /**
     * Obtiene la lista de comandos bloqueados
     */
    public Set<String> getBlockedCommands() {
        return new HashSet<>(blockedCommands);
    }
}
