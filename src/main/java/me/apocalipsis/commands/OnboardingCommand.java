package me.apocalipsis.commands;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.tutorial.OnboardingManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Comando /misionestuto para jugadores - Ver progreso de hitos del tutorial
 * Aliases: /tuto, /hitostuto
 */
public class OnboardingCommand implements CommandExecutor {
    
    private final Apocalipsis plugin;
    
    public OnboardingCommand(Apocalipsis plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (plugin.getTutorialManager() == null || plugin.getTutorialManager().getOnboardingManager() == null) {
            player.sendMessage("§cSistema de tutorial no disponible.");
            return true;
        }
        
        OnboardingManager onboarding = plugin.getTutorialManager().getOnboardingManager();
        UUID uuid = player.getUniqueId();
        
        // Verificar si ya completó el tutorial
        if (onboarding.hasCompletedOnboarding(uuid)) {
            player.sendMessage("");
            player.sendMessage("§a§l✓ ¡Has completado el tutorial!");
            player.sendMessage("§7Ya no hay más hitos pendientes.");
            player.sendMessage("§7Usa §f/avo menu §7para explorar el servidor.");
            player.sendMessage("");
            return true;
        }
        
        // Verificar si tiene onboarding activo
        OnboardingManager.OnboardingProgress progress = onboarding.getProgress(uuid);
        if (progress == null) {
            player.sendMessage("§cNo tienes tutorial activo.");
            return true;
        }
        
        // Mostrar progreso de hitos
        player.sendMessage("");
        player.sendMessage("§6§l╔═══════════════════════════════════════╗");
        player.sendMessage("§6§l║      🎯 TUS HITOS DE TUTORIAL 🎯     ║");
        player.sendMessage("§6§l╚═══════════════════════════════════════╝");
        player.sendMessage("");
        
        // Mostrar cada hito con su estado
        showMilestone(player, progress, OnboardingManager.OnboardingMilestone.WALK_100_BLOCKS, 
            "Caminar 100 bloques", "Explora el mundo y conoce tu entorno");
        
        showMilestone(player, progress, OnboardingManager.OnboardingMilestone.CRAFT_FIRST_ITEM,
            "Craftear tu primer item", "Abre tu mesa de crafteo y crea algo");
        
        showMilestone(player, progress, OnboardingManager.OnboardingMilestone.BUILD_SHELTER,
            "Construir un refugio", "Coloca bloques para protegerte");
        
        showMilestone(player, progress, OnboardingManager.OnboardingMilestone.COMPLETE_FIRST_MISSION,
            "Completar tu primera misión", "Revisa §f/avo misiones §7y completa una");
        
        showMilestone(player, progress, OnboardingManager.OnboardingMilestone.SURVIVE_TUTORIAL_DISASTER,
            "Sobrevivir un desastre", "Mantente vivo durante el evento");
        
        player.sendMessage("");
        
        int completados = progress.getCompletedCount();
        int total = OnboardingManager.OnboardingMilestone.values().length;
        double porcentaje = (completados * 100.0) / total;
        
        player.sendMessage("§7Progreso total: §e" + completados + "§7/§f" + total + " §7(§e" + String.format("%.0f", porcentaje) + "%§7)");
        player.sendMessage("");
        player.sendMessage("§8Completa todos los hitos para desbloquear recompensas");
        player.sendMessage("§8y acceder a todas las funciones del servidor.");
        player.sendMessage("");
        
        return true;
    }
    
    private void showMilestone(Player player, OnboardingManager.OnboardingProgress progress, 
                               OnboardingManager.OnboardingMilestone milestone, String nombre, String descripcion) {
        boolean completado = progress.isCompleted(milestone);
        
        if (completado) {
            player.sendMessage("§a✓ §f" + nombre);
            player.sendMessage("  §8▪ §7" + descripcion + " §8(completado)");
        } else {
            player.sendMessage("§c✗ §7" + nombre);
            player.sendMessage("  §8▪ §e" + descripcion);
        }
        player.sendMessage("");
    }
}
