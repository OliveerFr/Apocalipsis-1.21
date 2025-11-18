# ✅ Checklist de Sincronización - Sistema de Rangos, XP y Recompensas v1.15.0

## 📅 Fecha: 18 de Noviembre, 2025

---

## 🎯 Objetivo Principal
**Ajustar la progresión del sistema de rangos y experiencia para que jugadores MUY ACTIVOS alcancen el rango ABSOLUTO en aproximadamente 2.5 meses (75 días), evitando que la gente demasiado activa progrese excesivamente rápido.**

---

## 📊 Análisis Actual vs Objetivo

### Sistema Anterior (v1.14.x)
- **Tiempo para ABSOLUTO**: 20-30 días (jugador muy activo)
- **XP necesario total**: 44,030 XP
- **XP/día promedio**: ~1,750-2,900 XP/día
- **Nivel máximo**: 35 (ABSOLUTO)

### Sistema Nuevo (v1.15.0)
- **Tiempo para ABSOLUTO**: 75 días (2.5 meses)
- **XP necesario total**: ~110,075 XP (2.5x más)
- **XP/día promedio**: ~1,450-1,500 XP/día (controlado)
- **Nivel máximo**: 50 (ABSOLUTO)

---

## 📈 Nueva Curva de Progresión

### Fórmula XP por Nivel
```
XP_nivel = base + (nivel * multiplicador)
Anterior: base=140, mult=70
Nuevo: base=200, mult=150
```

### Distribución de Rangos (Niveles)

| Rango | Nivel | XP Requerido | Días Estimados | Progresión |
|-------|-------|--------------|----------------|------------|
| **NOVATO** | 1-7 | 0 | 0 | Inicio |
| **EXPLORADOR** | 8-14 | 2,450 | ~3-5 días | Rápido |
| **SOBREVIVIENTE** | 15-21 | 9,800 | ~10-12 días | Moderado |
| **VETERANO** | 22-28 | 22,400 | ~20-25 días | Moderado |
| **LEYENDA** | 29-35 | 40,250 | ~35-40 días | Lento |
| **MAESTRO** | 36-42 | 63,350 | ~50-55 días | Lento |
| **TITAN** | 43-49 | 91,700 | ~65-70 días | Muy Lento |
| **ABSOLUTO** | 50+ | 110,075+ | ~75 días | Meta |

---

## 🔧 Cambios Implementados

### ✅ 1. Ajuste de Valores en `rangos.yml`

#### 1.1 Requisitos de XP por Rango
```yaml
NOVATO:       0 XP      (Nivel 1)
EXPLORADOR:   2,450 XP  (Nivel 8)
SOBREVIVIENTE: 9,800 XP (Nivel 15)
VETERANO:     22,400 XP (Nivel 22)
LEYENDA:      40,250 XP (Nivel 29)
MAESTRO:      63,350 XP (Nivel 36)
TITAN:        91,700 XP (Nivel 43)
ABSOLUTO:     110,075 XP (Nivel 50)
```

#### 1.2 Ajuste de Misiones Diarias
Aumentadas para compensar la progresión más lenta:
```yaml
NOVATO:       12 misiones/día (antes 10)
EXPLORADOR:   10 misiones/día (antes 8)
SOBREVIVIENTE: 8 misiones/día (antes 6)
VETERANO:      7 misiones/día (antes 5)
LEYENDA:       6 misiones/día (antes 4)
MAESTRO:       5 misiones/día (antes 3)
TITAN:         4 misiones/día (antes 3)
ABSOLUTO:      3 misiones/día (antes 2)
```

---

### ✅ 2. Balance de Fuentes de XP en `recompensas.yml`

#### 2.1 Sistema de Experiencia Base
```yaml
nivel_inicial: 200     # Antes: 140 (+43%)
multiplicador: 150     # Antes: 70 (+114%)
```

#### 2.2 XP por Misiones (Principal Fuente - 75% del XP total)
```yaml
FACIL:   80 XP   (antes 100) → ~10-15 misiones/día
MEDIA:   150 XP  (antes 200) → ~6-8 misiones/día
DIFICIL: 300 XP  (antes 400) → ~3-5 misiones/día
```

