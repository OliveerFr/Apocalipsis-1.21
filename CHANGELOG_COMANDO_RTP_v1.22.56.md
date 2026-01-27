# Changelog - Comando /rtp (Random Teleport) v1.22.56

**Fecha**: 2024-01-XX  
**Versión**: 1.22.56  
**Tipo**: Nueva Funcionalidad

---

## 📝 Resumen

Se ha implementado un nuevo comando `/rtp` (Random Teleport) que permite a los jugadores teleportarse aleatoriamente a una ubicación segura en el overworld, ideal para alejarse de áreas pobladas y explorar nuevas zonas.

---

## ✨ Nuevas Funcionalidades

### 🎯 Comando /rtp

**Aliases disponibles**:
- `/avo rtp`
- `/avo randomtp`  
- `/avo wild`

**Características**:
- ✅ Teleportación aleatoria entre 1000-5000 bloques desde el spawn
- ✅ Solo funciona en overworld (superficie)
- ✅ Cooldown de 5 minutos (configurable)
- ✅ Búsqueda asíncrona (no congela el servidor)
- ✅ Validación de seguridad completa
- ✅ Distancia mínima de 200 bloques de otros jugadores
- ✅ Efectos visuales y sonoros al teleportarse
- ✅ Logging para administradores

### 🛡️ Validaciones de Seguridad

El sistema valida automáticamente cada ubicación antes de teleportar:

1. **Altura segura**: Entre Y=60 y altura máxima -10
2. **Sin peligros**: No lava, fuego, magma, agua o campfires
3. **Espacio libre**: Pies y cabeza sin bloques sólidos (evita suffocation)
4. **Suelo sólido**: Bloque sólido debajo del jugador
5. **Bioma apropiado**: No océanos ni ríos
6. **Distancia social**: Mínimo 200 bloques de otros jugadores

### 🔧 Sistema de Búsqueda

**Algoritmo**:
- 10 intentos de búsqueda
- Distribución uniforme usando trigonometría
- Coordenadas aleatorias: `spawn + (distancia × cos/sin(ángulo))`
- Bloques más altos en cada coordenada

**Rango de distancia**:
- Mínimo: 1000 bloques desde spawn
- Máximo: 5000 bloques desde spawn

---

## 🔨 Cambios Técnicos

### Archivos Modificados

#### 1. `ApocalipsisCommand.java`

**Método: `onCommand()` - Switch Case** (Línea ~245)
```java
case "rtp":
case "randomtp":
case "wild":
    cmdRandomTeleport(sender);
    break;
```

**Método: `cmdRandomTeleport()` - NUEVO** (Líneas ~7834-7889)
```java
private void cmdRandomTeleport(CommandSender sender) {
    // Verificaciones: jugador, overworld, cooldown
    // Búsqueda asíncrona de ubicación segura
    // Teleportación con efectos visuales
    // Aplicación de cooldown
}
```

**Método: `findRandomSafeLocation()` - NUEVO** (Líneas ~7897-7927)
```java
private Location findRandomSafeLocation(World world, Location playerLoc) {
    // 10 intentos de búsqueda
    // Rango: 1000-5000 bloques desde spawn
    // Distribución uniforme con trigonometría
    // Validación de seguridad por intento
}
```

**Método: `isLocationSafeForRTP()` - NUEVO** (Líneas ~7932-7986)
```java
private boolean isLocationSafeForRTP(Location location) {
    // 6 validaciones de seguridad:
    // - Altura, peligros, espacio, suelo, bioma, distancia
}
```

**Método: `showHelp()` - Página 5** (Línea ~343)
```java
{"§6▸ Teleporte", ""},
{"  §e/avo rtp", "§7TP aleatorio (1000-5000 bloques)"},
```

#### 2. `CooldownManager.java`

**Enum: `CooldownType`** (Líneas 148-154)
```java
public enum CooldownType {
    CAMBIO_MUNDO,    // 10 segundos
    CREAR_CICLO,     // 5 minutos
    RANDOM_TP        // 5 minutos - NUEVO
}
```

**Método: `getCooldownTime()`** (Líneas ~115)
```java
case RANDOM_TP:
    return config.getLong("cooldowns.random_tp", 300) * 1000; // 5 min
```

#### 3. `AvoTabCompleter.java`

**Array de Subcomandos** (Línea ~42)
```java
"rtp", "randomtp", "wild",  // AGREGADO
```

---

## 📊 Flujo de Ejecución

```
Jugador ejecuta /rtp
    ↓
¿Es jugador? → NO → Mensaje error
    ↓ SÍ
¿Está en overworld? → NO → Mensaje error
    ↓ SÍ
¿Cooldown disponible? → NO → Mostrar tiempo restante
    ↓ SÍ
Mensaje: "Buscando ubicación..."
    ↓
[ASYNC] Búsqueda de ubicación
    ├─ Intento 1-10
    │   ├─ Calcular coordenadas aleatorias
    │   ├─ Obtener bloque más alto
    │   └─ Validar seguridad (6 checks)
    └─ ¿Encontrada? → NO → Mensaje error
            ↓ SÍ
[SYNC] Teleportación
    ├─ Aplicar cooldown (5 min)
    ├─ Teleportar jugador
    ├─ Mensaje con coordenadas
    ├─ Efectos: sonido + partículas
    └─ Log para admins
```

---

## ⚙️ Configuración

### Archivo: `ciclos.yml` (Opcional)

Agregar configuración de cooldown personalizado:

