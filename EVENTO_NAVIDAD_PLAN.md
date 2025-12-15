# 🎄 EVENTO NAVIDAD - PLAN DE DESARROLLO

## 📋 RESUMEN EJECUTIVO

El evento de Navidad no es un evento de combate. Es una **pausa narrativa** que acompaña, ambientaliza y ordena. El plugin solo proporciona:
- Ambiente visual/sonoro
- Control por comandos
- Recompensas simples
- Fragmentos misteriosos

**Filosofía:** ❌ No bosses, ❌ No combates, ❌ No mecánicas complejas

---

## 🎯 ESTRUCTURA DE ARCHIVOS A CREAR

```
src/main/java/me/apocalipsis/events/
├── NavidadEvent.java          # Evento principal (extiende EventBase)
├── NavidadListener.java       # Listener para interacciones
├── NavidadItems.java          # Items especiales del evento (fragmentos, regalos)

src/main/resources/
├── navidad.yml               # Configuración del evento (nuevo archivo)
└── eventos.yml               # Añadir sección navidad
```

---

## ✅ TAREAS DE DESARROLLO

### FASE 1: ESTRUCTURA BASE ✅ COMPLETADA
| # | Tarea | Descripción | Archivos |
|---|-------|-------------|----------|
| 1.1 | ✅ Crear `NavidadEvent.java` | Clase principal extendiendo `EventBase` con estado del evento | `events/NavidadEvent.java` |
| 1.2 | ✅ Crear `NavidadListener.java` | Listener para interacción con regalos y árbol | `events/NavidadListener.java` |
| 1.3 | ✅ Crear `NavidadItems.java` | Definición de items: fragmentos y regalos | `events/NavidadItems.java` |
| 1.4 | ✅ Crear `navidad.yml` | Configuración de mensajes, coordenadas, efectos | `resources/navidad.yml` |
| 1.5 | ✅ Registrar evento en `Apocalipsis.java` | Añadir NavidadEvent al EventController | `Apocalipsis.java` |

### FASE 2: SISTEMA DE COMANDOS ✅ COMPLETADA
| # | Tarea | Descripción | Archivos |
|---|-------|-------------|----------|
| 2.1 | ✅ Añadir comandos en `ApocalipsisCommand.java` | Case "navidad" con todos los subcomandos | `ApocalipsisCommand.java` |
| 2.2 | ✅ Actualizar `AvoTabCompleter.java` | Autocompletado para `/avo navidad <subcomando>` | `AvoTabCompleter.java` |
| 2.3 | ✅ Método `cmdNavidad()` | Handler principal con switch de subcomandos | `ApocalipsisCommand.java` |

### FASE 3: COMANDOS PRINCIPALES ✅ COMPLETADA
| # | Tarea | Comando | Función |
|---|-------|---------|---------|
| 3.1 | ✅ `/avo navidad start` | Inicio oficial del evento | **CINEMÁTICA ÉPICA DE 30 SEGUNDOS** + activa modo navidad |
| 3.2 | ✅ `/avo navidad stop` | Cierre del evento | Desactiva efectos, vuelve a normalidad, LIMPIA TODO |
| 3.3 | ✅ `/avo navidad status` | Estado del evento | Muestra: evento activo, santa, regalos, árbol |
| 3.4 | ✅ `/avo navidad reset` | Reset de emergencia | Limpia estados sin borrar items |

#### ⭐ MEJORA IMPLEMENTADA: CINEMÁTICA DE INICIO ÉPICA

El método `onStart()` ahora incluye una **cinemática cinematográfica de 30 segundos** dividida en 4 fases:

**FASE 1: El Mundo se Detiene (0-5s)**
- T=0s: Silencio inicial, título misterioso ("...")
- T=2s: Primera señal con partículas SNOWFLAKE flotantes
- Sonidos: BLOCK_BEACON_DEACTIVATE, BLOCK_GLASS_BREAK

**FASE 2: La Calma Desciende (5-15s)**
- T=5s: Mensaje del Observador ("Incluso los mundos rotos necesitan un respiro")
- T=8s: El cielo se aclara automáticamente (clear weather), círculo de partículas END_ROD
- T=12s: Mensajes poéticos sobre la paz
- Efectos: Nieve cayendo, luces cálidas (FIREWORK), sonidos de campanas