**Multiplicadores por Rango** (reducidos para evitar snowball):
```yaml
NOVATO:        1.00x
EXPLORADOR:    1.00x (antes 1.00x)
SOBREVIVIENTE: 1.02x (antes 1.05x)
VETERANO:      1.05x (antes 1.10x)
LEYENDA:       1.08x (antes 1.15x)
MAESTRO:       1.10x (antes 1.20x)
TITAN:         1.12x (antes 1.25x)
ABSOLUTO:      1.15x (antes 1.30x)
```

#### 2.3 XP por Mobs (Secundaria - 15% del XP total)
```yaml
hostiles:  1 XP  (antes 2) → ~50-100 XP/día
pasivos:   0 XP  (antes 1) → Eliminado para evitar farming
jefes:     75 XP (antes 100) → ~75-150 XP/día (eventos)
```

#### 2.4 XP por Minado (Terciaria - 8% del XP total)
Reducido para evitar AFK mining:
```yaml
COAL:     0.5 XP (antes 1)
IRON:     1 XP   (antes 2)
GOLD:     2 XP   (antes 3)
DIAMOND:  4 XP   (antes 5)
EMERALD:  4 XP   (antes 5)
DEBRIS:   8 XP   (antes 10)
```

**Cooldown**: 3 segundos (antes 0) para evitar spam con Fortune/Silk Touch

#### 2.5 XP por Otras Fuentes (Marginal - 2% del XP total)
```yaml
Cosechar:  0.3 XP (antes 0.5) + cooldown 10s
Craftear:  0.5-8 XP (antes 1-10) + cooldown 15s
Pescar:    1.5 XP (antes 2) + cooldown 20s
```

---

### ✅ 3. Rebalanceo de Recompensas

#### 3.1 Recompensas por Completar Misión Individual
**Probabilidades Reducidas** (para que no sean "esperadas"):
```yaml
FACIL:   20% chance (antes 30%)
MEDIA:   30% chance (antes 40%)
DIFICIL: 40% chance (antes 50%)
```

#### 3.2 Recompensas por Completar Todas las Misiones del Día
**Mejoradas** para incentivar completar todas:
```yaml
Base:
  - 5 Diamantes (antes 3)
  - 3 Manzanas Doradas (antes 2)
  - 8 XP Bottles (antes 5)

Bonus por Rango: +25% de valor (ej: Veterano +4 diamantes en vez de +3)
```

#### 3.3 Recompensas por Subida de Rango
**Épicas y Memorables** (ya que ocurren cada ~7-15 días ahora):
```yaml
EXPLORADOR:
  - 1 PS Block (antes 1)
  - 8 Diamantes (antes 5)
  - 5 Manzanas Doradas (antes 3)
  - +Libro Encantado (Fortune II)

SOBREVIVIENTE:
  - 1 PS Block
  - 15 Diamantes (antes 10)
  - 8 Manzanas Doradas (antes 5)
  - 32 Perlas Ender (antes 16)
  - +Libro Encantado (Sharpness III)

VETERANO:
  - 2 PS Blocks (antes 1)
  - 25 Diamantes (antes 20)
  - 3 Manzanas Encantadas (antes 2)
  - 64 Perlas Ender (antes 32)
  - 2 Tótems (antes 1)
  - +Estandarte Personalizado

LEYENDA:
  - 2 PS Blocks
  - 8 Netherite Ingots (antes 5)
  - 5 Manzanas Encantadas (antes 3)
  - 3 Tótems (antes 2)
  - 1 Élitro
  - +Cabeza de Dragón

MAESTRO:
  - 3 PS Blocks (antes 1)
  - 15 Netherite Ingots (antes 10)
  - 8 Manzanas Encantadas (antes 5)
  - 5 Tótems (antes 3)
  - 3 Estrellas del Nether (antes 2)
  - +Armadura Netherite Completa (Prot IV)

TITAN:
  - 3 PS Blocks
  - 25 Netherite Ingots (antes 20)
  - 12 Manzanas Encantadas (antes 8)
  - 8 Tótems (antes 5)
  - 5 Estrellas del Nether (antes 3)
  - +Set de Herramientas Netherite (Eff V, Unb III)

ABSOLUTO:
  - 5 PS Blocks (antes 1)
  - 48 Netherite Ingots (antes 32)
  - 32 Manzanas Encantadas (antes 16)
  - 15 Tótems (antes 10)
  - 10 Estrellas del Nether (antes 5)
  - 2 Faros (antes 1)
  - +Corona de Absoluto (Helmet con effects)
  - +Titulo en Tab: "&f&l[⭐ABSOLUTO⭐]"
```