```yaml
cooldowns:
  cambio_mundo: 10      # 10 segundos
  crear_ciclo: 300      # 5 minutos
  random_tp: 300        # 5 minutos (default)
```

**Parámetros configurables**:
- `random_tp`: Tiempo de cooldown en segundos (default: 300)

---

## 🎮 Uso del Comando

### Ejemplo de Uso Normal

```
/avo rtp
```

**Salida**:
```
⚙ Buscando ubicación aleatoria segura...
Esto puede tardar unos segundos.

✓ ¡Teletransportado a ubicación aleatoria!
Coordenadas: 3421, 72, -1856
```

### Ejemplo con Cooldown Activo

```
/avo rtp
```

**Salida**:
```
✖ Debes esperar 3m 24s antes de volver a usar Random Teleport.
```

### Ejemplo en Nether/End

```
/avo rtp
```

**Salida**:
```
✖ Solo puedes usar /rtp en el overworld.
Vuelve a la superficie para usar este comando.
```

### Ejemplo sin Ubicación Segura

```
/avo rtp
```

**Salida**:
```
⚙ Buscando ubicación aleatoria segura...
Esto puede tardar unos segundos.

✖ No se pudo encontrar una ubicación segura.
Intenta de nuevo en unos momentos.
```

---

## 📋 Logs para Administradores

```log
[Apocalipsis] [RTP] Steve teleportado a 3421, 72, -1856
[Apocalipsis] [RTP] Ubicación segura encontrada en intento 3
[Apocalipsis] [RTP] No se encontró ubicación segura después de 10 intentos
```

---

## 🔄 Integración con Sistema de Ciclos

- ✅ Compatible con sistema multi-mundo
- ✅ Usa mismo sistema de validación de spawns seguros
- ✅ Integrado con CooldownManager existente
- ✅ Logging consistente con resto del sistema
- ✅ Ejecución asíncrona para no afectar performance

---

## 🐛 Testing Recomendado

### Casos de Prueba

1. **Teleportación Normal**
   - Ejecutar `/rtp` en overworld
   - Verificar coordenadas entre 1000-5000 bloques
   - Confirmar efectos visuales/sonoros
   - Verificar log en consola

2. **Validación de Mundo**
   - Ejecutar en Nether → Mensaje error
   - Ejecutar en End → Mensaje error
   - Ejecutar en overworld → Éxito

3. **Sistema de Cooldown**
   - Ejecutar `/rtp` dos veces seguidas
   - Segunda vez debe mostrar tiempo restante
   - Esperar 5 minutos → Debe permitir nuevo TP

4. **Distancia de Jugadores**
   - Con 2+ jugadores cerca (<200 bloques)
   - Verificar que busca ubicación alejada
   - Confirmar distancia mínima 200 bloques

5. **Ubicación No Segura**
   - Ejecutar en mundo con mucha agua
   - Verificar mensaje "No se encontró ubicación"
   - Confirmar que no teleporta

---

## 📈 Mejoras Futuras Sugeridas

1. **Configuración Avanzada**
   - Rango mínimo/máximo configurable en `ciclos.yml`
   - Número de intentos configurable
   - Distancia mínima de jugadores configurable

2. **Estadísticas**
   - Contador de RTP por jugador
   - Historial de ubicaciones visitadas
   - Top RTP del día/semana

3. **Funcionalidades Adicionales**
   - `/rtp back` para volver a ubicación anterior
   - Costos en economía (dinero/tokens)
   - Zonas prohibidas configurables
   - Integración con claims/regiones

4. **UX Mejorada**
   - Countdown de 3 segundos antes de TP
   - Partículas de preparación
   - Mapa con preview de ubicación (si es posible)

---

## ⚠️ Notas Importantes

### Performance
- La búsqueda es **asíncrona** para no congelar el servidor
- Máximo 10 intentos para evitar lag
- Cooldown de 5 minutos previene spam

### Seguridad
- Solo overworld (evita TP accidental a Nether/End)
- 6 validaciones de seguridad por ubicación
- No puede teleportarse cerca de otros jugadores

### Compatibilidad
- Compatible con sistema de ciclos multi-mundo
- Respeta inventarios separados de ciclos
- No interfiere con comandos existentes

---

## 👥 Casos de Uso

### Jugadores Nuevos
- Alejarse del spawn poblado
- Encontrar áreas vírgenes para construir
- Explorar el mundo de forma rápida

### Jugadores Avanzados
- Buscar biomas raros
- Encontrar estructuras alejadas
- Escapar de PvP (si aplica)

### Comunidad
- Reducir congestión en spawn
- Distribuir jugadores por el mapa
- Fomentar exploración

---

## 🎯 Conclusión

El comando `/rtp` es una adición valiosa que:
- ✅ Mejora la experiencia de exploración
- ✅ Reduce congestión en áreas populares
- ✅ Mantiene la seguridad del jugador
- ✅ Se integra perfectamente con el sistema existente
- ✅ Tiene performance optimizada
- ✅ Es fácil de usar y configurar

**Total de líneas agregadas**: ~160 líneas
**Archivos modificados**: 3 (ApocalipsisCommand, CooldownManager, AvoTabCompleter)
**Nuevos métodos**: 3 (cmdRandomTeleport, findRandomSafeLocation, isLocationSafeForRTP)

---

**Versión del plugin**: 1.22.56  
**Fecha de implementación**: 2024-01-XX  
**Autor**: Sistema Apocalipsis  
**Estado**: ✅ Implementado y Compilado
