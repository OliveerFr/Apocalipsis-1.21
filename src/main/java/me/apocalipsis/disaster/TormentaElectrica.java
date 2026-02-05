package me.apocalipsis.disaster;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.disaster.adapters.PerformanceAdapter;
import me.apocalipsis.state.TimeService;
import me.apocalipsis.ui.MessageBus;
import me.apocalipsis.ui.SoundUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ⚡ TORMENTA ELÉCTRICA CAÓTICA - Desastre de Ciclo 2
 * 
 * Rayos impredecibles que electrifican el terreno.
 * Reemplazo de Lluvia de Fuego con mecánicas completamente nuevas.
 */
public class TormentaElectrica extends DisasterBase {

    private final Random random = new Random();
    private final Set<Location> zonasIonizadas = ConcurrentHashMap.newKeySet();
    private final Map<Location, Long> ionizacionExpiracion = new ConcurrentHashMap<>();
    private final Map<UUID, Long> ultimoEMP = new ConcurrentHashMap<>();
    
    // Config
    private int rayosAdvertencia;
    private double rayosDamage;
    private boolean rayosIncendiar;
    
    private boolean cadenasEnabled;
    private int cadenasRadio;
    private int cadenasMaxSaltos;
    private double cadenasDamageBase;
    private double cadenasReduccion;
    
    private boolean sobrecargaEnabled;
    private double sobrecargaMultiplicador;
    private Set<Material> itemsMetalicos = new HashSet<>();
    
    private boolean zonasEnabled;
    private int zonasRadio;
    private int zonasDuracion;
    private double zonasDamagePorSeg;
    
    private boolean empEnabled;
    private boolean empSoloPico;
    private int empIntervalo;
    private int empRadio;
    private int empDuracion;
    
    private boolean cortocircuitoEnabled;
    private int cortocircuitoRadio;
    private int cortocircuitoDuracion;
    
    // Protección
    private boolean lightningRodsEnabled;
    private int lightningRodsRadio;
    
    private boolean aislanteEnabled;
    private double aislanteReduccion;
    private Set<Material> materialesAislantes = new HashSet<>();
    
    private boolean aguaVulnerabilidad;
    private double aguaMultiplicadorDamage;
    
    // Tasks
    private BukkitRunnable rayosTask;
    private BukkitRunnable zonasTask;
    private BukkitRunnable empTask;
    
    // Fases
    private boolean fasesEnabled;
    private double faseMultiplicador = 1.0;

    public TormentaElectrica(Apocalipsis plugin, MessageBus messageBus, SoundUtil soundUtil,
                            TimeService timeService, PerformanceAdapter performanceAdapter) {
        super(plugin, messageBus, soundUtil, timeService, performanceAdapter, "tormenta_electrica");
        loadConfiguration();
    }

