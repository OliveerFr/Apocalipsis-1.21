# Changelog - Sistema de Seguridad Avanzado para Ciclos v1.22.55

**Fecha**: 27 de Enero, 2026  
**Tipo**: Mejora de Seguridad y Protección  
**Componente**: Sistema de Ciclos Multi-Mundo

---

## 🛡️ Resumen Ejecutivo

Se han implementado **múltiples capas de seguridad** en el sistema de ciclos para proteger contra:
- Reconexiones abusivas / spam de teleportes
- Spawns peligrosos (lava, void, bloques sólidos)
- Múltiples ciclos activos simultáneos (inconsistencia de datos)
- Memory leaks por cooldowns acumulados
- Teleportes a mundos no cargados o corruptos
- Teleportes durante desconexión del jugador

---

## 🔐 Nuevas Capas de Seguridad Implementadas

### 1. **Sistema de Cooldown de Teleporte**
**Propósito**: Prevenir spam de reconexiones y abuso de sistema.

- **Cooldown**: 5 segundos entre reconexiones
- **Almacenamiento**: HashMap UUID → Timestamp
- **Comportamiento**: Si jugador reconecta antes de 5s, se bloquea teleporte y carga datos normales
- **Logging**: Registra intentos bloqueados con tiempo restante

```java
TELEPORT_COOLDOWN_MS = 5000; // 5 segundos
```

**Logs de ejemplo**:
```
[SEGURIDAD] player123 intentó reconexión rápida. Cooldown: 3s
```

---

### 2. **Validación de Spawn Seguro**
**Propósito**: Prevenir spawns en ubicaciones peligrosas.

**Verificaciones automáticas**:
- ✅ Límites del mundo (min/max height)
- ✅ Distancia del void (mínimo 5 bloques sobre bedrock)
- ✅ Materiales peligrosos: lava, fuego, magma blocks, campfires
- ✅ Bloques sólidos en pies/cabeza (suffocation)
- ✅ Suelo sólido presente

**Método**: `isLocationSafe(Location)`

**Búsqueda de alternativa**:
- Si spawn no es seguro → busca en espiral (radio 10 bloques)
- Búsqueda vertical: +/- 5 bloques
- Si no encuentra alternativa → **cancela operación**

**Método**: `findSafeLocation(World, Location)`

---

### 3. **Validación de Ciclo Único Activo**
**Propósito**: Detectar y advertir sobre múltiples ciclos activos.

**Comportamiento**:
- Itera sobre todos los ciclos registrados
- Cuenta cuántos tienen `isActivo() == true`
- **Si 0 ciclos activos**: ✓ Normal
- **Si 1 ciclo activo**: ✓ Correcto
- **Si 2+ ciclos activos**: ✗ **ERROR GRAVE** - Logs detallados

**Método**: `validateSingleActiveCycle()`

**Logs de error**:
```
[SEGURIDAD] ✗ FALLO DE VALIDACIÓN: 2 ciclos activos simultáneos detectados:
  - ciclo_1
  - ciclo_2
  └─ ACCIÓN REQUERIDA: Usar /avo ciclo info y desactivar ciclos duplicados
```

---

### 4. **Validación de Mundo Cargado**
**Propósito**: Prevenir teleportes a mundos no existentes/corruptos.

**Validaciones**:
1. `Bukkit.getWorld(activeCycle) != null` - Mundo existe
2. Verificar antes de cada teleporte
3. Log de error si mundo no cargado
4. **Fallback**: Cargar datos del mundo actual en vez de crash

**Logs de error**:
```
[SEGURIDAD] Ciclo activo 'ciclo_3' no existe o no está cargado. Abortando teleporte de player123
```

---

### 5. **Validación de Jugador Conectado**
**Propósito**: Prevenir teleportes después de desconexión.

**Implementación**:
- Delay de 1 tick antes de teleporte (prevenir conflictos de login)
- Dentro del scheduler: `if (!player.isOnline()) return;`
- Log de advertencia si jugador se desconectó

**Logs de advertencia**:
```
[SEGURIDAD] player456 se desconectó antes de teleporte a ciclo activo
```

---

### 6. **Limpieza Automática de Cooldowns**
**Propósito**: Prevenir memory leaks por jugadores desconectados.

**Comportamiento**:
- Ejecuta cada 30 minutos
- Elimina cooldowns mayores a 50 segundos (10x el cooldown normal)
- Método público para limpieza manual: `cleanupCooldowns()`

**Logs de debug**:
```
[SEGURIDAD] Limpieza de cooldowns: 15 entradas removidas
```

