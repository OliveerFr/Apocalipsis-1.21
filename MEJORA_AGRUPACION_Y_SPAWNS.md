# 🎭 Sistema de Brújula Direccional y Spawns Dramáticos - Evento 5

## 📋 Resumen de Cambios

Se implementaron dos sistemas nuevos para el **Evento 5: La Apertura del End** que aumentan el suspenso y fomentan el trabajo en equipo:

### ✅ 1. Agrupación Requerida para Brújula Direccional
Los jugadores **DEBEN estar juntos** para revelar la dirección al portal (NO coordenadas exactas).

### ✅ 2. Spawns Dramáticos Durante el Viaje
Mientras viajan al portal, aparecen entidades del End generando suspenso.

---

## 🧭 Sistema de Brújula Direccional

### Funcionamiento

1. **Inicio del Evento**: El evento comienza con el mensaje inicial, pero **SIN dirección revelada**
2. **Verificación Automática**: Cada 5 segundos verifica si los jugadores están agrupados
3. **Recordatorio**: Cada 30 segundos muestra un mensaje si siguen dispersos:
   ```
   §c⚠ Los jugadores están muy dispersos. Agrúpense para descubrir el camino al portal.
   ```
4. **Dirección Revelada**: Cuando detecta que están agrupados:
   ```
   §8[§7...§8] §7Deben estar juntos para descubrir el camino.
   
   [3 segundos después con efectos épicos]
   
   §5§l⚡ El camino se revela ⚡
   Sigan la flecha...
   El portal está muy lejos.
   ```

5. **Brújula en Tiempo Real**: Cada jugador ve en su actionbar:
   ```
   ⬆ Norte ⚡ 3.2 km
   ⬈ Noreste ⚡ 2847 bloques
   ➡ Este ⚡ 451 bloques
   ```

### Sistema de Flechas Direccionales

La brújula muestra **flechas Unicode** que apuntan hacia donde el jugador debe ir:

| Flecha | Dirección | Significado |
|--------|-----------|-------------|
| ⬆ | Adelante | El portal está frente a ti |
| ⬈ | Adelante-Derecha | Gira un poco a la derecha |
| ➡ | Derecha | Gira 90° a la derecha |
| ⬊ | Atrás-Derecha | Da la vuelta girando a la derecha |
| ⬇ | Atrás | El portal está detrás de ti |
| ⬋ | Atrás-Izquierda | Da la vuelta girando a la izquierda |
| ⬅ | Izquierda | Gira 90° a la izquierda |
| ⬉ | Adelante-Izquierda | Gira un poco a la izquierda |

### Direcciones Cardinales

Además de la flecha relativa, muestra la dirección cardinal absoluta:
- **Norte** (N)
- **Noreste** (NE)
- **Este** (E)
- **Sureste** (SE)
- **Sur** (S)
- **Suroeste** (SW)
- **Oeste** (W)
- **Noroeste** (NW)

### Código de Colores por Distancia

La distancia cambia de color según cercanía:

| Distancia | Color | Ejemplo |
|-----------|-------|---------|
| > 1000 bloques | §c Rojo | §c3.2 km |
| 500-1000 bloques | §6 Dorado | §62847 bloques |
| 100-500 bloques | §e Amarillo | §e451 bloques |
| < 100 bloques | §a Verde | §a87 bloques |

### Ejemplo en Acción

```
Jugador mirando al Sur, portal al Norte:
⬇ Sur ⚡ §c4.5 km

Jugador gira hacia el Norte:
⬆ Norte ⚡ §c4.5 km

Jugador camina 2km hacia el Norte:
⬆ Norte ⚡ §62.5 km

Jugador cerca del portal:
⬈ Noreste ⚡ §a45 bloques
```

### Configuración (apertura_end.yml)

```yaml
portal:
  agrupacion_requerida:
    enabled: true                # Activar/desactivar sistema
    radio_maximo: 50             # Distancia máxima entre jugadores (bloques)
    jugadores_minimos: 2         # Mínimo de jugadores para requerir agrupación
    mensaje_agrupacion: "§8[§7...§8] §7Deben estar juntos para descubrir el camino."
    mensaje_dispersos: "§c⚠ Los jugadores están muy dispersos. Agrúpense para descubrir el camino al portal."
```

### Efectos Visuales

Al revelar la brújula:
- 🔊 **Sonido**: `BLOCK_END_PORTAL_FRAME_FILL` (volumen 2.0)
- ✨ **Partículas**: 100 partículas PORTAL alrededor de cada jugador
- 📊 **Actionbar**: Actualización cada 0.5 segundos con dirección en tiempo real

---

## 👹 Sistema de Spawns Dramáticos

### Funcionamiento

Durante la **Fase 1: DESCUBRIMIENTO** (45 minutos de viaje al portal):
- ⏱️ **Intervalo Aleatorio**: Cada 2-5 minutos aparece un spawn
- 📍 **Ubicación**: Entre 15-30 bloques de un jugador aleatorio
- 🎲 **Tipo Aleatorio**: Se elige basado en probabilidades configuradas
- 💬 **Mensaje Atmosférico**: Cada spawn tiene su mensaje del Observador
- 🎨 **Efectos Únicos**: Partículas y sonidos específicos por tipo

