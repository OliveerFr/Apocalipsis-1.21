# CHANGELOG v1.22.64 - Rebalanceo Completo de Habilidades

## 🎯 Objetivos del Rebalanceo

1. **Eliminar duplicaciones** entre rangos y skill tree
2. **Remover habilidades OP** que rompen mecánicas de Minecraft
3. **Crear progresión balanceada** más gradual y significativa
4. **Mejorar experiencia de usuario** en el menú de skills

## ✅ Cambios Principales

### 1. **Sistema de Rangos Completamente Rebalanceado** ⚖️

#### ❌ Habilidades REMOVIDAS:
- **Night Vision** → Ahora COMPRABLE en skill tree (Exploración, 400 XP)
- **Dolphin's Grace** → COMPRABLE como "Nadador" (Supervivencia, 4000 XP)
- **Saturation** → ELIMINADA (muy OP, rompe mecánica de hambre)
- **Hero of the Village** → ELIMINADA (muy situacional, poco útil)
- **Conduit Power** → ELIMINADA (redundante con Water Breathing)

#### ✅ Habilidades AÑADIDAS:
- **Water Breathing** → Exploración acuática ilimitada
- **Speed** → Movilidad general mejorada
- **Fire Resistance** → Seguridad en el Nether

#### 🎯 Nueva Progresión de Rangos:

| Rango | Habilidades | Beneficio Principal |
|-------|-------------|---------------------|
| **NOVATO** | *Ninguna* | Punto de partida limpio |
| **EXPLORADOR** | Luck I | Mejor loot (+10% drops) |
| **SOBREVIVIENTE** | Luck I, Water Breathing | Exploración acuática ilimitada |
| **VETERANO** | Luck I, Water Breathing, Speed I | Movilidad mejorada (+20% velocidad) |
| **LEYENDA** | Luck II, Water Breathing, Speed I, Fire Resistance | Nether seguro + Mejor loot |
| **MAESTRO** | Luck II, Water Breathing, Speed I, Fire Resistance, Resistance I | Defensa física (-20% daño) |
| **TITAN** | Luck II, Speed II, Water Breathing, Fire Resistance, Resistance I, Strength I | Combate superior (+130% daño) |
| **ABSOLUTO** | Luck III, Speed II, Water Breathing, Fire Resistance, Resistance II, Strength I, Absorption I | Poder completo balanceado |

### 2. **Menú de Skills Mejorado** 🎨

#### Estados Visuales Claros:
- **🟢 Verde con ✓** - Skill desbloqueada y activa
- **🟡 Amarillo con ◈** - Disponible para comprar (tienes XP y requisitos)
- **🔴 Rojo con ✗** - Bloqueada (faltan requisitos o XP insuficiente)
- **⚫ Gris con ⛔** - Deshabilitada (no disponible temporalmente)

#### Información Detallada de Compra:
**Antes:**
```
Costo: 400 XP
▶ Click para comprar
```

**Ahora:**
```
Costo: 400 XP (verde si tienes, rojo si no)
Tienes: 350 XP (verde si suficiente, rojo si no)

✓ Puedes comprar esta habilidad
✗ No tienes suficiente XP (necesitas 50 XP más)
```

#### Skills Deshabilitadas:
```
⛔ DESHABILITADA
Esta habilidad está temporalmente
desactivada por los desarrolladores.

✗ No se puede comprar
```

## 📊 Comparación: Antes vs Ahora

### Sistema Anterior (Desbalanceado):
```
NOVATO:     Night Vision ← GRATIS
EXPLORADOR: Night Vision, Luck
LEYENDA:    Night Vision, Saturation, Dolphin's Grace, Luck II, Resistance
ABSOLUTO:   Night Vision II, Saturation, Dolphin's Grace II, Luck IV,
            Resistance II, Strength II, Absorption III, Hero of Village II
```
**Problemas:**
- ❌ Night Vision gratis desde nivel 1 (devalúa skill tree)
- ❌ Saturation = nunca tienes hambre (rompe mecánica)
- ❌ Demasiadas habilidades acumuladas (ABSOLUTO tenía 9 efectos)
- ❌ Hero of Village casi inútil (descuentos con aldeanos)

### Sistema Nuevo (Balanceado):
```
NOVATO:     Ninguna habilidad
EXPLORADOR: Luck I
VETERANO:   Luck I, Water Breathing, Speed I
LEYENDA:    Luck II, Water Breathing, Speed I, Fire Resistance
ABSOLUTO:   Luck III, Speed II, Water Breathing, Fire Resistance,
            Resistance II, Strength I, Absorption I
```
**Beneficios:**
- ✅ Progresión gradual y significativa
- ✅ Cada rango añade valor real
- ✅ Mecánicas de Minecraft intactas
- ✅ Habilidades útiles en múltiples situaciones
- ✅ ABSOLUTO tiene 7 efectos (vs 9 anterior) pero más balanceados

