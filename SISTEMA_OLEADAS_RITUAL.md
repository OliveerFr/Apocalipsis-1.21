# Sistema de Oleadas para Ritual del Eco de Brasas

## 📋 Resumen
Sistema dinámico de oleadas que transforma el ritual en una experiencia progresiva con múltiples sub-fases, defensores enemigos, efectos visuales intensificados y bloqueos estratégicos. **Se adapta automáticamente a la cantidad de pulsos configurada en eventos.yml**.

---

## ⚙️ Configuración Dinámica

### **Cálculo Automático**
El sistema calcula automáticamente los triggers de oleadas basándose en `pulsos_requeridos`:

```yaml
# eventos.yml
fase3:
  altar:
    pulsos_requeridos: 20  # Configurable - oleadas cada 4 pulsos (20%)
```

**Fórmula**: `intervaloOleadas = max(2, pulsoMaximo / 5)` (cada 20% del total)

### **Ejemplos de Escalado**

| pulsos_requeridos | Intervalo | Oleadas en pulsos |
|------------------|-----------|-------------------|
| 5 | 2 | 2, 4 |
| 8 | 2 | 2, 4, 6, 8 |
| 10 | 2 | 2, 4, 6, 8, 10 |
| 20 | 4 | 4, 8, 12, 16, 20 |
| 25 | 5 | 5, 10, 15, 20, 25 |
| 50 | 10 | 10, 20, 30, 40, 50 |

### **Spawn del Guardián**
- **Automático**: 75% de `pulsos_requeridos` si no se especifica
- **Manual**: Configurable en `fase3.guardian.spawn_en_pulso`

Ejemplos:
- Si `pulsos_requeridos = 20` → guardián en pulso 15 (o valor manual 17)
- Si `pulsos_requeridos = 8` → guardián en pulso 6

---

## ⚔️ Mecánica Principal

### **Oleadas Automáticas**
- **Trigger**: Cada 20% de `pulsos_requeridos` (calculado dinámicamente)
  - Ejemplo con 20 pulsos: cada 4 pulsos → 4, 8, 12, 16, 20
  - Ejemplo con 8 pulsos: cada 2 pulsos → 2, 4, 6, 8
  - Ejemplo con 50 pulsos: cada 10 pulsos → 10, 20, 30, 40, 50
- **Bloqueo**: Los jugadores NO pueden añadir pulsos mientras hay oleada activa
- **Completar**: Eliminar TODOS los enemigos spawneados en la oleada
- **Estado**: 3 estados posibles
  - `ESPERANDO`: No hay oleada activa, se puede añadir pulsos
  - `ACTIVA`: Oleada en curso, ritual bloqueado
  - `COMPLETADA`: Oleada eliminada, permite continuar (3 segundos de gracia)

### **Spawn de Enemigos**
- **Cantidad**: 2-7 enemigos según intensidad del ritual (0-100%)
- **Ubicación**: Círculo de radio 10 bloques alrededor del altar
- **Detección de suelo**: Busca bloques sólidos para evitar spawn en aire
- **HP escalado**: 1.0x a 2.0x según fase del ritual

---

## 🎯 Fases de Intensidad

### **Fase Inicial (0-25%)** - Pulsos 1-5
**Enemigos**: 2 base
- 50% Zombies
- 50% Esqueletos
- HP: 1.0x (normal)

**Efectos**:
- Partículas: Soul Fire Flame (5) cada 80 ticks
- Sonido: Respawn Anchor Ambient (volumen 0.3)

---

### **Fase Media (25-50%)** - Pulsos 6-10
**Enemigos**: 3 base
- 30% Zombies
- 30% Esqueletos
- 20% Spiders
- 20% Creepers
- HP: 1.25x

**Efectos**:
- Partículas: Soul Fire Flame (10) + Smoke (5) cada 60 ticks
- Sonido: Respawn Anchor Charge (volumen 0.5)

---

### **Fase Avanzada (50-75%)** - Pulsos 11-15
**Enemigos**: 4 base
- 25% Zombies
- 25% Esqueletos
- 20% Blazes
- 30% Piglin Brutes
- HP: 1.5x

**Efectos**:
- Partículas: Soul Fire Flame (15) + Flame (10) + Lava (5) cada 40 ticks
- Sonido: Wither Ambient (volumen 0.4)

---

### **Fase Final (75-100%)** - Pulsos 16-20
**Enemigos**: 5-7 base
- 30% Blazes
- 30% Piglin Brutes
- 20% Wither Skeletons
- 20% Ravagers
- HP: 2.0x (doble vida)

