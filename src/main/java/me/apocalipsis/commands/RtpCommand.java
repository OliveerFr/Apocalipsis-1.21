package me.apocalipsis.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.managers.CooldownManager;

import java.util.Random;

/**
 * Comando independiente /rtp para teletransporte aleatorio
 * Permite a los jugadores teleportarse a una ubicación aleatoria segura
 */
public class RtpCommand implements CommandExecutor {

    private final Apocalipsis plugin;
    private final Random random = new Random();

    public RtpCommand(Apocalipsis plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c✖ Este comando solo puede ser usado por jugadores.");
            return true;
        }

        Player player = (Player) sender;

        // Verificar permiso
        if (!player.hasPermission("apocalipsis.rtp")) {
            player.sendMessage("§c✖ No tienes permiso para usar este comando.");
            return true;
        }

        // Verificar que esté en overworld
        if (player.getWorld().getEnvironment() != World.Environment.NORMAL) {
            player.sendMessage("§c✖ Solo puedes usar /rtp en el overworld.");
            player.sendMessage("§7Vuelve a la superficie para usar este comando.");
            return true;
        }

        // Verificar cooldown (5 minutos)
        if (!plugin.getCooldownManager().canUse(player, CooldownManager.CooldownType.RANDOM_TP)) {
            plugin.getCooldownManager().sendCooldownMessage(player, CooldownManager.CooldownType.RANDOM_TP);
            return true;
        }

        // Mensaje de búsqueda
        player.sendMessage("");
        player.sendMessage("§8§m                                                    ");
        player.sendMessage("§e§l  ⚡ TELETRANSPORTE ALEATORIO");
        player.sendMessage("");
        player.sendMessage("  §7Buscando ubicación segura...");
        player.sendMessage("  §7Esto puede tardar unos segundos.");
        player.sendMessage("§8§m                                                    ");
        player.sendMessage("");
        