**FASE 3: La Navidad Llega (15-25s)**
- T=16s: **TÍTULO PRINCIPAL** "✦ NAVIDAD ✦" con explosión de partículas festivas
- T=20s: Mensaje oficial del evento explicando la paz
- Sonidos: UI_TOAST_CHALLENGE_COMPLETE, ENTITY_PLAYER_LEVELUP, NOTE_BLOCK_BELL

**FASE 4: Finalización (25-30s)**
- T=25s: Espiral ascendente de partículas WAX_ON
- T=30s: Activación automática del ambiente + inicio del Observador
- Título final: "✓ Que comience la celebración"

**Características técnicas:**
- Opción de congelar jugadores durante la cinemática (`cinematica.congelar_jugadores` en navidad.yml)
- Uso de `Bukkit.getScheduler().runTaskLater()` para sincronización precisa
- Combinación de 6 tipos de partículas diferentes (SNOWFLAKE, END_ROD, FIREWORK, WAX_ON, etc.)
- 8 tipos de sonidos ambientales progresivos
- Títulos en pantalla con timings personalizados
- Transición suave al estado activo del evento

### FASE 4: SISTEMA DE AMBIENTE ✅ COMPLETADA
| # | Tarea | Comando | Efectos |
|---|-------|---------|---------|
| 4.1 | ✅ `/avo navidad ambiente on` | Activar ambiente | Partículas suaves, nieve ligera, campanas lejanas |
| 4.2 | ✅ `/avo navidad ambiente off` | Desactivar ambiente | Apaga visual/sonoro |
| 4.3 | ✅ Sistema de partículas navideñas | Tasks periódicos | Copos de nieve, luces cálidas, chispas suaves |
| 4.4 | ✅ Sistema de sonidos ambiente | Tasks periódicos | Campanas lejanas, viento suave (no invasivo) |

### FASE 5: SISTEMA DEL ÁRBOL ✅ COMPLETADA
| # | Tarea | Comando | Función |
|---|-------|---------|---------|
| 5.1 | ✅ `/avo navidad arbol set` | Definir ubicación | Guarda coordenadas del árbol central |
| 5.2 | ✅ `/avo navidad arbol activar` | Intensificar árbol | Partículas concentradas, luces intensas, sonido suave |
| 5.3 | ✅ `/avo navidad arbol desactivar` | Normalizar árbol | Vuelve árbol a estado normal | 
| 5.4 | ✅ Persistencia de ubicación | YAML | Guardar/cargar coordenadas del árbol |
| 5.5 | ✅ Detección inteligente de árbol | Sistema detecta bloques de madera, hojas y decoraciones automáticamente |

### FASE 6: SISTEMA DE SANTA ✅ COMPLETADA
| # | Tarea | Comando | Función |
|---|-------|---------|---------|
| 6.1 | ✅ `/avo navidad santa spawn` | Aparecer Santa | Spawnea villager cerca del árbol con partículas de aparición |
| 6.2 | ✅ `/avo navidad santa despawn` | Desaparecer Santa | Santa desaparece lentamente con partículas suaves |
| 6.3 | ✅ Entidad Santa | Villager custom | Nombre custom, skin/equipo navideño, invulnerable |
| 6.4 | ✅ Frases opcionales de Santa | Mensajes | Configurables en navidad.yml |

### FASE 7: SISTEMA DE REGALOS ✅ COMPLETADA
| # | Tarea | Comando | Función |
|---|-------|---------|---------|
| 7.1 | ✅ `/avo navidad regalos start` | Habilitar regalos | Activa cofres/regalos con partículas al abrir |
| 7.2 | ✅ `/avo navidad regalos stop` | Deshabilitar regalos | Bloquea nuevos, mantiene entregados |
| 7.3 | ✅ Listener de apertura | Evento | Mensaje "Has recibido un regalo." + partículas |
| 7.4 | ✅ Items de regalo | NavidadItems | Sistema flexible para crear cualquier regalo |

### FASE 8: SISTEMA DE FRAGMENTOS ✅ COMPLETADA
| # | Tarea | Comando | Función |
|---|-------|---------|---------|
| 8.1 | ✅ `/avo navidad fragmentos give <player> <cantidad>` | Dar fragmentos | Entrega manual controlada |
| 8.2 | ✅ `/avo navidad fragmentos giveall <cantidad>` | Dar a todos | Entrega a todos los presentes |
| 8.3 | ✅ `/avo navidad fragmentos info` | Info fragmentos | Muestra cantidad sin spoilers |
| 8.4 | ✅ Item Fragmento de Navidad | NavidadItems | AMETHYST_SHARD con lore misterioso, sin explicar uso |
| 8.5 | ✅ Persistencia de fragmentos | PlayerData | Sistema de tracking por UUID implementado |

