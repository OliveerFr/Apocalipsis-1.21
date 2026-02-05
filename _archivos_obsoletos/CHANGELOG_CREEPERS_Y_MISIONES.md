# 📋 CHANGELOG - Creepers Letales & Misiones Dinámicas

**Versión:** 1.22.49  
**Fecha:** 20 de Enero, 2026

---

## 🔥 **CREEPERS MEJORADOS - Sistema de Daño Letal**

### **Características Implementadas**

#### **Sistema de Explosión Balanceado**
- **Radio de Explosión:** Moderado (1.2x = +20% alcance)
  - Creeper normal: 3.0 → **3.6 bloques**
  - Creeper cargado: 6.0 → **7.2 bloques**
  - ✅ NO destruye construcciones masivamente

- **Daño de Explosión:** Extremadamente Letal (4.5x)
  - Multiplicador de daño: **450%**
  - Sin armadura: ~121 corazones (**instakill garantizado**)
  - Con armadura diamante completa: ~40-60 corazones
  - Creeper cargado: **Obliteración total**

#### **Mejoras Adicionales de Creepers**
- **35% de probabilidad** de spawn como creeper cargado (vs <1% vanilla)
- **Speed II** permanente (más rápidos y difíciles de evitar)
- **Resistance II** permanente (aguantan más golpes)
- **30 HP** de vida (vs 20 HP vanilla)
- **Explosión con fuego** (incendia el área)

### **Archivos Creados/Modificados**

#### **Nuevo Archivo:** `CreeperEnhancer.java`
```
src/main/java/me/apocalipsis/listeners/CreeperEnhancer.java
```

**Funcionalidad:**
- `onCreeperSpawn()`: Aplica buffs al spawn (Speed II, Resistance II, 30 HP, 35% charged)
- `onCreeperExplode()`: Aumenta radio ligeramente (1.2x) y registra creeper para daño
- `onExplosionDamage()`: **MULTIPLICA DAÑO x4.5** cuando la explosión impacta entidades

#### **Modificado:** `Apocalipsis.java`
- Registrado `CreeperEnhancer` listener en el sistema de eventos

#### **Modificado:** `config.yml`
```yaml
creeper_mejoras:
  multiplicador_radio: 1.2        # Radio +20% (explosión pequeña)
  multiplicador_dano: 4.5         # Daño +350% (LETAL)
  probabilidad_cargado: 0.35      # 35% creepers cargados
  velocidad_extra: true           # Speed II
  resistencia_extra: true         # Resistance II
```

### **Tecnología Utilizada**
- **ExplosionPrimeEvent:** Controla radio de explosión antes de detonar
- **EntityDamageByEntityEvent:** Intercepta y multiplica daño de explosión
- **HashMap<UUID, Float>:** Tracking de creepers explosivos para aplicar multiplicador correcto
- **Attribute System:** Modificación de MAX_HEALTH usando API moderna de Bukkit

---

## 🎯 **MISIONES ABSOLUTO - Expansión Dinámica**

### **Cambios Implementados**

#### **Expansión Masiva de Pool de Misiones**
- **Antes:** 30 misiones totales (6 fáciles, 8 medias, 16 difíciles)
- **Ahora:** **42 misiones totales** (10 fáciles, 12 medias, 20 difíciles)
- **Impacto:** Con 15 misiones diarias, la rotación es extremadamente variada

### **Nuevas Misiones FÁCILES (10 total)**

