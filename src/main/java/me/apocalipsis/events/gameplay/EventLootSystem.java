package me.apocalipsis.events.gameplay;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Sistema de recompensas dinámico para eventos
 * 
 * Características:
 * - Items escalados por dificultad (Fácil/Normal/Difícil/Mítico)
 * - Armadura y armas Netherite con Mending garantizado
 * - Drops proporcionales a participación
 * - Loot especial para quien mata al boss
 * - Sistema de rareza (Común/Raro/Épico/Legendario)
 */
public class EventLootSystem {
    
    public enum Difficulty {
        EASY(1.0, "§aFácil"),
        NORMAL(1.5, "§eNormal"),
        HARD(2.0, "§6Difícil"),
        MYTHIC(3.0, "§5Mítico");
        
        public final double multiplier;
        public final String displayName;
        
        Difficulty(double multiplier, String displayName) {
            this.multiplier = multiplier;
            this.displayName = displayName;
        }
    }
    
    public enum Rarity {
        COMMON("§f", "Común", 0),
        RARE("§9", "Raro", 1),
        EPIC("§5", "Épico", 2),
        LEGENDARY("§6§l", "Legendario", 3);
        
        public final String color;
        public final String displayName;
        public final int level;
        
        Rarity(String color, String displayName, int level) {
            this.color = color;
            this.displayName = displayName;
            this.level = level;
        }
    }
    
    private final Random random = new Random();
    private Difficulty difficulty;
    
