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
import org.bukkit.inventory.ItemStack;

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
    
    // Cooldowns para habilidades de detección (UUID -> timestamp cuando termina el cooldown)
    private final Map<UUID, Long> rastroOroCooldowns = new HashMap<>();
    private final Map<UUID, Long> detectorSpawnersCooldowns = new HashMap<>();
    private final Map<UUID, Long> xrayDiamantesCooldowns = new HashMap<>();
    
    // Jugadores con habilidades de detección activas (UUID -> timestamp cuando termina)
    private final Map<UUID, Long> rastroOroActivo = new HashMap<>();
    private final Map<UUID, Long> detectorSpawnersActivo = new HashMap<>();
    private final Map<UUID, Long> xrayDiamantesActivo = new HashMap<>();
    
    // Cooldowns para habilidades de invocación (UUID -> timestamp cuando termina el cooldown)
    private final Map<UUID, Long> loboCompañeroCooldowns = new HashMap<>();
    private final Map<UUID, Long> gatoGuardianCooldowns = new HashMap<>();
    private final Map<UUID, Long> manadaLobosCooldowns = new HashMap<>();
    private final Map<UUID, Long> allayRecolectorCooldowns = new HashMap<>();
    private final Map<UUID, Long> abejasProtectorasCooldowns = new HashMap<>();
    private final Map<UUID, Long> golemProtectorCooldowns = new HashMap<>();
    private final Map<UUID, Long> vexVengadorCooldowns = new HashMap<>();
    private final Map<UUID, Long> wardenTemporalCooldowns = new HashMap<>();
    
    // Entidades invocadas activas (UUID jugador -> List<UUID entidad>)
    private final Map<UUID, java.util.List<UUID>> entidadesInvocadas = new HashMap<>();
    
    // Cooldowns para sinergias avanzadas
    private final Map<UUID, Long> omnipresenteCooldowns = new HashMap<>();
    private final Map<UUID, Long> avatarCaosCooldowns = new HashMap<>();
    
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
                
                // Cargar niveles de skills
                Map<Skill, SkillLevel> skillLevels = new HashMap<>();
                ConfigurationSection levelsSection = playerSection.getConfigurationSection("skill_levels");
                if (levelsSection != null) {
                    for (String skillId : levelsSection.getKeys(false)) {
                        Skill skill = Skill.fromId(skillId);
                        if (skill != null) {
                            int level = levelsSection.getInt(skillId, 1);
                            skillLevels.put(skill, SkillLevel.fromNumber(level));
                        }
                    }
                }
                // Asegurar nivel 1 para skills sin nivel guardado
                for (Skill skill : skills) {
                    if (!skillLevels.containsKey(skill)) {
                        skillLevels.put(skill, SkillLevel.LEVEL_1);
                    }
                }
                
                // Cargar XP gastada total
                int xpGastada = playerSection.getInt("xp_gastada", 0);
                
                playerData.put(uuid, new PlayerSkillData(skills, disabled, skillLevels, xpGastada));
                
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
            
            // Guardar niveles de skills
            for (Map.Entry<Skill, SkillLevel> levelEntry : data.getSkillLevels().entrySet()) {
                config.set(path + ".skill_levels." + levelEntry.getKey().getId(), 
                          levelEntry.getValue().getLevel());
            }
            
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
    
    /**
     * Limpia el cache de un jugador y fuerza recarga de habilidades
     */
    public void clearPlayerCache(UUID uuid) {
        // Si existe algún sistema de cache, limpiarlo aquí
        PlayerSkillData data = playerData.get(uuid);
        if (data != null) {
            plugin.getLogger().info("[Skills] Cache limpiado para UUID: " + uuid);
        }
    }
    
    // ==================== SISTEMA DE NIVELES ====================
    
    /**
     * Obtiene el nivel actual de una habilidad del jugador
     */
    public SkillLevel getSkillLevel(Player player, Skill skill) {
        return getData(player.getUniqueId()).getSkillLevel(skill);
    }
    
    public SkillLevel getSkillLevel(UUID uuid, Skill skill) {
        return getData(uuid).getSkillLevel(skill);
    }
    
    /**
     * Obtiene el valor del efecto escalado según el nivel
     */
    public double getScaledEffect(Player player, Skill skill) {
        if (!hasSkill(player, skill)) return 0;
        SkillLevel level = getSkillLevel(player, skill);
        double baseEffect = SkillConfig.getLevelEffect(skill.getId(), 1);
        return baseEffect * level.getEffectMultiplier();
    }
    
    public double getScaledEffect(UUID uuid, Skill skill) {
        if (!hasSkill(uuid, skill)) return 0;
        SkillLevel level = getSkillLevel(uuid, skill);
        double baseEffect = SkillConfig.getLevelEffect(skill.getId(), 1);
        return baseEffect * level.getEffectMultiplier();
    }
    
    /**
     * Obtiene el valor exacto del efecto para el nivel actual
     */
    public double getLevelEffect(Player player, Skill skill) {
        if (!hasSkill(player, skill)) return 0;
        int level = getSkillLevel(player, skill).getLevel();
        return SkillConfig.getLevelEffect(skill.getId(), level);
    }
    
    public double getLevelEffect(UUID uuid, Skill skill) {
        if (!hasSkill(uuid, skill)) return 0;
        int level = getSkillLevel(uuid, skill).getLevel();
        return SkillConfig.getLevelEffect(skill.getId(), level);
    }
    
    /**
     * Calcula el costo de mejorar al siguiente nivel
     */
    public int getUpgradeCost(Player player, Skill skill) {
        if (!hasSkill(player, skill)) return 0;
        SkillLevel currentLevel = getSkillLevel(player, skill);
        if (currentLevel.isMax()) return 0;
        
        SkillLevel nextLevel = currentLevel.getNext();
        int baseCost = skill.getBaseCost();
        return (int) (baseCost * nextLevel.getUpgradeCostMultiplier());
    }
    
    /**
     * Verifica si el jugador puede mejorar una skill
     */
    public boolean canUpgradeSkill(Player player, Skill skill) {
        if (!hasSkill(player, skill)) return false;
        if (!skill.isUpgradeable()) return false; // Skills no mejorables
        SkillLevel currentLevel = getSkillLevel(player, skill);
        if (currentLevel.isMax()) return false;
        
        int cost = getUpgradeCost(player, skill);
        int playerXP = plugin.getExperienceService().getXP(player);
        return playerXP >= cost;
    }
    
    /**
     * Mejora una skill al siguiente nivel
     */
    public boolean upgradeSkill(Player player, Skill skill) {
        if (!canUpgradeSkill(player, skill)) return false;
        
        UUID uuid = player.getUniqueId();
        SkillLevel currentLevel = getSkillLevel(player, skill);
        SkillLevel nextLevel = currentLevel.getNext();
        int cost = getUpgradeCost(player, skill);
        
        // Cobrar XP
        plugin.getExperienceService().spendXP(player, cost);
        
        // Subir nivel
        getData(uuid).setSkillLevel(skill, nextLevel);
        getData(uuid).addXpGastada(cost);
        
        // Reaplicar bonificaciones
        applySkillEffects(player);
        
        // Mensaje
        player.sendMessage("§a§l✦ §a¡" + skill.getDisplayName() + " mejorado a " + 
                          nextLevel.getColor() + "Nivel " + nextLevel.getRoman() + "§a!");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        
        // Guardar
        saveData();
        
        return true;
    }
    
    /**
     * Obtiene información de preview para upgrade
     */
    public UpgradePreview getUpgradePreview(Player player, Skill skill) {
        if (!hasSkill(player, skill)) return null;
        
        SkillLevel currentLevel = getSkillLevel(player, skill);
        if (currentLevel.isMax()) return null;
        
        SkillLevel nextLevel = currentLevel.getNext();
        int cost = getUpgradeCost(player, skill);
        int playerXP = plugin.getExperienceService().getXP(player);
        
        double currentEffect = SkillConfig.getLevelEffect(skill.getId(), currentLevel.getLevel());
        double nextEffect = SkillConfig.getLevelEffect(skill.getId(), nextLevel.getLevel());
        String bonus = SkillConfig.getLevel3Bonus(skill);
        
        return new UpgradePreview(currentLevel, nextLevel, cost, playerXP, 
                                  currentEffect, nextEffect, bonus);
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
        
        plugin.getLogger().info("[Skills] Aplicando efectos para " + player.getName() + " (" + skills.size() + " habilidades)");
        
        // Resetear atributos primero
        resetPlayerAttributes(player);
        
        // Calcular bonuses acumulados (ahora usando valores por nivel)
        double extraHearts = 0;
        double speedBonus = 0;
        double attackSpeedBonus = 0;
        
        int skillsApplied = 0;
        
        for (Skill skill : skills) {
            // Saltar si es toggleable y está desactivado
            if (skill.isToggleable() && !isSkillEnabled(uuid, skill)) {
                plugin.getLogger().fine("[Skills] " + skill.name() + " está toggleado OFF");
                continue;
            }
            
            switch (skill) {
                // === VIDA EXTRA (valores de SkillConfig en corazones) ===
                case PIEL_GRUESA:
                    extraHearts += getLevelEffect(uuid, Skill.PIEL_GRUESA); // 2/3/4
                    skillsApplied++;
                    break;
                case TANQUE:
                    extraHearts += getLevelEffect(uuid, Skill.TANQUE); // 4/6/8
                    skillsApplied++;
                    break;
                case INMORTAL:
                    extraHearts += getLevelEffect(uuid, Skill.INMORTAL); // 8/10/14
                    skillsApplied++;
                    break;
                    
                // === VELOCIDAD (valores de SkillConfig en %) ===
                case PASO_LIGERO:
                    speedBonus += getLevelEffect(uuid, Skill.PASO_LIGERO) / 100.0; // 10/15/20%
                    skillsApplied++;
                    break;
                case ZANCADAS:
                    speedBonus += getLevelEffect(uuid, Skill.ZANCADAS) / 100.0; // 20/30/40%
                    skillsApplied++;
                    break;
                case VELOCISTA:
                    speedBonus += getLevelEffect(uuid, Skill.VELOCISTA) / 100.0; // 30/40/50%
                    skillsApplied++;
                    break;
                
                // === VELOCIDAD DE ATAQUE ===
                case REFLEJOS:
                    attackSpeedBonus += getLevelEffect(uuid, Skill.REFLEJOS) / 100.0; // 10/15/20%
                    skillsApplied++;
                    break;
                    
                // === NADADOR ===
                case NADADOR:
                    // Se maneja en listener
                    skillsApplied++;
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
                plugin.getLogger().info("[Skills] Vida aplicada: " + newMax + " HP (+" + extraHearts + " corazones)");
            }
        }
        
        // Aplicar velocidad
        if (speedBonus > 0) {
            AttributeInstance movementSpeed = player.getAttribute(Attribute.MOVEMENT_SPEED);
            if (movementSpeed != null) {
                double baseSpeed = 0.1; // Velocidad base de Minecraft
                double newSpeed = baseSpeed * (1 + speedBonus);
                movementSpeed.setBaseValue(newSpeed);
                plugin.getLogger().info("[Skills] Velocidad aplicada: +" + (speedBonus * 100) + "%");
            }
        }
        
        // Aplicar velocidad de ataque (REFLEJOS)
        if (attackSpeedBonus > 0) {
            AttributeInstance attackSpeed = player.getAttribute(Attribute.ATTACK_SPEED);
            if (attackSpeed != null) {
                double baseAttackSpeed = 4.0; // Velocidad de ataque base
                double newAttackSpeed = baseAttackSpeed * (1 + attackSpeedBonus);
                attackSpeed.setBaseValue(newAttackSpeed);
                plugin.getLogger().info("[Skills] Velocidad de ataque aplicada: +" + (attackSpeedBonus * 100) + "%");
            }
        }
        
        plugin.getLogger().info("[Skills] Total de efectos aplicados: " + skillsApplied + "/" + skills.size());
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
        
        AttributeInstance attackSpeed = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attackSpeed != null) {
            attackSpeed.setBaseValue(4.0); // Velocidad de ataque base
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
    
    /**
     * Aplica un efecto de poción de forma ADITIVA.
     * Si el jugador ya tiene el efecto (beacon/poción), suma los niveles.
     * @param player El jugador
     * @param effectType Tipo de efecto
     * @param duration Duración en ticks
     * @param skillAmplifier Amplificador que aporta la habilidad (0 = nivel I, 1 = nivel II, etc)
     * @param ambient Si es ambiente (sin partículas grandes)
     * @param showParticles Si mostrar partículas
     */
    private void applyAdditiveEffect(Player player, org.bukkit.potion.PotionEffectType effectType, 
                                      int duration, int skillAmplifier, boolean ambient, boolean showParticles) {
        org.bukkit.potion.PotionEffect existing = player.getPotionEffect(effectType);
        int finalAmplifier = skillAmplifier;
        
        if (existing != null) {
            // Sumar el nivel del efecto existente + el de la habilidad + 1
            // Ejemplo: Beacon Haste I (amp 0) + Skill Haste I (amp 0) = Haste II (amp 1)
            finalAmplifier = existing.getAmplifier() + skillAmplifier + 1;
            // Limitar a nivel máximo razonable (nivel 5 = amplifier 4)
            finalAmplifier = Math.min(finalAmplifier, 4);
        }
        
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            effectType, duration, finalAmplifier, ambient, showParticles));
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
                    AttributeInstance maxHpRegen = player.getAttribute(Attribute.MAX_HEALTH);
                    if (maxHpRegen != null && player.getHealth() < maxHpRegen.getValue()) {
                        player.setHealth(Math.min(player.getHealth() + 1, maxHpRegen.getValue()));
                    }
                    break;
                    
                case AUTOSUFICIENTE:
                    // Regenerar 0.5 hambre cada 30s (se ejecuta cada 20s, así que ~0.33)
                    if (player.getFoodLevel() < 20) {
                        player.setFoodLevel(Math.min(player.getFoodLevel() + 1, 20));
                    }
                    break;
                
                case VISION_NOCTURNA:
                    // Aplicar visión nocturna permanente (no necesita ser aditiva, solo nivel 1)
                    applyAdditiveEffect(player, org.bukkit.potion.PotionEffectType.NIGHT_VISION, 500, 0, true, false);
                    break;
                
                case MINERO_EFICIENTE:
                    // Haste basado en nivel: Nivel 1 = Haste I, Nivel 2 = Haste I+, Nivel 3 = Haste II
                    // Se SUMA con beacon/pociones existentes
                    if (isSkillEnabled(uuid, Skill.MINERO_EFICIENTE)) {
                        int level = getSkillLevel(player, Skill.MINERO_EFICIENTE).getLevel();
                        int hasteLevel = level >= 3 ? 1 : 0; // Nivel 3 = Haste II (amplifier 1)
                        applyAdditiveEffect(player, org.bukkit.potion.PotionEffectType.HASTE, 500, hasteLevel, true, false);
                    }
                    break;
                
                case OJO_AGUILA:
                    // Marcar mobs cercanos con Glowing
                    for (org.bukkit.entity.Entity entity : player.getNearbyEntities(20, 10, 20)) {
                        if (entity instanceof org.bukkit.entity.Monster) {
                            ((org.bukkit.entity.LivingEntity) entity).addPotionEffect(
                                new org.bukkit.potion.PotionEffect(
                                    org.bukkit.potion.PotionEffectType.GLOWING, 500, 0, true, false));
                        }
                    }
                    break;
                
                case ZANCADAS:
                    // Salto mejorado - Se SUMA con beacon/pociones existentes
                    if (isSkillEnabled(uuid, Skill.ZANCADAS)) {
                        int level = getSkillLevel(player, Skill.ZANCADAS).getLevel();
                        int jumpLevel = level >= 3 ? 1 : 0; // Nivel 3 = Jump Boost II
                        applyAdditiveEffect(player, org.bukkit.potion.PotionEffectType.JUMP_BOOST, 500, jumpLevel, true, false);
                    }
                    break;
                
                case BERSERKER:
                    // +20% velocidad cuando tiene <25% vida - Se SUMA con Speed existente
                    AttributeInstance maxHpBerserker = player.getAttribute(Attribute.MAX_HEALTH);
                    if (maxHpBerserker != null) {
                        double healthPercent = player.getHealth() / maxHpBerserker.getValue();
                        if (healthPercent < 0.25) {
                            applyAdditiveEffect(player, org.bukkit.potion.PotionEffectType.SPEED, 500, 0, true, false);
                        }
                    }
                    break;
                
                case NADADOR:
                    // Velocidad de nado aumentada - Se SUMA con Dolphin's Grace existente
                    if (player.isInWater()) {
                        applyAdditiveEffect(player, org.bukkit.potion.PotionEffectType.DOLPHINS_GRACE, 500, 0, true, false);
                    }
                    break;
                
                case BRANQUIAS:
                    // Restaurar aire bajo el agua
                    if (player.isInWater() && player.getRemainingAir() < player.getMaximumAir()) {
                        int newAir = Math.min(player.getRemainingAir() + 30, player.getMaximumAir());
                        player.setRemainingAir(newAir);
                    }
                    break;
                
                case ANFIBIO:
                    // Respiración infinita bajo agua
                    if (player.isInWater()) {
                        player.setRemainingAir(player.getMaximumAir());
                    }
                    break;
                
                case BRUJULA_INTERNA:
                    // Mostrar coordenadas en action bar
                    org.bukkit.Location loc = player.getLocation();
                    String coords = String.format("§e⬤ §fX: §a%d §f| Y: §a%d §f| Z: §a%d",
                        loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                    player.sendActionBar(coords);
                    break;
                
                case EXPLORADOR_LIGERO:
                    // +20% velocidad cuando mochila llena (sinergia) - Se SUMA con Speed existente
                    int backpackSize = plugin.getBackpackService().getBackpackSize(uuid);
                    if (backpackSize > 0) {
                        org.bukkit.inventory.ItemStack[] backpackContents = plugin.getBackpackService().getBackpackContents(uuid);
                        if (backpackContents != null) {
                            int itemCount = 0;
                            for (org.bukkit.inventory.ItemStack item : backpackContents) {
                                if (item != null && item.getType() != org.bukkit.Material.AIR) {
                                    itemCount++;
                                }
                            }
                            // Si la mochila está 80%+ llena
                            if (itemCount >= backpackSize * 0.8) {
                                applyAdditiveEffect(player, org.bukkit.potion.PotionEffectType.SPEED, 500, 0, true, false);
                            }
                        }
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
    
    // ==================== HABILIDADES DE DETECCIÓN ====================
    
    /**
     * Activa Rastro de Oro - Marca minerales cercanos por 10 segundos
     * Cooldown: 60 segundos
     */
    public boolean activateRastroOro(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (!hasSkill(uuid, Skill.RASTRO_ORO)) {
            player.sendMessage("§c✗ No tienes la habilidad Rastro de Oro");
            return false;
        }
        
        // Verificar cooldown
        Long cooldownEnd = rastroOroCooldowns.get(uuid);
        if (cooldownEnd != null && System.currentTimeMillis() < cooldownEnd) {
            long remaining = (cooldownEnd - System.currentTimeMillis()) / 1000;
            player.sendMessage("§c✗ Rastro de Oro en cooldown: §e" + remaining + "s");
            return false;
        }
        
        // Activar por 10 segundos
        long duration = 10 * 1000; // 10 segundos
        rastroOroActivo.put(uuid, System.currentTimeMillis() + duration);
        
        // Cooldown de 60 segundos
        rastroOroCooldowns.put(uuid, System.currentTimeMillis() + 60 * 1000);
        
        player.sendMessage("§6§l✦ §eRastro de Oro activado! §7(10s)");
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 1.5f);
        
        // Escanear minerales cercanos
        scanAndHighlightOres(player);
        
        return true;
    }
    
    /**
     * Activa Detector de Spawners - Muestra partículas hacia spawners por 15 segundos
     * Cooldown: 90 segundos
     */
    public boolean activateDetectorSpawners(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (!hasSkill(uuid, Skill.DETECTOR_SPAWNERS)) {
            player.sendMessage("§c✗ No tienes la habilidad Detector de Spawners");
            return false;
        }
        
        Long cooldownEnd = detectorSpawnersCooldowns.get(uuid);
        if (cooldownEnd != null && System.currentTimeMillis() < cooldownEnd) {
            long remaining = (cooldownEnd - System.currentTimeMillis()) / 1000;
            player.sendMessage("§c✗ Detector de Spawners en cooldown: §e" + remaining + "s");
            return false;
        }
        
        // Activar por 15 segundos
        long duration = 15 * 1000;
        detectorSpawnersActivo.put(uuid, System.currentTimeMillis() + duration);
        
        // Cooldown de 90 segundos
        detectorSpawnersCooldowns.put(uuid, System.currentTimeMillis() + 90 * 1000);
        
        player.sendMessage("§5§l✦ §dDetector de Spawners activado! §7(15s)");
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_BEACON_POWER_SELECT, 0.5f, 1.2f);
        
        // Escanear spawners cercanos
        scanAndShowSpawners(player);
        
        return true;
    }
    
    /**
     * Activa X-Ray Diamantes - Resalta diamantes cercanos por 8 segundos
     * Cooldown: 120 segundos
     */
    public boolean activateXrayDiamantes(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (!hasSkill(uuid, Skill.XRAY_DIAMANTES)) {
            player.sendMessage("§c✗ No tienes la habilidad Sentido del Diamante");
            return false;
        }
        
        Long cooldownEnd = xrayDiamantesCooldowns.get(uuid);
        if (cooldownEnd != null && System.currentTimeMillis() < cooldownEnd) {
            long remaining = (cooldownEnd - System.currentTimeMillis()) / 1000;
            player.sendMessage("§c✗ Sentido del Diamante en cooldown: §e" + remaining + "s");
            return false;
        }
        
        // Activar por 8 segundos
        long duration = 8 * 1000;
        xrayDiamantesActivo.put(uuid, System.currentTimeMillis() + duration);
        
        // Cooldown de 120 segundos (2 minutos)
        xrayDiamantesCooldowns.put(uuid, System.currentTimeMillis() + 120 * 1000);
        
        player.sendMessage("§b§l✦ §bSentido del Diamante activado! §7(8s)");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.5f);
        
        // Escanear diamantes cercanos
        scanAndHighlightDiamonds(player);
        
        return true;
    }
    
    // Escanea y marca minerales valiosos (oro, hierro, cobre, etc)
    private void scanAndHighlightOres(Player player) {
        org.bukkit.Location center = player.getLocation();
        int radius = 15;
        int found = 0;
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    org.bukkit.block.Block block = center.clone().add(x, y, z).getBlock();
                    if (isValuableOre(block.getType())) {
                        // Mostrar partículas brillantes en el mineral
                        player.spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, 
                            block.getLocation().add(0.5, 0.5, 0.5), 5, 0.3, 0.3, 0.3, 0);
                        found++;
                    }
                }
            }
        }
        
        if (found > 0) {
            player.sendMessage("§6✦ §eEncontrados §a" + found + " §eminerales valiosos cerca!");
        } else {
            player.sendMessage("§7No hay minerales valiosos en un radio de " + radius + " bloques.");
        }
    }
    
    // Escanea y muestra dirección hacia spawners
    private void scanAndShowSpawners(Player player) {
        org.bukkit.Location center = player.getLocation();
        int radius = 30;
        java.util.List<org.bukkit.block.Block> spawners = new java.util.ArrayList<>();
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    org.bukkit.block.Block block = center.clone().add(x, y, z).getBlock();
                    if (block.getType() == org.bukkit.Material.SPAWNER) {
                        spawners.add(block);
                    }
                }
            }
        }
        
        if (spawners.isEmpty()) {
            player.sendMessage("§7No hay spawners en un radio de " + radius + " bloques.");
            return;
        }
        
        player.sendMessage("§5✦ §d¡Detectados §a" + spawners.size() + " §dspawners!");
        
        // Mostrar partículas hacia cada spawner
        for (org.bukkit.block.Block spawner : spawners) {
            org.bukkit.Location spawnerLoc = spawner.getLocation().add(0.5, 0.5, 0.5);
            double distance = center.distance(spawnerLoc);
            
            // Crear línea de partículas hacia el spawner
            org.bukkit.util.Vector direction = spawnerLoc.toVector().subtract(center.toVector()).normalize();
            for (double d = 1; d < Math.min(distance, 10); d += 0.5) {
                org.bukkit.Location particleLoc = center.clone().add(direction.clone().multiply(d));
                player.spawnParticle(org.bukkit.Particle.WITCH, particleLoc, 1, 0, 0, 0, 0);
            }
            
            // Mostrar partículas en el spawner
            player.spawnParticle(org.bukkit.Particle.FLAME, spawnerLoc, 20, 0.3, 0.3, 0.3, 0.02);
            
            // Mensaje con distancia
            player.sendMessage("§8  → §7Spawner a §e" + (int)distance + " §7bloques");
        }
    }
    
    // Escanea y resalta diamantes/netherite
    private void scanAndHighlightDiamonds(Player player) {
        org.bukkit.Location center = player.getLocation();
        int radius = 12;
        int diamonds = 0;
        int netherite = 0;
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    org.bukkit.block.Block block = center.clone().add(x, y, z).getBlock();
                    org.bukkit.Material type = block.getType();
                    
                    if (type == org.bukkit.Material.DIAMOND_ORE || type == org.bukkit.Material.DEEPSLATE_DIAMOND_ORE) {
                        // Partículas azules brillantes
                        player.spawnParticle(org.bukkit.Particle.SOUL_FIRE_FLAME, 
                            block.getLocation().add(0.5, 0.5, 0.5), 10, 0.3, 0.3, 0.3, 0.02);
                        diamonds++;
                    } else if (type == org.bukkit.Material.ANCIENT_DEBRIS) {
                        // Partículas doradas
                        player.spawnParticle(org.bukkit.Particle.LAVA, 
                            block.getLocation().add(0.5, 0.5, 0.5), 5, 0.2, 0.2, 0.2, 0);
                        netherite++;
                    }
                }
            }
        }
        
        StringBuilder msg = new StringBuilder("§b✦ ");
        if (diamonds > 0) {
            msg.append("§bDiamantes: §a").append(diamonds).append(" ");
        }
        if (netherite > 0) {
            msg.append("§6Ancient Debris: §a").append(netherite);
        }
        if (diamonds == 0 && netherite == 0) {
            msg.append("§7No hay diamantes/netherite en ").append(radius).append(" bloques.");
        }
        player.sendMessage(msg.toString());
    }
    
    private boolean isValuableOre(org.bukkit.Material type) {
        return type == org.bukkit.Material.IRON_ORE || 
               type == org.bukkit.Material.DEEPSLATE_IRON_ORE ||
               type == org.bukkit.Material.GOLD_ORE || 
               type == org.bukkit.Material.DEEPSLATE_GOLD_ORE ||
               type == org.bukkit.Material.COPPER_ORE || 
               type == org.bukkit.Material.DEEPSLATE_COPPER_ORE ||
               type == org.bukkit.Material.LAPIS_ORE || 
               type == org.bukkit.Material.DEEPSLATE_LAPIS_ORE ||
               type == org.bukkit.Material.REDSTONE_ORE || 
               type == org.bukkit.Material.DEEPSLATE_REDSTONE_ORE ||
               type == org.bukkit.Material.EMERALD_ORE || 
               type == org.bukkit.Material.DEEPSLATE_EMERALD_ORE ||
               type == org.bukkit.Material.NETHER_GOLD_ORE ||
               type == org.bukkit.Material.NETHER_QUARTZ_ORE;
    }
    
    // Obtener cooldown restante para mostrar en GUI
    public long getRastroOroCooldown(UUID uuid) {
        Long end = rastroOroCooldowns.get(uuid);
        if (end == null) return 0;
        return Math.max(0, end - System.currentTimeMillis());
    }
    
    public long getDetectorSpawnersCooldown(UUID uuid) {
        Long end = detectorSpawnersCooldowns.get(uuid);
        if (end == null) return 0;
        return Math.max(0, end - System.currentTimeMillis());
    }
    
    public long getXrayDiamantesCooldown(UUID uuid) {
        Long end = xrayDiamantesCooldowns.get(uuid);
        if (end == null) return 0;
        return Math.max(0, end - System.currentTimeMillis());
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
    
    // ==================== INVOCACIONES ====================
    
    // Task ID para el seguimiento de mascotas
    private int petFollowTaskId = -1;
    
    /**
     * Inicia el sistema de seguimiento de mascotas invocadas
     */
    private void startPetFollowTask() {
        if (petFollowTaskId != -1) {
            Bukkit.getScheduler().cancelTask(petFollowTaskId);
        }
        
        // Task que hace que las mascotas sigan a sus dueños cada 10 ticks
        petFollowTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (Map.Entry<UUID, java.util.List<UUID>> entry : entidadesInvocadas.entrySet()) {
                UUID playerUuid = entry.getKey();
                Player player = Bukkit.getPlayer(playerUuid);
                if (player == null || !player.isOnline()) continue;
                
                java.util.List<UUID> entityIds = entry.getValue();
                if (entityIds == null) continue;
                
                for (UUID entityId : entityIds) {
                    org.bukkit.entity.Entity entity = Bukkit.getEntity(entityId);
                    if (entity == null || entity.isDead()) continue;
                    
                    // Calcular distancia al jugador
                    double distance = entity.getLocation().distance(player.getLocation());
                    
                    // Si está muy lejos, teleportar
                    if (distance > 30) {
                        entity.teleport(player.getLocation().add(
                            (Math.random() - 0.5) * 2, 0, (Math.random() - 0.5) * 2));
                        continue;
                    }
                    
                    // Si está a distancia media, hacer que camine hacia el jugador
                    if (distance > 6 && entity instanceof org.bukkit.entity.Mob mob) {
                        // El pathfinder hará que se acerque
                        mob.getPathfinder().moveTo(player.getLocation());
                    }
                    
                    // Partículas sutiles de vinculo cada cierto tiempo
                    if (Math.random() < 0.1) {
                        entity.getWorld().spawnParticle(org.bukkit.Particle.HEART, 
                            entity.getLocation().add(0, 1, 0), 1, 0.2, 0.2, 0.2, 0);
                    }
                }
            }
        }, 10L, 10L); // Cada 0.5 segundos
    }
    
    /**
     * Invoca un lobo compañero que sigue y protege al jugador
     * Duración: 15 minutos, Cooldown: 20 minutos
     */
    public boolean invocarLobo(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (!hasSkill(uuid, Skill.LOBO_COMPANERO) && !hasSkill(uuid, Skill.MANADA_LOBOS)) {
            player.sendMessage("§c✗ No tienes la habilidad Lobo Compañero");
            return false;
        }
        
        // Verificar cooldown
        Long cooldownEnd = loboCompañeroCooldowns.get(uuid);
        if (cooldownEnd != null && System.currentTimeMillis() < cooldownEnd) {
            long remaining = (cooldownEnd - System.currentTimeMillis()) / 1000;
            player.sendMessage("§c✗ Lobo Compañero en cooldown: §e" + formatTime(remaining));
            return false;
        }
        
        // Limpiar lobos anteriores de este jugador
        despawnEntidades(uuid);
        
        int cantidad = hasSkill(uuid, Skill.MANADA_LOBOS) ? 3 : 1;
        boolean mejorado = hasSkill(uuid, Skill.DOMADOR_BESTIAS);
        
        // Iniciar task de seguimiento si no está corriendo
        if (petFollowTaskId == -1) {
            startPetFollowTask();
        }
        
        for (int i = 0; i < cantidad; i++) {
            org.bukkit.Location loc = player.getLocation().add(
                (Math.random() - 0.5) * 2, 0, (Math.random() - 0.5) * 2);
            org.bukkit.entity.Wolf wolf = player.getWorld().spawn(loc, org.bukkit.entity.Wolf.class);
            wolf.setTamed(true);
            wolf.setOwner(player);
            wolf.setCustomName("§b🐺 " + player.getName());
            wolf.setCustomNameVisible(true);
            wolf.setSitting(false); // Asegurar que NO esté sentado
            wolf.setCollarColor(org.bukkit.DyeColor.CYAN);
            
            // Prevenir que desaparezca
            wolf.setPersistent(true);
            wolf.setRemoveWhenFarAway(false);
            
            if (mejorado) {
                // 2x más fuerte con DOMADOR_BESTIAS
                wolf.getAttribute(Attribute.MAX_HEALTH).setBaseValue(40); // 20 hearts
                wolf.setHealth(40);
                wolf.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(8); // 4 hearts damage
                wolf.setCustomName("§6🐺 " + player.getName() + " §c[+]");
            }
            
            // Registrar entidad
            entidadesInvocadas.computeIfAbsent(uuid, k -> new java.util.ArrayList<>())
                .add(wolf.getUniqueId());
            
            // Efecto de aparición
            wolf.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, wolf.getLocation().add(0, 0.5, 0), 
                10, 0.3, 0.3, 0.3, 0.02);
        }
        
        // Programar despawn después de 15 minutos
        final java.util.List<UUID> spawnedWolves = new java.util.ArrayList<>(
            entidadesInvocadas.getOrDefault(uuid, new java.util.ArrayList<>()));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (UUID wolfId : spawnedWolves) {
                org.bukkit.entity.Entity entity = Bukkit.getEntity(wolfId);
                if (entity != null && !entity.isDead()) {
                    entity.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, 
                        entity.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
                    entity.remove();
                }
            }
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage("§7Tus lobos se han marchado...");
            }
        }, 15 * 60 * 20L);
        
        // Cooldown de 20 minutos
        loboCompañeroCooldowns.put(uuid, System.currentTimeMillis() + 20 * 60 * 1000);
        
        String msg = cantidad > 1 ? "§b§l🐺 ¡Manada de " + cantidad + " lobos invocada! §7(15 min)" 
                                  : "§b§l🐺 ¡Lobo compañero invocado! §7(15 min)";
        player.sendMessage(msg);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WOLF_AMBIENT, 1.0f, 0.8f);
        
        return true;
    }
    
    /**
     * Invoca un gato guardián que ahuyenta creepers y phantoms
     * Duración: 20 minutos, Cooldown: 25 minutos
     */
    public boolean invocarGato(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (!hasSkill(uuid, Skill.GATO_GUARDIAN)) {
            player.sendMessage("§c✗ No tienes la habilidad Gato Guardián");
            return false;
        }
        
        Long cooldownEnd = gatoGuardianCooldowns.get(uuid);
        if (cooldownEnd != null && System.currentTimeMillis() < cooldownEnd) {
            long remaining = (cooldownEnd - System.currentTimeMillis()) / 1000;
            player.sendMessage("§c✗ Gato Guardián en cooldown: §e" + formatTime(remaining));
            return false;
        }
        
        // Limpiar entidades anteriores
        despawnEntidades(uuid);
        
        // Iniciar task de seguimiento si no está corriendo
        if (petFollowTaskId == -1) {
            startPetFollowTask();
        }
        
        boolean mejorado = hasSkill(uuid, Skill.DOMADOR_BESTIAS);
        
        org.bukkit.entity.Cat cat = player.getWorld().spawn(player.getLocation(), org.bukkit.entity.Cat.class);
        cat.setTamed(true);
        cat.setOwner(player);
        cat.setCustomName("§e🐱 " + player.getName());
        cat.setCustomNameVisible(true);
        cat.setSitting(false); // Asegurar que NO esté sentado
        
        // Prevenir que desaparezca
        cat.setPersistent(true);
        cat.setRemoveWhenFarAway(false);
        
        // Elegir un tipo de gato aleatorio pero bonito
        org.bukkit.entity.Cat.Type[] tipos = org.bukkit.entity.Cat.Type.values();
        cat.setCatType(tipos[(int)(Math.random() * tipos.length)]);
        
        if (mejorado) {
            cat.getAttribute(Attribute.MAX_HEALTH).setBaseValue(40);
            cat.setHealth(40);
            cat.setCustomName("§6🐱 " + player.getName() + " §c[+]");
        }
        
        // Efecto de aparición
        cat.getWorld().spawnParticle(org.bukkit.Particle.HEART, cat.getLocation().add(0, 0.5, 0), 
            5, 0.3, 0.3, 0.3, 0);
        
        entidadesInvocadas.computeIfAbsent(uuid, k -> new java.util.ArrayList<>())
            .add(cat.getUniqueId());
        
        // Despawn después de 20 minutos
        final UUID catId = cat.getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            org.bukkit.entity.Entity entity = Bukkit.getEntity(catId);
            if (entity != null && !entity.isDead()) {
                entity.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, 
                    entity.getLocation(), 15, 0.3, 0.3, 0.3, 0.05);
                entity.remove();
            }
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage("§7Tu gato guardián se ha marchado...");
            }
        }, 20 * 60 * 20L);
        
        // Cooldown 25 minutos
        gatoGuardianCooldowns.put(uuid, System.currentTimeMillis() + 25 * 60 * 1000);
        
        player.sendMessage("§e§l🐱 ¡Gato guardián invocado! §7(20 min) - Ahuyenta creepers y phantoms");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_CAT_PURREOW, 1.0f, 1.0f);
        
        return true;
    }
    
    /**
     * Invoca un allay recolector que recoge items cercanos
     * Duración: 10 minutos, Cooldown: 15 minutos
     */
    public boolean invocarAllay(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (!hasSkill(uuid, Skill.ALLAY_RECOLECTOR)) {
            player.sendMessage("§c✗ No tienes la habilidad Allay Recolector");
            return false;
        }
        
        Long cooldownEnd = allayRecolectorCooldowns.get(uuid);
        if (cooldownEnd != null && System.currentTimeMillis() < cooldownEnd) {
            long remaining = (cooldownEnd - System.currentTimeMillis()) / 1000;
            player.sendMessage("§c✗ Allay Recolector en cooldown: §e" + formatTime(remaining));
            return false;
        }
        
        despawnEntidades(uuid);
        
        // Iniciar task de seguimiento si no está corriendo
        if (petFollowTaskId == -1) {
            startPetFollowTask();
        }
        
        boolean mejorado = hasSkill(uuid, Skill.DOMADOR_BESTIAS);
        int cantidad = mejorado ? 2 : 1; // Con Domador de Bestias, 2 allays
        
        for (int i = 0; i < cantidad; i++) {
            org.bukkit.entity.Allay allay = player.getWorld().spawn(
                player.getLocation().add((Math.random() - 0.5) * 2, 1.5, (Math.random() - 0.5) * 2), 
                org.bukkit.entity.Allay.class);
            allay.setCustomName("§d✧ " + player.getName());
            allay.setCustomNameVisible(true);
            
            // Prevenir que desaparezca
            allay.setPersistent(true);
            allay.setRemoveWhenFarAway(false);
            
            // Efecto de aparición
            allay.getWorld().spawnParticle(org.bukkit.Particle.NOTE, 
                allay.getLocation(), 10, 0.5, 0.5, 0.5, 0);
            
            entidadesInvocadas.computeIfAbsent(uuid, k -> new java.util.ArrayList<>())
                .add(allay.getUniqueId());
            
            // Task para que el allay recoja items y los lleve al jugador
            final UUID allayId = allay.getUniqueId();
            Bukkit.getScheduler().runTaskTimer(plugin, task -> {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null || !p.isOnline()) {
                    task.cancel();
                    return;
                }
                
                org.bukkit.entity.Entity entity = Bukkit.getEntity(allayId);
                if (entity == null || entity.isDead()) {
                    task.cancel();
                    return;
                }
                
                org.bukkit.entity.Allay a = (org.bukkit.entity.Allay) entity;
                
                // Hacer que siga al jugador
                if (a.getLocation().distance(p.getLocation()) > 15) {
                    a.teleport(p.getLocation().add(0, 1.5, 0));
                } else if (a.getLocation().distance(p.getLocation()) > 5) {
                    // Volar hacia el jugador
                    org.bukkit.util.Vector direction = p.getLocation().add(0, 1, 0)
                        .toVector().subtract(a.getLocation().toVector()).normalize().multiply(0.3);
                    a.setVelocity(direction);
                }
                
                // Buscar items cercanos al allay y llevarlos al jugador
                int itemsCollected = 0;
                for (org.bukkit.entity.Entity e : entity.getNearbyEntities(8, 4, 8)) {
                    if (e instanceof org.bukkit.entity.Item item && !item.isDead()) {
                        // Verificar si el item puede ser recogido
                        if (item.getPickupDelay() > 0) continue;
                        
                        // Dar el item directamente al jugador
                        ItemStack stack = item.getItemStack();
                        java.util.HashMap<Integer, ItemStack> leftover = p.getInventory().addItem(stack);
                        
                        if (leftover.isEmpty()) {
                            item.remove();
                            itemsCollected++;
                            
                            // Efecto de recolección
                            a.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, 
                                item.getLocation(), 5, 0.2, 0.2, 0.2, 0);
                        }
                    }
                }
                
                // Notificar al jugador (máx cada 5 segundos)
                if (itemsCollected > 0 && Math.random() < 0.3) {
                    p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_ALLAY_AMBIENT_WITH_ITEM, 0.5f, 1.2f);
                }
            }, 20L, 20L); // Cada segundo
        }
        
        // Despawn después de 10 minutos
        final java.util.List<UUID> spawnedAllays = new java.util.ArrayList<>(
            entidadesInvocadas.getOrDefault(uuid, new java.util.ArrayList<>()));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (UUID allayId : spawnedAllays) {
                org.bukkit.entity.Entity entity = Bukkit.getEntity(allayId);
                if (entity != null && !entity.isDead()) {
                    entity.getWorld().spawnParticle(org.bukkit.Particle.NOTE, 
                        entity.getLocation(), 15, 0.5, 0.5, 0.5, 0);
                    entity.remove();
                }
            }
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage("§7Tu allay recolector se ha marchado...");
            }
        }, 10 * 60 * 20L);
        
        // Cooldown 15 minutos
        allayRecolectorCooldowns.put(uuid, System.currentTimeMillis() + 15 * 60 * 1000);
        
        String msg = cantidad > 1 ? "§d§l✧ ¡" + cantidad + " Allays recolectores invocados! §7(10 min)"
                                  : "§d§l✧ ¡Allay recolector invocado! §7(10 min)";
        player.sendMessage(msg);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, 1.0f, 1.0f);
        
        return true;
    }
    
    /**
     * Invoca abejas que atacan a quien dañe al jugador
     * Duración: 5 minutos, Cooldown: 10 minutos
     */
    public boolean invocarAbejas(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (!hasSkill(uuid, Skill.ABEJAS_PROTECTORAS)) {
            player.sendMessage("§c✗ No tienes la habilidad Abejas Protectoras");
            return false;
        }
        
        if (!isSkillEnabled(uuid, Skill.ABEJAS_PROTECTORAS)) {
            player.sendMessage("§c✗ Abejas Protectoras está desactivada");
            return false;
        }
        
        Long cooldownEnd = abejasProtectorasCooldowns.get(uuid);
        if (cooldownEnd != null && System.currentTimeMillis() < cooldownEnd) {
            long remaining = (cooldownEnd - System.currentTimeMillis()) / 1000;
            player.sendMessage("§c✗ Abejas Protectoras en cooldown: §e" + formatTime(remaining));
            return false;
        }
        
        despawnEntidades(uuid);
        
        boolean mejorado = hasSkill(uuid, Skill.DOMADOR_BESTIAS);
        int cantidad = mejorado ? 6 : 3;
        
        for (int i = 0; i < cantidad; i++) {
            org.bukkit.Location loc = player.getLocation().add(
                (Math.random() - 0.5) * 3, 1 + Math.random(), (Math.random() - 0.5) * 3);
            org.bukkit.entity.Bee bee = player.getWorld().spawn(loc, org.bukkit.entity.Bee.class);
            bee.setCustomName("§6" + player.getName() + "'s Bee");
            bee.setCustomNameVisible(false);
            
            if (mejorado) {
                bee.getAttribute(Attribute.MAX_HEALTH).setBaseValue(20);
                bee.setHealth(20);
            }
            
            entidadesInvocadas.computeIfAbsent(uuid, k -> new java.util.ArrayList<>())
                .add(bee.getUniqueId());
        }
        
        // Despawn después de 5 minutos
        Bukkit.getScheduler().runTaskLater(plugin, () -> despawnEntidades(uuid), 5 * 60 * 20L);
        
        // Cooldown 10 minutos
        abejasProtectorasCooldowns.put(uuid, System.currentTimeMillis() + 10 * 60 * 1000);
        
        player.sendMessage("§6§l🐝 ¡" + cantidad + " abejas protectoras invocadas! §7(5 min)");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_BEE_LOOP, 1.0f, 1.0f);
        
        return true;
    }
    
    /**
     * Invoca un gólem de hierro temporal
     * Duración: 5 minutos, Cooldown: 10 minutos
     */
    public boolean invocarGolem(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (!hasSkill(uuid, Skill.GOLEM_PROTECTOR)) {
            player.sendMessage("§c✗ No tienes la habilidad Gólem Protector");
            return false;
        }
        
        Long cooldownEnd = golemProtectorCooldowns.get(uuid);
        if (cooldownEnd != null && System.currentTimeMillis() < cooldownEnd) {
            long remaining = (cooldownEnd - System.currentTimeMillis()) / 1000;
            player.sendMessage("§c✗ Gólem Protector en cooldown: §e" + formatTime(remaining));
            return false;
        }
        
        despawnEntidades(uuid);
        
        // Iniciar task de seguimiento si no está corriendo
        if (petFollowTaskId == -1) {
            startPetFollowTask();
        }
        
        boolean mejorado = hasSkill(uuid, Skill.DOMADOR_BESTIAS);
        
        org.bukkit.entity.IronGolem golem = player.getWorld().spawn(player.getLocation(), org.bukkit.entity.IronGolem.class);
        golem.setPlayerCreated(true);
        golem.setCustomName("§7🛡 " + player.getName());
        golem.setCustomNameVisible(true);
        
        // Prevenir que desaparezca
        golem.setPersistent(true);
        golem.setRemoveWhenFarAway(false);
        
        if (mejorado) {
            golem.getAttribute(Attribute.MAX_HEALTH).setBaseValue(200); // 100 hearts
            golem.setHealth(200);
            golem.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(30);
            golem.setCustomName("§6🛡 " + player.getName() + " §c[+]");
        } else {
            golem.getAttribute(Attribute.MAX_HEALTH).setBaseValue(100);
            golem.setHealth(100);
            golem.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(15);
        }
        
        // Efecto de aparición épico
        golem.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION_EMITTER, 
            golem.getLocation().add(0, 1, 0), 1);
        golem.getWorld().playSound(golem.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_LAND, 0.7f, 0.5f);
        
        entidadesInvocadas.computeIfAbsent(uuid, k -> new java.util.ArrayList<>())
            .add(golem.getUniqueId());
        
        // Task para que el golem ataque mobs hostiles cerca del jugador
        final UUID golemId = golem.getUniqueId();
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) {
                task.cancel();
                return;
            }
            
            org.bukkit.entity.Entity entity = Bukkit.getEntity(golemId);
            if (entity == null || entity.isDead()) {
                task.cancel();
                return;
            }
            
            org.bukkit.entity.IronGolem g = (org.bukkit.entity.IronGolem) entity;
            
            // Hacer que siga al jugador si está lejos
            double dist = g.getLocation().distance(p.getLocation());
            if (dist > 25) {
                g.teleport(p.getLocation().add(2, 0, 2));
            } else if (dist > 10 && g.getTarget() == null) {
                g.getPathfinder().moveTo(p.getLocation());
            }
            
            // Buscar mobs hostiles cercanos para atacar
            if (g.getTarget() == null || g.getTarget().isDead()) {
                for (org.bukkit.entity.Entity e : g.getNearbyEntities(15, 8, 15)) {
                    if (e instanceof org.bukkit.entity.Monster monster && !(e instanceof org.bukkit.entity.Player)) {
                        g.setTarget(monster);
                        break;
                    }
                }
            }
        }, 20L, 20L);
        
        // Despawn después de 5 minutos
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            org.bukkit.entity.Entity entity = Bukkit.getEntity(golemId);
            if (entity != null && !entity.isDead()) {
                entity.getWorld().spawnParticle(org.bukkit.Particle.SMOKE, 
                    entity.getLocation(), 30, 0.5, 1, 0.5, 0.1);
                entity.remove();
            }
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage("§7Tu gólem protector se ha desvanecido...");
            }
        }, 5 * 60 * 20L);
        
        // Cooldown 10 minutos
        golemProtectorCooldowns.put(uuid, System.currentTimeMillis() + 10 * 60 * 1000);
        
        player.sendMessage("§7§l🛡 ¡Gólem de hierro invocado! §7(5 min)");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_IRON_GOLEM_REPAIR, 1.0f, 0.8f);
        
        return true;
    }
    
    /**
     * Invoca Vex vengadores que atacan al objetivo
     * Duración: 30 segundos, Cooldown: 3 minutos
     */
    public boolean invocarVex(Player player, org.bukkit.entity.LivingEntity target) {
        UUID uuid = player.getUniqueId();
        
        if (!hasSkill(uuid, Skill.VEX_VENGADOR)) {
            player.sendMessage("§c✗ No tienes la habilidad Vex Vengador");
            return false;
        }
        
        if (target == null) {
            player.sendMessage("§c✗ Debes mirar a un objetivo");
            return false;
        }
        
        Long cooldownEnd = vexVengadorCooldowns.get(uuid);
        if (cooldownEnd != null && System.currentTimeMillis() < cooldownEnd) {
            long remaining = (cooldownEnd - System.currentTimeMillis()) / 1000;
            player.sendMessage("§c✗ Vex Vengador en cooldown: §e" + formatTime(remaining));
            return false;
        }
        
        boolean mejorado = hasSkill(uuid, Skill.DOMADOR_BESTIAS);
        int cantidad = mejorado ? 4 : 2;
        
        for (int i = 0; i < cantidad; i++) {
            org.bukkit.Location loc = player.getLocation().add(0, 1.5, 0);
            org.bukkit.entity.Vex vex = player.getWorld().spawn(loc, org.bukkit.entity.Vex.class);
            vex.setCustomName("§5" + player.getName() + "'s Vex");
            vex.setCustomNameVisible(false);
            
            // Hacer que ataque al objetivo
            ((org.bukkit.entity.Mob) vex).setTarget(target);
            
            if (mejorado) {
                vex.getAttribute(Attribute.MAX_HEALTH).setBaseValue(30);
                vex.setHealth(30);
            }
            
            // Auto-despawn después de 30 segundos
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!vex.isDead()) vex.remove();
            }, 30 * 20L);
        }
        
        // Cooldown 3 minutos
        vexVengadorCooldowns.put(uuid, System.currentTimeMillis() + 3 * 60 * 1000);
        
        player.sendMessage("§5§l⚔ ¡" + cantidad + " Vex vengadores invocados! §7(30s)");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VEX_CHARGE, 1.0f, 1.0f);
        
        return true;
    }
    
    /**
     * Invoca un mini-warden aliado temporal
     * Duración: 30 segundos, Cooldown: 30 minutos
     */
    public boolean invocarWarden(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (!hasSkill(uuid, Skill.WARDEN_TEMPORAL)) {
            player.sendMessage("§c✗ No tienes la habilidad Warden Temporal");
            return false;
        }
        
        Long cooldownEnd = wardenTemporalCooldowns.get(uuid);
        if (cooldownEnd != null && System.currentTimeMillis() < cooldownEnd) {
            long remaining = (cooldownEnd - System.currentTimeMillis()) / 1000;
            player.sendMessage("§c✗ Warden Temporal en cooldown: §e" + formatTime(remaining));
            return false;
        }
        
        // Invocar un Warden real aliado
        org.bukkit.entity.Warden warden = player.getWorld().spawn(player.getLocation(), org.bukkit.entity.Warden.class);
        warden.setCustomName("§4§l" + player.getName() + "'s Warden");
        warden.setCustomNameVisible(true);
        
        boolean mejorado = hasSkill(uuid, Skill.DOMADOR_BESTIAS);
        double health = mejorado ? 1000 : 500; // Warden normal tiene 500
        double damage = mejorado ? 60 : 30;    // Warden normal hace ~30 daño
        
        warden.getAttribute(Attribute.MAX_HEALTH).setBaseValue(health);
        warden.setHealth(health);
        warden.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(damage);
        
        // Evitar que ataque al jugador dueño
        warden.setAnger(player, 0);
        
        entidadesInvocadas.computeIfAbsent(uuid, k -> new java.util.ArrayList<>())
            .add(warden.getUniqueId());
        
        // Task para hacer que ataque mobs hostiles cercanos
        final UUID wardenId = warden.getUniqueId();
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            org.bukkit.entity.Entity entity = Bukkit.getEntity(wardenId);
            if (entity == null || entity.isDead()) {
                task.cancel();
                return;
            }
            
            org.bukkit.entity.Mob mob = (org.bukkit.entity.Mob) entity;
            for (org.bukkit.entity.Entity e : entity.getNearbyEntities(10, 5, 10)) {
                if (e instanceof org.bukkit.entity.Monster && !(e instanceof org.bukkit.entity.Player)) {
                    mob.setTarget((org.bukkit.entity.LivingEntity) e);
                    break;
                }
            }
        }, 20L, 40L);
        
        // Despawn después de 30 segundos
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            org.bukkit.entity.Entity entity = Bukkit.getEntity(wardenId);
            if (entity != null && !entity.isDead()) {
                entity.remove();
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    p.sendMessage("§4§l☠ Tu Warden temporal se ha desvanecido...");
                }
            }
        }, 30 * 20L);
        
        // Cooldown 30 minutos
        wardenTemporalCooldowns.put(uuid, System.currentTimeMillis() + 30 * 60 * 1000);
        
        player.sendMessage("§4§l☠ ¡WARDEN TEMPORAL INVOCADO! §7(30s)");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WARDEN_ROAR, 1.0f, 1.0f);
        
        return true;
    }
    
    /**
     * Elimina todas las entidades invocadas por un jugador
     */
    public void despawnEntidades(UUID uuid) {
        java.util.List<UUID> entidades = entidadesInvocadas.remove(uuid);
        if (entidades == null) return;
        
        for (UUID entityId : entidades) {
            org.bukkit.entity.Entity entity = Bukkit.getEntity(entityId);
            if (entity != null && !entity.isDead()) {
                entity.remove();
            }
        }
    }
    
    /**
     * Obtiene las entidades invocadas por un jugador
     */
    public java.util.List<UUID> getEntidadesInvocadas(UUID uuid) {
        return entidadesInvocadas.get(uuid);
    }
    
    /**
     * Obtiene TODAS las entidades invocadas de todos los jugadores
     * Usado por el listener para detectar muerte de mascotas
     */
    public Map<UUID, java.util.List<UUID>> getAllInvocaciones() {
        return entidadesInvocadas;
    }
    
    /**
     * Formatea tiempo en segundos a formato legible
     */
    private String formatTime(long seconds) {
        if (seconds < 60) return seconds + "s";
        long min = seconds / 60;
        long sec = seconds % 60;
        return min + "m " + sec + "s";
    }
    
    // Getters para cooldowns de invocación
    public long getLoboCooldown(UUID uuid) {
        Long end = loboCompañeroCooldowns.get(uuid);
        return end == null ? 0 : Math.max(0, end - System.currentTimeMillis());
    }
    
    public long getGatoCooldown(UUID uuid) {
        Long end = gatoGuardianCooldowns.get(uuid);
        return end == null ? 0 : Math.max(0, end - System.currentTimeMillis());
    }
    
    public long getAllayCooldown(UUID uuid) {
        Long end = allayRecolectorCooldowns.get(uuid);
        return end == null ? 0 : Math.max(0, end - System.currentTimeMillis());
    }
    
    public long getAbejasCooldown(UUID uuid) {
        Long end = abejasProtectorasCooldowns.get(uuid);
        return end == null ? 0 : Math.max(0, end - System.currentTimeMillis());
    }
    
    public long getGolemCooldown(UUID uuid) {
        Long end = golemProtectorCooldowns.get(uuid);
        return end == null ? 0 : Math.max(0, end - System.currentTimeMillis());
    }
    
    public long getVexCooldown(UUID uuid) {
        Long end = vexVengadorCooldowns.get(uuid);
        return end == null ? 0 : Math.max(0, end - System.currentTimeMillis());
    }
    
    public long getWardenCooldown(UUID uuid) {
        Long end = wardenTemporalCooldowns.get(uuid);
        return end == null ? 0 : Math.max(0, end - System.currentTimeMillis());
    }
    
    // ==================== SINERGIAS AVANZADAS ====================
    
    /**
     * OMNIPRESENTE - Ver a través de paredes por 5 segundos
     * Cooldown: 2 minutos
     */
    public boolean activateOmnipresente(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (!hasSkill(uuid, Skill.OMNIPRESENTE)) {
            player.sendMessage("§c✗ No tienes la habilidad Omnipresente");
            return false;
        }
        
        Long cooldownEnd = omnipresenteCooldowns.get(uuid);
        if (cooldownEnd != null && System.currentTimeMillis() < cooldownEnd) {
            long remaining = (cooldownEnd - System.currentTimeMillis()) / 1000;
            player.sendMessage("§c✗ Omnipresente en cooldown: §e" + formatTime(remaining));
            return false;
        }
        
        // Efecto de visión espectral (ver mobs a través de paredes)
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.GLOWING, 100, 0, true, false)); // 5s
        
        // También dar visión nocturna
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.NIGHT_VISION, 100, 0, true, false));
        
        // Resaltar TODOS los bloques importantes cercanos (minerales, cofres, spawners)
        scanEverything(player);
        
        // Cooldown 2 minutos
        omnipresenteCooldowns.put(uuid, System.currentTimeMillis() + 2 * 60 * 1000);
        
        player.sendMessage("§5§l✦ §dOmnipresente activado! §7(5s)");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 1.5f);
        
        return true;
    }
    
    private void scanEverything(Player player) {
        org.bukkit.Location center = player.getLocation();
        int radius = 20;
        int foundOres = 0;
        int foundChests = 0;
        int foundSpawners = 0;
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    org.bukkit.block.Block block = center.clone().add(x, y, z).getBlock();
                    org.bukkit.Material type = block.getType();
                    
                    // Minerales
                    if (isValuableOre(type) || type == org.bukkit.Material.DIAMOND_ORE || 
                        type == org.bukkit.Material.DEEPSLATE_DIAMOND_ORE ||
                        type == org.bukkit.Material.ANCIENT_DEBRIS) {
                        player.spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, 
                            block.getLocation().add(0.5, 0.5, 0.5), 3, 0.2, 0.2, 0.2, 0);
                        foundOres++;
                    }
                    // Cofres
                    else if (type == org.bukkit.Material.CHEST || 
                             type == org.bukkit.Material.TRAPPED_CHEST ||
                             type == org.bukkit.Material.BARREL) {
                        player.spawnParticle(org.bukkit.Particle.CRIT, 
                            block.getLocation().add(0.5, 0.5, 0.5), 5, 0.2, 0.2, 0.2, 0);
                        foundChests++;
                    }
                    // Spawners
                    else if (type == org.bukkit.Material.SPAWNER) {
                        player.spawnParticle(org.bukkit.Particle.FLAME, 
                            block.getLocation().add(0.5, 0.5, 0.5), 10, 0.3, 0.3, 0.3, 0.02);
                        foundSpawners++;
                    }
                }
            }
        }
        
        if (foundOres > 0 || foundChests > 0 || foundSpawners > 0) {
            player.sendMessage("§5✦ §dDetectado: §e" + foundOres + " §7minerales, §e" + 
                foundChests + " §7cofres, §e" + foundSpawners + " §7spawners");
        }
    }
    
    /**
     * AVATAR DEL CAOS - Activa TODAS las habilidades toggleables por 30 segundos
     * Cooldown: 1 hora
     */
    public boolean activateAvatarCaos(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (!hasSkill(uuid, Skill.AVATAR_CAOS)) {
            player.sendMessage("§c✗ No tienes la habilidad Avatar del Caos");
            return false;
        }
        
        Long cooldownEnd = avatarCaosCooldowns.get(uuid);
        if (cooldownEnd != null && System.currentTimeMillis() < cooldownEnd) {
            long remaining = (cooldownEnd - System.currentTimeMillis()) / 1000;
            player.sendMessage("§c✗ Avatar del Caos en cooldown: §e" + formatTime(remaining));
            return false;
        }
        
        // Aplicar TODOS los efectos posibles
        player.sendMessage("§4§l⚡ §c§lAVATAR DEL CAOS ACTIVADO!");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.2f);
        
        // Vida extra temporal (absorción)
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.ABSORPTION, 600, 4, true, true)); // 10 hearts extra
        
        // Velocidad máxima
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.SPEED, 600, 2, true, true));
        
        // Fuerza
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.STRENGTH, 600, 1, true, true));
        
        // Haste
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.HASTE, 600, 2, true, true));
        
        // Resistencia
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.RESISTANCE, 600, 1, true, true));
        
        // Regeneración
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.REGENERATION, 600, 1, true, true));
        
        // Visión nocturna
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.NIGHT_VISION, 600, 0, true, true));
        
        // Fire resistance
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE, 600, 0, true, true));
        
        // Water breathing
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.WATER_BREATHING, 600, 0, true, true));
        
        // Dolphins grace
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.DOLPHINS_GRACE, 600, 0, true, true));
        
        // Jump boost
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.JUMP_BOOST, 600, 1, true, true));
        
        // Efectos visuales épicos
        player.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION_EMITTER, player.getLocation(), 1);
        
        // Mensaje a todos los jugadores
        Bukkit.broadcastMessage("§4§l⚡ §c" + player.getName() + " §fha desatado el §4§lAVATAR DEL CAOS§f!");
        
        // Cooldown 1 hora
        avatarCaosCooldowns.put(uuid, System.currentTimeMillis() + 60 * 60 * 1000);
        
        // Mensaje de fin después de 30 segundos
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage("§7El poder del Avatar del Caos se desvanece...");
            }
        }, 600L);
        
        return true;
    }
    
    // ==================== SHUTDOWN ====================
    
    public void shutdown() {
        if (effectTaskId != -1) {
            Bukkit.getScheduler().cancelTask(effectTaskId);
            effectTaskId = -1;
        }
        if (petFollowTaskId != -1) {
            Bukkit.getScheduler().cancelTask(petFollowTaskId);
            petFollowTaskId = -1;
        }
        
        // Despawnear todas las entidades invocadas
        for (UUID uuid : new java.util.ArrayList<>(entidadesInvocadas.keySet())) {
            despawnEntidades(uuid);
        }
        
        saveData();
    }
    
    // ==================== CLASE INTERNA: PlayerSkillData ====================
    
    private static class PlayerSkillData {
        private final Set<Skill> skills;
        private final Set<Skill> disabledToggles;
        private final Map<Skill, SkillLevel> skillLevels; // NUEVO: niveles por skill
        private int xpGastada;
        
        public PlayerSkillData() {
            this.skills = new HashSet<>();
            this.disabledToggles = new HashSet<>();
            this.skillLevels = new HashMap<>();
            this.xpGastada = 0;
        }
        
        public PlayerSkillData(Set<Skill> skills, Set<Skill> disabledToggles, 
                               Map<Skill, SkillLevel> skillLevels, int xpGastada) {
            this.skills = new HashSet<>(skills);
            this.disabledToggles = new HashSet<>(disabledToggles);
            this.skillLevels = new HashMap<>(skillLevels);
            this.xpGastada = xpGastada;
        }
        
        public Set<Skill> getSkills() { return skills; }
        public Set<Skill> getDisabledToggles() { return disabledToggles; }
        public Map<Skill, SkillLevel> getSkillLevels() { return skillLevels; }
        public int getXpGastada() { return xpGastada; }
        
        public boolean hasSkill(Skill skill) { return skills.contains(skill); }
        public void addSkill(Skill skill) { 
            skills.add(skill);
            if (!skillLevels.containsKey(skill)) {
                skillLevels.put(skill, SkillLevel.LEVEL_1); // Nivel inicial
            }
        }
        public void removeSkill(Skill skill) { 
            skills.remove(skill); 
            disabledToggles.remove(skill);
            skillLevels.remove(skill);
        }
        
        public SkillLevel getSkillLevel(Skill skill) {
            return skillLevels.getOrDefault(skill, SkillLevel.LEVEL_1);
        }
        
        public void setSkillLevel(Skill skill, SkillLevel level) {
            if (skills.contains(skill)) {
                skillLevels.put(skill, level);
            }
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
