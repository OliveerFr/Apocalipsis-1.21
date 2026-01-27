# 📋 Changelog Completo - Apocalipsis v1.22.56

**Fecha:** 27 de enero de 2026  
**Versión:** 1.22.56  
**Build:** Apocalipsis-1.22.55.jar

---

## 🎒 SISTEMA DE MÚLTIPLES MOCHILAS (NUEVO)

### Problema Solucionado
Las habilidades de backpack dejaban de funcionar al llegar al máximo de 54 slots (cofre doble). Los jugadores no podían expandir más allá de ese límite, desperdiciando puntos de habilidad.

### Solución Implementada

**Sistema de 2 Mochilas Independientes por Jugador:**

```bash
/mochila        # Abre mochila #1 (comportamiento normal)
/mochila 2      # Abre mochila #2 (nueva)
/bp             # Alias de /mochila
/bp 2           # Abre segunda mochila
```

#### Características Principales

✅ **2 mochilas completas por jugador**
- Cada mochila tiene el tamaño según la habilidad desbloqueada
- Contenido independiente entre mochilas
- Guardado automático al cerrar
- Migración automática: mochilas antiguas → mochila #1

✅ **Capacidad expandida:**
- Con **Bolsillos Profundos III**: 27 slots × 2 = **54 slots adicionales**
- Con **Bolsillos Sin Fondo III**: 45 slots × 2 = **90 slots adicionales**
- Con **Inventario Infinito**: 54 slots × 2 = **108 slots adicionales**

✅ **Sistema de moderación mejorado:**

```bash
# Ver mochilas de otros jugadores
/bp Notch           # Ver mochila #1 de Notch
/bp Notch 2         # Ver mochila #2 de Notch
/bp Steve 2         # Ver mochila #2 de Steve

# Comandos de administración
/avo mochila ver Notch 2         # Ver mochila específica
/avo mochila vaciar Notch 2      # Vaciar mochila específica
/avo mochila lista               # Lista todas las mochilas con contenido
```

**Permisos:**
- `apocalipsis.mochila.mod` - Ver mochilas ajenas
- `apocalipsis.mochila.admin` - Vaciar mochilas

✅ **TabCompleter completo:**
- `/mochila` autocompleta: `1`, `2`, nombres de jugadores
- `/bp` autocompleta: `1`, `2`, nombres de jugadores
- Segundo argumento (moderación): autocompleta números de mochila

#### Cambios Técnicos

**BackpackService.java:**
- Estructura de datos: `Map<UUID, Map<Integer, ItemStack[]>>`
- Clase `BackpackHolder`: agregado campo `backpackNumber`
- Clase `ModViewHolder`: agregado campo `backpackNumber`
- Métodos sobrecargados:
  - `openBackpack(Player player, int backpackNumber)`
  - `getBackpackContents(UUID uuid, int backpackNumber)`
  - `setBackpackContents(UUID uuid, int number, ItemStack[])`
  - `openBackpackAsAdmin(Player, UUID, String, int number)`
  - `clearBackpack(UUID, Player, int number)`

**Formato de Guardado (YAML):**

```yaml
# Formato antiguo (migrado automáticamente)
backpacks:
  uuid-1234: [item1, item2, ...]

# Formato nuevo
backpacks:
  uuid-1234:
    1: [item1, item2, ...]  # Mochila #1
    2: [item3, item4, ...]  # Mochila #2
```

**Migración Automática:**
- Detecta formato antiguo al cargar `backpacks.yml`
- Convierte a mochila #1 sin pérdida de datos
- Log: `[Backpack] Migrado formato antiguo de <uuid> a mochila #1`

#### Validación y Límites

**Validación de números:**
- Rango permitido: 1-2
- Error si se intenta usar 3+: "Número de mochila inválido. Usa 1 o 2."

**Logs de moderación:**
- Apertura: `[MOCHILA-MOD] Admin abrió mochila #2 de Notch`
- Vaciado: `[MOCHILA-MOD] Admin vació la mochila #2 de Steve`

#### Ejemplos de Uso

**Jugadores:**
```bash
# Organización básica
/mochila      # Recursos de construcción
/mochila 2    # Comida y pociones

# Uso en exploración
/bp           # Items importantes
/bp 2         # Loot temporal
```

**Moderadores:**
```bash
# Inspección
/bp Notch 1   # Ver primera mochila
/bp Notch 2   # Ver segunda mochila

# Administración
/avo mochila lista
> Mochilas con contenido (5):
>   • Notch (mochila #1, 32 items)
>   • Notch (mochila #2, 18 items)
>   • Steve (mochila #1, 54 items)

/avo mochila vaciar Notch 2
> ✓ Mochila #2 de Notch vaciada.
```

---

## 🌍 SISTEMA DE CICLOS MULTI-MUNDO (MEJORADO)

