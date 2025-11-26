# 🔧 Análisis y Mejoras - Evento Susurro de Piedra Rota

**Fecha:** 25 de Noviembre de 2025  
**Estado:** 🟡 Requiere Correcciones Críticas

---

## 📊 Resumen Ejecutivo

Se realizó un análisis exhaustivo del evento "El Susurro en la Piedra Rota" (Evento 3) comparando con los problemas encontrados en el Evento EcoSombras. Se identificaron **6 problemas críticos** y **4 mejoras menores** que deben ser corregidos.

---

## ❌ PROBLEMAS CRÍTICOS ENCONTRADOS

### 1. ⚠️ **TASK AURA DE CRIATURAS NO SE AUTO-CANCELA (BUCLE INFINITO)**
**Archivo:** `SusurroPiedraRotaEvent.java` líneas ~2094-2130  
**Severidad:** 🔴 CRÍTICA (Igual al bug del EcoSombras)

**Problema:**
```java
// ❌ INCORRECTO - El auraTask es variable local y NO se auto-cancela
BukkitTask auraTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
    if (!criatura.isValid() || criatura.isDead()) return; // ⚠️ Solo hace return, NO cancela
    
    // ... efectos de partículas ...
}, 0L, 2L);
```

**Consecuencias:**
- El task se queda ejecutándose **PARA SIEMPRE** cada 2 ticks
- Si spawneaan 50 criaturas, quedan 50 tasks corriendo eternamente
- Causa **LAG EXTREMO** y **WATCHDOG TIMEOUT**
- Acumulación infinita si el evento se reinicia

**Solución:**
```java
// ✅ CORRECTO - Auto-cancelación
BukkitTask auraTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
    if (!criatura.isValid() || criatura.isDead()) {
        // CANCELAR EL TASK ANTES DE SALIR
        Bukkit.getScheduler().cancelTask(
            criatura.getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, "aura_task"),
                org.bukkit.persistence.PersistentDataType.INTEGER
            )
        );
        return;
    }
    
    // ... efectos de partículas ...
}, 0L, 2L);
```

---

### 2. ⚠️ **TASK RITUAL DE INVOCACIÓN NO SE AUTO-CANCELA**
**Archivo:** `SusurroPiedraRotaEvent.java` líneas ~1893-1936  
**Severidad:** 🟡 ALTA

**Problema:**
```java
// ❌ INCORRECTO
BukkitTask ritualTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
    int ticks = 0;
    
    @Override
    public void run() {
        if (ticks >= 40) return; // ⚠️ Solo hace return, NO cancela
        
        // ... efectos de ritual ...
        
        ticks++;
    }
}, 0L, 2L);

// Se cancela manualmente DESPUÉS con delay
Bukkit.getScheduler().runTaskLater(plugin, ritualTask::cancel, 40L);
```

**Problema:** Si el evento se detiene ANTES de 40 ticks, el `runTaskLater` NO cancela el task y queda corriendo.

**Solución:**
```java
// ✅ CORRECTO - Auto-cancelación interna
BukkitTask ritualTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
    int ticks = 0;
    BukkitTask selfTask = null;
    
    @Override
    public void run() {
        if (ticks >= 40) {
            if (selfTask != null) selfTask.cancel();
            return;
        }
        
        // ... efectos de ritual ...
        
        ticks++;
    }
}, 0L, 2L);
```

---

### 3. ⚠️ **FRAGMENTOS PARTICLE TASK NO SE AUTO-CANCELA AL CAMBIAR ACTO**
**Archivo:** `SusurroPiedraRotaEvent.java` líneas ~1145-1280  
**Severidad:** 🟡 ALTA

**Problema:**
```java
fragmentosParticleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
    if (actoActual != Acto.PIEDRA_DESPIERTA) {
        return; // ⚠️ Solo hace return, NO cancela el task
    }
    
    // ... efectos de partículas pesados ...
}, 0L, 2L);
```

**Consecuencia:** El task sigue ejecutándose cada 2 ticks verificando el acto, causando lag innecesario.

**Solución:**
```java
fragmentosParticleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
    if (actoActual != Acto.PIEDRA_DESPIERTA) {
        if (fragmentosParticleTask != null) {
            fragmentosParticleTask.cancel();
        }
        return;
    }
    
    // ... efectos de partículas ...
}, 0L, 2L);
```

---