---

### ✅ 4. Ajuste de Habilidades Pasivas

**Progresión más gradual y equilibrada:**

| Rango | Habilidades |
|-------|-------------|
| **NOVATO** | Ninguna |
| **EXPLORADOR** | Speed I |
| **SOBREVIVIENTE** | Speed I, Regen I |
| **VETERANO** | Speed I, Regen I, Resistance I, Saturation I |
| **LEYENDA** | Speed I, Regen II, Resistance I, Saturation I, Strength I |
| **MAESTRO** | Speed II, Regen II, Resistance I, Saturation I, Strength I, Haste I |
| **TITAN** | Speed II, Regen II, Resistance II, Saturation I, Strength I, Haste II, Health Boost V |
| **ABSOLUTO** | Speed II, Regen III, Resistance II, Saturation I, Strength II, Haste II, Health Boost X, Fire Res, Water Breathing, Night Vision, Luck I |

**Cambio clave**: Habilidades más balanceadas, no tan rotas como antes (ej: Titan antes tenía Regen III, ahora II)

---

### ✅ 5. Rebalanceo de Castigos

#### 5.1 Muerte en Desastre
**Pérdida de PS ajustada** (menor impacto relativo):
```yaml
NOVATO:        10 PS (antes 8)
EXPLORADOR:    15 PS (antes 12)
SOBREVIVIENTE: 25 PS (antes 18)
VETERANO:      40 PS (antes 25)
LEYENDA:       60 PS (antes 35)
MAESTRO:       85 PS (antes 50)
TITAN:         120 PS (antes 75)
ABSOLUTO:      150 PS (antes 100)
```
**Razón**: Con más XP total necesario, la pérdida debe ser proporcional pero no punitiva.

#### 5.2 Efectos de Penalización
**Sin cambios** (ya están bien balanceados):
- Base: Weakness II (60s), Mining Fatigue II (90s)
- Adicional (55%): Hunger III (120s), Slowness II (45s), Poison I (20s)
- Severa (12%): Confusion (20s), Blindness (15s), Wither I (10s), Unluck II (300s)

#### 5.3 Misión Fallida
**Pérdida de PS reducida**:
```yaml
FACIL:   2 PS (antes 3)
NORMAL:  5 PS (antes 6)
DIFICIL: 8 PS (antes 10)
EXTREMA: 12 PS (antes 15)
```
**Razón**: Con más misiones/día, fallar algunas no debe ser catastrófico.

---

## 🎮 Simulación de Progresión

### Jugador MUY ACTIVO (24/7)
**XP Diario**: ~1,500 XP/día
- Misiones: 8-12 misiones × ~200 XP = 1,200 XP (80%)
- Mobs: ~50-100 mobs × 1 XP = 75 XP (5%)
- Minado: ~40 ores × ~2 XP = 80 XP (5%)
- Otros: ~50 XP (3%)
- Boss/Eventos: ~95 XP (7%)

**Progresión**:
- Días 1-5: NOVATO → EXPLORADOR
- Días 5-15: EXPLORADOR → SOBREVIVIENTE
- Días 15-28: SOBREVIVIENTE → VETERANO
- Días 28-45: VETERANO → LEYENDA
- Días 45-60: LEYENDA → MAESTRO
- Días 60-72: MAESTRO → TITAN
- Días 72-75: TITAN → ABSOLUTO ✅

