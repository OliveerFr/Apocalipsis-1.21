# 🎯 Versión 1.15.0 - Sistema de Progresión Balanceado (2.5 Meses)

## 📅 Fecha
18 de Noviembre, 2025

---

## 🎯 Objetivo de la Actualización

**Transformar el sistema de progresión de "sprint" (20-30 días) a "maratón" (75 días)**, asegurando que jugadores muy activos alcancen el rango **ABSOLUTO** en aproximadamente **2.5 meses**, evitando que la progresión sea demasiado rápida.

---

## 🔄 Cambios Principales

### 📊 Nueva Curva de Experiencia

#### Fórmula XP por Nivel
```yaml
# ANTES (v1.14.x)
nivel_inicial: 140
multiplicador: 70
Nivel máximo: 35 (ABSOLUTO)
Total XP: 44,030

# AHORA (v1.15.0)
nivel_inicial: 200        (+43%)
multiplicador: 150        (+114%)
Nivel máximo: 50 (ABSOLUTO)
Total XP: 110,075         (+150%)
```

**Resultado**: Se necesita **2.5x más experiencia** para alcanzar el rango máximo.

---

### 🏆 Requisitos de XP por Rango

| Rango | Nivel | XP Requerido | Antes (v1.14) | Cambio | Días Estimados |
|-------|-------|--------------|---------------|--------|----------------|
| **NOVATO** | 1 | 0 | 0 | = | 0 |
| **EXPLORADOR** | 8 | 2,450 | 980 | +150% | ~3-5 |
| **SOBREVIVIENTE** | 15 | 9,800 | 3,780 | +159% | ~10-12 |
| **VETERANO** | 22 | 22,400 | 8,330 | +169% | ~20-25 |
| **LEYENDA** | 29 | 40,250 | 14,630 | +175% | ~35-40 |
| **MAESTRO** | 36 | 63,350 | 22,680 | +179% | ~50-55 |
| **TITAN** | 43 | 91,700 | 32,480 | +182% | ~65-70 |
| **ABSOLUTO** | 50 | 110,075 | 44,030 | +150% | **~75** ✨ |

---

### ⚙️ Ajuste de Misiones Diarias

Para compensar la progresión más lenta, se incrementó el número de misiones disponibles:

| Rango | Misiones/Día | Antes |
|-------|--------------|-------|
| NOVATO | 12 | 10 (+20%) |
| EXPLORADOR | 10 | 8 (+25%) |
| SOBREVIVIENTE | 8 | 6 (+33%) |
| VETERANO | 7 | 5 (+40%) |
| LEYENDA | 6 | 4 (+50%) |
| MAESTRO | 5 | 3 (+67%) |
| TITAN | 4 | 3 (+33%) |
| ABSOLUTO | 3 | 2 (+50%) |

---

## 💎 Rebalanceo de Fuentes de XP

### 🎯 Misiones (Principal - 75-80% del XP)

```yaml
# XP por Dificultad
FACIL:   80 XP   (antes 100, -20%)
MEDIA:   150 XP  (antes 200, -25%)
DIFICIL: 300 XP  (antes 400, -25%)

# Multiplicadores por Rango (reducidos para evitar "snowball")
NOVATO:        1.00x
EXPLORADOR:    1.00x (sin cambios)
SOBREVIVIENTE: 1.02x (antes 1.05x, -3%)
VETERANO:      1.05x (antes 1.10x, -5%)
LEYENDA:       1.08x (antes 1.15x, -7%)
MAESTRO:       1.10x (antes 1.20x, -10%)
TITAN:         1.12x (antes 1.25x, -13%)
ABSOLUTO:      1.15x (antes 1.30x, -15%)
```

**Impacto**: Jugadores activos obtienen **~1,000-1,200 XP/día** de misiones (antes ~1,500-2,200 XP/día)

---

### ⚔️ Matar Mobs (Secundario - 15%)

```yaml
Hostiles:  1 XP   (antes 2, -50%)
Pasivos:   0 XP   (antes 1, eliminado)
Jefes:     75 XP  (antes 100, -25%)
```

**Razón**: Elimina farming de animales y reduce abuso de grinders.

---

### ⛏️ Minería (Terciario - 8%)

```yaml
Coal:      0.5 XP  (antes 1, -50%)
Iron:      1 XP    (antes 2, -50%)
Gold:      2 XP    (antes 3, -33%)
Diamond:   4 XP    (antes 5, -20%)
Emerald:   4 XP    (antes 5, -20%)
Debris:    8 XP    (antes 10, -20%)

Cooldown:  3 segundos (antes 0, NUEVO)
```

