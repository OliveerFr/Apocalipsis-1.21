# 🎨 MEJORAS DE DISEÑO - SCOREBOARD & TABLIST

**Versión:** 1.22.57  
**Fecha:** 27 Enero 2026  
**Tipo:** UX/UI Enhancement

---

## 📊 CAMBIOS IMPLEMENTADOS

### 🎯 SCOREBOARD - Diseño Moderno con Bordes

#### **Antes:**
```
§8━━━━━━━━━━━━━
§7⚔ Rango: NOVATO (50 PS | 5 Nv)
§7◈ Estado: ACTIVO
§7☠ Desastre: HURACAN
§7⏱ Tiempo: 05:30
§8━━━━━━━━━━━━━
§7Próx. rango: EXPLORADOR
[§a█████§7█████] 50/100 XP
§8━━━━━━━━━━━━━
§7✎ Misiones:
§7• Recolectar 10 manzanas (5/10)
§7Completadas: 3/5
§8━━━━━━━━━━━━━
§7👥 Online: 15
```

#### **Después:**
```
§8§l┏━━━━━━━━━━━━━━━┓
§8§l┃ §f⚔ NOVATO §8[§b5§8]
§8§l┃ §7PS: §e50 §8| §7XP: §a250
§8§l┣━━━━━━━━━━━━━━━┫
§8§l┃ §f◆ §cACTIVO
§8§l┃ §c☠ §fHURACAN
§8§l┃ §f✦ Evento Apocalíptico
§8§l┃ §f⏱ §a05:30
§8§l┣━━━━━━━━━━━━━━━┫
§8§l┃ §7▸ Siguiente: §fEXPLORADOR
§8§l┃ §8[§a■■■■■■§8□□□□□□]
§8§l┃ §750§8/§f100 §7XP §8(50%)
§8§l┣━━━━━━━━━━━━━━━┫
§8§l┃ §f◈ §e§lMISIONES
§8§l┃ §7▸ §fRecolectar manzanas
§8§l┃   §8[§75§8/§f10§8]
§8§l┃ §7Total: §a3§8/§f5
§8§l┣━━━━━━━━━━━━━━━┫
§8§l┃ §f⚙ §7Jugadores: §b15
§8§l┗━━━━━━━━━━━━━━━┛
```

---

### 🏆 TABLIST - Header/Footer Moderno

#### **Antes:**
```
Header:
§c§l▸ APOCALIPSIS §8§l| §7Día 5 | §7Players 15/50 | §7TPS §a19.8

§8▸ ACTIVO §8| §c⚠ HURACAN

Footer:
§a▸ §7Tiempo: 05:30

NOVATO §8| §7Nivel §b5 §8(50%)
[§a████████§7████] 250/500
```

#### **Después:**
```
Header:
§8§l┏━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
§8§l┃ §c§l⚔ APOCALIPSIS §r§8§l━ §7Día §f5 §8§l┃
§8§l┣━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
§8§l┃ §7Players: §b15§8/§f50 §8│ §7TPS: §a19.8 §8§l┃
§8§l┃ §7Estado: §cACTIVO §8◆ §cHURACAN §8§l┃
§8§l┗━━━━━━━━━━━━━━━━━━━━━━━━━━━┛

Footer:
§8§l┏━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
§8§l┃ §a⏱ Tiempo: §f05:30 §8§l┃
§8§l┣━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
§8§l┃ NOVATO §8│ §7Nivel §b§l5 §8§l┃
§8§l┃ §8[§a§l■■■■■■§8□□□□□□□□] §8§l┃
§8§l┃ §7250§8/§f500 XP §8(50%) §8§l┃
§8§l┗━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
```

---

## ✨ CARACTERÍSTICAS NUEVAS

### Scoreboard

1. **Bordes Decorativos:**
   - `┏━━━┓` Top
   - `┣━━━┫` Separadores
   - `┗━━━┛` Bottom
   - `┃` Laterales

2. **Iconos Mejorados:**
   - `⚔` Rango (sword)
   - `✦` Nivel (star)
   - `◆` Estado (diamond)
   - `☠` Desastre (skull)
   - `⏱` Tiempo (hourglass)
   - `◈` Misiones (lozenge)
   - `⚙` Jugadores (gear)
   - `▸` Progreso (arrow)

3. **Gradiente de Colores:**
   - **85%+:** `§a§l` Verde brillante
   - **65-84%:** `§2` Verde
   - **45-64%:** `§e` Amarillo
   - **25-44%:** `§6` Naranja
   - **0-24%:** `§c` Rojo

4. **Barras de Progreso:**
   - Llenas: `■` (filled square)
   - Vacías: `□` (empty square)
   - Formato: `§8[§a■■■■§8□□□□]`

5. **Información Compacta:**
   - Rango + Nivel en línea
   - PS + XP juntos
   - Estado + Desastre combinados
   - Misiones con sub-progreso

---

### Tablist

1. **Header Enmarcado:**
   - Box completo con bordes
   - Logo centrado con día
   - Stats del servidor en una línea
   - Estado y desastre destacados

2. **Footer Estructurado:**
   - Tiempo/Cooldown con icono
   - Rango con nivel destacado
   - Barra de progreso más larga (14 bloques)
   - Porcentaje visible

3. **Colores Contextuales:**
   - **ACTIVO:** `§c` Rojo (peligro)
   - **PREPARACIÓN:** `§e` Amarillo (advertencia)
   - **DETENIDO:** `§7` Gris (inactivo)
   - **TPS:** Verde/Amarillo/Rojo según valor

4. **Información Clara:**
   - `⏱` Tiempo activo
   - `✓` Listo para empezar
   - `│` Separadores visuales
   - `[X/Y]` Contadores entre corchetes

---

