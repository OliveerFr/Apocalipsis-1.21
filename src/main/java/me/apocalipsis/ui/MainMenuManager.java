/*
 * Apocalipsis Plugin - Menú Principal
 * Menú centralizado con acceso a todas las funciones del jugador
 */
package me.apocalipsis.ui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.missions.MissionAssignment;
import me.apocalipsis.missions.MissionRank;

import java.util.*;

/**
 * Sistema de menú principal para jugadores.
 * Acceso centralizado a todas las funciones del plugin.
 * Comando: /avo menu
 */
public class MainMenuManager implements Listener {
    
    private final Apocalipsis plugin;
    private final MessageBus messageBus;
    
    // Título del menú principal
    private static final String MENU_TITLE = "§5§l✦ §dMenú Apocalipsis §5§l✦";
    
    public MainMenuManager(Apocalipsis plugin) {
        this.plugin = plugin;
        this.messageBus = plugin.getMessageBus();
        
        // Registrar listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        plugin.getLogger().info("[MainMenuManager] ✓ Sistema de menú principal iniciado");
    }
    
    /**
     * Abre el menú principal para un jugador
     */
    public void openMainMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 54, MENU_TITLE);
        
        // ═══════════════════════════════════════════════════════════════
        // DECORACIÓN SUPERIOR E INFERIOR
        // ═══════════════════════════════════════════════════════════════
        ItemStack border = createBorderItem(Material.PURPLE_STAINED_GLASS_PANE);
        ItemStack corner = createBorderItem(Material.BLACK_STAINED_GLASS_PANE);
        
        // Esquinas
        menu.setItem(0, corner);
        menu.setItem(8, corner);
        menu.setItem(45, corner);
        menu.setItem(53, corner);
        
        // Bordes superiores e inferiores
        for (int i = 1; i < 8; i++) {
            menu.setItem(i, border);
            menu.setItem(45 + i, border);
        }
        
        // Bordes laterales
        for (int i = 9; i < 45; i += 9) {
            menu.setItem(i, border);
            menu.setItem(i + 8, border);
        }
        
        // ═══════════════════════════════════════════════════════════════
        // CABEZA DEL JUGADOR CON INFO + BARRA DE PROGRESO
        // ═══════════════════════════════════════════════════════════════
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(player);
            
            // Obtener día actual del ciclo
            int diaActual = plugin.getStateManager().getCurrentDay();
            skullMeta.setDisplayName("§e§l" + player.getName() + " §8| §5Día " + diaActual);
            
            // Obtener info del jugador
            MissionRank rank = plugin.getRankService().getRank(player);
            int xp = plugin.getExperienceService().getXP(player);
            int ps = plugin.getMissionService().getPlayerPs(player);
            int level = plugin.getExperienceService().getLevel(player);
            
            // Calcular barra de progreso hacia siguiente rango
            MissionRank nextRank = getNextRank(rank);
            String progressBar = "";
            String progressText = "";
            
            if (nextRank != null) {
                int currentXP = xp;
                int currentRankXP = rank.getXpRequired();
                int nextRankXP = nextRank.getXpRequired();
                int xpInRange = currentXP - currentRankXP;
                int xpNeeded = nextRankXP - currentRankXP;
                float percentage = (float) xpInRange / xpNeeded;
                percentage = Math.max(0, Math.min(1, percentage)); // Clamp 0-1
                
                // Crear barra visual de 20 caracteres
                int filledBars = (int) (percentage * 20);
                StringBuilder bar = new StringBuilder("§a");
                for (int i = 0; i < 20; i++) {
                    if (i < filledBars) {
                        bar.append("▌");
                    } else {
                        if (i == filledBars) bar.append("§7");
                        bar.append("▌");
                    }
                }
                progressBar = bar.toString();
                int xpFaltante = nextRankXP - currentXP;
                progressText = "§7Próximo: " + nextRank.getDisplayName() + " §8(§e" + xpFaltante + " XP§8)";
            } else {
                progressBar = "§6▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌";
                progressText = "§6§l¡RANGO MÁXIMO ALCANZADO!";
            }
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Rango: " + rank.getDisplayName());
            lore.add("§7Nivel: §f" + level);
            lore.add("§7XP: §a" + xp);
            lore.add("§7PS: §e" + ps);
            lore.add("");
            lore.add("§7Progreso: " + progressBar);
            lore.add(progressText);
            lore.add("");
            lore.add("§8▸ Tu progreso en el Apocalipsis");
            
            skullMeta.setLore(lore);
            skull.setItemMeta(skullMeta);
        }
        menu.setItem(4, skull);
        
        // ═══════════════════════════════════════════════════════════════
        // OPCIONES DEL MENÚ
        // ═══════════════════════════════════════════════════════════════
        
        // Slot 19: MISIONES
        int missionsActive = getMissionCount(player, false);
        int missionsCompleted = getMissionCount(player, true);
        int totalMissions = missionsActive + missionsCompleted;
        
        // Indicador de misiones faltantes (si no tiene ninguna)
        Material missionMaterial = Material.WRITABLE_BOOK;
        String missionWarning = "";
        if (totalMissions == 0) {
            missionMaterial = Material.BARRIER;
            missionWarning = "§c§l¡SIN MISIONES!";
        } else if (missionsCompleted == totalMissions && totalMissions > 0) {
            missionMaterial = Material.ENCHANTED_BOOK;
            missionWarning = "§a§l¡TODAS COMPLETADAS!";
        }
        
        List<String> missionLore = new ArrayList<>();
        missionLore.add("");
        if (!missionWarning.isEmpty()) {
            missionLore.add(missionWarning);
            missionLore.add("");
        }
        missionLore.add("§7Estado: §f" + missionsCompleted + "/" + totalMissions + " §7completadas");
        missionLore.add("");
        if (totalMissions == 0) {
            missionLore.add("§c¡No tienes misiones asignadas!");
            missionLore.add("§7Espera a que inicie un nuevo día");
            missionLore.add("§7o contacta a un administrador.");
        } else {
            missionLore.add("§7Mira tus misiones asignadas");
            missionLore.add("§7y tu progreso actual.");
        }
        missionLore.add("");
        missionLore.add("§e▶ Click para ver misiones");
        
        menu.setItem(19, createMenuItem(
            missionMaterial,
            "§a§l📋 Misiones Diarias",
            missionLore,
            "MISIONES"
        ));
        
        // Slot 21: RECOMPENSAS
        int pendingRewards = getPendingRewardsCount(player);
        Material rewardMaterial = pendingRewards > 0 ? Material.ENDER_CHEST : Material.CHEST;
        String rewardStatus = pendingRewards > 0 ? "§a" + pendingRewards + " pendientes" : "§7Ninguna pendiente";
        menu.setItem(21, createMenuItem(
            rewardMaterial,
            "§6§l🎁 Recompensas",
            Arrays.asList(
                "",
                "§7Estado: " + rewardStatus,
                "",
                "§7Reclama tus recompensas de",
                "§7eventos y subidas de rango.",
                "",
                "§e▶ Click para reclamar"
            ),
            "RECOMPENSAS"
        ));
        
        // Slot 23: ESTADÍSTICAS
        menu.setItem(23, createMenuItem(
            Material.EXPERIENCE_BOTTLE,
            "§b§l📊 Estadísticas",
            Arrays.asList(
                "",
                "§7Ver tu progreso detallado:",
                "§8• XP ganado",
                "§8• Misiones completadas",
                "§8• Desastres sobrevividos",
                "§8• Eventos participados",
                "",
                "§e▶ Click para ver stats"
            ),
            "ESTADISTICAS"
        ));
        
        // Slot 25: PROTECCIONES
        menu.setItem(25, createMenuItem(
            Material.SHIELD,
            "§e§l🛡 Guía de Protecciones",
            Arrays.asList(
                "",
                "§7Aprende a protegerte de:",
                "§c• Terremotos",
                "§6• Lluvia de Fuego",
                "§9• Huracanes",
                "",
                "§7¡Sobrevive al apocalipsis!",
                "",
                "§e▶ Click para ver guía"
            ),
            "PROTECCIONES"
        ));
        
        // Slot 28: RANGOS
        menu.setItem(28, createMenuItem(
            Material.NETHER_STAR,
            "§d§l⭐ Sistema de Rangos",
            Arrays.asList(
                "",
                "§7Tu rango actual: " + plugin.getRankService().getRank(player).getDisplayName(),
                "",
                "§7Ver todos los rangos y",
                "§7sus recompensas.",
                "",
                "§e▶ Click para ver rangos"
            ),
            "RANGOS"
        ));
        
        // Slot 30: SKILLS (Árbol de habilidades)
        menu.setItem(30, createMenuItem(
            Material.ENCHANTING_TABLE,
            "§5§l✧ Habilidades",
            Arrays.asList(
                "",
                "§7Desbloquea habilidades",
                "§7especiales con tus PS.",
                "",
                "§7Gana PS completando misiones.",
                "",
                "§e▶ Click para ver árbol"
            ),
            "SKILLS"
        ));
        
        // Slot 32: EVASIONES
        int evasiones = getEvasionesCount(player);
        menu.setItem(32, createMenuItem(
            Material.FEATHER,
            "§c§l⚡ Evasiones",
            Arrays.asList(
                "",
                "§7Evasiones disponibles: §e" + evasiones,
                "",
                "§7Usa evasiones para escapar",
                "§7de desastres peligrosos.",
                "",
                "§e▶ Click para más info"
            ),
            "EVASIONES"
        ));
        
        // Slot 33: ESTADO STREAMER (NUEVO)
        boolean streamerOnline = isStreamerOnline();
        String estadoStreamer = streamerOnline ? "§a§l✓ ONLINE" : "§c§l✗ OFFLINE";
        String xpMultiplier = streamerOnline ? "§aXP Normal (x1.0)" : "§cXP Reducido (x0.3)";
        Material streamerIcon = streamerOnline ? Material.LIME_DYE : Material.GRAY_DYE;
        
        List<String> streamerLore = new ArrayList<>();
        streamerLore.add("");
        streamerLore.add("§7Estado: " + estadoStreamer);
        streamerLore.add("§7Multiplicador: " + xpMultiplier);
        streamerLore.add("");
        if (streamerOnline) {
            streamerLore.add("§a¡El streamer está conectado!");
            streamerLore.add("§7Aprovecha para ganar XP normal");
            streamerLore.add("§7y participar en eventos exclusivos.");
        } else {
            streamerLore.add("§7El streamer no está conectado.");
            streamerLore.add("§7XP reducido al 30% del normal.");
            streamerLore.add("§e¡Vuelve cuando haya stream!");
        }
        streamerLore.add("");
        streamerLore.add("§8▸ Sistema de presencia");
        
        menu.setItem(33, createMenuItem(
            streamerIcon,
            "§6§l🎮 Estado del Stream",
            streamerLore,
            "STREAM_STATUS"
        ));
        
        // Slot 34: EVENTOS
        String estadoEvento = plugin.getEventController().hasActiveEvent() ? "§a¡ACTIVO!" : "§7Ninguno activo";
        menu.setItem(34, createMenuItem(
            Material.ENDER_EYE,
            "§9§l🌟 Eventos",
            Arrays.asList(
                "",
                "§7Estado: " + estadoEvento,
                "",
                "§7Participa en eventos",
                "§7especiales y gana",
                "§7recompensas únicas.",
                "",
                "§e▶ Click para más info"
            ),
            "EVENTOS"
        ));
        
        // Slot 40: AYUDA
        menu.setItem(40, createMenuItem(
            Material.BOOK,
            "§f§l❓ Ayuda",
            Arrays.asList(
                "",
                "§7Guía completa del plugin",
                "§7y lista de comandos.",
                "",
                "§7¿Primera vez? ¡Empieza aquí!",
                "",
                "§e▶ Click para ver ayuda"
            ),
            "AYUDA"
        ));
        
        // ═══════════════════════════════════════════════════════════════
        // ACCESO RÁPIDO - PARTE INFERIOR
        // ═══════════════════════════════════════════════════════════════
        
        // Slot 46: TOKENS DE STREAM
        int tokensCount = getStreamTokens(player);
        Material tokenMaterial = tokensCount > 0 ? Material.NETHER_STAR : Material.GHAST_TEAR;
        String tokenStatus = tokensCount > 0 ? "§e" + tokensCount + " disponibles" : "§7Ninguno";
        
        List<String> tokenLore = new ArrayList<>();
        tokenLore.add("");
        tokenLore.add("§7Tokens: " + tokenStatus);
        tokenLore.add("");
        if (streamerOnline) {
            tokenLore.add("§a¡Dropean de mobs durante stream!");
            tokenLore.add("§75% chance por mob hostil");
            tokenLore.add("§7Canjéalos por recompensas épicas.");
        } else {
            tokenLore.add("§7Solo dropean cuando el");
            tokenLore.add("§7streamer está online.");
            tokenLore.add("§e¡Vuelve durante el stream!");
        }
        tokenLore.add("");
        tokenLore.add("§e▶ Click para ver tienda");
        
        menu.setItem(46, createMenuItem(
            tokenMaterial,
            "§6§l⭐ Tokens de Stream",
            tokenLore,
            "STREAM_TOKENS"
        ));
        
        // Slot 47: CANJE DE TOKENS
        menu.setItem(47, createMenuItem(
            Material.EMERALD,
            "§a§l💰 Canjear Tokens",
            Arrays.asList(
                "",
                "§7Intercambia tokens por:",
                "§8• Kit Diamante (5 tokens)",
                "§8• Kit Netherite (15 tokens)",
                "§8• Élitro + Cohetes (10 tokens)",
                "§8• Bloques protección (8 tokens)",
                "§8• Mega Pack Épico (25 tokens)",
                "",
                "§7Tus tokens: §e" + tokensCount,
                "",
                "§e▶ Click para canjear"
            ),
            "CANJEAR_TOKENS"
        ));
        
        // Slot 48: RANKING DE STREAM
        int rankingPosition = getStreamRanking(player);
        String rankingText = rankingPosition > 0 ? "§e#" + rankingPosition : "§7No clasificado";
        Material rankingMaterial = rankingPosition <= 3 ? Material.GOLD_INGOT : Material.IRON_INGOT;
        
        List<String> rankingLore = new ArrayList<>();
        rankingLore.add("");
        rankingLore.add("§7Tu posición: " + rankingText);
        rankingLore.add("");
        rankingLore.add("§7Ranking de jugadores más");
        rankingLore.add("§7activos durante streams.");
        rankingLore.add("");
        rankingLore.add("§6Premios semanales:");
        rankingLore.add("§8• Top 1: §63 Bloques Netherite");
        rankingLore.add("§8• Top 2: §62 Bloques Netherite");
        rankingLore.add("§8• Top 3: §61 Bloque Netherite");
        rankingLore.add("");
        rankingLore.add("§e▶ Click para ver top 10");
        
        menu.setItem(48, createMenuItem(
            rankingMaterial,
            "§6§l🏆 Ranking de Stream",
            rankingLore,
            "STREAM_RANKING"
        ));
        
        // Slot 49: TUTORIAL (para nuevos jugadores)
        boolean hasReachedGlobal = plugin.getProgressiveDifficultySystem().hasReachedGlobalDifficulty(player);
        Material tutorialMaterial = hasReachedGlobal ? Material.WRITTEN_BOOK : Material.ENCHANTED_BOOK;
        String tutorialTitle = hasReachedGlobal ? "§7§l📖 Tutorial" : "§a§l📖 Tutorial §8(§e¡Nuevo!§8)";
        
        long playedMinutes = plugin.getProgressiveDifficultySystem().getPlayedTimeMinutes(player);
        long remainingMinutes = plugin.getProgressiveDifficultySystem().getRemainingTimeToNextPhase(player);
        String timeFormatted = plugin.getProgressiveDifficultySystem().formatRemainingTime(remainingMinutes);
        int difficulty = plugin.getProgressiveDifficultySystem().getPlayerPhase(player).getPercentDifficulty();
        
        List<String> tutorialLore = new ArrayList<>();
        tutorialLore.add("");
        tutorialLore.add("§7Guía para nuevos jugadores");
        tutorialLore.add("");
        if (hasReachedGlobal) {
            tutorialLore.add("§a✓ Dificultad Global alcanzada");
            tutorialLore.add("§7Puedes revisar el tutorial");
            tutorialLore.add("§7en cualquier momento.");
        } else {
            tutorialLore.add("§7Dificultad actual: §c" + difficulty + "%");
            tutorialLore.add("§7Tiempo jugado: §e" + (playedMinutes) + " min");
            tutorialLore.add("§7Próxima fase en: §e" + timeFormatted);
            tutorialLore.add("");
            tutorialLore.add("§e¡Aprende a sobrevivir!");
        }
        tutorialLore.add("");
        tutorialLore.add("§e▶ Click para ver tutorial");
        
        menu.setItem(49, createMenuItem(
            tutorialMaterial,
            tutorialTitle,
            tutorialLore,
            "TUTORIAL"
        ));
        
        // Slot 50: MISIONES DE STREAM (solo visible si streamer online)
        if (streamerOnline) {
            menu.setItem(50, createMenuItem(
                Material.WRITABLE_BOOK,
                "§d§l✨ Misiones Exclusivas",
                Arrays.asList(
                    "",
                    "§a¡El streamer está online!",
                    "",
                    "§7Misiones exclusivas disponibles:",
                    "§8• Caza Épica (+200 XP base)",
                    "§8• Minería Masiva (+150 XP base)",
                    "§8• Constructor Veloz (+180 XP base)",
                    "",
                    "§d¡Recompensas dobles!",
                    "",
                    "§e▶ Click para ver misiones"
                ),
                "MISIONES_STREAM"
            ));
        }
        
        // Slot 51: AYUDA
        menu.setItem(51, createMenuItem(
            Material.BOOK,
            "§f§l❓ Ayuda",
            Arrays.asList(
                "",
                "§7Guía completa del plugin",
                "§7y lista de comandos.",
                "",
                "§7¿Primera vez? ¡Empieza aquí!",
                "",
                "§e▶ Click para ver ayuda"
            ),
            "AYUDA"
        ));
        
        // Slot 52: ENDERCHEST
        menu.setItem(52, createMenuItem(
            Material.ENDER_CHEST,
            "§5§l📦 Ender Chest",
            Arrays.asList(
                "",
                "§7Accede a tu cofre del end",
                "§7desde cualquier lugar.",
                "",
                "§e▶ Click para abrir"
            ),
            "ENDERCHEST"
        ));
        
        player.openInventory(menu);
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.8f, 1.2f);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // LISTENER DE CLICKS
    // ═══════════════════════════════════════════════════════════════════
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        String title = event.getView().getTitle();
        if (!title.equals(MENU_TITLE)) return;
        
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (!clicked.hasItemMeta() || !clicked.getItemMeta().hasLore()) return;
        
        // Buscar ID del item
        List<String> lore = clicked.getItemMeta().getLore();
        if (lore == null) return;
        
        for (String line : lore) {
            if (line.startsWith("§8ID:")) {
                String id = line.replace("§8ID:", "").trim();
                handleMenuClick(player, id);
                return;
            }
        }
    }
    
    /**
     * Maneja el click en una opción del menú
     */
    private void handleMenuClick(Player player, String id) {
        player.closeInventory();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        
        // Pequeño delay para evitar problemas
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            switch (id) {
                case "MISIONES":
                    // Mostrar misiones en chat
                    player.performCommand("avo status mission");
                    break;
                    
                case "RECOMPENSAS":
                    // Abrir menú de recompensas
                    if (plugin.getRewardClaimSystem() != null) {
                        plugin.getRewardClaimSystem().openRewardsMenu(player);
                    } else {
                        player.sendMessage("§cEl sistema de recompensas no está disponible.");
                    }
                    break;
                    
                case "ESTADISTICAS":
                    // Mostrar stats
                    player.performCommand("avo stats");
                    break;
                    
                case "PROTECCIONES":
                    // Mostrar guía de protecciones
                    player.performCommand("avo protecciones");
                    break;
                    
                case "RANGOS":
                    // Mostrar info de rangos
                    showRanksInfo(player);
                    break;
                    
                case "SKILLS":
                    // Abrir árbol de habilidades
                    if (plugin.getSkillTreeGUI() != null) {
                        plugin.getSkillTreeGUI().openMainMenu(player);
                    } else {
                        player.sendMessage("§cEl árbol de habilidades no está disponible.");
                    }
                    break;
                    
                case "EVASIONES":
                    // Mostrar info de evasiones
                    player.performCommand("avo evasion");
                    break;
                    
                case "EVENTOS":
                    // Mostrar info de eventos
                    showEventsInfo(player);
                    break;
                    
                case "AYUDA":
                    // Mostrar ayuda
                    showHelpInfo(player);
                    break;
                    
                case "MOCHILA":
                    // Abrir mochila (comando genérico, ajustar según plugin)
                    player.performCommand("backpack");
                    break;
                    
                case "TUTORIAL":
                    // Abrir menú de tutorial
                    showTutorialMenu(player);
                    break;
                    
                case "STREAM_TOKENS":
                    // Mostrar info de tokens (por ahora mensaje)
                    int tokens = getStreamTokens(player);
                    player.sendMessage("§6§l⭐ TOKENS DE STREAM");
                    player.sendMessage("");
                    player.sendMessage("§7Tokens actuales: §e" + tokens);
                    player.sendMessage("");
                    player.sendMessage("§7Los tokens dropean de mobs hostiles");
                    player.sendMessage("§7cuando el streamer está online.");
                    player.sendMessage("");
                    player.sendMessage("§7Canjéalos usando el slot de");
                    player.sendMessage("§a💰 Canjear Tokens §7en el menú.");
                    break;
                    
                case "CANJEAR_TOKENS":
                    // Abrir menú de canje (por ahora mensaje)
                    int playerTokens = getStreamTokens(player);
                    player.sendMessage("§a§l💰 CANJE DE TOKENS");
                    player.sendMessage("");
                    player.sendMessage("§7Tus tokens: §e" + playerTokens);
                    player.sendMessage("");
                    player.sendMessage("§7Recompensas disponibles:");
                    player.sendMessage("§8• §eKit Diamante §8- §65 tokens");
                    player.sendMessage("§8• §eÉlitro + Cohetes §8- §610 tokens");
                    player.sendMessage("§8• §eBloques Protección §8- §68 tokens");
                    player.sendMessage("§8• §eKit Netherite §8- §615 tokens");
                    player.sendMessage("§8• §eMega Pack Épico §8- §625 tokens");
                    player.sendMessage("");
                    player.sendMessage("§c(Sistema en desarrollo)");
                    break;
                    
                case "STREAM_RANKING":
                    // Mostrar ranking (por ahora mensaje)
                    int position = getStreamRanking(player);
                    player.sendMessage("§6§l🏆 RANKING DE STREAM");
                    player.sendMessage("");
                    player.sendMessage("§7Tu posición: " + (position > 0 ? "§e#" + position : "§7No clasificado"));
                    player.sendMessage("");
                    player.sendMessage("§7Ranking de jugadores más activos");
                    player.sendMessage("§7durante los streams del servidor.");
                    player.sendMessage("");
                    player.sendMessage("§6Premios semanales:");
                    player.sendMessage("§8• §eTop 1: §63 Bloques Netherite");
                    player.sendMessage("§8• §eTop 2: §62 Bloques Netherite");
                    player.sendMessage("§8• §eTop 3: §61 Bloque Netherite");
                    player.sendMessage("");
                    player.sendMessage("§c(Sistema en desarrollo)");
                    break;
                    
                case "MISIONES_STREAM":
                    // Mostrar misiones exclusivas
                    player.sendMessage("§d§l✨ MISIONES EXCLUSIVAS");
                    player.sendMessage("");
                    player.sendMessage("§a¡El streamer está online!");
                    player.sendMessage("");
                    player.sendMessage("§7Misiones exclusivas disponibles:");
                    player.sendMessage("§8• §eCaza Épica §8- §6+200 XP base");
                    player.sendMessage("§8• §eMinería Masiva §8- §6+150 XP base");
                    player.sendMessage("§8• §eConstructor Veloz §8- §6+180 XP base");
                    player.sendMessage("");
                    player.sendMessage("§d¡Recompensas dobles durante stream!");
                    player.sendMessage("");
                    player.sendMessage("§c(Sistema en desarrollo)");
                    break;
                    
                case "AYUDA":
                    // Mostrar ayuda
                    showHelpInfo(player);
                    break;
                    
                case "ENDERCHEST":
                    // Abrir enderchest del jugador
                    player.openInventory(player.getEnderChest());
                    break;
                    
                default:
                    player.sendMessage("§cOpción no implementada: " + id);
            }
        }, 2L);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // MENSAJES DE INFORMACIÓN
    // ═══════════════════════════════════════════════════════════════════
    
    private void showRanksInfo(Player player) {
        MissionRank currentRank = plugin.getRankService().getRank(player);
        int currentXP = plugin.getExperienceService().getXP(player);
        
        player.sendMessage("");
        player.sendMessage("§5§l═══════ SISTEMA DE RANGOS ═══════");
        player.sendMessage("");
        player.sendMessage("§7Tu rango actual: " + currentRank.getDisplayName());
        player.sendMessage("§7Tu XP: §a" + currentXP);
        player.sendMessage("");
        player.sendMessage("§7§lRangos disponibles:");
        
        for (MissionRank rank : MissionRank.values()) {
            String marker = rank == currentRank ? " §a◄ TÚ" : "";
            String unlocked = currentXP >= rank.getXpRequired() ? "§a✓" : "§c✗";
            player.sendMessage("  " + unlocked + " " + rank.getDisplayName() + " §8- " + rank.getXpRequired() + " XP" + marker);
        }
        
        // Mostrar siguiente rango
        MissionRank nextRank = getNextRank(currentRank);
        if (nextRank != null) {
            int needed = nextRank.getXpRequired() - currentXP;
            player.sendMessage("");
            player.sendMessage("§7Siguiente: " + nextRank.getDisplayName() + " §7(§e" + needed + " XP §7faltantes)");
        } else {
            player.sendMessage("");
            player.sendMessage("§6§l¡Has alcanzado el rango máximo!");
        }
        
        player.sendMessage("");
        player.sendMessage("§5§l══════════════════════════════════");
        player.sendMessage("");
    }
    
    private void showEventsInfo(Player player) {
        player.sendMessage("");
        player.sendMessage("§9§l═══════ EVENTOS ESPECIALES ═══════");
        player.sendMessage("");
        
        if (plugin.getEventController().hasActiveEvent()) {
            player.sendMessage("§a§l¡EVENTO ACTIVO!");
            player.sendMessage("§7Participa para ganar recompensas únicas.");
        } else {
            player.sendMessage("§7No hay eventos activos actualmente.");
            player.sendMessage("§7Los eventos se anuncian automáticamente.");
        }
        
        player.sendMessage("");
        player.sendMessage("§7§lTipos de eventos:");
        player.sendMessage("  §c• Eco de Brasas §8- Evento de fuego");
        player.sendMessage("  §5• Eco de Sombras §8- Evento de oscuridad");
        player.sendMessage("  §9• Susurro de Piedra §8- Evento de minería");
        player.sendMessage("");
        player.sendMessage("§7¡Estate atento a los anuncios!");
        player.sendMessage("");
        player.sendMessage("§9§l═══════════════════════════════════");
        player.sendMessage("");
    }
    
    private void showHelpInfo(Player player) {
        player.sendMessage("");
        player.sendMessage("§d§l═══════ AYUDA APOCALIPSIS ═══════");
        player.sendMessage("");
        player.sendMessage("§e§lComandos principales:");
        player.sendMessage("  §a/avo menu §8- Abre este menú");
        player.sendMessage("  §a/recompensa §8- Reclama recompensas");
        player.sendMessage("  §a/avo status §8- Ver tu estado");
        player.sendMessage("  §a/avo stats §8- Ver estadísticas");
        player.sendMessage("  §a/avo protecciones §8- Guía de protecciones");
        player.sendMessage("  §a/avo evasion §8- Info de evasiones");
        player.sendMessage("");
        player.sendMessage("§e§l¿Cómo subir de rango?");
        player.sendMessage("  §7• Completa tus §emisiones diarias§7");
        player.sendMessage("  §7• Sobrevive a los §cdesastres§7");
        player.sendMessage("  §7• Participa en §9eventos§7");
        player.sendMessage("");
        player.sendMessage("§e§l¿Qué son los PS?");
        player.sendMessage("  §7Puntos de Supervivencia que ganas");
        player.sendMessage("  §7completando misiones. Úsalos para");
        player.sendMessage("  §7desbloquear §5habilidades§7.");
        player.sendMessage("");
        player.sendMessage("§d§l══════════════════════════════════");
        player.sendMessage("");
    }
        player.sendMessage("  §a/recompensa §8- Reclama recompensas");
        player.sendMessage("  §a/avo status §8- Ver tu estado");
        player.sendMessage("  §a/avo stats §8- Ver estadísticas");
        player.sendMessage("  §a/avo protecciones §8- Guía de protecciones");
        player.sendMessage("  §a/avo evasion §8- Info de evasiones");
        player.sendMessage("");
        player.sendMessage("§e§l¿Cómo subir de rango?");
        player.sendMessage("  §7• Completa tus §emisiones diarias§7");
        player.sendMessage("  §7• Sobrevive a los §cdesastres§7");
        player.sendMessage("  §7• Participa en §9eventos§7");
        player.sendMessage("");
        player.sendMessage("§e§l¿Qué son los PS?");
        player.sendMessage("  §7Puntos de Supervivencia que ganas");
        player.sendMessage("  §7completando misiones. Úsalos para");
        player.sendMessage("  §7desbloquear §5habilidades§7.");
        player.sendMessage("");
        player.sendMessage("§d§l══════════════════════════════════");
        player.sendMessage("");
    }
    
    /**
     * Muestra el menú de tutorial para nuevos jugadores
     */
    private void showTutorialMenu(Player player) {
        boolean hasReachedGlobal = plugin.getProgressiveDifficultySystem().hasReachedGlobalDifficulty(player);
        long playedMinutes = plugin.getProgressiveDifficultySystem().getPlayedTimeMinutes(player);
        long remainingMinutes = plugin.getProgressiveDifficultySystem().getRemainingTimeToNextPhase(player);
        String timeFormatted = plugin.getProgressiveDifficultySystem().formatRemainingTime(remainingMinutes);
        int difficulty = plugin.getProgressiveDifficultySystem().getPlayerPhase(player).getPercentDifficulty();
        
        player.sendMessage("");
        player.sendMessage("§6§l═════════ TUTORIAL APOCALIPSIS ═════════");
        player.sendMessage("");
        
        if (!hasReachedGlobal) {
            player.sendMessage("§e§l📊 TU PROGRESO DE ADAPTACIÓN:");
            player.sendMessage("  §7Dificultad actual: §c" + difficulty + "%");
            player.sendMessage("  §7Tiempo jugado: §e" + playedMinutes + " minutos");
            player.sendMessage("  §7Próxima fase en: §e" + timeFormatted);
            player.sendMessage("");
            player.sendMessage("§7Los desastres empiezan §asuaves §7y aumentan");
            player.sendMessage("§7gradualmente durante tus primeras §e4 horas§7.");
            player.sendMessage("");
        } else {
            player.sendMessage("§a§l✓ Has alcanzado la dificultad global");
            player.sendMessage("§7Ya no tienes protección de nuevos.");
            player.sendMessage("");
        }
        
        player.sendMessage("§e§l🌋 SOBRE LOS DESASTRES:");
        player.sendMessage("  §c• Terremoto §8- Rompe bloques cercanos");
        player.sendMessage("    §7→ Evasión: Aléjate del epicentro");
        player.sendMessage("  §6• Lluvia de Fuego §8- Incendia todo");
        player.sendMessage("    §7→ Evasión: Usa agua o corre");
        player.sendMessage("  §9• Huracán §8- Empuja y lanza items");
        player.sendMessage("    §7→ Evasión: Agáchate o escóndete");
        player.sendMessage("");
        
        player.sendMessage("§e§l📋 PRIORIDADES DE SUPERVIVENCIA:");
        player.sendMessage("  §a1. §7Construye un §erefugio subterráneo");
        player.sendMessage("  §a2. §7Completa §emisiones diarias §7(/misiones)");
        player.sendMessage("  §a3. §7Consigue §6bloques de protección §7(por rango)");
        player.sendMessage("  §a4. §7Desbloquea §5habilidades §7(/habilidades)");
        player.sendMessage("");
        
        player.sendMessage("§e§l📈 CÓMO MEJORAR:");
        player.sendMessage("  §7• Gana XP completando misiones");
        player.sendMessage("  §7• Sube de rango para obtener:");
        player.sendMessage("    §8→ Habilidades permanentes");
        player.sendMessage("    §8→ Recompensas épicas");
        player.sendMessage("    §8→ Bloques de protección");
        player.sendMessage("");
        
        player.sendMessage("§e§lCOMANDOS ÚTILES:");
        player.sendMessage("  §a/avo menu §8- Menú principal");
        player.sendMessage("  §a/misiones §8- Ver misiones");
        player.sendMessage("  §a/habilidades §8- Árbol de habilidades");
        player.sendMessage("  §a/avo stats §8- Tus estadísticas");
        player.sendMessage("  §a/recompensa §8- Reclamar recompensas");
        player.sendMessage("");
        
        if (!hasReachedGlobal) {
            player.sendMessage("§6§l💡 CONSEJO:");
            player.sendMessage("§7Usa /avo menu frecuentemente para ver tu progreso.");
            player.sendMessage("§7¡Los primeros días son cruciales!");
            player.sendMessage("");
        }
        
        player.sendMessage("§6§l═════════════════════════════════════════");
        player.sendMessage("");
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // UTILIDADES
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Verifica si el streamer está online
     */
    private boolean isStreamerOnline() {
        String streamerUsername = plugin.getConfigManager().getRecompensasConfig()
            .getString("xp_dinamico.presencia_streamer.streamer_username", "Riolu");
        
        Player streamer = Bukkit.getPlayer(streamerUsername);
        return streamer != null && streamer.isOnline();
    }
    
    /**
     * Obtiene la cantidad de tokens de stream de un jugador
     * TODO: Implementar sistema de tokens real
     */
    private int getStreamTokens(Player player) {
        // Por ahora devuelve 0, se implementará con StreamTokenManager
        return 0;
    }
    
    /**
     * Obtiene la posición en el ranking de stream de un jugador
     * TODO: Implementar sistema de ranking real
     */
    private int getStreamRanking(Player player) {
        // Por ahora devuelve 0, se implementará con StreamRankingSystem
        return 0;
    }
    
    /**
     * Verifica si el jugador ha completado el tutorial
     */
    private boolean hasCompletedTutorial(Player player) {
        return plugin.getProgressiveDifficultySystem().hasReachedGlobalDifficulty(player);
    }
    
    private ItemStack createBorderItem(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createMenuItem(Material material, String name, List<String> lore, String id) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            
            // Añadir ID oculto al final del lore
            List<String> fullLore = new ArrayList<>(lore);
            fullLore.add("§8ID:" + id);
            
            meta.setLore(fullLore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private int getMissionCount(Player player, boolean completed) {
        List<MissionAssignment> assignments = plugin.getMissionService().getActiveAssignments(player);
        return (int) assignments.stream()
            .filter(a -> a.isCompleted() == completed)
            .count();
    }
    
    private int getPendingRewardsCount(Player player) {
        if (plugin.getRewardClaimSystem() == null) return 0;
        return plugin.getRewardClaimSystem().getTotalPendingItems(player.getUniqueId());
    }
    
    private int getEvasionesCount(Player player) {
        if (plugin.getDisasterEvasionTracker() == null) return 0;
        return plugin.getDisasterEvasionTracker().getEvasionCount(player.getUniqueId());
    }
    
    private MissionRank getNextRank(MissionRank current) {
        MissionRank[] ranks = MissionRank.values();
        for (int i = 0; i < ranks.length - 1; i++) {
            if (ranks[i] == current) {
                return ranks[i + 1];
            }
        }
        return null;
    }
}