        // Efectos iniciales
        player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 1.5f);
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 30, 0.5, 1, 0.5, 0.1);

        // Ejecutar búsqueda asíncrona para no congelar el servidor
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            World world = player.getWorld();
            Location safeLoc = findRandomSafeLocation(world);

            // Volver al thread principal para teleportar
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (safeLoc == null) {
                    player.sendMessage("");
                    player.sendMessage("§c✖ No se pudo encontrar una ubicación segura.");
                    player.sendMessage("§7Intenta de nuevo en unos momentos.");
                    player.sendMessage("");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }

                // Aplicar cooldown
                plugin.getCooldownManager().applyCooldown(player, CooldownManager.CooldownType.RANDOM_TP);

                // Teleportar con efectos
                player.teleport(safeLoc);
                
                // Mensaje de éxito
                player.sendMessage("");
                player.sendMessage("§8§m                                                    ");
                player.sendMessage("§a§l  ✓ TELETRANSPORTE EXITOSO");
                player.sendMessage("");
                player.sendMessage("  §7Coordenadas: §e" + safeLoc.getBlockX() + "§7, §e" + 
                    safeLoc.getBlockY() + "§7, §e" + safeLoc.getBlockZ());
                player.sendMessage("  §7Distancia del spawn: §e" + 
                    (int) safeLoc.distance(world.getSpawnLocation()) + " bloques");
                player.sendMessage("");
                player.sendMessage("  §7Próximo uso: §e5 minutos");
                player.sendMessage("§8§m                                                    ");
                player.sendMessage("");

                // Efectos visuales y sonoros
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2.0f);
                player.getWorld().spawnParticle(Particle.PORTAL, safeLoc, 100, 1, 1, 1, 0.2);
                player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, safeLoc, 50, 0.5, 1, 0.5, 0.1);
                player.getWorld().spawnParticle(Particle.FIREWORK, safeLoc, 30, 0.5, 0.5, 0.5, 0.1);

                // Log para admins
                plugin.getLogger().info("[RTP] " + player.getName() + " teleportado a " +
                    safeLoc.getBlockX() + ", " + safeLoc.getBlockY() + ", " + safeLoc.getBlockZ() +
                    " (Distancia: " + (int) safeLoc.distance(world.getSpawnLocation()) + " bloques)");
            });
        });

        return true;
    }

    /**
     * Encuentra una ubicación aleatoria segura en el mundo
     * @param world Mundo donde buscar
     * @return Ubicación segura encontrada o null
     */
    private Location findRandomSafeLocation(World world) {
        // Rango de búsqueda: 1000 a 5000 bloques del spawn
        int minRadius = 1000;
        int maxRadius = 5000;

        // Intentar hasta 15 veces encontrar ubicación segura
        for (int attempt = 0; attempt < 15; attempt++) {
            // Generar coordenadas aleatorias
            double angle = random.nextDouble() * 2 * Math.PI;
            int distance = minRadius + random.nextInt(maxRadius - minRadius);

            int randomX = (int) (world.getSpawnLocation().getX() + distance * Math.cos(angle));
            int randomZ = (int) (world.getSpawnLocation().getZ() + distance * Math.sin(angle));

            // Obtener bloque más alto en esas coordenadas
            Location checkLoc = world.getHighestBlockAt(randomX, randomZ).getLocation().add(0, 1, 0);

            // Verificar que la ubicación sea segura
            if (isLocationSafeForRTP(checkLoc)) {
                plugin.getLogger().info("[RTP] Ubicación segura encontrada en intento " + (attempt + 1) + 
                    " - Distancia: " + (int) checkLoc.distance(world.getSpawnLocation()) + " bloques");
                return checkLoc;
            }
        }

        plugin.getLogger().warning("[RTP] No se encontró ubicación segura después de 15 intentos");
        return null;
    }

    /**
     * Verifica si una ubicación es segura para RTP
     * @param location Ubicación a verificar
     * @return true si es segura
     */
    private boolean isLocationSafeForRTP(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }

        World world = location.getWorld();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        // Verificar límites de altura
        if (y < 60 || y > world.getMaxHeight() - 10) {
            return false;
        }

        // Obtener bloques relevantes
        Material ground = world.getBlockAt(x, y - 1, z).getType();
        Material feet = world.getBlockAt(x, y, z).getType();
        Material head = world.getBlockAt(x, y + 1, z).getType();

        // Verificar que el suelo sea sólido
        if (!ground.isSolid()) {
            return false;
        }

        // Verificar materiales peligrosos en el suelo
        if (ground == Material.LAVA || ground == Material.MAGMA_BLOCK || 
            ground == Material.CACTUS || ground == Material.FIRE || 
            ground == Material.SOUL_FIRE || ground == Material.CAMPFIRE ||
            ground == Material.SOUL_CAMPFIRE) {
            return false;
        }

        // Verificar que el espacio para el jugador esté vacío
        if (feet.isSolid() || head.isSolid()) {
            return false;
        }

        // Verificar materiales peligrosos en el espacio del jugador
        if (feet == Material.LAVA || feet == Material.WATER || feet == Material.FIRE ||
            feet == Material.SOUL_FIRE || head == Material.LAVA || head == Material.WATER) {
            return false;
        }

        // Verificar que no haya bloques peligrosos alrededor (radio 2)
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                Material nearby = world.getBlockAt(x + dx, y - 1, z + dz).getType();
                if (nearby == Material.LAVA) {
                    return false;
                }
            }
        }

        // Verificar bioma (evitar océanos profundos)
        org.bukkit.block.Biome biome = world.getBiome(x, y, z);
        if (biome.toString().contains("OCEAN") && !biome.toString().contains("WARM")) {
            // Permitir océanos cálidos pero no profundos
            if (biome.toString().contains("DEEP")) {
                return false;
            }
        }

        // Verificar que no esté demasiado cerca de lava (radio 5)
        int lavaCount = 0;
        for (int dx = -5; dx <= 5; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -5; dz <= 5; dz++) {
                    if (world.getBlockAt(x + dx, y + dy, z + dz).getType() == Material.LAVA) {
                        lavaCount++;
                        if (lavaCount > 3) {
                            return false; // Demasiada lava cerca
                        }
                    }
                }
            }
        }

        // Todo bien, ubicación segura
        return true;
    }
}