    public EventLootSystem(Difficulty difficulty) {
        this.difficulty = difficulty;
    }
    
    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }
    
    /**
     * Genera drops para jugadores por participación general
     */
    public List<ItemStack> generateParticipationReward(int participationScore) {
        List<ItemStack> rewards = new ArrayList<>();
        
        // Escalar según participación (0-100)
        int normalizedScore = Math.min(100, participationScore);
        
        // Experiencia base
        int xpAmount = (int) (300 * difficulty.multiplier * (normalizedScore / 100.0));
        
        // Items según participación
        if (normalizedScore >= 80) {
            // Alta participación: Item épico o legendario
            rewards.add(generateRandomArmor(Rarity.EPIC));
            rewards.add(generateEnchantedBook(Rarity.EPIC));
            
            if (random.nextDouble() < 0.3 * difficulty.multiplier) {
                rewards.add(generateRandomWeapon(Rarity.LEGENDARY));
            }
            
        } else if (normalizedScore >= 50) {
            // Media participación: Item raro
            rewards.add(generateRandomArmor(Rarity.RARE));
            rewards.add(generateEnchantedBook(Rarity.RARE));
            
        } else if (normalizedScore >= 20) {
            // Baja participación: Item común mejorado
            rewards.add(generateRandomArmor(Rarity.COMMON));
        }
        
        // Materiales base siempre
        rewards.add(new ItemStack(Material.DIAMOND, (int) (8 * difficulty.multiplier)));
        rewards.add(new ItemStack(Material.NETHERITE_SCRAP, (int) (2 * difficulty.multiplier)));
        rewards.add(new ItemStack(Material.EMERALD, (int) (5 * difficulty.multiplier)));
        
        return rewards;
    }
    
    /**
     * Genera drops para quien mata al boss final
     */
    public List<ItemStack> generateBossKillerReward() {
        List<ItemStack> rewards = new ArrayList<>();
        
        // Arma legendaria garantizada
        rewards.add(generateBossWeapon());
        
        // Armadura legendaria (1-2 piezas)
        rewards.add(generateRandomArmor(Rarity.LEGENDARY));
        
        if (random.nextDouble() < 0.5 * difficulty.multiplier) {
            rewards.add(generateRandomArmor(Rarity.LEGENDARY));
        }
        
        // Libro épico garantizado
        rewards.add(generateEnchantedBook(Rarity.LEGENDARY));
        
        // Materiales premium
        rewards.add(new ItemStack(Material.NETHERITE_INGOT, (int) (4 * difficulty.multiplier)));
        rewards.add(new ItemStack(Material.DIAMOND, (int) (16 * difficulty.multiplier)));
        rewards.add(new ItemStack(Material.EMERALD, (int) (12 * difficulty.multiplier)));
        
        // Item cosmético especial
        rewards.add(generateCosmeticItem());
        
        return rewards;
    }
    
    /**
     * Genera drops por cada mob eliminado durante el evento
     */
    public List<ItemStack> generateMobKillReward() {
        List<ItemStack> rewards = new ArrayList<>();
        
        // Chance de drop basado en dificultad
        double dropChance = 0.15 * difficulty.multiplier;
        
        if (random.nextDouble() < dropChance) {
            // Fragmentos de sombra (custom)
            rewards.add(createFragmentoSombra());
        }
        
        if (random.nextDouble() < dropChance * 0.5) {
            // Item aleatorio común/raro
            Rarity rarity = random.nextBoolean() ? Rarity.COMMON : Rarity.RARE;
            rewards.add(generateRandomArmor(rarity));
        }
        
        return rewards;
    }
    
    /**
     * Genera arma del boss - Netherite Sword legendaria
     */
    private ItemStack generateBossWeapon() {
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = sword.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(Rarity.LEGENDARY.color + "Hoja del Guardián del Umbral");
            meta.setLore(Arrays.asList(
                "§7Una espada forjada en las sombras",
                "§7del propio Guardián.",
                "",
                Rarity.LEGENDARY.color + "❖ " + Rarity.LEGENDARY.displayName + " ❖",
                "§8Dificultad: " + difficulty.displayName
            ));
            sword.setItemMeta(meta);
        }
        
        // Encantos máximos + Mending
        sword.addUnsafeEnchantment(Enchantment.SHARPNESS, 7);
        sword.addEnchantment(Enchantment.LOOTING, 3);
        sword.addEnchantment(Enchantment.SWEEPING_EDGE, 3);
        sword.addEnchantment(Enchantment.FIRE_ASPECT, 2);
        sword.addEnchantment(Enchantment.KNOCKBACK, 2);
        sword.addEnchantment(Enchantment.MENDING, 1);
        sword.addEnchantment(Enchantment.UNBREAKING, 3);
        
        return sword;
    }
    
    /**
     * Genera pieza de armadura según rareza
     */
    private ItemStack generateRandomArmor(Rarity rarity) {
        Material[] armorPieces = {
            Material.NETHERITE_HELMET,
            Material.NETHERITE_CHESTPLATE,
            Material.NETHERITE_LEGGINGS,
            Material.NETHERITE_BOOTS
        };
        
        // En dificultades bajas, puede ser Diamond
        if (difficulty == Difficulty.EASY && rarity == Rarity.COMMON) {
            armorPieces = new Material[]{
                Material.DIAMOND_HELMET,
                Material.DIAMOND_CHESTPLATE,
                Material.DIAMOND_LEGGINGS,
                Material.DIAMOND_BOOTS
            };
        }
        
        Material piece = armorPieces[random.nextInt(armorPieces.length)];
        ItemStack armor = new ItemStack(piece);
        ItemMeta meta = armor.getItemMeta();
        
        if (meta != null) {
            String pieceName = piece.name().toLowerCase().replace("_", " ").replace("netherite ", "").replace("diamond ", "");
            pieceName = pieceName.substring(0, 1).toUpperCase() + pieceName.substring(1);
            
            meta.setDisplayName(rarity.color + pieceName + " del Eco de Sombras");
            meta.setLore(Arrays.asList(
                "§7Armadura infundida con la esencia",
                "§7de las Sombras Largas.",
                "",
                rarity.color + "❖ " + rarity.displayName + " ❖",
                "§8Dificultad: " + difficulty.displayName
            ));
            armor.setItemMeta(meta);
        }
        
        // Encantos según rareza
        applyArmorEnchantments(armor, rarity);
        
        return armor;
    }
    
    /**
     * Aplica encantos a armadura según rareza
     */
    private void applyArmorEnchantments(ItemStack armor, Rarity rarity) {
        // Mending siempre presente
        armor.addEnchantment(Enchantment.MENDING, 1);
        armor.addEnchantment(Enchantment.UNBREAKING, 3);
        
        switch (rarity) {
            case LEGENDARY:
                armor.addUnsafeEnchantment(Enchantment.PROTECTION, 6);
                armor.addEnchantment(Enchantment.THORNS, 3);
                
                if (armor.getType().name().contains("BOOTS")) {
                    armor.addEnchantment(Enchantment.FEATHER_FALLING, 4);
                    armor.addEnchantment(Enchantment.SOUL_SPEED, 3);
                    armor.addEnchantment(Enchantment.DEPTH_STRIDER, 3);
                } else if (armor.getType().name().contains("HELMET")) {
                    armor.addEnchantment(Enchantment.RESPIRATION, 3);
                    armor.addEnchantment(Enchantment.AQUA_AFFINITY, 1);
                }
                break;
                
            case EPIC:
                armor.addEnchantment(Enchantment.PROTECTION, 5);
                armor.addEnchantment(Enchantment.THORNS, 2);
                
                if (armor.getType().name().contains("BOOTS")) {
                    armor.addEnchantment(Enchantment.FEATHER_FALLING, 4);
                    armor.addEnchantment(Enchantment.DEPTH_STRIDER, 3);
                } else if (armor.getType().name().contains("HELMET")) {
                    armor.addEnchantment(Enchantment.RESPIRATION, 3);
                }
                break;
                
            case RARE:
                armor.addEnchantment(Enchantment.PROTECTION, 4);
                
                if (armor.getType().name().contains("BOOTS")) {
                    armor.addEnchantment(Enchantment.FEATHER_FALLING, 4);
                } else if (armor.getType().name().contains("HELMET")) {
                    armor.addEnchantment(Enchantment.RESPIRATION, 2);
                }
                break;
                
            case COMMON:
                armor.addEnchantment(Enchantment.PROTECTION, 3);
                break;
        }
    }
    
    /**
     * Genera arma aleatoria según rareza
     */
    private ItemStack generateRandomWeapon(Rarity rarity) {
        Material[] weapons = {
            Material.NETHERITE_SWORD,
            Material.NETHERITE_AXE,
            Material.BOW,
            Material.CROSSBOW
        };
        
        Material weapon = weapons[random.nextInt(weapons.length)];
        ItemStack item = new ItemStack(weapon);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            String weaponName = weapon.name().toLowerCase().replace("_", " ").replace("netherite ", "");
            weaponName = weaponName.substring(0, 1).toUpperCase() + weaponName.substring(1);
            
            meta.setDisplayName(rarity.color + weaponName + " de las Sombras");
            meta.setLore(Arrays.asList(
                "§7Un arma forjada para enfrentar",
                "§7las sombras más oscuras.",
                "",
                rarity.color + "❖ " + rarity.displayName + " ❖",
                "§8Dificultad: " + difficulty.displayName
            ));
            item.setItemMeta(meta);
        }
        
        // Encantos según rareza y tipo de arma
        item.addEnchantment(Enchantment.MENDING, 1);
        item.addEnchantment(Enchantment.UNBREAKING, 3);
        
        if (weapon == Material.NETHERITE_SWORD) {
            int sharpness = rarity.level + 4; // 4-7
            item.addUnsafeEnchantment(Enchantment.SHARPNESS, sharpness);
            item.addEnchantment(Enchantment.LOOTING, Math.min(3, rarity.level + 1));
            
            if (rarity.level >= 2) {
                item.addEnchantment(Enchantment.SWEEPING_EDGE, 3);
                item.addEnchantment(Enchantment.FIRE_ASPECT, 2);
            }
            
        } else if (weapon == Material.NETHERITE_AXE) {
            int sharpness = rarity.level + 4;
            item.addUnsafeEnchantment(Enchantment.SHARPNESS, sharpness);
            item.addEnchantment(Enchantment.EFFICIENCY, 5);
            
        } else if (weapon == Material.BOW) {
            int power = rarity.level + 4;
            item.addUnsafeEnchantment(Enchantment.POWER, power);
            item.addEnchantment(Enchantment.INFINITY, 1);
            item.addEnchantment(Enchantment.FLAME, 1);
            item.addEnchantment(Enchantment.PUNCH, 2);
            
        } else if (weapon == Material.CROSSBOW) {
            item.addEnchantment(Enchantment.PIERCING, 4);
            item.addEnchantment(Enchantment.QUICK_CHARGE, 3);
            item.addEnchantment(Enchantment.MULTISHOT, 1);
        }
        
        return item;
    }
    
    /**
     * Genera libro encantado según rareza
     */
    private ItemStack generateEnchantedBook(Rarity rarity) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(rarity.color + "Tomo del Eco de Sombras");
            meta.setLore(Arrays.asList(
                "§7Conocimiento arcano extraído",
                "§7de las profundidades sombrías.",
                "",
                rarity.color + "❖ " + rarity.displayName + " ❖"
            ));
            book.setItemMeta(meta);
        }
        
        // Encantos según rareza
        switch (rarity) {
            case LEGENDARY:
                book.addUnsafeEnchantment(Enchantment.PROTECTION, 6);
                book.addEnchantment(Enchantment.MENDING, 1);
                book.addUnsafeEnchantment(Enchantment.SHARPNESS, 7);
                break;
                
            case EPIC:
                book.addEnchantment(Enchantment.PROTECTION, 5);
                book.addEnchantment(Enchantment.MENDING, 1);
                book.addEnchantment(Enchantment.SHARPNESS, 5);
                break;
                
            case RARE:
                book.addEnchantment(Enchantment.PROTECTION, 4);
                book.addEnchantment(Enchantment.MENDING, 1);
                break;
                
            case COMMON:
                book.addEnchantment(Enchantment.UNBREAKING, 3);
                break;
        }
        
        return book;
    }
    
    /**
     * Genera item cosmético especial
     */
    private ItemStack generateCosmeticItem() {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = head.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§5§l⬢ Corona del Conquistador ⬢");
            meta.setLore(Arrays.asList(
                "§7Has derrotado al Guardián del Umbral",
                "§7y demostrado tu valía ante las sombras.",
                "",
                "§8Item cosmético - No proporciona stats",
                "§8Símbolo de prestigio y poder",
                "",
                Rarity.LEGENDARY.color + "❖ " + Rarity.LEGENDARY.displayName + " ❖"
            ));
            head.setItemMeta(meta);
        }
        
        return head;
    }
    
    /**
     * Crea fragmento de sombra para sellar anclas
     */
    private ItemStack createFragmentoSombra() {
        ItemStack fragment = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = fragment.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§8⬢ Fragmento de Sombra ⬢");
            meta.setLore(Arrays.asList(
                "§7Un fragmento de la esencia de",
                "§7las Sombras Largas.",
                "",
                "§8Usado para sellar Anclas del Mundo"
            ));
            fragment.setItemMeta(meta);
        }
        
        return fragment;
    }
    
    /**
     * Otorga recompensas a un jugador
     */
    public void giveRewards(Player player, List<ItemStack> rewards) {
        Location playerLoc = player.getLocation();
        
        for (ItemStack reward : rewards) {
            // Intentar agregar al inventario
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(reward);
            
            // Si no cabe, dropear en el suelo
            if (!leftover.isEmpty()) {
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(playerLoc, drop);
                }
            }
        }
        
        // Efectos visuales
        player.spawnParticle(Particle.TOTEM_OF_UNDYING, playerLoc.clone().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
        player.spawnParticle(Particle.END_ROD, playerLoc.clone().add(0, 1, 0), 20, 0.3, 1, 0.3, 0.05);
        player.playSound(playerLoc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        player.playSound(playerLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f);
    }
}