| ID | Nombre | Tipo | Objetivo | Cantidad | PS |
|---|---|---|---|---|---|
| `matar_skeleton_absoluto_facil` | Eliminar esqueletos (huesos) | MATAR | SKELETON | 10 | 20 |
| `craftear_papel_absoluto_facil` | Craftear papel (trades/libros) | CRAFTEAR | PAPER | 16 | 18 |
| `consumir_stew_absoluto_facil` | Consumir guiso de hongos | CONSUMIR | MUSHROOM_STEW | 3 | 17 |
| `cocinar_kelp_absoluto_facil` | Secar kelp (combustible) | COCINAR | DRIED_KELP | 12 | 16 |
| `craftear_compass_absoluto_facil` | Craftear brújula (navegación) | CRAFTEAR | COMPASS | 1 | 21 |
| `romper_cactus_absoluto_facil` | Cosechar cactus (trampas) | ROMPER | CACTUS | 16 | 15 |
| `matar_spider_absoluto_facil` | Cazar arañas (hilos) | MATAR | SPIDER | 12 | 19 |
| `craftear_barrel_absoluto_facil` | Craftear barriles (storage) | CRAFTEAR | BARREL | 4 | 18 |
| `consumir_cookie_absoluto_facil` | Consumir galletas (snack) | CONSUMIR | COOKIE | 8 | 14 |
| `cocinar_potato_absoluto_facil` | Cocinar patatas (comida rápida) | COCINAR | BAKED_POTATO | 10 | 17 |

### **Nuevas Misiones MEDIAS (12 total)**

| ID | Nombre | Tipo | Objetivo | Cantidad | PS |
|---|---|---|---|---|---|
| `matar_creepers_absoluto_media` | Cazar creepers mejorados | MATAR | CREEPER | 10 | 42 |
| `matar_drowned_absoluto_media` | Cazar ahogados (tridente) | MATAR | DROWNED | 15 | 45 |
| `craftear_lectern_absoluto_media` | Craftear atriles (aldeanos) | CRAFTEAR | LECTERN | 3 | 38 |
| `matar_silverfish_absoluto_media` | Exterminar lepismas (stronghold) | MATAR | SILVERFISH | 12 | 40 |
| `cocinar_glass_absoluto_media` | Fundir vidrio (construcción) | COCINAR | GLASS | 32 | 35 |
| `romper_prismarine_absoluto_media` | Romper prismarino (monumento) | ROMPER | PRISMARINE | 24 | 43 |
| `craftear_redstone_lamp_absoluto_media` | Craftear lámparas redstone | CRAFTEAR | REDSTONE_LAMP | 6 | 37 |
| `matar_guardian_absoluto_media` | Eliminar guardianes (cristales) | MATAR | GUARDIAN | 18 | 46 |
| `craftear_hopper_absoluto_media` | Craftear tolvas (automation) | CRAFTEAR | HOPPER | 4 | 41 |
| `romper_soul_sand_absoluto_media` | Minar soul sand (wither) | ROMPER | SOUL_SAND | 16 | 39 |
| `matar_husk_absoluto_media` | Eliminar huéspedes (desierto) | MATAR | HUSK | 14 | 40 |
| `craftear_piston_absoluto_media` | Craftear pistones (redstone) | CRAFTEAR | PISTON | 6 | 44 |

### **Nuevas Misiones DIFÍCILES (20 total)**

#### **Exploración y Raids Épicos**
- **Warden (Deep Dark):** 1 kill → 250 PS
- **Elder Guardian (Monumento):** 2 kills → 210 PS
- **Ravager (Raid):** 3 kills → 205 PS
- **Evoker (Totem):** 5 kills → 192 PS
- **Woodland Mansion (Vindicators):** 12 kills → 190 PS

#### **Farmeo de End**
- **Shulkers:** 16 kills → 185 PS
- **Endermites (granja):** 20 kills → 160 PS

#### **Netherite y Recursos Premium**
- **Ancient Debris (romper):** 12 bloques → 200 PS
- **Netherite Scrap (fundir):** 12 → 195 PS
- **Wither Skeletons:** 12 kills → 178 PS
- **Blazes (Fortaleza):** 18 kills → 175 PS

#### **Crafteo End-Game**
- **Beacon:** 1 → 230 PS
- **Conduit:** 1 → 215 PS
- **Netherite Sword:** 1 → 220 PS
- **Lodestone:** 2 → 170 PS
- **Smithing Table:** 1 → 165 PS

#### **Exploración Diversa**
- **Piglin Brutes (Bastión):** 8 kills → 198 PS
- **Crying Obsidian (ruinas):** 8 bloques → 168 PS
- **Gilded Blackstone:** 12 bloques → 172 PS