### Entidades Configuradas

#### 1. 👤 Enderman (40% probabilidad)
- **Cantidad**: 2-5 Enderman
- **Mensaje**: `§8[§7...§8] §7Están observando.`
- **Efectos**: 
  - 50 partículas PORTAL (radio 3)
  - 30 partículas REVERSE_PORTAL (radio 2)
- **Sonido**: Grito de Enderman

#### 2. 🪱 Endermite (25% probabilidad)
- **Cantidad**: 3-8 Endermites
- **Mensaje**: `§8[§7...§8] §7Algo se arrastra desde el vacío.`
- **Efectos**: 40 partículas END_ROD (radio 2)
- **Sonido**: Silverfish ambiente

#### 3. 📦 Shulker (20% probabilidad)
- **Cantidad**: 1-3 Shulkers
- **Mensaje**: `§8[§7...§8] §7No deberían estar aquí.`
- **Efectos**:
  - 60 partículas DRAGON_BREATH (radio 3)
  - 40 partículas PORTAL (radio 2)
- **Sonido**: Shulker ambiente

#### 4. 🦇 Phantom (15% probabilidad)
- **Cantidad**: 2-4 Phantoms
- **Mensaje**: `§8[§7...§8] §7El cielo también está corrupto.`
- **Efectos**:
  - 30 partículas SMOKE (radio 4)
  - 20 partículas END_ROD (radio 3)
- **Sonido**: Phantom ambiente

### Configuración (apertura_end.yml)

```yaml
spawns_dramaticos:
  enabled: true
  intervalo_minimo: 120    # Mínimo 2 minutos entre spawns
  intervalo_maximo: 300    # Máximo 5 minutos entre spawns
  radio_spawn: 30          # Spawns aparecen entre 15-30 bloques
  
  entidades:
    - tipo: "ENDERMAN"
      probabilidad: 40
      cantidad_min: 2
      cantidad_max: 5
      mensaje: "§8[§7...§8] §7Están observando."
      efectos_aparicion:
        - "PORTAL:50:3"
        - "REVERSE_PORTAL:30:2"
      sonido: "ENTITY_ENDERMAN_SCREAM:1.5:0.7"
```

---

## 🎬 Flujo del Evento Completo

### Inicio (0:00)
```
1. Jugadores ejecutan: /avo evento5 start
2. Mensaje inicial aparece (SIN dirección revelada)
3. Sistema verifica agrupación cada 5 segundos
4. Si dispersos: Mensaje de recordatorio cada 30s
```

### Agrupación Detectada
```
1. "Deben estar juntos para descubrir el camino."
2. [3 segundos] Efectos épicos (PORTAL + sonido)
3. "El camino se revela" - Sistema de brújula activado
4. Actionbar muestra: ⬆ Norte ⚡ 3.2 km
```

### Durante el Viaje (45 minutos)
```
Cada jugador ve en su actionbar:
  ⬆ Norte ⚡ 3.2 km    (lejos)
  ⬈ Noreste ⚡ 847m   (acercándose)
  ➡ Este ⚡ 234 bloques (cerca)
  
Mientras:
  - Spawns dramáticos cada 2-5 minutos
  - Mensajes del Observador en hitos (15min, 5min, 1min, etc.)
  - Enderman, Endermites, Shulkers, Phantoms aparecen
  - Efectos de partículas y sonidos únicos
  - Brújula se actualiza cada 0.5 segundos
```

### Llegada al Portal
```
1. Detección de jugadores cerca (50 bloques)
2. Portal se activa gradualmente (30 segundos, 8 fases)
3. Sistema de spawns se detiene
4. Brújula se desactiva
5. Continúa evento normal (entrada al End, combate)
```

---

## 🔧 Personalización

### Desactivar Agrupación
```yaml
portal:
  agrupacion_requerida:
    enabled: false  # Brújula inmediata, sin esperar agrupación
```

### Velocidad de Actualización de Brújula
En el código Java, modificar:
```java
.runTaskTimer(plugin, 0L, 10L); // 10 ticks = 0.5 segundos
// Cambiar a:
.runTaskTimer(plugin, 0L, 5L);  // 5 ticks = 0.25 segundos (más rápido)
.runTaskTimer(plugin, 0L, 20L); // 20 ticks = 1 segundo (más lento)
```

### Cambiar Frecuencia de Spawns
```yaml
spawns_dramaticos:
  intervalo_minimo: 60   # Spawns cada 1-3 minutos (más intenso)
  intervalo_maximo: 180
```

### Añadir Nueva Entidad
```yaml
- tipo: "WITHER_SKELETON"
  probabilidad: 10
  cantidad_min: 1
  cantidad_max: 3
  mensaje: "§8[§7...§8] §7Guerreros olvidados despiertan."
  efectos_aparicion:
    - "SOUL_FIRE_FLAME:40:2"
    - "SMOKE:30:3"
  sonido: "ENTITY_WITHER_SKELETON_AMBIENT:1.2:0.6"
```