### Seguridad y Validación

✅ **Validación de nombres de ciclos:**
- Solo permite letras, números, guiones y guiones bajos
- Previene nombres especiales del sistema: `world`, `world_nether`, `world_the_end`
- Límite de longitud: 3-32 caracteres
- Mensaje de error descriptivo si el nombre es inválido

✅ **Protección de mundos predeterminados:**
- Imposible eliminar/sobrescribir mundos principales
- Validación antes de crear nuevos ciclos
- Prevención de conflictos de nombres

✅ **Sistema anti-evasión mejorado:**
- Previene que jugadores eviten castigos cambiando de mundo
- Trackeo de castigos activos al cambiar de ciclo
- Mensaje: "No puedes cambiar de mundo mientras estés castigado"

### Recompensas de Rango Permanente

✅ **Sistema de recompensas por ciclo:**
- Cada jugador puede reclamar recompensas UNA VEZ por ciclo
- Recompensas basadas en el rango permanente del jugador
- Comando: `/avo recompensas mundo` o `/recompensa`

**Rangos y recompensas:**
```
Survivor:
  - 3 Manzanas Doradas
  - 16 Bistecs
  - 200 XP

Hunter:
  - 5 Manzanas Doradas
  - 32 Bistecs
  - 1 Totem de Inmortalidad
  - 500 XP

Warden:
  - 10 Manzanas Doradas
  - 64 Bistecs
  - 2 Totems de Inmortalidad
  - 1 Elytra
  - 1000 XP

Hunter_Adventurer (especial):
  - 15 Manzanas Doradas
  - 64 Bistecs
  - 3 Totems de Inmortalidad
  - 1 Elytra
  - 5 Diamantes
  - 2000 XP
```

✅ **Reseteo de recompensas:**
- Comando admin: `/avo recompensas mundo reset <mundo>`
- Permite resetear recompensas de un mundo específico
- Útil para eventos especiales o correcciones

✅ **Menú visual:**
- GUI con información del rango
- Muestra recompensas disponibles
- Indica si ya fueron reclamadas
- Botón de reclamar (verde) o reclamado (rojo)

### Tracking y Persistencia

✅ **Base de datos de recompensas:**
- Archivo: `rangos_recompensas.yml`
- Estructura: `UUID → mundo → timestamp de reclamo`
- Guardado automático al reclamar

✅ **Prevención de duplicación:**
- Validación antes de otorgar items
- Check de espacio en inventario
- Rollback si falla la entrega

### Comandos de Ciclos Actualizados

```bash
# Crear ciclo
/ciclo crear <nombre>              # Crea nuevo mundo
/avo ciclo nuevo <nombre>          # Alias

# Gestión
/ciclo listar                      # Lista todos los ciclos
/ciclo info <mundo>                # Info detallada
/ciclo activar <mundo>             # Activa ciclo
/ciclo desactivar <mundo>          # Desactiva ciclo

# Teleporte
/ciclo tp <mundo>                  # Teleporta al spawn

# Seguridad
/ciclo validar                     # Valida todos los ciclos
/ciclo security                    # Check de seguridad
/ciclo autocorrect                 # Corrige problemas

# Recompensas (NUEVO)
/recompensa                        # Abre menú
/avo recompensas mundo             # Abre menú
/avo recompensas mundo reset <mundo>  # Resetea (admin)
```

### Mejoras de Rendimiento

✅ **Cache de ciclos activos:**
- Reduce lecturas de disco
- Actualización automática al crear/eliminar
- Invalidación inteligente

✅ **Validación asíncrona:**
- Checks de seguridad no bloquean server
- Reportes en consola sin lag
- Corrección automática de problemas menores

---

## 🎯 SISTEMA DE HABILIDADES (BALANCEO)

### Skills Deshabilitadas (22 total)

Para mejorar la experiencia de nuevos jugadores, se desactivaron habilidades poco útiles o muy situacionales. El código se mantiene para reactivarlas en el futuro.

**Deshabilitadas:**

**UTILIDAD (3):**
- Estómago de Hierro - Hambre no es problema
- Metabolismo Lento - Mejora del anterior
- Autosuficiente - Regenera muy lento

**SUPERVIVENCIA (2):**
- Resistencia al Fuego - Solo Nether
- Ignífugo - Muy situacional

**COMBATE (3):**
- Piel de Escamas - Solo -5% daño
- Reflejos - Attack speed menor
- Bloqueo Perfecto - Requiere escudo

**EXPLORACIÓN (8):**
- Brújula Interna - F3 ya existe
- Telescopio - Catalejo vanilla
- Mapa Mental - Poco útil
- Pisadas Silenciosas - Poco impacto
- Sombra - Mejora anterior
- Rastro de Oro - Muy tramposo
- Detector de Spawners - Muy específico
- XRay Diamantes - Demasiado OP

