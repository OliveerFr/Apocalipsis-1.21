# Evento 6 - Integración con Sistema de Ciclos

## 🔄 Cómo Funciona la Integración

El Evento 6 "Cuando el Mundo Decide Olvidar" utiliza el **Sistema de Ciclos** existente para manejar el reinicio del mundo de forma segura y automática.

---

## 📋 Flujo del Evento

### 1. Inicio del Evento (`/avo evento6 start`)

```
✓ Verifica que el sistema de ciclos esté activado
✓ Registra a todos los jugadores online como participantes
✓ Inicia la progresión automática de 10 actos
```

### 2. Actos 1-4: Construcción de Tensión (0-14.5 min)

Durante estos actos, los jugadores permanecen en el mundo original (`world`):
- **Acto 1**: Normalidad aparente
- **Acto 2**: Primeras rarezas (efectos sutiles)
- **Acto 3**: Inestabilidad (lag simulado, partículas)
- **Acto 4**: El Quiebre (secuencia dramática)

### 3. **ACTO 5: EL REINICIO** (14.5-15 min) ⚡

Este es el momento crucial donde ocurre el reinicio:

#### Paso 1: Pantalla Negra (0-2 seg)
```java
aplicarEfectoTodos(PotionEffectType.BLINDNESS, 40);
```

#### Paso 2: Crear Nuevo Ciclo (2 seg)
```java
// Verifica si el mundo ya existe
World mundoExistente = Bukkit.getWorld("world_ciclo_reset");

if (mundoExistente != null) {
    // Usa mundo existente, solo activa el ciclo
    cicloManager.activateCycle("world_ciclo_reset", false);
} else {
    // Crea nuevo mundo + activa ciclo
    cicloManager.createAndActivateCycle(
        "world_ciclo_reset",      // Nombre del mundo
        World.Environment.NORMAL, // Ambiente overworld
        Difficulty.HARD,          // Dificultad
        false                     // NO teleportar automáticamente
    );
}
```

**¿Qué hace `createAndActivateCycle()`?**
1. Crea el mundo usando WorldCreator de Bukkit (nativo, sin Multiverse)
2. Configura propiedades: Dificultad HARD, PvP activado, mobs/animales
3. Registra el mundo en `ciclos.yml` como ciclo activo
4. Prepara el sistema para separar inventarios/datos por mundo

#### Paso 3: Teleportar Jugadores (2.5 seg)
```java
for (UUID uuid : participantes) {
    Player player = Bukkit.getPlayer(uuid);
    
    // 1. Guardar datos del mundo actual ANTES de teleportar
    String mundoActual = player.getWorld().getName();
    cicloManager.handlePlayerLeaveWorld(player, mundoActual);
    
    // 2. Teleportar al spawn del nuevo ciclo
    player.teleport(nuevoMundo.getSpawnLocation());
    
    // 3. El WorldChangeListener carga automáticamente datos del nuevo ciclo
}
```

**¿Qué hace `handlePlayerLeaveWorld()`?**
1. **Sanitiza el inventario** (remueve shulkers, bundles, etc.)
2. **Guarda el inventario completo** del mundo actual
3. **Captura y guarda datos de progreso**:
   - XP y nivel del plugin
   - Skills compradas
   - Misiones activas
   - PS (Puntos de Supervivencia)
   - Rango actual
4. **Escribe a disco** (`world_inventories.yml` y `world_data.yml`)

#### Paso 4: Efectos de Spawn (3 seg)
```java
// Partículas END_ROD + sonido RESPAWN_ANCHOR_CHARGE
// Dar items iniciales según evento6_mundo_olvidado.yml
darItemsIniciales();
```

### 4. Actos 6-10: Exploración Post-Reinicio (15-40 min)

Los jugadores ahora están en el **nuevo ciclo** (`world_ciclo_reset`):
- **Acto 6**: Despertar - "No los borró... solo borró el lugar"
- **Acto 7**: Comprensión - "Reiniciar es más fácil que cambiar"
- **Acto 8**: La Fractura - El Nether NO se reseteó
- **Acto 9**: El End Permanece - El End NO se reseteó
- **Acto 10**: Cierre - "Este no es un comienzo. Es una repetición."

---

## 🔐 Sistema de Separación de Datos

Cada mundo mantiene sus propios datos de jugador:

### Archivos de Almacenamiento

#### `world_inventories.yml`
```yaml
<UUID>:
  world:  # Mundo original
    inventory: [...]
    armor: [...]
    offhand: {...}
    level: 30
    exp: 0.75
  world_ciclo_reset:  # Nuevo ciclo
    inventory: []  # Vacío al inicio
    armor: []
    offhand: {}
```