    private void loadConfiguration() {
        ConfigurationSection cfg = plugin.getConfigManager().getDesastresConfig()
            .getConfigurationSection("desastres.tormenta_electrica");
        
        if (cfg == null) {
            plugin.getLogger().warning("[TormentaElectrica] Configuración no encontrada, usando valores default");
            setDefaultConfig();
            return;
        }
        
        // Duración
        int duracionSegundos = cfg.getInt("duracion_segundos", 70);
        setMaxTicks(duracionSegundos);
        
        // Rayos
        ConfigurationSection ray = cfg.getConfigurationSection("rayos_dirigidos");
        if (ray != null) {
            rayosAdvertencia = ray.getInt("advertencia_ticks", 60);
            rayosDamage = ray.getDouble("damage", 3.0);
            rayosIncendiar = ray.getBoolean("incendiar_bloques", true);
        } else {
            rayosAdvertencia = 60;
            rayosDamage = 3.0;
            rayosIncendiar = true;
        }
        
        // Cadenas
        ConfigurationSection cad = cfg.getConfigurationSection("cadenas_electricas");
        if (cad != null) {
            cadenasEnabled = cad.getBoolean("enabled", true);
            cadenasRadio = cad.getInt("radio_salto", 5);
            cadenasMaxSaltos = cad.getInt("max_saltos", 3);
            cadenasDamageBase = cad.getDouble("damage_base", 3.0);
            cadenasReduccion = cad.getDouble("reduccion_por_salto", 1.0);
        } else {
            cadenasEnabled = true;
            cadenasRadio = 5;
            cadenasMaxSaltos = 3;
            cadenasDamageBase = 3.0;
            cadenasReduccion = 1.0;
        }
        
        // Sobrecarga
        ConfigurationSection sob = cfg.getConfigurationSection("sobrecarga_electrica");
        if (sob != null) {
            sobrecargaEnabled = sob.getBoolean("enabled", true);
            sobrecargaMultiplicador = sob.getDouble("multiplicador_atraccion", 3.0);
            List<String> items = sob.getStringList("items_metalicos");
            for (String item : items) {
                try {
                    itemsMetalicos.add(Material.valueOf(item));
                } catch (IllegalArgumentException e) {
                    // Ignorar
                }
            }
        } else {
            sobrecargaEnabled = true;
            sobrecargaMultiplicador = 3.0;
        }
        
        // Zonas
        ConfigurationSection zon = cfg.getConfigurationSection("zonas_ionizadas");
        if (zon != null) {
            zonasEnabled = zon.getBoolean("enabled", true);
            zonasRadio = zon.getInt("radio", 4);
            zonasDuracion = zon.getInt("duracion_ticks", 300);
            zonasDamagePorSeg = zon.getDouble("damage_por_segundo", 0.5);
        } else {
            zonasEnabled = true;
            zonasRadio = 4;
            zonasDuracion = 300;
            zonasDamagePorSeg = 0.5;
        }
        
        // EMP
        ConfigurationSection emp = cfg.getConfigurationSection("emp_pulse");
        if (emp != null) {
            empEnabled = emp.getBoolean("enabled", true);
            empSoloPico = emp.getBoolean("solo_en_pico", true);
            empIntervalo = emp.getInt("intervalo_ticks", 900);
            empRadio = emp.getInt("radio", 12);
            empDuracion = emp.getInt("duracion_ticks", 200);
        } else {
            empEnabled = true;
            empSoloPico = true;
            empIntervalo = 900;
            empRadio = 12;
            empDuracion = 200;
        }
        
        // Cortocircuito
        ConfigurationSection cor = cfg.getConfigurationSection("cortocircuito");
        if (cor != null) {
            cortocircuitoEnabled = cor.getBoolean("enabled", true);
            cortocircuitoRadio = cor.getInt("radio_desde_rayo", 6);
            cortocircuitoDuracion = cor.getInt("duracion_ticks", 160);
        } else {
            cortocircuitoEnabled = true;
            cortocircuitoRadio = 6;
            cortocircuitoDuracion = 160;
        }
        
        // Protección
        ConfigurationSection prot = cfg.getConfigurationSection("proteccion");
        if (prot != null) {
            ConfigurationSection lr = prot.getConfigurationSection("lightning_rods");
            if (lr != null) {
                lightningRodsEnabled = lr.getBoolean("enabled", true);
                lightningRodsRadio = lr.getInt("radio_proteccion", 16);
            } else {
                lightningRodsEnabled = true;
                lightningRodsRadio = 16;
            }
            
            ConfigurationSection ais = prot.getConfigurationSection("bloques_aislantes");
            if (ais != null) {
                aislanteEnabled = ais.getBoolean("enabled", true);
                aislanteReduccion = ais.getDouble("reduccion_probabilidad", 0.70);
                List<String> mats = ais.getStringList("materiales");
                for (String mat : mats) {
                    try {
                        materialesAislantes.add(Material.valueOf(mat));
                    } catch (IllegalArgumentException e) {
                        // Ignorar
                    }
                }
            } else {
                aislanteEnabled = true;
                aislanteReduccion = 0.70;
            }
            
            ConfigurationSection agua = prot.getConfigurationSection("agua_vulnerabilidad");
            if (agua != null) {
                aguaVulnerabilidad = true;
                aguaMultiplicadorDamage = agua.getDouble("multiplicador_damage", 2.0);
            } else {
                aguaVulnerabilidad = true;
                aguaMultiplicadorDamage = 2.0;
            }
        }
        
        // Fases
        ConfigurationSection fases = cfg.getConfigurationSection("fases");
        if (fases != null) {
            fasesEnabled = fases.getBoolean("enabled", true);
        } else {
            fasesEnabled = true;
        }
    }
    
