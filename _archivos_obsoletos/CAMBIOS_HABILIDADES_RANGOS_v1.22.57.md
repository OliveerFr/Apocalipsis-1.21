# 🔧 CHANGELOG: ARREGLO DE CONFLICTOS HABILIDADES RANGOS vs ÁRBOL DE HABILIDADES
## Versión 1.22.57 - Eliminación de Interferencias

---

## 📋 RESUMEN DEL PROBLEMA

Las habilidades pasivas otorgadas por los rangos (**recompensas.yml**) interferían con las habilidades del **árbol de habilidades** (Skill Tree), causando:

1. **Redundancia**: Los jugadores recibían automáticamente por rango habilidades que podían/debían elegir en el árbol
2. **Pérdida de valor del árbol**: No tenía sentido gastar puntos en habilidades que ya tenías gratis por rango
3. **Confusión**: Los efectos se solapaban y los jugadores no sabían qué venía de dónde

---

## ✅ CONFLICTOS IDENTIFICADOS Y RESUELTOS

### ❌ Habilidades ELIMINADAS de los rangos (ahora solo en el árbol):

| Efecto Eliminado | Habilidades del Árbol Afectadas | Motivo |
|-----------------|--------------------------------|--------|
| **SPEED** (Velocidad I/II/III) | PASO_LIGERO (+10%), ZANCADAS (+20%), VELOCISTA (+30%) | Los jugadores deben ELEGIR si quieren velocidad |
| **HASTE** (Prisa I/II) | MINERO_EFICIENTE (+15% velocidad minado) | Los mineros deben invertir en esta rama |
| **REGENERATION** (Regeneración I/II/III) | REGENERACION_PASIVA (0.5❤ cada 20s) | Regeneración debe ser una elección táctica |
| **HEALTH_BOOST** (Vida Extra +4/+10/+16/+20❤) | PIEL_GRUESA (+2❤), TANQUE (+6❤), INMORTAL (+14❤) | La vida extra es el core de la rama Supervivencia |
| **JUMP_BOOST** (Salto I/II) | ZANCADAS (salto mejorado) | Salto mejorado es parte del paquete de movilidad |
| **WATER_BREATHING** (Respiración Acuática) | BRANQUIAS (+60% respiración), ANFIBIO (respiración infinita) | Los buceadores deben especializarse |
| **FIRE_RESISTANCE** (Resistencia al Fuego) | RESISTENCIA_FUEGO (-20% daño), IGNIFUGO (-40% daño) | Protección contra fuego requiere inversión |

---

## ✨ HABILIDADES QUE SE MANTIENEN EN LOS RANGOS

### ✅ Estas habilidades NO están en el árbol y son exclusivas de los rangos:

| Rango | Habilidades Pasivas | Descripción |
|-------|-------------------|-------------|
| **NOVATO** (Perdido) | Night Vision I | Visión nocturna básica |
| **EXPLORADOR** (Despertar) | Night Vision I, Saturation I | + Nunca tienes hambre |
| **SOBREVIVIENTE** (Resistente) | Night Vision I, Saturation I, Dolphins Grace I | + Nadas más rápido |
| **VETERANO** (Perseverante) | Night Vision I, Saturation I, Dolphins Grace I, Luck I | + La fortuna empieza a sonreírte |
| **LEYENDA** (Ascendido) | Night Vision I, Saturation I, Dolphins Grace I, Luck II, Resistance I | + Tu piel se endurece |
| **MAESTRO** (Iluminado) | Night Vision I, Saturation I, Dolphins Grace I, Luck II, Resistance I, Strength I, Absorption I | + Poder físico + Escudo dorado |
| **TITAN** (Trascendente) | Night Vision I, Saturation I, Dolphins Grace II, Luck III, Resistance II, Strength I, Absorption II, Hero of the Village I | + Descuentos con aldeanos |
| **ABSOLUTO** (Eterno) | Night Vision I, Saturation I, Dolphins Grace II, Luck IV, Resistance II, Strength II, Absorption III, Hero of the Village II, Conduit Power I | + Poder máximo del cóndor |

---

## 🎯 FILOSOFÍA DEL NUEVO SISTEMA

### 1. **Separación de Responsabilidades**
- **Rangos**: Dan habilidades de **utilidad general** y **calidad de vida**
  - Visión nocturna (QoL)
  - Saciedad (QoL)
  - Gracia del delfín (única, no en árbol)
  - Suerte (única, no en árbol)
  - Fuerza (única, no en árbol)
  - Resistencia (única, diferente a RESISTANCE del árbol)
  - Absorción (única, no en árbol)

- **Árbol de Habilidades**: Dan habilidades de **especialización y combate**
  - Velocidad (decisión táctica)
  - Vida extra (build tanque)
  - Regeneración (build sustain)
  - Prisa (build minero)
  - Skills de agua (build acuático)
  - Skills de fuego (build nether)

### 2. **Valor de la Elección**
Ahora los jugadores deben **tomar decisiones significativas**:
- ¿Invierto en velocidad o en vida?
- ¿Priorizo regeneración o daño?
- ¿Me especializo en minería o combate?

### 3. **Progresión Complementaria**
- Los rangos dan **base general** (visión nocturna, suerte, etc.)
- El árbol permite **especializarse** en un estilo de juego

---

## 📊 IMPACTO EN EL GAMEPLAY

### Antes del cambio:
```
EXPLORADOR: Speed I gratis → No necesitas PASO_LIGERO del árbol
VETERANO: Regeneration I + Haste I gratis → No necesitas esas ramas
ABSOLUTO: Speed III + Regen III + Health Boost 10 gratis → Árbol innecesario
```