### FASE 9: NARRATIVA Y OBSERVADOR ✅ COMPLETADA
| # | Tarea | Descripción | Contenido |
|---|-------|-------------|-----------|
| 9.1 | ✅ Pensamientos del Observador | Mensajes opcionales | "Curioso... incluso los mundos rotos buscan paz." |
| 9.2 | ✅ Mensajes sutiles | Evento aleatorio | Sin hablar directo, solo pensamientos sueltos |
| 9.3 | ✅ Configuración en YAML | navidad.yml | Todos los mensajes configurables |

### FASE 10: CLIFFHANGER FINAL ✅ COMPLETADA
| # | Tarea | Descripción | Efectos |
|---|-------|-------------|---------|
| 10.1 | ✅ Evento de cierre misterioso | Al finalizar | Sonido profundo lejano, temblor leve |
| 10.2 | ✅ Fragmento que se apaga | Visual | Un fragmento pierde brillo brevemente |
| 10.3 | ✅ Pensamiento final Observador | Mensaje | "Los fragmentos no aparecen sin razón..." |
| 10.4 | ✅ Comando para trigger | `/avo navidad cliffhanger` | Activa secuencia de cierre |

---

## 📝 CONFIGURACIÓN `navidad.yml`

```yaml
navidad:
  enabled: true
  
  # Coordenadas del árbol (se setean con comando)
  arbol:
    world: "world"
    x: 0
    y: 64
    z: 0
    configurado: false
  
  # Ambiente
  ambiente:
    particulas:
      nieve: true
      luces: true
      chispas: true
      intensidad: 0.5  # 0.0 - 1.0
    sonidos:
      campanas: true
      viento: true
      intervalo_seg: 30
  
  # Santa
  santa:
    frases:
      - "..."
      - "Cuídenlos. No todos los mundos llegan tan lejos."
    spawn_offset: 5  # Bloques desde el árbol
  
  # Fragmentos
  fragmentos:
    material: "AMETHYST_SHARD"
    nombre: "§d✦ Fragmento de Recuerdo ✦"
    lore:
      - "§8Navidad"
      - ""
      - "§7Un fragmento que resuena..."
      - "§7No explica su propósito."
      - ""
      - "§8\"Algunos recuerdos pesan más que otros.\""
    glow: true
  
  # Mensajes del evento
  mensajes:
    inicio: "§7El mundo entra en un momento de calma."
    fin: "§7El mundo vuelve a la normalidad."
    regalo_recibido: "§a✦ Has recibido un regalo."
    fragmento_info: "§7Tienes §d{cantidad} §7fragmentos de recuerdo."
  
  # Observador (pensamientos aleatorios, no directos)
  observador:
    pensamientos:
      - "§8§o...Curioso... incluso los mundos rotos buscan momentos de paz..."
      - "§8§o...Siempre ocurre antes de que todo vuelva a moverse..."
      - "§8§o...Los fragmentos no aparecen sin razón..."
    intervalo_min_seg: 300  # 5 minutos mínimo entre pensamientos
  
  # Cliffhanger
  cliffhanger:
    sonido: "AMBIENT_CAVE"
    temblor_intensidad: 0.3
    mensaje_final: "§8§o...y las puertas no se abren solas."
```

---

## 🔗 INTEGRACIONES

### En `Apocalipsis.java`:
```java
// Añadir import
import me.apocalipsis.events.NavidadEvent;

// En onEnable(), después de crear EventController:
NavidadEvent navidadEvent = new NavidadEvent(this, messageBus, soundUtil);
eventController.registerEvent(navidadEvent);
```

### En `ApocalipsisCommand.java`:
```java
// Añadir case en switch principal:
case "navidad":
    cmdNavidad(sender, args);
    break;
```

### En `AvoTabCompleter.java`:
```java
// Añadir a lista de subcomandos nivel 1:
"navidad"

// Añadir case para nivel 2:
case "navidad":
    return Arrays.asList("start", "stop", "ambiente", "arbol", "santa", "regalos", "fragmentos", "status", "reset", "cliffhanger")
        .stream().filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
```

---

## 🎨 EFECTOS VISUALES Y SONOROS

