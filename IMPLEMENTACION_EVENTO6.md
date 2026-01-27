# 🔧 IMPLEMENTACIÓN TÉCNICA - EVENTO 6

## 📋 Guía de Implementación Java

Esta guía detalla cómo implementar el Evento 6 "Cuando el Mundo Decide Olvidar" en el código del plugin.

---

## 📁 Estructura de Archivos

```
src/main/java/com/riolubruh/apocalipsis/
├── eventos/
│   ├── Evento6MundoOlvidado.java          # Clase principal del evento
│   ├── MundoOlvidadoFase.java             # Enum de fases/actos
│   └── listeners/
│       ├── Evento6EffectsListener.java    # Efectos especiales
│       └── Evento6NetherEndListener.java  # Detección Nether/End
└── managers/
    └── CicloManager.java                   # Ya existe (reutilizar)
```

---

## 🎯 Clase Principal: `Evento6MundoOlvidado.java`

### Estructura Base

```java
package com.riolubruh.apocalipsis.eventos;

import com.riolubruh.apocalipsis.Apocalipsis;
import com.riolubruh.apocalipsis.managers.CicloManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;

public class Evento6MundoOlvidado {
    
    private final Apocalipsis plugin;
    private final CicloManager cicloManager;
    
    private FileConfiguration config;
    private File configFile;
    
    // Estado del evento
    private boolean eventoActivo = false;
    private MundoOlvidadoFase faseActual = MundoOlvidadoFase.INACTIVO;
    private long tiempoInicio = 0;
    private Set<UUID> participantes = new HashSet<>();
    
    // Tareas programadas
    private BukkitRunnable tareaProgresion;
    private Map<MundoOlvidadoFase, BukkitRunnable> tareasActos = new HashMap<>();
    
    public Evento6MundoOlvidado(Apocalipsis plugin) {
        this.plugin = plugin;
        this.cicloManager = plugin.getCicloManager();
        cargarConfiguracion();
    }
    
    private void cargarConfiguracion() {
        configFile = new File(plugin.getDataFolder(), "evento6_mundo_olvidado.yml");
        if (!configFile.exists()) {
            plugin.saveResource("evento6_mundo_olvidado.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }
    
    /**
     * Iniciar el evento
     * @param iniciador Jugador que inició el evento
     * @return true si se inició correctamente
     */
    public boolean iniciarEvento(Player iniciador) {
        // Verificaciones previas
        if (eventoActivo) {
            iniciador.sendMessage(config.getString("mensajes.error_ya_activo"));
            return false;
        }
        
        if (!cicloManager.isEnabled()) {
            iniciador.sendMessage(config.getString("mensajes.error_ciclos_desactivado"));
            return false;
        }
        
        // Iniciar evento
        eventoActivo = true;
        tiempoInicio = System.currentTimeMillis();
        faseActual = MundoOlvidadoFase.ACTO_1_NORMALIDAD;
        
        // Registrar participantes (todos online)
        for (Player player : Bukkit.getOnlinePlayers()) {
            participantes.add(player.getUniqueId());
        }
        
        // Broadcast inicio
        Bukkit.broadcastMessage(config.getString("mensajes.evento_iniciado"));
        
        // Iniciar progresión automática
        iniciarProgresionAutomatica();
        
        // Log
        plugin.getLogger().info("[Evento 6] Iniciado por " + iniciador.getName());
        plugin.getLogger().info("[Evento 6] Participantes: " + participantes.size());
        
        return true;
    }
    
    /**
     * Sistema de progresión automática por tiempo
     */
    private void iniciarProgresionAutomatica() {
        tareaProgresion = new BukkitRunnable() {
            @Override
            public void run() {
                if (!eventoActivo) {
                    cancel();
                    return;
                }
                
                long tiempoTranscurrido = (System.currentTimeMillis() - tiempoInicio) / 1000; // segundos
                
                // Verificar cambio de acto según tiempo
                MundoOlvidadoFase siguienteAcuto = obtenerActoPorTiempo(tiempoTranscurrido);
                
                if (siguienteAcuto != faseActual) {
                    cambiarAActo(siguienteAcuto);
                }
            }
        };
        
        // Ejecutar cada 10 segundos
        tareaProgresion.runTaskTimer(plugin, 0L, 200L);
    }
    
    /**
     * Determina qué acto debe estar activo según el tiempo transcurrido
     */
    private MundoOlvidadoFase obtenerActoPorTiempo(long segundos) {
        if (segundos < 900) return MundoOlvidadoFase.ACTO_1_NORMALIDAD;
        else if (segundos < 1800) return MundoOlvidadoFase.ACTO_2_RAREZAS;
        else if (segundos < 3000) return MundoOlvidadoFase.ACTO_3_INESTABILIDAD;
        else if (segundos < 3300) return MundoOlvidadoFase.ACTO_4_QUIEBRE;
        else if (segundos < 3360) return MundoOlvidadoFase.ACTO_5_REINICIO;
        else if (segundos < 4200) return MundoOlvidadoFase.ACTO_6_NUEVO_MUNDO;
        else if (segundos < 5100) return MundoOlvidadoFase.ACTO_7_COMPRENSION;
        else if (segundos < 5700) return MundoOlvidadoFase.ACTO_8_FRACTURA;
        else if (segundos < 6300) return MundoOlvidadoFase.ACTO_9_END_PERMANECE;
        else if (segundos < 7200) return MundoOlvidadoFase.ACTO_10_CIERRE;
        else return MundoOlvidadoFase.COMPLETADO;
    }
    
    /**
     * Cambiar al acto especificado
     */
    private void cambiarAActo(MundoOlvidadoFase nuevoActo) {
        // Cancelar tareas del acto anterior
        if (tareasActos.containsKey(faseActual)) {
            tareasActos.get(faseActual).cancel();
        }
        
        // Actualizar fase
        MundoOlvidadoFase actoAnterior = faseActual;
        faseActual = nuevoActo;
        
        // Log
        plugin.getLogger().info("[Evento 6] Cambio: " + actoAnterior + " → " + nuevoActo);
        
        // Ejecutar lógica del nuevo acto
        switch (nuevoActo) {
            case ACTO_1_NORMALIDAD:
                ejecutarActo1();
                break;
            case ACTO_2_RAREZAS:
                ejecutarActo2();
                break;
            case ACTO_3_INESTABILIDAD:
                ejecutarActo3();
                break;
            case ACTO_4_QUIEBRE:
                ejecutarActo4();
                break;
            case ACTO_5_REINICIO:
                ejecutarActo5();
                break;
            case ACTO_6_NUEVO_MUNDO:
                ejecutarActo6();
                break;
            case ACTO_7_COMPRENSION:
                ejecutarActo7();
                break;
            case ACTO_8_FRACTURA:
                ejecutarActo8();
                break;
            case ACTO_9_END_PERMANECE:
                ejecutarActo9();
                break;
            case ACTO_10_CIERRE:
                ejecutarActo10();
                break;
            case COMPLETADO:
                finalizarEvento();
                break;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // IMPLEMENTACIÓN DE ACTOS
    // ═══════════════════════════════════════════════════════════════
    
    private void ejecutarActo1() {
        // ACTO 1: NORMALIDAD - Silencio casi total
        
        BukkitRunnable tarea = new BukkitRunnable() {
            int contador = 0;
            
            @Override
            public void run() {
                contador++;
                
                // A los 5 minutos (300 seg / 10 = 30 iteraciones)
                if (contador == 30) {
                    enviarMensajeObservador("§8[§7...§8]", false);
                }
            }
        };
        
        tarea.runTaskTimer(plugin, 0L, 200L); // Cada 10 segundos
        tareasActos.put(MundoOlvidadoFase.ACTO_1_NORMALIDAD, tarea);
    }
    
    private void ejecutarActo2() {
        // ACTO 2: PRIMERAS RAREZAS - Efectos sutiles
        
        BukkitRunnable tarea = new BukkitRunnable() {
            int contador = 0;
            
            @Override
            public void run() {
                contador++;
                
                // Mensaje a los 3 minutos
                if (contador == 18) {
                    enviarMensajeObservador("§8[§7...§8] §7Hmm...", false);
                }
                
                // Mensaje a los 9 minutos
                if (contador == 54) {
                    enviarMensajeObservador("§8[§7...§8] §7No todavía...", false);
                }
                
                // Efectos aleatorios cada 2-5 minutos
                if (contador % 12 == 0 && Math.random() < 0.4) {
                    ejecutarEfectoAleatorioRarezas();
                }
            }
        };
        
        tarea.runTaskTimer(plugin, 0L, 200L);
        tareasActos.put(MundoOlvidadoFase.ACTO_2_RAREZAS, tarea);
    }
    
    private void ejecutarActo3() {
        // ACTO 3: INESTABILIDAD - Efectos constantes
        
        // Mensaje inicial
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            enviarMensajeObservador("§8[§7...§8] §7Otra vez no...", true);
        }, 6000L); // 5 minutos
        
        BukkitRunnable tarea = new BukkitRunnable() {
            @Override
            public void run() {
                // Partículas de ceniza
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (participantes.contains(player.getUniqueId())) {
                        player.getWorld().spawnParticle(
                            org.bukkit.Particle.ASH,
                            player.getLocation().add(
                                Math.random() * 10 - 5,
                                Math.random() * 5,
                                Math.random() * 10 - 5
                            ),
                            5
                        );
                    }
                }
                
                // Efectos aleatorios adicionales
                if (Math.random() < 0.1) {
                    ejecutarEfectoInestabilidad();
                }
            }
        };
        
        tarea.runTaskTimer(plugin, 0L, 100L); // Cada 5 segundos
        tareasActos.put(MundoOlvidadoFase.ACTO_3_INESTABILIDAD, tarea);
    }
    
    private void ejecutarActo4() {
        // ACTO 4: EL QUIEBRE - Secuencia automática dramática
        
        // Paso 1: Ceguera (0 segundos)
        aplicarEfectoTodos(org.bukkit.potion.PotionEffectType.BLINDNESS, 100);
        reproducirSonidoTodos(org.bukkit.Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.0f);
        
        // Paso 2: Sonido profundo (5 segundos)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            reproducirSonidoTodos(org.bukkit.Sound.ENTITY_WITHER_SPAWN, 2.0f, 0.3f);
        }, 100L);
        
        // Paso 3: Vibración - Partículas (6 segundos)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (participantes.contains(player.getUniqueId())) {
                    player.getWorld().spawnParticle(
                        org.bukkit.Particle.ELECTRIC_SPARK,
                        player.getLocation(),
                        1000,
                        50, 50, 50,
                        0.5
                    );
                }
            }
        }, 120L);
        
        // Paso 4: Congelación (7 segundos)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            aplicarEfectoTodos(org.bukkit.potion.PotionEffectType.SLOWNESS, 60);
            aplicarEfectoTodos(org.bukkit.potion.PotionEffectType.MINING_FATIGUE, 60);
        }, 140L);
        
        // Paso 5: Mensaje final (10 segundos)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (participantes.contains(player.getUniqueId())) {
                    player.sendTitle(
                        "§8§l...",
                        "§cEl mundo ya tomó la decisión.",
                        20, 100, 30
                    );
                }
            }
            reproducirSonidoTodos(org.bukkit.Sound.ENTITY_ENDER_DRAGON_DEATH, 1.0f, 0.5f);
        }, 200L);
        
        // Paso 6: Silencio (14 segundos - 20 segundos de silencio)
        // El cambio al Acto 5 ocurrirá automáticamente por el timer
    }
    
    private void ejecutarActo5() {
        // ACTO 5: EL REINICIO - Usa el sistema de ciclos
        
        // Paso 1: Pantalla negra
        aplicarEfectoTodos(org.bukkit.potion.PotionEffectType.BLINDNESS, 80);
        
        // Paso 2: Crear nuevo ciclo (4 segundos)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            String nombreCicloNuevo = config.getString("evento.mundo_nuevo_nombre", "world_ciclo_reset");
            
            // Usar CicloManager para crear el ciclo
            boolean creado = cicloManager.crearCicloNuevo(
                nombreCicloNuevo,
                "NORMAL",
                "HARD",
                true,  // PVP
                true,  // Monsters
                true   // Animals
            );
            
            if (!creado) {
                plugin.getLogger().severe("[Evento 6] ERROR: No se pudo crear el nuevo ciclo!");
                return;
            }
            
            plugin.getLogger().info("[Evento 6] Nuevo ciclo creado: " + nombreCicloNuevo);
            
        }, 80L);
        
        // Paso 3: Teleportar todos (5 segundos)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            String nombreCicloNuevo = config.getString("evento.mundo_nuevo_nombre", "world_ciclo_reset");
            
            // Teleportar todos los participantes
            for (UUID uuid : participantes) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    cicloManager.teleportarJugadorACiclo(player, nombreCicloNuevo);
                }
            }
            
            plugin.getLogger().info("[Evento 6] Jugadores teleportados al nuevo ciclo");
            
        }, 100L);
        
        // Paso 4: Efectos de spawn (6 segundos)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (UUID uuid : participantes) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    player.getWorld().spawnParticle(
                        org.bukkit.Particle.END_ROD,
                        player.getLocation(),
                        200,
                        2, 2, 2,
                        0.1
                    );
                    player.playSound(
                        player.getLocation(),
                        org.bukkit.Sound.BLOCK_RESPAWN_ANCHOR_CHARGE,
                        1.0f, 1.0f
                    );
                }
            }
            
            // Dar items iniciales
            darItemsIniciales();
            
        }, 120L);
    }
    
    private void ejecutarActo6() {
        // ACTO 6: NUEVO MUNDO - Mensajes de revelación
        
        // Mensaje 1: "No los borró..." (10 segundos)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (participantes.contains(player.getUniqueId())) {
                    player.sendTitle(
                        "§8§o...",
                        "§7§oNo los borró...",
                        20, 60, 20
                    );
                }
            }
        }, 200L);
        
        // Mensaje 2: "Solo borró el lugar." (14 segundos)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            enviarMensajeObservador("§8[§7...§8] §7Solo borró el lugar.", false);
        }, 280L);
        
        // Mensaje 3: Pausa (5 minutos)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            enviarMensajeObservador("§8[§7...§8]", false);
        }, 6000L);
    }
    
    private void ejecutarActo7() {
        // ACTO 7: COMPRENSIÓN - Mensajes espaciados
        
        // Mensaje 1 (3 min)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (participantes.contains(player.getUniqueId())) {
                    player.sendTitle(
                        "§8§o...",
                        "§7§oEl mundo hace esto cuando se cansa.",
                        20, 60, 20
                    );
                }
            }
        }, 3600L);
        
        // Mensaje 2 (9 min)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            enviarMensajeObservador("§8[§7...§8] §7Reiniciar es más fácil que cambiar.", false);
        }, 10800L);
        
        // Mensaje 3 (13 min)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            enviarMensajeObservador("§8[§7...§8]", false);
        }, 15600L);
    }
    
    private void ejecutarActo8() {
        // ACTO 8: LA FRACTURA - Nether no se reseteó
        
        // Mensaje (2 min)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (participantes.contains(player.getUniqueId())) {
                    player.sendTitle(
                        "§8§o...",
                        "§7§oLo que está debajo...",
                        20, 60, 20
                    );
                }
            }
        }, 2400L);
        
        // Mensaje 2 (3 min)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            enviarMensajeObservador("§8[§7...§8] §7no olvida tan fácil.", false);
        }, 3600L);
    }
    
    private void ejecutarActo9() {
        // ACTO 9: EL END PERMANECE
        
        // Mensaje (3 min)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (participantes.contains(player.getUniqueId())) {
                    player.sendTitle(
                        "§8§o...",
                        "§7§oAlgunos lugares no se reinician.",
                        20, 60, 20
                    );
                }
            }
        }, 3600L);
        
        // Mensaje 2 (4 min)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            enviarMensajeObservador("§8[§7...§8] §7Solo observan.", false);
        }, 4800L);
    }
    
    private void ejecutarActo10() {
        // ACTO 10: CIERRE - Mensaje final y recompensas
        
        // Mensaje final 1 (10 min)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (participantes.contains(player.getUniqueId())) {
                    player.sendTitle(
                        "§8§l...",
                        "§7§oEste no es un comienzo.",
                        20, 60, 20
                    );
                }
            }
        }, 12000L);
        
        // Mensaje final 2 (11 min)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            enviarMensajeObservador("§8§l[§7§l...§8§l] §7Es una repetición.", false);
        }, 13200L);
        
        // Mensaje 3 (12 min)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            enviarMensajeObservador("§8[§7...§8]", false);
        }, 14400L);
        
        // Dar recompensas finales (14.5 min)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            darRecompensasFinales();
        }, 17400L);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // MÉTODOS AUXILIARES
    // ═══════════════════════════════════════════════════════════════
    
    private void enviarMensajeObservador(String mensaje, boolean esTitle) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (participantes.contains(player.getUniqueId())) {
                if (esTitle) {
                    player.sendTitle("§8§o...", mensaje, 20, 60, 20);
                } else {
                    player.sendMessage(mensaje);
                }
            }
        }
    }
    
    private void aplicarEfectoTodos(org.bukkit.potion.PotionEffectType efecto, int duracion) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (participantes.contains(player.getUniqueId())) {
                player.addPotionEffect(
                    new org.bukkit.potion.PotionEffect(efecto, duracion, 5, false, false)
                );
            }
        }
    }
    
    private void reproducirSonidoTodos(org.bukkit.Sound sonido, float volumen, float pitch) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (participantes.contains(player.getUniqueId())) {
                player.playSound(player.getLocation(), sonido, volumen, pitch);
            }
        }
    }
    
    private void ejecutarEfectoAleatorioRarezas() {
        // Implementar efectos sutiles del Acto 2
        // (trueno sin lluvia, cambio cielo, sonido lejano, mob estático)
    }
    
    private void ejecutarEfectoInestabilidad() {
        // Implementar efectos del Acto 3
        // (lag simulado, reloj detenido, portal sonidos)
    }
    
    private void darItemsIniciales() {
        // Dar 16 madera + 8 pan según config
    }
    
    private void darRecompensasFinales() {
        // Dar items finales y PS según config
    }
    
    private void finalizarEvento() {
        eventoActivo = false;
        
        // Cancelar todas las tareas
        if (tareaProgresion != null) {
            tareaProgresion.cancel();
        }
        for (BukkitRunnable tarea : tareasActos.values()) {
            if (tarea != null) {
                tarea.cancel();
            }
        }
        
        Bukkit.broadcastMessage(config.getString("mensajes.evento_completado"));
        plugin.getLogger().info("[Evento 6] Evento finalizado");
    }
    
    // Getters
    public boolean isEventoActivo() { return eventoActivo; }
    public MundoOlvidadoFase getFaseActual() { return faseActual; }
    public Set<UUID> getParticipantes() { return participantes; }
}
```

