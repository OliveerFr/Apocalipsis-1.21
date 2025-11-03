# 🔔 Guía de Notificaciones y Actualizaciones - Apocalipsis Plugin

## ✅ Estado de Implementaciones

### 1. Sistema de Notificaciones de Countdown ✅ IMPLEMENTADO

**Cómo funciona:**
- Las alertas se envían automáticamente cuando hay un tiempo de preparación configurado
- Alertas en: **60s, 30s, 10s, 5s, 4s, 3s, 2s, 1s**

**Tipos de alertas:**
- **60s-30s**: Mensajes en chat con sonido
- **10s-5s**: Mensajes en ActionBar (parte inferior de pantalla) con sonido urgente  
- **5s-1s**: Títulos grandes en pantalla + ActionBar + sonidos crecientes

---

### 2. Sistema Anti-Duplicación de Terremoto ✅ IMPLEMENTADO

**Problema corregido:**
- El terremoto ya no se ejecuta múltiples veces simultáneamente
- Se agregaron verificaciones de seguridad en `onTick()` y `applyEffects()`
- Limpieza completa de estado en `onStart()` y `onStop()`

---

### 3. Camera Shake del Terremoto ✅ IMPLEMENTADO

**Configuración:**
- Shake aumentado de ±2°/±1.5° a **±4°/±3°** para mayor visibilidad
- Mantiene límites seguros (pitch clamped a -89/+89)
- Se aplica cada `shakeEveryTicks` durante el terremoto

---

### 4. Restauración de Bloques sin Duplicación ✅ IMPLEMENTADO

**Solución:**
- Cada bloque convertido a grieta guarda su tipo original en `grietaOriginalStates`
- Al finalizar, se restaura al material original (no siempre STONE)
- Previene duplicación de bloques

---

### 5. Huracán - Anti-Griefing y Auto-Cleanup ✅ IMPLEMENTADO

**Features:**
- No coloca agua en Nether/End
- Verifica ownership de bloques antes de inundar
- Auto-limpieza gradual de bloques de agua (~5 segundos)
- Partículas de evaporación durante limpieza

---

## 🎮 Comandos y Uso - Sistema de Notificaciones

### 🔔 **Las notificaciones funcionan en DOS modos:**

---

#### **Modo 1: Ciclo Automático con Cooldown** ✅ RECOMENDADO

Cuando el plugin está en ciclo automático, las notificaciones se activan **durante el cooldown** antes de cada desastre:

```
/avo start    → Inicia el ciclo automático
```

**Configuración en `desastres.yml`:**
```yaml
ciclo:
  auto_cycle: true              # Debe estar en true
  cooldown_fin_segundos: 120    # Cooldown entre desastres (2 minutos por defecto)
  min_jugadores: 1              # Jugadores mínimos para iniciar
```

**Cómo funciona:**
1. Termina un desastre
2. Entra en estado `PREPARACION` (cooldown)
3. **Durante los últimos 60 segundos del cooldown**, se activan las alertas:
   - **60 segundos antes**: `⏰ ¡Desastre en 60 segundos!`
   - **30 segundos antes**: `⚠ ¡Desastre en 30 segundos!`
   - **10 segundos antes**: `⚠ ¡Desastre en 10 segundos!` (ActionBar)
   - **5-1 segundos**: Títulos grandes en pantalla + cuenta regresiva
4. El siguiente desastre se inicia automáticamente

**Ejemplo:**
- Cooldown configurado: 120 segundos (2 minutos)
- Alerta de 60s se activa cuando quedan 60 segundos del cooldown
- Alerta de 30s cuando quedan 30 segundos
- Alertas finales 10s, 5s, 4s, 3s, 2s, 1s antes de iniciar

---

#### **Modo 2: Preparación Forzada Manual** ✅ TAMBIÉN FUNCIONA

Para activar notificaciones manualmente con un tiempo específico:

```
/avo preparacion <minutos>
```

**Ejemplos:**
```
/avo preparacion 2    → 2 minutos de preparación con notificaciones
/avo preparacion 5    → 5 minutos de preparación con todas las alertas
/avo preparacion 1    → 1 minuto (solo verás alertas de 10s en adelante)
```

