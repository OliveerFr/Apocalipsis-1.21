# Comandos Eco de Brasas - Implementación Completa

## ✅ Cambios Realizados

### 1. **Comando `skip` agregado**
- **Comando:** `/avo eco skip`
- **Función:** Salta cinemáticas/diálogos actuales y avanza a la siguiente actividad o fase
- **Uso:** Perfecto para testing rápido del evento
- **Comportamiento:**
  - Cancela tasks de cinemáticas activas (intro, transiciones)
  - Avanza automáticamente a la siguiente fase jugable
  - Si está en INTRO → salta a RECOLECCIÓN
  - Si está en TRANSICION_2 → salta a ESTABILIZACIÓN
  - Si está en TRANSICION_3 → salta a RITUAL_FINAL

### 2. **Comando `fase` arreglado**
- **Comando:** `/avo eco fase <1|2|3>`
- **Función:** Fuerza cambio directo a una fase específica
- **Ahora funciona correctamente:**
  - Cancela cinematics activas antes de cambiar
  - Resetea `ticksEnFase` a 0
  - Logs claros en consola

### 3. **Comando `next` arreglado**
- **Comando:** `/avo eco next`
- **Función:** Avanza a la siguiente fase en orden
- **Ahora funciona correctamente:**
  - Cancela cinematics antes de avanzar
  - Muestra mensaje con fase actual después de avanzar
  - Maneja correctamente el final del evento

### 4. **Comando `info` mejorado**
- **Comando:** `/avo eco info`
- **Función:** Muestra información detallada del estado actual
- **Ahora muestra:**
  - Fase actual
  - Progreso de la fase (0-100%)
  - Tiempo en fase (segundos y minutos)
  - Tiempo total del evento
  - **Fase 1:** Grietas cerradas, grietas activas
  - **Fase 2:** Anclas completas, fragmentos por ancla
  - **Fase 3:** Progreso ritual, pulso actual/máximo, guardián spawneado

### 5. **Comando `pulso` arreglado**
- **Comando:** `/avo eco pulso <add|set> <valor>`
- **Función:** Modifica pulso del ritual (solo fase 3)
- **Ahora funciona correctamente:**
  - Modifica `pulsoActual` correctamente
  - Respeta límites (0-100)
  - Muestra efectos visuales en el altar
  - Logs claros con antes/después

### 6. **Comando `ancla` arreglado**
- **Comando:** `/avo eco ancla <1-3>`
- **Función:** Completa forzadamente un ancla específica
- **Ahora funciona correctamente:**
  - Solo funciona en fase 2
  - Marca ancla como completa (100% fragmentos)
  - Efectos visuales (FLASH, FLAME, levelup sound)
  - Broadcast a todos los jugadores

## 📋 Lista Completa de Comandos

```
/avo eco start          - Inicia el evento Eco de Brasas
/avo eco stop           - Detiene el evento completamente
/avo eco skip           - Salta cinemática/diálogo actual [NUEVO]
/avo eco next           - Avanza a la siguiente fase
/avo eco fase <1|2|3>   - Fuerza fase específica
/avo eco info           - Información detallada del evento
/avo eco pulso <add|set> <valor> - Modifica pulso ritual
/avo eco ancla <1-3>    - Completa ancla específica
```

## 🔧 Métodos Implementados en EcoBrasasEvent.java

### `forzarFase(String fase)`
- Cambia directamente a fase 1, 2 o 3
- Cancela cinematics activas
- Resetea temporizador de fase

### `forzarSiguienteFase()`
- Avanza en orden: INTRO → RECOLECCION → ESTABILIZACION → RITUAL_FINAL → VICTORIA
- Cancela cinematics antes de avanzar
- Retorna false si ya terminó

### `cancelarCinematicasActivas()`
- Cancela task de diálogos (`dialogoTask`)
- Permite skip limpio de cinematics

### `getProgresoFase()`
- **Fase 1:** `(grietasCerradas * 100) / grietasMetaTotal`
- **Fase 2:** Promedio de progreso de 3 anclas
- **Fase 3:** `(pulsoActual * 100) / pulsoMaximo`

### `getInfoDetallada()`
- StringBuilder con toda la info relevante
- Formato coloreado con códigos Minecraft
- Info específica por fase

### `completarAncla(int anclaId)`
- Valida ID (1-3) y fase actual
- Marca como completa en mapa `anclaProgreso`
- Efectos visuales y broadcast

### `addPulsoGlobal(int cantidad)`
- Solo funciona en fase 3
- Modifica `pulsoActual` con límites
- Efectos visuales en altar

## 🎮 Flujo de Testing Rápido

```bash
/avo eco start          # Inicia evento
/avo eco skip           # Salta intro → va directo a Fase 1
/avo eco fase 2         # Fuerza Fase 2
/avo eco ancla 1        # Completa ancla 1
/avo eco ancla 2        # Completa ancla 2
/avo eco ancla 3        # Completa ancla 3
/avo eco skip           # Salta transición → Fase 3
/avo eco pulso set 100  # Completa ritual
/avo eco info           # Ver estado final
```

## 📦 Compilación

**JAR generado:** `target\Apocalipsis-1.0.0.jar`
**Tamaño:** 246,402 bytes
**Fecha:** 11/12/2025 11:41 AM

**Estado:** ✅ BUILD SUCCESS - Sin errores de compilación

## 🚀 Próximo Paso

Usa el comando SCP de `sharing.md` para subir el JAR al servidor:

```bash
scp "C:\Users\riolu\Videos\Eventos\Apocalipsis-1.21.8\target\Apocalipsis-1.0.0.jar" oliveerf@Oliveerf.top:"C:\Users\OliveerF\Desktop\Servers\Server Test\plugins\Apocalipsis-1.0.0.jar"
```

Luego en servidor:
1. `/reload` o reinicia
2. `/avo eco start` para probar
3. Usa `/avo eco skip` para avanzar rápido en testing