#### `world_data.yml`
```yaml
<UUID>:
  world:  # Mundo original
    xp: 15000
    nivel: 22
    ps: 500
    rango: "VETERANO"
  world_ciclo_reset:  # Nuevo ciclo
    xp: 0      # Empieza desde 0
    nivel: 1   # Nivel 1
    ps: 0      # Sin PS
    rango: "NOVATO"  # Rango base
```

### ¿Qué se Conserva?
✅ **Memoria del jugador**: El sistema guarda ambos mundos por separado
✅ **Progreso original**: Puedes volver al mundo original con todo intacto
✅ **Misiones**: Se mantienen las misiones del sistema

### ¿Qué se Resetea?
❌ **Inventario**: Vacío en el nuevo ciclo
❌ **XP y Nivel**: Empieza en 0/nivel 1
❌ **Construcciones**: Mundo nuevo generado
❌ **Rango**: Vuelve a NOVATO en el ciclo

### ¿Qué NO se Resetea? (Elemento Narrativo)
🔥 **Nether**: Mantiene el Nether original (se accede desde el ciclo)
🔥 **The End**: Mantiene el End original
📝 **Razón narrativa**: "Algunos lugares no se reinician. Solo observan."

---

## 🛡️ Protecciones del Sistema

El sistema de ciclos tiene múltiples capas de protección:

### 1. Items Bloqueados
```yaml
protecciones:
  materiales_bloqueados:
    - SHULKER_BOX
    - WHITE_SHULKER_BOX
    - BUNDLE
    - CHEST_MINECART
    - HOPPER_MINECART
```

### 2. Sanitización Automática
Antes de guardar el inventario:
```java
ItemSanitizer.SanitizeResult result = 
    itemSanitizer.sanitizeInventory(player.getInventory().getContents());

if (result.hadProblematicItems()) {
    player.sendMessage("§c✗ Items problemáticos removidos: " + result.getTotalRemoved());
}
```

### 3. Listener de Cambio de Mundo
```java
@EventHandler
public void onWorldChange(PlayerChangedWorldEvent event) {
    Player player = event.getPlayer();
    String fromWorld = event.getFrom().getName();
    String toWorld = player.getWorld().getName();
    
    cicloManager.handleWorldChange(player, fromWorld, toWorld);
}
```

---

## 📊 Configuración del Evento

### `evento6_mundo_olvidado.yml`

```yaml
evento:
  mundo_actual: "world"
  mundo_nuevo_nombre: "world_ciclo_reset"
  
  mundo_nuevo:
    generador: "NORMAL"
    dificultad: "HARD"
    pvp_enabled: true
    spawn_monsters: true
    spawn_animals: true
    mantener_nether_original: true  # ⚠️ Nether NO se resetea
    mantener_end_original: true      # ⚠️ End NO se resetea
  
  reseteo:
    resetear_inventario: true
    resetear_xp: true
    resetear_construcciones: true
    resetear_misiones: false  # ✓ Se conservan
    resetear_rangos: false    # ✓ Se conservan (pero empiezan como NOVATO en ciclo)
    resetear_skills: false    # ✓ Se conservan
    
    items_iniciales:
      - material: "OAK_LOG"
        cantidad: 16
      - material: "BREAD"
        cantidad: 8
```

---

## 🔧 Métodos Clave del CicloManager

### Crear y Activar Ciclo
```java
public boolean createAndActivateCycle(
    String worldName,           // "world_ciclo_reset"
    World.Environment environment,  // NORMAL, NETHER, THE_END
    Difficulty difficulty,      // EASY, NORMAL, HARD, PEACEFUL
    boolean teleportAll         // false (lo maneja el evento)
)
```

### Activar Ciclo Existente
```java
public boolean activateCycle(
    String worldName,    // Nombre del mundo
    boolean teleportAll  // false
)
```

### Crear Mundo (Nativo Bukkit)
```java
public boolean createCycleWorld(
    String worldName,
    World.Environment environment,
    WorldType worldType,        // NORMAL, FLAT, LARGE_BIOMES, AMPLIFIED
    Difficulty difficulty,
    boolean generateStructures  // true
)
```

### Manejo de Cambio de Mundo
```java
public void handlePlayerLeaveWorld(Player player, String worldName)
public void handlePlayerEnterWorld(Player player, String worldName)
public void handleWorldChange(Player player, String fromWorld, String toWorld)
```

---

## ⚙️ Proceso Técnico Completo

