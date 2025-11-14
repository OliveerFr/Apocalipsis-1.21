# 🎭 EVENTOS vs DESASTRES - Separación de Arquitectura

## 📋 Resumen de Cambios

Se ha creado una **arquitectura completamente nueva** para eventos narrativos únicos, separándola del sistema de desastres automáticos.

---

## 🏗️ Nueva Estructura de Carpetas

```
src/main/java/me/apocalipsis/
├── disaster/                    # Sistema de desastres (EXISTENTE)
│   ├── Disaster.java
│   ├── DisasterBase.java
│   ├── DisasterController.java
│   ├── DisasterRegistry.java
│   ├── HuracanNew.java
│   ├── LluviaFuegoNew.java
│   └── TerremotoNew.java
│
└── events/                      # Sistema de eventos (NUEVO ✨)
    ├── EventBase.java           # ✅ Clase abstracta para eventos
    ├── EventController.java     # ✅ Controlador sin ciclo automático
    └── EcoBrasasEvent.java      # ⏳ PENDIENTE (refactorizar de EcoBrasasNew)
```

```
src/main/resources/
├── desastres.yml     # Solo desastres automáticos (Huracan, Lluvia, Terremoto)
└── eventos.yml       # ✅ NUEVO - Config narrativa de eventos únicos
```

---

## 🆚 Diferencias Clave: Desastres vs Eventos

| Aspecto | **Desastres** | **Eventos** |
|---------|--------------|------------|
| **Controlador** | `DisasterController` | `EventController` ✨ |
| **Inicio** | Automático (ciclo + weights) | Manual (`/avo eco start`) |
| **Config** | `desastres.yml` | `eventos.yml` ✨ |
| **Registro** | `DisasterRegistry` | `EventController.registerEvent()` |
| **Cooldown** | Sí (600s entre desastres) | No (evento único) |
| **Narrativa** | No tiene | **Observador como narrador** ✨ |
| **Cinematics** | No tiene | **Delays, sonidos, títulos** ✨ |
| **Fases** | Intensidad variable | **Fases con transiciones épicas** ✨ |
| **Objetivo** | Sobrevivir | **Completar historia cooperativa** ✨ |
| **Recompensas** | No tiene | **Loot + sensación de logro** ✨ |

---

## ✅ Cambios Completados

### 1. **Creada Arquitectura de Eventos**
- ✅ `EventBase.java` - Clase abstracta con métodos `onStart()`, `onStop()`, `onTick()`
- ✅ `EventController.java` - Gestión de eventos sin ciclo automático
- ✅ Métodos: `registerEvent()`, `startEvent()`, `stopActiveEvent()`, `tick()`

### 2. **Configuración de Eventos**
- ✅ `eventos.yml` creado con estructura completa:
  ```yaml
  eventos:
    eco_brasas:
      narrativa:
        intro:                  # Cinemática al iniciar
        transicion_fase2:       # Cinemática Fase 1→2
        transicion_fase3:       # Cinemática Fase 2→3
        victoria:               # Cinemática final
      dialogos_observador:      # Mensajes periódicos
      fase1: {...}              # Recolección (grietas)
      fase2: {...}              # Estabilización (anclas)
      fase3: {...}              # Ritual Final (altar + boss)
  ```

### 3. **Limpieza de desastres.yml**
- ✅ Eliminada sección `eco_brasas` completa
- ✅ Removido `eco_brasas: 1` de weights
- ✅ Ahora solo contiene: `huracan`, `lluvia_fuego`, `terremoto`

### 4. **DisasterRegistry Actualizado**
- ✅ Eliminado `register(new EcoBrasasNew(...))`
- ✅ Eliminado `import me.apocalipsis.events.EcoBrasasNew`
- ✅ Comentario añadido: "EcoBrasas movido a EventController"

### 5. **Documentación Creada**
- ✅ `FLUJO_NARRATIVO_ECO_DE_BRASAS.md` - Timeline completo del evento:
  - 00:00 - Inicio con cinemática intro
  - 00:05-25:00 - Fase 1 (Recolección)
  - 25:00-70:00 - Fase 2 (Estabilización)
  - 70:00-85:00 - Fase 3 (Ritual Final)
  - Victoria - Cinemática épica
- ✅ Explicación detallada de cada paso automático
- ✅ Comparación Desastre vs Evento

---

## ⏳ Pendiente de Implementación

