# 📊 CHANGELOG v1.22.62 - Rebalanceo de Habilidades de Rangos

## 📅 Fecha: 2025

---

## 🎯 OBJETIVO DEL REBALANCEO

Eliminar habilidades **overpowered (OP)** de rangos bajos para mejorar la progresión y hacer que alcanzar rangos altos sea más gratificante.

**Problema principal:** SATURATION en rangos bajos (EXPLORADOR, SOBREVIVIENTE, VETERANO) eliminaba completamente la necesidad de comer, haciendo el juego demasiado fácil desde el inicio.

---

## ⚖️ CAMBIOS IMPLEMENTADOS

### 📋 RESUMEN DE CAMBIOS POR RANGO

| Rango | Cambios |
|-------|---------|
| **NOVATO** | ✅ Sin cambios - Solo NIGHT_VISION (básico) |
| **EXPLORADOR** | ❌ SATURATION removido → ✅ LUCK I agregado |
| **SOBREVIVIENTE** | ❌ SATURATION removido → Mantiene LUCK I y DOLPHINS_GRACE |
| **VETERANO** | ❌ SATURATION removido → ✅ RESISTANCE I agregado |
| **LEYENDA** | ✅ SATURATION agregado (primer rango con este buff) |
| **MAESTRO** | ✅ DOLPHINS_GRACE upgrade a nivel II |
| **TITAN** | ✅ Sin cambios |
| **ABSOLUTO** | ✅ Sin cambios |

---

## 🔧 CAMBIOS DETALLADOS

### 🟢 NOVATO (Perdido)
**Sin cambios**
```yaml
- NIGHT_VISION I → Visión Nocturna (El primer don)
```

---

### 🟢 EXPLORADOR (Despertar)
**ANTES:**
```yaml
- NIGHT_VISION I
- SATURATION I ← ❌ MUY OP para rango bajo
```

**DESPUÉS:**
```yaml
- NIGHT_VISION I
- LUCK I ← ✅ Mejora moderada, no OP
```

**Justificación:** SATURATION eliminaba la necesidad de comida desde día 3-5. LUCK I es útil pero no rompe el juego.

---

### 🟢 SOBREVIVIENTE (Resistente)
**ANTES:**
```yaml
- NIGHT_VISION I
- SATURATION I ← ❌ Todavía muy OP
- DOLPHINS_GRACE I
```

**DESPUÉS:**
```yaml
- NIGHT_VISION I
- LUCK I
- DOLPHINS_GRACE I
```

**Justificación:** Mantiene la utilidad acuática y suerte, pero los jugadores deben seguir gestionando comida.

---

### 🟢 VETERANO (Perseverante)
**ANTES:**
```yaml
- NIGHT_VISION I
- SATURATION I ← ❌ Aún muy pronto para este buff
- DOLPHINS_GRACE I
- LUCK I
```

**DESPUÉS:**
```yaml
- NIGHT_VISION I
- LUCK I
- DOLPHINS_GRACE I
- RESISTANCE I ← ✅ Nuevo buff defensivo moderado
```

**Justificación:** RESISTANCE I (8% reducción de daño) es útil pero no elimina un mechanic completo como SATURATION.

---

### 🟡 LEYENDA (Ascendido)
**NUEVO CAMBIO:**
```yaml
+ SATURATION I ← ✅ Ahora el PRIMER rango con este buff
- NIGHT_VISION I
- DOLPHINS_GRACE I
- LUCK II (upgrade)
- RESISTANCE I
```

**Justificación:** Los jugadores deben alcanzar LEYENDA (~día 35-40) para obtener SATURATION. Esto es más equilibrado y gratificante.

---

### 🟡 MAESTRO (Iluminado)
**MEJORA:**
```yaml
- DOLPHINS_GRACE I → II ← ✅ Upgrade a nivel II
- Resto sin cambios
```

**Justificación:** DOLPHINS_GRACE I pasó a LEYENDA, así que MAESTRO obtiene el upgrade a nivel II directamente.

---

### 🟢 TITAN y ABSOLUTO
**Sin cambios** - Ya estaban bien balanceados.

---

## 📊 TABLA COMPARATIVA DE HABILIDADES

### Antes del Rebalanceo
```
NOVATO:        NIGHT_VISION
EXPLORADOR:    NIGHT_VISION, SATURATION ← ❌ OP
SOBREVIVIENTE: NIGHT_VISION, SATURATION, DOLPHINS_GRACE ← ❌ OP
VETERANO:      NIGHT_VISION, SATURATION, DOLPHINS_GRACE, LUCK ← ❌ OP
LEYENDA:       NIGHT_VISION, SATURATION, DOLPHINS_GRACE, LUCK II, RESISTANCE
MAESTRO:       + STRENGTH, ABSORPTION
TITAN:         + Upgrades
ABSOLUTO:      + Más upgrades
```