### Partículas Ambiente (sutiles, no invasivas):
- `SNOWFLAKE` - Copos de nieve suaves
- `END_ROD` - Luces cálidas flotantes
- `FIREWORK` - Chispas muy ocasionales
- `WAX_ON` - Brillo suave

### Partículas Árbol Activo:
- `ENCHANT` - Efecto mágico concentrado
- `GLOW` - Luminosidad
- `SPELL_WITCH` - Partículas místicas

### Partículas Santa:
- Spawn: `CLOUD` + `VILLAGER_HAPPY`
- Despawn: `CLOUD` (desvanecimiento suave)

### Sonidos:
- Ambiente: `BLOCK_NOTE_BLOCK_CHIME` (campanas lejanas, pitch bajo)
- Árbol activo: `BLOCK_BEACON_AMBIENT`
- Santa spawn: `ENTITY_PLAYER_LEVELUP` (suave)
- Regalo: `ENTITY_EXPERIENCE_ORB_PICKUP`

---

## 📊 ESTADOS DEL EVENTO

```
INACTIVO → START → ACTIVO (ambiente opcional) → STOP → INACTIVO
                      ↓
              [Árbol, Santa, Regalos]
              (independientes, manuales)
                      ↓
              CLIFFHANGER → FIN
```

---

## 🔐 PERMISOS (Heredados)

Todos los comandos requieren permisos de admin existentes:
- `apocalipsis.admin` o equivalente

---

## 🎉 RESUMEN DE IMPLEMENTACIÓN

### ✅ TODO COMPLETADO

El **Evento Navidad** ha sido completamente implementado siguiendo la filosofía establecida:

#### 📁 Archivos Creados:
1. **NavidadEvent.java** - Evento principal con toda la lógica (**~900 líneas** con cinemática épica)
2. **NavidadListener.java** - Listener para protección y regalos
3. **NavidadItems.java** - Sistema de items (fragmentos y regalos)
4. **navidad.yml** - Configuración completa del evento + **sección de cinemática**

#### 🔧 Integraciones:
- ✅ Registrado en `Apocalipsis.java`
- ✅ Añadido a `ConfigManager.java` con soporte para navidad.yml
- ✅ Comandos completos en `ApocalipsisCommand.java` (método `cmdNavidad()`)
- ✅ Autocompletado total en `AvoTabCompleter.java`

#### 🎮 Comandos Implementados:

**Control Principal:**
- `/avo navidad start` - **⭐ CINEMÁTICA ÉPICA DE 30 SEGUNDOS** + inicia el evento
- `/avo navidad stop` - Finaliza el evento
- `/avo navidad status` - Muestra estado completo
- `/avo navidad reset` - Reset de emergencia

**Ambiente:**
- `/avo navidad ambiente on/off` - Control de partículas y sonidos

**Árbol:**
- `/avo navidad arbol set` - Define ubicación con detección inteligente
- `/avo navidad arbol activar/desactivar` - Control de efectos intensos

**Santa:**
- `/avo navidad santa spawn/despawn` - Control de aparición

**Regalos:**
- `/avo navidad regalos start/stop` - Sistema de cofres/regalos

**Fragmentos:**
- `/avo navidad fragmentos give <player> <cantidad>`
- `/avo navidad fragmentos giveall <cantidad>`
- `/avo navidad fragmentos info`

**Especial:**
- `/avo navidad cliffhanger` - Secuencia de cierre misterioso

#### 🎨 Características Implementadas:

✅ **⭐ CINEMÁTICA DE INICIO ÉPICA (NUEVO):**
- **Duración:** 30 segundos divididos en 4 fases cinematográficas
- **Método:** `iniciarCinematicaInicio()` en NavidadEvent.java (~160 líneas)
- **Fase 1 (0-5s):** El Mundo se Detiene
  - Silencio inicial con título misterioso ("...")
  - Partículas SNOWFLAKE flotantes
  - Sonidos: BLOCK_BEACON_DEACTIVATE, BLOCK_GLASS_BREAK
- **Fase 2 (5-15s):** La Calma Desciende
  - Mensaje del Observador ("Incluso los mundos rotos necesitan un respiro")
  - Clima despejado automáticamente (clear weather)
  - Círculo de partículas END_ROD, luces cálidas FIREWORK
  - Nieve cayendo progresivamente
