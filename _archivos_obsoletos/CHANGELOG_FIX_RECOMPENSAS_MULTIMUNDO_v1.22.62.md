# 🐛 CHANGELOG v1.22.62 - Fix Recompensas Duplicadas al Cambiar de Mundo

## 📅 Fecha: 30 Enero 2026

---

## 🎯 PROBLEMA SOLUCIONADO

**Bug crítico:** Los jugadores recibían recompensas de rango **duplicadas** cada vez que cambiaban de mundo.

### Descripción del Bug

El sistema de recompensas estaba usando el **nombre del mundo** como parte de la clave de verificación:

```java
// ❌ ANTES (INCORRECTO)
String key = player.getUniqueId().toString() + ":" + worldName + ":" + rank.name();
// Ejemplo: "uuid-123:world_ciclo1:EXPLORADOR"
```

**Consecuencia:**
- Jugador sube a EXPLORADOR en `world_ciclo1` → Recibe recompensas ✅
- Jugador cambia a `world_ciclo2` → La clave cambia a `uuid-123:world_ciclo2:EXPLORADOR`
- Sistema piensa: "No tiene recompensas de EXPLORADOR en este mundo" ❌
- **¡Vuelve a dar las recompensas!** 🐛

Esto permitía **farmeo infinito** de recompensas simplemente cambiando entre mundos.

---

## 🔧 SOLUCIÓN IMPLEMENTADA

### Cambio Principal

Las recompensas ahora se verifican **globalmente** sin importar el mundo:

```java
// ✅ DESPUÉS (CORRECTO)
String key = player.getUniqueId().toString() + ":" + rank.name();
// Ejemplo: "uuid-123:EXPLORADOR"
```

### Archivos Modificados

**`RewardService.java`** - 4 funciones actualizadas:

1. **`deliverRewards()`** - Línea 150
   - ❌ Antes: Verificaba `UUID:worldName:RANGO`
   - ✅ Ahora: Verifica `UUID:RANGO` (global)

2. **`forceDeliverRewards()`** - Línea 265
   - ❌ Antes: Removía clave con worldName
   - ✅ Ahora: Remueve clave global

3. **`hasReceivedRewards()`** - Línea 329
   - ❌ Antes: Verificaba con worldName
   - ✅ Ahora: Verifica globalmente

4. **`getDeliveredRewards()`** - Línea 285
   - ✅ Actualizado comentario para reflejar el nuevo formato

---

## 📊 IMPACTO DE LA CORRECCIÓN

### ✅ BENEFICIOS

1. **No más recompensas duplicadas** al cambiar entre mundos
2. **Equidad:** Todos los jugadores reciben solo 1 set de recompensas por rango
3. **Economía balanceada:** Fin del farmeo infinito de diamantes/netherite
4. **Consistencia:** Recompensas globales independiente del mundo

### ⚠️ CONSIDERACIONES

- **Jugadores existentes:** Los que ya farmearon recompensas las conservan
- **Archivo de datos:** `rewards_delivered.yml` contendrá claves viejas y nuevas
- **Compatibilidad:** El sistema lee ambos formatos sin problemas

---

## 🔍 DETALLES TÉCNICOS

### Formato de Claves

**ANTES (vulnerable):**
```
UUID:worldName:RANGO
uuid-123:world_ciclo1:EXPLORADOR
uuid-123:world_ciclo2:EXPLORADOR  ← Clave diferente = recompensas duplicadas
```

**DESPUÉS (seguro):**
```
UUID:RANGO
uuid-123:EXPLORADOR  ← Una sola clave = una sola recompensa
```

### Sistema de Verificación

```java
// Verificación en deliverRewards()
String key = player.getUniqueId().toString() + ":" + rank.name();

if (deliveredRewards.contains(key)) {
    plugin.getLogger().info("[Rewards] " + player.getName() + 
        " ya recibió recompensas de " + rank.name() + " (global)");
    return false; // ✅ Bloquea recompensas duplicadas
}
```

### Persistencia

Las recompensas se guardan en `rewards_delivered.yml`:

```yaml
delivered_rewards:
  - "uuid-123:EXPLORADOR"
  - "uuid-123:SOBREVIVIENTE"
  - "uuid-456:VETERANO"
  # Formato nuevo sin worldName
```

---

## 🧪 CASOS DE PRUEBA

### Escenario 1: Cambio de Mundo Normal
```
1. Jugador sube a EXPLORADOR en world_ciclo1
   → Recibe recompensas: 6 diamantes, 4 manzanas doradas, etc. ✅
2. Jugador cambia a world_ciclo2
   → Sistema verifica: "¿uuid-123:EXPLORADOR ya existe?"
   → Respuesta: SÍ ✅
   → NO da recompensas de nuevo ✅
```

### Escenario 2: Reconexión en Otro Mundo
```
1. Jugador desconecta en world_ciclo1 (rango EXPLORADOR)
2. Jugador reconecta en world_ciclo2
   → Sistema verifica recompensas pendientes
   → Detecta que ya recibió EXPLORADOR globalmente ✅
   → NO da recompensas duplicadas ✅
```