---

### 7. **Validación en `setSpawn()`**
**Propósito**: Asegurar que spawns manuales sean seguros.

**Nuevas validaciones**:
1. Mundo cargado y válido
2. Ubicación pertenece al mundo correcto
3. **Ubicación es segura** (usa `isLocationSafe()`)
4. Búsqueda de alternativa si no es segura
5. **Cancela operación** si no hay alternativa

**Logs mejorados**:
```
[SEGURIDAD] Ubicación de spawn no es segura en 'ciclo_1'
  └─ Coordenadas: 100, 10, -200
  └─ Usando spawn seguro alternativo: 105, 15, -195
```

---

### 8. **Logging Completo de Auditoría**
**Propósito**: Rastrear todos los teleportes para debugging.

**Información registrada**:
- UUID del jugador
- Mundo origen → Mundo destino
- Razón del teleporte
- Coordenadas exactas del destino
- Confirmación de teleporte exitoso

**Formato de logs**:
```
[CicloManager] TELEPORTE AUTORIZADO: player789 (uuid-here) desde 'world' → 'ciclo_1'
  └─ Razón: Primera vez en ciclo activo
  └─ Destino: 100, 70, -200
[CicloManager] ✓ Teleporte completado: player789
```

---

## 🔧 Nuevas Funcionalidades

### Comando: `/avo ciclo security`
**Aliases**: `/avo ciclo seguridad`  
**Permiso**: `apocalipsis.ciclo.admin`

**Funcionalidad**:
- ✓ Valida que solo haya un ciclo activo
- ✓ Muestra información del ciclo activo actual
- ✓ Muestra coordenadas del spawn
- ✓ Limpia cooldowns manualmente
- ✓ Panel visual con estado de seguridad

**Output de ejemplo**:
```
§e§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
§6§lSEGURIDAD DE CICLOS

§a✓ Ciclo activo único: CORRECTO
§e◆ Ciclo activo actual: §bciclo_1
§e◆ Spawn: §7100, 70, -200
§a✓ Cooldowns limpiados

§e§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
```

---

### Tarea Programada: Validación Automática
**Frecuencia**: Cada 30 minutos (36000 ticks)  
**Modo**: Asíncrono (no afecta TPS)

**Operaciones**:
1. `validateSingleActiveCycle()` - Detectar ciclos duplicados
2. `cleanupCooldowns()` - Prevenir memory leaks

**Logs al iniciar**:
```
[SEGURIDAD] Tareas de validación automática iniciadas (cada 30 min)
```

---

## 📊 Estructura de Código

### Nuevas Variables de Clase
```java
// Sistema de cooldown
private final Map<UUID, Long> teleportCooldowns = new HashMap<>();
private static final long TELEPORT_COOLDOWN_MS = 5000;
```

### Nuevos Métodos

#### Métodos de Seguridad
```java
private boolean isLocationSafe(Location location)
private Location findSafeLocation(World world, Location center)
public boolean validateSingleActiveCycle()
public void cleanupCooldowns()
private void startSecurityTasks()
```

#### Métodos Mejorados
```java
public String getActiveCycle() // + validación de ciclo único
public void handlePlayerJoin() // + 5 capas de seguridad
public boolean setSpawn() // + validación de spawn seguro
```

---

## 🔍 Flujo de Seguridad en Reconexión

```
PlayerJoinEvent
    ↓
handlePlayerJoin(player, worldName)
    ↓
1. getActiveCycle() → Detectar ciclo activo
    ↓
2. dataManager.hasData() → Verificar si es primera vez
    ↓
3. SEGURIDAD 1: Verificar cooldown (5s)
    ├─ Bloqueado → Cargar datos normales, return
    └─ OK → Continuar
    ↓
4. SEGURIDAD 2: Validar mundo existe (Bukkit.getWorld)
    ├─ null → Log error, cargar datos normales, return
    └─ OK → Continuar
    ↓
5. SEGURIDAD 3: Validar spawn seguro (isLocationSafe)
    ├─ No seguro → Buscar alternativa (findSafeLocation)
    │   ├─ No encontrado → Log error, return
    │   └─ Encontrado → Usar alternativa
    └─ Seguro → Continuar
    ↓
6. Registrar cooldown (teleportCooldowns.put)
    ↓
7. Scheduler (1 tick delay)
    ↓
8. SEGURIDAD 4: Verificar jugador online
    ├─ Offline → Log advertencia, return
    └─ Online → Continuar
    ↓
9. player.teleport(finalSpawn)
    ↓
10. Log de confirmación + mensajes al jugador
```