### 1. **Refactorizar EcoBrasasNew.java** (CRÍTICO)
```java
// ANTES (en disaster/)
public class EcoBrasasNew extends DisasterBase {...}

// DESPUÉS (en events/)
public class EcoBrasasEvent extends EventBase {
    @Override
    public void onStart() {
        // Iniciar cinemática intro (5s delay)
        scheduleIntroSequence();
    }
    
    @Override
    public void onTick() {
        // Progresión automática de fases
        checkPhaseTransition();
        
        // Diálogos periódicos del Observador
        if (shouldShowDialogue()) {
            showObservadorDialogue();
        }
    }
    
    private void scheduleIntroSequence() {
        // NUEVO: Cinematics con delays
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Sonido + mensajes + título
        }, 5 * 20); // 5 segundos
    }
}
```

**Cambios necesarios:**
- Mover archivo de `disaster/` a `events/`
- Cambiar `extends DisasterBase` → `extends EventBase`
- Implementar cinematics automáticos (delays, sonidos, títulos)
- Leer configuración de `eventos.yml` en lugar de `desastres.yml`
- Sistema de diálogos periódicos del Observador
- Transiciones automáticas entre fases con pausas dramáticas

### 2. **Actualizar ApocalipsisCommand.java**
```java
// ANTES
private void cmdEco(CommandSender sender, String[] args) {
    me.apocalipsis.disaster.EcoBrasasNew ecoBrasas = null;
    if (disasterController.getActiveDisaster() instanceof ...) {...}
    disasterController.startDisaster("eco_brasas");
}

// DESPUÉS
private void cmdEco(CommandSender sender, String[] args) {
    EcoBrasasEvent ecoBrasas = null;
    if (eventController.getActiveEvent() instanceof EcoBrasasEvent) {
        ecoBrasas = (EcoBrasasEvent) eventController.getActiveEvent();
    }
    
    switch (subCmd) {
        case "start":
            eventController.startEvent("eco_brasas"); // Usa EventController
            break;
        case "stop":
            eventController.stopActiveEvent();
            break;
        // ... resto igual
    }
}
```

**Cambios necesarios:**
- Cambiar todas las referencias de `DisasterController` → `EventController`
- Usar `eventController.startEvent()` en lugar de `disasterController.startDisaster()`
- Verificar conflictos: no permitir evento si hay desastre activo (y viceversa)

### 3. **Integrar EventController en Apocalipsis.java**
```java
public class Apocalipsis extends JavaPlugin {
    private DisasterController disasterController;
    private EventController eventController; // NUEVO
    
    @Override
    public void onEnable() {
        // ... inicialización existente ...
        
        // Inicializar EventController
        eventController = new EventController(this);
        
        // Registrar eventos
        EcoBrasasEvent ecoBrasas = new EcoBrasasEvent(
            this, messageBus, soundUtil, timeService
        );
        eventController.registerEvent(ecoBrasas);
        
        // Iniciar tick loop
        getServer().getScheduler().runTaskTimer(this, () -> {
            disasterController.tick(); // Existente
            eventController.tick();     // NUEVO - tick de eventos
        }, 0L, 1L);
    }
    
    public EventController getEventController() {
        return eventController;
    }
}
```

**Cambios necesarios:**
- Añadir field `EventController eventController`
- Inicializar en `onEnable()`
- Registrar `EcoBrasasEvent`
- Llamar `eventController.tick()` en el tick loop principal
- Crear getter `getEventController()`
- Actualizar `ApocalipsisCommand` para recibir `EventController` en constructor

---

## 🎯 Garantías de Separación

### ✅ Eco de Brasas NO puede iniciarse automáticamente

1. **No está en DisasterRegistry**
   ```java
   // DisasterRegistry.java - LÍNEA ELIMINADA
   // register(new EcoBrasasNew(...)); // ❌ Ya no existe
   ```

2. **No tiene weight en desastres.yml**
   ```yaml
   weights:
     huracan: 1
     lluvia_fuego: 1
     terremoto: 1
     # eco_brasas: NO INCLUIDO ✅
   ```

3. **DisasterController no lo reconoce**
   - `disasterController.startDisaster("eco_brasas")` → **Retorna false** (no existe)
   - Ciclo automático solo considera: huracan, lluvia_fuego, terremoto

4. **EventController es manual**
   ```java
   // EventController.java
   // NO tiene método autoStart()
   // NO tiene sistema de weights
   // NO tiene cooldown automático
   ```

### ✅ Sistema de eventos es independiente

```
DisasterController                EventController
├── Auto-start (ciclo)           ├── Manual start (/comando)
├── Weights (probabilidades)     ├── Sin weights
├── Cooldown (600s)              ├── Sin cooldown
├── Múltiples simultáneos        ├── Solo uno a la vez
└── Sin narrativa                └── Narrativa rica (Observador)
```