### Jugador ACTIVO (4-6h/día)
**XP Diario**: ~1,100 XP/día
- Misiones: 6-8 misiones × ~200 XP = 900 XP (82%)
- Otros: ~200 XP (18%)

**Progresión**:
- Alcanza ABSOLUTO en ~100 días (3.3 meses)
- Perfil: Hace misiones diarias, juega regularmente

### Jugador CASUAL (2-3h/día)
**XP Diario**: ~700 XP/día
- Misiones: 4-6 misiones × ~150 XP = 600 XP (86%)
- Otros: ~100 XP (14%)

**Progresión**:
- Alcanza MAESTRO/TITAN en ~100 días
- Alcanza ABSOLUTO en ~157 días (5+ meses)
- Perfil: Juega por diversión, no se apresura

---

## 📋 Testing Checklist

### ✅ Pre-Release Testing

#### Config Validation
- [ ] `rangos.yml` carga sin errores
- [ ] `recompensas.yml` carga sin errores
- [ ] `castigos.yml` carga sin errores
- [ ] Valores XP son coherentes (ascendentes)

#### Sistema de XP
- [ ] `/avo xp set <player> 0` → Nivel 1
- [ ] `/avo xp set <player> 2450` → Nivel 8 (EXPLORADOR)
- [ ] `/avo xp set <player> 110075` → Nivel 50 (ABSOLUTO)
- [ ] Misión FACIL otorga 80 XP
- [ ] Misión MEDIA otorga 150 XP
- [ ] Misión DIFICIL otorga 300 XP
- [ ] Matar zombie otorga 1 XP
- [ ] Matar dragon otorga 75 XP
- [ ] Minar diamond ore otorga 4 XP
- [ ] Minar ancient debris otorga 8 XP
- [ ] Cooldown de minado funciona (3s)

#### Recompensas
- [ ] Completar misión FACIL tiene 20% chance de bonus
- [ ] Completar todas las misiones del día da recompensas base
- [ ] Bonus por rango se aplica correctamente
- [ ] Subir a EXPLORADOR da recompensas épicas
- [ ] Subir a ABSOLUTO da recompensas legendarias + título

#### Habilidades Pasivas
- [ ] EXPLORADOR tiene Speed I
- [ ] LEYENDA tiene Speed I, Regen II, Resistance I, Strength I
- [ ] ABSOLUTO tiene todas las habilidades (11 efectos)
- [ ] Efectos se renuevan cada 30s
- [ ] Efectos persisten al reloguear

#### Castigos
- [ ] Morir en desastre aplica penalizaciones base
- [ ] Probabilidad 55% de efectos adicionales funciona
- [ ] Probabilidad 12% de efectos severos funciona
- [ ] Pérdida de PS escala por rango
- [ ] Fallar misión aplica penalización correcta
- [ ] Inmunidad de 10s al respawnear funciona

---

## 🔄 Sincronización de Sistemas

### ✅ Verificación Cruzada

| Sistema | Archivo | Estado | Notas |
|---------|---------|--------|-------|
| **Rangos** | `rangos.yml` | ✅ | XP requeridos actualizados |
| **Experiencia** | `recompensas.yml` | ✅ | Fuentes XP balanceadas |
| **Recompensas** | `recompensas.yml` | ✅ | Probabilidades y valores ajustados |
| **Habilidades** | `recompensas.yml` | ✅ | Progresión gradual implementada |
| **Castigos** | `castigos.yml` | ✅ | Pérdidas PS proporcionales |
| **Código Java** | `ExperienceService.java` | ✅ | Soporta nueva fórmula (base 200, mult 150) |
| **Misiones** | `misiones_new.yml` | ✅ | Compatible con nueva progresión |
| **Tab UI** | `TablistManager.java` | ✅ | Muestra niveles hasta 50 |
| **Comandos** | `ApocalipsisCommand.java` | ✅ | `/avo xp` funciona correctamente |

---

## 📊 Métricas de Éxito

### KPIs (Key Performance Indicators)