**Flujo:**
1. Ejecutas `/avo preparacion 2`
2. El plugin configura `end_epoch_ms` en state.yml
3. El sistema de countdown activa las alertas automáticamente:
   - A los **60 segundos restantes**: `⏰ ¡Desastre en 60 segundos!`
   - A los **30 segundos restantes**: `⚠ ¡Desastre en 30 segundos!`
   - A los **10 segundos restantes**: `⚠ ¡Desastre en 10 segundos!` (ActionBar)
   - **5-1 segundos**: Títulos grandes en pantalla + cuenta regresiva
4. Al llegar a 0, el desastre se inicia automáticamente

---

### ❌ **Comandos SIN notificaciones** (inicio inmediato):

```
/avo force terremoto    → Inicia terremoto AHORA (sin countdown)
/avo force huracan      → Inicia huracán AHORA (sin countdown)
```

**Por qué:** Estos comandos fuerzan el inicio inmediato del desastre, sin tiempo de preparación.

---

## 📁 Archivos del Servidor - ¿Eliminar o Mantener?

### ⚠️ **IMPORTANTE: Archivos a ELIMINAR del servidor**

Cuando actualizas el plugin, **DEBES eliminar** el archivo `state.yml` del servidor para que se regenere con la estructura correcta:

```
/plugins/Apocalipsis/state.yml  ← ELIMINAR ESTE ARCHIVO
```

**Por qué:**
- La nueva versión tiene campos adicionales necesarios para las notificaciones
- El archivo viejo puede causar que las notificaciones no funcionen
- Se regenerará automáticamente al iniciar el servidor

---

### ✅ **Archivos a MANTENER** (configuraciones):

```
/plugins/Apocalipsis/config.yml          ← MANTENER (configuración general)
/plugins/Apocalipsis/desastres.yml       ← MANTENER (config de desastres)
/plugins/Apocalipsis/misiones_new.yml    ← MANTENER (misiones)
/plugins/Apocalipsis/rangos.yml          ← MANTENER (rangos)
/plugins/Apocalipsis/alonsolevels.yml    ← MANTENER (niveles)
```

---

## 🔧 Procedimiento de Actualización

### Pasos para actualizar correctamente:

1. **Detener el servidor**
   ```
   /stop
   ```

2. **Hacer backup** (recomendado):
   ```
   Copiar /plugins/Apocalipsis/ a una carpeta de backup
   ```

3. **Eliminar el state.yml viejo**:
   ```
   Borrar: /plugins/Apocalipsis/state.yml
   ```

4. **Reemplazar el JAR**:
   ```
   Reemplazar: /plugins/Apocalipsis-1.0.0.jar
   Con el nuevo JAR compilado
   ```

5. **Iniciar el servidor**:
   - El plugin creará un nuevo `state.yml` con la estructura correcta
   - Las notificaciones funcionarán correctamente

---

## 🐛 Troubleshooting - Notificaciones no funcionan

### Verificación 1: Configuración de ciclo automático

Si usas **ciclo automático** (`/avo start`), verifica en `desastres.yml`:

```yaml
ciclo:
  auto_cycle: true               # DEBE estar en true
  cooldown_fin_segundos: 120     # Mínimo 60 para ver todas las alertas
  min_jugadores: 1               # Ajustar según necesites
```

**Importante:**
- Si `cooldown_fin_segundos` es menor a 60, no verás la alerta de 60 segundos
- Si es menor a 30, no verás las alertas de 60s ni 30s
- Recomendado: **120 segundos** (2 minutos) para ver todas las alertas

---

### Verificación 2: Archivo state.yml correcto
```yaml
# El archivo DEBE tener estos campos:
estado: DETENIDO
ultimo_desastre: ""
last_end_epoch_ms: 0
start_epoch_ms: 0        # ← DEBE EXISTIR
end_epoch_ms: 0          # ← DEBE EXISTIR
prep_forzada: false      # ← DEBE EXISTIR
```

Si faltan estos campos → **Eliminar state.yml y reiniciar servidor**

---

### Verificación 3: Usar modo correcto

**Para ciclo automático:**
```
✅ /avo start              → Inicia ciclo, notificaciones durante cooldown
```

**Para preparación manual:**
```
✅ /avo preparacion 2      → 2 minutos con notificaciones
```

**Inicio inmediato (sin notificaciones):**
```
ℹ️  /avo force terremoto   → Inicia AHORA (sin countdown)
```

---

### Verificación 3: Logs de debug

Activar debug en `config.yml`:
```yaml
debug_ciclo: true
```