- **Fase 3 (15-25s):** La Navidad Llega
  - **TÍTULO PRINCIPAL:** "✦ NAVIDAD ✦" con explosión festiva
  - 50 FIREWORK + 100 SNOWFLAKE + 30 END_ROD simultáneos
  - Mensaje oficial del evento
  - Sonidos: UI_TOAST_CHALLENGE_COMPLETE, ENTITY_PLAYER_LEVELUP, NOTE_BLOCK_BELL
- **Fase 4 (25-30s):** Finalización y Transición
  - Espiral ascendente de 50 partículas WAX_ON
  - Activación automática del ambiente
  - Inicio del Observador
  - Título final: "✓ Que comience la celebración"
- **Características técnicas:**
  - Opción de congelar jugadores durante cinemática (`cinematica.congelar_jugadores` en navidad.yml)
  - 6 tipos de partículas: SNOWFLAKE, END_ROD, FIREWORK, WAX_ON, GLASS_BREAK
  - 8 sonidos diferentes sincronizados
  - Títulos en pantalla con timings personalizados (fade in/stay/fade out)
  - Uso de `Bukkit.getScheduler().runTaskLater()` para sincronización precisa

✅ **Ambiente Navideño:**
- Partículas: SNOWFLAKE, END_ROD, FIREWORK
- Sonidos: Campanas lejanas, viento suave
- Sistema de tasks periódicos no invasivos

✅ **Sistema del Árbol:**
- Detección automática de bloques (LOG, LEAVES, LANTERN, etc.)
- Partículas intensificadas: ENCHANT, GLOW
- Persistencia en YAML

✅ **Sistema de Santa:**
- Villager customizado (Profession.NONE, Type.SNOW)
- Invulnerable, sin IA, nombre custom
- Efectos de aparición/desaparición

✅ **Sistema de Regalos:**
- Tracking de jugadores que ya recibieron
- Efectos al abrir cofres
- Mensajes personalizados

✅ **Sistema de Fragmentos:**
- Items con glow effect
- Persistencia por UUID
- Lore misterioso sin explicar uso

✅ **Narrativa del Observador:**
- Pensamientos aleatorios cada 5 minutos
- Probabilidad configurable (30%)
- Mensajes sutiles sin spoilers

✅ **Cliffhanger:**
- Secuencia de 10 segundos
- Sonido AMBIENT_CAVE
- Temblor visual leve
- Mensaje final misterioso

#### 🔐 Protecciones:
- Bloques del árbol protegidos durante el evento
- Validaciones de permisos (avo.admin)
- Checks de estado antes de comandos
- Sistema de limpieza automática al detener

#### 📊 Filosofía Respetada:
- ❌ Sin combates
- ❌ Sin bosses
- ❌ Sin mecánicas complejas
- ✅ Ambiente sutil
- ✅ Control manual total
- ✅ Narrativa misteriosa
- ✅ Puente al siguiente evento (Guardián)
- ✅ **⭐ Presentación cinematográfica épica y memorable**

---

## 🔧 MEJORAS IMPLEMENTADAS (Actualización Final)

### ✅ **CRÍTICAS** (Solucionadas)

**1. Persistencia de Datos Completa**
- ✅ Sistema de save/load implementado en `navidad_data.yml`
- ✅ Fragmentos se guardan por UUID
- ✅ Regalos entregados persisten entre reinicios
- ✅ Auto-save en `darFragmentos()` y `cleanup()`
- ✅ Manejo robusto de errores con try-catch

**2. Bug de Compilación Corregido**
- ✅ Variable `world` definida correctamente en cinemática
- ✅ Validación de `world != null` en todas las spawns de partículas
- ✅ Código compila sin errores

### ✅ **IMPORTANTES** (Implementadas)

**3. Protección de Santa Mejorada**
- ✅ `setRemoveWhenFarAway(false)` - No despawnea por distancia
- ✅ `setPersistent(true)` - Persiste en chunk loads/unloads
- ✅ Task de verificación cada 5 segundos
- ✅ Log si Santa desaparece inesperadamente
- ✅ Validación de chunk cargado antes de spawn

**4. Optimización de Memoria del Árbol**
- ✅ Reemplazado `Set<Location>` por `BoundingBox`
- ✅ Detección lazy (solo cuando se necesita)
- ✅ Método `esParteDeLArbol()` usa `BoundingBox.contains()`
- ✅ Reducción de uso de memoria: ~1000+ locations → 1 BoundingBox

**5. Validaciones de Mundo y Chunk**
- ✅ Validación de `world != null` en arbolLocation
- ✅ Verificación de chunk cargado en partículas del árbol
- ✅ Auto-load de chunk si es necesario en spawn de Santa