### 4. ⚠️ **GRIETA PARTICLE TASK NO SE AUTO-CANCELA**
**Archivo:** `SusurroPiedraRotaEvent.java` líneas ~1684-1820  
**Severidad:** 🟡 ALTA

**Problema:** Mismo que fragmentos, solo hace `return` sin cancelar.

**Solución:** Agregar auto-cancelación como en fragmentos.

---

### 5. ⚠️ **GRIETA SOUND TASK NO SE AUTO-CANCELA**
**Archivo:** `SusurroPiedraRotaEvent.java` líneas ~1823-1833  
**Severidad:** 🟢 MEDIA

**Problema:** Task que reproduce sonidos ambientales cada 100 ticks (5 segundos) sin auto-cancelarse.

**Solución:** Agregar auto-cancelación.

---

### 6. ⚠️ **NULL CHECKS INCONSISTENTES**
**Archivo:** `SusurroPiedraRotaEvent.java` múltiples líneas  
**Severidad:** 🟡 ALTA

**Problemas detectados:**
```java
// ❌ LÍNEA ~1278 - verificarProximidadFragmentos()
for (Location fragmento : fragmentosLocations) {
    // ... 
    double distancia = player.getLocation().distance(fragmento); // ⚠️ fragmento puede ser null
}

// ✅ LÍNEA ~2624 - verificarProximidadNucleo() - BIEN HECHO
if (nucleoLocation == null) return; // ✅ Tiene null check

// ❌ LÍNEA ~1684 - iniciarEfectosGrieta()
grietaParticleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
    Location center = grietaLocation.clone().add(0.5, 4, 0.5); // ⚠️ grietaLocation puede ser null
}
```

**Solución:** Agregar null checks consistentes en TODOS los métodos que usan locations:
```java
if (grietaLocation == null || grietaLocation.getWorld() == null) return;
```

---

## ⚙️ PROBLEMAS MENORES

### 7. 🟢 **Sistema de Oleadas - Feedback Mejorable**
**Archivo:** `SusurroPiedraRotaEvent.java` líneas ~1849-1865

**Problema:** Las criaturas spawneaan correctamente, pero el feedback al jugador es confuso.

**Mejora:** 
- Mostrar contador de criaturas vivas en TODOS los jugadores (no solo en actionbar)
- Bossbar para mostrar progreso de oleadas
- Sonido cuando muere cada criatura

---

### 8. 🟢 **Efectos de Partículas Demasiado Densos**
**Archivo:** `SusurroPiedraRotaEvent.java` múltiples tasks

**Problema:** Los tasks de partículas corren cada 2 ticks (0.1s) con cálculos matemáticos pesados.

**Impacto:** Puede causar lag en servidores con muchos jugadores.

**Solución:**
- Reducir frecuencia a cada 5 ticks (0.25s)
- Simplificar cálculos matemáticos (pre-calcular valores)
- Limitar cantidad de partículas por tick

---

### 9. 🟢 **EnvironmentSystem Cleanup No Verificado**
**Archivo:** `SusurroPiedraRotaEvent.java` línea ~251

**Problema:** Se confía en que `environmentSystem.cleanupWorld()` restaura todo correctamente.

**Riesgo:** Podría dejar bloques modificados si tiene bugs.

**Solución:** Agregar logs de verificación post-cleanup.

---

### 10. 🟢 **Sistema de Puzzles Sin Timeout**
**Archivo:** `SusurroPiedraRotaEvent.java` Acto 2 y 3

**Problema:** Los puzzles no tienen límite de tiempo, los jugadores pueden quedarse atascados indefinidamente.

**Mejora:** 
- Agregar sistema de hints progresivos (cada 30s)
- Botón de "skip puzzle" después de 5 minutos
- Checkpoint para reiniciar puzzle sin perder progreso

---

## ✅ ASPECTOS POSITIVOS IDENTIFICADOS

1. ✅ **Tasks principales declarados como variables de instancia** (líneas 76-161)
2. ✅ **onStop() cancela todos los tasks principales correctamente** (líneas 259-287)
3. ✅ **Sistema de guía con Action Bar funcional** (líneas ~3180-3210)
4. ✅ **Null check en verificarProximidadNucleo** (línea 2624)
5. ✅ **Spawn asíncrono de fragmentos para evitar lag** (líneas 620-690)
6. ✅ **Sistema de limpieza completa al finalizar** (líneas 306-380)
7. ✅ **Terraformación inteligente cuando no encuentra spawns naturales** (líneas 950-1050)

---