**Efectos**:
- Partículas: Soul Fire Flame (25) + Flame (20) + Lava (10) + End Rod (15)
- Beam vertical: 10 bloques de altura con Soul Fire Flame
- Sonido: Ender Dragon Growl (volumen 0.6) cada 30 ticks

---

## 🛡️ Guardián del Eco

### **Spawn Especial**
- **Pulso**: Configurable o automático (75% del total)
  - Si `pulsos_requeridos = 20` y `spawn_en_pulso = 17` → spawn manual en pulso 17
  - Si `pulsos_requeridos = 20` y no se especifica → spawn automático en pulso 15 (75%)
  - Si `pulsos_requeridos = 8` → spawn automático en pulso 6 (75%)
- **Tratamiento**: Oleada especial que bloquea el ritual
- **Persistencia**: Mismo sistema que antes (200 HP, Wither Skeleton)

### **Integración con Oleadas**
- Al spawnear el guardián, `oleadaState` cambia a `ACTIVA`
- Los jugadores NO pueden añadir pulsos hasta derrotarlo
- Se mantiene todo el sistema anterior (tracking, death listener, drops)

---

## 🎨 Efectos Visuales Progresivos

### **Sistema de Intensidad**
```java
intensidadRitual = (pulsoActual / pulsoMaximo) * 100; // 0-100
```

### **Efectos por Fase**
| Fase | Intensidad | Partículas/seg | Sonidos/seg | Complejidad |
|------|-----------|----------------|-------------|-------------|
| Inicial | 0-25% | 5 | 0.0125 | Baja |
| Media | 25-50% | 15 | 0.0167 | Media |
| Avanzada | 50-75% | 30 | 0.025 | Alta |
| Final | 75-100% | 70+ | 0.033 | Épica |

---

## 🔊 Mensajes y Notificaciones

### **Inicio de Oleada**
```
§c§l⚔ OLEADA X §8[Fase] - §c# defensores §7aparecen!
```
- Fase Inicial: §7Inicial
- Fase Media: §eMedia
- Fase Avanzada: §6Avanzada
- Fase Final: §c§lFinal

Sonido: `ENTITY_WITHER_SPAWN` (volumen 0.5, pitch 1.2)

### **Oleada Activa (ActionBar cada 2s)**
```
§c⚔ OLEADA X §8- §7Defensores: §cN
```

### **Oleada Completada**
```
§a§l✓ OLEADA COMPLETADA §8- §7El ritual puede continuar...
```
Sonido: `UI_TOAST_CHALLENGE_COMPLETE` (volumen 1.0)
Partículas: `TOTEM_OF_UNDYING` (50 partículas)

### **Bloqueo por Oleada**
```
§c§l[Oleada Activa] §7Derrota a los §cN defensores §7para continuar
```
Sonido: `BLOCK_ANVIL_LAND` (volumen 0.5, pitch 0.8)
Partículas: `ANGRY_VILLAGER` (10 partículas)

---

## 🧩 Arquitectura Técnica

### **Nuevas Variables**
```java
private enum OleadaState { ESPERANDO, ACTIVA, COMPLETADA }
private OleadaState oleadaState = OleadaState.ESPERANDO;
private int oleadaActual = 0;
private List<Entity> enemigosOleada = new ArrayList<>();
private int intensidadRitual = 0; // 0-100
private int ultimaOleadaPulso = -1;
private int intervaloOleadas = 4; // Calculado dinámicamente en inicializarAltar()
```

### **Métodos Principales**
1. **`tickOleadas()`**: Gestiona estado de oleadas, limpia enemigos muertos, detecta completación
2. **`spawnOleada()`**: Spawn enemigos en círculo, configura HP/nombre según intensidad
3. **`seleccionarTipoEnemigo(int intensidad)`**: Elige tipo de mob según fase
4. **`completarOleada()`**: Marca oleada como completada, efectos de victoria
5. **`mostrarMarcadorOleada()`**: ActionBar con progreso + beam visual
6. **`tickEfectosRitual()`**: Efectos progresivos según intensidad (0-100)

### **Modificaciones Existentes**
- **`tickFaseRitual()`**: Añadidas llamadas a `tickEfectosRitual()` y `tickOleadas()`
- **`onAltarInteractuado()`**: Verifica `oleadaState == ACTIVA` ANTES del guardián

---

## 📊 Flujo de Ejecución

