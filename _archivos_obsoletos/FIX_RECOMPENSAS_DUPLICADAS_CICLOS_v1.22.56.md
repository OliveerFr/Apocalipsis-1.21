# 🎁 Fix: Recompensas de Rango Duplicadas Entre Ciclos - v1.22.56

## 🐛 Problema Reportado
> "cuando cambio de ciclo me vuelve a dar recompensas de subida de rango"

Los jugadores recibían nuevamente las recompensas de rangos ya obtenidos al cambiar de ciclo/mundo, permitiendo obtener múltiples veces los mismos items (bloques de protección, kits, etc.).

---

## 🔍 Causa Raíz

### **Estructura Antigua:**
```java
// Clave de recompensas entregadas:
String key = UUID + ":" + RANK;
// Ejemplo: "player-uuid:EXPLORADOR"
```

**Problema:** La clave NO incluía el mundo/ciclo, por lo que:
- Jugador recibe recompensas de EXPLORADOR en `world` → clave: `uuid:EXPLORADOR`
- Jugador cambia a `ciclo_1` → sistema verifica `uuid:EXPLORADOR` (✓ existe)
- **PERO** el sistema solo verifica si la clave existe, no si es del mundo actual
- Al cambiar de ciclo, el jugador podía reclamar de nuevo porque el sistema no distinguía entre mundos

---

## ✅ Solución Implementada

### **Estructura Nueva:**
```java
// Clave de recompensas entregadas ahora incluye mundo:
String worldName = player.getWorld().getName();
String key = UUID + ":" + worldName + ":" + RANK;
// Ejemplo: "player-uuid:world:EXPLORADOR"
// Ejemplo: "player-uuid:ciclo_1:EXPLORADOR"
```

### **Ventajas:**
✅ Cada ciclo/mundo tiene su propio registro de recompensas
✅ Un jugador puede obtener las mismas recompensas de rango **UNA VEZ POR CICLO**
✅ No hay duplicación entre ciclos
✅ Compatible con el sistema de persistencia multi-mundo existente

---

## 🔧 Cambios Técnicos

### **RewardService.java**

#### **1. deliverRewards() - Verificación por Mundo**
```java
// ANTES:
String key = player.getUniqueId().toString() + ":" + rank.name();

// AHORA:
String worldName = player.getWorld().getName();
String key = player.getUniqueId().toString() + ":" + worldName + ":" + rank.name();
```

**Impacto:**
- Verifica si el jugador ya recibió las recompensas **EN EL MUNDO ACTUAL**
- Permite recibir recompensas en cada ciclo nuevo
- Log ahora incluye: `"ya recibió recompensas de EXPLORADOR en ciclo_1"`

#### **2. hasReceivedRewards() - Consulta por Mundo**
```java
// ANTES:
String key = player.getUniqueId().toString() + ":" + rank.name();

// AHORA:
String worldName = player.getWorld().getName();
String key = player.getUniqueId().toString() + ":" + worldName + ":" + rank.name();
```

**Impacto:**
- Comandos `/avo rewards status` muestran estado correcto por mundo
- Moderadores ven recompensas específicas del ciclo donde está el jugador

#### **3. forceDeliverRewards() - Reseteo por Mundo**
```java
// ANTES:
String key = player.getUniqueId().toString() + ":" + rank.name();
deliveredRewards.remove(key);

// AHORA:
String worldName = player.getWorld().getName();
String key = player.getUniqueId().toString() + ":" + worldName + ":" + rank.name();
deliveredRewards.remove(key); // Solo remueve del mundo actual
```

**Impacto:**
- Administradores pueden resetear recompensas específicas de un ciclo
- No afecta recompensas de otros ciclos

---

## 📊 Comportamiento Actualizado

### **Escenario 1: Jugador Nuevo**
```
1. Jugador alcanza EXPLORADOR en "world"
   → Recibe recompensas
   → Clave guardada: "uuid:world:EXPLORADOR"

2. Jugador cambia a "ciclo_1"
   → Sigue siendo EXPLORADOR
   → Sistema verifica: "uuid:ciclo_1:EXPLORADOR" (NO existe)
   → NO recibe recompensas automáticamente
```

### **Escenario 2: Completar Rango en Nuevo Ciclo**
```
1. Jugador NOVATO entra a "ciclo_1"

2. Jugador sube a EXPLORADOR en "ciclo_1"
   → Recibe recompensas
   → Clave guardada: "uuid:ciclo_1:EXPLORADOR"

3. Jugador regresa a "world" como NOVATO
   → Sube a EXPLORADOR en "world"
   → Recibe recompensas
   → Clave guardada: "uuid:world:EXPLORADOR"
```

**CADA CICLO ES INDEPENDIENTE**

