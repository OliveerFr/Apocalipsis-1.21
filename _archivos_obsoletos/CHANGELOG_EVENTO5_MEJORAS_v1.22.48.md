# 📋 CHANGELOG - Evento 5: La Apertura del End
## Versión 1.22.48 - Mejoras Dinámicas y Sistema de Tareas

---

## 🎯 Resumen de Cambios

Esta actualización transforma completamente la **Fase 1 (Descubrimiento)** del Evento 5, añadiendo:
- ✅ Sistema de tareas coherente con el End
- ✅ Spawn continuo de Endermans
- ✅ Instrucciones claras al inicio
- ✅ Sistema de waypoints progresivos
- ✅ Mecánicas de acercamiento dinámico al portal
- ✅ Comando de teletransporte para testing

---

## 🔧 Mejoras Técnicas

### **1. Fix del Comando `/avo evento5 next`**
**Problema detectado:**
- El comando establecía el timer exactamente en el checkpoint
- El BukkitRunnable decrementaba ANTES de verificar
- Los diálogos nunca se ejecutaban

**Solución implementada:**
```java
// ANTES
descubrimientoTimer = dialogo; // Se salta el checkpoint

// AHORA  
descubrimientoTimer = dialogo + 5; // +5 segundos ANTES del checkpoint
```

**Resultado:**
- ✅ Los diálogos ahora se ejecutan correctamente
- ✅ Los efectos cinematográficos se muestran
- ✅ Testing rápido funcional

---

## 🎮 Sistema de Tareas Rediseñado

### **Antes (Genérico - Sin Coherencia)**
❌ Matar criaturas hostiles
❌ Romper minerales/madera
❌ Experimentar la muerte
❌ Eliminar un Enderman

**Problemas:**
- No tenían relación con el End
- Confuso para los jugadores
- Sin contexto narrativo

### **Ahora (Temático End - Coherente)**
✅ **Tarea 1: Eliminar un Enderman** - Los emisarios del End están aquí
✅ **Tarea 2: Recolectar Obsidiana** - El material del portal
✅ **Tarea 3: Conseguir un Ojo de Ender** - La clave dimensional

**Mejoras:**
- 🎯 100% coherente con el tema del End
- 📖 Historia clara y lógica
- 🎬 Cada tarea tiene significado narrativo

---

## 📢 Sistema de Instrucciones Claras

### **Mensaje Inicial (Nuevo)**
Al iniciar el evento, los jugadores ven:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━
⚡ INSTRUCCIONES DEL OBSERVADOR ⚡

El portal al End está materializándose...
Pero su ubicación está borrosa.

Debéis completar 3 tareas para revelar la ubicación:

  1. Eliminar un Enderman - Los emisarios del End están aquí
  2. Recolectar Obsidiana - El material del portal
  3. Conseguir un Ojo de Ender - La clave dimensional

Los Endermans aparecerán continuamente...
━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

**Características:**
- 🎯 Instrucciones inmediatas y claras
- 📋 Lista específica de objetivos
- 🔊 Sonidos temáticos (Ender Dragon Growl)
- ⏱️ Aparece 2 segundos después del inicio

---

## 🎆 Mensajes de Progreso Mejorados

### **Al Completar Cada Tarea:**
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━
✓ TAREA COMPLETADA

[Jugador] ha eliminado un Enderman
'El End siente la pérdida...'

Progreso: 1/3 tareas
━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

**Características:**
- 🎊 Banner grande y visible
- 👤 Menciona al jugador que completó la tarea
- 💬 Mensaje temático único por tarea
- 📊 Barra de progreso clara (X/3)
- 🔊 Efectos de sonido únicos:
  - Enderman: `ENTITY_ENDERMAN_SCREAM` + `ENTITY_PLAYER_LEVELUP`
  - Obsidiana: `BLOCK_GLASS_BREAK` + `ENTITY_PLAYER_LEVELUP`
  - Ojo de Ender: `BLOCK_END_PORTAL_FRAME_FILL` + `ENTITY_PLAYER_LEVELUP`

---

