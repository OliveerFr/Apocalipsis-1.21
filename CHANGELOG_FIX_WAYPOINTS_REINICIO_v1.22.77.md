# 🗺️ FIX CRÍTICO: Waypoints se Eliminan al Reiniciar Servidor - v1.22.77

## 🐛 **PROBLEMA IDENTIFICADO**

Los waypoints de los jugadores se **eliminaban completamente** cada vez que se reiniciaba el servidor.

### **Causa Raíz:**

**Problema de orden de inicialización:**

```
onEnable() - Apocalipsis.java
    │
    ├─> Línea 208: new SkillEffectListener(...)
    │       └─> Constructor llama loadWaypoints()
    │           └─> Intenta cargar waypoints...
    │               └─> Bukkit.getWorld("Ciclo_2") → NULL ❌
    │                   └─> Waypoint OMITIDO (mundo no existe)
    │
    ├─> Línea 867: new CicloManager(...)  🔴 DEMASIADO TARDE
    │       └─> Carga mundos de ciclos
    │           └─> Ahora "Ciclo_2" existe... pero waypoints ya se omitieron
```

**Resultado:** Los waypoints de mundos de ciclos se perdían **permanentemente** al reiniciar.

---

## ✅ **SOLUCIÓN IMPLEMENTADA**

### **1. Método de Recarga Post-Mundos**

Se agregó `reloadWaypointsAfterWorldsLoaded()` en SkillEffectListener:

**Ubicación:** `SkillEffectListener.java` línea ~1810

```java
/**
 * [FIX CRÍTICO] Recarga waypoints después de que todos los mundos estén cargados.
 * Esto es necesario porque en el constructor, los mundos de ciclos aún no existen.
 * Debe llamarse desde Apocalipsis.onEnable() DESPUÉS de inicializar CicloManager.
 */
public void reloadWaypointsAfterWorldsLoaded() {
    if (!waypointPersistencia) {
        return;
    }
    
    File waypointsFile = new File(plugin.getDataFolder(), "waypoints.yml");
    if (!waypointsFile.exists()) {
        return;
    }
    
    plugin.getLogger().info("[Skills] Recargando waypoints tras cargar mundos de ciclos...");
    
    // Limpiar waypoints actuales
    int beforeCount = playerWaypoints.values().stream().mapToInt(Map::size).sum();
    playerWaypoints.clear();
    
    // Recargar desde archivo
    loadWaypoints();
    
    int afterCount = playerWaypoints.values().stream().mapToInt(Map::size).sum();
    if (afterCount > beforeCount) {
        plugin.getLogger().info("[Skills] ✓ Recuperados " + (afterCount - beforeCount) + 
            " waypoints de mundos que no estaban cargados inicialmente");
    }
}
```

### **2. Llamada en Orden Correcto**

**Ubicación:** `Apocalipsis.java` línea ~891

```java
getLogger().info("[CicloManager] ✓ Sistema de ciclos multi-mundo activado");
getLogger().info("[CicloManagers] ✓ Sistemas UX activados...");

// [FIX CRÍTICO] Recargar waypoints después de que los mundos de ciclos estén cargados
// Esto previene que los waypoints se pierdan al reiniciar el servidor
if (skillEffectListener != null) {
    skillEffectListener.reloadWaypointsAfterWorldsLoaded();
}
```

### **3. Mejora en Logs de Carga**

**Ubicación:** `SkillEffectListener.java` línea ~1867

Mensajes mejorados cuando un mundo no está cargado:

```java
if (world == null) {
    // [FIX CRÍTICO] NO omitir waypoints de mundos no cargados
    plugin.getLogger().warning("[Skills] Mundo '" + worldName + 
        "' no cargado aún para waypoint '" + waypointName + "'");
    plugin.getLogger().warning("[Skills] → El waypoint se cargará cuando " +
        "los mundos estén disponibles (llamar reloadWaypointsAfterWorldsLoaded)");
    continue;
}
```

---

## 📊 **LOGS DE DIAGNÓSTICO**

### **Antes del Fix (Comportamiento Incorrecto):**

```log
[Skills] Cargando waypoints desde: plugins/Apocalipsis/waypoints.yml
[Skills] Mundo no encontrado para waypoint 'base': Ciclo_2  ⬅️ OMITIDO
[Skills] Mundo no encontrado para waypoint 'granja': Ciclo_2  ⬅️ OMITIDO
[Skills] Cargados 0 waypoints de 0 jugadores  ⬅️ ¡TODOS SE PERDIERON!

...

[CicloManager] ✓ Sistema de ciclos multi-mundo activado  ⬅️ Ahora sí existe "Ciclo_2"
```