---

## 🎯 Casos de Uso Cubiertos

### ✅ Caso 1: Jugador Nuevo en Ciclo Activo
**Escenario**: Jugador se conecta por primera vez  
**Resultado**: Teleportado al ciclo activo con todas las validaciones  
**Protección**: Cooldown, spawn seguro, mundo válido

---

### ✅ Caso 2: Reconexión Rápida (Spam)
**Escenario**: Jugador se desconecta/reconecta < 5s  
**Resultado**: Teleporte bloqueado, cargan datos normales  
**Log**: `[SEGURIDAD] player intentó reconexión rápida. Cooldown: Xs`

---

### ✅ Caso 3: Spawn en Lava
**Escenario**: Spawn configurado en lava/fuego  
**Resultado**: Busca alternativa segura, si no la hay → cancela  
**Log**: `[SEGURIDAD] Spawn no seguro... Usando alternativa: X, Y, Z`

---

### ✅ Caso 4: Múltiples Ciclos Activos
**Escenario**: Corrupción de datos → 2 ciclos marcados activos  
**Resultado**: Detectado cada 30 min + logs detallados  
**Log**: `[SEGURIDAD] ✗ FALLO: 2 ciclos activos simultáneos`

---

### ✅ Caso 5: Mundo No Cargado
**Escenario**: Ciclo activo en mundo corrupto/no existente  
**Resultado**: Teleporte cancelado, cargan datos del mundo actual  
**Log**: `[SEGURIDAD] Ciclo 'X' no existe. Abortando teleporte`

---

### ✅ Caso 6: Desconexión Durante Login
**Escenario**: Jugador se desconecta antes del scheduler (1 tick)  
**Resultado**: Teleporte cancelado, sin crash  
**Log**: `[SEGURIDAD] player se desconectó antes de teleporte`

---

### ✅ Caso 7: Memory Leak de Cooldowns
**Escenario**: 1000 jugadores antiguos con cooldowns guardados  
**Resultado**: Limpieza automática cada 30 min  
**Log**: `[SEGURIDAD] Limpieza: 950 entradas removidas`

---

## 🧪 Testing Recomendado

### Test 1: Cooldown de Teleporte
1. Conectarse al servidor
2. Desconectarse inmediatamente
3. Reconectarse < 5s
4. **Expected**: No teleporta, log de cooldown

---

### Test 2: Spawn Peligroso
1. `/avo ciclo setspawn ciclo_test` en lava
2. Desconectarse
3. Reconectarse
4. **Expected**: Busca spawn alternativo o cancela

---

### Test 3: Validación de Seguridad
1. `/avo ciclo security`
2. **Expected**: Panel con estado de ciclos
3. Verificar que solo hay 1 ciclo activo

---

### Test 4: Limpieza de Cooldowns
1. Esperar 30 minutos con servidor activo
2. Verificar logs
3. **Expected**: `[SEGURIDAD] Limpieza de cooldowns...`

---

## 📝 Notas de Compatibilidad

- ✅ **Compatible** con sistema de ciclos existente
- ✅ **Retrocompatible** con datos de ciclos_data.yml
- ✅ **No afecta** jugadores con progreso existente
- ✅ **Mejora** estabilidad y seguridad sin cambios visuales

---

## 🚀 Mejoras Futuras Sugeridas

1. **Configuración de cooldown**: Permitir ajustar 5s en config.yml
2. **Whitelist de spawns seguros**: Pre-validar spawns antes de activar ciclo
3. **Auto-reparación**: Si hay 2+ ciclos activos → desactivar automáticamente el más viejo
4. **Alertas Discord**: Notificar admins cuando hay fallos de validación
5. **Backup automático**: Guardar backup antes de setear spawn

---

## 🔗 Archivos Modificados

- `CicloManager.java` - Lógica de seguridad principal
- `ApocalipsisCommand.java` - Comando `/avo ciclo security`
- `AvoTabCompleter.java` - Autocompletado de comandos

---

## 📌 Comandos de Administración

### Validación Manual
```
/avo ciclo security
```

### Información de Ciclos
```
/avo ciclo info
```

### Establecer Spawn Seguro
```
/avo ciclo setspawn [mundo]
```
*Ahora con validación automática de seguridad*

---

**Desarrollado por**: Sistema de Seguridad Avanzado  
**Versión**: 1.22.55  
**Compatibilidad**: Minecraft 1.21.8, Spigot/Paper