## 👾 Spawn Continuo de Endermans

### **Sistema Automatizado**
```java
// Aparecen cada 2 minutos durante toda la Fase 1
// Spawn de 1-3 Endermans por oleada
// Cerca de jugadores aleatorios (15-30 bloques)
```

**Características:**
- ⏰ **Frecuencia:** Cada 2 minutos (2400 ticks)
- 👾 **Cantidad:** 1-3 Endermans por oleada
- 📍 **Ubicación:** 15-30 bloques de jugadores aleatorios
- 🏷️ **Nombre:** "§5Emisario del End" (visible)
- ✨ **Efectos de aparición:**
  - Partículas: PORTAL (50) + REVERSE_PORTAL (30)
  - Sonidos: ENDERMAN_AMBIENT + END_PORTAL_SPAWN

**Mensaje al Spawn:**
```
[...] Emisarios del End han aparecido en el mundo...
```

---

## 📍 Sistema de Waypoints Progresivos

### **5 Waypoints Dinámicos**

#### **Waypoint 1 - 40 minutos (80% distancia)**
- 🎯 Nombre: **§5§l⚡ ECO DISTANTE**
- 📢 "Algo resuena al norte..."
- 🗺️ 80% del camino hacia el portal

#### **Waypoint 2 - 30 minutos (60% distancia)**
- 🎯 Nombre: **§5§l⚡ RESONANCIA CRECIENTE**  
- 📢 "La energía se intensifica..."
- 🗺️ 60% del camino (más cerca)

#### **Waypoint 3 - 20 minutos (40% distancia)**
- 🎯 Nombre: **§5§l⚡ LLAMADO DEL VACÍO**
- 📢 "El portal está cerca..."
- 🗺️ 40% del camino (cercano)

#### **Waypoint 4 - 10 minutos (20% distancia)**
- 🎯 Nombre: **§5§l⚡ PORTAL EMERGENTE**
- 📢 "§c§l¡EL PORTAL ESTÁ MUY CERCA!"
- 🗺️ 20% del camino (muy cerca)
- 🚀 **ACTIVA:** Sistema de empujes

#### **Waypoint 5 - 3 minutos (5% distancia)**
- 🎯 Nombre: **§c§l⚡ EPICENTRO DEL VACÍO**
- 📢 "§c§l¡¡EL PORTAL ESTÁ AQUÍ!!"
- 🗺️ 5% del camino (casi en el portal)
- ⚡ **ACTIVA:** Teletransporte intensivo

---

## 🌟 Efectos Visuales de Waypoints

### **Cada Waypoint Incluye:**

**1. Aparición Épica:**
- 💥 3 explosiones consecutivas (1 por segundo)
- ⭐ 100 partículas END_ROD en espiral
- 🌀 200 partículas PORTAL
- 🔊 Sonidos: END_PORTAL_SPAWN + ENDER_DRAGON_GROWL

**2. Efectos Continuos:**
- 📡 **Pilar de luz:** 50 bloques hacia el cielo
  - Partículas: END_ROD + PORTAL
  - Actualización: Cada 0.5 segundos
  
- ⭕ **Anillo en el suelo:** Radio de 3 bloques
  - Partículas: REVERSE_PORTAL
  - Rotación continua

**3. Guía Individual por Jugador:**
- 🎯 Camino de partículas ENCHANT
- 📏 10 puntos de 3 bloques cada uno
- 👁️ Solo visible para el jugador
- ✅ Solo si está a 10-500 bloques del waypoint

---

## 🚀 Sistema de Acercamiento Dinámico

### **Nivel 1: Empujes Suaves (10 minutos)**

**Activación:** Cuando aparece el Waypoint 4

**Características:**
- ⏰ **Frecuencia:** Cada 30 segundos
- 📏 **Rango:** Solo jugadores >100 bloques del portal
- 💨 **Intensidad:** Vector 0.3 (suave)
- 📢 **Mensaje:** "§5§l» §7Algo te atrae hacia el portal... §5§l«"
- ✨ **Efectos:** 
  - 20 partículas PORTAL
  - Sonido: ENDERMAN_TELEPORT

