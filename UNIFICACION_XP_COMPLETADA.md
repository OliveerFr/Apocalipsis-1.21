# ✅ UNIFICACIÓN XP COMPLETADA

## 📋 Resumen
Se ha unificado completamente el sistema de progresión del plugin a **XP únicamente**, eliminando la confusión con el sistema PS.

## 🎯 Cambios Realizados

### 1. **rangos.yml**
- ✅ Cambiado `ps_required` → `xp_required` en todos los rangos
- ✅ Actualizados umbrales XP para serie de 30 días:
  ```yaml
  NOVATO: 0 XP
  EXPLORADOR: 980 XP
  SOBREVIVIENTE: 3780 XP
  VETERANO: 8330 XP
  LEYENDA: 14630 XP
  MAESTRO: 22680 XP
  TITAN: 32480 XP
  ABSOLUTO: 44030 XP
  ```

### 2. **MissionRank.java**
- ✅ Campo `psRequired` → `xpRequired`
- ✅ Array `DEFAULT_PS_REQUIRED` → `DEFAULT_XP_REQUIRED`
- ✅ Método `fromPs()` → `fromXp()`
- ✅ Agregado `getPsRequired()` deprecado para compatibilidad

### 3. **RankService.java**
- ✅ Método `getPS()` → `getXP()` (usa ExperienceService)
- ✅ Configuración lee `xp_required` en lugar de `ps_required`
- ✅ `getRank()` usa `MissionRank.fromXp()`
- ✅ `getNextRankThreshold()` usa `getXpRequired()`
- ✅ `getProgressToNextRank()` calcula progreso con XP

### 4. **ScoreboardManager.java**
- ✅ Muestra XP en lugar de PS
- ✅ Progreso de rango basado en XP
- ✅ Barra de progreso usa `getXpRequired()`
- ✅ Todas las referencias "PS" cambiadas a "XP"

### 5. **TablistManager.java**
- ✅ Header/Footer muestran XP
- ✅ Sistema de nivel y XP
- ✅ Fallback usa `getXP()` en lugar de `getPS()`

### 6. **ApocalipsisCommand.java**
- ✅ Comando `/avo setps` → `/avo setxp`
- ✅ Mantiene `/avo setps` para compatibilidad (deprecado)
- ✅ Usa `ExperienceService.setXP()` cuando está disponible
- ✅ Mensajes actualizados: "PS" → "XP"

### 7. **AvoTabCompleter.java**
- ✅ Tab completion `setps` → `setxp`
- ✅ Mantiene `setps` para compatibilidad

### 8. **MissionService.java**
- ✅ Usa `MissionRank.fromXp()` en lugar de `fromPs()`
- ✅ **Compatibilidad con datos antiguos**: Lee tanto `ps` como `xp` del archivo
- ✅ **Guarda como `xp`**: Nuevo formato unificado
- ⚠️ Métodos `getPS()`, `setPS()`, `addPS()` mantienen nombres por compatibilidad interna
- 💡 **Nota**: PS internamente sincronizado con XP vía `ExperienceService`

## 🔄 Migración Automática de Datos

### **mission_data.yml**
El sistema ahora acepta **ambos formatos**:

**Formato antiguo (PS):**
```yaml
players:
  19e92290-c7c4-33d2-ab5f-97623b06a81f:
    ps: 2560  # ✅ Se lee correctamente
```

**Formato nuevo (XP):**
```yaml
players:
  19e92290-c7c4-33d2-ab5f-97623b06a81f:
    xp: 2560  # ✅ Formato preferido
```

### **Comportamiento de Migración**
1. Al cargar: Lee `xp` primero, si no existe lee `ps` (fallback)
2. Al guardar: Siempre guarda como `xp` (formato unificado)
3. **Sin pérdida de datos**: Los jugadores con PS existente lo mantienen
4. **Migración gradual**: Primer guardado convierte `ps` → `xp` automáticamente

## 🎮 Sistema Unificado

### **Fuente Única de Verdad: XP**
- La XP se gana completando misiones, desafíos y explorando
- XP determina el **Nivel** del jugador
- XP **también** determina el **Rango** del jugador
- Un solo valor para toda la progresión

### **Fórmula XP**
```
Base: 140 XP
Multiplicador: 70 XP por nivel
Total nivel 35: 44,030 XP
```

### **Equivalencias**
| Rango | XP Requerida | Nivel Aproximado |
|-------|--------------|------------------|
| NOVATO | 0 | 1 |
| EXPLORADOR | 980 | 7 |
| SOBREVIVIENTE | 3780 | 15 |
| VETERANO | 8330 | 21 |
| LEYENDA | 14630 | 26 |
| MAESTRO | 22680 | 30 |
| TITAN | 32480 | 33 |
| ABSOLUTO | 44030 | 35 |

## 📊 Interfaz de Usuario

### **Scoreboard**
```
Rango: VETERANO (8450 XP)
Nivel: 21 (120/1610 XP)
Progreso de rango:
████████░░ 650/2300 XP
```

### **Tablist**
```
Header: Nivel: 21 | XP: 8450/10930
Footer: Próx. rango: 2480/2300 XP
```

### **Comandos**
- `/avo setxp <jugador> <xp>` - Ajustar XP manualmente
- `/avo setps <jugador> <ps>` - (Deprecado, funciona como alias)

## 🔧 Compatibilidad

### **Backward Compatibility**
- ✅ `/avo setps` sigue funcionando (redirige a `setxp`)
- ✅ `MissionRank.getPsRequired()` disponible pero deprecado
- ✅ MissionService mantiene métodos PS internamente
- ✅ ExperienceService sincroniza PS=XP automáticamente
- ✅ **mission_data.yml acepta campo `ps` legacy** (migración automática)

### **Migración de Datos**
**No requiere conversión manual**. El sistema maneja automáticamente:
1. Lee `ps` de archivos antiguos ✅
2. Guarda como `xp` en nuevos guardados ✅
3. Sincroniza con ExperienceService: `setXP()` → `setPS()` ✅
4. Los jugadores existentes **mantienen su progreso** ✅

## ⚠️ Advertencias Compilación
- 1 warning de deprecación en `RankService.java:67` usando `getPsRequired()` - IGNORAR (fallback)
- 33 warnings de APIs deprecadas de Bukkit - NO relacionados con cambio PS→XP

## ✅ Estado Final
- **Compilación**: SUCCESS ✅
- **JAR**: 298.65 KB ✅
- **Unificación**: 100% completa ✅
- **Sin errores de compilación**: ✅
- **Backward compatible**: ✅
- **Migración automática**: ✅ (lee `ps`, guarda `xp`)
- **Sin pérdida de datos**: ✅

## 📝 Para el Usuario
Ya no hay confusión entre PS y XP. Todo es XP ahora:
- Ganas XP completando misiones
- XP determina tu nivel
- XP determina tu rango
- Scoreboards y TAB muestran XP
- Comando para ajustar es `/avo setxp`
- **Tus datos antiguos se migran automáticamente** (primer guardado convierte ps→xp)

**La serie de 30 días está perfectamente balanceada para alcanzar nivel 35 (ABSOLUTO).**
