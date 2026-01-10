package me.apocalipsis.events;

import java.util.Map;
import java.util.Random;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.events.CaminoEndEvent;
import me.apocalipsis.events.CaminoEndEvent.AnomaliaData;
import me.apocalipsis.events.CaminoEndEvent.TipoAnomalia;

/**
 * Listener para interacciones del evento El Camino al End
 */
public class CaminoEndListener implements Listener {
    
    private final CaminoEndEvent evento;
    private final Apocalipsis plugin;
    private final Random random = new Random();
    
    // Sistema de puzzle ANTIGUA - Reconstrucción de Memoria
    private Map<Location, Set<Location>> bloquesColocadosPorAnomalia = new HashMap<>();
    private Set<Material> bloquesRequeridos = new HashSet<>();
    private Set<Integer> ultimosFragmentosAnunciados = new HashSet<>();
    
    public CaminoEndListener(CaminoEndEvent evento, Apocalipsis plugin) {
        this.evento = evento;
        this.plugin = plugin;
        
        // Bloques requeridos para el puzzle ANTIGUA
        bloquesRequeridos.add(Material.NETHERRACK);   // Eco de Brasas
        bloquesRequeridos.add(Material.SCULK);         // Eco de Sombras
        bloquesRequeridos.add(Material.DEEPSLATE);     // Piedra Rota
        bloquesRequeridos.add(Material.END_STONE);     // Camino al End
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
                
                // Mostrar tipo de anomalía con colores y mensajes apropiados
                if (datos.tipo != CaminoEndEvent.TipoAnomalia.NORMAL && datos.tipo != CaminoEndEvent.TipoAnomalia.OCULTA) {
                    String mensaje;
                    if (datos.tipo == CaminoEndEvent.TipoAnomalia.ANTIGUA) {
                        mensaje = "§5§l⚠ ¡Anomalía " + datos.tipo.nombre + " detectada!";
                    } else if (datos.tipo == CaminoEndEvent.TipoAnomalia.INESTABLE) {
                        mensaje = "§e⚠ Anomalía " + datos.tipo.nombre + " detectada";
                    } else if (datos.tipo.esEco()) {
                        mensaje = "§d⚡ Anomalía " + datos.tipo.nombre + " detectada";
                    } else {
                        mensaje = "§7⚡ Anomalía detectada";
                    }
                    
                    jugador.sendActionBar(mensaje);
                }
                // Las anomalías ocultas no muestran mensaje al acercarse
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
            // Para anomalías ocultas, requiere estar MUY cerca (1.5 bloques)
            double distanciaRequerida = datos.tipo == CaminoEndEvent.TipoAnomalia.OCULTA ? 1.5 : 3.0;
            
            if (distancia <= distanciaRequerida) {
                // Marcar como obtenido
                datos.fragmentoObtenido = true;
                
                // Dar fragmento al jugador con tipo de anomalía
                evento.onJugadorRecolectaFragmento(jugador, anomaliaLoc, datos.tipo);
                
                // Efectos visuales y sonoros (más intensos para anomalías raras)
                int cantidadParticulas;
                if (datos.tipo == CaminoEndEvent.TipoAnomalia.ANTIGUA) {
                    cantidadParticulas = 80;
                } else if (datos.tipo == CaminoEndEvent.TipoAnomalia.OCULTA) {
                    cantidadParticulas = 70;
                } else if (datos.tipo == CaminoEndEvent.TipoAnomalia.INESTABLE) {
                    cantidadParticulas = 60;
                } else if (datos.tipo.esEco()) {
                    cantidadParticulas = 55;
                } else {
                    cantidadParticulas = 50;
                }
                
                jugador.getWorld().spawnParticle(Particle.EXPLOSION, anomaliaLoc, 3);
                jugador.getWorld().spawnParticle(datos.tipo.particula, anomaliaLoc, cantidadParticulas, 0.5, 0.5, 0.5, 0.1);
                jugador.getWorld().spawnParticle(Particle.END_ROD, anomaliaLoc, 30, 0.3, 1.0, 0.3, 0.15);
                
                float pitch;
                if (datos.tipo == CaminoEndEvent.TipoAnomalia.ANTIGUA) {
                    pitch = 2.0f;
                } else if (datos.tipo == CaminoEndEvent.TipoAnomalia.OCULTA) {
                    pitch = 1.9f;
                } else if (datos.tipo == CaminoEndEvent.TipoAnomalia.INESTABLE) {
                    pitch = 1.7f;
                } else if (datos.tipo.esEco()) {
                    pitch = 1.6f;
                } else {
                    pitch = 1.5f;
                }
                
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, pitch);
                
                // Mensaje especial al encontrar anomalía oculta
                if (datos.tipo == CaminoEndEvent.TipoAnomalia.OCULTA) {
                    jugador.sendTitle("§d§l✦ DESCUBIERTA ✦", "§7§oAnomalia oculta revelada", 10, 40, 10);
                }
                
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
    
    /**
     * Sistema de puzzle ANTIGUA - Detecta colocación de bloques
     * Requiere 4 bloques específicos en patrón de cruz cerca de anomalía ANTIGUA
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;
        
        Player jugador = event.getPlayer();
        Location blockLoc = event.getBlockPlaced().getLocation();
        
        // Solo en fase RESONANCIA
        if (evento.getFaseActual() != CaminoEndEvent.Fase.RESONANCIA) {
            return;
        }
        
        // Verificar si el bloque es uno de los requeridos
        Material tipoBloque = event.getBlockPlaced().getType();
        if (!bloquesRequeridos.contains(tipoBloque)) {
            return;
        }
        
        // Buscar anomalía ANTIGUA cercana (radio 15 bloques)
        Map<Location, AnomaliaData> anomalias = evento.getAnomaliasActivas();
        Location anomaliaAnciana = null;
        
        for (Map.Entry<Location, AnomaliaData> entry : anomalias.entrySet()) {
            if (entry.getValue().tipo == TipoAnomalia.ANTIGUA && 
                entry.getKey().distance(blockLoc) <= 15) {
                anomaliaAnciana = entry.getKey();
                break;
            }
        }
        
        if (anomaliaAnciana == null) {
            return; // No hay anomalía antigua cercana
        }
        
        // Registrar bloque colocado
        Set<Location> bloques = bloquesColocadosPorAnomalia.computeIfAbsent(anomaliaAnciana, k -> new HashSet<>());
        bloques.add(blockLoc);
        
        // Feedback visual
        jugador.sendMessage("§d§l✦ §7Bloque de eco detectado - " + bloques.size() + "/4");
        jugador.playSound(jugador.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.5f, 1.0f);
        blockLoc.getWorld().spawnParticle(Particle.GLOW, blockLoc.add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.1);
        
        // Verificar si completó el puzzle
        if (bloques.size() >= 4) {
            verificarPuzzleAntigua(jugador, anomaliaAnciana, bloques);
        }
    }
    
    private void verificarPuzzleAntigua(Player jugador, Location anomaliaLoc, Set<Location> bloques) {
        // Validar patrón de cruz (centro + 4 direcciones)
        // En este caso, simplemente verificamos que hay 4 bloques diferentes tipos
        
        Set<Material> tiposColocados = new HashSet<>();
        for (Location loc : bloques) {
            tiposColocados.add(loc.getBlock().getType());
        }
        
        // Si tenemos los 4 tipos correctos
        if (tiposColocados.size() == 4 && 
            tiposColocados.contains(Material.NETHERRACK) &&
            tiposColocados.contains(Material.SCULK) &&
            tiposColocados.contains(Material.DEEPSLATE) &&
            tiposColocados.contains(Material.END_STONE)) {
            
            completarPuzzleAntigua(jugador, anomaliaLoc);
        }
    }
    
    private void completarPuzzleAntigua(Player jugador, Location anomaliaLoc) {
        // Remover tracking
        bloquesColocadosPorAnomalia.remove(anomaliaLoc);
        
        // Efectos epicos
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            p.sendTitle(
                "§d§l✦ REVELACIÓN ✦",
                "§7Los cuatro ecos se unen...",
                10, 60, 15
            );
            p.sendMessage("");
            p.sendMessage("§5§l⚡ EL OBSERVADOR:");
            p.sendMessage("§7§o\"Vieron la conexión...\"");
            p.sendMessage("§7§o\"Brasas. Sombras. Piedra. Vacío.\"");
            p.sendMessage("§d§o\"No fueron eventos separados...\"");
            p.sendMessage("§d§o\"Fueron... capítulos del mismo libro.\"");
            p.sendMessage("");
            
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 1.2f);
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.5f);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 2.0f);
        }
        
        // Efectos visuales en la anomalía
        anomaliaLoc.getWorld().spawnParticle(Particle.PORTAL, anomaliaLoc, 100, 1, 2, 1, 0.5);
        anomaliaLoc.getWorld().spawnParticle(Particle.GLOW, anomaliaLoc, 80, 0.8, 1.5, 0.8, 0.2);
        
        // Recompensa: 15 fragmentos bonus
        ItemStack fragmentos = evento.getItems().crearFragmentoDelVacio();
        fragmentos.setAmount(15);
        jugador.getInventory().addItem(fragmentos);
        
        jugador.sendMessage("§a§l+ 15 FRAGMENTOS DEL VACÍO");
        jugador.sendMessage("§7El Observador reconoce tu descubrimiento...");
        jugador.playSound(jugador.getLocation(), Sound.ENTITY_EVOKER_CELEBRATE, 1.0f, 1.0f);
        
        // Actualizar contador global
        evento.onJugadorRecolectaFragmento(jugador, anomaliaLoc, TipoAnomalia.ANTIGUA);
    }
    
    /**
     * Mensajes narrativos dinámicos según progreso
     */
    public void enviarMensajeProgreso(int fragmentosActuales) {
        if (ultimosFragmentosAnunciados.contains(fragmentosActuales)) {
            return; // Ya mostrado
        }
        
        ultimosFragmentosAnunciados.add(fragmentosActuales);
        
        String[] mensajes = null;
        
        switch (fragmentosActuales) {
            case 10:
                mensajes = new String[] {
                    "§5§l⚡ EL OBSERVADOR:",
                    "§7§o\"Están... encontrando muchos...\"",
                    "§8§o\"Más de los que esperaba.\""
                };
                break;
            case 20:
                mensajes = new String[] {
                    "§5§l⚡ EL OBSERVADOR:",
                    "§7§o\"La mitad del camino...\"",
                    "§7§o\"¿Sienten cómo el aire cambia?\""
                };
                break;
            case 30:
                mensajes = new String[] {
                    "§5§l⚡ EL OBSERVADOR:",
                    "§c§o\"Demasiados fragmentos...\"",
                    "§8§o\"Algo está... prestando atención.\""
                };
                break;
            case 35:
                mensajes = new String[] {
                    "§5§l⚡ EL OBSERVADOR:",
                    "§c§o\"DETENGAN... no, continúen...\"",
                    "§7§o\"Es demasiado tarde para detenerse.\""
                };
                break;
        }
        
        if (mensajes != null) {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                p.sendMessage("");
                for (String mensaje : mensajes) {
                    p.sendMessage(mensaje);
                }
                p.sendMessage("");
                p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_STARE, 0.5f, 0.8f);
            }
        }
    }
    
    /**
     * Mensajes al descubrir anomalías raras
     */
    public void enviarMensajeAnomaliaRara(TipoAnomalia tipo) {
        String[] mensajes = null;
        
        switch (tipo) {
            case ANTIGUA:
                mensajes = new String[] {
                    "§5§l⚡ EL OBSERVADOR:",
                    "§d§o\"Esa... es muy antigua.\"",
                    "§8§o\"De un ciclo anterior... antes de que yo...\"",
                    "§7§o\"No importa. Continúen.\""
                };
                break;
            case OCULTA:
                mensajes = new String[] {
                    "§5§l⚡ EL OBSERVADOR:",
                    "§d§o\"La encontraron.\"",
                    "§7§o\"Estaba esperando... ¿cuánto tiempo?\""
                };
                break;
            case ECO_BRASAS:
                mensajes = new String[] {
                    "§5§l⚡ EL OBSERVADOR:",
                    "§7§o\"Fuego que nunca murió...\"",
                    "§8§o\"Recuerdo ese calor.\""
                };
                break;
            case ECO_SOMBRAS:
                mensajes = new String[] {
                    "§5§l⚡ EL OBSERVADOR:",
                    "§8§o\"Se mueven... como lo hice yo...\"",
                    "§7§o\"Antes de quedar... así.\""
                };
                break;
            case ECO_PIEDRA:
                mensajes = new String[] {
                    "§5§l⚡ EL OBSERVADOR:",
                    "§7§o\"Memorias rotas...\"",
                    "§8§o\"Algunas veces... también lo fui.\""
                };
                break;
            case NORMAL:
            case INESTABLE:
                // Sin mensajes especiales para estos tipos
                return;
        }
        
        if (mensajes != null) {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                p.sendMessage("");
                for (String mensaje : mensajes) {
                    p.sendMessage(mensaje);
                }
                p.sendMessage("");
                p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_STARE, 0.4f, 0.7f);
            }
        }
    }
}