### **Nivel 2: Teletransporte Intensivo (3 minutos)**

**Activación:** Cuando aparece el Waypoint 5 (Final)

**Características:**
- ⏰ **Frecuencia:** Cada 15 segundos (más agresivo)
- 📏 **Rango:** Solo jugadores >200 bloques del portal
- ⚡ **Efecto:** Teletransporte de 100 bloques hacia el portal
- 🎬 **Título:** "§5§l⚡ El Vacío te arrastra"
- 💬 **Mensaje:** "[...] El portal te llama..."
- ✨ **Efectos Pre-TP:**
  - 100 partículas PORTAL en origen
  - Sonido: ENDERMAN_TELEPORT (pitch 0.8)
- ✨ **Efectos Post-TP:**
  - 100 partículas REVERSE_PORTAL en destino
  - Sonido: ENDERMAN_TELEPORT (pitch 1.2)

---

## 🎮 Nuevos Comandos de Testing

### **`/avo evento5 tp` (NUEVO)**
**Aliases:** `/avo evento5 teleport`

**Función:**
- Teletransporta al jugador cerca del portal (10 bloques)
- Ajusta altura automáticamente al bloque más alto
- Útil para testing de proximidad y tareas

**Uso:**
```
/avo evento5 tp
```

**Respuesta:**
```
✓ Teletransportado cerca del portal para testing.
```

### **Tab Completion Actualizado**
Agregado autocompletado para:
- `tp`
- `teleport`

---

## 📊 Resumen de Archivos Modificados

### **AperturaEndEvent.java** (+260 líneas)
**Nuevos métodos:**
- `crearWaypoint(double factorDistancia, String nombre, String mensaje)`
- `calcularCentroJugadores()`
- `iniciarEmpujesHaciaPortal()`
- `iniciarTeletransporteIntensivo()`
- `getPortalLocation()` - Getter para comandos

**Modificaciones:**
- Sistema de tareas completamente rediseñado (3 listeners)
- Mensajes de instrucciones iniciales
- Integración de waypoints en checkpoints de tiempo
- Spawn automático de Endermans cada 2 minutos
- Fix del comando next (+5 segundos offset)

### **ApocalipsisCommand.java** (+28 líneas)
**Nuevo caso:** `tp` / `teleport`
- Validación de jugador
- Validación de evento activo
- Validación de portal definido
- Teletransporte con ajuste de altura

### **AvoTabCompleter.java** (+2 sugerencias)
**Agregado:**
- "tp"
- "teleport"

---

## 📈 Mejoras de Experiencia de Usuario

### **Antes:**
❌ No se sabía qué hacer
❌ Tareas genéricas sin sentido
❌ Sin guía hacia el portal
❌ Fase 1 = solo esperar 45 minutos
❌ Comando next no funcionaba
❌ Sin Endermans para cazar

### **Ahora:**
✅ Instrucciones claras al inicio
✅ Tareas coherentes con el End
✅ 5 waypoints guiando progresivamente
✅ Spawn continuo de Endermans (cada 2 min)
✅ Empujes suaves hacia el portal (10 min)
✅ Teletransporte forzado (3 min finales)
✅ Comando next funcional para testing
✅ Comando tp para testing rápido
✅ Fase 1 completamente dinámica y activa

---

## 🎬 Timeline de la Fase 1 Mejorada

```
45:00 - INICIO
  ├─ Mensaje de instrucciones (3 tareas)
  └─ Spawn de Endermans cada 2 minutos

40:00 - WAYPOINT 1 (ECO DISTANTE)
  └─ Guía visual + pilar de luz

30:00 - WAYPOINT 2 (RESONANCIA CRECIENTE)
  └─ Acercamiento progresivo

20:00 - WAYPOINT 3 (LLAMADO DEL VACÍO)
  └─ Portal cercano

10:00 - WAYPOINT 4 (PORTAL EMERGENTE)
  ├─ ACTIVACIÓN: Empujes cada 30s
  └─ Jugadores lejos son empujados suavemente

03:00 - WAYPOINT 5 (EPICENTRO DEL VACÍO)
  ├─ ACTIVACIÓN: Teletransporte cada 15s
  └─ Jugadores muy lejos son teletransportados

00:00 - PORTAL SE ACTIVA
  └─ Inicio Fase 2
```

