# 🎯 CHANGELOG - Sistema de Rangos por Nivel v1.22.70

## 📋 Resumen
Cambio del sistema de rangos para usar **niveles** en vez de **XP total**, facilitando el control y haciendo más intuitivo el progreso.

---

## ✨ Cambios Principales

### 1. **MissionRank - Nuevo campo `levelRequired`**
- ✅ Agregado `levelRequired` para cada rango
- ✅ Valores por defecto: NOVATO=1, EXPLORADOR=5, SOBREVIVIENTE=10, VETERANO=15, LEYENDA=20, MAESTRO=25, TITAN=30, ABSOLUTO=35
- ✅ Método `fromLevel()` ahora es el método principal (compara con `levelRequired`)
- ✅ Agregado getter `getLevelRequired()`
- ⚠️ `fromXp()` y `xpRequired` mantenidos por compatibilidad (deprecados)

### 2. **RankService - Uso de niveles**
- ✅ `getRank()` ahora usa `ExperienceService.getLevel()` + `MissionRank.fromLevel()`
- ✅ `loadRanksConfig()` ahora lee `level_required` desde `rangos.yml`
- ✅ `getProgressToNextRank()` calcula progreso usando niveles
- ✅ `getNextRankThreshold()` devuelve nivel requerido del siguiente rango
- ✅ Agregado `getLevelForRank()` - obtiene nivel requerido para un rango
- ✅ Agregado `getRankForLevel()` - obtiene rango para un nivel
- ⚠️ `getXpForRank()` y `getRankForXP()` deprecados (mantenidos por compatibilidad)

### 3. **SkillService - Preview de compras**
- ✅ `previewPurchase()` ahora calcula niveles antes y después
- ✅ Usa `getRankForLevel()` para determinar si bajará de rango
- ✅ Más preciso para advertir sobre bajadas de rango

### 4. **ExperienceService - Método público**
- ✅ `calculateLevel()` ahora es público (necesario para SkillService)

---

## 🔧 Configuración Required (rangos.yml)

Agregar `level_required` a cada rango en `rangos.yml`:

```yaml
ranks:
  NOVATO:
    display_name: "&7Perdido"
    level_required: 1    # NUEVO
    ps_required: 0
    misiones_diarias: 10
    # ... resto de config
  
  EXPLORADOR:
    display_name: "&aDespertar"
    level_required: 5    # NUEVO
    ps_required: 980
    misiones_diarias: 8
    # ... resto de config
  
  SOBREVIVIENTE:
    display_name: "&bResistente"
    level_required: 10   # NUEVO
    ps_required: 3780
    misiones_diarias: 6
    # ... resto de config
  
  VETERANO:
    display_name: "&ePerseverante"
    level_required: 15   # NUEVO
    ps_required: 8330
    misiones_diarias: 5
    # ... resto de config
  
  LEYENDA:
    display_name: "&6Ascendido"
    level_required: 20   # NUEVO
    ps_required: 14630
    misiones_diarias: 4
    # ... resto de config
  
  MAESTRO:
    display_name: "&5Trascendente"
    level_required: 25   # NUEVO
    ps_required: 22680
    misiones_diarias: 3
    # ... resto de config
  
  TITAN:
    display_name: "&4Inmortal"
    level_required: 30   # NUEVO
    ps_required: 32480
    misiones_diarias: 3
    # ... resto de config
  
  ABSOLUTO:
    display_name: "&c&lEterno"
    level_required: 35   # NUEVO
    ps_required: 44030
    misiones_diarias: 2
    # ... resto de config
```

---

## 📊 Ventajas del Nuevo Sistema

### ✅ **Más Fácil de Controlar**
- Los admins solo necesitan cambiar un número (`level_required: 15`) en vez de calcular XP total
- Más intuitivo: "Rango Veterano = Nivel 15" vs "Rango Veterano = 8330 XP"

### ✅ **Más Claro para Jugadores**
- Los jugadores ven su nivel en la pantalla
- Saben exactamente cuánto falta: "Necesito nivel 20 para Leyenda"

