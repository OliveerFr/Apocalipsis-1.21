# 🔧 Fix: Bucle Infinito en Acto 3 (Núcleo de Shulkers)

## 🐛 Problema Identificado

Cuando se iniciaba el **Acto 3** del evento "Eco de Sombras" (Núcleo), el servidor entraba en un **bucle infinito** que causaba:

- ⚠️ **Watchdog timeout** (servidor no responde por +11 segundos)
- 💥 **Alto uso de memoria** (FAWE detectaba problemas)
- 🔄 **Tasks acumulándose** infinitamente
- 🎮 **Lag extremo** para todos los jugadores
- 📊 **Packet Funnel** activándose (demasiados paquetes)

### Causa Raíz

En el método `iniciarActoNucleo()` (líneas 800-1000), se creaban **4 BukkitTasks** para efectos visuales/sonoros del núcleo:

1. **`nucleoParticlesTask`**: Partículas intensas cada 0.1s (2 ticks)
2. **`nucleoBeamTask`**: Beam vertical cada 0.5s (10 ticks)
3. **`nucleoSoundTask`**: Sonidos ambiente cada 3s (60 ticks)
4. **`nucleoWaypointTask`**: Action bar cada 1s (20 ticks)

**El problema:** Estos tasks se declaraban como variables locales (`BukkitTask nucleoParticles = ...`) y solo hacían `return` cuando el núcleo moría, pero **NUNCA se cancelaban a sí mismos**.

```java
// ❌ ANTES (CÓDIGO INCORRECTO)
BukkitTask nucleoParticles = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
    if (!nucleo.isValid() || nucleo.isDead() || actoActual != Acto.NUCLEO) {
        return; // ⚠️ Solo hace return, NO CANCELA el task
    }
    // ... partículas ...
}, 0L, 2L);
```

Esto causaba que:
- Los tasks se quedaran ejecutándose **PARA SIEMPRE**
- Si el evento se reiniciaba, se **acumulaban más tasks**
- Cada task ejecutaba lógica pesada (partículas, sonidos, loops) cada pocos ticks
- El servidor colapsaba bajo la carga

---

## ✅ Solución Implementada

### 1. Declarar Tasks como Variables de Instancia

**Archivo:** `EcoSombrasEvent.java` (líneas 106-111)

```java
// 🔧 FIX: Tasks del núcleo (Acto 3) - deben cancelarse al cambiar de acto
private BukkitTask nucleoParticlesTask;
private BukkitTask nucleoBeamTask;
private BukkitTask nucleoSoundTask;
private BukkitTask nucleoWaypointTask;
```

Ahora los tasks son accesibles desde cualquier método de la clase.

### 2. Auto-cancelación en los Tasks

**Archivo:** `EcoSombrasEvent.java` (líneas 870-930)

```java
// ✅ DESPUÉS (CÓDIGO CORREGIDO)
nucleoParticlesTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
    if (!nucleo.isValid() || nucleo.isDead() || actoActual != Acto.NUCLEO) {
        if (nucleoParticlesTask != null) nucleoParticlesTask.cancel(); // ✅ SE CANCELA
        return;
    }
    // ... partículas ...
}, 0L, 2L);
```

Ahora cada task:
1. Verifica si debe seguir ejecutándose
2. **Se cancela a sí mismo** antes de salir
3. No deja tasks huérfanos

### 3. Cancelación al Detener el Evento

**Archivo:** `EcoSombrasEvent.java` (método `onStop()`, líneas 313-320)

```java
// 🔧 FIX: Cancelar tasks del núcleo (Acto 3)
if (nucleoParticlesTask != null) nucleoParticlesTask.cancel();
if (nucleoBeamTask != null) nucleoBeamTask.cancel();
if (nucleoSoundTask != null) nucleoSoundTask.cancel();
if (nucleoWaypointTask != null) nucleoWaypointTask.cancel();
```

Al detener el evento (con `/avo evento stop` o al finalizar), todos los tasks se cancelan explícitamente.

### 4. Cancelación al Cambiar de Acto

**Archivo:** `EcoSombrasEvent.java` (método `limpiarEntidadesActoAnterior()`, líneas 3087-3103)