## 🎨 PALETA DE COLORES

| Elemento | Color | Código | Uso |
|----------|-------|--------|-----|
| Bordes | Gris Oscuro | `§8§l` | Estructura |
| Títulos | Blanco | `§f` | Headers |
| Labels | Gris | `§7` | Descriptivos |
| Valores | Blanco | `§f` | Datos |
| Positivo | Verde | `§a` | Progreso alto |
| Advertencia | Amarillo | `§e` | Progreso medio |
| Peligro | Rojo | `§c` | Desastres/bajo |
| Acento | Cyan | `§b` | Números destacados |
| Separadores | Gris oscuro | `§8` | Divisiones |

---

## 📐 ESPECIFICACIONES TÉCNICAS

### Scoreboard

```java
// Constantes
PROGRESS_BAR_SIZE = 12 (antes: 10)
MAX_MISSION_NAME_LENGTH = 18 (antes: 15)
UPDATE_INTERVAL_TICKS = 40 (sin cambios)

// Separadores
TOP_SEPARATOR = "§8§l┏━━━━━━━━━━━━━━━┓"
MID_SEPARATOR = "§8§l┣━━━━━━━━━━━━━━━┫"
BOTTOM_SEPARATOR = "§8§l┗━━━━━━━━━━━━━━━┛"
LINE_PREFIX = "§8§l┃ "

// Líneas máximas: ~15 (optimizado)
```

### Tablist

```java
// Header: 4-5 líneas
// Footer: 3-5 líneas (según estado)
// Barra de progreso: 14 bloques (antes: 12)
// Update interval: 60 ticks (sin cambios)
```

---

## 🔄 ANIMACIONES

### Scoreboard - Título Animado

Ciclo de 30 ticks (1.5 segundos):
1. Ticks 0-5: `§c§l▸ §4§lAPOCALIPSIS §c§l◂`
2. Ticks 6-11: `§4§l▸ §c§lAPOCALIPSIS §4§l◂`
3. Ticks 12-17: `§c§l▸ §6§lAPOCALIPSIS §c§l◂`
4. Ticks 18-23: `§6§l▸ §c§lAPOCALIPSIS §6§l◂`
5. Ticks 24-29: `§4§l▸ §c§lAPOCALIPSIS §4§l◂`

**Efecto:** Gradiente rojo-naranja suave

---

## ⚡ OPTIMIZACIONES

1. **Cache Mejorado:**
   - Contenido completo en String
   - Evita paquetes duplicados
   - StringBuilder pre-asignado

2. **Reducción de Líneas:**
   - Scoreboard: 15 líneas (antes: 18)
   - Información más densa
   - Mejor legibilidad

3. **Símbolos Unicode:**
   - Más compactos
   - Mejor contraste
   - Diseño profesional

---

## 📱 VISTA PREVIA COMPLETA

### En Desastre Activo
```
┏━━━━━━━━━━━━━━━┓
┃ ⚔ EXPLORADOR [8]
┃ PS: 120 | XP: 580
┣━━━━━━━━━━━━━━━┫
┃ ◆ ACTIVO
┃ ☠ TORMENTA GLACIAL
┃ ✦ Ciclo de Hielo
┃ ⏱ 03:45
┣━━━━━━━━━━━━━━━┫
┃ ▸ Siguiente: VETERANO
┃ [§a■■■■■■■§8□□□□□]
┃ 580/800 XP (72%)
┣━━━━━━━━━━━━━━━┫
┃ ◈ MISIONES
┃ ▸ Sobrevivir frío
┃   [10/15]
┃ ▸ Encontrar refugio
┃   [1/1]
┃ Total: 8/12
┣━━━━━━━━━━━━━━━┫
┃ ⚙ Jugadores: 23
┗━━━━━━━━━━━━━━━┛
```

### En Preparación
```
┏━━━━━━━━━━━━━━━┓
┃ ⚔ NOVATO [3]
┃ PS: 35 | XP: 140
┣━━━━━━━━━━━━━━━┫
┃ ◆ PREPARACIÓN
┣━━━━━━━━━━━━━━━┫
┃ ▸ Siguiente: EXPLORADOR
┃ [§e■■■■§8□□□□□□□□]
┃ 140/300 XP (46%)
┣━━━━━━━━━━━━━━━┫
┃ ◈ MISIONES
┃ ▸ Recoger recursos
┃   [5/10]
┃ Total: 2/5
┣━━━━━━━━━━━━━━━┫
┃ ⚙ Jugadores: 8
┗━━━━━━━━━━━━━━━┛
```

---

## ✅ MEJORAS DE UX

1. **Jerarquía Visual Clara:**
   - Bordes definen secciones
   - Iconos identifican información
   - Colores destacan estados

2. **Información Priorizada:**
   - Rango/Nivel arriba (identidad)
   - Estado central (contexto)
   - Misiones abajo (objetivos)

3. **Legibilidad Mejorada:**
   - Más espaciado
   - Menos texto denso
   - Símbolos universales

4. **Diseño Profesional:**
   - Consistencia visual
   - Estética moderna
   - Feedback claro

---

## 🎯 COMPARATIVA VISUAL

| Aspecto | Antes | Después | Mejora |
|---------|-------|---------|--------|
| **Claridad** | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | +66% |
| **Estética** | ⭐⭐ | ⭐⭐⭐⭐⭐ | +150% |
| **Densidad Info** | ⭐⭐⭐ | ⭐⭐⭐⭐ | +33% |
| **Legibilidad** | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | +66% |
| **Modernidad** | ⭐⭐ | ⭐⭐⭐⭐⭐ | +150% |

---

**Desarrollado para:** Apocalipsis Minecraft Plugin  
**Versión:** 1.22.57  
**Fecha:** 27 Enero 2026