---

## 📝 Notas de Implementación

### ConfigManager debe cargar eventos.yml
```java
// En ConfigManager.java - AÑADIR
private FileConfiguration eventosConfig;

public void loadEventosConfig() {
    File eventosFile = new File(plugin.getDataFolder(), "eventos.yml");
    if (!eventosFile.exists()) {
        plugin.saveResource("eventos.yml", false);
    }
    eventosConfig = YamlConfiguration.loadConfiguration(eventosFile);
}

public FileConfiguration getEventosConfig() {
    return eventosConfig;
}
```

### EcoBrasasEvent debe leer eventos.yml
```java
// En EcoBrasasEvent.java
private void loadConfig() {
    ConfigurationSection config = plugin.getConfigManager()
        .getEventosConfig()
        .getConfigurationSection("eventos.eco_brasas");
    
    // Leer narrativa
    ConfigurationSection intro = config.getConfigurationSection("narrativa.intro");
    introDelay = intro.getInt("delay_seg", 5);
    introMessages = intro.getStringList("mensajes");
    
    // ... etc
}
```

---

## 🎬 Cómo Debería Funcionar el Evento

Ver documento completo: **`FLUJO_NARRATIVO_ECO_DE_BRASAS.md`**

**Resumen ultra-corto:**

1. Admin ejecuta `/avo eco start`
2. **Pausa 5s** → Cinemática intro (sonidos + mensajes + título)
3. **Fase 1 (25 min)**: Grietas spawneadas cada 8 min, jugadores las cierran y recolectan fragmentos
4. **Pausa 10s** → Cinemática transición (Observador anuncia anclas)
5. **Fase 2 (45 min)**: 3 anclas requieren entregas cooperativas de fragmentos
6. **Pausa 15s** → Cinemática épica (altar se manifiesta)
7. **Fase 3 (15 min)**: Jugadores clickean altar (100 pulsos) + boss spawneado al 75%
8. **Victoria** → Cinemática final (fuegos artificiales + recompensas)

**Durante todo el evento:**
- Observador habla cada 3 minutos (diálogos aleatorios)
- Hologramas muestran progreso en tiempo real
- Sonidos atmosféricos constantes
- Feedback inmediato a cada acción

---

## ✅ Estado Actual

| Tarea | Estado | Archivo |
|-------|--------|---------|
| Arquitectura base eventos | ✅ Completado | `EventBase.java`, `EventController.java` |
| Config eventos.yml | ✅ Completado | `eventos.yml` |
| Limpieza desastres.yml | ✅ Completado | `desastres.yml` |
| Limpieza DisasterRegistry | ✅ Completado | `DisasterRegistry.java` |
| Documentación flujo | ✅ Completado | `FLUJO_NARRATIVO_ECO_DE_BRASAS.md` |
| Refactorizar EcoBrasasNew | ⏳ Pendiente | `events/EcoBrasasEvent.java` |
| Actualizar comandos | ⏳ Pendiente | `ApocalipsisCommand.java` |
| Integrar EventController | ⏳ Pendiente | `Apocalipsis.java` |

---

## 🚀 Próximos Pasos

1. **Refactorizar EcoBrasasNew** → `EcoBrasasEvent extends EventBase`
   - Implementar cinematics automáticos
   - Sistema de diálogos periódicos
   - Leer de `eventos.yml`

2. **Actualizar ApocalipsisCommand**
   - Usar `EventController` en lugar de `DisasterController`
   - Mantener mismos comandos (`/avo eco start`, etc.)

3. **Integrar en Apocalipsis.java**
   - Inicializar `EventController`
   - Registrar `EcoBrasasEvent`
   - Añadir a tick loop

4. **Testing**
   - Verificar que NO se inicia automáticamente
   - Probar cinematics y transiciones
   - Confirmar diálogos del Observador

---

## 📚 Archivos de Referencia

- **Arquitectura**: `events/EventBase.java`, `events/EventController.java`
- **Config**: `eventos.yml`
- **Documentación**: `FLUJO_NARRATIVO_ECO_DE_BRASAS.md`
- **Comandos**: `COMANDOS_ECO_DE_BRASAS.md`
- **Este documento**: `EVENTOS_VS_DESASTRES.md`

---

**Resumen en una línea:** Eco de Brasas ahora es un **evento narrativo independiente** con cinematics automáticos, completamente separado del sistema de desastres aleatorios. 🎭✨