### Después del cambio:
```
EXPLORADOR: Saturation I → Útil pero no rompe el árbol
VETERANO: Luck I → Única, no interfiere
ABSOLUTO: Luck IV + Strength II + Absorption III → Poderoso pero complementario
```

---

## 🔄 CAMBIOS ESPECÍFICOS POR RANGO

### EXPLORADOR (antes):
- ❌ SPEED I (eliminado)
- ✅ SATURATION I (añadido en su lugar)

### SOBREVIVIENTE (antes):
- ❌ SPEED I (eliminado)
- ❌ WATER_BREATHING (eliminado)
- ✅ DOLPHINS_GRACE I (añadido en su lugar)

### VETERANO (antes):
- ❌ SPEED I (eliminado)
- ❌ WATER_BREATHING (eliminado)
- ❌ REGENERATION I (eliminado)
- ❌ HASTE I (eliminado)
- ✅ LUCK I (añadido)

### LEYENDA (antes):
- ❌ SPEED II (eliminado)
- ❌ WATER_BREATHING (eliminado)
- ❌ REGENERATION I (eliminado)
- ❌ HASTE I (eliminado)
- ❌ HEALTH_BOOST 2 (eliminado)
- ✅ LUCK II (mejorado)

### MAESTRO (antes):
- ❌ SPEED II (eliminado)
- ❌ WATER_BREATHING (eliminado)
- ❌ REGENERATION II (eliminado)
- ❌ HASTE II (eliminado)
- ❌ HEALTH_BOOST 5 (eliminado)
- ✅ ABSORPTION I (añadido)

### TITAN (antes):
- ❌ SPEED II (eliminado)
- ❌ WATER_BREATHING (eliminado)
- ❌ REGENERATION II (eliminado)
- ❌ HASTE II (eliminado)
- ❌ HEALTH_BOOST 8 (eliminado)
- ❌ FIRE_RESISTANCE (eliminado)
- ❌ JUMP_BOOST I (eliminado)
- ✅ DOLPHINS_GRACE II (mejorado)
- ✅ HERO_OF_THE_VILLAGE I (añadido)
- ✅ ABSORPTION II (mejorado)

### ABSOLUTO (antes):
- ❌ SPEED III (eliminado)
- ❌ WATER_BREATHING (eliminado)
- ❌ REGENERATION III (eliminado)
- ❌ HASTE II (eliminado)
- ❌ HEALTH_BOOST 10 (eliminado)
- ❌ FIRE_RESISTANCE (eliminado)
- ❌ JUMP_BOOST II (eliminado)
- ✅ LUCK IV (mejorado a máximo)
- ✅ STRENGTH II (mejorado)
- ✅ HERO_OF_THE_VILLAGE II (mejorado)
- ✅ ABSORPTION III (mejorado)
- ✅ CONDUIT_POWER I (añadido)

---

## 🎮 EFECTOS ÚNICOS QUE SE MANTIENEN EN RANGOS

### Estos efectos NO existen en el árbol y son exclusivos:

1. **NIGHT_VISION** - Visión nocturna permanente (QoL esencial)
2. **SATURATION** - Nunca tienes hambre (QoL, diferente de ESTOMAGO_HIERRO)
3. **DOLPHINS_GRACE** - Nadas más rápido (único, no en árbol)
4. **LUCK** - Mejor loot y fortuna general (único, no en árbol)
5. **RESISTANCE** - Reducción de daño general (único, diferente de skills específicos)
6. **STRENGTH** - Más daño cuerpo a cuerpo (único, diferente de GOLPE_CERTERO)
7. **ABSORPTION** - Escudo dorado permanente (único, no en árbol)
8. **HERO_OF_THE_VILLAGE** - Descuentos con aldeanos (único, no en árbol)
9. **CONDUIT_POWER** - Poder del cóndor bajo agua (único, no en árbol)

---

## 📝 NOTAS TÉCNICAS

### Archivo modificado:
- `src/main/resources/recompensas.yml`
- Sección: `habilidades_por_rango`

### Compatibilidad:
- ✅ Los jugadores actuales NO pierden habilidades del árbol
- ✅ Los rangos existentes se actualizan automáticamente
- ✅ No requiere reset de datos

### Testing recomendado:
1. Verificar que los efectos de rangos se aplican correctamente
2. Confirmar que las habilidades del árbol funcionan independientemente
3. Revisar que no hay conflictos de efectos solapados

---

## 🎯 BENEFICIOS DEL CAMBIO

### Para los jugadores:
- ✅ Decisiones más significativas en el árbol
- ✅ Cada punto de skill tiene valor real
- ✅ Mayor personalización y builds únicos
- ✅ Sensación de progresión más clara

### Para el servidor:
- ✅ Sistema más balanceado y justo
- ✅ Menor confusión sobre fuentes de efectos
- ✅ Mejor separación de sistemas (rangos vs skills)
- ✅ Más longevidad y engagement con el árbol

---

## 📌 RESUMEN EJECUTIVO

**Antes**: Los rangos daban TODAS las habilidades importantes gratis → Árbol innecesario  
**Ahora**: Los rangos dan utilidades únicas → Árbol esencial para especializarse

**Resultado**: Sistema más equilibrado, decisiones más importantes, mayor rejugabilidad.

---

*Changelog generado automáticamente - v1.22.57*  
*Fecha: $(Get-Date -Format "dd/MM/yyyy HH:mm")*