## 🎮 Impacto en Jugadores

### Jugadores Actuales
- ⚠️ **Perderán Night Vision automática** al actualizar
  - Pueden comprarla en skill tree (400 XP, rama Exploración)
  - O usar antorchas/lámparas como antes
- ⚠️ **Perderán Saturation** (sin vuelta atrás)
  - Deberán gestionar hambre normalmente
  - Comida es más importante ahora
- ⚠️ **Perderán Dolphin's Grace automática**
  - Pueden comprar skill "Nadador" (4000 XP)
  - O esperar a rango SOBREVIVIENTE para Water Breathing
- ✅ **Ganarán Water Breathing** (muy útil)
- ✅ **Ganarán Speed** (movilidad constantemente útil)
- ✅ **Ganarán Fire Resistance** (Nether más seguro)

### Jugadores Nuevos
- ✅ Empiezan sin habilidades (justo)
- ✅ Progresión clara y motivadora
- ✅ Cada rango se siente como un logro
- ✅ Pueden elegir qué comprar en skill tree

## 🔧 Cambios Técnicos

### Archivos Modificados

#### 1. `Skill.java`
- Removida `"vision_nocturna"` de DISABLED_SKILLS
- Removido import `java.util.HashSet` sin usar
- Ahora VISION_NOCTURNA aparece en el menú

#### 2. `SkillTreeGUI.java`
**Mejoras en `createSkillItem()`:**
```java
// Verificación de skill habilitada
boolean skillEnabled = skill.isEnabled();

// Material para deshabilitadas
if (!skillEnabled) {
    displayMat = Material.BARRIER;
    prefix = "§8§l⛔ ";
}

// Comparación XP vs costo
int currentXP = plugin.getExperienceService().getXP(player);
int cost = skill.getBaseCost();
boolean canAfford = currentXP >= cost;

lore.add("§7Costo: " + (canAfford ? "§a" : "§c") + cost + " XP");
lore.add("§7Tienes: " + (canAfford ? "§a" : "§c") + currentXP + " XP");

if (canAfford) {
    lore.add("§a✓ Puedes comprar esta habilidad");
} else {
    lore.add("§c✗ No tienes suficiente XP");
    lore.add("§7Necesitas " + (cost - currentXP) + " XP más");
}
```

#### 3. `recompensas.yml`
**Cambios completos en `habilidades_por_rango`:**
- NOVATO: `[]` (vacío)
- EXPLORADOR: `LUCK:1`
- SOBREVIVIENTE: `LUCK:1`, `WATER_BREATHING:1`
- VETERANO: `LUCK:1`, `WATER_BREATHING:1`, `SPEED:1`
- LEYENDA: `LUCK:2`, `WATER_BREATHING:1`, `SPEED:1`, `FIRE_RESISTANCE:1`
- MAESTRO: `LUCK:2`, `WATER_BREATHING:1`, `SPEED:1`, `FIRE_RESISTANCE:1`, `RESISTANCE:1`
- TITAN: `LUCK:2`, `SPEED:2`, `WATER_BREATHING:1`, `FIRE_RESISTANCE:1`, `RESISTANCE:1`, `STRENGTH:1`
- ABSOLUTO: `LUCK:3`, `SPEED:2`, `WATER_BREATHING:1`, `FIRE_RESISTANCE:1`, `RESISTANCE:2`, `STRENGTH:1`, `ABSORPTION:1`

## 🧪 Testing Requerido

- [ ] Compilar sin errores
- [ ] Verificar que VISION_NOCTURNA aparece en skill tree
- [ ] Verificar menú muestra XP actual vs costo
- [ ] Verificar colores (verde si puedes pagar, rojo si no)
- [ ] Verificar skills deshabilitadas usan barrera
- [ ] Verificar rangos otorgan habilidades correctas
- [ ] Verificar Water Breathing funciona
- [ ] Verificar Speed funciona
- [ ] Verificar Fire Resistance funciona
- [ ] Verificar jugadores NOVATO no tienen habilidades
- [ ] Verificar que NO se da Night Vision automáticamente
- [ ] Verificar que NO se da Saturation
- [ ] Verificar que NO se da Dolphin's Grace
- [ ] Verificar que NO se da Hero of the Village

## 📌 Versión
- **Versión:** v1.22.64
- **Fecha:** Enero 31, 2026
- **Tipo de cambio:** Rebalanceo mayor + Mejoras de UX
- **Impacto:** Alto (cambios significativos en progresión)

## 🎯 Compilar

```bash
mvn package -DskipTests
```

---

**Resumen:** Sistema de habilidades completamente rebalanceado. Night Vision ahora es comprable, rangos dan habilidades más útiles y balanceadas, menú muestra claramente si puedes comprar o no. Progresión más gradual y satisfactoria.