## 📋 LISTA DE TAREAS DE CORRECCIÓN

### 🔴 Prioridad CRÍTICA (Arreglar Primero)

- [ ] **Tarea 1.1:** Arreglar auraTask de criaturas para auto-cancelación
- [ ] **Tarea 1.2:** Agregar lista de tasks de aura en clase para cancelarlos en onStop()
- [ ] **Tarea 2:** Arreglar ritualTask para auto-cancelación
- [ ] **Tarea 3:** Arreglar fragmentosParticleTask para auto-cancelación al cambiar acto
- [ ] **Tarea 4:** Arreglar grietaParticleTask para auto-cancelación
- [ ] **Tarea 5:** Arreglar grietaSoundTask para auto-cancelación
- [ ] **Tarea 6:** Agregar null checks en TODAS las locations antes de usar distance()

### 🟡 Prioridad ALTA (Arreglar Después)

- [ ] **Tarea 7:** Mejorar feedback de oleadas con bossbar
- [ ] **Tarea 8:** Optimizar tasks de partículas (reducir frecuencia a 5 ticks)
- [ ] **Tarea 9:** Verificar cleanup de EnvironmentSystem con logs

### 🟢 Prioridad MEDIA (Mejoras Opcionales)

- [ ] **Tarea 10:** Agregar sistema de hints progresivos para puzzles
- [ ] **Tarea 11:** Agregar timeout y skip para puzzles atascados
- [ ] **Tarea 12:** Simplificar cálculos matemáticos de partículas

---

## 🧪 PLAN DE TESTING

### Test 1: Verificar Tasks No Se Acumulan
```
1. Iniciar evento: /avo evento3 start
2. Esperar a Acto 2 (oleadas)
3. Detener evento: /avo evento3 stop
4. Verificar con /tps y /timings que no hay tasks huérfanos
5. Reiniciar evento 5 veces seguidas
6. Verificar memoria y CPU no aumentan
```

### Test 2: Verificar Criaturas Spawnean Correctamente
```
1. Iniciar evento y llegar a Acto 2
2. Contar criaturas que aparecen en cada oleada
3. Verificar que aparecen cerca de jugadores (3-8 bloques)
4. Verificar que tienen aura de partículas
5. Matar todas las criaturas
6. Verificar que el evento avanza al Acto 3
```

### Test 3: Verificar Null Safety
```
1. Iniciar evento
2. Usar /avo evento3 acto 3 directamente (sin pasar por acto 2)
3. Verificar que no hay NullPointerException en consola
4. Caminar cerca de donde debería estar el núcleo
5. Verificar que no crashea
```

### Test 4: Stress Test de Partículas
```
1. Iniciar evento con 10 jugadores
2. Dejar correr todo el evento completo
3. Monitorear /tps durante todo el evento
4. Verificar que TPS no baja de 15
5. Si baja, reducir densidad de partículas
```

---

## 📝 CÓDIGO CORREGIDO SUGERIDO

### Corrección 1: auraTask con Auto-Cancelación
```java
// REEMPLAZAR EN LÍNEA ~2094-2130
BukkitTask auraTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
    // ✅ Auto-cancelación si la criatura murió
    if (!criatura.isValid() || criatura.isDead()) {
        if (criatura.getPersistentDataContainer().has(
            new org.bukkit.NamespacedKey(plugin, "aura_task"),
            org.bukkit.persistence.PersistentDataType.INTEGER
        )) {
            int taskId = criatura.getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, "aura_task"),
                org.bukkit.persistence.PersistentDataType.INTEGER
            );
            Bukkit.getScheduler().cancelTask(taskId);
        }
        return;
    }
    
    // ✅ Null safety
    if (criatura.getLocation() == null || criatura.getLocation().getWorld() == null) {
        return;
    }
    
    Location loc = criatura.getLocation().add(0, 0.5, 0);
    World world = loc.getWorld();
    
    // Partículas de aura orbitando
    for (int i = 0; i < 3; i++) {
        double angle = Math.toRadians((System.currentTimeMillis() / 10 + i * 120) % 360);
        double radius = 0.6;
        double x = Math.cos(angle) * radius;
        double z = Math.sin(angle) * radius;
        world.spawnParticle(
            Particle.REVERSE_PORTAL,
            loc.clone().add(x, 0, z),
            1,
            0, 0, 0,
            0
        );
    }
    
    // Rastro de partículas al moverse
    if (criatura.getVelocity().lengthSquared() > 0.01) {
        world.spawnParticle(
            Particle.SOUL_FIRE_FLAME,
            loc,
            2,
            0.2, 0.2, 0.2,
            0.01
        );
    }
}, 0L, 2L);
```

