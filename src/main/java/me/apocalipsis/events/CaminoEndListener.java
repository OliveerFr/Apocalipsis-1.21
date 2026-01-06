package me.apocalipsis.events;

import java.util.Map;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.events.CaminoEndEvent.AnomaliaData;

/**
 * Listener para interacciones del evento El Camino al End
 */
public class CaminoEndListener implements Listener {
    
    private final CaminoEndEvent evento;
    private final Apocalipsis plugin;
    private final Random random = new Random();
    
    public CaminoEndListener(CaminoEndEvent evento, Apocalipsis plugin) {
        this.evento = evento;
        this.plugin = plugin;
    }
    
    /**
     * Detecta cuando un jugador se acerca a una anomalía
     * En fase RESONANCIA, permite recolectar fragmento
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player jugador = event.getPlayer();
        Location ubicacionJugador = jugador.getLocation();
        
        // Solo procesar si el evento está activo
        if (evento.getFaseActual() == null) {
            return;
        }
        
        // Verificar si el jugador está cerca de alguna anomalía
        Map<Location, AnomaliaData> anomalias = evento.getAnomaliasActivas();
        
        for (Map.Entry<Location, AnomaliaData> entry : anomalias.entrySet()) {
            Location anomaliaLoc = entry.getKey();
            AnomaliaData datos = entry.getValue();
            
            double distancia = ubicacionJugador.distance(anomaliaLoc);
            
            // Mensaje de primera vez que ven una anomalía (15 bloques)
            if (distancia <= 15.0 && distancia > 3.0) {
                evento.onJugadorEncuentraPrimeraAnomalia(jugador);
                
                // Mostrar tipo de anomalía
                if (datos.tipo != CaminoEndEvent.TipoAnomalia.NORMAL) {
                    String mensaje = datos.tipo == CaminoEndEvent.TipoAnomalia.ANTIGUA ?
                        "§5§l⚠ ¡Anomalía " + datos.tipo.nombre + " detectada!" :
                        "§e⚠ Anomalía " + datos.tipo.nombre + " detectada";
                    
                    jugador.sendActionBar(mensaje);
                }
            }
            
            // Solo en fase RESONANCIA o REVELACION se puede recolectar
            if (evento.getFaseActual() != CaminoEndEvent.Fase.RESONANCIA && 
                evento.getFaseActual() != CaminoEndEvent.Fase.REVELACION) {
                continue;
            }
            
            // Ignorar si ya se obtuvo fragmento de esta anomalía
            if (datos.fragmentoObtenido) {
                continue;
            }
            
            // Verificar distancia para recolectar (3 bloques)
            if (distancia <= 3.0) {
                // Marcar como obtenido
                datos.fragmentoObtenido = true;
                
                // Dar fragmento al jugador con tipo de anomalía
                evento.onJugadorRecolectaFragmento(jugador, anomaliaLoc, datos.tipo);
                
                // Efectos visuales y sonoros (más intensos para anomalías raras)
                int cantidadParticulas = datos.tipo == CaminoEndEvent.TipoAnomalia.ANTIGUA ? 80 : 
                                        (datos.tipo == CaminoEndEvent.TipoAnomalia.INESTABLE ? 60 : 50);
                
                jugador.getWorld().spawnParticle(Particle.EXPLOSION, anomaliaLoc, 3);
                jugador.getWorld().spawnParticle(datos.tipo.particula, anomaliaLoc, cantidadParticulas, 0.5, 0.5, 0.5, 0.1);
                jugador.getWorld().spawnParticle(Particle.END_ROD, anomaliaLoc, 30, 0.3, 1.0, 0.3, 0.15);
                
                float pitch = datos.tipo == CaminoEndEvent.TipoAnomalia.ANTIGUA ? 2.0f : 
                             (datos.tipo == CaminoEndEvent.TipoAnomalia.INESTABLE ? 1.7f : 1.5f);
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, pitch);
                
                break; // Solo una anomalía por tick
            }
        }
    }
    
    /**
     * Detecta interacción con el portal incompleto (opcional)
     * Puede usarse para dar información adicional o efectos
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player jugador = event.getPlayer();
        
        // Solo click derecho
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        // Verificar si está en fase REVELACION
        if (evento.getFaseActual() != CaminoEndEvent.Fase.REVELACION) {
            return;
        }
        
        // Verificar si el bloque clickeado es parte del portal
        if (event.getClickedBlock() == null) {
            return;
        }
        
        Material tipo = event.getClickedBlock().getType();
        if (tipo == Material.END_PORTAL_FRAME || tipo == Material.END_STONE_BRICKS) {
            Location clickLoc = event.getClickedBlock().getLocation();
            
            // El portal RECHAZA el toque - es peligroso
            if (tipo == Material.END_PORTAL_FRAME) {
                // Daño menor + knockback
                jugador.damage(1.0);
                jugador.setVelocity(jugador.getLocation().getDirection().multiply(-0.8).setY(0.4));
                
                // Visión aterradora aleatoria
                String[] visiones = {
                    "§5§l⚠ VES EL END",
                    "§4§lALGO OBSERVA",
                    "§5§lEL VACÍO TE MIRA",
                    "§c§lINCOMPLETO",
                    "§4§lNO ESTÁS LISTO"
                };
                jugador.sendTitle(
                    visiones[random.nextInt(visiones.length)],
                    "§7§o...el portal rechaza tu toque...",
                    5, 30, 10
                );
                
                // Efecto de corrupción temporal
                jugador.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.NAUSEA, 100, 0, false, false, false));
                jugador.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.BLINDNESS, 20, 0, false, false, false));
                
                // Sonidos aterradores
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 1.0f, 0.6f);
                jugador.playSound(jugador.getLocation(), Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.8f, 0.8f);
                
                // Partículas dramáticas
                clickLoc.getWorld().spawnParticle(Particle.SONIC_BOOM, clickLoc.add(0, 1, 0), 1);
                clickLoc.getWorld().spawnParticle(Particle.SCULK_SOUL, clickLoc, 30, 0.5, 0.5, 0.5, 0.1);
                
            } else {
                // Click en base de piedra del End - información
                // Mostrar fragmentos que faltan
                int fragmentosFaltantes = 40 - evento.getFragmentosRecolectados();
                int fragmentosPorcentaje = (evento.getFragmentosRecolectados() * 100) / 40;
                
                jugador.sendMessage("§5§l⚡ EL OBSERVADOR:");
                jugador.sendMessage("§7§o\"El portal... " + fragmentosPorcentaje + "% completo...\"");
                
                if (fragmentosFaltantes > 0) {
                    jugador.sendMessage("§7§o\"Aún faltan " + fragmentosFaltantes + " piezas... invisibles...\"");
                } else {
                    jugador.sendMessage("§7§o\"Los fragmentos... todos recuperados...\"");
                    jugador.sendMessage("§7§o\"Pero el portal... permanece cerrado...\"");
                }
                
                jugador.sendMessage("§7§o\"...que el camino existe.\"");
                
                // Efecto de partículas
                clickLoc.getWorld().spawnParticle(Particle.PORTAL, clickLoc, 20, 0.5, 0.5, 0.5, 0.05);
                jugador.playSound(jugador.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.3f, 0.8f);
            }
        }
    }
    
    /**
     * Detecta uso de Brújula del Eco (implementación futura)
     * La brújula apunta a la anomalía más cercana
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBrujulaUso(PlayerInteractEvent event) {
        Player jugador = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null) {
            return;
        }
        
        // Verificar si es Brújula del Vacío
        if (!evento.getItems().esBrujulaDelVacio(item)) {
            return;
        }
        
        // Solo click derecho
        if (event.getAction() != Action.RIGHT_CLICK_AIR && 
            event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        // Buscar anomalía más cercana
        Location ubicacionJugador = jugador.getLocation();
        Map<Location, AnomaliaData> anomalias = evento.getAnomaliasActivas();
        
        if (anomalias.isEmpty()) {
            jugador.sendMessage("§7La brújula no detecta ninguna anomalía cercana...");
            jugador.playSound(jugador.getLocation(), Sound.BLOCK_LEVER_CLICK, 0.5f, 0.5f);
            return;
        }
        
        // Encontrar la más cercana
        Location anomaliaMasCercana = null;
        double distanciaMinima = Double.MAX_VALUE;
        
        for (Location anomaliaLoc : anomalias.keySet()) {
            double distancia = ubicacionJugador.distance(anomaliaLoc);
            if (distancia < distanciaMinima) {
                distanciaMinima = distancia;
                anomaliaMasCercana = anomaliaLoc;
            }
        }
        
        if (anomaliaMasCercana != null) {
            int distanciaEntero = (int) distanciaMinima;
            String direccion = obtenerDireccion(ubicacionJugador, anomaliaMasCercana);
            
            jugador.sendMessage("§5§l⚡ Brújula del Eco:");
            jugador.sendMessage("§7Anomalía detectada al §e" + direccion + " §7(§e~" + distanciaEntero + "m§7)");
            
            // Efecto visual en dirección
            Location particleLoc = jugador.getEyeLocation().add(
                jugador.getLocation().getDirection().multiply(2)
            );
            jugador.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 10, 0.2, 0.2, 0.2, 0.01);
            jugador.playSound(jugador.getLocation(), Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.5f, 1.5f);
        }
    }
    
    /**
     * Obtiene dirección cardinal aproximada entre dos ubicaciones
     */
    private String obtenerDireccion(Location desde, Location hacia) {
        double dx = hacia.getX() - desde.getX();
        double dz = hacia.getZ() - desde.getZ();
        
        double angulo = Math.toDegrees(Math.atan2(dz, dx));
        if (angulo < 0) angulo += 360;
        
        if (angulo >= 337.5 || angulo < 22.5) return "Este";
        if (angulo >= 22.5 && angulo < 67.5) return "Sureste";
        if (angulo >= 67.5 && angulo < 112.5) return "Sur";
        if (angulo >= 112.5 && angulo < 157.5) return "Suroeste";
        if (angulo >= 157.5 && angulo < 202.5) return "Oeste";
        if (angulo >= 202.5 && angulo < 247.5) return "Noroeste";
        if (angulo >= 247.5 && angulo < 292.5) return "Norte";
        if (angulo >= 292.5 && angulo < 337.5) return "Noreste";
        
        return "Desconocida";
    }
}