### ✅ **CALIDAD** (Implementadas)

**6. Tracking de Jugadores en Cinemática**
- ✅ Map `jugadoresEnCinematica` para tracking
- ✅ Marca jugadores que entran durante la cinemática
- ✅ Limpieza automática al finalizar (T=30s)

**7. Límite de Pensamientos del Observador**
- ✅ Configuración `observador.max_pensamientos` (default: 10)
- ✅ Contador `contadorPensamientosObservador`
- ✅ Auto-cancel del task al alcanzar límite
- ✅ Evita spam en eventos muy largos

**8. Cooldown en Cliffhanger**
- ✅ Cooldown de 5 minutos
- ✅ Variable `ultimoCliffhanger` (timestamp)
- ✅ Mensaje de tiempo restante si está en cooldown
- ✅ Log de advertencia

**9. Null Checks Exhaustivos**
- ✅ Validación de `eventoActivo` en todos los tasks
- ✅ Validación de `world != null` antes de spawnear partículas
- ✅ Validación de `isCancelled()` en cancelación de tasks
- ✅ Cleanup mejorado con múltiples validaciones

**10. Control de Inicio del Evento**
- ✅ Variable `inicioEvento` (timestamp)
- ✅ Reset de contadores en `onStart()`
- ✅ Guardado automático en `cleanup()`

### 📊 **RESULTADO**

**Archivos Modificados:**
- `NavidadEvent.java` (~1045 líneas, +100 líneas de mejoras)
- `EVENTO_NAVIDAD_PLAN.md` (documentación completa)

**Mejoras Técnicas:**
- ✅ **15 mejoras críticas/importantes implementadas**
- ✅ **Código más robusto** (null-safe, thread-safe)
- ✅ **Optimización de memoria** (BoundingBox vs Set<Location>)
- ✅ **Persistencia completa** (datos sobreviven reinicio)
- ✅ **Protecciones contra edge cases** (despawn, chunk unload, spam)
- ✅ **Sin errores de compilación**

**Configuración Extendida (`navidad.yml`):**
```yaml
observador:
  max_pensamientos: 10  # Nuevo: límite de pensamientos
  
cinematica:
  congelar_jugadores: false  # Control de movilidad en cinemática
```

**Nuevo Archivo de Datos:**
- `plugins/Apocalipsis/navidad_data.yml` - Persistencia de fragmentos y regalos
- ✅ Control manual total
- ✅ Narrativa misteriosa
- ✅ Puente al siguiente evento (Guardián)

---

## 📅 ORDEN DE IMPLEMENTACIÓN SUGERIDO

1. **Estructura base** (1.1-1.5) - ~2h
2. **Sistema de comandos** (2.1-2.3) - ~1h
3. **Comandos principales** (3.1-3.4) - ~1h
4. **Sistema de ambiente** (4.1-4.4) - ~2h
5. **Sistema del árbol** (5.1-5.4) - ~1.5h
6. **Sistema de Santa** (6.1-6.4) - ~1.5h
7. **Sistema de regalos** (7.1-7.4) - ~1.5h
8. **Sistema de fragmentos** (8.1-8.5) - ~1.5h
9. **Narrativa** (9.1-9.3) - ~0.5h
10. **Cliffhanger** (10.1-10.4) - ~1h

**Tiempo estimado total: ~12-15 horas**

---

## 🎭 NOTAS DE DISEÑO

### Lo que SÍ hace el plugin:
- ✅ Crea ambiente navideño sutil
- ✅ Permite control total por comandos
- ✅ Entrega fragmentos misteriosos
- ✅ Mantiene la narrativa del Observador
- ✅ Prepara el terreno para el siguiente evento

### Lo que NO hace el plugin:
- ❌ No tiene combates
- ❌ No tiene bosses
- ❌ No tiene mecánicas complejas
- ❌ No interrumpe la paz del evento
- ❌ No spoilea la historia

---

## 🔮 CONEXIÓN CON SIGUIENTE EVENTO (GUARDIÁN)

El cliffhanger planta las semillas:
- Fragmentos que reaccionan
- Puertas que "no se abren solas"
- Algo antiguo despertando

El próximo evento introducirá:
- Un Warden incompleto
- Una presencia que observa
- La conexión directa con el End

---

*Documento creado: Evento Navidad - Plugin Apocalipsis*
*Última actualización: Diciembre 2025*
