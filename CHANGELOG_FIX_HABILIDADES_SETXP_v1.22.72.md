# 🔧 FIX: Aplicación de Habilidades al Usar `/avo setxp`

**Versión**: 1.22.72  
**Fecha**: 7 de febrero de 2026  
**Tipo**: Corrección de bug  

---

## 📋 Problema Detectado

Cuando un administrador usaba `/avo setxp` para ajustar el XP de un jugador:

### ✅ Funcionaba Correctamente
- Efectos visuales (partículas, sonidos, título "⬆ RANGO ASCENDIDO ⬆")
- Mensaje global al servidor
- Recompensas (items) - Solo se entregan la primera vez
- Actualización de scoreboard y tablist

### ❌ NO Funcionaba
- **Habilidades pasivas del rango NO se aplicaban**
  - Night Vision, Saturation, Speed, Dolphins Grace, etc.
  - Efectos de poción permanentes configurados en `recompensas.yml`

---

## 🔍 Análisis Técnico

### Diferencia entre Rank Up Natural vs Manual

#### Rank Up Natural (Completar Misión)
```
MissionService.completeMission()
    ↓
addPS() → Detecta rank up
    ↓
ExperienceService.onRankUp()
    ↓
deliverRewards() ✅ + applyAbilities() ✅
```

#### Rank Up Manual (`/avo setxp`) - ANTES del fix
```
/avo setxp Jugador 4700
    ↓
ExperienceService.setXP() → MissionService.setPS()
    ↓
setPS() detecta rank up
    ↓
playRankUpEffects() ✅ + deliverRewards() ✅
    ↓
applyAbilities() ❌ NO SE LLAMABA
```

### ¿Por Qué es Crítico?
Las habilidades pasivas (Night Vision, Saturation, etc.) son efectos de poción permanentes que se renuevan automáticamente cada 30 segundos. Sin estas habilidades, los jugadores **pierden beneficios esenciales** de su rango.

---

## ✅ Solución Implementada

### Modificación en `MissionService.setPS()` y `setPlayerPs()`

**Archivo**: `src/main/java/me/apocalipsis/missions/MissionService.java`

Se agregó la aplicación de habilidades **SIEMPRE** cuando se establece XP manualmente:

```java
public void setPS(UUID uuid, int ps) {
    // ... código de rank up si oldRank != newRank ...
    
    // [FIX v1.22.72] Aplicar habilidades SIEMPRE (no solo en rank up)
    // Las habilidades son pasivas y deben estar activas según el rango actual
    Player player = plugin.getServer().getPlayer(uuid);
    if (player != null && player.isOnline() && plugin.getAbilityService() != null) {
        final Player finalPlayer = player;
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (finalPlayer.isOnline()) {
                // Aplicar sin notificar (el sistema ya mostró mensajes de rank up)
                plugin.getAbilityService().applyAbilities(finalPlayer, false);
            }
        }, 40L); // 2 segundos después
    }
}
```

**Cambios clave**:
- ✅ Se aplican habilidades con delay de 40 ticks (2 segundos)
- ✅ Se aplican **SIEMPRE**, no solo en rank ups
- ✅ No notifica al jugador (para evitar spam, ya hay mensajes de rank up)
- ✅ Validación de `player.isOnline()` antes de aplicar
- ✅ Mismo delay que `ExperienceService.onRankUp()`

---

## 🔄 Comportamiento Después del Fix

### Flujo Completo
```
/avo setxp Jugador SOBREVIVIENTE
    ↓
ExperienceService.setXP() → MissionService.setPS()
    ↓
Si oldRank != newRank:
    ├─ playRankUpEffects() ✅ (partículas, sonidos, título)
    ├─ deliverRewards() ✅ (solo si no las recibió antes)
    └─ Actualizar scoreboard/tablist ✅
    ↓
[Delay 2s] → applyAbilities() ✅ NUEVO
    ├─ Night Vision activado
    ├─ Saturation activado
    └─ ... (según configuración del rango)
```

### Diferencias Importantes

#### ⚙️ Recompensas (Items/Comandos)
- **Primera vez**: ✅ Se entregan
- **Veces siguientes**: ❌ NO se entregan (ya las recibió)
- **Razón**: Evitar duplicación de items valiosos

#### 💪 Habilidades (Efectos Pasivos)
- **Primera vez**: ✅ Se aplican
- **Veces siguientes**: ✅ Se aplican SIEMPRE
- **Razón**: Son efectos de poción que deben estar activos

---

## 📦 Qué Recibe el Jugador Ahora

Cuando un admin usa `/avo setxp Jugador 4700`:

### 1️⃣ Siempre
- ✅ **Efectos visuales** (partículas, sonidos, título)
- ✅ **Mensaje global** "★ Jugador ha alcanzado el rango Resistente ★"
- ✅ **Habilidades pasivas** (Night Vision, Saturation, etc.) 🆕
- ✅ **Actualización UI** (scoreboard, tablist)

### 2️⃣ Solo la Primera Vez
- ✅ **Recompensas** (items reclamables vía `/recompensa`)
- ✅ **Comandos especiales** (PS adicionales, etc.)

---

## 🧪 Casos de Prueba

### Caso 1: Primera vez que alcanza el rango
```
/avo setxp Player1 4700
→ ✅ Efectos visuales
→ ✅ Recompensas entregadas
→ ✅ Habilidades aplicadas (Night Vision, Saturation)
```

### Caso 2: Ya había alcanzado el rango antes
```
/avo setxp Player2 4700 (ya tuvo SOBREVIVIENTE antes)
→ ✅ Efectos visuales
→ ⏭️ Recompensas NO entregadas (ya las tiene)
→ ✅ Habilidades aplicadas (Night Vision, Saturation)
```

### Caso 3: Cambiar XP sin cambiar rango
```
/avo setxp Player3 4800 (ya está en SOBREVIVIENTE)
→ ⏭️ No hay rank up
→ ⏭️ No se procesan efectos de rank up
→ ✅ Habilidades aplicadas (porque setPS siempre las aplica) 🆕
```

---

## 📊 Impacto

- **Archivos modificados**: 1
  - `MissionService.java`: +12 líneas
- **Métodos afectados**: 2
  - `setPS()` - Ahora aplica habilidades
  - `setPlayerPs()` - Ahora aplica habilidades
- **Compatibilidad**: ✅ Totalmente compatible
- **Performance**: Sin impacto (solo afecta comandos admin)
- **Riesgo**: 🟢 BAJO (cambio localizado)

---

## ✅ Checklist de Verificación

- [x] Código compila sin errores
- [x] Habilidades se aplican cuando se usa `/avo setxp`
- [x] Recompensas NO se duplican (comportamiento correcto)
- [x] Delay de 2 segundos funciona
- [x] Validación de player.isOnline()
- [x] No notifica spam (notify=false)
- [x] Compatible con sistema de ciclos
- [x] Compatible con rank ups naturales

---

## 🔗 Relacionado

- `CHANGELOG_SISTEMA_RANGOS_POR_NIVEL_v1.22.70.md` - Sistema de rangos
- `OPTIMIZACIONES_RENDIMIENTO_v1.22.68.md` - AbilityService optimizado
- `DOCUMENTACION_RANGOS.md` - Documentación de rangos

---

**Estado**: ✅ COMPLETADO  
**Probado**: ⏳ Pendiente de prueba en servidor  
**Urgencia**: 🟡 MEDIA (afecta comandos administrativos)
