# 🔧 Fix: Auto-Start con Estado PREPARACION Inválido

## ❌ Problema Identificado

Al iniciar el servidor, el sistema quedaba en estado `PREPARACION` pero con timestamps en `0`, causando que:

1. **Los desastres nunca iniciaban** aunque el contador llegara a 0
2. **El auto-start no se ejecutaba** porque el estado ya era `PREPARACION` (no `DETENIDO`)
3. **El sistema quedaba bloqueado** en un estado inconsistente

### Causa Raíz

El archivo `state.yml` tenía:
```yaml
estado: PREPARACION
start_epoch_ms: 0
end_epoch_ms: 0
```

El código de auto-start solo verificaba:
```java
if (!"ACTIVO".equalsIgnoreCase(estado) && !stateManager.isSafeModeActive())
```

Esto significa:
- ✅ Si estado = `DETENIDO` → auto-start se ejecuta
- ❌ Si estado = `PREPARACION` → auto-start NO se ejecuta (aunque timestamps sean 0)

## ✅ Solución Implementada

### 1️⃣ **Validación Mejorada de Auto-Start**

Se modificó la lógica para verificar:

```java
// Auto-start si:
// 1. NO está en ACTIVO
// 2. NO está en safe mode  
// 3. Los timestamps son inválidos cuando está en PREPARACION

if ("PREPARACION".equalsIgnoreCase(estado)) {
    // Si está en PREPARACION pero timestamps inválidos, reiniciar
    if (startEpoch == 0L || endEpoch == 0L || endEpoch <= now) {
        getLogger().warning("[AutoStart] PREPARACION detectada con timestamps inválidos - reiniciando ciclo");
        needsAutoStart = true;
    }
}
```

### 2️⃣ **Estado Inicial Corregido**

Se cambió el `state.yml` inicial de:
```yaml
estado: PREPARACION  # ❌ Problemático
```

A:
```yaml
estado: DETENIDO     # ✅ Correcto
```

### 3️⃣ **Logging Mejorado**

Ahora el sistema registra claramente qué decisión tomó:

```
[AutoStart] Desastre activo detectado - no se auto-inicia
[AutoStart] Safe mode activo - no se auto-inicia
[AutoStart] PREPARACION válida en curso - no se reinicia
[AutoStart] PREPARACION detectada con timestamps inválidos - reiniciando ciclo
[AutoStart] ✓ Ciclo de desastres iniciado automáticamente (estado: PREPARACION)
```

## 🔄 Flujo de Auto-Start Mejorado

### Escenario 1: Servidor Limpio (Primera Vez)
```
1. state.yml tiene: estado=DETENIDO, timestamps=0
2. Auto-start detecta: estado != ACTIVO
3. Auto-start ejecuta: Inicia PREPARACION con timestamps válidos
4. ✅ Desastres inician correctamente
```

### Escenario 2: PREPARACION con Timestamps Válidos
```
1. state.yml tiene: estado=PREPARACION, timestamps válidos
2. Auto-start detecta: PREPARACION válida en curso
3. Auto-start NO reinicia
4. ✅ Continúa la preparación existente
```

### Escenario 3: PREPARACION con Timestamps Inválidos (BUG)
```
1. state.yml tiene: estado=PREPARACION, timestamps=0
2. Auto-start detecta: Timestamps inválidos
3. Auto-start reinicia: Genera nuevos timestamps
4. ✅ Desastres inician correctamente
```

### Escenario 4: Desastre Activo
```
1. state.yml tiene: estado=ACTIVO
2. Auto-start detecta: Ya hay desastre activo
3. Auto-start NO ejecuta
4. ✅ No interfiere con desastre en curso
```

### Escenario 5: Safe Mode
```
1. state.yml tiene: safe_mode=true
2. Auto-start detecta: Safe mode activo
3. Auto-start NO ejecuta
4. ✅ Respeta el modo seguro
```

## 🧪 Pruebas de Verificación

### Test 1: Servidor con state.yml Corrupto
```bash
# 1. Editar state.yml manualmente
estado: PREPARACION
start_epoch_ms: 0
end_epoch_ms: 0

# 2. Reiniciar servidor
# 3. Verificar en consola:
[AutoStart] PREPARACION detectada con timestamps inválidos - reiniciando ciclo
[AutoStart] ✓ Ciclo de desastres iniciado automáticamente

# 4. Verificar que los desastres inician correctamente
```

### Test 2: Servidor Normal
```bash
# 1. state.yml tiene estado=DETENIDO
# 2. Reiniciar servidor
# 3. Verificar en consola:
[AutoStart] ✓ Ciclo de desastres iniciado automáticamente (estado: DETENIDO)

# 4. Verificar que preparación inicia
```