```
┌─────────────────────────────────────────────────────────────────┐
│                    EVENTO 6: FLUJO TÉCNICO                      │
└─────────────────────────────────────────────────────────────────┘

1. Jugadores en "world" (0-14.5 min)
   │
   ├─> Acto 1-4: Narrativa de tensión
   │
2. ACTO 5: REINICIO (14.5 min)
   │
   ├─> Pantalla negra (BLINDNESS 2 seg)
   │
   ├─> createAndActivateCycle("world_ciclo_reset")
   │   ├─> WorldCreator.create() → Genera nuevo mundo
   │   ├─> Configurar: HARD, PvP ON, Mobs ON
   │   ├─> Registrar en ciclos.yml
   │   └─> Preparar separación de datos
   │
   ├─> Para cada jugador participante:
   │   ├─> handlePlayerLeaveWorld("world")
   │   │   ├─> Sanitizar inventario
   │   │   ├─> Guardar inventario en world_inventories.yml
   │   │   ├─> Capturar datos (XP, skills, PS, rango)
   │   │   └─> Guardar en world_data.yml
   │   │
   │   ├─> player.teleport(world_ciclo_reset.spawn)
   │   │
   │   └─> WorldChangeListener detecta cambio
   │       └─> handlePlayerEnterWorld("world_ciclo_reset")
   │           ├─> Cargar inventario de world_ciclo_reset (vacío)
   │           ├─> Cargar datos de world_ciclo_reset (XP=0, nivel=1, etc.)
   │           └─> Aplicar al jugador
   │
   ├─> Partículas END_ROD + Sonido
   │
   └─> darItemsIniciales() → OAK_LOG x16, BREAD x8
       
3. Jugadores en "world_ciclo_reset" (15-40 min)
   │
   └─> Actos 6-10: Exploración y revelación

┌─────────────────────────────────────────────────────────────────┐
│  RESULTADO FINAL:                                               │
│  • Mundo original "world" intacto (nadie dentro)                │
│  • Nuevo mundo "world_ciclo_reset" activo (todos dentro)        │
│  • Datos de ambos mundos guardados por separado                 │
│  • Nether/End compartidos (elemento narrativo)                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🎮 Testing y Verificación

### Comandos de Prueba

```bash
# 1. Iniciar evento
/avo evento6 start

# 2. Verificar estado (durante el evento)
/avo evento6 info

# 3. Después del reinicio (a los 15 min), verificar:
/avo ciclo listar
→ Debe mostrar "world_ciclo_reset" como activo

# 4. Verificar datos guardados
/avo ciclo info world
→ Muestra jugadores=0 (nadie en mundo original)

/avo ciclo info world_ciclo_reset
→ Muestra jugadores activos

# 5. Para volver al mundo original (solo admins):
/avo ciclo teleport world
→ Tu inventario/XP del mundo original se restaura
```

### Archivos a Verificar

1. **`ciclos.yml`** - Debe tener entrada para `world_ciclo_reset`
2. **`world_inventories.yml`** - Debe tener secciones separadas por mundo
3. **`world_data.yml`** - Debe tener progreso separado por mundo
4. **Carpeta del servidor** - Debe existir `world_ciclo_reset/` con region files

---

## ⚠️ Notas Importantes

### Multiverse NO es Requerido
El sistema usa **WorldCreator nativo de Bukkit**, no requiere Multiverse-Core.

### Nether/End Compartidos
Por diseño narrativo, el Nether y el End NO se resetean:
- Portales del nuevo ciclo conectan al Nether/End original
- Esto es intencional: "Algunos lugares no se reinician"

### Datos Persistentes
Los datos se guardan automáticamente:
- Al cambiar de mundo
- Al desconectarse
- Cada 5 minutos (auto-save del plugin)

### Reversibilidad
Los jugadores (con permisos) pueden volver al mundo original:
```
/avo ciclo teleport world
```
Su inventario y progreso del mundo original se restaura completamente.

---

## 🐛 Troubleshooting

### "Sistema de ciclos desactivado"
```yaml
# Verifica en ciclos.yml:
enabled: true
```

### "El mundo ya existe"
El evento reutiliza el mundo si ya existe. Para mundo limpio:
```bash
# Detener servidor
# Eliminar carpeta world_ciclo_reset/
# Iniciar servidor
# /avo evento6 start
```

### "No se pudo crear el ciclo"
Verifica logs:
```
[CicloManager] ✗ Error: El mundo no se pudo crear
```
Posibles causas:
- Falta espacio en disco
- Permisos de escritura
- Nombre de mundo inválido

### Jugadores no aparecen en nuevo mundo
Verifica WorldChangeListener esté registrado:
```java
plugin.getServer().getPluginManager().registerEvents(
    new WorldChangeListener(cicloManager), plugin
);
```

---

## 📝 Resumen

El Evento 6 es un **showcase del Sistema de Ciclos**:
- ✅ Demuestra creación de mundos en vivo
- ✅ Muestra separación de inventarios/datos
- ✅ Narrativa que explica el concepto de "ciclos"
- ✅ Prueba la robustez del sistema bajo presión

**Filosofía**: "El mundo se resetea. Los jugadores recuerdan."