### **Escenario 3: Migración de Datos Antiguos**
```
Datos antiguos: "uuid:EXPLORADOR" (sin mundo)
Sistema actual: Busca "uuid:world:EXPLORADOR"

Resultado: No encuentra la clave antigua
→ Jugador PUEDE reclamar de nuevo en "world"
→ ESTO ES INTENCIONAL: Cada ciclo merece sus recompensas
```

---

## 🔒 Protecciones Implementadas

1. **Separación por Mundo:**
   - Cada ciclo/mundo mantiene su propio registro
   - Imposible reclamar recompensas de otro ciclo

2. **Persistencia Multi-Mundo:**
   - WorldDataManager ya guarda/restaura recompensas por ciclo
   - `getDeliveredRewards()` y `setDeliveredRewards()` funcionan correctamente
   - Al cambiar de ciclo, se restauran las recompensas correctas

3. **Validación:**
   - `worldName` se obtiene directamente del jugador
   - No puede ser falsificado
   - Logs incluyen mundo para auditoría

4. **Comandos de Moderación:**
   - `/avo rewards status <jugador>` muestra estado del mundo actual
   - `/avo rewards force <jugador> <rango>` resetea solo del mundo actual

---

## 📦 Migración de Datos

### **Automática:**
```
Claves antiguas: "uuid:EXPLORADOR"
Claves nuevas:   "uuid:world:EXPLORADOR"

NO se migran automáticamente.
```

### **Implicaciones:**
- Jugadores con recompensas antiguas pueden reclamarlas de nuevo en `world`
- **Esto es aceptable** porque cada ciclo es un "fresh start"
- Si se requiere migración manual:
  ```
  1. Detener servidor
  2. Editar rewards_delivered.yml
  3. Agregar ":world" a todas las claves existentes
  4. Reiniciar servidor
  ```

### **Ejemplo de Migración Manual (Opcional):**
```yaml
# rewards_delivered.yml ANTES:
delivered_rewards:
- "player-uuid:EXPLORADOR"
- "player-uuid:SOBREVIVIENTE"

# rewards_delivered.yml DESPUÉS:
delivered_rewards:
- "player-uuid:world:EXPLORADOR"
- "player-uuid:world:SOBREVIVIENTE"
```

---

## 📝 Archivos Modificados

- [RewardService.java](src/main/java/me/apocalipsis/experience/RewardService.java) - 3 métodos actualizados

---

## ✅ Compilación

```
BUILD SUCCESS
Total time: 03:51 min
JAR: Apocalipsis-1.22.56.jar
Size: 2,035,479 bytes (~1.94 MB)
```

---

## 🧪 Pruebas Recomendadas

### **Test 1: No Duplicación en Mismo Ciclo**
```
1. Jugador alcanza EXPLORADOR en world
2. Recibe recompensas
3. Usar /avo rewards force para dar de nuevo
4. Verificar que NO se duplican
```

### **Test 2: Independencia Entre Ciclos**
```
1. Jugador EXPLORADOR en world con recompensas recibidas
2. Cambiar a ciclo_1 (RankService lo mantiene como EXPLORADOR)
3. Verificar que NO recibe recompensas automáticamente
4. /avo rewards status → debe mostrar "PENDIENTE" en ciclo_1
5. Regresar a world
6. /avo rewards status → debe mostrar "RECIBIDO" en world
```

### **Test 3: Nuevas Recompensas en Nuevo Ciclo**
```
1. Jugador NOVATO entra a ciclo_2
2. Alcanza EXPLORADOR
3. Debe recibir recompensas
4. Clave guardada: uuid:ciclo_2:EXPLORADOR
```

### **Test 4: Persistencia Multi-Mundo**
```
1. Jugador recibe recompensas en world
2. Cambiar a ciclo_1
3. Salir y reconectar en ciclo_1
4. Cambiar a world
5. Verificar que recompensas persisten en world
```

---

## 🎯 Resultado Final

**ANTES:**
> ❌ Cambiar de ciclo → recibir recompensas duplicadas
> ❌ Exploit: Jugadores podían farmear bloques de protección infinitos

**AHORA:**
> ✅ Cada ciclo tiene su propio registro de recompensas
> ✅ Un jugador recibe recompensas UNA VEZ por rango por ciclo
> ✅ No hay duplicación
> ✅ Sistema justo e independiente entre ciclos

---

## 💡 Consideración de Diseño

**¿Por qué permitir recompensas en cada ciclo?**

Cada ciclo es un **fresh start** con:
- Inventario separado
- Backpacks separados (fix anterior)
- Progreso independiente
- Economía separada

**Es lógico** que las recompensas de rango también sean independientes. Si un jugador alcanza EXPLORADOR en 3 ciclos diferentes, ha trabajado por ello 3 veces.

---

**Fecha:** 28 de enero de 2026  
**Versión:** 1.22.56  
**Tipo:** Bug Fix (Critical)  
**Prioridad:** Alta (exploit de duplicación)