**INVOCACIÓN (2):**
- Gato Guardián - Muy situacional
- Allay Recolector - Redundante

**SINERGIAS (4):**
- Cazador Experto - Muy específico
- Minero Guerrero - Poco usado
- Explorador Ligero - Condición rara
- Omnipresente - Wallhack tramposo

**Resultado:**
- **Antes:** 60 habilidades (muchas inútiles)
- **Ahora:** 38 habilidades (solo útiles)
- **Reducción:** -37% de clutter

### Balanceo de Valores

**Supervivencia (vida reducida):**
```
Tanque:    4/6/8   → 3/5/7   corazones extra
Inmortal:  8/10/14 → 6/8/10  corazones extra
```

**Utilidad (velocidad reducida):**
```
Zancadas:   20/30/40% → 15/20/25% velocidad
Velocista:  30/40/50% → 25/30/35% velocidad
```

**Combate (daño reducido):**
```
Francotirador: 20/35/50% → 15/25/35% daño
Ejecutor:      25/40/60% → 20/30/40% daño
Berserker:     40/60/80% → 30/40/50% daño
Vampirismo:    5/8/12%   → 4/6/8%    lifesteal
```

**Drops reducidos:**
```
Toque Fortuna: 10/20/30% → 8/12/15%
Seda Natural:  5/10/15%  → 3/5/8%
```

---

## 🐛 FIXES DE BUGS

### Misiones de Tutorial

✅ **Fix tracking de misiones COCINAR:**
- Problema: No registraba progreso si jugador había completado casi todas las misiones
- Solución: Agregado `EventPriority.MONITOR` a `onSmelt()` y `onConsume()`
- Versión: 1.22.55

✅ **Fix variable duplicada:**
- Archivo: `ApocalipsisCommand.java`
- Conflicto de variable `player` en múltiples scopes
- Corregido para evitar warnings

### Sistema de Tokens

✅ **Fix duplicación de tokens:**
- Problema: Tokens físicos (inventario) + tokens en DB causaban duplicación
- Solución: Sistema unificado de tracking
- Prevención de exploits

### Auto-Start Desastres

✅ **Inicio automático:**
- Los desastres ahora se auto-inician al cargar el plugin
- No requiere comando manual `/avo start`
- Mejora UX para reiniciar servidor

### New Day Automático

✅ **Ciclo diario automático:**
- Ejecuta `/avo newday` cada 24 horas reales
- Mantiene economía y progresión balanceada
- No requiere intervención manual

---

## 🔧 CAMBIOS TÉCNICOS

### Archivos Modificados

**v1.22.56 (Mochilas + Ciclos):**
1. `BackpackService.java` - Sistema de múltiples mochilas
2. `Apocalipsis.java` - Comandos `/mochila` y `/bp` expandidos
3. `AvoTabCompleter.java` - Autocompletado de mochilas
4. `Skill.java` - Set DISABLED_SKILLS
5. `SkillConfig.java` - Valores balanceados
6. `CicloManager.java` - Validación y seguridad
7. `PermRankManager.java` - Sistema de recompensas
8. `RecompensaCommand.java` - Comando `/recompensa`

**v1.22.55 (Misiones):**
1. `MissionListener.java` - Fix tracking COCINAR
2. `ApocalipsisCommand.java` - Fix variable duplicada

### Nuevos Archivos

```
backpacks.yml              # Base de datos de mochilas (nuevo formato)
rangos_recompensas.yml     # Tracking de recompensas por mundo
```

### Base de Datos

**backpacks.yml:**
```yaml
backpacks:
  <uuid>:
    1: [items mochila 1]
    2: [items mochila 2]
```

**rangos_recompensas.yml:**
```yaml
players:
  <uuid>:
    world: timestamp
    ciclo_1: timestamp
    ciclo_2: timestamp
```

---

## 📊 ESTADÍSTICAS

### Capacidad de Almacenamiento

| Habilidad              | Antes      | Ahora (×2) | Aumento  |
|------------------------|------------|------------|----------|
| Bolsillos Profundos I  | 9 slots    | 18 slots   | +100%    |
| Bolsillos Profundos II | 18 slots   | 36 slots   | +100%    |
| Bolsillos Profundos III| 27 slots   | 54 slots   | +100%    |
| Sin Fondo I            | 27 slots   | 54 slots   | +100%    |
| Sin Fondo II           | 36 slots   | 72 slots   | +100%    |
| Sin Fondo III          | 45 slots   | 90 slots   | +100%    |
| **Inventario Infinito**| **54 slots**|**108 slots**|**+100%**|

### Habilidades

| Categoría      | Antes | Ahora | Cambio |
|----------------|-------|-------|--------|
| Total          | 60    | 38    | -37%   |
| Habilitadas    | 60    | 38    | -22    |
| Deshabilitadas | 0     | 22    | +22    |

