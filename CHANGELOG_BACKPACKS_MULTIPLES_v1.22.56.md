# Changelog - Sistema de Múltiples Mochilas v1.22.56

**Fecha:** 27 de enero de 2026  
**Plugin:** Apocalipsis-1.22.56.jar  
**Cambios:** Sistema de mochilas expandido con soporte de 10 mochilas por jugador

---

## 🎒 NUEVO: Sistema de Múltiples Mochilas

### Problema Anterior
- Las habilidades de backpack dejaban de funcionar al llegar a 54 slots (cofre doble)
- No había forma de expandir más allá del máximo
- Solo existía una mochila por jugador

### Solución Implementada

**10 mochilas independientes por jugador:**
```
/mochila      → Abre mochila #1 (comportamiento normal)
/bp           → Abre mochila #1
/bp 2         → Abre mochila #2
/bp 3         → Abre mochila #3
...
/bp 10        → Abre mochila #10
```

**Características:**
- ✅ Cada mochila tiene el tamaño según la habilidad (9/18/27/36/45/54 slots)
- ✅ Contenido independiente entre mochilas
- ✅ Guardado automático al cerrar
- ✅ Migración automática: mochilas antiguas → mochila #1

---

## 👮 NUEVO: Comandos de Moderación

### `/bp <jugador>` - Ver Mochila de Otro Jugador

**Uso:**
```
/bp Notch         → Ver mochila #1 de Notch
/bp Notch 2       → Ver mochila #2 de Notch
/bp Notch 5       → Ver mochila #5 de Notch
```

**Permisos:**
- `apocalipsis.mochila.mod` - Requerido para ver mochilas ajenas

**Características:**
- ✅ Funciona con jugadores online y offline
- ✅ Los cambios se guardan automáticamente
- ✅ Título muestra `[MOD] Mochila #X de <jugador>`
- ✅ Log de seguridad en consola

### Comandos Existentes Actualizados

**`/avo mochila ver <jugador> [número]`:**
```bash
/avo mochila ver Notch      # Ver mochila #1
/avo mochila ver Notch 3    # Ver mochila #3
```

**`/avo mochila vaciar <jugador> [número]`:**
```bash
/avo mochila vaciar Notch     # Vacía mochila #1
/avo mochila vaciar Notch 5   # Vacía mochila #5
```

---

## 🔧 Cambios Técnicos

### `BackpackService.java`

**Estructura de Datos:**
```java
// ANTES: Un array por jugador
Map<UUID, ItemStack[]> backpacks

// AHORA: 10 arrays por jugador
Map<UUID, Map<Integer, ItemStack[]>> backpacks
```

**Nuevos Métodos:**
```java
openBackpack(Player, int backpackNumber)
getBackpackContents(UUID, int number)
setBackpackContents(UUID, int number, ItemStack[])
openBackpackAsAdmin(Player mod, UUID target, String name, int number)
clearBackpack(UUID, Player mod, int number)
```

**Clases Actualizadas:**
```java
BackpackHolder {
    - UUID owner
    - int backpackNumber  // NUEVO
}

ModViewHolder {
    - UUID owner
    - UUID moderator
    - int backpackNumber  // NUEVO
}
```

### `Apocalipsis.java`

**Nuevo Comando `/bp`:**
```java
/bp           → openBackpack(player, 1)
/bp 2         → openBackpack(player, 2)
/bp Notch     → openBackpackAsAdmin(mod, Notch, 1)
/bp Notch 3   → openBackpackAsAdmin(mod, Notch, 3)
```

**TabCompleter:**
- `/mochila` → Sugiere números 1-10 y nombres de jugadores
- `/bp` → Sugiere números 1-10 y nombres de jugadores

### Formato de Guardado (YAML)

**Formato Antiguo:**
```yaml
backpacks:
  uuid-1234: [item1, item2, ...]
```

**Formato Nuevo:**
```yaml
backpacks:
  uuid-1234:
    1: [item1, item2, ...]  # Mochila #1
    2: [item3, item4, ...]  # Mochila #2
    5: [item5, item6, ...]  # Mochila #5
```

**Migración Automática:**
- Detecta formato antiguo al cargar
- Convierte a mochila #1
- Log: `[Backpack] Migrado formato antiguo de <uuid> a mochila #1`

---

## 📋 Ejemplos de Uso

### Jugadores

**Expandir almacenamiento:**
```
1. Comprar habilidad Inventario Infinito (54 slots)
2. /mochila    → Mochila #1 (54 slots)
3. /bp 2       → Mochila #2 (54 slots)
4. /bp 3       → Mochila #3 (54 slots)
...
Total: 540 slots (10 × 54) de almacenamiento extra!
```

**Organización:**
```
/mochila    → Recursos de construcción
/bp 2       → Comida y pociones
/bp 3       → Armaduras y armas
/bp 4       → Redstone y mecanismos
/bp 5       → Decoración
```

### Moderadores