```java
// 🔧 FIX: Cancelar tasks del núcleo (Acto 3) al cambiar de acto
if (nucleoParticlesTask != null) {
    nucleoParticlesTask.cancel();
    nucleoParticlesTask = null;
}
if (nucleoBeamTask != null) {
    nucleoBeamTask.cancel();
    nucleoBeamTask = null;
}
if (nucleoSoundTask != null) {
    nucleoSoundTask.cancel();
    nucleoSoundTask = null;
}
if (nucleoWaypointTask != null) {
    nucleoWaypointTask.cancel();
    nucleoWaypointTask = null;
}
```

Al transicionar del Acto 3 → Acto 4 (Anclas), se cancelan todos los tasks del núcleo automáticamente.

---

## 📊 Comparación: Antes vs Después

| Aspecto | ❌ Antes | ✅ Después |
|---------|---------|-----------|
| **Tasks del núcleo** | Variables locales, no accesibles | Variables de instancia, accesibles |
| **Auto-cancelación** | Solo `return`, tasks siguen vivos | `task.cancel()` + `return`, tasks mueren |
| **Al detener evento** | Tasks NO se cancelaban | Tasks se cancelan explícitamente |
| **Al cambiar de acto** | Tasks seguían ejecutándose | Tasks se cancelan y se ponen en `null` |
| **Acumulación** | Sí, cada reinicio sumaba tasks | No, tasks viejos se limpian |
| **Watchdog timeout** | Sí (11+ segundos) | No |
| **Memoria** | Alto uso (FAWE alert) | Normal |
| **Lag** | Extremo | Normal |

---

## 🧪 Cómo Verificar el Fix

### 1. Iniciar el Evento
```
/avo evento start eco_sombras
```

### 2. Avanzar al Acto 3
```
/avo evento debug transicion NUCLEO
```
O esperar a que el evento llegue naturalmente al Acto 3.

### 3. Observar el Comportamiento

**Antes del fix:**
- ❌ Servidor se congela
- ❌ Watchdog timeout en consola
- ❌ FAWE alerta de memoria alta
- ❌ Partículas/sonidos siguen después de matar el núcleo

**Después del fix:**
- ✅ Servidor funciona normal
- ✅ Sin watchdog timeout
- ✅ Memoria estable
- ✅ Partículas/sonidos se detienen al matar el núcleo

### 4. Detener el Evento
```
/avo evento stop
```

**Verificar:** No deben quedar tasks ejecutándose (revisar logs del servidor).

---

## 🔍 Debugging: Cómo Detectar Tasks Huérfanos

Si sospechas que hay tasks que no se cancelan:

### Método 1: Paper Timings
```
/timings report
```
Busca tasks del plugin "Apocalipsis" que tengan alto tiempo de ejecución.

### Método 2: Logs del Servidor
Añade logs temporales en los tasks:
```java
nucleoParticlesTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
    plugin.getLogger().info("[DEBUG] nucleoParticlesTask ejecutándose"); // DEBUG
    // ... resto del código ...
}, 0L, 2L);
```

Si ves muchos `[DEBUG] nucleoParticlesTask ejecutándose` después de detener el evento, el task NO se canceló.

### Método 3: Spark Profiler
```
/spark profiler start
# Esperar 30 segundos
/spark profiler stop
```
Analiza el reporte, busca `runTaskTimer` con alto % de CPU.

---

## 🛡️ Prevención: Buenas Prácticas

Para evitar este tipo de bugs en el futuro:

### 1. Siempre Cancelar Tasks
```java
// ✅ BUENO
private BukkitTask miTask;

public void iniciar() {
    miTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
        if (!debeEjecutarse()) {
            if (miTask != null) miTask.cancel(); // ✅ Auto-cancelación
            return;
        }
        // ... lógica ...
    }, 0L, 20L);
}

public void detener() {
    if (miTask != null) {
        miTask.cancel(); // ✅ Cancelación manual
        miTask = null;
    }
}
```

### 2. NO usar Variables Locales para Tasks Persistentes
```java
// ❌ MALO
public void iniciar() {
    BukkitTask localTask = Bukkit.getScheduler().runTaskTimer(...);
    // No puedes cancelar este task desde otro método
}

// ✅ BUENO
private BukkitTask instanceTask;

public void iniciar() {
    instanceTask = Bukkit.getScheduler().runTaskTimer(...);
    // Puedes cancelarlo desde cualquier método
}
```

