# 🗺️ CHANGELOG - Sistema de Waypoints Múltiples

**Versión:** 1.22.47  
**Fecha:** 18 de Enero, 2026

---

## ✨ NUEVAS CARACTERÍSTICAS

### 📍 Sistema de Waypoints Múltiples

Se ha implementado un sistema completo de waypoints múltiples que permite a los jugadores guardar y gestionar varios puntos de teletransporte.

#### **Características Principales:**

##### 🎯 Límites por Rango
- **Jugadores normales:** 1 waypoint
- **Rango Hunter_Adventurer:** 10 waypoints
- Sistema escalable para futuros rangos especiales

##### 🎮 Nuevos Comandos

```
/waypoint set <nombre>        - Guardar un waypoint con nombre personalizado
/waypoint tp <nombre>         - Teletransportarse a un waypoint específico
/waypoint list                - Listar todos tus waypoints guardados
/waypoint delete <nombre>     - Eliminar un waypoint
/waypoint                     - Ver ayuda y límite actual
```

**Alias disponibles:** `/wp`

##### 🔧 Funcionalidades Técnicas

- **Autocompletado inteligente:** Tab completer muestra tus waypoints al usar `/waypoint tp` o `/waypoint delete`
- **Validación de nombres:** Solo letras, números, guiones y guiones bajos (máximo 16 caracteres)
- **Persistencia mejorada:** Guarda automáticamente en `waypoints.yml` con formato extendido
- **Retrocompatibilidad:** Los waypoints antiguos se migran automáticamente al nombre "default"
- **Verificación de mundos:** Sistema verifica que el mundo siga cargado antes de teleportar
- **Cooldown global:** 300 segundos (5 minutos) entre teletransportes
- **Efectos visuales:** Partículas END_ROD y ENCHANT al guardar, PORTAL al teleportar
- **Sonidos:** Nota musical al guardar, sonido de Enderman al teleportar

---

## 🎨 RANGO HUNTER_ADVENTURER

### Configuración Actualizada

```yaml
hunter_adventurer:
  display_name: "§f[Hunter_Adventurer]"
  tab_prefix: "§f[Hunter_Adventurer] "
  chat_prefix: "§f[Hunter_Adventurer] "
  color: "§c"                    # Rojo para el nombre del jugador
  prioridad: 1                   # Prioridad baja (rangos XP tienen precedencia)
  efectos:
    - REGENERATION:2
    - STRENGTH:1
  heredar_efectos_rango_normal: true
```

### Beneficios Exclusivos
- ✅ **10 waypoints simultáneos** (vs 1 waypoint normal)
- ✅ Efectos permanentes: Regeneración II y Fuerza I
- ✅ Hereda efectos del rango normal (se suman)
- ✅ Prefijo distintivo en tab y chat

---

## 📝 EJEMPLOS DE USO

### Creando Waypoints
```
/waypoint set casa          → Waypoint 'casa' guardado (1/10)
/waypoint set mina          → Waypoint 'mina' guardado (2/10)
/waypoint set granja        → Waypoint 'granja' guardado (3/10)
/waypoint set nether        → Waypoint 'nether' guardado (4/10)
```

### Listando Waypoints
```
/waypoint list
→ ⚑ Waypoints (4/10):
  casa → 100, 64, 200 (world)
  mina → -450, 12, 680 (world)
  granja → 1200, 70, -340 (world)
  nether → 50, 75, 100 (world_nether)
```

### Teletransportándose
```
/waypoint tp casa           → Teletransportado al waypoint 'casa'
/waypoint tp mina           → Teletransportado al waypoint 'mina'
```

### Gestionando Waypoints
```
/waypoint delete granja     → Waypoint 'granja' eliminado
/waypoint list              → Muestra waypoints actualizados (3/10)
```

---

## 🛠️ CAMBIOS TÉCNICOS

### Archivos Modificados

1. **SkillEffectListener.java**
   - Cambiado `Map<UUID, Location>` a `Map<UUID, Map<String, Location>>`
   - Nuevos métodos: `setWaypoint(Player, String)`, `teleportToWaypoint(Player, String)`
   - Métodos adicionales: `listWaypoints()`, `deleteWaypoint()`, `getWaypointLimit()`
   - Sistema de persistencia actualizado con soporte para nombres
   - Migración automática de formato antiguo a nuevo
   - Importado `ConfigurationSection` para manejo avanzado de YAML

2. **Apocalipsis.java**
   - Comando `/waypoint` completamente rediseñado
   - Tab completer mejorado con autocompletado dinámico
   - Validación de nombres de waypoints
   - Mensajes de ayuda contextuales

3. **skills.yml**
   - Documentación actualizada sobre límites por rango
   - Comentarios sobre el sistema de límites

4. **plugin.yml**
   - Descripción y uso actualizados del comando `/waypoint`

5. **rangos_permanentes.yml**
   - Agregada guía completa de códigos de color de Minecraft
   - Rango `hunter_adventurer` configurado y documentado
   - Prioridad ajustada a 1 para compatibilidad con rangos XP

---

## 🔄 COMPATIBILIDAD

### Retrocompatibilidad
- ✅ Los waypoints antiguos (sin nombre) se convierten automáticamente a "default"
- ✅ Comandos legacy funcionan para jugadores con 1 waypoint
- ✅ No requiere migración manual de datos

### Formato de Datos

**Antes (formato antiguo):**
```yaml
waypoints:
  <uuid>:
    world: world
    x: 100
    y: 64
    z: 200
```

**Ahora (formato nuevo):**
```yaml
waypoints:
  <uuid>:
    casa:
      world: world
      x: 100
      y: 64
      z: 200
    mina:
      world: world
      x: -450
      y: 12
      z: 680
```

---

## 📋 CÓDIGOS DE COLOR DISPONIBLES

Agregados en `rangos_permanentes.yml` para referencia rápida:

```
§0 - Negro          §8 - Gris oscuro
§1 - Azul oscuro    §9 - Azul
§2 - Verde oscuro   §a - Verde
§3 - Aqua oscuro    §b - Aqua
§4 - Rojo oscuro    §c - Rojo
§5 - Púrpura        §d - Rosa
§6 - Dorado         §e - Amarillo
§7 - Gris           §f - Blanco
```

**Formatos:**
- §l - Negrita
- §o - Cursiva
- §n - Subrayado
- §m - Tachado
- §k - Ofuscado
- §r - Reset

---

## 🚀 PRÓXIMAS MEJORAS SUGERIDAS

- [ ] Sistema de waypoints compartidos entre jugadores del mismo clan
- [ ] Categorías de waypoints (exploración, recursos, eventos)
- [ ] Waypoints públicos de la comunidad
- [ ] Cooldown reducido para rangos superiores
- [ ] Comando `/waypoint nearest` para encontrar el waypoint más cercano
- [ ] Límites personalizables por rango en configuración

---

## 📌 NOTAS IMPORTANTES

- El límite de waypoints para `hunter_adventurer` está **hardcodeado** en `SkillEffectListener.java`
- Para agregar más rangos con límites especiales, modificar el método `getWaypointLimit()`
- El cooldown es **global** para todos los waypoints (no individual por waypoint)
- Los waypoints se guardan inmediatamente si `persistencia: true` en `skills.yml`

---

**Desarrollado por:** Sistema de Rangos Permanentes de Apocalipsis  
**Compatibilidad:** Minecraft 1.21.8 / Paper
