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

import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Endermite;
import org.bukkit.entity.LivingEntity;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.ItemMeta;
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
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE DROPS DE ENTIDADES
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Maneja drops especiales cuando mueren entidades del evento
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        
        if (killer == null || !evento.isActive()) return;
        
        // Verificar si es una entidad marcada del evento
        org.bukkit.NamespacedKey keyGuardian = new org.bukkit.NamespacedKey(plugin, "anomalia_guardian");
        org.bukkit.NamespacedKey keyEndermite = new org.bukkit.NamespacedKey(plugin, "anomalia_endermite");
        org.bukkit.NamespacedKey keyWardenFinal = new org.bukkit.NamespacedKey(plugin, "warden_final");
        org.bukkit.NamespacedKey keyHordaBruto = new org.bukkit.NamespacedKey(plugin, "horda_bruto");
        org.bukkit.NamespacedKey keyHordaEnderman = new org.bukkit.NamespacedKey(plugin, "horda_enderman");
        org.bukkit.NamespacedKey keyHordaEndermite = new org.bukkit.NamespacedKey(plugin, "horda_endermite");
        
        boolean esGuardian = entity.getPersistentDataContainer().has(keyGuardian, org.bukkit.persistence.PersistentDataType.BYTE);
        boolean esEndermite = entity.getPersistentDataContainer().has(keyEndermite, org.bukkit.persistence.PersistentDataType.BYTE);
        boolean esWardenFinal = entity.getPersistentDataContainer().has(keyWardenFinal, org.bukkit.persistence.PersistentDataType.BYTE);
        boolean esHordaBruto = entity.getPersistentDataContainer().has(keyHordaBruto, org.bukkit.persistence.PersistentDataType.BYTE);
        boolean esHordaEnderman = entity.getPersistentDataContainer().has(keyHordaEnderman, org.bukkit.persistence.PersistentDataType.BYTE);
        boolean esHordaEndermite = entity.getPersistentDataContainer().has(keyHordaEndermite, org.bukkit.persistence.PersistentDataType.BYTE);
        
        if (!esGuardian && !esEndermite && !esWardenFinal && !esHordaBruto && !esHordaEnderman && !esHordaEndermite) return;
        
        // ═══════════════════════════════════════════════════════════════════
        // DROPS DEL GUARDIÁN DEL VACÍO (Enderman)
        // ═══════════════════════════════════════════════════════════════════
        if (esGuardian && entity instanceof Enderman) {
            event.getDrops().clear(); // Limpiar drops normales
            
            // SIEMPRE: Fragmentos del Vacío (2-4)
            int cantidadFragmentos = 2 + random.nextInt(3);
            ItemStack fragmentos = evento.getItems().crearFragmentoDelVacio();
            fragmentos.setAmount(cantidadFragmentos);
            event.getDrops().add(fragmentos);
            
            // 30%: Espada de Ender mejorada
            if (random.nextDouble() < 0.30) {
                ItemStack espadaEnder = crearEspadaDelVacio();
                event.getDrops().add(espadaEnder);
                killer.sendMessage("§5§l⟫ DROP RARO: §dEspada del Vacío");
                killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            }
            
            // 25%: Pico de Ender mejorado
            else if (random.nextDouble() < 0.25) {
                ItemStack picoEnder = crearPicoDelVacio();
                event.getDrops().add(picoEnder);
                killer.sendMessage("§5§l⟫ DROP RARO: §dPico del Vacío");
                killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            }
            
            // 20%: Escudo Dimensional
            else if (random.nextDouble() < 0.20) {
                ItemStack escudo = crearEscudoDimensional();
                event.getDrops().add(escudo);
                killer.sendMessage("§5§l⟫ DROP RARO: §dEscudo Dimensional");
                killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            }
            
            // 15%: Casco del Observador
            else if (random.nextDouble() < 0.15) {
                ItemStack casco = crearCascoDelObservador();
                event.getDrops().add(casco);
                killer.sendMessage("§5§l⟫ DROP ÉPICO: §5Casco del Observador");
                killer.playSound(killer.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 1.2f);
            }
            
            // SIEMPRE: Perlas de Ender (1-3)
            ItemStack perlas = new ItemStack(Material.ENDER_PEARL, 1 + random.nextInt(3));
            event.getDrops().add(perlas);
            
            // Efecto visual
            entity.getWorld().spawnParticle(Particle.PORTAL, entity.getLocation(), 100, 1, 1, 1, 0.5);
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDERMAN_DEATH, 1.0f, 0.7f);
        }
        
        // ═══════════════════════════════════════════════════════════════════
        // DROPS DEL PARÁSITO DEL VACÍO (Endermite)
        // ═══════════════════════════════════════════════════════════════════
        else if (esEndermite && entity instanceof Endermite) {
            event.getDrops().clear();
            
            // 50%: Fragmento del Vacío (1)
            if (random.nextDouble() < 0.50) {
                event.getDrops().add(evento.getItems().crearFragmentoDelVacio());
            }
            
            // 15%: Polvo de Ender (útil para pociones)
            if (random.nextDouble() < 0.15) {
                ItemStack polvoEnder = crearPolvoDelVacio();
                event.getDrops().add(polvoEnder);
                killer.sendMessage("§7§l⟫ DROP: §dPolvo del Vacío");
            }
            
            // Partículas pequeñas
            entity.getWorld().spawnParticle(Particle.PORTAL, entity.getLocation(), 20, 0.5, 0.5, 0.5, 0.2);
        }
        
        // ═══════════════════════════════════════════════════════════════════
        // DROPS DEL WARDEN FINAL (ÉPICO)
        // ═══════════════════════════════════════════════════════════════════
        else if (esWardenFinal && entity instanceof org.bukkit.entity.Warden) {
            event.getDrops().clear();
            
            // ANUNCIO GLOBAL DE VICTORIA
            plugin.getServer().broadcastMessage("");
            plugin.getServer().broadcastMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            plugin.getServer().broadcastMessage("§4§l☠ EL GUARDIÁN DE LAS PROFUNDIDADES HA CAÍDO ☠");
            plugin.getServer().broadcastMessage("");
            plugin.getServer().broadcastMessage("§6§l⚔ VENCIDO POR: §e" + killer.getName());
            plugin.getServer().broadcastMessage("");
            plugin.getServer().broadcastMessage("§5§l⚡ EL OBSERVADOR §8§o[PERTURBADO]:");
            plugin.getServer().broadcastMessage("§c§l§o\"...Ha caído... pero ¿por qué estaba aquí?...\"");
            plugin.getServer().broadcastMessage("");
            plugin.getServer().broadcastMessage("§7§o\"Algo... algo se está moviendo en las sombras...\"");
            plugin.getServer().broadcastMessage("§7§o\"Este guardián no era del End...\"");
            plugin.getServer().broadcastMessage("§8§o\"...Venía desde más allá... desde LAS PROFUNDIDADES...\"");
            plugin.getServer().broadcastMessage("");
            plugin.getServer().broadcastMessage("§4§l§o\"Si ESO fue enviado como guardián...\"");
            plugin.getServer().broadcastMessage("§4§l§o\"...¿Qué está intentando proteger?...\"");
            plugin.getServer().broadcastMessage("§4§l§o\"...¿O de qué nos está ADVIRTIENDO?...\"");
            plugin.getServer().broadcastMessage("");
            plugin.getServer().broadcastMessage("§5§l◆ El camino continúa... pero el misterio se profundiza.");
            plugin.getServer().broadcastMessage("§8§o\"Lo que viene después del End... podría ser peor...\"");
            plugin.getServer().broadcastMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            plugin.getServer().broadcastMessage("");
            
            // Efectos visuales MASIVOS
            Location loc = entity.getLocation();
            for (int i = 0; i < 200; i++) {
                loc.getWorld().spawnParticle(Particle.SCULK_SOUL, loc, 1,
                    (random.nextDouble() - 0.5) * 8, random.nextDouble() * 5, (random.nextDouble() - 0.5) * 8, 0.3);
            }
            loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 50, 3, 2, 3, 1.0);
            loc.getWorld().spawnParticle(Particle.SOUL, loc, 100, 4, 3, 4, 0.5);
            
            // Sonido épico para todos
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 0.5f);
                p.playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 1.2f);
                p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 0.5f);
                p.sendTitle("§4§l☠ VICTORIA... TEMPORAL ☠", "§8§o...pero las profundidades guardan más secretos...", 20, 120, 30);
            }
            
            // ════════════════════════════════════════════════════════════════
            // DROPS ÉPICOS GARANTIZADOS
            // ════════════════════════════════════════════════════════════════
            
            // SIEMPRE: Corazón de las Profundidades (item único)
            ItemStack corazon = crearCorazonDelVacio();
            event.getDrops().add(corazon);
            killer.sendMessage("§d§l⟫ DROP LEGENDARIO: §4Corazón de las Profundidades");
            killer.sendMessage("§8§o   \"...Un fragmento de un misterio mayor...\"");
            
            // SIEMPRE: Fragmentos masivos (10-15)
            int cantidadFragmentos = 10 + random.nextInt(6);
            ItemStack fragmentos = evento.getItems().crearFragmentoDelVacio();
            fragmentos.setAmount(cantidadFragmentos);
            event.getDrops().add(fragmentos);
            
            // 80%: Espada de Netherite MEJORADA
            if (random.nextDouble() < 0.80) {
                ItemStack espadaNetherite = crearEspadaDelGuardian();
                event.getDrops().add(espadaNetherite);
                killer.sendMessage("§4§l⟫ DROP ÉPICO: §cEspada del Guardián");
            }
            
            // 70%: Hacha de Netherite MEJORADA
            if (random.nextDouble() < 0.70) {
                ItemStack hachaNetherite = crearHachaDelGuardian();
                event.getDrops().add(hachaNetherite);
                killer.sendMessage("§4§l⟫ DROP ÉPICO: §cHacha del Guardián");
            }
            
            // 60%: Peto de Netherite MEJORADO
            if (random.nextDouble() < 0.60) {
                ItemStack petoNetherite = crearPetoDelGuardian();
                event.getDrops().add(petoNetherite);
                killer.sendMessage("§4§l⟫ DROP ÉPICO: §cPeto del Guardián");
            }
            
            // 50%: Pantalones de Netherite MEJORADOS
            if (random.nextDouble() < 0.50) {
                ItemStack pantalonesNetherite = crearPantalonesDelGuardian();
                event.getDrops().add(pantalonesNetherite);
                killer.sendMessage("§4§l⟫ DROP ÉPICO: §cPantalones del Guardián");
            }
            
            // SIEMPRE: Escombros Antiguos (8-16)
            ItemStack escombros = new ItemStack(Material.ANCIENT_DEBRIS, 8 + random.nextInt(9));
            event.getDrops().add(escombros);
            
            // SIEMPRE: Diamantes (16-32)
            ItemStack diamantes = new ItemStack(Material.DIAMOND, 16 + random.nextInt(17));
            event.getDrops().add(diamantes);
            
            // Resetear Warden en el evento para permitir transición
            evento.resetearWarden();
        }
        
        // ═══════════════════════════════════════════════════════════════════
        // DROPS DE HORDAS (Fase RESONANCIA)
        // ═══════════════════════════════════════════════════════════════════
        else if (esHordaBruto && entity instanceof org.bukkit.entity.PiglinBrute) {
            // Drops mejorados para Brutos
            
            // 40%: Fragmento del Vacío
            if (random.nextDouble() < 0.40) {
                event.getDrops().add(evento.getItems().crearFragmentoDelVacio());
            }
            
            // 20%: Oro extra
            if (random.nextDouble() < 0.20) {
                event.getDrops().add(new ItemStack(Material.GOLD_INGOT, 3 + random.nextInt(5)));
            }
            
            // 10%: Espada de oro encantada
            if (random.nextDouble() < 0.10) {
                ItemStack espadaOro = new ItemStack(Material.GOLDEN_SWORD);
                espadaOro.addEnchantment(Enchantment.SHARPNESS, 3);
                espadaOro.addEnchantment(Enchantment.KNOCKBACK, 2);
                event.getDrops().add(espadaOro);
            }
        }
        
        else if (esHordaEnderman && entity instanceof Enderman) {
            // Drops mejorados para Enderman de Hordas
            
            // 35%: Fragmento del Vacío
            if (random.nextDouble() < 0.35) {
                event.getDrops().add(evento.getItems().crearFragmentoDelVacio());
            }
            
            // SIEMPRE: Perlas de Ender
            event.getDrops().add(new ItemStack(Material.ENDER_PEARL, 1 + random.nextInt(2)));
        }
        
        else if (esHordaEndermite && entity instanceof Endermite) {
            // 25%: Fragmento del Vacío
            if (random.nextDouble() < 0.25) {
                event.getDrops().add(evento.getItems().crearFragmentoDelVacio());
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // CREACIÓN DE ITEMS ÚNICOS (FASE 1 - ANOMALÍAS)
    // ═══════════════════════════════════════════════════════════════════
    
    private ItemStack crearEspadaDelVacio() {
        ItemStack espada = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = espada.getItemMeta();
        
        meta.setDisplayName("§5§lEspada del Vacío");
        meta.setLore(java.util.Arrays.asList(
            "§7Forjada en la oscuridad dimensional",
            "§7del End, esta espada corta entre",
            "§7las realidades.",
            "",
            "§9Sharpness IV",
            "§9Knockback II",
            "§9Looting I",
            "",
            "§5§o\"El vacío reclama lo suyo...\""
        ));
        
        meta.addEnchant(Enchantment.SHARPNESS, 4, true);
        meta.addEnchant(Enchantment.KNOCKBACK, 2, true);
        meta.addEnchant(Enchantment.LOOTING, 1, true);
        meta.setUnbreakable(false);
        
        espada.setItemMeta(meta);
        return espada;
    }
    
    private ItemStack crearPicoDelVacio() {
        ItemStack pico = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta meta = pico.getItemMeta();
        
        meta.setDisplayName("§5§lPico del Vacío");
        meta.setLore(java.util.Arrays.asList(
            "§7Un pico infundido con la energía",
            "§7dimensional del End.",
            "",
            "§9Efficiency IV",
            "§9Fortune II",
            "§9Unbreaking III",
            "",
            "§5§o\"Rompe las barreras dimensionales...\""
        ));
        
        meta.addEnchant(Enchantment.EFFICIENCY, 4, true);
        meta.addEnchant(Enchantment.FORTUNE, 2, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        
        pico.setItemMeta(meta);
        return pico;
    }
    
    private ItemStack crearEscudoDimensional() {
        ItemStack escudo = new ItemStack(Material.SHIELD);
        ItemMeta meta = escudo.getItemMeta();
        
        meta.setDisplayName("§5§lEscudo Dimensional");
        meta.setLore(java.util.Arrays.asList(
            "§7Un escudo que refleja ataques",
            "§7a través del vacío dimensional.",
            "",
            "§9Unbreaking III",
            "§9Mending",
            "",
            "§5§o\"La defensa del observador...\""
        ));
        
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addEnchant(Enchantment.MENDING, 1, true);
        
        escudo.setItemMeta(meta);
        return escudo;
    }
    
    private ItemStack crearCascoDelObservador() {
        ItemStack casco = new ItemStack(Material.NETHERITE_HELMET);
        ItemMeta meta = casco.getItemMeta();
        
        meta.setDisplayName("§5§l⚡ Casco del Observador");
        meta.setLore(java.util.Arrays.asList(
            "§7El casco que una vez perteneció",
            "§7al mismo Observador.",
            "",
            "§9Protection IV",
            "§9Unbreaking III",
            "§9Respiration III",
            "§9Aqua Affinity",
            "",
            "§5§o\"Ves más allá de lo visible...\""
        ));
        
        meta.addEnchant(Enchantment.PROTECTION, 4, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addEnchant(Enchantment.RESPIRATION, 3, true);
        meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
        
        casco.setItemMeta(meta);
        return casco;
    }
    
    private ItemStack crearPolvoDelVacio() {
        ItemStack polvo = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta meta = polvo.getItemMeta();
        
        meta.setDisplayName("§dPolvo del Vacío");
        meta.setLore(java.util.Arrays.asList(
            "§7Polvo dimensional extraído de",
            "§7parásitos del vacío.",
            "",
            "§7Útil para pociones y",
            "§7encantamientos avanzados.",
            "",
            "§5§o\"Partículas de otra dimensión...\""
        ));
        
        polvo.setItemMeta(meta);
        return polvo;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // CREACIÓN DE ITEMS ÚNICOS DEL WARDEN (FASE 2 - RESONANCIA)
    // ═══════════════════════════════════════════════════════════════════
    
    private ItemStack crearCorazonDelVacio() {
        ItemStack corazon = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = corazon.getItemMeta();
        
        meta.setDisplayName("§4§l◆ CORAZÓN DE LAS PROFUNDIDADES ◆");
        meta.setLore(java.util.Arrays.asList(
            "§7━━━━━━━━━━━━━━━━━━━━━━━━━━",
            "§cEl núcleo pulsante del",
            "§cGuardián de las Profundidades.",
            "",
            "§8Emana una energía que no pertenece",
            "§8a ningún mundo conocido...",
            "",
            "§7• §oNo es del End",
            "§7• §oNo es del Nether",
            "§7• §oNo es del Overworld",
            "",
            "§5§lORIGEN DESCONOCIDO:",
            "§8§o\"Viene desde MÁS ALLÁ...\"",
            "§8§o\"Desde las profundidades olvidadas...\"",
            "",
            "§4§l§o\"...¿Protector? ¿Centinela? ¿Heraldo?...\"",
            "§4§l§o\"...El futuro revelará su propósito...\"",
            "",
            "§6§l⚠ ADVERTENCIA DEL OBSERVADOR:",
            "§7\"Si esto fue enviado como guardián...\"",
            "§7\"...temo lo que vendrá después del End...\"",
            "",
            "§7━━━━━━━━━━━━━━━━━━━━━━━━━━",
            "§d§lLEGENDARIO §8§o- §4§oSpoiler del Futuro"
        ));
        
        meta.addEnchant(Enchantment.PROTECTION, 10, true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        
        corazon.setItemMeta(meta);
        return corazon;
    }
    
    private ItemStack crearEspadaDelGuardian() {
        ItemStack espada = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = espada.getItemMeta();
        
        meta.setDisplayName("§4§l⚔ ESPADA DEL GUARDIÁN §4§l⚔");
        meta.setLore(java.util.Arrays.asList(
            "§7━━━━━━━━━━━━━━━━━━━━━━━",
            "§cForjada con la esencia del",
            "§cGuardián del Vacío Absoluto.",
            "",
            "§7Corta la realidad misma.",
            "",
            "§c§lENCATAMIENTOS:",
            "§7• Filo V",
            "§7• Perdición III",
            "§7• Empuje II",
            "§7• Aspecto ígneo II",
            "",
            "§4§o\"...CORTA EL VACÍO...\"",
            "§7━━━━━━━━━━━━━━━━━━━━━━━",
            "§4§lÉPICO"
        ));
        
        meta.addEnchant(Enchantment.SHARPNESS, 5, false);
        meta.addEnchant(Enchantment.LOOTING, 3, false);
        meta.addEnchant(Enchantment.KNOCKBACK, 2, false);
        meta.addEnchant(Enchantment.FIRE_ASPECT, 2, false);
        meta.addEnchant(Enchantment.UNBREAKING, 3, false);
        
        espada.setItemMeta(meta);
        return espada;
    }
    
    private ItemStack crearHachaDelGuardian() {
        ItemStack hacha = new ItemStack(Material.NETHERITE_AXE);
        ItemMeta meta = hacha.getItemMeta();
        
        meta.setDisplayName("§4§l⚒ HACHA DEL GUARDIÁN §4§l⚒");
        meta.setLore(java.util.Arrays.asList(
            "§7━━━━━━━━━━━━━━━━━━━━━━━",
            "§cHacha imbuida con el poder",
            "§cdel Guardián del Vacío.",
            "",
            "§7Destruye todo a su paso.",
            "",
            "§c§lENCATAMIENTOS:",
            "§7• Filo V",
            "§7• Eficiencia V",
            "§7• Fortuna II",
            "§7• Irrompibilidad III",
            "",
            "§4§o\"...ROMPE LO IRROMPIBLE...\"",
            "§7━━━━━━━━━━━━━━━━━━━━━━━",
            "§4§lÉPICO"
        ));
        
        meta.addEnchant(Enchantment.SHARPNESS, 5, false);
        meta.addEnchant(Enchantment.EFFICIENCY, 5, false);
        meta.addEnchant(Enchantment.FORTUNE, 2, false);
        meta.addEnchant(Enchantment.UNBREAKING, 3, false);
        
        hacha.setItemMeta(meta);
        return hacha;
    }
    
    private ItemStack crearPetoDelGuardian() {
        ItemStack peto = new ItemStack(Material.NETHERITE_CHESTPLATE);
        ItemMeta meta = peto.getItemMeta();
        
        meta.setDisplayName("§4§l⬛ PETO DEL GUARDIÁN §4§l⬛");
        meta.setLore(java.util.Arrays.asList(
            "§7━━━━━━━━━━━━━━━━━━━━━━━",
            "§cArmadura forjada con fragmentos",
            "§cdel Guardián del Vacío.",
            "",
            "§7Protección dimensional absoluta.",
            "",
            "§c§lENCATAMIENTOS:",
            "§7• Protección V",
            "§7• Irrompibilidad III",
            "§7• Reparación",
            "§7• Resistencia al retroceso",
            "",
            "§4§o\"...INMUNE AL VACÍO...\"",
            "§7━━━━━━━━━━━━━━━━━━━━━━━",
            "§4§lÉPICO"
        ));
        
        meta.addEnchant(Enchantment.PROTECTION, 5, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, false);
        meta.addEnchant(Enchantment.MENDING, 1, false);
        // Nota: Resistencia al retroceso es característica de netherite por defecto
        
        peto.setItemMeta(meta);
        return peto;
    }
    
    private ItemStack crearPantalonesDelGuardian() {
        ItemStack pantalones = new ItemStack(Material.NETHERITE_LEGGINGS);
        ItemMeta meta = pantalones.getItemMeta();
        
        meta.setDisplayName("§4§l⬛ PANTALONES DEL GUARDIÁN §4§l⬛");
        meta.setLore(java.util.Arrays.asList(
            "§7━━━━━━━━━━━━━━━━━━━━━━━",
            "§cPantalones imbuidos con la",
            "§cesencia del Guardián.",
            "",
            "§7Agilidad y resistencia mejoradas.",
            "",
            "§c§lENCATAMIENTOS:",
            "§7• Protección IV",
            "§7• Protección contra proyectiles III",
            "§7• Irrompibilidad III",
            "§7• Reparación",
            "",
            "§4§o\"...CAMINA EN EL VACÍO...\"",
            "§7━━━━━━━━━━━━━━━━━━━━━━━",
            "§4§lÉPICO"
        ));
        
        meta.addEnchant(Enchantment.PROTECTION, 4, false);
        meta.addEnchant(Enchantment.PROJECTILE_PROTECTION, 3, false);
        meta.addEnchant(Enchantment.UNBREAKING, 3, false);
        meta.addEnchant(Enchantment.MENDING, 1, false);
        
        pantalones.setItemMeta(meta);
        return pantalones;
    }
}