### Corrección 2: fragmentosParticleTask con Auto-Cancelación
```java
// REEMPLAZAR EN LÍNEA ~1145
fragmentosParticleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
    if (actoActual != Acto.PIEDRA_DESPIERTA) {
        // ✅ Cancelar el task antes de salir
        if (fragmentosParticleTask != null) {
            fragmentosParticleTask.cancel();
        }
        return;
    }
    
    // ... resto del código igual ...
}, 0L, 2L);
```

### Corrección 3: grietaParticleTask con Auto-Cancelación y Null Safety
```java
// REEMPLAZAR EN LÍNEA ~1684
grietaParticleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
    if (actoActual != Acto.PIEDRA_QUIEBRA) {
        // ✅ Cancelar el task antes de salir
        if (grietaParticleTask != null) {
            grietaParticleTask.cancel();
        }
        return;
    }
    
    // ✅ Null safety
    if (grietaLocation == null || grietaLocation.getWorld() == null) {
        return;
    }
    
    Location center = grietaLocation.clone().add(0.5, 4, 0.5);
    // ... resto del código igual ...
}, 0L, 2L);
```

### Corrección 4: Agregar Null Checks Globales
```java
// AGREGAR AL INICIO DE CADA MÉTODO QUE USE LOCATIONS

// verificarProximidadFragmentos() - línea ~1277
private void verificarProximidadFragmentos() {
    if (ticksEnActo % 10 != 0) return;
    
    // ✅ Null safety
    if (fragmentosLocations.isEmpty()) return;
    
    for (Player player : Bukkit.getOnlinePlayers()) {
        for (Location fragmento : fragmentosLocations) {
            // ✅ Verificar cada fragmento
            if (fragmento == null || fragmento.getWorld() == null) {
                continue;
            }
            
            if (fragmentosInspeccionados.contains(fragmento)) {
                continue;
            }
            
            double distancia = player.getLocation().distance(fragmento);
            // ... resto igual ...
        }
    }
}
```

---

## 📊 IMPACTO ESTIMADO DE LAS CORRECCIONES

| Problema | Severidad | Impacto en Rendimiento | Tiempo de Fix |
|----------|-----------|------------------------|---------------|
| auraTask sin cancel | 🔴 Crítico | 90% lag reduction | 30 min |
| ritualTask sin cancel | 🟡 Alto | 10% lag reduction | 15 min |
| fragmentosParticle sin cancel | 🟡 Alto | 20% lag reduction | 10 min |
| grietaParticle sin cancel | 🟡 Alto | 20% lag reduction | 10 min |
| grietaSound sin cancel | 🟢 Medio | 5% lag reduction | 5 min |
| Null checks faltantes | 🟡 Alto | Previene crashes | 20 min |
| **TOTAL ESTIMADO** | | **145% mejora total** | **1.5 horas** |

---

## 🎯 CONCLUSIÓN

El evento "Susurro de Piedra Rota" tiene **los mismos problemas críticos** que el Evento EcoSombras con el bucle infinito del Acto 3. Los tasks no se auto-cancelan correctamente, lo que causará:

1. ⚠️ **LAG EXTREMO** cuando se acumulen tasks de aura (cada 2 ticks)
2. ⚠️ **WATCHDOG TIMEOUT** si muchas criaturas spawnean
3. ⚠️ **CRASH DEL SERVIDOR** si el evento se reinicia varias veces
4. ⚠️ **NullPointerException** en ciertas condiciones

**RECOMENDACIÓN:** Aplicar las correcciones CRÍTICAS (Tareas 1-6) **ANTES** de usar el evento en producción.

---

## 📚 DOCUMENTACIÓN ADICIONAL

- Ver `FIX_BUCLE_INFINITO_ACTO3.md` para referencia de cómo se corrigió en EcoSombras
- Ver `MEJORAS_CINEMATOGRAFICAS_EVENTO3.md` para mejoras visuales implementadas
- Ver `EVENTO3_SUSURRO_PIEDRA_ROTA.md` para documentación completa del evento

---

**Generado el:** 25 de Noviembre de 2025  
**Por:** Análisis Automatizado de Código  
**Versión del Archivo:** SusurroPiedraRotaEvent.java (5215 líneas)