---

## 📝 Enum de Fases: `MundoOlvidadoFase.java`

```java
package com.riolubruh.apocalipsis.eventos;

public enum MundoOlvidadoFase {
    INACTIVO,
    ACTO_1_NORMALIDAD,
    ACTO_2_RAREZAS,
    ACTO_3_INESTABILIDAD,
    ACTO_4_QUIEBRE,
    ACTO_5_REINICIO,
    ACTO_6_NUEVO_MUNDO,
    ACTO_7_COMPRENSION,
    ACTO_8_FRACTURA,
    ACTO_9_END_PERMANECE,
    ACTO_10_CIERRE,
    COMPLETADO;
    
    @Override
    public String toString() {
        return name().replace("_", " ");
    }
}
```

---

## 🎮 Comando: Agregar a `ApocalipsisCommand.java`

```java
// En el método onCommand():

if (args[0].equalsIgnoreCase("evento6")) {
    if (!player.hasPermission("apocalipsis.evento6.admin")) {
        player.sendMessage("§c✗ No tienes permisos.");
        return true;
    }
    
    if (args.length < 2) {
        player.sendMessage("§c✗ Uso: /avo evento6 <start|stop|info>");
        return true;
    }
    
    Evento6MundoOlvidado evento6 = plugin.getEvento6();
    
    if (args[1].equalsIgnoreCase("start")) {
        if (evento6.iniciarEvento(player)) {
            player.sendMessage("§a✓ Evento 6 iniciado correctamente.");
        }
    }
    else if (args[1].equalsIgnoreCase("stop")) {
        // Implementar detener evento
    }
    else if (args[1].equalsIgnoreCase("info")) {
        player.sendMessage("§7━━━━━━━ §8Evento 6 §7━━━━━━━");
        player.sendMessage("§7Estado: " + (evento6.isEventoActivo() ? "§aActivo" : "§cInactivo"));
        player.sendMessage("§7Fase: §e" + evento6.getFaseActual());
        player.sendMessage("§7Participantes: §e" + evento6.getParticipantes().size());
    }
    
    return true;
}
```

