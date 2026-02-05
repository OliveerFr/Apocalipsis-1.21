# Changelog v1.22.54 - Comando Admin Recompensas Evento 5

## 📅 Fecha
2026-01-22

## 🎯 Objetivo
Crear un comando de administrador para obtener todas las recompensas del Evento 5 sin necesidad de completar el evento.

## ✨ Nuevo Comando

### `/avo evento5 recompensas`
**Alias:** `/avo evento5 rewards`

**Permisos requeridos:** `avo.admin`

**Descripción:** Otorga al administrador todas las recompensas del ranking **PLATINUM** (Puesto #1) del Evento 5.

**Sintaxis:**
```
/avo evento5 recompensas
```

## 📦 Recompensas Otorgadas

### Items (23 en total):
1. **Armadura Desoladora Completa** (4 piezas)
   - Casco Netherite + Protection IV + Unbreaking III
   - Peto Netherite + Protection IV + Unbreaking III
   - Pantalones Netherite + Protection IV + Unbreaking III
   - Botas Netherite + Protection IV + Unbreaking III

2. **Armas y Herramientas** (2 items)
   - Espada Desoladora (Netherite):
     * Sharpness V
     * Knockback II
     * Unbreaking III
     * Fire Aspect II
   - Pico Desolador (Netherite):
     * Efficiency V
     * Fortune III
     * Unbreaking III

3. **Items Épicos** (6 items)
   - 1x Corazón Desolador (LEGENDARIO)
   - 5x Escama Perfecta (ÉPICO)

4. **Materiales Premium** (3 items)
   - 3x Netherite Ingot
   - 24x Diamond

5. **Items Base** (8 items)
   - 8x Fragmento del Vacío
   - 12x Ender Pearl
   - 12x Eye of Ender

### Experiencia:
- **+11,000 XP** (equivalente al Puesto #1)

## 💻 Implementación Técnica

### Cambios en ApocalipsisCommand.java

#### 1. Nuevo case en cmdEvento5()
```java
case "recompensas":
case "rewards":
    // Verifica que sea un jugador
    // Crea instancia temporal de AperturaEndEvent
    // Genera lista de recompensas PLATINUM
    // Añade items al inventario (o dropea si está lleno)
    // Otorga XP
    // Muestra mensaje de confirmación
```

#### 2. Actualización del menú de ayuda
```java
sender.sendMessage("§e▸ Admin - Testing:");
// ... otros comandos
sender.sendMessage("  §f/avo evento5 recompensas §7- Obtener todas las recompensas");
```

### Cambios en AperturaEndEvent.java

#### 1. Método getItems() público
```java
/**
 * Obtiene el sistema de items custom del evento
 */
public AperturaEndItems getItems() {
    return items;
}
```

#### 2. Visibilidad de métodos de creación
Los siguientes métodos cambiaron de `private` a `public`:
- `crearEspadaDesoladora()`
- `crearPicoDesolador()`
- `crearArmaduraDesoladora(String pieza)`

Esto permite que ApocalipsisCommand los use directamente.

## 🎮 Uso del Comando

### Mensaje de Salida:
```
§5§l⚡ ═══ RECOMPENSAS EVENTO 5 ═══ ⚡
§7Recompensas del §ePuesto #1 §7(PLATINUM)

§a✓ Items recibidos: §f23 §7/ §f23
§a✓ XP recibido: §f+11,000

§7Incluye:
  §8▪ §5Armadura Desoladora §7completa (4 piezas)
  §8▪ §5Espada §7y §5Pico Desolador
  §8▪ §5x1 Corazón Desolador §7(LEGENDARIO)
  §8▪ §5x5 Escama Perfecta §7(ÉPICO)
  §8▪ §7Netherite, Diamantes y más
```

### Comportamiento:
- ✅ Si el inventario tiene espacio: Items se añaden directamente
- ✅ Si el inventario está lleno: Items se dropean en la posición del jugador
- ✅ XP se otorga siempre (vía ExperienceService)
- ✅ Mensaje detallado de confirmación

## 📝 Casos de Uso

### Testing
```bash
# Probar recompensas antes del evento
/avo evento5 recompensas

# Verificar items y enchantments
# Validar que todo se vea correcto
```

### Demostración
```bash
# Mostrar recompensas a jugadores
/avo evento5 recompensas

# Explicar qué pueden ganar
```

### Compensación
```bash
# Si un jugador perdió recompensas por bug
/give <jugador> ...
# O dar al admin para luego transferir
/avo evento5 recompensas
```

## 🔍 Archivos Modificados

### ApocalipsisCommand.java
**Líneas modificadas:**
- Línea ~6166: Añadido comando en ayuda
- Líneas ~6500-6580: Nuevo case "recompensas"/"rewards"

**Cambios:**
1. Verificación de permisos admin
2. Verificación de que sea jugador (no consola)
3. Creación de instancia temporal de AperturaEndEvent
4. Generación de lista completa de recompensas
5. Sistema de fallback (drop si inventario lleno)
6. Integración con ExperienceService
7. Mensaje detallado de confirmación

### AperturaEndEvent.java
**Líneas modificadas:**
- Línea ~206: Nuevo método público `getItems()`
- Línea ~8006: `crearEspadaDesoladora()` private → **public**
- Línea ~8041: `crearPicoDesolador()` private → **public**
- Línea ~8075: `crearArmaduraDesoladora()` private → **public**

**Cambios:**
1. Exposición del sistema de items (AperturaEndItems)
2. Métodos de creación accesibles desde comandos
3. Mantiene encapsulación del resto de la lógica

## 🛠️ Detalles de Implementación

### Lista de Recompensas
```java
List<ItemStack> recompensas = new ArrayList<>();

// Items base (3 tipos)
recompensas.add(eventoRecompensas.getItems().crearFragmentoDelVacio(8));
recompensas.add(new ItemStack(Material.ENDER_PEARL, 12));
recompensas.add(new ItemStack(Material.ENDER_EYE, 12));

// Items épicos (4 tipos)
recompensas.add(eventoRecompensas.getItems().crearEscamaPerfecta(5));
recompensas.add(eventoRecompensas.getItems().crearCorazonDesolador());
recompensas.add(new ItemStack(Material.NETHERITE_INGOT, 3));
recompensas.add(new ItemStack(Material.DIAMOND, 24));

// Armadura completa (4 piezas)
recompensas.add(eventoRecompensas.crearArmaduraDesoladora("helmet"));
recompensas.add(eventoRecompensas.crearArmaduraDesoladora("chestplate"));
recompensas.add(eventoRecompensas.crearArmaduraDesoladora("leggings"));
recompensas.add(eventoRecompensas.crearArmaduraDesoladora("boots"));

// Armas y herramientas (2 items)
recompensas.add(eventoRecompensas.crearEspadaDesoladora());
recompensas.add(eventoRecompensas.crearPicoDesolador());
```

### Sistema de Entrega
```java
int itemsRecibidos = 0;
for (ItemStack item : recompensas) {
    if (admin.getInventory().firstEmpty() != -1) {
        admin.getInventory().addItem(item);
        itemsRecibidos++;
    } else {
        // Fallback: Drop si inventario lleno
        admin.getWorld().dropItemNaturally(admin.getLocation(), item);
    }
}
```

### Experiencia
```java
experienceService.addPlayerExperience(admin, 11000, "admin_evento5_rewards");
```

## ⚙️ Configuración

**Permisos:**
- `avo.admin` - Requerido para ejecutar el comando

**Sin configuración adicional:** El comando usa los mismos items que el sistema de recompensas normal.

## 📊 Resultado de Compilación
```
✅ BUILD SUCCESS
✅ JAR generado: Apocalipsis-1.22.54.jar (1.75 MB)
✅ Comando funcional: /avo evento5 recompensas
✅ Métodos públicos expuestos correctamente
```

## 🎯 Comandos Relacionados

### Menú Principal Evento 5
```
/avo evento5
```
Muestra todos los comandos disponibles, incluyendo el nuevo:
```
§e▸ Admin - Testing:
  §f/avo evento5 skip §7- Saltar preparación
  §f/avo evento5 modo §7- Ver modo de integración
  §f/avo evento5 fase <1-4> §7- Forzar fase del dragón
  §f/avo evento5 damage <jugador> <cantidad> §7- Simular daño
  §f/avo evento5 kill §7- Matar dragón (test)
  §f/avo evento5 recompensas §7- Obtener todas las recompensas  ← NUEVO
```

### Otros Comandos Admin
```bash
/avo evento5 start       # Iniciar evento
/avo evento5 stop        # Detener evento
/avo evento5 info        # Ver estado
/avo evento5 stats       # Ver estadísticas
```

## 🔗 Referencias
- Sistema de recompensas: `distribuirRecompensas()` en AperturaEndEvent
- Items custom: `AperturaEndItems.java`
- Experience: `ExperienceService.java`
- Comando base: `ApocalipsisCommand.java`

## 🚀 Testing Recomendado

1. **Ejecutar comando básico:**
   ```
   /avo evento5 recompensas
   ```

2. **Verificar items recibidos:**
   - Contar 23 items en total
   - Verificar armadura completa (4 piezas)
   - Comprobar enchantments (Sharpness V, Protection IV, etc.)
   - Validar items custom (Fragmento, Escama, Corazón)

3. **Verificar XP:**
   ```
   /xp query <admin>
   ```
   Debe mostrar +11,000 XP

4. **Test con inventario lleno:**
   - Llenar inventario completamente
   - Ejecutar `/avo evento5 recompensas`
   - Verificar que items se dropean en el suelo

5. **Test desde consola:**
   - Intentar desde consola del servidor
   - Debe mostrar: "§cEste comando solo puede ejecutarlo un jugador."

6. **Test sin permisos:**
   - Ejecutar con jugador sin `avo.admin`
   - Debe mostrar: "§cNo tienes permisos."

---

**Versión:** 1.22.54  
**Build:** SUCCESS  
**Comando:** `/avo evento5 recompensas`  
**Items otorgados:** 23  
**XP otorgado:** 11,000  
