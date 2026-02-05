# 🎒 Separación de Backpacks por Ciclo/Mundo - v1.22.56

## 📋 Resumen
Sistema de mochilas actualizado para separar completamente el inventario por ciclo/mundo, evitando que los jugadores accedan a items de otros ciclos a través de sus backpacks.

---

## 🔧 Cambios Técnicos

### **BackpackService.java - Reestructuración Completa**

#### **1. Estructura de Datos Actualizada**
```java
// ANTES:
Map<UUID, Map<Integer, ItemStack[]>> backpacks;
// UUID -> Número de Mochila -> Items

// AHORA:
Map<UUID, Map<String, Map<Integer, ItemStack[]>>> backpacks;
// UUID -> Nombre del Mundo -> Número de Mochila -> Items
```

#### **2. BackpackHolder Actualizado**
- ✅ Agregado campo `worldName`
- ✅ Constructor actualizado para recibir mundo
- ✅ Getter `getWorldName()` implementado

#### **3. Métodos de Acceso Actualizados**

**openBackpack():**
```java
// Ahora detecta el mundo actual del jugador
String worldName = player.getWorld().getName();

// Título incluye el mundo:
// §6Mochila #1 §8(§bworld§8)
// §6Mochila #2 §8(§bciclo_1§8)
```

**getBackpackContents():**
```java
// Sobrecarga con 3 parámetros:
getBackpackContents(UUID uuid, String worldName, int backpackNumber)

// Mantiene compatibilidad:
getBackpackContents(UUID uuid, int backpackNumber) → usa "world"
getBackpackContents(UUID uuid) → usa "world" y mochila #1
```

**setBackpackContents():**
```java
// Ahora guarda por mundo:
setBackpackContents(UUID uuid, String worldName, int backpackNumber, ItemStack[] contents)
```

#### **4. Persistencia (YAML) Actualizada**

**loadBackpacks():**
- ✅ Carga nueva estructura: `backpacks.uuid.worldName.number`
- ✅ **Migración automática** de formatos antiguos:
  - Formato muy antiguo: `backpacks.uuid = [items]` → migra a `world/1`
  - Formato antiguo: `backpacks.uuid.1 = [items]` → migra a `world/1`
  - Formato nuevo: `backpacks.uuid.worldName.1 = [items]` → carga directo
- ✅ Logs de migración para tracking

**saveBackpacks():**
```yaml
# Estructura en backpacks.yml:
backpacks:
  player-uuid:
    world:
      1: [items...]
      2: [items...]
    ciclo_1:
      1: [items...]
    ciclo_2:
      1: [items...]
```

#### **5. Moderación Actualizada**

**ModViewHolder:**
- ✅ Agregado campo `worldName`
- ✅ Constructor actualizado con 4 parámetros
- ✅ Getter `getWorldName()` implementado

**openBackpackAsAdmin():**
```java
// Sobrecarga con mundo específico:
openBackpackAsAdmin(Player mod, UUID target, String name, int number, String world)

// Auto-detecta mundo del jugador:
openBackpackAsAdmin(Player mod, UUID target, String name, int number)

// Título de moderador:
// §c[MOD] §eMochila #1 de Player §8(§bworld§8)
```

**clearBackpack():**
```java
// Limpia mochila en mundo específico:
clearBackpack(UUID target, Player moderator, int number, String world)

// Log con mundo:
// [MOCHILA-MOD] Admin vació la mochila #1 de Player (mundo: ciclo_1)
```

**getBackpackList():**
```java
// Lista incluye mundo:
// "Player (mochila #1, mundo: world, 5 items)"
// "Player (mochila #1, mundo: ciclo_1, 3 items)"
```

#### **6. Cierre de Inventario**

**onInventoryClose():**
- ✅ Detecta `worldName` del holder
- ✅ Guarda con `setBackpackContents(uuid, worldName, number, contents)`
- ✅ Funciona tanto para BackpackHolder como ModViewHolder