---

## 🔌 Registrar en `Apocalipsis.java`

```java
public class Apocalipsis extends JavaPlugin {
    
    private Evento6MundoOlvidado evento6;
    
    @Override
    public void onEnable() {
        // ... código existente ...
        
        // Inicializar Evento 6
        evento6 = new Evento6MundoOlvidado(this);
        getLogger().info("[Evento 6] Sistema cargado correctamente.");
    }
    
    public Evento6MundoOlvidado getEvento6() {
        return evento6;
    }
}
```

---

## ✅ Checklist de Implementación

### Fase 1: Archivos Base
- [ ] Copiar `evento6_mundo_olvidado.yml` a `src/main/resources/`
- [ ] Crear `Evento6MundoOlvidado.java`
- [ ] Crear `MundoOlvidadoFase.java`
- [ ] Registrar en `Apocalipsis.java`

### Fase 2: Comandos
- [ ] Agregar comando `/avo evento6` en `ApocalipsisCommand.java`
- [ ] Agregar permisos en `plugin.yml`:
  ```yaml
  apocalipsis.evento6.admin:
    description: "Permite gestionar el Evento 6"
    default: op
  ```

### Fase 3: Integración con Ciclos
- [ ] Verificar que `CicloManager` está funcional
- [ ] Probar creación de ciclo nuevo
- [ ] Probar teleporte de jugadores
- [ ] Probar reseteo de datos