### Después del Rebalanceo
```
NOVATO:        NIGHT_VISION
EXPLORADOR:    NIGHT_VISION, LUCK ← ✅ Balanceado
SOBREVIVIENTE: NIGHT_VISION, LUCK, DOLPHINS_GRACE ← ✅ Balanceado
VETERANO:      NIGHT_VISION, LUCK, DOLPHINS_GRACE, RESISTANCE ← ✅ Balanceado
LEYENDA:       NIGHT_VISION, SATURATION, DOLPHINS_GRACE, LUCK II, RESISTANCE ← ✅ Primera SATURATION
MAESTRO:       + DOLPHINS_GRACE II, STRENGTH, ABSORPTION
TITAN:         + Upgrades
ABSOLUTO:      + Más upgrades
```

---

## 🎮 IMPACTO EN LA JUGABILIDAD

### ✅ MEJORAS

1. **Progresión más gratificante**: Ahora alcanzar LEYENDA es un logro importante (SATURATION desbloqueado)
2. **Primeros días más desafiantes**: Jugadores deben gestionar comida en rangos bajos
3. **Mejor balance**: Habilidades crecen gradualmente sin saltos OP
4. **Incentivo para subir de rango**: SATURATION se convierte en recompensa aspiracional

### ⚠️ AJUSTES PARA JUGADORES

- **Rangos bajos (EXPLORADOR-VETERANO)**: Ahora deben llevar comida y comerla normalmente
- **Rango LEYENDA**: Primera vez que se desbloquea SATURATION (recompensa épica)
- **Rangos altos**: Sin cambios, mantienen sus habilidades poderosas

---

## 🔍 DETALLES TÉCNICOS

### Archivos Modificados
```
src/main/resources/recompensas.yml
  - Sección: habilidades_por_rango
  - Cambios: 4 rangos modificados (EXPLORADOR, SOBREVIVIENTE, VETERANO, LEYENDA, MAESTRO)
```

### Sistema de Aplicación
- Las habilidades se aplican automáticamente cada 30 segundos
- Duración de 60 segundos por efecto
- Sin cambios en la lógica de `AbilityService.java`

---

## 📈 CURVA DE PROGRESIÓN MEJORADA

```
Día 1-3:   NOVATO        → Solo visión nocturna (básico)
Día 3-5:   EXPLORADOR    → + Suerte (no comida gratis) ✅
Día 10-12: SOBREVIVIENTE → + Nado rápido (aún sin comida gratis) ✅
Día 20-25: VETERANO      → + Resistencia leve (sin comida gratis) ✅
Día 35-40: LEYENDA       → ¡SATURATION DESBLOQUEADO! 🎉
Día 50+:   MAESTRO+      → Habilidades épicas
```

---

## ✅ RESULTADO FINAL

| Aspecto | Antes | Después |
|---------|-------|---------|
| **Rangos con SATURATION** | 4 (desde día 3) | 4 (desde día 35) |
| **Primer rango con SATURATION** | EXPLORADOR (día 3-5) | LEYENDA (día 35-40) |
| **Dificultad primeros días** | Muy fácil | Moderada ✅ |
| **Incentivo para subir** | Bajo | Alto ✅ |
| **Balance general** | Desbalanceado | Equilibrado ✅ |

---

## 📝 NOTAS IMPORTANTES

- ✅ **Compatibilidad**: Cambios solo en configuración YAML, sin modificar código Java
- ✅ **Retrocompatibilidad**: Jugadores actuales verán los cambios al reconectar
- ✅ **Balance philosophy**: Habilidades QoL (Quality of Life) se desbloquean gradualmente
- ⚠️ **Feedback esperado**: Jugadores en rangos bajos pueden notar que necesitan comer ahora

---

## 🎯 PRÓXIMOS PASOS

1. ✅ Monitorear feedback de jugadores en rangos bajos
2. ✅ Ajustar si es necesario basado en datos de juego
3. ✅ Considerar añadir más habilidades únicas en rangos medios

---

## 🏷️ TAGS
`v1.22.62` `balance` `rangos` `habilidades` `saturation` `nerf` `progression`

---

**Versión:** 1.22.62  
**Tipo:** Balance / Nerf rangos bajos  
**Prioridad:** Alta  
**Impacto:** Medio-Alto (afecta progresión temprana)