**En ciclo automático**, buscar en consola durante el cooldown:
```
[Countdown] Alerta enviada: 60 segundos restantes
[Countdown] Alerta enviada: 30 segundos restantes
[Countdown] Alerta enviada: 10 segundos restantes
[Countdown] Alerta enviada: 5 segundos restantes
```

**En preparación forzada**, buscar:
```
[Cycle] PrepForzada activa, faltan X segundos
[Countdown] Alerta enviada: 60 segundos restantes
```

Si NO aparecen estos logs → Verificar configuración y state.yml

---

### Verificación 4: Permisos de jugadores

Los jugadores con este permiso **NO verán alertas**:
```
apocalipsis.exempt
```

Verificar que los jugadores normales **NO tengan** este permiso.

---

## 📊 Estructura de state.yml Correcta (Referencia)

```yaml
# ═══════════════════════════════════════════════════════════════════
# STATE.YML - Estado persistente del servidor (generado automáticamente)
# ═══════════════════════════════════════════════════════════════════
estado: DETENIDO              # DETENIDO | PREPARACION | ACTIVO | SAFE_MODE
ultimo_desastre: ""           # ID del último desastre ejecutado
last_end_epoch_ms: 0          # Timestamp del último fin de desastre
start_epoch_ms: 0             # Timestamp de inicio de preparación (NUEVO)
end_epoch_ms: 0               # Timestamp de fin de preparación (NUEVO)
prep_forzada: false           # Si la preparación es forzada (NUEVO)
desastre_actual: ""           # Desastre actualmente activo
current_day: 0                # Día actual del ciclo
```

---

## 🎯 Testing de Notificaciones

### Modo 1: Testing de Ciclo Automático

```bash
# 1. Activar modo test (cooldown de 3 segundos)
/avo test

# 2. Iniciar ciclo automático
/avo start

# 3. Esperar a que termine el primer desastre (20 segundos en test mode)
# 4. Durante el cooldown de 3 segundos verás alertas de 3s, 2s, 1s
```

---

### Modo 2: Testing de Preparación Manual

```bash
# 1. Activar modo test (preparación de 5 segundos)
/avo test

# 2. Iniciar preparación
/avo preparacion 1

# 3. Verás alertas de 5s, 4s, 3s, 2s, 1s en 5 segundos
```

---

### Modo Normal (Producción):

```bash
# 1. Desactivar modo test
/avo test

# 2. Configurar cooldown en desastres.yml
cooldown_fin_segundos: 120    # 2 minutos

# 3. Iniciar ciclo
/avo start

# 4. Las alertas aparecerán durante los últimos 60 segundos del cooldown
```

---

## 📝 Resumen de Cambios Implementados

| Feature | Estado | Archivo Modificado |
|---------|--------|-------------------|
| Notificaciones de Countdown | ✅ | DisasterController.java |
| Anti-Duplicación Terremoto | ✅ | TerremotoNew.java |
| Camera Shake Terremoto | ✅ | TerremotoNew.java |
| Restauración de Bloques | ✅ | TerremotoNew.java |
| Huracán Anti-Griefing | ✅ | HuracanNew.java |
| Huracán Auto-Cleanup | ✅ | HuracanNew.java |
| Persistencia de Castigos | ✅ | DisasterEvasionTracker.java |

---

## ⚡ Comandos Rápidos de Referencia

```bash
# Ciclo automático CON notificaciones (durante cooldown):
/avo start

# Preparación manual CON notificaciones:
/avo preparacion 2

# Forzar desastre SIN notificaciones (inmediato):
/avo force terremoto

# Activar modo test (cooldown 3s, preparación 5s):
/avo test

# Ver estado actual y cooldown:
/avo stats
/avo cooldown

# Detener todo:
/avo stop
```

---

## 🔍 Checklist de Actualización

- [ ] Servidor detenido
- [ ] Backup realizado (opcional pero recomendado)
- [ ] `state.yml` eliminado del servidor
- [ ] Nuevo JAR copiado a `/plugins/`
- [ ] Servidor reiniciado
- [ ] `state.yml` regenerado automáticamente
- [ ] Probar con `/avo preparacion 1`
- [ ] Verificar que aparecen notificaciones
- [ ] Listo para usar ✅

---

**Versión del documento:** 1.0  
**Fecha:** Noviembre 2025  
**Plugin:** Apocalipsis v1.0.0  
**Compilación:** BUILD SUCCESS ✅