    private void setDefaultConfig() {
        setMaxTicks(70);
        rayosAdvertencia = 60;
        rayosDamage = 3.0;
        rayosIncendiar = true;
        
        cadenasEnabled = true;
        cadenasRadio = 5;
        cadenasMaxSaltos = 3;
        cadenasDamageBase = 3.0;
        cadenasReduccion = 1.0;
        
        sobrecargaEnabled = true;
        sobrecargaMultiplicador = 3.0;
        
        zonasEnabled = true;
        zonasRadio = 4;
        zonasDuracion = 300;
        zonasDamagePorSeg = 0.5;
        
        empEnabled = true;
        empSoloPico = true;
        empIntervalo = 900;
        empRadio = 12;
        empDuracion = 200;
        
        cortocircuitoEnabled = true;
        cortocircuitoRadio = 6;
        cortocircuitoDuracion = 160;
        
        lightningRodsEnabled = true;
        lightningRodsRadio = 16;
        
        aislanteEnabled = true;
        aislanteReduccion = 0.70;
        
        aguaVulnerabilidad = true;
        aguaMultiplicadorDamage = 2.0;
        
        fasesEnabled = true;
    }

    @Override
    protected void onStart() {
        zonasIonizadas.clear();
        ionizacionExpiracion.clear();
        ultimoEMP.clear();
        faseMultiplicador = 1.0;
        
        // Anuncios
        messageBus.broadcast("§8§m                                                    ", "disaster");
        messageBus.broadcast("§e§l      ⚡ TORMENTA ELÉCTRICA ⚡", "disaster");
        messageBus.broadcast("", "disaster");
        messageBus.broadcast("  §f§l¡Rayos electrifican el terreno!", "disaster");
        messageBus.broadcast("  §7Cadenas eléctricas saltan entre jugadores", "disaster");
        messageBus.broadcast("", "disaster");
        messageBus.broadcast("  §e⚠ Lightning Rods desvían rayos", "disaster");
        messageBus.broadcast("  §7Bloques aislantes protegen", "disaster");
        messageBus.broadcast("§8§m                                                    ", "disaster");
        
        // Iniciar tasks
        startRayos();
        if (zonasEnabled) startZonasMonitor();
        if (empEnabled) startEMP();
        
        plugin.getLogger().info("[TormentaElectrica] Desastre iniciado");
    }

    @Override
    protected void onTick() {
        // Actualizar multiplicador de fase
        if (fasesEnabled) {
            actualizarFase();
        }
        
        // Limpiar zonas expiradas
        long now = System.currentTimeMillis();
        zonasIonizadas.removeIf(loc -> {
            Long expira = ionizacionExpiracion.get(loc);
            return expira != null && now > expira;
        });
    }

    @Override
    protected void onStop() {
        // Cancelar tasks
        if (rayosTask != null) {
            rayosTask.cancel();
            rayosTask = null;
        }
        if (zonasTask != null) {
            zonasTask.cancel();
            zonasTask = null;
        }
        if (empTask != null) {
            empTask.cancel();
            empTask = null;
        }
        
        // Limpiar data
        zonasIonizadas.clear();
        ionizacionExpiracion.clear();
        ultimoEMP.clear();
        
        messageBus.broadcast("§e§l⚡ §fLa tormenta eléctrica se disipa...", "disaster");
        
        plugin.getLogger().info("[TormentaElectrica] Desastre detenido");
    }
    