---

## 🐛 Bugs Corregidos

### **Bug #1: Comando next no mostraba diálogos**
- **Causa:** Timer se establecía al valor exacto del checkpoint
- **Síntoma:** Diálogos nunca se ejecutaban al usar next
- **Solución:** Offset de +5 segundos antes del checkpoint
- **Estado:** ✅ Resuelto

### **Bug #2: Sin método getPortalLocation()**
- **Causa:** Método getter faltante
- **Síntoma:** Error de compilación en comando tp
- **Solución:** Agregado getter público
- **Estado:** ✅ Resuelto

---

## 📦 Información de Build

**Versión:** 1.22.48
**JAR:** Apocalipsis-1.22.48.jar
**Tamaño:** 0.69 MB
**Estado:** ✅ BUILD SUCCESS
**Errores de compilación:** 0
**Warnings:** Solo deprecations de API antigua

---

## 🔮 Impacto en Gameplay

### **Engagement:**
- ⬆️ **Actividad constante:** Ya no es solo esperar
- ⬆️ **Feedback visual:** Waypoints y efectos guían a los jugadores
- ⬆️ **Progresión forzada:** Sistema evita AFK/inactividad
- ⬆️ **Claridad:** Jugadores saben exactamente qué hacer

### **Narrativa:**
- 🎭 **Coherencia temática:** Todo relacionado con el End
- 📖 **Historia clara:** Tareas cuentan una narrativa
- 🎬 **Cinematografía:** Efectos visuales épicos en cada hito
- 💭 **Inmersión:** Mensajes del Observador crean atmósfera

### **Testing:**
- 🚀 **Comando next funcional:** Testing rápido de diálogos
- 📍 **Comando tp:** Verificación rápida de proximidad
- ⏱️ **Offset de tiempo:** Diálogos se ejecutan correctamente

---

## 📝 Notas para Jugadores

### **Consejos para la Fase 1:**

1. **Lee las instrucciones iniciales** - Aparecen 2 segundos después del inicio
2. **Completa las 3 tareas** - Necesarias para revelar la ubicación del portal
3. **Sigue los waypoints** - Aparecen progresivamente guiándote al portal
4. **Busca Endermans** - Aparecen cada 2 minutos cerca de los jugadores
5. **No te alejes** - El sistema te empujará/teletransportará si estás muy lejos

### **Indicadores Visuales:**

- 💜 **Pilar morado hacia el cielo** = Waypoint activo
- ✨ **Camino de partículas verdes** = Ruta hacia el waypoint
- 🌀 **Remolino de partículas** = Enderman spawneando
- ⚡ **Flash de partículas rojas** = Empuje hacia el portal
- 🌠 **Explosión de partículas** = Teletransporte

---

## 🎯 Próximas Mejoras Sugeridas

- [ ] Sistema de recompensas por completar tareas rápido
- [ ] Leaderboard de quién completó más tareas
- [ ] Más tipos de Endermans especiales
- [ ] Waypoints opcionales con loot
- [ ] Achievements por llegar a cada waypoint

---

## 👨‍💻 Información Técnica

**Compatibilidad:**
- Minecraft: 1.21.8
- Bukkit/Paper API: Latest
- Java: 21

**Dependencias:**
- Adventure API (para títulos/mensajes)
- Paper API (para partículas avanzadas)

**Performance:**
- ✅ Optimizado con BukkitRunnable
- ✅ Partículas solo para jugadores cercanos
- ✅ Tasks canceladas automáticamente al cambiar de fase

---

## 📅 Fecha de Release

**Versión:** 1.22.48
**Fecha:** 19 de Enero, 2026
**Compilación:** Exitosa
**Estado:** Listo para producción

---

**¡Gracias por usar el Sistema de Eventos de Apocalipsis! 🎮⚡**