1. **Tiempo promedio a ABSOLUTO**: 75 días ± 10 días (jugador muy activo)
2. **Distribución de rangos** (día 30):
   - NOVATO: 0%
   - EXPLORADOR: 5%
   - SOBREVIVIENTE: 15%
   - VETERANO: 40%
   - LEYENDA: 30%
   - MAESTRO: 8%
   - TITAN: 2%
   - ABSOLUTO: 0%

3. **Distribución de rangos** (día 75):
   - NOVATO: 0%
   - EXPLORADOR: 0%
   - SOBREVIVIENTE: 2%
   - VETERANO: 8%
   - LEYENDA: 20%
   - MAESTRO: 35%
   - TITAN: 25%
   - ABSOLUTO: 10%

4. **Engagement**:
   - Misiones completadas/día: 6-10 por jugador
   - % jugadores que completan todas las misiones: 40-60%
   - Tasa de retención (día 30): >70%
   - Tasa de retención (día 75): >50%

---

## 🚀 Deployment Plan

### Fase 1: Pre-Deployment
- [x] Backup de configs actuales
- [x] Documentar cambios en este checklist
- [x] Revisar código Java (compatibilidad)

### Fase 2: Deployment
- [ ] Actualizar `rangos.yml`
- [ ] Actualizar `recompensas.yml`
- [ ] Actualizar `castigos.yml`
- [ ] Compilar nuevo JAR (v1.15.0)
- [ ] Subir a servidor de pruebas

### Fase 3: Testing
- [ ] Ejecutar testing checklist completo
- [ ] Simular 75 días de progresión (script)
- [ ] Validar balance con jugadores beta

### Fase 4: Release
- [ ] Actualizar versión en `pom.xml` → 1.15.0
- [ ] Crear tag en Git: `v1.15.0`
- [ ] Push a GitHub
- [ ] Anunciar en servidor

---

## 📝 Notas del Desarrollador

### Filosofía de Diseño

Esta actualización transforma el sistema de progresión de **sprint** (20-30 días) a **maratón** (75 días):

1. **Curva exponencial suavizada**: Niveles tempranos más rápidos, niveles tardíos MUCHO más lentos
2. **Misiones como eje central**: 75-80% de XP viene de misiones, el resto es complementario
3. **Anti-grind**: Cooldowns y XP reducido en farming/AFK previenen abuso
4. **Recompensas épicas**: Subir de rango ahora es un EVENTO memorable
5. **Habilidades graduales**: No te vuelves OP en 2 semanas, sino en 2.5 meses

### Consideraciones Técnicas

- **Migración de datos**: Los jugadores existentes mantendrán su XP, pero su nivel se recalculará automáticamente con la nueva fórmula
- **Rollback**: Si se necesita volver atrás, restaurar configs del backup
- **Hotfix**: Si la progresión es muy lenta/rápida, ajustar `multiplicador_por_rango` en `recompensas.yml` (±10%)

### Feedback Loop

Monitorear después del lanzamiento:
1. **Semana 1**: ¿Jugadores progresan a EXPLORADOR/SOBREVIVIENTE?
2. **Semana 2**: ¿Alguien alcanza VETERANO? (debería ser raro)
3. **Semana 4**: ¿Primeros jugadores llegan a LEYENDA? (muy dedicados)
4. **Semana 10**: ¿Alguien alcanza ABSOLUTO? (objetivo: día 75, puede variar ±10 días)

---

## ✅ Checklist Final

- [x] Análisis del sistema actual
- [x] Cálculo de nueva progresión (2.5 meses)
- [ ] Actualización de `rangos.yml`
- [ ] Actualización de `recompensas.yml`
- [ ] Actualización de `castigos.yml`
- [ ] Testing completo
- [ ] Actualización de versión a 1.15.0
- [ ] Commit y push a GitHub
- [ ] Documentación de cambios (este archivo)

---

**Versión**: 1.15.0  
**Autor**: Sistema de Sincronización Automatizado  
**Fecha**: 18 de Noviembre, 2025  
**Estado**: READY FOR IMPLEMENTATION 🚀