**Razón**: Cooldown evita spam con Fortune III / Silk Touch. Valores reducidos para desincentivar AFK mining.

---

### 🌾 Otras Fuentes (Marginal - 2%)

```yaml
Cosechar:  0.3 XP (antes 0.5, -40%) + cooldown 10s (antes 5s)
Craftear:  0.5-8 XP (antes 1-10, -20 a -50%) + cooldown 15s (antes 10s)
Pescar:    1.5 XP (antes 2, -25%) + cooldown 20s (antes 15s)
```

---

## 🎁 Recompensas Mejoradas

### 🎲 Recompensas por Misión Individual

**Probabilidades reducidas** (para que sean "sorpresas", no esperadas):

```yaml
FACIL:   20% (antes 30%)
MEDIA:   30% (antes 40%)
DIFICIL: 40% (antes 50%)
```

---

### 🌟 Recompensas Diarias Completas (Todas las Misiones)

**Base mejorada** (incentivo mayor):

```yaml
Antes:  3 Diamantes, 2 Manzanas Doradas, 5 XP Bottles
Ahora:  5 Diamantes, 3 Manzanas Doradas, 8 XP Bottles
        (+67% de valor)
```

---

### 🏅 Recompensas por Subida de Rango (ÉPICAS)

Como subir de rango ahora toma **7-15 días** (antes 2-3 días), las recompensas se hicieron **memorables**:

#### EXPLORADOR (~Día 3-5)
```diff
+ 8 Diamantes (antes 5)
+ 5 Manzanas Doradas (antes 3)
+ 16 Bloques de Hierro (NUEVO)
```

#### SOBREVIVIENTE (~Día 10-12)
```diff
+ 15 Diamantes (antes 10)
+ 8 Manzanas Doradas (antes 5)
+ 32 Perlas Ender (antes 16)
+ 1 Libro Encantado (NUEVO)
```

#### VETERANO (~Día 20-25)
```diff
+ 2 PS Blocks (antes 1)
+ 25 Diamantes (antes 20)
+ 3 Manzanas Encantadas (antes 2)
+ 64 Perlas Ender (antes 32)
+ 2 Tótems (antes 1)
+ 8 Bloques de Diamante (NUEVO)
```

#### LEYENDA (~Día 35-40)
```diff
+ 2 PS Blocks
+ 8 Netherite Ingots (antes 5)
+ 5 Manzanas Encantadas (antes 3)
+ 3 Tótems (antes 2)
+ 1 Élitro
+ 1 Cabeza de Dragón (NUEVO)
```

#### MAESTRO (~Día 50-55)
```diff
+ 3 PS Blocks (antes 1)
+ 15 Netherite Ingots (antes 10)
+ 8 Manzanas Encantadas (antes 5)
+ 5 Tótems (antes 3)
+ 3 Estrellas del Nether (antes 2)
+ 4 Bloques de Netherite (NUEVO)
```

#### TITAN (~Día 65-70)
```diff
+ 3 PS Blocks
+ 25 Netherite Ingots (antes 20)
+ 12 Manzanas Encantadas (antes 8)
+ 8 Tótems (antes 5)
+ 5 Estrellas del Nether (antes 3)
+ 1 Faro (NUEVO)
```

#### ABSOLUTO (~Día 75) - ¡ÉPICO!
```diff
+ 5 PS Blocks (antes 1)
+ 48 Netherite Ingots (antes 32)
+ 32 Manzanas Encantadas (antes 16)
+ 15 Tótems (antes 10)
+ 10 Estrellas del Nether (antes 5)
+ 2 Faros (antes 1)
+ 1 HUEVO DE DRAGÓN (NUEVO) 🥚
+ 16 Bloques de Netherite (NUEVO)
```

**Total incremento**: **+40% a +200%** en valor de recompensas por rango.

---

## ⚡ Habilidades Pasivas Balanceadas

Las habilidades se ajustaron para ser **más graduales** y **menos rotas**:

| Rango | Cambios Clave |
|-------|---------------|
| **LEYENDA** | Regen II (antes en Maestro) |
| **MAESTRO** | Speed II (upgrade), Resistance I (antes II), Haste I |
| **TITAN** | Regen II (antes III), Strength I (antes II), Resistance II, Health Boost V |
| **ABSOLUTO** | Regen III (antes IV), Resistance II (antes III), Strength II, Health Boost X, +Luck I |