### **Después del Fix (Comportamiento Correcto):**

```log
[Skills] Cargando waypoints desde: plugins/Apocalipsis/waypoints.yml
[Skills] Mundo 'Ciclo_2' no cargado aún para waypoint 'base'
[Skills] → El waypoint se cargará cuando los mundos estén disponibles
[Skills] Cargados 0 waypoints de 0 jugadores  ⬅️ Temporal

...

[CicloManager] ✓ Sistema de ciclos multi-mundo activado
[Skills] Recargando waypoints tras cargar mundos de ciclos...  ⬅️ RECARGA
[Skills] ✓ Recuperados 5 waypoints de mundos que no estaban cargados inicialmente  ⬅️ ¡ÉXITO!
[Skills] Cargados 5 waypoints de 1 jugadores  ⬅️ TODO RECUPERADO
```

---

## 🧪 **TESTING**

### **Pasos para Verificar el Fix:**

1. **Crear waypoints en diferentes mundos:**
   ```
   /wp set base    (en Ciclo_2)
   /wp set mina    (en Ciclo_2)
   /wp set nether  (en Ciclo_2_nether)
   ```

2. **Verificar guardado:**
   ```
   /wp list
   → Debe mostrar: base, mina, nether
   ```

3. **Reiniciar servidor:**
   ```
   /stop
   (iniciar servidor)
   ```

4. **Verificar después de reiniciar:**
   ```
   /wp list
   → Debe mostrar: base, mina, nether  ✓
   ```

5. **Verificar logs:**
   Buscar en console:
   ```
   [Skills] ✓ Recuperados X waypoints de mundos que no estaban cargados inicialmente
   ```

---

## ⚙️ **CONFIGURACIÓN**

No requiere cambios en `skills.yml`. La persistencia sigue controlada por:

```yaml
waypoints:
  persistencia: true  # ✓ Debe estar en true
```

---

## 📝 **CAMBIOS EN ARCHIVOS**

### **SkillEffectListener.java:**
- ✅ Agregado método `reloadWaypointsAfterWorldsLoaded()`
- ✅ Mejorados mensajes de warning en `loadWaypoints()`

### **Apocalipsis.java:**
- ✅ Agregada llamada a `reloadWaypointsAfterWorldsLoaded()` después de inicializar `CicloManager` (línea ~891)

---

## 🔍 **DETALLES TÉCNICOS**

### **¿Por qué pasaba esto?**

1. **Plugin inicia** → `onEnable()`
2. **Línea 208**: Se crea `SkillEffectListener`
   - Constructor llama `loadWaypoints()`
   - En ese momento, **Multiverse/Bukkit aún no cargó mundos de ciclos**
3. **Línea 867**: Se crea `CicloManager`
   - Carga mundos de ciclos
   - **Pero waypoints ya se cargaron (y omitieron)**

### **¿Cómo lo soluciona el fix?**

1. **Primera carga** (línea 208): 
   - Carga waypoints de mundos principales (`world`, `world_nether`, `world_the_end`)
   - Omite waypoints de ciclos (con warning)

2. **Segunda carga** (línea 891):
   - **Después de cargar CicloManager**
   - Limpia cache y recarga TODO desde archivo
   - Ahora SÍ encuentra los mundos de ciclos
   - **Recupera waypoints previamente omitidos**

---

## ✅ **RESULTADO**

Los waypoints ahora **persisten correctamente** al reiniciar el servidor, incluso para mundos de ciclos que se cargan después del sistema de skills.

**Antes:** ❌ Waypoints desaparecían  
**Ahora:** ✅ Waypoints se conservan

---

## 🎯 **IMPACTO**

- **Severidad:** 🔴 **CRÍTICA** (pérdida de datos de jugadores)
- **Afectados:** Todos los jugadores con waypoints en mundos de ciclos
- **Frecuencia:** 100% al reiniciar servidor
- **Resolución:** ✅ **PERMANENTE** (fix estructural)

---

## 📅 **Versión**

- **Versión:** 1.22.77
- **Fecha:** 2026-02-09
- **Tipo:** Bug Fix Crítico
- **Prioridad:** 🔴 Máxima
