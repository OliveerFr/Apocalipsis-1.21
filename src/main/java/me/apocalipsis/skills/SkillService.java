package me.apocalipsis.skills;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.missions.MissionRank;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio principal para gestionar el árbol de habilidades.
 * Maneja compras, toggles, requisitos y persistencia.
 */
public class SkillService {
    
    private final Apocalipsis plugin;
    private final File dataFile;
    
    // Datos de jugadores: UUID -> PlayerSkillData
    private final Map<UUID, PlayerSkillData> playerData = new HashMap<>();
    
    // Cooldowns para Fénix (revive diario)
    private final Map<UUID, Long> phoenixCooldowns = new HashMap<>();
    
    // Cooldown para Vuelo de Emergencia
    private final Map<UUID, Long> glideCooldowns = new HashMap<>();
    
    // Configuración
    private int minXpRestante = 100;
    private boolean confirmarCompras = true;
    private boolean advertirBajadaRango = true;
    
    // Task para efectos periódicos
    private int effectTaskId = -1;
    
    public SkillService(Apocalipsis plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "skill_data.yml");
        loadConfig();
        loadData();
        startEffectTask();
    }
    
    // ==================== CONFIGURACIÓN ====================
    
    private void loadConfig() {
        // Los valores podrían venir de un archivo de config en el futuro
        minXpRestante = 100;
        confirmarCompras = true;
        advertirBajadaRango = true;
    }
    
    // ==================== PERSISTENCIA ====================
    
    private void loadData() {
        if (!dataFile.exists()) {
            return;
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection playersSection = config.getConfigurationSection("players");
        
        if (playersSection == null) return;
        
        for (String uuidStr : playersSection.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                ConfigurationSection playerSection = playersSection.getConfigurationSection(uuidStr);
                
                if (playerSection == null) continue;
                
                // Cargar habilidades desbloqueadas
                List<String> skillIds = playerSection.getStringList("skills");
                Set<Skill> skills = new HashSet<>();
                for (String id : skillIds) {
                    Skill skill = Skill.fromId(id);
                    if (skill != null) {
                        skills.add(skill);
                    }
                }
                
                // Cargar toggles desactivados
                List<String> disabledToggles = playerSection.getStringList("disabled_toggles");
                Set<Skill> disabled = new HashSet<>();
                for (String id : disabledToggles) {
                    Skill skill = Skill.fromId(id);
                    if (skill != null) {
                        disabled.add(skill);
                    }
                }
                
                // Cargar XP gastada total
                int xpGastada = playerSection.getInt("xp_gastada", 0);
                
                playerData.put(uuid, new PlayerSkillData(skills, disabled, xpGastada));
                
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[Skills] UUID inválido en skill_data.yml: " + uuidStr);
            }
        }
        
        // Cargar cooldowns de Fénix
        ConfigurationSection phoenixSection = config.getConfigurationSection("phoenix_cooldowns");
        if (phoenixSection != null) {
            for (String uuidStr : phoenixSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    long cooldown = phoenixSection.getLong(uuidStr);
                    phoenixCooldowns.put(uuid, cooldown);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }
    
    public void saveData() {
        FileConfiguration config = new YamlConfiguration();
        
        for (Map.Entry<UUID, PlayerSkillData> entry : playerData.entrySet()) {
            String path = "players." + entry.getKey().toString();
            PlayerSkillData data = entry.getValue();
            
            // Guardar habilidades
            List<String> skillIds = data.getSkills().stream()
                .map(Skill::getId)
                .collect(Collectors.toList());
            config.set(path + ".skills", skillIds);
            
            // Guardar toggles desactivados
            List<String> disabledIds = data.getDisabledToggles().stream()
                .map(Skill::getId)
                .collect(Collectors.toList());
            config.set(path + ".disabled_toggles", disabledIds);
            
            // Guardar XP gastada
            config.set(path + ".xp_gastada", data.getXpGastada());
        }
        
        // Guardar cooldowns de Fénix
        for (Map.Entry<UUID, Long> entry : phoenixCooldowns.entrySet()) {
            config.set("phoenix_cooldowns." + entry.getKey().toString(), entry.getValue());
        }
        
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[Skills] Error guardando skill_data.yml: " + e.getMessage());
        }
    }
    
    // ==================== DATOS DEL JUGADOR ====================
    
    private PlayerSkillData getData(UUID uuid) {
        return playerData.computeIfAbsent(uuid, k -> new PlayerSkillData());
    }
    
    public Set<Skill> getUnlockedSkills(Player player) {
        return getData(player.getUniqueId()).getSkills();
    }
    
    public Set<Skill> getUnlockedSkills(UUID uuid) {
        return getData(uuid).getSkills();
    }
    
    public boolean hasSkill(Player player, Skill skill) {
        return getData(player.getUniqueId()).hasSkill(skill);
    }
    
    public boolean hasSkill(UUID uuid, Skill skill) {
        return getData(uuid).hasSkill(skill);
    }
    
    public int getXpGastada(Player player) {
        return getData(player.getUniqueId()).getXpGastada();
    }
    
    public int getTotalXpGastada(UUID uuid) {
        return getData(uuid).getXpGastada();
    }
    
    public int getSkillCount(Player player) {
        return getData(player.getUniqueId()).getSkills().size();
    }
    
    public int getTotalSkillCount() {
        return Skill.values().length;
    }
    
    // ==================== TOGGLES ====================
    
    public boolean isSkillEnabled(Player player, Skill skill) {
        if (!skill.isToggleable()) return true;
        return !getData(player.getUniqueId()).isToggleDisabled(skill);
    }
    
    public boolean isSkillEnabled(UUID uuid, Skill skill) {
        if (!skill.isToggleable()) return true;
        return !getData(uuid).isToggleDisabled(skill);
    }
    
    public void toggleSkill(Player player, Skill skill) {
        if (!skill.isToggleable()) {
            player.sendMessage("§c✗ Esta habilidad no se puede desactivar.");
            return;
        }
        
        if (!hasSkill(player, skill)) {
            player.sendMessage("§c✗ No tienes desbloqueada esta habilidad.");
            return;
        }
        
        PlayerSkillData data = getData(player.getUniqueId());
        boolean nowEnabled;
        
        if (data.isToggleDisabled(skill)) {
            data.enableToggle(skill);
            nowEnabled = true;
        } else {
            data.disableToggle(skill);
            nowEnabled = false;
        }
        
        // Aplicar o quitar efecto inmediatamente
        applySkillEffects(player);
        
        String status = nowEnabled ? "§a✓ ACTIVADA" : "§c✗ DESACTIVADA";
        player.sendMessage("§6§l⚡ " + skill.getColoredName() + " §7" + status);
        
        saveData();
    }
    
    public List<Skill> getToggleableSkills(Player player) {
        return getData(player.getUniqueId()).getSkills().stream()
            .filter(Skill::isToggleable)
            .collect(Collectors.toList());
    }
    
    // ==================== REQUISITOS ====================
    
    public boolean meetsRequirements(Player player, Skill skill) {
        for (String reqId : skill.getRequirements()) {
            Skill required = Skill.fromId(reqId);
            if (required != null && !hasSkill(player, required)) {
                return false;
            }
        }
        return true;
    }
    
    public List<Skill> getMissingRequirements(Player player, Skill skill) {
        List<Skill> missing = new ArrayList<>();
        for (String reqId : skill.getRequirements()) {
            Skill required = Skill.fromId(reqId);
            if (required != null && !hasSkill(player, required)) {
                missing.add(required);
            }
        }
        return missing;
    }
    
    // ==================== COMPRA DE HABILIDADES ====================
    
    /**
     * Resultado de una compra de habilidad
     */
    public enum PurchaseResult {
        SUCCESS,
        ALREADY_OWNED,
        MISSING_REQUIREMENTS,
        NOT_ENOUGH_XP,
        WOULD_DROP_TOO_LOW,
        DURING_DISASTER
    }
    
    /**
     * Intenta comprar una habilidad para el jugador
     */
    public PurchaseResult purchaseSkill(Player player, Skill skill) {
        // Verificar si ya la tiene
        if (hasSkill(player, skill)) {
            return PurchaseResult.ALREADY_OWNED;
        }
        
        // Verificar requisitos
        if (!meetsRequirements(player, skill)) {
            return PurchaseResult.MISSING_REQUIREMENTS;
        }
        
        // Verificar si hay desastre activo
        if (plugin.getDisasterController() != null && plugin.getDisasterController().hasActiveDisaster()) {
            return PurchaseResult.DURING_DISASTER;
        }
        
        // Obtener XP del jugador
        int playerXP = plugin.getExperienceService().getXP(player);
        int cost = skill.getBaseCost(); // Usamos costo base directo
        
        // Verificar si tiene suficiente XP
        if (playerXP < cost) {
            return PurchaseResult.NOT_ENOUGH_XP;
        }
        
        // Verificar que no quede por debajo del mínimo
        if (playerXP - cost < minXpRestante) {
            return PurchaseResult.WOULD_DROP_TOO_LOW;
        }
        
        // ¡Comprar!
        int newXP = playerXP - cost;
        plugin.getExperienceService().setXP(player, newXP);
        
        // Añadir habilidad
        PlayerSkillData data = getData(player.getUniqueId());
        data.addSkill(skill);
        data.addXpGastada(cost);
        
        // Aplicar efectos
        applySkillEffects(player);
        
        // Guardar datos
        saveData();
        
        return PurchaseResult.SUCCESS;
    }
    
    /**
     * Obtiene información sobre el impacto de una compra
     */
    public PurchasePreview previewPurchase(Player player, Skill skill) {
        int currentXP = plugin.getExperienceService().getXP(player);
        int cost = skill.getBaseCost();
        int newXP = currentXP - cost;
        
        MissionRank currentRank = plugin.getRankService().getRank(player);
        MissionRank newRank = plugin.getRankService().getRankForXP(newXP);
        
        boolean willDropRank = newRank != currentRank && newRank.ordinal() < currentRank.ordinal();
        
        return new PurchasePreview(currentXP, newXP, cost, currentRank, newRank, willDropRank);
    }
    
    // ==================== APLICAR EFECTOS ====================
    
    /**
     * Aplica todos los efectos de las habilidades del jugador
     */
    public void applySkillEffects(Player player) {
        UUID uuid = player.getUniqueId();
        Set<Skill> skills = getUnlockedSkills(uuid);
        
        // Resetear atributos primero
        resetPlayerAttributes(player);
        
        // Calcular bonuses acumulados
        int extraHearts = 0;
        double speedBonus = 0;
        
        for (Skill skill : skills) {
            // Saltar si es toggleable y está desactivado
            if (skill.isToggleable() && !isSkillEnabled(uuid, skill)) {
                continue;
            }
            
            switch (skill) {
                // === VIDA EXTRA ===
                case PIEL_GRUESA:
                    extraHearts += 2;
                    break;
                case TANQUE:
                    extraHearts += 4; // Acumulativo con piel gruesa
                    break;
                case INMORTAL:
                    extraHearts += 8; // Acumulativo
                    break;
                    
                // === VELOCIDAD ===
                case PASO_LIGERO:
                    speedBonus += 0.10;
                    break;
                case ZANCADAS:
                    speedBonus += 0.10; // +10% adicional (total 20%)
                    break;
                case VELOCISTA:
                    speedBonus += 0.10; // +10% adicional (total 30%)
                    break;
                    
                // === NADADOR ===
                case NADADOR:
                    // Se maneja en listener
                    break;
                    
                default:
                    break;
            }
        }
        
        // Aplicar vida extra
        if (extraHearts > 0) {
            AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null) {
                double newMax = 20 + (extraHearts * 2); // Cada "corazón" = 2 HP
                maxHealth.setBaseValue(newMax);
            }
        }
        
        // Aplicar velocidad
        if (speedBonus > 0) {
            AttributeInstance movementSpeed = player.getAttribute(Attribute.MOVEMENT_SPEED);
            if (movementSpeed != null) {
                double baseSpeed = 0.1; // Velocidad base de Minecraft
                movementSpeed.setBaseValue(baseSpeed * (1 + speedBonus));
            }
        }
    }
    
    /**
     * Resetea los atributos del jugador a valores base
     */
    private void resetPlayerAttributes(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(20); // 10 corazones base
        }
        
        AttributeInstance movementSpeed = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.setBaseValue(0.1); // Velocidad base
        }
    }
    
    // ==================== EFECTOS PERIÓDICOS ====================
    
    private void startEffectTask() {
        if (effectTaskId != -1) {
            Bukkit.getScheduler().cancelTask(effectTaskId);
        }
        
        // Cada 20 segundos para regeneración pasiva y hambre
        effectTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                processPeriodicEffects(player);
            }
        }, 400L, 400L); // 20 segundos
    }
    
    private void processPeriodicEffects(Player player) {
        UUID uuid = player.getUniqueId();
        Set<Skill> skills = getUnlockedSkills(uuid);
        
        for (Skill skill : skills) {
            if (skill.isToggleable() && !isSkillEnabled(uuid, skill)) {
                continue;
            }
            
            switch (skill) {
                case REGENERACION_PASIVA:
                    // Regenerar 0.5 corazones (1 HP) cada 20s
                    if (player.getHealth() < player.getAttribute(Attribute.MAX_HEALTH).getValue()) {
                        player.setHealth(Math.min(
                            player.getHealth() + 1,
                            player.getAttribute(Attribute.MAX_HEALTH).getValue()
                        ));
                    }
                    break;
                    
                case AUTOSUFICIENTE:
                    // Regenerar 0.5 hambre cada 30s (se ejecuta cada 20s, así que ~0.33)
                    if (player.getFoodLevel() < 20) {
                        player.setFoodLevel(Math.min(player.getFoodLevel() + 1, 20));
                    }
                    break;
                    
                default:
                    break;
            }
        }
    }
    
    // ==================== COOLDOWNS ====================
    
    public boolean isPhoenixReady(Player player) {
        Long lastUse = phoenixCooldowns.get(player.getUniqueId());
        if (lastUse == null) return true;
        
        // 24 horas en milisegundos
        long cooldown = 24 * 60 * 60 * 1000;
        return System.currentTimeMillis() - lastUse >= cooldown;
    }
    
    public void usePhoenix(Player player) {
        phoenixCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        saveData();
    }
    
    public long getPhoenixCooldownRemaining(Player player) {
        Long lastUse = phoenixCooldowns.get(player.getUniqueId());
        if (lastUse == null) return 0;
        
        long cooldown = 24 * 60 * 60 * 1000;
        long remaining = cooldown - (System.currentTimeMillis() - lastUse);
        return Math.max(0, remaining);
    }
    
    public boolean isGlideReady(Player player) {
        Long lastUse = glideCooldowns.get(player.getUniqueId());
        if (lastUse == null) return true;
        
        // 1 minuto en milisegundos
        long cooldown = 60 * 1000;
        return System.currentTimeMillis() - lastUse >= cooldown;
    }
    
    public void useGlide(Player player) {
        glideCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }
    
    // ==================== ADMIN ====================
    
    public void giveSkill(Player player, Skill skill) {
        PlayerSkillData data = getData(player.getUniqueId());
        data.addSkill(skill);
        applySkillEffects(player);
        saveData();
    }
    
    public void removeSkill(Player player, Skill skill) {
        PlayerSkillData data = getData(player.getUniqueId());
        data.removeSkill(skill);
        applySkillEffects(player);
        saveData();
    }
    
    public void resetPlayer(Player player) {
        playerData.remove(player.getUniqueId());
        phoenixCooldowns.remove(player.getUniqueId());
        glideCooldowns.remove(player.getUniqueId());
        resetPlayerAttributes(player);
        saveData();
    }
    
    // ==================== SHUTDOWN ====================
    
    public void shutdown() {
        if (effectTaskId != -1) {
            Bukkit.getScheduler().cancelTask(effectTaskId);
            effectTaskId = -1;
        }
        saveData();
    }
    
    // ==================== CLASE INTERNA: PlayerSkillData ====================
    
    private static class PlayerSkillData {
        private final Set<Skill> skills;
        private final Set<Skill> disabledToggles;
        private int xpGastada;
        
        public PlayerSkillData() {
            this.skills = new HashSet<>();
            this.disabledToggles = new HashSet<>();
            this.xpGastada = 0;
        }
        
        public PlayerSkillData(Set<Skill> skills, Set<Skill> disabledToggles, int xpGastada) {
            this.skills = new HashSet<>(skills);
            this.disabledToggles = new HashSet<>(disabledToggles);
            this.xpGastada = xpGastada;
        }
        
        public Set<Skill> getSkills() { return skills; }
        public Set<Skill> getDisabledToggles() { return disabledToggles; }
        public int getXpGastada() { return xpGastada; }
        
        public boolean hasSkill(Skill skill) { return skills.contains(skill); }
        public void addSkill(Skill skill) { skills.add(skill); }
        public void removeSkill(Skill skill) { 
            skills.remove(skill); 
            disabledToggles.remove(skill);
        }
        
        public boolean isToggleDisabled(Skill skill) { return disabledToggles.contains(skill); }
        public void disableToggle(Skill skill) { disabledToggles.add(skill); }
        public void enableToggle(Skill skill) { disabledToggles.remove(skill); }
        
        public void addXpGastada(int amount) { xpGastada += amount; }
    }
    
    // ==================== CLASE INTERNA: PurchasePreview ====================
    
    public static class PurchasePreview {
        public final int currentXP;
        public final int newXP;
        public final int cost;
        public final MissionRank currentRank;
        public final MissionRank newRank;
        public final boolean willDropRank;
        
        public PurchasePreview(int currentXP, int newXP, int cost, 
                              MissionRank currentRank, MissionRank newRank, boolean willDropRank) {
            this.currentXP = currentXP;
            this.newXP = newXP;
            this.cost = cost;
            this.currentRank = currentRank;
            this.newRank = newRank;
            this.willDropRank = willDropRank;
        }
    }
}