### Recompensas por Rango

| Rango               | Items | XP   | Especiales |
|---------------------|-------|------|------------|
| Survivor            | 19    | 200  | -          |
| Hunter              | 38    | 500  | Totem ×1   |
| Warden              | 76    | 1000 | Totem ×2, Elytra |
| Hunter_Adventurer   | 88    | 2000 | Totem ×3, Elytra, Diamante ×5 |

---

## 🚀 INSTALACIÓN Y ACTUALIZACIÓN

### Actualizar desde v1.22.55 o anterior

1. **Hacer backup:**
   ```bash
   # Backup de datos importantes
   /backup
   /avo backup
   ```

2. **Detener servidor**

3. **Reemplazar JAR:**
   ```bash
   # Borrar antiguo
   rm plugins/Apocalipsis-1.22.55.jar
   
   # Copiar nuevo
   cp Apocalipsis-1.22.56.jar plugins/
   ```

4. **Iniciar servidor**

5. **Verificar migración:**
   - Mochilas antiguas migradas automáticamente
   - Check logs: `[Backpack] Migrado formato antiguo`
   - Verificar `/mochila` funciona normalmente

### Configuración Recomendada

**Permisos:**
```yaml
# Moderadores
apocalipsis.mochila.mod: true          # Ver mochilas ajenas
apocalipsis.ciclo.create: true         # Crear ciclos

# Admins
apocalipsis.mochila.admin: true        # Vaciar mochilas
apocalipsis.recompensas.reset: true    # Resetear recompensas
```

---

## ⚠️ NOTAS IMPORTANTES

### Compatibilidad

✅ **Compatible con:**
- Versiones anteriores de Apocalipsis (1.22.x)
- Mochilas existentes (migración automática)
- Plugins: WorldGuard, ProtectionStones, ModelEngine, MythicMobs

⚠️ **Requiere:**
- Minecraft 1.21+
- Java 21
- Spigot/Paper 1.21+

### Rendimiento

**Optimizaciones:**
- Cache de ciclos activos
- Guardado asíncrono de mochilas
- Validación lazy de permisos

**Recomendaciones:**
- Backup de `backpacks.yml` antes de actualizar
- Monitorear uso de memoria con múltiples mochilas
- Límite de 2 mochilas previene sobrecarga

### Migración de Datos

**Automática:**
- ✅ Mochilas antiguas → Mochila #1
- ✅ Recompensas de rango (nueva DB)
- ✅ Skills deshabilitadas (invisible para jugadores)

**Manual (opcional):**
- Resetear recompensas: `/avo recompensas mundo reset <mundo>`
- Reactivar skills: editar `DISABLED_SKILLS` en `Skill.java`

---

## 🐛 PROBLEMAS CONOCIDOS

### En Investigación

1. **Compilación lenta de Maven:**
   - 162 archivos fuente toman ~3-5 minutos
   - Workaround: compilar con `-q` flag

2. **TabCompleter duplicado:**
   - Warnings de IDE sobre eficiencia
   - No afecta funcionalidad

### Solucionados en Esta Versión

- ✅ Misiones COCINAR no tracking
- ✅ Variable `player` duplicada
- ✅ Tokens duplicados
- ✅ Formato antiguo de mochilas
- ✅ Nombres de ciclos inválidos

---

## 📞 SOPORTE

**Reportar bugs:**
- GitHub Issues: `OliveerFr/Apocalipsis-1.21`
- Discord: [Tu servidor]

**Documentación:**
- Wiki: [URL]
- Guías: Ver archivos `GUIA_*.md`

**Logs importantes:**
```bash
# Ver logs de mochilas
tail -f logs/latest.log | grep "Backpack"

# Ver logs de ciclos
tail -f logs/latest.log | grep "Ciclo"

# Ver logs de recompensas
tail -f logs/latest.log | grep "Recompensas"
```

---

## 🎉 AGRADECIMIENTOS

- **Testing:** Comunidad del servidor
- **Feedback:** Jugadores que reportaron bugs
- **Desarrollo:** OliveerFr

---

## 📅 PRÓXIMAS VERSIONES

**v1.22.57 (Planeado):**
- [ ] GUI para seleccionar mochilas (menú visual)
- [ ] Renombrar mochilas personalizadas
- [ ] Compartir mochilas entre jugadores
- [ ] Mochilas de equipo/clan

**v1.22.58 (Planeado):**
- [ ] Sistema de logros para ciclos
- [ ] Recompensas progresivas por tiempo en ciclo
- [ ] Eventos especiales por ciclo

---

**Versión:** 1.22.56  
**Compilado:** 27 de enero de 2026  
**Build:** Maven 3.x + Java 21  
**Tested:** Minecraft 1.21.8 + Paper

🚀 **¡Disfruta las nuevas características!**