### Ajustar Radio de Agrupación
```yaml
agrupacion_requerida:
  radio_maximo: 30   # Jugadores más cerca (más difícil)
  # o
  radio_maximo: 100  # Jugadores más lejos (más fácil)
```

---

## 📊 Estadísticas Técnicas

### Sistema de Brújula
- ✅ Actualización cada 0.5 segundos (10 ticks)
- ✅ 8 direcciones relativas (flechas Unicode)
- ✅ 8 direcciones cardinales (Norte, Sur, Este, Oeste, etc.)
- ✅ Cálculo dinámico basado en yaw del jugador
- ✅ 4 rangos de distancia con código de colores
- ✅ Formato: `[Flecha] [Cardinal] ⚡ [Distancia]`
- ✅ Mostrado en actionbar (no invasivo)

### Agrupación
- ✅ Verificación cada 5 segundos (100 ticks)
- ✅ Recordatorio cada 30 segundos (6 verificaciones)
- ✅ Detección de mundo y distancia
- ✅ Fallback si menos jugadores del mínimo
- ✅ Efectos visuales al revelar (100 partículas PORTAL)

### Spawns
- ✅ Tarea que corre cada tick durante Fase 1
- ✅ Contador de ticks hasta próximo spawn
- ✅ Sistema de probabilidad ponderada
- ✅ Spawn aleatorio entre 15-30 bloques
- ✅ 4 tipos de entidades configurables
- ✅ Efectos y sonidos únicos por tipo
- ✅ Log de cada spawn en consola

---

## ✅ Estado de Implementación

| Componente | Estado |
|------------|--------|
| Sistema de agrupación | ✅ Implementado |
| Verificación periódica | ✅ Implementado |
| Mensajes de recordatorio | ✅ Implementado |
| Brújula direccional | ✅ Implementado |
| Flechas Unicode (8 direcciones) | ✅ Implementado |
| Direcciones cardinales | ✅ Implementado |
| Código de colores por distancia | ✅ Implementado |
| Actualización en tiempo real | ✅ Implementado |
| Efectos visuales | ✅ Implementado |
| Sistema de spawns | ✅ Implementado |
| 4 tipos de entidades | ✅ Implementado |
| Probabilidades configurables | ✅ Implementado |
| Efectos por entidad | ✅ Implementado |
| Mensajes atmosféricos | ✅ Implementado |
| Compilación Maven | ✅ BUILD SUCCESS |

---

## 🎯 Ventajas del Sistema de Brújula

### Inmersión Total
- 🧭 **SIN coordenadas exactas** - Navegación realista
- 📍 Brújula en tiempo real que se actualiza mientras caminan
- 🎮 Experiencia orgánica (seguir flechas vs copiar coordenadas)
- 🗺️ Los jugadores deben prestar atención constantemente

### Trabajo en Equipo Mejorado
- 🤝 Deben reunirse antes del viaje
- 📞 Comunicación constante durante el viaje
- 🎯 Todos ven la misma dirección desde su perspectiva
- 🧭 "Voy al Norte" tiene significado real

### Suspenso Aumentado
- 👹 Spawns inesperados + navegación incierta
- ⏱️ No saben exactamente qué tan lejos están
- 💬 Deben confiar en la brújula
- 🎯 Más desafiante sin F3

---

## 🎮 Comandos de Testing

### Iniciar Evento Normal
```
/avo evento5 start
```

### Iniciar con Cuenta Regresiva
```
/avo evento5 start 5   (5 minutos de suspenso)
```

### Verificar Logs
```
[Apertura End] Evento iniciado - Fase de preparación
[Apertura End] Coordenadas del portal anunciadas a jugadores agrupados
[Apertura End] Spawn dramático: 3 x ENDERMAN cerca de Riolu
[Apertura End] Spawn dramático: 5 x ENDERMITE cerca de Player2
```

---

## 🎯 Ventajas del Sistema

### Trabajo en Equipo
- 🤝 Los jugadores DEBEN reunirse antes de iniciar el viaje
- 📞 Fomenta comunicación ("¿Dónde estás?", "Vamos al spawn")
- 🎯 Crea punto de partida común

### Suspenso Dramático
- 👹 Entidades aparecen inesperadamente durante el viaje
- ⏱️ Intervalos aleatorios (2-5 min) = imprevisibilidad
- 💬 Mensajes del Observador refuerzan atmósfera

### Experiencia Cinematográfica
- 🎬 Revelación épica de coordenadas con efectos
- 🎨 Cada spawn tiene efectos y sonidos únicos
- 📖 Narrativa inmersiva con mensajes atmosféricos

### Balanceo
- ⚖️ Sistema completamente configurable
- 🔧 Desactivable si no se desea
- 📊 Probabilidades ajustables por tipo de entidad

---

**¡Sistema completamente funcional y listo para testing! 🎉**