### **Beneficios de la Expansión**

✅ **Variedad extrema:** 42 misiones vs 15 diarias = rotación única cada día  
✅ **Sin repetición:** Los jugadores raramente verán las mismas misiones seguidas  
✅ **Mecánicas diversas:** Exploración, raids, crafteo, farmeo, combate  
✅ **Todas las dificultades:** Balance entre fácil/media/difícil  
✅ **Objetivos funcionales:** Cada misión tiene propósito (aldeanos, redstone, navegación, etc.)

---

## 📁 **Archivos Modificados**

### **Sistema de Creepers**
- ✅ `src/main/java/me/apocalipsis/listeners/CreeperEnhancer.java` (NUEVO)
- ✅ `src/main/java/me/apocalipsis/Apocalipsis.java` (listener registrado)
- ✅ `src/main/resources/config.yml` (sección creeper_mejoras)
- ✅ `target/classes/config.yml` (sincronizado)

### **Sistema de Misiones**
- ✅ `src/main/resources/misiones_new.yml` (42 misiones ABSOLUTO)
- ✅ `target/classes/misiones_new.yml` (sincronizado)

---

## 🎮 **Impacto en Gameplay**

### **Creepers: Terror Nocturno**
- Los jugadores **DEBEN usar armadura completa** para sobrevivir
- Estrategias de combate cuerpo a cuerpo son suicidas
- Arco con distancia es obligatorio
- Construcciones necesitan iluminación perfecta
- **Desafío máximo** sin romper construcciones masivamente

### **Misiones ABSOLUTO: Engagement Máximo**
- Jugadores del rango más alto **nunca** verán misiones repetitivas
- Incentiva exploración de **todas las dimensiones** (Overworld, Nether, End, Deep Dark)
- Requiere **diversificación de builds** (redstone, farms, construcción, combate)
- **Recompensas balanceadas** según dificultad real de cada misión

---

## ⚙️ **Configuración Recomendada**

### **Para Servidores Hardcore:**
```yaml
creeper_mejoras:
  multiplicador_radio: 1.5        # Radio más grande
  multiplicador_dano: 5.0         # Daño aún más letal
  probabilidad_cargado: 0.50      # 50% creepers cargados
```

### **Para Servidores Casuales:**
```yaml
creeper_mejoras:
  multiplicador_radio: 1.0        # Radio vanilla
  multiplicador_dano: 3.0         # Daño moderado
  probabilidad_cargado: 0.20      # 20% creepers cargados
```

---

## 🐛 **Bugs Conocidos / Advertencias**

- ⚠️ Warnings de compilación por concatenación de strings en logs (no afecta funcionalidad)
- ⚠️ `Attribute.MAX_HEALTH` puede retornar null en entidades customizadas (protegido con null-check)

---

## 📊 **Estadísticas del Update**

- **Líneas de código añadidas:** ~160 (CreeperEnhancer)
- **Misiones ABSOLUTO:** 30 → **42** (+40% contenido)
- **Variedad de tipos de misión:** 5 tipos (MATAR, ROMPER, CRAFTEAR, COCINAR, CONSUMIR)
- **Mobs únicos en misiones:** 20+ tipos diferentes
- **Recompensas totales ABSOLUTO:** 14-250 PS por misión

---

## 🔜 **Próximas Mejoras Sugeridas**

1. **Optimización de logs** (usar String.format en lugar de concatenación)
2. **Sistema de achievements** por completar misiones épicas (Warden, Elder Guardian)
3. **Misiones combinadas** (ej: "Raid completo: 5 Vindicators + 2 Evokers")
4. **Missions tracking UI** con progreso en bossbar
5. **Creeper variants** (Ice Creeper, Fire Creeper con efectos especiales)

---

**Desarrollado por:** Apocalipsis Team  
**Testing:** Pendiente en servidor de producción  
**Compatibilidad:** Minecraft 1.21.8 | Bukkit/Spigot API