    @Override
    public void applyEffects(Player player) {
        // Daño de zonas ionizadas
        if (zonasEnabled) {
            for (Location zona : zonasIonizadas) {
                if (player.getLocation().distance(zona) < zonasRadio) {
                    player.damage(zonasDamagePorSeg / 20.0);
                    player.spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation(), 5, 0.5, 1, 0.5, 0.1);
                    player.sendActionBar("§e⚡ ¡Zona ionizada!");
                    break;
                }
            }
        }
    }
    
    @Override
    protected String getDisasterName() {
        return "TORMENTA ELÉCTRICA";
    }
    
    @Override
    protected String[] getPhaseNames() {
        return new String[] {
            "§7Carga Eléctrica",
            "§eRayos Dispersos",
            "§6Tormenta Intensa",
            "§c§lDESCARGAS MASIVAS",
            "§4§l¡CAOS ELÉCTRICO!"
        };
    }
    
    // ============================================
    // MECÁNICAS ESPECÍFICAS
    // ============================================
    
    private void actualizarFase() {
        double progreso = (double) tickCounter / maxTicks;
        
        if (progreso < 0.30) {
            // INICIO
            faseMultiplicador = 0.8;
        } else if (progreso < 0.70) {
            // PICO
            faseMultiplicador = 1.5;
        } else {
            // DECLIVE
            faseMultiplicador = 0.9;
        }
    }
    
    private String getCurrentPhaseString() {
        double progreso = (double) tickCounter / maxTicks;
        
        if (progreso < 0.30) return "INICIO";
        else if (progreso < 0.70) return "PICO";
        else return "DECLIVE";
    }
    
    private void startRayos() {
        rayosTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive()) {
                    cancel();
                    return;
                }
                
                String phase = getCurrentPhaseString();
                int cantidad = getCantidadRayosPorFase(phase);
                
                for (int i = 0; i < cantidad; i++) {
                    lanzarRayo();
                }
            }
        };
        
        long intervalo = getIntervaloRayosPorFase();
        rayosTask.runTaskTimer(plugin, 40L, intervalo);
    }
    
    private int getCantidadRayosPorFase(String phase) {
        switch (phase) {
            case "INICIO": return 1;
            case "PICO": return 4;
            case "DECLIVE": return 2;
            default: return 1;
        }
    }
    
    private long getIntervaloRayosPorFase() {
        String phase = getCurrentPhaseString();
        switch (phase) {
            case "INICIO": return 240L; // 12s
            case "PICO": return 160L;   // 8s
            case "DECLIVE": return 300L; // 15s
            default: return 200L;
        }
    }
    
    private void lanzarRayo() {
        List<Player> jugadores = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!isPlayerExempt(p)) {
                jugadores.add(p);
            }
        }
        
        if (jugadores.isEmpty()) return;
        
        // Seleccionar target considerando sobrecarga
        Player target = seleccionarTargetConSobrecarga(jugadores);
        if (target == null) return;
        
        Location impactoInicial = target.getLocation();
        
        // Verificar Lightning Rod cercano
        final Location impacto;
        if (lightningRodsEnabled && tieneLightningRodCerca(impactoInicial)) {
            Location rodLoc = encontrarLightningRodCercano(impactoInicial);
            if (rodLoc != null) {
                impacto = rodLoc;
                target.sendActionBar("§e⚡ §aLightning Rod desvió el rayo §e⚡");
                target.playSound(target.getLocation(), Sound.BLOCK_BELL_USE, 0.5f, 2.0f);
                target.spawnParticle(Particle.HAPPY_VILLAGER, target.getLocation(), 10, 1, 1, 1, 0);
            } else {
                impacto = impactoInicial;
            }
        } else {
            impacto = impactoInicial;
        }
        
        // Advertencia
        advertirRayo(impacto);
        
        // Lanzar rayo después de advertencia
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive()) {
                    cancel();
                    return;
                }
                ejecutarRayo(impacto);
            }
        }.runTaskLater(plugin, rayosAdvertencia);
    }
    
    private Player seleccionarTargetConSobrecarga(List<Player> jugadores) {
        if (!sobrecargaEnabled || jugadores.isEmpty()) {
            return jugadores.get(random.nextInt(jugadores.size()));
        }
        
        // Priorizar jugadores con metal
        List<Player> conMetal = new ArrayList<>();
        for (Player p : jugadores) {
            if (tieneItemsMetalicos(p)) {
                for (int i = 0; i < sobrecargaMultiplicador; i++) {
                    conMetal.add(p);
                }
                // Advertir al jugador CON DIAGNÓSTICO
                String tipoMetal = diagnosticarArmaduraMetal(p);
                p.sendActionBar("§c§l⚡ PELIGRO §8| §7" + tipoMetal + " §catrae rayos §7(§c+" + (sobrecargaMultiplicador * 100 - 100) + "%§7)");
                p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.3f, 2.0f);
                p.spawnParticle(Particle.ELECTRIC_SPARK, p.getLocation(), 10, 0.5, 1, 0.5, 0.1);
                
                // Consejo cada 15 segundos
                if (tickCounter % 300 == 0) {
                    p.sendMessage("§c⚡ §7TORMENTA ELÉCTRICA: Tu armadura metálica atrae rayos");
                    p.sendMessage("§7  §8→ §7Quítate la armadura de §cHierro/Oro/Chainmail§7 temporalmente");
                    p.sendMessage("§7  §8→ §7O coloca un §eLightning Rod§7 en radio de §e" + lightningRodsRadio + " bloques§7 para desviar rayos");
                }
            } else {
                conMetal.add(p);
                
                // Feedback positivo para jugadores sin metal
                if (tickCounter % 600 == 0) {
                    p.sendActionBar("§a§l✓ SIN METAL §8| §7Probabilidad normal de rayos");
                }
            }
        }
        
        return conMetal.get(random.nextInt(conMetal.size()));
    }
    
    /**
     * Diagnostica qué armadura metálica está usando el jugador
     */
    private String diagnosticarArmaduraMetal(Player p) {
        java.util.List<String> piezasMetal = new java.util.ArrayList<>();
        
        if (p.getInventory().getHelmet() != null) {
            Material tipo = p.getInventory().getHelmet().getType();
            if (tipo.name().contains("IRON")) piezasMetal.add("casco hierro");
            else if (tipo.name().contains("GOLDEN")) piezasMetal.add("casco oro");
            else if (tipo.name().contains("CHAINMAIL")) piezasMetal.add("casco chainmail");
        }
        if (p.getInventory().getChestplate() != null) {
            Material tipo = p.getInventory().getChestplate().getType();
            if (tipo.name().contains("IRON")) piezasMetal.add("peto hierro");
            else if (tipo.name().contains("GOLDEN")) piezasMetal.add("peto oro");
            else if (tipo.name().contains("CHAINMAIL")) piezasMetal.add("peto chainmail");
        }
        if (p.getInventory().getLeggings() != null) {
            Material tipo = p.getInventory().getLeggings().getType();
            if (tipo.name().contains("IRON")) piezasMetal.add("pantalones hierro");
            else if (tipo.name().contains("GOLDEN")) piezasMetal.add("pantalones oro");
            else if (tipo.name().contains("CHAINMAIL")) piezasMetal.add("pantalones chainmail");
        }
        if (p.getInventory().getBoots() != null) {
            Material tipo = p.getInventory().getBoots().getType();
            if (tipo.name().contains("IRON")) piezasMetal.add("botas hierro");
            else if (tipo.name().contains("GOLDEN")) piezasMetal.add("botas oro");
            else if (tipo.name().contains("CHAINMAIL")) piezasMetal.add("botas chainmail");
        }
        
        if (piezasMetal.isEmpty()) {
            return "armadura metálica";
        } else if (piezasMetal.size() == 1) {
            return piezasMetal.get(0);
        } else if (piezasMetal.size() == 4) {
            return "armadura completa de metal";
        } else {
            return piezasMetal.size() + " piezas metálicas";
        }
    }
    
    private boolean tieneLightningRodCerca(Location loc) {
        for (int x = -lightningRodsRadio; x <= lightningRodsRadio; x++) {
            for (int y = -lightningRodsRadio; y <= lightningRodsRadio; y++) {
                for (int z = -lightningRodsRadio; z <= lightningRodsRadio; z++) {
                    Block b = loc.getWorld().getBlockAt(
                        loc.getBlockX() + x,
                        loc.getBlockY() + y,
                        loc.getBlockZ() + z
                    );
                    if (b.getType() == Material.LIGHTNING_ROD) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private Location encontrarLightningRodCercano(Location loc) {
        for (int x = -lightningRodsRadio; x <= lightningRodsRadio; x++) {
            for (int y = -lightningRodsRadio; y <= lightningRodsRadio; y++) {
                for (int z = -lightningRodsRadio; z <= lightningRodsRadio; z++) {
                    Block b = loc.getWorld().getBlockAt(
                        loc.getBlockX() + x,
                        loc.getBlockY() + y,
                        loc.getBlockZ() + z
                    );
                    if (b.getType() == Material.LIGHTNING_ROD) {
                        return b.getLocation();
                    }
                }
            }
        }
        return null;
    }
    
    private boolean tieneItemsMetalicos(Player p) {
        for (Material item : itemsMetalicos) {
            if (p.getInventory().getHelmet() != null && p.getInventory().getHelmet().getType() == item) return true;
            if (p.getInventory().getChestplate() != null && p.getInventory().getChestplate().getType() == item) return true;
            if (p.getInventory().getLeggings() != null && p.getInventory().getLeggings().getType() == item) return true;
            if (p.getInventory().getBoots() != null && p.getInventory().getBoots().getType() == item) return true;
            if (p.getInventory().getItemInMainHand().getType() == item) return true;
        }
        return false;
    }
    
    private void advertirRayo(Location loc) {
        // CINEMÁTICO: Sonidos épicos de advertencia
        loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
        loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6f, 1.8f);
        
        // CINEMÁTICO: Advertencia visual mejorada con columna épica de 40 bloques
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= rayosAdvertencia / 10 || !isActive()) {
                    cancel();
                    return;
                }
                
                // Columna de advertencia eléctrica de 40 bloques
                for (int y = 0; y < 40; y++) {
                    Location particleLoc = loc.clone().add(0, y, 0);
                    
                    // Partículas eléctricas densas
                    loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particleLoc, 15, 0.4, 0.3, 0.4, 0.15);
                    loc.getWorld().spawnParticle(Particle.FIREWORK, particleLoc, 5, 0.3, 0.2, 0.3, 0.05);
                    loc.getWorld().spawnParticle(Particle.CLOUD, particleLoc, 3, 0.4, 0.2, 0.4, 0.02);
                    
                    // Partículas amarillas brillantes cada 2 bloques
                    if (y % 2 == 0) {
                        loc.getWorld().spawnParticle(Particle.DUST, particleLoc, 6, 0.3, 0.2, 0.3,
                            new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 255, 100), 2.0f));
                    }
                }
                
                // CINEMÁTICO: Anillos expansivos en el suelo
                for (int angle = 0; angle < 360; angle += 20) {
                    double rad = Math.toRadians(angle);
                    double radius = 2 + (ticks * 0.2);
                    double x = loc.getX() + radius * Math.cos(rad);
                    double z = loc.getZ() + radius * Math.sin(rad);
                    Location ringLoc = new Location(loc.getWorld(), x, loc.getY(), z);
                    loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, ringLoc, 2, 0.1, 0, 0.1, 0);
                }
                
                // Sonido eléctrico pulsante
                if (ticks % 2 == 0) {
                    loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.5f);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 10L);
        
        // CINEMÁTICO: Advertir a jugadores cercanos con títulos
        for (Player player : loc.getWorld().getPlayers()) {
            if (isPlayerExempt(player)) continue;
            if (player.getLocation().distance(loc) < 20) {
                player.sendTitle("§e§l⚡ RAYO", "§f§l3 segundos...", 10, 40, 10);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                
                // Countdown 2, 1
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.sendTitle("§e§l2", "", 0, 15, 5);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                }, 20L);
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.sendTitle("§e§l1", "", 0, 15, 5);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                }, 40L);
            }
        }
    }
    
    private void ejecutarRayo(Location loc) {
        // CINEMÁTICO: Efectos visuales épicos de impacto ANTES del rayo
        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 3, 1, 1, 1, 0);
        loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 80, 2, 2, 2, 0.3);
        loc.getWorld().spawnParticle(Particle.FIREWORK, loc, 40, 1.5, 1.5, 1.5, 0.2);
        loc.getWorld().spawnParticle(Particle.FLASH, loc, 2, 0.5, 0.5, 0.5, 0);
        
        // CINEMÁTICO: Ondas de choque eléctricas expansivas
        for (int radius = 1; radius <= 8; radius++) {
            final int r = radius;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (int angle = 0; angle < 360; angle += 15) {
                    double rad = Math.toRadians(angle);
                    double x = loc.getX() + r * Math.cos(rad);
                    double z = loc.getZ() + r * Math.sin(rad);
                    Location shockwaveLoc = new Location(loc.getWorld(), x, loc.getY(), z);
                    
                    loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, shockwaveLoc, 5, 0.3, 0.5, 0.3, 0.05);
                    loc.getWorld().spawnParticle(Particle.DUST, shockwaveLoc, 3, 0.2, 0.3, 0.2,
                        new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 255, 150), 1.5f));
                }
            }, (long) r * 2);
        }
        
        // Rayo vanilla
        loc.getWorld().strikeLightning(loc);
        
        // CINEMÁTICO: Sonidos épicos combinados
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);
        loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1.0f);
        loc.getWorld().playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 1.5f);
        
        // Damage al target
        List<Player> jugadores = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!isPlayerExempt(p)) {
                jugadores.add(p);
            }
        }
        
        for (Player p : jugadores) {
            double distance = p.getLocation().distance(loc);
            
            // CINEMÁTICO: Título de impacto para jugadores cercanos
            if (distance < 15) {
                p.sendTitle("§e§l⚡ IMPACTO ⚡", "", 5, 20, 10);
                p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 1.2f);
            }
            
            if (distance < 3) {
                double damage = rayosDamage * faseMultiplicador;
                boolean tieneProteccion = false;
                String proteccionMsg = "";
                
                // Bloques aislantes
                if (aislanteEnabled && estaSobreBloqueAislante(p)) {
                    damage *= (1.0 - aislanteReduccion);
                    tieneProteccion = true;
                    proteccionMsg = "§6🛡 Bloque Aislante";
                    p.spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation(), 5, 0.5, 0.5, 0.5, 0);
                    p.playSound(p.getLocation(), Sound.BLOCK_WOOL_BREAK, 0.5f, 1.5f);
                }
                
                // Multiplicar por agua (vulnerabilidad)
                if (aguaVulnerabilidad && p.getLocation().getBlock().getType() == Material.WATER) {
                    damage *= aguaMultiplicadorDamage;
                    p.sendActionBar("§c⚡ ¡AGUA = x2 DAÑO! §c⚡");
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT_DROWN, 1.0f, 0.8f);
                } else if (tieneProteccion) {
                    double reduccion = aislanteReduccion * 100;
                    p.sendActionBar(String.format("%s §a-%.0f%%", proteccionMsg, reduccion));
                }
                
                p.damage(damage);
                p.setFireTicks(60);
                
                // Cadenas eléctricas
                if (cadenasEnabled) {
                    aplicarCadenasElectricas(p, 0);
                }
            }
        }
        
        // Zona ionizada
        if (zonasEnabled) {
            crearZonaIonizada(loc);
        }
        
        // Cortocircuito
        if (cortocircuitoEnabled) {
            aplicarCortocircuito(loc);
        }
    }
    
    private void aplicarCadenasElectricas(Player origen, int saltoActual) {
        if (saltoActual >= cadenasMaxSaltos) return;
        
        List<Player> jugadores = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!isPlayerExempt(p) && !p.equals(origen)) {
                jugadores.add(p);
            }
        }
        
        for (Player target : jugadores) {
            if (target.getLocation().distance(origen.getLocation()) < cadenasRadio) {
                double damage = cadenasDamageBase - (saltoActual * cadenasReduccion);
                if (damage > 0) {
                    target.damage(damage * faseMultiplicador);
                    target.sendActionBar("§e⚡ ¡Cadena eléctrica!");
                    
                    // Visual
                    dibujarLineaElectrica(origen.getLocation(), target.getLocation());
                    
                    // Siguiente salto
                    aplicarCadenasElectricas(target, saltoActual + 1);
                }
                break;
            }
        }
    }
    
    private boolean estaSobreBloqueAislante(Player p) {
        Block below = p.getLocation().subtract(0, 1, 0).getBlock();
        return materialesAislantes.contains(below.getType());
    }
    
    private void dibujarLineaElectrica(Location desde, Location hasta) {
        double distancia = desde.distance(hasta);
        int particulas = (int) (distancia * 3);
        
        org.bukkit.util.Vector direction = hasta.toVector().subtract(desde.toVector()).normalize();
        
        for (int i = 0; i < particulas; i++) {
            double ratio = (double) i / particulas;
            Location particleLoc = desde.clone().add(direction.clone().multiply(distancia * ratio));
            particleLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particleLoc, 1, 0.1, 0.1, 0.1, 0);
        }
    }
    
    private void crearZonaIonizada(Location loc) {
        zonasIonizadas.add(loc.clone());
        ionizacionExpiracion.put(loc.clone(), System.currentTimeMillis() + (zonasDuracion * 50L));
    }
    
    private void aplicarCortocircuito(Location centro) {
        // Este método podría desactivar redstone, pero requeriría tracking complejo
        // Por ahora solo es visual
        for (int x = -cortocircuitoRadio; x <= cortocircuitoRadio; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -cortocircuitoRadio; z <= cortocircuitoRadio; z++) {
                    Location checkLoc = centro.clone().add(x, y, z);
                    if (random.nextDouble() < 0.3) {
                        checkLoc.getWorld().spawnParticle(Particle.SMOKE, checkLoc, 1);
                    }
                }
            }
        }
    }
    
    private void startZonasMonitor() {
        zonasTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive()) {
                    cancel();
                    return;
                }
                
                // Visual para zonas
                for (Location zona : zonasIonizadas) {
                    if (random.nextDouble() < 0.5) {
                        zona.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, zona, 5, zonasRadio, 0.1, zonasRadio, 0.05);
                    }
                }
            }
        };
        zonasTask.runTaskTimer(plugin, 0L, 20L);
    }
    
    private void startEMP() {
        empTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive()) {
                    cancel();
                    return;
                }
                
                String phase = getCurrentPhaseString();
                if (empSoloPico && !phase.equals("PICO")) {
                    return;
                }
                
                aplicarEMP();
            }
        };
        empTask.runTaskTimer(plugin, empIntervalo, empIntervalo);
    }
    
    private void aplicarEMP() {
        messageBus.broadcast("§e§l📡 ¡PULSO EMP!", "disaster");
        
        List<Player> jugadores = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!isPlayerExempt(p)) {
                jugadores.add(p);
            }
        }
        
        for (Player p : jugadores) {
            // Desactivar elytras, etc.
            if (p.isGliding()) {
                p.setGliding(false);
            }
            
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, empDuracion, 0, false, true));
            p.sendActionBar("§c⚡ EMP - Equipamiento desactivado");
            ultimoEMP.put(p.getUniqueId(), System.currentTimeMillis());
        }
    }
}