```
┌──────────────────────────────────────────┐
│ Jugador hace click en altar             │
└──────────────────────┬───────────────────┘
                       │
        ┌──────────────▼──────────────┐
        │ ¿oleadaState == ACTIVA?     │
        └──────┬──────────────┬───────┘
               │ Sí           │ No
       ┌───────▼────┐         │
       │ BLOQUEO    │         │
       │ Mensaje    │    ┌────▼─────┐
       │ + Efectos  │    │ Verificar│
       │ return     │    │ guardián │
       └────────────┘    └────┬─────┘
                              │
                         ┌────▼─────────┐
                         │ Incrementar  │
                         │ pulsoActual  │
                         └────┬─────────┘
                              │
                    ┌─────────▼──────────┐
                    │ ¿Pulso múltiplo 4? │
                    └─────┬──────────┬───┘
                          │ Sí       │ No
                    ┌─────▼────┐     │
                    │ spawnOle │     │
                    │ ada()    │     │
                    │ Estado→  │     │
                    │ ACTIVA   │     │
                    └──────────┘     │
                                     │
              ┌──────────────────────▼──┐
              │ Continuar ritual normal │
              └─────────────────────────┘
```

---

## 🎮 Experiencia de Juego

### **Progresión Dramática**
1. **Pulsos 1-5**: Enemigos básicos, efectos sutiles → aprendizaje
2. **Pulsos 6-10**: Más variedad, efectos aumentan → tensión creciente
3. **Pulsos 11-15**: Enemigos élite, efectos intensos → desafío real
4. **Pulsos 16-17**: Enemigos épicos, efectos dramáticos → momento cumbre
5. **Pulso 17**: Guardián + oleada épica simultánea → batalla final

### **Ritmo de Combate**
- **Sin oleada**: Jugadores añaden pulsos libremente
- **Con oleada**: Deben eliminar todos los enemigos para continuar
- **Gracia de 3 segundos**: Respiro entre oleadas para recuperación

### **Desafío Escalado**
- Oleada 1: 2 mobs normales (fácil)
- Oleada 2: 3 mobs 1.25x HP (medio)
- Oleada 3: 4 mobs 1.5x HP + élite (difícil)
- Oleada 4: 5 mobs 2x HP + Guardián (épico)
- Oleada 5: 7 mobs 2x HP + élite puro (extremo)

---

## 📝 Notas de Implementación

### ✅ **Completado**
- [x] Enum `OleadaState` con 3 estados
- [x] Variables de tracking (oleadaActual, enemigosOleada, intensidadRitual)
- [x] Método `tickOleadas()` con detección de completación
- [x] Método `spawnOleada()` con spawn en círculo
- [x] Método `seleccionarTipoEnemigo()` con 4 fases
- [x] Método `tickEfectosRitual()` con intensidad 0-100
- [x] Bloqueo en `onAltarInteractuado()` durante oleada activa
- [x] Integración con sistema de guardián existente
- [x] Mensajes y efectos de oleada (inicio, activa, completada)
- [x] Documentación en eventos.yml con todas las fases

### 🔧 **Configuración Recomendada**

**Para ritual estándar (15 minutos)**:
```yaml
pulsos_requeridos: 20        # 5 oleadas cada 4 pulsos
spawn_en_pulso: 15           # Guardián al 75% (o déjalo calcular automáticamente)
cooldown_ms: 0               # Sin cooldown
```

**Para ritual rápido (10 minutos)**:
```yaml
pulsos_requeridos: 10        # 5 oleadas cada 2 pulsos
# spawn_en_pulso se calcula automáticamente: 8 (75% de 10)
cooldown_ms: 0
```

**Para ritual épico (30 minutos)**:
```yaml
pulsos_requeridos: 50        # 5 oleadas cada 10 pulsos
# spawn_en_pulso se calcula automáticamente: 38 (75% de 50)
cooldown_ms: 0
```

### 🎯 **Balance Sugerido**
- **Fase Inicial**: Tutorial, enemigos débiles
- **Fase Media**: Transición, añadir variedad
- **Fase Avanzada**: Desafío real, enemigos fuertes
- **Fase Final**: Momento épico, todo al máximo

---

## 🚀 Resultado Final

El ritual ahora ofrece:
✅ **Oleadas dinámicas** calculadas automáticamente (cada 20% del total de pulsos)
✅ **Configuración flexible** - funciona con cualquier cantidad de pulsos (5, 8, 20, 50, etc.)
✅ **4 fases de intensidad** con enemigos y efectos escalados
✅ **Bloqueos estratégicos** que requieren eliminar oleadas
✅ **Efectos visuales progresivos** que aumentan con el ritual
✅ **Guardián adaptable** - spawn al 75% o configuración manual
✅ **Experiencia épica** con beam vertical y sonidos dramáticos en fase final
✅ **Ritmo de combate variado** entre momentos de construcción y defensa

**Escalabilidad**: El sistema se adapta automáticamente desde rituales cortos (8 pulsos) hasta épicos (50+ pulsos)

**Build Status**: ✅ SUCCESS (Apocalipsis-1.0.0.jar)