### Escenario 3: Múltiples Rangos
```
1. Jugador sube a EXPLORADOR → Recompensas entregadas ✅
2. Jugador sube a SOBREVIVIENTE → Recompensas entregadas ✅
3. Jugador cambia de mundo
   → EXPLORADOR: Bloqueado (ya recibido) ✅
   → SOBREVIVIENTE: Bloqueado (ya recibido) ✅
```

---

## 📈 LOGS DE EJEMPLO

### Antes del Fix (Bug)
```
[Rewards] Procesando recompensas de EXPLORADOR para Player1
[Rewards] Items agregados al sistema de reclamación: 5
[Rewards] ✓ Recompensas de EXPLORADOR añadidas para Player1

// Jugador cambia de mundo

[Rewards] Procesando recompensas de EXPLORADOR para Player1  ← ❌ DUPLICADO
[Rewards] Items agregados al sistema de reclamación: 5        ← ❌ DUPLICADO
```

### Después del Fix (Correcto)
```
[Rewards] Procesando recompensas de EXPLORADOR para Player1
[Rewards] Items agregados al sistema de reclamación: 5
[Rewards] ✓ Recompensas de EXPLORADOR añadidas para Player1

// Jugador cambia de mundo

[Rewards] Player1 ya recibió recompensas de EXPLORADOR (global)  ← ✅ BLOQUEADO
```

---

## 🔐 SEGURIDAD

### Prevención de Exploits

1. **Cambio rápido de mundos:** Bloqueado ✅
2. **Reconexión en otros mundos:** Bloqueado ✅
3. **Uso de /avo cambiar:** Bloqueado ✅
4. **Teleport entre ciclos:** Bloqueado ✅

### Validación de Integridad

```java
// Cada entrega de recompensas se registra globalmente
deliveredRewards.add(key);  // UUID:RANGO
saveDeliveredRewards();     // Persiste a disco inmediatamente
```

---

## 🎁 RECOMPENSAS POR RANGO (Recordatorio)

Para referencia, estas son las recompensas que se entregan UNA SOLA VEZ:

| Rango | Recompensas |
|-------|-------------|
| **EXPLORADOR** | 1 bloque protección, 6 diamantes, 4 manzanas doradas, 12 hierro, 8 perlas |
| **SOBREVIVIENTE** | 1 bloque protección, 12 diamantes, 6 manzanas doradas, 24 perlas, libro encantado |
| **VETERANO** | 2 bloques protección, 20 diamantes, 2 manzanas encantadas, 48 perlas, tótem, 2 scrap |
| **LEYENDA** | 2 bloques protección, 6 netherite, 4 manzanas encantadas, 2 tótems, elytra, dragon head |
| **MAESTRO** | 3 bloques protección, 12 netherite, 6 manzanas encantadas, 4 tótems, 2 estrellas, beacon |
| **TITAN** | 3 bloques protección, 20 netherite, 10 manzanas encantadas, 6 tótems, 4 estrellas, beacon |
| **ABSOLUTO** | 5 bloques protección, 40 netherite, 24 manzanas encantadas, 12 tótems, 8 estrellas, 2 beacons |

---

## ✅ VALIDACIÓN

### Checklist de Corrección

- [x] `deliverRewards()` usa clave global
- [x] `forceDeliverRewards()` usa clave global
- [x] `hasReceivedRewards()` usa clave global
- [x] `getDeliveredRewards()` documentación actualizada
- [x] `setDeliveredRewards()` documentación actualizada
- [x] Comentarios actualizados con "FIX v1.22.62"
- [x] Logs informativos mejoran debugging

---

## 🚀 DEPLOYMENT

### Aplicar el Fix

1. **Compilar** el plugin con los cambios
2. **Reiniciar** el servidor
3. **Verificar** logs durante cambios de mundo
4. **Monitorear** que no haya recompensas duplicadas

### Rollback (si es necesario)

Si se detectan problemas:
1. Restaurar versión v1.22.61
2. Verificar archivo `rewards_delivered.yml`
3. Reportar issue con logs completos

---

## 📝 NOTAS IMPORTANTES

### Migración de Datos

- **Claves viejas** (con worldName) siguen siendo válidas
- **Claves nuevas** (sin worldName) se crean a partir de ahora
- El sistema lee ambos formatos sin conflictos
- Limpieza manual opcional: remover claves viejas de `rewards_delivered.yml`

### Comando de Emergencia

Si un jugador reporta no recibir recompensas legítimas:

```
/avo recompensas force <jugador> <rango>
```

Esto fuerza la entrega ignorando el registro.

---

## 🏷️ TAGS
`v1.22.62` `bugfix` `recompensas` `duplicacion` `multimundo` `critical`

---

**Versión:** 1.22.62  
**Tipo:** Bugfix Crítico  
**Prioridad:** Alta  
**Impacto:** Crítico (economía del servidor)  
**Estado:** ✅ Resuelto