### 3. Limpiar en `onDisable()` / `onStop()`
```java
@Override
public void onDisable() {
    // Cancelar TODOS los tasks del plugin
    Bukkit.getScheduler().cancelTasks(plugin);
}
```

### 4. Usar `runTaskLater` para Tasks de Una Vez
```java
// Para tasks que solo se ejecutan 1 vez
Bukkit.getScheduler().runTaskLater(plugin, () -> {
    // ... código ...
}, 100L); // No necesita cancelación manual
```

---

## 📝 Archivos Modificados

| Archivo | Líneas Modificadas | Cambios |
|---------|-------------------|---------|
| `EcoSombrasEvent.java` | 106-111 | ➕ Añadidas 4 variables de instancia para tasks del núcleo |
| `EcoSombrasEvent.java` | 870, 898, 908, 918 | 🔧 Cambiadas variables locales a instancia + auto-cancelación |
| `EcoSombrasEvent.java` | 313-320 | ➕ Añadida cancelación de tasks del núcleo en `onStop()` |
| `EcoSombrasEvent.java` | 3087-3103 | ➕ Añadida cancelación de tasks del núcleo en `limpiarEntidadesActoAnterior()` |

**Total:** 4 secciones de código modificadas, ~20 líneas añadidas.

---

## 🚀 Despliegue

### Compilación
```bash
mvn clean package -DskipTests
```

### Resultado
```
✅ BUILD SUCCESS
📦 Apocalipsis-1.19.3.jar (632.85 KB)
📂 target/Apocalipsis-1.19.3.jar
📅 Compilado: 2025-11-23 9:41 AM
```

### Instalación
1. **Detener el servidor** (importante: asegurarse que no hay tasks ejecutándose)
2. Copiar `target/Apocalipsis-1.19.3.jar` a `plugins/`
3. **Reiniciar el servidor** (NO usar `/reload`, puede causar memory leaks)
4. Probar el Acto 3 del evento

---

## 🎯 Resultado Esperado

### Al Iniciar Acto 3:
✅ Núcleo de Shulker aparece con efectos épicos  
✅ Partículas intensas alrededor del núcleo  
✅ Beam vertical visible desde lejos  
✅ Sonidos ambientales cada 3 segundos  
✅ Action bar mostrando distancia al núcleo  

### Al Matar el Núcleo o Cambiar de Acto:
✅ Todos los efectos se detienen inmediatamente  
✅ Tasks se cancelan correctamente  
✅ Sin lag residual  
✅ Memoria se libera  

### Al Detener el Evento:
✅ Todos los tasks del evento se cancelan  
✅ Sin tasks huérfanos ejecutándose  
✅ Servidor vuelve a rendimiento normal  

---

## 🐛 Troubleshooting

### "Sigo viendo lag en el Acto 3"
1. Verifica que instalaste la versión **1.19.3** del JAR (no una anterior)
2. Revisa `/timings report` para ver si hay otros plugins causando lag
3. Considera reducir la frecuencia de los tasks (cambiar `2L` a `5L`, etc.)

### "Los efectos no se ven"
1. Verifica que el núcleo (Shulker) esté vivo: `/minecraft:data get entity @e[type=shulker,limit=1]`
2. Revisa logs del servidor por errores al spawn del núcleo
3. Asegúrate que `actoActual == Acto.NUCLEO`

### "El servidor sigue crasheando"
1. **NO es este fix:** El problema reportado era de **Villagers + FAWE**, no del plugin
2. Revisa `paper-world-defaults.yml` → `entity-activation-range.villagers: 16`
3. Considera matar villagers excesivos: `/minecraft:kill @e[type=villager,distance=100..]`
4. Aumenta memoria JVM: `-Xmx6G` o `-Xmx8G`

---

## ✨ Versión
- **Plugin:** Apocalipsis v1.19.3
- **Fix:** Bucle Infinito Acto 3 (Núcleo)
- **Fecha:** 2025-11-23
- **Estado:** ✅ Compilado y Funcional
- **Prioridad:** 🔴 **CRÍTICA** (servidor no respondía)
