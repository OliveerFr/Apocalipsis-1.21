# 🧩 MINI-EVENTO: "El Camino al End"
## 📖 Plan de Implementación Técnica

---

## 📋 ÍNDICE

1. [Contexto Narrativo](#contexto-narrativo)
2. [Arquitectura del Sistema](#arquitectura-del-sistema)
3. [Estructura de Archivos](#estructura-de-archivos)
4. [Fases del Evento](#fases-del-evento)
5. [Sistema de Fragmentos del Eco](#sistema-de-fragmentos-del-eco)
6. [Implementación de Anomalías](#implementación-de-anomalías)
7. [Sistema de Mensajes](#sistema-de-mensajes)
8. [Comandos y Admin](#comandos-y-admin)
9. [Configuración YAML](#configuración-yaml)
10. [Tasklist de Implementación](#tasklist-de-implementación)

---

## 🎯 CONTEXTO NARRATIVO

## 🎭 PERSONALIDAD DEL OBSERVADOR

### ¿Quién es el Observador?

**Naturaleza:**
- No es un NPC ni una entidad física
- Es una **consciencia que vigila sin ser vista**
- Percibe eventos que "aún no ocurren" o "que ya ocurrieron"
- Habla en **pensamientos**, no diálogos directos
- Testigo de acontecimientos, no arquitecto

**Estilo Narrativo:**
- 💭 **Reflexivo y críptico**: No explica directamente, deja pistas
- 🕯️ **Itálicas y comillas**: `§7§o"Esto… no estaba aquí antes."`
- ⏸️ **Pausas con elipsis**: `"El mundo no recuerda así… Esto viene de más lejos."`
- 🧩 **Fragmentado**: Pensamientos sueltos, no conversaciones completas
- 🔍 **Retrospectivo**: `"Como lo hicieron en aquel mundo…"` (referencias al pasado)

**Tono Emocional:**
- 😔 **Preocupación sutil**: No alarma, pero inquieta
- 🤔 **Confusión genuina**: Incluso él no entiende todo
- 💫 **Nostalgia**: Recuerda mundos caídos, ecos pasados
- ⚠️ **Advertencias veladas**: No ordena, sugiere

### Formato Visual del Observador

**En eventos anteriores usa:**
```
§5§l⚡ EL OBSERVADOR
```

**Para pensamientos usa:**
```
§7§o"Texto del pensamiento…"
```

**Ejemplos reales de otros eventos:**

**Eco de Brasas:**
```yaml
§5§l⚡ EL OBSERVADOR: §7§oEl portal no duerme, solo respira más lento.
§5§l⚡ EL OBSERVADOR: §7§oLa tierra quiebra donde ustedes caminan.
§5§l⚡ EL OBSERVADOR: §7§oEl fuego busca forma… no enemigos.
```

**Eco de las Sombras:**
```yaml
§7§o"No deberían moverse solas… eso pasó antes… y terminó mal."
§7§o"Estiran su forma buscando un anfitrión… como lo hicieron en aquel mundo…"
§7§o"Sellan la herida, pero no la causa…"
```

**El Susurro en la Piedra Rota:**
```yaml
§7§o"El mundo comienza a recordar mal…"
§7§o"Fragmentos de memoria se manifiestan físicamente."
```

---

## 🎭 NARRATIVA: EL CAMINO AL END

### Diálogos del Observador en Este Evento

**Filosofía del Evento:**
Este mini-evento es una **pausa reflexiva** entre eventos grandes. El Observador percibe algo nuevo, algo que no encaja con el Nether ni el Overworld. No es amenazante (aún), solo... extraño.

**Tono de los Mensajes:**
- 🤔 **Confusión genuina**: "No reconozco esta resonancia"
- 🔍 **Descubrimiento progresivo**: De "algo raro" a "es un rastro"
- 🧩 **Conexión con el pasado**: "El Nether fue solo una puerta más"
- 🎯 **Dirección sutil**: No dice "ve al End", dice "algo espera"

---

### Historia Previa
- **Eco de Brasas**: El Nether se abrió, el calor se filtró
- **Eco de las Sombras**: Las sombras se movieron, algo "de más lejos"
- **Observador**: Consciencia que vigila, no explica directamente

### Propósito del Mini-Evento
✅ **QUÉ ES:**
- Una transición sutil entre eventos grandes
- Una pista de que "algo más" existe (El End)
- Experiencia de 2-3 horas, NO combate pesado
- Exploración > Combate

❌ **QUÉ NO ES:**
- NO es un boss fight
- NO resuelve nada (solo abre preguntas)
- NO menciona Neo/La Muerte directamente
- NO explica todo el lore

### Mensaje Central
> "El mundo no está completo. Hay algo que no pertenece al Overworld ni al Nether."

---

## 🏗️ ARQUITECTURA DEL SISTEMA

### Estructura Base
```
EventBase (clase abstracta existente)
    ↓
CaminoEndEvent (nueva clase)
```

### Diferencias con otros eventos
| Característica | Eco de Brasas/Sombras | El Camino al End |
|----------------|------------------------|------------------|
| **Duración** | 2-3 horas | 2-3 horas |
| **Fases** | 3-6 actos | 3 fases simples |
| **Combate** | Alto (bosses, oleadas) | Bajo (exploración) |
| **Items** | Fragmentos para ritual | Fragmentos coleccionables |
| **Objetivo** | Completar ritual/boss | Recolectar + entender |
| **Progresión** | Lineal (fases secuenciales) | Paralela (global) |

---

## 📁 ESTRUCTURA DE ARCHIVOS

### Archivos Nuevos a Crear

```
src/main/java/me/apocalipsis/events/
├── CaminoEndEvent.java                  # Clase principal del evento
├── CaminoEndListener.java               # Listener de interacciones
└── CaminoEndItems.java                  # Factory de items únicos

src/main/resources/
└── camino_end.yml                        # Configuración del evento
```

### Archivos a Modificar

```
src/main/java/me/apocalipsis/
├── Apocalipsis.java                      # Registrar evento en onEnable()
├── commands/ApocalipsisCommand.java      # Añadir comandos /avo evento4 (alias: caminoend)
└── commands/AvoTabCompleter.java         # Autocompletado

src/main/resources/
└── eventos.yml                           # Añadir sección camino_end
```

---

## 🎬 FASES DEL EVENTO

### ⏱️ Duración Total: 2-3 horas (7200-10800 ticks)

---

### 🔹 FASE 1: ANOMALÍAS (30-45 minutos)

#### Objetivo
Crear ambiente de "algo raro está pasando" sin explicar qué.

#### Mecánicas

**1. Partículas Raras**
```yaml
anomalias:
  particulas:
    - tipo: END_ROD
      intervalo_ticks: 200      # Cada 10 segundos
      cantidad: 3-5
      spawn_aleatorio: true
      radio_spawn: 300          # 300 bloques del mundo spawn
    
    - tipo: PORTAL
      intervalo_ticks: 400      # Cada 20 segundos
      cantidad: 10-20
      spawn_cercano_jugadores: true
      radio: 50
```

**2. Sonidos del End (leves)**
```yaml
sonidos_ambiente:
  - sound: ENTITY_ENDERMAN_AMBIENT
    intervalo_ticks: 600        # Cada 30 segundos
    volumen: 0.3                # Muy bajo
    pitch: 0.6
    aleatorio: true
  
  - sound: BLOCK_END_PORTAL_FRAME_FILL
    intervalo_ticks: 900        # Cada 45 segundos
    volumen: 0.2
    pitch: 0.8
    solo_cerca_anomalias: true
```

**3. Mensajes del Observador**
```yaml
mensajes_observador:
  intervalo_ticks: 2400         # Cada 2 minutos
  prefijo: "§5§l⚡ EL OBSERVADOR"  # Consistente con otros eventos
  formato_pensamiento: "§7§o\"[mensaje]\""  # Itálicas + comillas
  
  mensajes:
    # Confusión inicial
    - "§7§o\"Esto… no estaba aquí antes.\""
    - "§7§o\"El mundo está mostrando grietas que no llevan a ningún lado.\""
    
    # Reconocimiento
    - "§7§o\"No es del Nether. No es del Overworld.\""
    - "§7§o\"Hay una resonancia que no reconozco.\""
    
    # Reflexión
    - "§7§o\"Algo… resiste.\""
    - "§7§o\"El aire vibra de forma extraña…\""
    - "§7§o\"Esto no se comporta como los ecos anteriores.\""
```

#### Mecánica: Puntos de Anomalía

**Spawn de Anomalías**
```java
// Spawn aleatorio en mundo
Location anomaliaLoc = getRandomLocationInRange(300);

// Marcar visualmente
world.spawnParticle(Particle.END_ROD, anomaliaLoc, 50, 1, 2, 1, 0.1);
world.spawnParticle(Particle.PORTAL, anomaliaLoc, 100, 2, 3, 2, 0.5);

// Sonido posicional
world.playSound(anomaliaLoc, Sound.BLOCK_BEACON_AMBIENT, 0.4f, 0.6f);

// Hologram (opcional)
// "§d§o???"
```

**Interacción del Jugador**
- Al acercarse a una anomalía (5 bloques):
  - Efecto breve: `SLOW_DIGGING` (1 segundo, nivel 1)
  - Mensaje: `"§8§oEl aire vibra de forma extraña..."`
  - Partículas propias: `DRAGON_BREATH`

---

### 🔹 FASE 2: ECOS (45-60 minutos)

#### Objetivo
Jugadores encuentran estructuras/puntos que dropean "Fragmentos del Eco".

#### Mecánicas

**1. Estructuras/Altares Simples**
```yaml
estructuras:
  cantidad: 8-12                # 8-12 estructuras en el mundo
  tipos:
    - FRAGMENTO_SUSPENDIDO      # Bloque flotante con partículas
    - ALTAR_ROTO                # Pequeña plataforma de end stone
    - RUINA_INCOMPLETA          # 3-5 bloques de purpur dispersos
  
  distancia_spawn:
    min: 150                    # Mínimo 150 bloques del spawn
    max: 500                    # Máximo 500 bloques
  
  separacion_minima: 100        # Mínimo 100 bloques entre estructuras
```

**2. Fragmentos del Eco (nuevo item)**
```yaml
items:
  fragmento_eco:
    material: ECHO_SHARD
    nombre: "§5Fragmento del Eco"
    lore:
      - "§8El Camino al End"
      - ""
      - "§7Un rastro de algo lejano."
      - "§7No reacciona como los fragmentos del Nether."
      - "§7Solo… resuena."
      - ""
      - "§8§o\"Esto no es corrupción. Es un rastro.\""
    glow: true
    drop_por_estructura: 2-4    # 2-4 fragmentos por estructura
```

**3. Sistema de Recolección Global**
```yaml
recoleccion:
  meta_global: 40               # 40 fragmentos en total (todos los jugadores)
  tracking: true                # Trackear progreso global
  
  notificaciones:
    cada_fragmentos: 10         # Notificar cada 10 fragmentos
    mensaje: "§5§l⬢ §7Fragmentos recolectados: §d{count}§7/§d40"
```

#### Interacción con Estructuras

**Tipo 1: Fragmento Suspendido**
```java
// End Rod flotante + End Stone debajo
// Click derecho para recoger fragmentos
public void onPlayerInteract(PlayerInteractEvent event) {
    if (clickedBlock.getType() == Material.END_ROD) {
        // Dar 2-4 fragmentos
        int cantidad = 2 + random.nextInt(3); // 2-4
        ItemStack fragmento = items.crearFragmentoEco();
        fragmento.setAmount(cantidad);
        player.getInventory().addItem(fragmento);
        
        // Efectos
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, 1.2f);
        player.spawnParticle(Particle.PORTAL, clickedBlock.getLocation(), 50);
        
        // Remover estructura
        clickedBlock.setType(Material.AIR);
        clickedBlock.getRelative(0, -1, 0).setType(Material.AIR);
        
        // Mensaje
        player.sendMessage("§d§l✦ §7Has obtenido §5" + cantidad + " Fragmentos del Eco");
        
        // Actualizar contador global
        incrementarContadorGlobal(cantidad);
    }
}
```

**Tipo 2: Altar Roto**
```java
// Plataforma 3x3 de End Stone con Purpur Pillar en centro
// Interactuar = spawn items + romper altar
```

**Tipo 3: Ruina Incompleta**
```java
// 3-5 bloques de Purpur dispersos en área 5x5
// Romper cualquiera = drop fragmentos + despawn resto
```

---

### 🔹 FASE 3: REVELACIÓN SUTIL (15-30 minutos)

#### Objetivo
Al alcanzar 40 fragmentos globales, el Observador deja caer la pista del End.

#### Trigger de Revelación
```yaml
revelacion:
  trigger_fragmentos: 40        # Al recolectar 40 fragmentos globalmente
  
  mensajes_secuencia:
    - delay: 0
      mensaje: "§5§l⚡ EL OBSERVADOR §r§7detecta algo..."
      broadcast: true
    
    - delay: 100                # 5 segundos
      mensaje: "§8§o\"Esto no es corrupción.\""
      broadcast: true
    
    - delay: 200                # 10 segundos
      mensaje: "§8§o\"Es un rastro.\""
      broadcast: true
    
    - delay: 300                # 15 segundos
      mensaje: "§7§o\"El Nether se abrió… pero no fue el final de nada.\""
      broadcast: true
    
    - delay: 400                # 20 segundos
      mensaje: "§7§o\"Solo fue una puerta más.\""
      broadcast: true
```

#### Cierre del Evento (Cliffhanger)
```yaml
cierre:
  delay_tras_revelacion: 600    # 30 segundos tras última mensaje
  
  mensajes_finales:
    - "§d§l━━━━━━━━━━━━━━━━━━━━━━━"
    - ""
    - "§5§l⚡ EL OBSERVADOR §r§7susurra..."
    - ""
    - "§8§o\"El camino final no se abre destruyendo…\""
    - "§8§o\"…sino aceptando lo que quedó atrás.\""
    - ""
    - "§7Algo ha sido §d§omarcado§7."
    - ""
    - "§d§l━━━━━━━━━━━━━━━━━━━━━━━"
  
  efectos_globales:
    - particulas:
        tipo: END_GATEWAY
        cantidad: 1000
        spawn: world_spawn
    
    - sonido:
        sound: BLOCK_END_PORTAL_SPAWN
        volumen: 1.0
        pitch: 0.7
  
  marcar_ubicacion:
    enabled: true
    tipo: PORTAL_INCOMPLETO      # Estructura de End Portal Frame sin ojos
    coordenadas_relativas: "0, 64, 0"  # Relativo al spawn
    mensaje_coordenadas: "§7Coordenadas: §d{x} {y} {z}"
```

#### Portal Incompleto
```java
// Generar estructura de End Portal sin Eyes of Ender
public void generarPortalIncompleto(Location center) {
    // Frame de 5x5 (solo marco, sin bloques internos)
    // END_PORTAL_FRAME sin eyes
    // Partículas PORTAL constantes
    // Hologram: "§d§oSellado"
    
    // Guardar ubicación en config para futuro evento del End
    config.set("portal_end.location.x", center.getX());
    config.set("portal_end.location.y", center.getY());
    config.set("portal_end.location.z", center.getZ());
    config.set("portal_end.sellado", true);
    saveConfig();
}
```

---

## 💎 SISTEMA DE FRAGMENTOS DEL ECO

### Item: Fragmento del Eco

```java
public class CaminoEndItems {
    
    /**
     * Crear Fragmento del Eco (item coleccionable)
     */
    public static ItemStack crearFragmentoEco() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§5Fragmento del Eco");
            
            List<String> lore = new ArrayList<>();
            lore.add("§8Ecos que no deberían existir");
            lore.add("");
            lore.add("§7Un rastro de algo lejano.");
            lore.add("§7No reacciona como los fragmentos del Nether.");
            lore.add("§7Solo… resuena.");
            lore.add("");
            lore.add("§8§o\"Esto no es corrupción. Es un rastro.\"");
            
            meta.setLore(lore);
            meta.addEnchant(Enchantment.LUCK, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
}
```

### Tracking Global de Fragmentos

```java
// En EcosQueNoDeberianEvent.java
private int fragmentosRecolectadosGlobalmente = 0;
private static final int META_FRAGMENTOS = 40;

public void incrementarContadorGlobal(int cantidad) {
    fragmentosRecolectadosGlobalmente += cantidad;
    
    // Notificar cada 10 fragmentos
    if (fragmentosRecolectadosGlobalmente % 10 == 0) {
        messageBus.broadcast(
            "§5§l⬢ §7Fragmentos recolectados: §d" + fragmentosRecolectadosGlobalmente + "§7/§d" + META_FRAGMENTOS,
            "fragment_milestone"
        );
        soundUtil.playSoundAll(Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
    }
    
    // Trigger revelación al alcanzar meta
    if (fragmentosRecolectadosGlobalmente >= META_FRAGMENTOS && faseActual != Fase.REVELACION) {
        iniciarFaseRevelacion();
    }
}
```

---

## 🌀 IMPLEMENTACIÓN DE ANOMALÍAS

### Clase: Anomalia

```java
public class Anomalia {
    private final Location location;
    private final AnomaliaType tipo;
    private boolean activa;
    private BukkitTask particleTask;
    private BukkitTask soundTask;
    
    public enum AnomaliaType {
        FRAGMENTO_SUSPENDIDO,
        ALTAR_ROTO,
        RUINA_INCOMPLETA
    }
    
    public Anomalia(Location loc, AnomaliaType tipo) {
        this.location = loc;
        this.tipo = tipo;
        this.activa = true;
    }
    
    public void spawn() {
        World world = location.getWorld();
        
        switch (tipo) {
            case FRAGMENTO_SUSPENDIDO:
                spawnFragmentoSuspendido(world);
                break;
            case ALTAR_ROTO:
                spawnAltarRoto(world);
                break;
            case RUINA_INCOMPLETA:
                spawnRuinaIncompleta(world);
                break;
        }
        
        iniciarEfectos();
    }
    
    private void spawnFragmentoSuspendido(World world) {
        // End Stone base
        location.getBlock().setType(Material.END_STONE);
        
        // End Rod flotante encima
        Location rodLoc = location.clone().add(0, 2, 0);
        rodLoc.getBlock().setType(Material.END_ROD);
    }
    
    private void spawnAltarRoto(World world) {
        // Plataforma 3x3 de End Stone
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Location blockLoc = location.clone().add(x, 0, z);
                blockLoc.getBlock().setType(Material.END_STONE);
            }
        }
        
        // Purpur Pillar en centro
        location.clone().add(0, 1, 0).getBlock().setType(Material.PURPUR_PILLAR);
    }
    
    private void spawnRuinaIncompleta(World world) {
        // 3-5 bloques de Purpur dispersos en área 5x5
        int bloques = 3 + new Random().nextInt(3); // 3-5
        
        for (int i = 0; i < bloques; i++) {
            int offsetX = -2 + new Random().nextInt(5);
            int offsetZ = -2 + new Random().nextInt(5);
            Location blockLoc = location.clone().add(offsetX, 0, offsetZ);
            blockLoc.getBlock().setType(Material.PURPUR_BLOCK);
        }
    }
    
    private void iniciarEfectos() {
        World world = location.getWorld();
        
        // Partículas cada segundo
        particleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!activa) return;
            
            world.spawnParticle(Particle.END_ROD, location.clone().add(0, 2, 0), 5, 0.5, 1, 0.5, 0.01);
            world.spawnParticle(Particle.PORTAL, location.clone().add(0, 1, 0), 10, 1, 1, 1, 0.1);
        }, 0L, 20L);
        
        // Sonido cada 15 segundos
        soundTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!activa) return;
            world.playSound(location, Sound.BLOCK_BEACON_AMBIENT, 0.3f, 0.6f);
        }, 0L, 300L);
    }
    
    public void despawn() {
        activa = false;
        
        if (particleTask != null) particleTask.cancel();
        if (soundTask != null) soundTask.cancel();
        
        // Limpiar bloques
        // (implementación según tipo)
    }
}
```

---

## 💬 SISTEMA DE MENSAJES

### Mensajes del Observador (Fase 1)

```java
private void tickFaseAnomalias() {
    ticksEnFase++;
    
    // Mensajes cada 2 minutos
    if (ticksEnFase % 2400 == 0) {
        enviarMensajeObservadorAleatorio();
    }
    
    // Spawn anomalías
    if (ticksEnFase % 600 == 0) { // Cada 30 segundos
        spawnAnomaliaAleatoria();
    }
}

private void enviarMensajeObservadorAleatorio() {
    String[] mensajes = {
        "§8§o\"Esto… no estaba aquí antes.\"",
        "§8§o\"El mundo está mostrando grietas que no llevan a ningún lado.\"",
        "§8§o\"No es del Nether. No es del Overworld.\"",
        "§8§o\"Algo… resiste.\"",
        "§8§o\"Hay una resonancia que no reconozco.\""
    };
    
    String mensaje = mensajes[new Random().nextInt(mensajes.length)];
    messageBus.broadcast("§5§l⚡ EL OBSERVADOR §r§7percibe...", "observador");
    
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
        messageBus.broadcast(mensaje, "observador_pensamiento");
    }, 20L);
}
```

### Secuencia de Revelación (Fase 3)

```java
private void iniciarFaseRevelacion() {
    faseActual = Fase.REVELACION;
    ticksEnFase = 0;
    
    // Detener spawns de anomalías
    cancelarTareasAnomalias();
    
    // Secuencia de mensajes
    messageBus.broadcast("§5§l⚡ EL OBSERVADOR §r§7detecta algo...", "revelacion");
    soundUtil.playSoundAll(Sound.BLOCK_END_PORTAL_FRAME_FILL, 1.0f, 0.8f);
    
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
        messageBus.broadcast("§8§o\"Esto no es corrupción.\"", "revelacion");
    }, 100L);
    
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
        messageBus.broadcast("§8§o\"Es un rastro.\"", "revelacion");
    }, 200L);
    
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
        messageBus.broadcast("§7§o\"El Nether se abrió… pero no fue el final de nada.\"", "revelacion");
    }, 300L);
    
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
        messageBus.broadcast("§7§o\"Solo fue una puerta más.\"", "revelacion");
        iniciarCierre();
    }, 400L);
}

private void iniciarCierre() {
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
        // Mensajes finales
        messageBus.broadcast("§d§l━━━━━━━━━━━━━━━━━━━━━━━", "cierre");
        messageBus.broadcast("", "space");
        messageBus.broadcast("§5§l⚡ EL OBSERVADOR §r§7susurra...", "cierre");
        messageBus.broadcast("", "space");
        messageBus.broadcast("§8§o\"El camino final no se abre destruyendo…\"", "cierre");
        messageBus.broadcast("§8§o\"…sino aceptando lo que quedó atrás.\"", "cierre");
        messageBus.broadcast("", "space");
        messageBus.broadcast("§7Algo ha sido §d§omarcado§7.", "cierre");
        messageBus.broadcast("", "space");
        messageBus.broadcast("§d§l━━━━━━━━━━━━━━━━━━━━━━━", "cierre");
        
        // Efectos visuales
        World world = Bukkit.getWorld("world");
        Location spawn = world.getSpawnLocation();
        
        world.spawnParticle(Particle.END_GATEWAY, spawn, 1000, 10, 10, 10, 0.5);
        soundUtil.playSoundAll(Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 0.7f);
        
        // Generar portal incompleto
        Location portalLoc = spawn.clone().add(0, 64, 0);
        generarPortalIncompleto(portalLoc);
        
        // Mostrar coordenadas
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            messageBus.broadcast(
                String.format("§7Coordenadas: §d%d %d %d", 
                    portalLoc.getBlockX(), 
                    portalLoc.getBlockY(), 
                    portalLoc.getBlockZ()
                ),
                "coordenadas"
            );
        }, 100L);
        
        // Finalizar evento
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            finalizarEvento();
        }, 600L);
        
    }, 600L); // 30 segundos tras última revelación
}
```

---

## ⚙️ COMANDOS Y ADMIN

### Convención de Comandos

Siguiendo la convención de eventos existentes:
- **Eco de Brasas**: `/avo eco`
- **Eco de las Sombras**: `/avo eco_sombras`
- **El Susurro en la Piedra Rota**: `/avo evento3` (alias: `susurro`)
- **Navidad**: `/avo navidad`

**El Camino al End** usará:
- **Comando principal**: `/avo evento4`
- **Alias alternativo**: `/avo caminoend`

### Comandos de Control

```java
// En ApocalipsisCommand.java

case "evento4":
case "caminoend":
case "caminoalend":
    if (args.length < 2) {
        sender.sendMessage("§e/avo evento4 <start|stop|info|fase|fragmentos>");
        sender.sendMessage("§8Alias: §7/avo caminoend");
        return true;
    }
    
    String subCmd = args[1].toLowerCase();
    switch (subCmd) {
        case "start":
            if (eventController.hasActiveEvent()) {
                sender.sendMessage("§cYa hay un evento activo");
                return true;
            }
            
            eventController.startEvent("camino_end");
            sender.sendMessage("§a✓ Evento 'El Camino al End' iniciado");
            break;
        
        case "stop":
            eventController.stopActiveEvent();
            sender.sendMessage("§a✓ Evento detenido");
            break;
        
        case "info":
            CaminoEndEvent evento = getEventoCaminoEnd();
            if (evento == null || !evento.isActive()) {
                sender.sendMessage("§cEvento no está activo");
                return true;
            }
            
            sender.sendMessage("§e━━━━━ §dCAMINO AL END §e━━━━━");
            sender.sendMessage("§7Fase: §f" + evento.getFaseActual());
            sender.sendMessage("§7Tiempo en fase: §f" + (evento.getTicksEnFase() / 20) + "s");
            sender.sendMessage("§7Fragmentos recolectados: §d" + evento.getFragmentosGlobales() + "§7/§d40");
            sender.sendMessage("§7Anomalías activas: §f" + evento.getAnomaliasActivas());
            break;
        
        case "fase":
            if (args.length < 3) {
                sender.sendMessage("§cUso: /avo evento4 fase <anomalias|ecos|revelacion>");
                return true;
            }
            
            String faseNombre = args[2].toLowerCase();
            evento = getEventoCaminoEnd();
            if (evento == null || !evento.isActive()) {
                sender.sendMessage("§cEvento no está activo");
                return true;
            }
            
            switch (faseNombre) {
                case "anomalias":
                    evento.forzarFase(Fase.ANOMALIAS);
                    sender.sendMessage("§a✓ Fase cambiada a: ANOMALÍAS");
                    break;
                case "ecos":
                    evento.forzarFase(Fase.ECOS);
                    sender.sendMessage("§a✓ Fase cambiada a: ECOS");
                    break;
                case "revelacion":
                    evento.forzarFase(Fase.REVELACION);
                    sender.sendMessage("§a✓ Fase cambiada a: REVELACIÓN");
                    break;
                default:
                    sender.sendMessage("§cFase inválida");
            }
            break;
        
        case "fragmentos":
            if (args.length < 3) {
                sender.sendMessage("§cUso: /avo evento4 fragmentos <set|add> <cantidad>");
                return true;
            }
            
            String accion = args[2].toLowerCase();
            int cantidad = Integer.parseInt(args[3]);
            
            evento = getEventoCaminoEnd();
            if (evento == null || !evento.isActive()) {
                sender.sendMessage("§cEvento no está activo");
                return true;
            }
            
            if (accion.equals("set")) {
                evento.setFragmentosGlobales(cantidad);
                sender.sendMessage("§a✓ Fragmentos establecidos a: " + cantidad);
            } else if (accion.equals("add")) {
                evento.incrementarContadorGlobal(cantidad);
                sender.sendMessage("§a✓ Añadidos " + cantidad + " fragmentos");
            }
            break;
    }
    break;
```

### Tab Completer

```java
// En AvoTabCompleter.java

if (args.length == 2 && (args[0].equalsIgnoreCase("evento4") || 
                         args[0].equalsIgnoreCase("caminoend"))) {
    return Arrays.asList("start", "stop", "info", "fase", "fragmentos");
}

if (args.length == 3 && (args[0].equalsIgnoreCase("evento4") || 
                         args[0].equalsIgnoreCase("caminoend"))) {
    if (args[1].equalsIgnoreCase("fase")) {
        return Arrays.asList("anomalias", "ecos", "revelacion");
    }
    if (args[1].equalsIgnoreCase("fragmentos")) {
        return Arrays.asList("set", "add");
    }
}
```

---

## 📄 CONFIGURACIÓN YAML

### Archivo: `camino_end.yml`

```yaml
# ═══════════════════════════════════════════════════════════════════
# EL CAMINO AL END - Mini-evento de transición
# ═══════════════════════════════════════════════════════════════════
# Propósito: Introducir la existencia del End sin explicar todo
# Duración: 2-3 horas
# Enfoque: Exploración > Combate
# ═══════════════════════════════════════════════════════════════════

camino_end:
  enabled: true
  duracion_total_seg: 9000      # 150 minutos (2.5 horas)
  
  # ═══════════════════════════════════════════════════════════════
  # FASE 1: ANOMALÍAS (30 minutos)
  # ═══════════════════════════════════════════════════════════════
  fase_anomalias:
    duracion_seg: 1800
    
    particulas_ambiente:
      end_rod:
        tipo: "END_ROD"
        intervalo_ticks: 200    # Cada 10 segundos
        cantidad: 5
        spawn_aleatorio: true
        radio_spawn: 300        # Desde spawn
      
      portal:
        tipo: "PORTAL"
        intervalo_ticks: 400    # Cada 20 segundos
        cantidad: 15
        spawn_cercano_jugadores: true
        radio: 50
    
    sonidos_ambiente:
      enderman:
        sound: "ENTITY_ENDERMAN_AMBIENT"
        intervalo_ticks: 600    # Cada 30 segundos
        volumen: 0.3
        pitch: 0.6
        aleatorio: true
      
      portal_frame:
        sound: "BLOCK_END_PORTAL_FRAME_FILL"
        intervalo_ticks: 900    # Cada 45 segundos
        volumen: 0.2
        pitch: 0.8
    
    mensajes_observador:
      intervalo_ticks: 2400     # Cada 2 minutos
      prefijo: "§5§l⚡ EL OBSERVADOR"  # Consistente con otros eventos
      formato_pensamiento: "§7§o\"[mensaje]\""  # Itálicas + comillas
      sonido:
        sound: "BLOCK_SCULK_SENSOR_CLICKING"
        volumen: 0.3
        pitch: 0.8
      
      mensajes:
        - "§7§o\"Esto… no estaba aquí antes.\""
        - "§7§o\"El mundo está mostrando grietas que no llevan a ningún lado.\""
        - "§7§o\"No es del Nether. No es del Overworld.\""
        - "§7§o\"Algo… resiste.\""
        - "§7§o\"Hay una resonancia que no reconozco.\""
        - "§7§o\"El aire vibra de forma extraña…\""
        - "§7§o\"Esto no se comporta como los ecos anteriores.\""
    
    spawn_anomalias:
      intervalo_ticks: 600      # Cada 30 segundos una anomalía
      max_simultaneas: 5        # Máximo 5 anomalías activas
  
  # ═══════════════════════════════════════════════════════════════
  # FASE 2: ECOS (60 minutos)
  # ═══════════════════════════════════════════════════════════════
  fase_ecos:
    duracion_seg: 3600
    
    estructuras:
      cantidad_total: 12        # 12 estructuras en total
      
      tipos:
        fragmento_suspendido:
          probabilidad: 0.5     # 50% chance
          bloques:
            - "END_STONE (base)"
            - "END_ROD (flotante +2Y)"
          drop_fragmentos: 3    # 3 fragmentos fijos
        
        altar_roto:
          probabilidad: 0.3     # 30% chance
          bloques:
            - "END_STONE (plataforma 3x3)"
            - "PURPUR_PILLAR (centro)"
          drop_fragmentos: 4    # 4 fragmentos fijos
        
        ruina_incompleta:
          probabilidad: 0.2     # 20% chance
          bloques:
            - "PURPUR_BLOCK (3-5 dispersos)"
          drop_fragmentos: 2    # 2 fragmentos fijos
      
      spawn:
        distancia_min: 150      # Mínimo 150 bloques del spawn
        distancia_max: 500      # Máximo 500 bloques
        separacion_minima: 100  # Separación entre estructuras
      
      efectos:
        particulas:
          - tipo: "END_ROD"
            cantidad: 5
            intervalo_ticks: 20
          - tipo: "PORTAL"
            cantidad: 10
            intervalo_ticks: 20
        
        sonido:
          sound: "BLOCK_BEACON_AMBIENT"
          volumen: 0.3
          pitch: 0.6
          intervalo_ticks: 300  # Cada 15 segundos
    
    recoleccion:
      meta_global: 40           # 40 fragmentos para completar
      
      notificaciones:
        cada_fragmentos: 10     # Notificar cada 10 fragmentos
        mensaje: "§5§l⬢ §7Fragmentos recolectados: §d{count}§7/§d40"
        sonido:
          sound: "BLOCK_AMETHYST_BLOCK_CHIME"
          volumen: 1.0
          pitch: 1.2
  
  # ═══════════════════════════════════════════════════════════════
  # FASE 3: REVELACIÓN (15 minutos)
  # ═══════════════════════════════════════════════════════════════
  fase_revelacion:
    duracion_seg: 900
    
    trigger:
      fragmentos_requeridos: 40
    
    secuencia_mensajes:
      - delay_ticks: 0
        mensaje: "§5§l⚡ EL OBSERVADOR §r§7detecta algo..."
        broadcast: true
      
      - delay_ticks: 100
        mensaje: "§8§o\"Esto no es corrupción.\""
        broadcast: true
      
      - delay_ticks: 200
        mensaje: "§8§o\"Es un rastro.\""
        broadcast: true
      
      - delay_ticks: 300
        mensaje: "§7§o\"El Nether se abrió… pero no fue el final de nada.\""
        broadcast: true
      
      - delay_ticks: 400
        mensaje: "§7§o\"Solo fue una puerta más.\""
        broadcast: true
    
    cierre:
      delay_tras_revelacion_ticks: 600  # 30 segundos
      
      mensajes_finales:
        - "§d§l━━━━━━━━━━━━━━━━━━━━━━━"
        - ""
        - "§5§l⚡ EL OBSERVADOR §r§7susurra..."
        - ""
        - "§8§o\"El camino final no se abre destruyendo…\""
        - "§8§o\"…sino aceptando lo que quedó atrás.\""
        - ""
        - "§7Algo ha sido §d§omarcado§7."
        - ""
        - "§d§l━━━━━━━━━━━━━━━━━━━━━━━"
      
      efectos_globales:
        particulas:
          tipo: "END_GATEWAY"
          cantidad: 1000
          radio: 10
        
        sonido:
          sound: "BLOCK_END_PORTAL_SPAWN"
          volumen: 1.0
          pitch: 0.7
      
      portal_incompleto:
        enabled: true
        offset_spawn:           # Relativo al spawn del mundo
          x: 0
          y: 64
          z: 0
        
        estructura:
          frame: "END_PORTAL_FRAME"  # Sin eyes
          tamaño: "5x5"
          particulas:
            tipo: "PORTAL"
            cantidad: 20
            intervalo_ticks: 20
        
        hologram:
          texto: "§d§oSellado"
          offset_y: 2.0
        
        mensaje_coordenadas: "§7Coordenadas: §d{x} {y} {z}"
  
  # ═══════════════════════════════════════════════════════════════
  # ITEMS DEL EVENTO
  # ═══════════════════════════════════════════════════════════════
  items:
    fragmento_eco:
      material: "ECHO_SHARD"
      nombre: "§5Fragmento del Eco"
      lore:
        - "§8Ecos que no deberían existir"
        - ""
        - "§7Un rastro de algo lejano."
        - "§7No reacciona como los fragmentos del Nether."
        - "§7Solo… resuena."
        - ""
        - "§8§o\"Esto no es corrupción. Es un rastro.\""
      glow: true
  
  # ═══════════════════════════════════════════════════════════════
  # RECOMPENSAS (OPCIONAL - por participación)
  # ═══════════════════════════════════════════════════════════════
  recompensas:
    por_completar:
      ps: 200                   # 200 PS por completar evento
      mensaje: "§a✓ Has recibido §e200 PS §apor participar"
    
    por_fragmento_entregado:
      ps: 5                     # 5 PS por cada fragmento entregado
```

---

## ✅ TASKLIST DE IMPLEMENTACIÓN

### 📦 FASE 1: ESTRUCTURA BASE (1-2 horas)

| # | Tarea | Archivo | Estado |
|---|-------|---------|--------|
| 1.1 | Crear `CaminoEndEvent.java` | `events/CaminoEndEvent.java` | ⬜ |
| 1.2 | Crear `CaminoEndListener.java` | `events/CaminoEndListener.java` | ⬜ |
| 1.3 | Crear `CaminoEndItems.java` | `events/CaminoEndItems.java` | ⬜ |
| 1.4 | Crear `camino_end.yml` | `resources/camino_end.yml` | ⬜ |
| 1.5 | Registrar evento en `Apocalipsis.java` | `Apocalipsis.java` | ⬜ |
| 1.6 | Añadir comandos en `ApocalipsisCommand.java` | `commands/ApocalipsisCommand.java` | ⬜ |
| 1.7 | Añadir tab completer en `AvoTabCompleter.java` | `commands/AvoTabCompleter.java` | ⬜ |

### 🎨 FASE 2: ITEMS Y ANOMALÍAS (2-3 horas)

| # | Tarea | Descripción | Estado |
|---|-------|-------------|--------|
| 2.1 | Implementar `crearFragmentoEco()` | Item con lore y glow | ⬜ |
| 2.2 | Crear clase `Anomalia` | Gestión de estructuras | ⬜ |
| 2.3 | Implementar spawn aleatorio de anomalías | Sistema de spawn mundial | ⬜ |
| 2.4 | Sistema de partículas para anomalías | Efectos END_ROD + PORTAL | ⬜ |
| 2.5 | Sistema de sonidos posicionales | Beacon ambient + End sounds | ⬜ |
| 2.6 | Implementar 3 tipos de estructuras | Suspendido, Altar, Ruina | ⬜ |

### 🔄 FASE 3: FASES DEL EVENTO (3-4 horas)

| # | Tarea | Descripción | Estado |
|---|-------|-------------|--------|
| 3.1 | Implementar Fase 1: Anomalías | Spawn + mensajes + partículas | ⬜ |
| 3.2 | Implementar Fase 2: Ecos | Estructuras + fragmentos | ⬜ |
| 3.3 | Sistema de tracking global | Contador de fragmentos | ⬜ |
| 3.4 | Implementar Fase 3: Revelación | Secuencia de mensajes | ⬜ |
| 3.5 | Sistema de transición entre fases | Auto-progresión | ⬜ |

### 🏛️ FASE 4: PORTAL INCOMPLETO (1-2 horas)

| # | Tarea | Descripción | Estado |
|---|-------|-------------|--------|
| 4.1 | Generador de estructura de portal | 5x5 End Portal Frame sin eyes | ⬜ |
| 4.2 | Sistema de partículas permanentes | Partículas PORTAL constantes | ⬜ |
| 4.3 | Hologram "Sellado" | Texto flotante | ⬜ |
| 4.4 | Guardar coordenadas en config | Para futuro evento del End | ⬜ |
| 4.5 | Broadcast de coordenadas al final | Mensaje global | ⬜ |

### 💬 FASE 5: MENSAJES Y NARRATIVA (2 horas)

| # | Tarea | Descripción | Estado |
|---|-------|-------------|--------|
| 5.1 | Mensajes aleatorios del Observador | 5 mensajes diferentes | ⬜ |
| 5.2 | Secuencia de revelación | 5 mensajes secuenciales | ⬜ |
| 5.3 | Mensajes de cierre (cliffhanger) | Diálogo final completo | ⬜ |
| 5.4 | Notificaciones de progreso | "Fragmentos: X/40" | ⬜ |
| 5.5 | Efectos visuales/sonoros | Sincronizados con mensajes | ⬜ |

### 🎮 FASE 6: LISTENER E INTERACCIONES (2 horas)

| # | Tarea | Descripción | Estado |
|---|-------|-------------|--------|
| 6.1 | `onPlayerInteract` para estructuras | Click derecho en anomalías | ⬜ |
| 6.2 | Sistema de drop de fragmentos | Dar items al jugador | ⬜ |
| 6.3 | Efectos al recolectar | Partículas + sonidos | ⬜ |
| 6.4 | Despawn de estructuras usadas | Limpiar bloques | ⬜ |
| 6.5 | Proximidad a anomalías | Efectos al acercarse | ⬜ |

### ⚙️ FASE 7: COMANDOS Y TESTING (1-2 horas)

| # | Tarea | Descripción | Estado |
|---|-------|-------------|--------|
| 7.1 | Comando `/avo evento4 start` | Iniciar evento | ⬜ |
| 7.2 | Comando `/avo evento4 stop` | Detener evento | ⬜ |
| 7.3 | Comando `/avo evento4 info` | Estado del evento | ⬜ |
| 7.4 | Comando `/avo evento4 fase` | Cambiar fase manualmente | ⬜ |
| 7.5 | Comando `/avo evento4 fragmentos` | Ajustar contador | ⬜ |
| 7.6 | Tab completions | Autocompletado de comandos | ⬜ |
| 7.7 | Testing completo | Probar todas las fases | ⬜ |

### 🎁 FASE 8: RECOMPENSAS (OPCIONAL) (1 hora)

| # | Tarea | Descripción | Estado |
|---|-------|-------------|--------|
| 8.1 | Sistema de tracking de participación | Guardar UUIDs de participantes | ⬜ |
| 8.2 | Recompensa al completar | 200 PS al finalizar | ⬜ |
| 8.3 | Recompensa por fragmento | 5 PS por fragmento entregado | ⬜ |

---

## 📊 RESUMEN DE TIEMPOS

| Fase | Duración Estimada |
|------|-------------------|
| Fase 1: Estructura Base | 1-2 horas |
| Fase 2: Items y Anomalías | 2-3 horas |
| Fase 3: Fases del Evento | 3-4 horas |
| Fase 4: Portal Incompleto | 1-2 horas |
| Fase 5: Mensajes y Narrativa | 2 horas |
| Fase 6: Listener e Interacciones | 2 horas |
| Fase 7: Comandos y Testing | 1-2 horas |
| Fase 8: Recompensas (Opcional) | 1 hora |
| **TOTAL** | **13-18 horas** |

---

## 🎯 DIFERENCIAS CLAVE CON EVENTOS ANTERIORES

### Vs. Eco de Brasas
| Aspecto | Eco de Brasas | El Camino al End |
|---------|---------------|--------------------|
| Combate | Alto (Guardián boss) | Nulo (solo exploración) |
| Fases | 3 fases complejas | 3 fases simples |
| Ritual | Grietas + Anclas + Altar | Solo recolección |
| Duración por fase | 25-45 min cada una | 30-60 min cada una |
| Items | Consumibles para ritual | Coleccionables (no se usan) |

### Vs. Eco de las Sombras
| Aspecto | Eco de Sombras | El Camino al End |
|---------|----------------|--------------------|
| Enemigos | Sombras + Guardián | Ninguno |
| Actos | 7 actos narrativos | 3 fases simples |
| QTE/Mecánicas | QTE system, telegraphed attacks | Solo exploración |
| Progresión | Actos secuenciales | Fases paralelas |
| Boss fight | Sí (Guardián del Umbral) | No |

### Filosofía Única
✅ **Este evento es:**
- **Transición narrativa** (no clímax)
- **Exploración pasiva** (no combate)
- **Pista sutil** (no explicación completa)
- **Cliffhanger** (deja preguntas abiertas)

---

## 🔮 CONEXIÓN CON FUTURO EVENTO DEL END

### Elementos que Preparan el Terreno

1. **Portal Incompleto**
   - Se guarda ubicación en `camino_end.yml`
   - Futuro evento puede leerlo y "activarlo"
   - Frame sin eyes → necesitan ser colocados

2. **Fragmentos del Eco**
   - NO se consumen en este evento
   - Podrían ser requisito para abrir el End
   - Los jugadores los guardan sin saber su uso

3. **Mensajes del Observador**
   - "El camino final no se abre destruyendo…"
   - "…sino aceptando lo que quedó atrás."
   - **Pista**: No se fuerza, se acepta/entiende

4. **Lore Establecido**
   - El End existe
   - Está bloqueado/sellado intencionalmente
   - "Algo lo está bloqueando / esperando"

---

## 📝 NOTAS FINALES

### Consideraciones Técnicas

1. **Performance**
   - Limitar anomalías simultáneas (max 5)
   - Cancelar tasks al cambiar de fase
   - Limpiar entidades al finalizar

2. **Persistencia**
   - Guardar fragmentos globales en `state.yml`
   - Guardar ubicación de portal en config
   - NO persistir estructuras (re-spawn al reiniciar)

3. **Compatibilidad**
   - Funciona con `EventBase` existente
   - Usa `MessageBus` y `SoundUtil` ya existentes
   - Integración con `EventController`

### Testing Checklist

- [ ] Spawn aleatorio de anomalías funciona
- [ ] Estructuras generan correctamente
- [ ] Fragmentos se dropean al interactuar
- [ ] Contador global se actualiza
- [ ] Transición entre fases es fluida
- [ ] Mensajes del Observador aparecen
- [ ] Revelación se trigger a los 40 fragmentos
- [ ] Portal incompleto se genera
- [ ] Coordenadas se muestran correctamente
- [ ] Evento finaliza sin errores
- [ ] Comandos admin funcionan
- [ ] Tab completer funciona

---

## 🎨 FILOSOFÍA DE DISEÑO

> **"Menos es más"**

Este mini-evento es intencionalmente **sutil** y **misterioso**:

- NO sobrecarga con información
- NO tiene combate épico
- NO resuelve la narrativa

Su propósito es **plantar una semilla**:
- ¿Qué son estos fragmentos?
- ¿Por qué el portal está sellado?
- ¿Qué es "aceptar lo que quedó atrás"?

El jugador termina con **más preguntas que respuestas**.
Y eso es **exactamente lo que buscamos**.

---

**FIN DEL PLAN** ✨