**Inspección:**
```bash
# Ver todas las mochilas de un jugador
/bp Notch 1
/bp Notch 2
/bp Notch 3
...
```

**Moderación:**
```bash
# Vaciar mochila específica (requiere apocalipsis.mochila.admin)
/avo mochila vaciar Notch 2
§a✓ Mochila #2 de Notch vaciada.
```

**Lista de mochilas:**
```bash
/avo mochila lista
§6§l✦ §eMochilas con contenido (15):
  §7• §fNotch (mochila #1, 32 items)
  §7• §fNotch (mochila #2, 18 items)
  §7• §fSteve (mochila #1, 54 items)
  §7• §fAlex (mochila #1, 12 items)
  §7• §fAlex (mochila #5, 3 items)
  ...
```

---

## 🔐 Permisos

**Jugadores:**
- Ningún permiso requerido (solo necesitan habilidad)

**Moderadores:**
- `apocalipsis.mochila.mod` - Ver mochilas ajenas (`/bp <jugador>`)
- `apocalipsis.mochila.admin` - Vaciar mochilas (`/avo mochila vaciar`)

---

## ⚡ Optimizaciones

**Carga Lazy:**
- Solo se cargan las mochilas que se abren
- No se crean arrays vacíos innecesariamente

**Guardado Eficiente:**
- Solo guarda mochilas con contenido
- Mochilas vacías no se escriben al YAML

**Migración Automática:**
- Detecta formato antiguo
- Convierte sin pérdida de datos
- Compatible con backpacks.yml existentes

---

## 🐛 Fixes Incluidos

1. **Expansión bloqueada en 54 slots:**
   - ❌ Antes: Habilidades superiores no funcionaban
   - ✅ Ahora: Usa múltiples mochilas para expandir

2. **Moderación limitada:**
   - ❌ Antes: Solo `/avo mochila ver <jugador>`
   - ✅ Ahora: `/bp <jugador>` directo + selección de número

3. **No había TabCompleter:**
   - ❌ Antes: Escribir comandos manualmente
   - ✅ Ahora: Autocompletado de números y jugadores

---

## 📊 Estadísticas

**Capacidad Total por Jugador:**

| Habilidad              | Slots/Mochila | × 10 Mochilas | Total    |
|------------------------|---------------|---------------|----------|
| Bolsillos Profundos I  | 9 slots       | 10 mochilas   | 90 slots |
| Bolsillos Profundos II | 18 slots      | 10 mochilas   | 180 slots|
| Bolsillos Profundos III| 27 slots      | 10 mochilas   | 270 slots|
| Sin Fondo I            | 27 slots      | 10 mochilas   | 270 slots|
| Sin Fondo II           | 36 slots      | 10 mochilas   | 360 slots|
| Sin Fondo III          | 45 slots      | 10 mochilas   | 450 slots|
| **Inventario Infinito**| **54 slots**  | **10 mochilas**|**540 slots**|

**Inventario Vanilla:** 36 slots (27 + 9 hotbar)  
**Con Inventario Infinito + 10 mochilas:** 540 slots adicionales = **15× el inventario vanilla!**

---

## ⚠️ Notas Importantes

**Límite de 10 mochilas:**
- Hardcodeado en validación
- Rango: 1-10
- Puede aumentarse en el futuro si es necesario

**Tamaño de mochilas:**
- Depende de la habilidad desbloqueada
- Todas las mochilas (1-10) tienen el mismo tamaño
- Ej: Con Inventario Infinito, TODAS son de 54 slots

**Persistencia:**
- Guardado automático al cerrar inventario
- Guardado global en `plugins/Apocalipsis/backpacks.yml`
- Backup recomendado antes de actualizar

---

## 🔄 Compatibilidad

**Con versiones anteriores:**
- ✅ Mochilas existentes se migran automáticamente
- ✅ No hay pérdida de items
- ✅ Formato antiguo detectado y convertido

**Con plugins externos:**
- ✅ Compatible con inventarios custom
- ✅ No conflicto con WorldGuard/ProtectionStones
- ✅ Eventos de Bukkit respetados

---

## 📝 Testing Realizado

- [x] Migración de formato antiguo
- [x] Apertura de mochilas 1-10
- [x] Guardado de contenido independiente
- [x] Moderación con `/bp <jugador>`
- [x] TabCompleter funcionando
- [x] Validación de números (1-10)
- [x] Permisos de moderación
- [x] Logs de seguridad

---

## 🚀 Próximas Mejoras Sugeridas

- [ ] GUI para seleccionar mochilas (menú visual)
- [ ] Renombrar mochilas (`/bp rename 2 "Armaduras"`)
- [ ] Compartir mochilas entre jugadores
- [ ] Mochilas de equipo/clan
- [ ] Icono visual en inventario para abrir mochilas

---

**Compilado:** Apocalipsis-1.22.56.jar  
**Tamaño:** ~2.16 MB  
**Build:** Maven 3.x + Java 21  
**Tested:** Minecraft 1.21.8