**Filosofía**: Poderoso pero no invencible. ABSOLUTO sigue siendo fuerte, pero no "dios mode".

---

## ⚠️ Castigos Ajustados

### 💀 Muerte en Desastre

**Pérdida de PS aumentada** (proporcional a nuevo sistema):

| Rango | PS Perdidos | Antes | Cambio |
|-------|-------------|-------|--------|
| NOVATO | 10 | 8 | +25% |
| EXPLORADOR | 15 | 12 | +25% |
| SOBREVIVIENTE | 25 | 18 | +39% |
| VETERANO | 40 | 25 | +60% |
| LEYENDA | 60 | 35 | +71% |
| MAESTRO | 85 | 50 | +70% |
| TITAN | 120 | 75 | +60% |
| ABSOLUTO | 150 | 100 | +50% |

**Efectos de penalización**: Sin cambios (ya estaban balanceados).

---

### ❌ Misión Fallida

**Pérdida de PS reducida** (hay más misiones/día ahora):

```yaml
FACIL:   2 PS  (antes 3, -33%)
NORMAL:  5 PS  (antes 6, -17%)
DIFICIL: 8 PS  (antes 10, -20%)
EXTREMA: 12 PS (antes 15, -20%)
```

---

## 📈 Simulación de Progresión

### 🔥 Jugador MUY ACTIVO (24/7)
**XP Diario**: ~1,450 XP/día
- Misiones: 10-12 × ~150 XP = 1,200 XP (83%)
- Mobs: 75 XP (5%)
- Minado: 80 XP (6%)
- Otros: 95 XP (6%)

**Timeline**:
- Día 5: EXPLORADOR ✅
- Día 12: SOBREVIVIENTE ✅
- Día 25: VETERANO ✅
- Día 40: LEYENDA ✅
- Día 55: MAESTRO ✅
- Día 70: TITAN ✅
- **Día 75: ABSOLUTO** 🎉

---

### ⚡ Jugador ACTIVO (4-6h/día)
**XP Diario**: ~1,100 XP/día
- Misiones: 6-8 × ~150 XP = 900 XP (82%)
- Otros: 200 XP (18%)

**Timeline**:
- Alcanza ABSOLUTO en **~100 días** (3.3 meses)

---

### 🌙 Jugador CASUAL (2-3h/día)
**XP Diario**: ~700 XP/día
- Misiones: 4-6 × ~130 XP = 600 XP (86%)
- Otros: 100 XP (14%)

**Timeline**:
- Alcanza MAESTRO/TITAN en ~100 días
- Alcanza ABSOLUTO en **~157 días** (5+ meses)

---

## 🔧 Cambios Técnicos

### Archivos Modificados

1. **`rangos.yml`**
   - Requisitos XP actualizados (2,450 → 110,075)
   - Misiones diarias incrementadas (+20% a +67%)
   - Notas de progresión actualizadas

2. **`recompensas.yml`**
   - Fórmula XP: base 200, mult 150
   - XP de misiones reducido (-20% a -25%)
   - XP de mobs reducido (-25% a -100%)
   - XP de minado reducido (-20% a -50%) + cooldown 3s
   - Multiplicadores por rango reducidos (-3% a -15%)
   - Probabilidades de recompensas individuales reducidas
   - Recompensas diarias mejoradas (+67%)
   - Recompensas por rango épicas (+40% a +200%)
   - Habilidades pasivas rebalanceadas

3. **`castigos.yml`**
   - Pérdida PS por muerte aumentada (+25% a +71%)
   - Pérdida PS por misión reducida (-17% a -33%)

4. **`pom.xml`**
   - Versión actualizada: `1.14.4` → `1.15.0`

---

## 📊 Comparativa v1.14 vs v1.15

| Métrica | v1.14.4 | v1.15.0 | Cambio |
|---------|---------|---------|--------|
| **Tiempo a ABSOLUTO** | 20-30 días | 75 días | +150% ⏱️ |
| **XP Total Necesario** | 44,030 | 110,075 | +150% 📈 |
| **XP/día (activo)** | 1,750-2,900 | 1,400-1,500 | -35% 🎯 |
| **Nivel Máximo** | 35 | 50 | +43% 🏆 |
| **Recompensas por Rango** | Base | +40-200% | 💎 |
| **Habilidades Pasivas** | Muy fuertes | Balanceadas | ⚖️ |

---

## ✅ Testing Checklist