### ✅ **Mejor Balance**
- Fácil ajustar progresión: cambiar `level_required: 20` a `level_required: 18`
- No necesitas recalcular toda la curva de XP

### ✅ **Compatible con Sistema Actual**
- XP sigue funcionando igual para skills y misiones
- Solo cambia cómo se determinan los rangos
- Métodos antiguos deprecados pero funcionales

---

## 🔄 Compatibilidad

### Mantenido por compatibilidad:
- ❌ `MissionRank.fromXp()` - deprecado, usar `fromLevel()`
- ❌ `MissionRank.getXpRequired()` - deprecado, usar `getLevelRequired()`
- ❌ `RankService.getXpForRank()` - deprecado, usar `getLevelForRank()`
- ❌ `RankService.getRankForXP()` - deprecado, usar `getRankForLevel()`

### Nuevo sistema:
- ✅ `MissionRank.fromLevel(int level)` - **MÉTODO PRINCIPAL**
- ✅ `MissionRank.getLevelRequired()` - nivel requerido del rango
- ✅ `RankService.getLevelForRank(MissionRank)` - nivel del rango
- ✅ `RankService.getRankForLevel(int level)` - rango para nivel

---

## 🎮 Ejemplos de Uso

### Para admins:
```java
// Antes (confuso):
// "¿Cuánto XP necesito para que sea Veterano? 8330... ¿y eso cuánto es?"

// Ahora (claro):
int nivelRequerido = MissionRank.VETERANO.getLevelRequired(); // 15
player.sendMessage("Necesitas nivel " + nivelRequerido + " para ser Veterano");
```

### Para desarrollo:
```java
// Verificar rango por nivel
int nivel = experienceService.getLevel(player);
MissionRank rango = MissionRank.fromLevel(nivel); // Directo y claro

// Preview de compra de skill
int nuevoNivel = experienceService.calculateLevel(xpDespuesDeCompra);
MissionRank nuevoRango = rankService.getRankForLevel(nuevoNivel);
if (nuevoRango.ordinal() < rangoActual.ordinal()) {
    player.sendMessage("¡Bajarás de rango!");
}
```

---

## 📝 Testing Checklist

- [ ] Verificar que rangos se asignan correctamente por nivel
- [ ] Probar progreso hacia siguiente rango
- [ ] Verificar preview de compra de skills (bajada de rango)
- [ ] Confirmar que comandos `/avo setxp` funcionan
- [ ] Verificar que recompensas de rango se entregan correctamente
- [ ] Probar mensajes de ascenso de rango
- [ ] Verificar permisos de rango

---

## 🐛 Fixes Incluidos

### Sistema de Efectos de Habilidades
- ✅ Límites máximos globales para efectos de poción
- ✅ Tracking de efectos aplicados para prevenir bucles
- ✅ Sistema de 3 capas de validación
- ✅ Haste máximo nivel 3 (antes podía llegar a 5)
- ✅ Detección inteligente de fuentes externas (beacons, pociones)
- ✅ Limpieza automática de tracking al desconectar

### Balance de Habilidades de Invocación
- ✅ Manada de lobos: 2/3/4 (era 2/3/5)
- ✅ Enjambre de abejas: 2/3/5 (era 3/5/8)
- ✅ Abejas protectoras: 2/3/5 (era 2/4/6)
- ✅ Ejército de esqueletos: 2/3/5 (era 2/4/6)
- ✅ Todas las invocaciones limitadas a máximo 5 entidades

---

## 🚀 Deployment

1. Actualizar `rangos.yml` con los nuevos campos `level_required`
2. Recargar plugin o reiniciar servidor
3. Verificar logs: `[RANGOS.YML] Cargados X rangos desde rangos.yml`
4. Probar con `/avo ranks` para ver rangos
5. Verificar que `/avo setxp <player> <rango>` funciona

---

**Versión:** 1.22.70  
**Fecha:** 2026-02-05  
**Autor:** Sistema de IA  
**Tipo:** Feature + Improvement + Bugfix