### Fase 4: Efectos Especiales
- [ ] Implementar efectos del Acto 2 (rarezas)
- [ ] Implementar efectos del Acto 3 (inestabilidad)
- [ ] Implementar secuencia del Acto 4 (quiebre)
- [ ] Implementar efectos Nether (Acto 8)
- [ ] Implementar efectos End (Acto 9)

### Fase 5: Items y Recompensas
- [ ] Implementar `darItemsIniciales()`
- [ ] Implementar `darRecompensasFinales()`
- [ ] Integrar con sistema de PS

### Fase 6: Testing
- [ ] Probar evento completo en servidor de desarrollo
- [ ] Verificar todos los actos funcionan
- [ ] Verificar timing correcto
- [ ] Verificar que Nether/End NO se resetean
- [ ] Verificar recompensas

---

## 🐛 Notas Importantes

### Dependencias

```xml
<!-- En pom.xml -->
<dependencies>
    <dependency>
        <groupId>org.spigotmc</groupId>
        <artifactId>spigot-api</artifactId>
        <version>1.21-R0.1-SNAPSHOT</version>
    </dependency>
    
    <!-- Multiverse-Core (opcional pero recomendado) -->
    <dependency>
        <groupId>com.onarandombox.multiversecore</groupId>
        <artifactId>Multiverse-Core</artifactId>
        <version>4.3.1</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

### Consideraciones de Rendimiento

1. **Partículas:** Limitar cantidad para evitar lag
2. **Tareas asíncronas:** Usar `runTaskTimerAsynchronously()` cuando sea posible
3. **Backup:** Crear backup ANTES del Acto 5 (reinicio)

### Seguridad

1. **Permisos:** Solo admins pueden iniciar
2. **Validación:** Verificar que ciclos está activo
3. **Cancelación:** Permitir detener evento en emergencia

---

## 📚 Referencias

- [evento6_mundo_olvidado.yml](src/main/resources/evento6_mundo_olvidado.yml) - Configuración
- [EVENTO6_MUNDO_OLVIDADO.md](EVENTO6_MUNDO_OLVIDADO.md) - Documentación narrativa
- [SISTEMA_CICLOS.md](SISTEMA_CICLOS.md) - Sistema de ciclos
- [CicloManager.java](src/main/java/com/riolubruh/apocalipsis/managers/CicloManager.java) - Gestión de ciclos

---

**Última actualización:** 2026-01-26  
**Versión:** 1.0.0  
**Estado:** Listo para implementar
