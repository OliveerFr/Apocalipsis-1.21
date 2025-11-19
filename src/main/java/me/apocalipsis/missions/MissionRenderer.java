package me.apocalipsis.missions;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Sistema de renderización visual avanzado para misiones.
 * Proporciona iconos, barras de progreso y colores consistentes.
 */
public class MissionRenderer {
    
    // Iconos Unicode para tipos de misión
    private static final Map<MissionType, String> MISSION_ICONS = new HashMap<>();
    
    static {
        MISSION_ICONS.put(MissionType.MATAR, "⚔");
        MISSION_ICONS.put(MissionType.ROMPER, "⛏");
        MISSION_ICONS.put(MissionType.CRAFTEAR, "🔨");
        MISSION_ICONS.put(MissionType.COCINAR, "🍳");
        MISSION_ICONS.put(MissionType.CONSUMIR, "🍖");
        MISSION_ICONS.put(MissionType.EXPLORAR, "🗺");
        MISSION_ICONS.put(MissionType.ALTURA, "⛰");
    }
    
    /**
     * Obtiene el color asociado a una dificultad
     */
    public static NamedTextColor getDifficultyColor(MissionDifficulty difficulty) {
        switch (difficulty) {
            case FACIL:
                return NamedTextColor.GREEN;
            case MEDIA:
                return NamedTextColor.YELLOW;
            case DIFICIL:
                return NamedTextColor.RED;
            default:
                return NamedTextColor.GRAY;
        }
    }
    
    /**
     * Genera una barra de progreso visual usando bloques Unicode
     * @param current Progreso actual
     * @param target Objetivo total
     * @param length Longitud de la barra (número de bloques)
     * @return String con la barra de progreso (▰▰▰▱▱)
     */
    public static String buildProgressBar(int current, int target, int length) {
        if (target <= 0) return "▱".repeat(length);
        
        double percentage = Math.min(1.0, (double) current / target);
        int filled = (int) Math.round(percentage * length);
        int empty = length - filled;
        
        return "▰".repeat(filled) + "▱".repeat(empty);
    }
    
    /**
     * Obtiene el icono de una misión según su tipo
     */
    public static String getMissionIcon(MissionType type) {
        return MISSION_ICONS.getOrDefault(type, "🎯");
    }
    
    /**
     * Renderiza una misión completa con formato visual mejorado
     * @return Component con la misión formateada
     */
    public static Component renderMission(MissionAssignment assignment) {
        if (assignment == null) {
            return Component.text("Sin misión activa", NamedTextColor.GRAY, TextDecoration.ITALIC);
        }
        
        MissionCatalog mission = assignment.getMission();
        MissionDifficulty difficulty = mission.getDificultad();
        NamedTextColor color = getDifficultyColor(difficulty);
        String icon = getMissionIcon(mission.getTipo());
        int target = mission.getCantidad();
        String progressBar = buildProgressBar(assignment.getProgress(), target, 10);
        double percentage = target > 0 
            ? (double) assignment.getProgress() / target * 100 
            : 0;
        
        return Component.text()
            .append(Component.text(icon + " ", NamedTextColor.WHITE))
            .append(Component.text(mission.getTipo().name(), color, TextDecoration.BOLD))
            .append(Component.text(" » ", NamedTextColor.DARK_GRAY))
            .append(Component.text(mission.getObjetivo(), NamedTextColor.GRAY))
            .append(Component.newline())
            .append(Component.text("  ", NamedTextColor.WHITE))
            .append(Component.text(progressBar, color))
            .append(Component.text(" ", NamedTextColor.WHITE))
            .append(Component.text(String.format("%d/%d", assignment.getProgress(), target), NamedTextColor.WHITE))
            .append(Component.text(String.format(" (%.1f%%)", percentage), NamedTextColor.GRAY))
            .build();
    }
    
    /**
     * Renderiza una misión en formato compacto (una línea)
     */
    public static Component renderMissionCompact(MissionAssignment assignment) {
        if (assignment == null) {
            return Component.text("---", NamedTextColor.GRAY);
        }
        
        MissionCatalog mission = assignment.getMission();
        NamedTextColor color = getDifficultyColor(mission.getDificultad());
        String icon = getMissionIcon(mission.getTipo());
        
        return Component.text()
            .append(Component.text(icon, NamedTextColor.WHITE))
            .append(Component.text(" ", NamedTextColor.WHITE))
            .append(Component.text(mission.getObjetivo(), color))
            .append(Component.text(" ", NamedTextColor.WHITE))
            .append(Component.text(String.format("%d/%d", assignment.getProgress(), mission.getCantidad()), NamedTextColor.GRAY))
            .build();
    }
    
    /**
     * Muestra mensaje de misión completada con animación
     */
    public static void showMissionComplete(Player player, MissionAssignment assignment, int xpAwarded) {
        MissionCatalog mission = assignment.getMission();
        String icon = getMissionIcon(mission.getTipo());
        NamedTextColor color = getDifficultyColor(mission.getDificultad());
        
        Component title = Component.text()
            .append(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD))
            .append(Component.text("MISIÓN COMPLETADA", NamedTextColor.GOLD, TextDecoration.BOLD))
            .build();
        
        Component subtitle = Component.text()
            .append(Component.text(icon + " ", NamedTextColor.WHITE))
            .append(Component.text(mission.getObjetivo(), color))
            .append(Component.text(" +" + xpAwarded + " XP", NamedTextColor.GREEN))
            .build();
        
        player.sendMessage(Component.text("▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬", NamedTextColor.DARK_GRAY, TextDecoration.STRIKETHROUGH));
        player.sendMessage(title);
        player.sendMessage(subtitle);
        player.sendMessage(Component.text("▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬", NamedTextColor.DARK_GRAY, TextDecoration.STRIKETHROUGH));
    }
    
    /**
     * Obtiene el color de progreso según porcentaje
     */
    public static NamedTextColor getProgressColor(double percentage) {
        if (percentage >= 75.0) return NamedTextColor.GREEN;
        if (percentage >= 50.0) return NamedTextColor.YELLOW;
        if (percentage >= 25.0) return NamedTextColor.GOLD;
        return NamedTextColor.RED;
    }
}