---

## 🎯 Funcionalidad

### **Comportamiento Actual:**

1. **Jugador en `world`:**
   - Abre mochila → ve solo items de `world`
   - Guarda items → se guardan en `world`

2. **Jugador cambia a `ciclo_1`:**
   - Abre mochila → ve solo items de `ciclo_1` (vacío si es primera vez)
   - Items de `world` NO son accesibles

3. **Jugador regresa a `world`:**
   - Abre mochila → recupera items originales de `world`

4. **Moderador inspecciona:**
   - Puede ver mochilas de cualquier mundo
   - Título indica claramente el mundo
   - Cambios se guardan en el mundo correcto

---

## 📦 Migración de Datos

### **Automática al cargar:**
```
[Backpack] Migrado formato antiguo de <uuid> a world/mochila #1
[Backpack] Migrado <uuid> a formato con mundos
Cargadas 5 mochilas de jugadores (10 mochilas nuevas, 5 migradas).
Mochilas migradas guardadas en formato nuevo.
```

### **Retrocompatibilidad:**
- ✅ Mochilas antiguas se migran a mundo `"world"`
- ✅ No se pierden datos durante la migración
- ✅ Guardado automático después de migrar
- ✅ Formato antiguo ya no se genera

---

## 🔒 Protecciones Implementadas

1. **Separación por Mundo:**
   - Cada mundo tiene su propio set de mochilas
   - Imposible acceder a items de otros mundos

2. **Validación:**
   - `worldName` se obtiene del jugador (no puede falsificarse)
   - Títulos de inventario incluyen mundo para claridad

3. **Moderación:**
   - Logs incluyen mundo para auditoría
   - Moderadores deben especificar qué mundo revisar

4. **Persistencia:**
   - Guardado automático al cerrar
   - Estructura YAML clara y organizada

---

## 📊 Compilación

```
BUILD SUCCESS
Total time: 04:07 min
JAR: Apocalipsis-1.22.56.jar
Size: 2,035,376 bytes (~1.94 MB)
```

---

## 🧪 Pruebas Recomendadas

1. **Separación básica:**
   ```
   1. Guardar items en mochila en world
   2. Cambiar a ciclo_1
   3. Verificar mochila vacía en ciclo_1
   4. Regresar a world
   5. Verificar items persisten
   ```

2. **Migración:**
   ```
   1. Copiar backpacks.yml antiguo
   2. Reiniciar servidor
   3. Verificar logs de migración
   4. Comprobar items en mundo "world"
   ```

3. **Moderación:**
   ```
   1. Jugador con items en varios mundos
   2. Moderador abre con /backpack view <player> 1
   3. Verificar título muestra mundo correcto
   4. Modificar items
   5. Verificar cambios se guardan en mundo correcto
   ```

---

## 📝 Archivos Modificados

- `src/main/java/me/apocalipsis/skills/BackpackService.java` (654 líneas, reestructuración completa)

---

## ✅ Problema Resuelto

**ANTES:**
> "estoy en ciclo diferente y se hay cosas que no deberian osea el backpack esta global"

**AHORA:**
> ✅ Backpacks completamente separados por ciclo/mundo
> ✅ Imposible acceder a items de otros ciclos
> ✅ Sistema de migración automática
> ✅ Moderación con soporte multi-mundo
> ✅ Logs claros para auditoría

---

## 🚀 Uso

**Para Jugadores:**
- `/backpack` → abre mochila del mundo actual
- Los items están separados por ciclo automáticamente

**Para Moderadores:**
- `/backpack view <player>` → ve mochila en mundo actual del jugador
- Título indica claramente qué mundo está viendo
- Comandos de limpieza usan mundo del jugador objetivo

---

**Fecha:** 28 de enero de 2026  
**Versión:** 1.22.56  
**Tipo:** Bug Fix + Feature Enhancement  
**Prioridad:** Alta (afecta integridad de ciclos)