### Test 3: PREPARACION Válida en Curso
```bash
# 1. Ejecutar /avo start (inicia preparación)
# 2. Reiniciar servidor DURANTE la preparación
# 3. Verificar en consola:
[AutoStart] PREPARACION válida en curso - no se reinicia

# 4. Verificar que continúa el countdown
```

## 📊 Cambios en el Código

### Archivo: `Apocalipsis.java`

#### Método `onEnable()` - Sección Auto-Start

**ANTES:**
```java
if (!"ACTIVO".equalsIgnoreCase(estado) && !stateManager.isSafeModeActive()) {
    // Iniciar auto-start
}
```

**AHORA:**
```java
boolean needsAutoStart = false;

if ("ACTIVO".equalsIgnoreCase(estado)) {
    // No auto-start si ya hay desastre
} else if (stateManager.isSafeModeActive()) {
    // No auto-start si safe mode
} else if ("PREPARACION".equalsIgnoreCase(estado)) {
    // Verificar timestamps
    if (startEpoch == 0L || endEpoch == 0L || endEpoch <= now) {
        needsAutoStart = true; // Timestamps inválidos
    }
} else {
    needsAutoStart = true; // Estado DETENIDO u otro
}

if (needsAutoStart) {
    // Iniciar auto-start
}
```

### Archivo: `state.yml`

**ANTES:**
```yaml
estado: PREPARACION  # Estado inicial problemático
```

**AHORA:**
```yaml
estado: DETENIDO     # Estado inicial correcto
```

## 🎯 Beneficios del Fix

### ✅ **Robustez**
- El sistema se recupera automáticamente de estados inconsistentes
- No requiere intervención manual para corregir timestamps corruptos

### ✅ **Claridad**
- Logs detallados explican exactamente qué decisión tomó el auto-start
- Facilita el debugging de problemas de inicio

### ✅ **Compatibilidad**
- Respeta preparaciones válidas en curso (no las interrumpe)
- No afecta desastres activos
- Mantiene safe mode intacto

## 🔍 Detección de Estados Inválidos

El sistema ahora detecta y corrige automáticamente:

| Estado | Timestamps | Acción |
|--------|------------|--------|
| DETENIDO | Cualquiera | ✅ Iniciar auto-start |
| PREPARACION | Válidos | ✅ Continuar preparación |
| PREPARACION | 0 o expirados | ✅ Reiniciar con timestamps válidos |
| ACTIVO | Cualquiera | ⏸️ No auto-start |
| SAFE_MODE | Cualquiera | ⏸️ No auto-start |

## 📝 Notas Técnicas

### Condiciones de Timestamps Inválidos

Se considera inválido cuando:
```java
startEpoch == 0L      // Nunca se configuró
|| endEpoch == 0L     // Nunca se configuró  
|| endEpoch <= now    // Ya expiró
```

### Timestamp de Expiración

Si `end_epoch_ms` es menor o igual al tiempo actual, se considera que la preparación ya terminó y el sistema debería haber iniciado el desastre. Si no lo hizo, significa que hay un problema.

## 🚨 Troubleshooting

### Problema: "Desastres nunca inician después del fix"

**Verificar:**
1. El archivo `desastres.yml` tiene `auto_cycle: true`
2. El archivo `desastres.yml` tiene `start_on_boot: true`
3. Hay al menos 1 jugador conectado si `min_jugadores: 1`
4. Los logs muestran `[AutoStart] ✓ Ciclo de desastres iniciado`

### Problema: "Auto-start se ejecuta en cada reload"

**Causa:** El estado queda en `DETENIDO` después de cada reload.

**Solución:** Normal si el servidor se detuvo limpiamente. Si quieres que continúe:
- No detener el servidor durante preparación/desastres
- O configurar `start_on_boot: false` para control manual

### Problema: "Estado sigue en PREPARACION con timestamps 0"

**Causa:** El fix no se aplicó o hay un error al guardar `state.yml`.

**Solución:**
1. Verificar que el JAR compilado incluye los cambios
2. Verificar permisos de escritura en `state.yml`
3. Ver logs para errores de guardado

## ✅ Checklist Post-Fix

- [ ] Compilar plugin: `mvn clean package`
- [ ] Copiar JAR al servidor
- [ ] Verificar que `state.yml` tiene `estado: DETENIDO`
- [ ] Reiniciar servidor
- [ ] Verificar logs: `[AutoStart] ✓ Ciclo de desastres iniciado`
- [ ] Esperar fin de preparación
- [ ] Confirmar que desastre inicia automáticamente
- [ ] Reiniciar durante preparación
- [ ] Confirmar que NO reinicia (continúa preparación válida)

---

**Archivos modificados:**
- `src/main/java/me/apocalipsis/Apocalipsis.java`
- `src/main/resources/state.yml`

**Versión:** 1.22.60+  
**Fecha:** 30 de enero de 2026  
**Relacionado con:** AUTO-START, PREPARACION, Estado del Servidor