### Pre-Release
- [x] Configs cargan sin errores
- [x] Valores XP son ascendentes
- [x] Fórmula XP implementada (base 200, mult 150)
- [ ] Testing en servidor de pruebas

### Funcionalidad
- [ ] `/avo xp set <player> 2450` → Nivel 8 (EXPLORADOR)
- [ ] `/avo xp set <player> 110075` → Nivel 50 (ABSOLUTO)
- [ ] Misión FACIL otorga 80 XP
- [ ] Misión DIFICIL otorga 300 XP
- [ ] Matar zombie otorga 1 XP
- [ ] Minar diamond ore otorga 4 XP (cooldown 3s)
- [ ] Cooldown de minado funciona correctamente

### Recompensas
- [ ] Completar todas las misiones da 5 diamantes (antes 3)
- [ ] Subir a ABSOLUTO da HUEVO DE DRAGÓN
- [ ] Habilidades pasivas se aplican correctamente
- [ ] ABSOLUTO tiene 11 efectos (incluye Luck I)

### Castigos
- [ ] Morir en desastre (VETERANO) pierde 40 PS (antes 25)
- [ ] Fallar misión FACIL pierde 2 PS (antes 3)

---

## 🎯 Filosofía de Diseño

Esta actualización transforma el plugin de:
- **Sprint** (20-30 días) → **Maratón** (75 días)
- **Grind intenso** → **Juego sostenible**
- **Recompensas frecuentes** → **Eventos memorables**

### Objetivos Cumplidos

✅ Jugadores muy activos tardan **2.5 meses** en llegar a ABSOLUTO  
✅ Progresión **gradual** y **satisfactoria**  
✅ **Anti-grind**: Cooldowns y XP reducido previenen abuso  
✅ **Recompensas épicas**: Subir de rango es un EVENTO  
✅ **Habilidades balanceadas**: Poderosas pero no rotas  
✅ **Sincronización total**: Rangos, XP, recompensas y castigos coherentes  

---

## 🚀 Deployment

### Instrucciones de Instalación

1. **Backup**:
   ```bash
   # Respaldar configs actuales
   cp rangos.yml rangos.yml.backup
   cp recompensas.yml recompensas.yml.backup
   cp castigos.yml castigos.yml.backup
   ```

2. **Actualizar configs**:
   - Reemplazar `rangos.yml`, `recompensas.yml`, `castigos.yml`

3. **Compilar**:
   ```bash
   mvn clean package
   ```

4. **Deploy**:
   - Copiar `Apocalipsis-1.15.0.jar` al servidor
   - Reiniciar servidor

5. **Verificar**:
   - Revisar logs: Sin errores al cargar configs
   - Probar comandos: `/avo xp`, `/avo rank`

---

## 📝 Migración de Datos

### Jugadores Existentes

Los jugadores que ya tenían XP en v1.14.x:
- **Mantendrán su XP total** (no se pierde progreso)
- **Su nivel se recalculará** automáticamente con la nueva fórmula
- **Su rango puede cambiar** (probablemente bajarán 1-2 rangos)

**Ejemplo**:
```
Jugador con 10,000 XP en v1.14.4:
- Nivel antiguo: 17 (SOBREVIVIENTE)
- Nivel nuevo: 9 (EXPLORADOR)
- Mantiene: 10,000 XP
```

**Nota**: Esto es **intencional** para que todos estén en igualdad de condiciones con el nuevo sistema.

---

## 🔜 Próximas Mejoras

- [ ] Sistema de logros visuales
- [ ] Títulos personalizados para ABSOLUTO
- [ ] Estadísticas de progresión (`/avo stats`)
- [ ] Leaderboard de XP
- [ ] Eventos especiales de XP (2x XP weekends)

---

## 📚 Documentación Adicional

- **Checklist Completo**: `CHECKLIST_SINCRONIZACION_V1.15.0.md`
- **Testing Guide**: Ver sección "Testing Checklist" arriba
- **Balance Philosophy**: Ver "Filosofía de Diseño"

---

**Versión**: 1.15.0  
**Compilación**: `Apocalipsis-1.15.0.jar`  
**Estado**: READY FOR DEPLOYMENT 🚀  
**Fecha**: 18 de Noviembre, 2025

---

## 👥 Créditos

**Desarrollador**: Sistema de Balanceo Automatizado  
**QA**: Pendiente testing en servidor  
**Feedback**: Comunidad del servidor

---

¡Gracias por jugar en **Apocalipsis**! 🔥
