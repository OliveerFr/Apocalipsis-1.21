# Mejoras Sistema de Desastres Naturales - Batalla del End
**Fecha**: 2024
**Archivo**: `AperturaEndEvent.java`

## Resumen de Cambios

Se ha reemplazado completamente el sistema simple de desastres naturales con un sistema avanzado inspirado en los desastres globales de "avo start".

---

## Sistema Anterior (Simple)

### Características Limitadas:
- **Duración fija**: 5-10 segundos
- **Sin fases**: Intensidad constante
- **Sin advertencias**: Desastres aparecían sin avisos
- **Efectos básicos**: 1-2 mecánicas por desastre
- **Sin interacción ambiental**: No transformaba el terreno

### Desastres Originales:
1. **Lluvia de Fuego**: 2-3 fireballs cada 10 ticks durante 5-10s
2. **Terremoto**: Vibración de jugadores durante 5s con partículas
3. **Huracán**: Empuje en espiral durante 6s

---

## Sistema MEJORADO (Avanzado)

### Mecánicas Globales Nuevas:

#### 1. **Sistema de Fases** (Todas los desastres)
Cada desastre tiene 3 fases dinámicas de intensidad:
- **INICIO (20%)**: Intensidad 0.8x - Advertencia visual
- **PICO (60%)**: Intensidad 1.4x - Efectos máximos
- **DECLIVE (20%)**: Intensidad 0.9x - Efectos residuales

#### 2. **Intensidad Escalada por Fase del Dragón**
La frecuencia e intensidad aumentan según el HP del dragón:
- **Fase 1 (100-75%)**: Cada 3-4 min, intensidad 1.0x
- **Fase 2 (75-50%)**: Cada 2-3 min, intensidad 1.3x
- **Fase 3 (50-25%)**: Cada 1.5-2.5 min, intensidad 1.6x
- **Fase 4 (25-0%)**: Cada 1-2 min, intensidad 2.0x (**EXTREMO**)

#### 3. **Variedad Progresiva**
Nuevos desastres se desbloquean según la fase:
- **Fase 1**: Lluvia de Fuego, Terremoto
- **Fase 2**: + Huracán del Vacío
- **Fase 3**: + Lluvia de Meteoritos
- **Fase 4**: + Colapso Dimensional (EXTREMO)

---

## Desastres Mejorados en Detalle

### 🔥 **LLUVIA DE FUEGO** (Mejorada)
**Duración**: 80 segundos (vs 5-10s anterior)

**Mecánicas Nuevas**:
1. **Meteoritos con Advertencia**:
   - Columna de partículas 3 segundos antes del impacto
   - Sonido de advertencia (BLOCK_BEACON_AMBIENT)
   - Explosión grande (2.5x intensidad)
   - Fuegos persistentes en área de impacto

2. **Calor Extremo** (Solo en PICO):
   - Daña a jugadores sin techo cada 2s
   - Requiere estrategia: construir refugios
   - Feedback: "El calor extremo te quema - ¡Busca refugio!"

3. **Transformación de Terreno** (Solo en PICO):
   - End Stone → Magma Blocks
   - Efectos de partículas de lava
   - Cambios permanentes hasta fin del desastre

4. **Fuegos Persistentes**:
   - Fuegos duran 30s después del desastre
   - Crean zonas peligrosas
   - Auto-limpieza después de 30s

**Diferencia visual**: Columnas de advertencia, lluvia constante + meteoritos grandes

---

### 🌍 **TERREMOTO** (Completamente Rediseñado)
**Duración**: 70 segundos (vs 5s anterior)

**Mecánicas Nuevas**:
1. **Grietas en el Suelo**:
   - Crea líneas de vacío (AIR blocks) en End Stone/Obsidian
   - Longitud escalada por intensidad (3-8 bloques)
   - Máximo 8 grietas activas
   - Se reparan automáticamente tras 20s

2. **Derrumbes de Bloques**:
   - FallingBlocks caen desde el cielo
   - End Stone y Obsidian como proyectiles
   - Causan daño al impactar jugadores
   - Cantidad escalada por intensidad

3. **Ondas Sísmicas Visuales**:
   - Anillos expansivos de partículas cada 15 ticks
   - Efecto visual de vibración del terreno
   - Radio de 8 bloques

4. **Desorientación** (PICO y DECLIVE):
   - Slowness (intensidad variable)
   - Mining Fatigue (intensidad variable)
   - Duración: 60 ticks (3s)

5. **Réplicas** (Solo en DECLIVE):
   - Cada 15 segundos
   - Intensidad 1.5x de la réplica
   - Advertencia: "§6§l⚠ §eRÉPLICA SÍSMICA"

6. **Sonidos Sísmicos**:
   - ENTITY_GENERIC_EXPLODE (pitch 0.5f)
   - ENTITY_WARDEN_DIG (pitch 0.7f)

**Versión EXTREMA** (Fase 4):
- Intensidad x2
- Efecto Nausea durante 10s
- Mensaje: "§4§l⚠ TERREMOTO DEVASTADOR ⚠"

---

### 🌪️ **HURACÁN DEL VACÍO** (Completamente Rediseñado)
**Duración**: 75 segundos (vs 6s anterior)

**Mecánicas Nuevas**:
1. **Vórtices Dimensionales**:
   - Crea centros de vórtice para cada jugador
   - Jugadores son aspirados en espiral hacia los vórtices
   - Partículas PORTAL en espiral visual

2. **Rachas de Viento Dinámicas**:
   - Empuje en espiral + atracción al vórtice
   - Fuerza escalada por intensidad
   - Oscilación vertical (efecto de olas)

3. **Objetos Voladores** (Solo en PICO):
   - Lanza bloques del End (End Stone, Obsidian, End Stone Bricks)
   - Bloques caen con física real (FallingBlock)
   - Causan daño escalado por intensidad
   - Máximo 15 objetos voladores activos

4. **Visibilidad Reducida** (Solo en PICO):
   - Blindness cada 60 ticks
   - Duración: 80 ticks (4s)
   - Feedback: "[El viento te ciega...]"

5. **Efectos Visuales Mejorados**:
   - Partículas CLOUD, SOUL, END_ROD
   - Vórtice visual con partículas PORTAL en espiral
   - Radio visual de 5 bloques

6. **Sonidos de Viento**:
   - ITEM_ELYTRA_FLYING (volumen 1.2f)
   - ENTITY_PHANTOM_FLAP (volumen 0.8f)

**Diferencia clave**: De simple empuje a sistema complejo de vórtices con objetos voladores

---

### ☄️ **LLUVIA DE METEORITOS** (NUEVO - Fase 3+)
**Duración**: 60 segundos

**Mecánicas**:
1. **Meteoritos Múltiples**:
   - Lanza 1 + intensidad meteoritos cada 20 ticks
   - Sistema de advertencia igual que Lluvia de Fuego
   - Explosiones grandes (2.5x × 1.2 intensidad)

2. **Lluvia Constante Combinada**:
   - 4 fireballs pequeños cada 15 ticks
   - Yield: 1.5 × intensidad

3. **Fuegos Persistentes**:
   - Limpieza después de 40s (vs 30s normal)

**Advertencia**: "§c§l⚠ ALERTA: Múltiples meteoritos detectados ⚠"

---

### 🌌 **COLAPSO DIMENSIONAL** (NUEVO - Solo Fase 4)
**Duración**: 50 segundos de **CAOS TOTAL**

**Mecánicas Combinadas** (Todos los desastres anteriores activos):
1. **Meteoritos**: Cada 25 ticks, intensidad 1.5x
2. **Grietas Dimensionales**: Cada 40 ticks, intensidad 1.3x (máx 12)
3. **Vientos Caóticos**: Cada 3 ticks, empuje aleatorio total
4. **Temblores**: Cada 5 ticks, vibración constante
5. **Efectos Negativos** (cada 80 ticks):
   - Wither II (60 ticks)
   - Nausea (100 ticks)
   - Slowness II (60 ticks)

6. **Daño Constante Alto**:
   - 4.0 × intensidad cada 30 ticks
   - Mensaje: "§4§l⚠ La realidad se desgarra a tu alrededor"

7. **Efectos Visuales Apocalípticos**:
   - PORTAL (30 partículas)
   - FLAME (15 partículas)
   - END_ROD (10 partículas)
   - Todo cada 5 ticks

8. **Sonidos Apocalípticos** (cada 40 ticks):
   - ENTITY_GENERIC_EXPLODE
   - ENTITY_WARDEN_ROAR
   - ENTITY_ENDER_DRAGON_GROWL

**Anuncio Épico**:
```
§4§l━━━━━━━━━━━━━━━━━━━━━━━━━━━
§4§l      ⚠ COLAPSO DIMENSIONAL ⚠
§c  El dragón rasga el tejido del End
§4§l━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

**Título**: "§4§l⚠ COLAPSO" / "§c§lDIMENSIONAL"

---

## Mejoras en Anuncios

### Barra de Intensidad Visual
```
§7Intensidad: §a████§7██████ (Moderado)
§7Intensidad: §6██████§7████ (Alto)
§7Intensidad: §c██████████ (Extremo)
```

### Mensajes Mejorados
Antes:
```
§6§lLLUVIA DE FUEGO
§7El dragón invoca las fuerzas del End...
```

Ahora:
```
§6§l⚠ LLUVIA DE FUEGO ⚠

§7Intensidad: §e██████§7████
§8[§7...§8] §7El dragón desata su furia...
```

### Sonidos Épicos Mejorados
- **Intensidad >= 1.6** (Fase 3-4):
  - ENTITY_WITHER_SPAWN (vol 1.0f, pitch 0.6f)
  - ENTITY_ENDER_DRAGON_GROWL (vol 1.5f, pitch 0.5f)
  - ENTITY_WARDEN_SONIC_BOOM (vol 0.8f, pitch 0.7f)

- **Intensidad < 1.6** (Fase 1-2):
  - ENTITY_WITHER_SPAWN (vol 0.6f, pitch 0.8f)
  - ENTITY_ENDER_DRAGON_GROWL (vol 1.0f, pitch 0.6f)

---

## Métodos Auxiliares Nuevos

### 1. `generarBarraIntensidad(double intensidad)`
Genera representación visual de intensidad (10 barras):
- §e (Amarillo): < 5 barras
- §6 (Naranja): 5-7 barras
- §c (Rojo): >= 8 barras

### 2. `lanzarMeteoritoConAdvertencia(...)` 
- Crea columna de advertencia (60 ticks)
- Lanza meteorito real después
- Gestiona fuegos persistentes

### 3. `lanzarMeteoritoReal(...)`
- Spawn Fireball grande
- Explosión 2.5x intensidad
- 8 fuegos persistentes en área

### 4. `crearGrieta(...)`
- Genera línea de grietas (3-8 bloques)
- Dirección aleatoria
- Partículas de ruptura

### 5. `crearDerrumbe(...)`
- Spawn 2-intensidad FallingBlocks
- Materiales: End Stone / Obsidian
- Daño escalado: 1.0f/block

### 6. `lanzarReplica(...)`
- Terremoto breve (40 ticks)
- Intensidad 1.5x
- Advertencia visual

### 7. `ejecutarTerremotoExtremo(...)`
- Wrapper para Fase 4
- Intensidad x2
- Nausea adicional

### 8. `lanzarObjetoVolador(...)`
- Spawn FallingBlock
- Velocidad dirigida al jugador
- Daño: 1.5 × intensidad

### 9. `ejecutarLluviaMeteorito(...)`
- Combinación de meteoritos + lluvia
- Frecuencia alta
- Fuegos duran 40s

### 10. `ejecutarColapsoDimensional(...)`
- Combina todos los desastres
- Caos total durante 50s
- Efectos múltiples simultáneos

---

## Colores por Tipo de Desastre

| Tipo | Color | Hex |
|------|-------|-----|
| Lluvia de Fuego | §6 | Naranja |
| Lluvia Meteoritos | §6 | Naranja |
| Terremoto | §c | Rojo |
| Terremoto Extremo | §c | Rojo |
| Huracán | §b | Cyan |
| Tormenta Dimensional | §b | Cyan |
| Colapso Dimensional | §4 | Rojo Oscuro |

---

## Importaciones Necesarias Agregadas

```java
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
```

---

## Comparativa de Complejidad

| Aspecto | Sistema Anterior | Sistema Mejorado |
|---------|------------------|------------------|
| **Líneas de código** | ~118 líneas (3 métodos) | **~1200 líneas** (10+ métodos) |
| **Tipos de desastres** | 3 básicos | **8 variantes** (3 mejorados + 5 nuevos) |
| **Duración promedio** | 5-10 segundos | **60-80 segundos** con fases |
| **Mecánicas por desastre** | 1-2 efectos | **5-8 mecánicas** complejas |
| **Interacción ambiental** | Ninguna | **Grietas, transformaciones, fuegos** |
| **Sistema de fases** | No | **Sí**: INICIO → PICO → DECLIVE |
| **Advertencias visuales** | No | **Sí**: Columnas, partículas, sonidos |
| **Escalado dinámico** | No | **Sí**: Por fase del dragón |
| **Frecuencia** | 1-3 min fija | **1-4 min** escalada por fase |

---

## Impacto en Gameplay

### Antes:
- Desastres eran "molestias" breves
- Poca estrategia requerida
- Efectos predecibles y simples

### Ahora:
- Desastres son **amenazas épicas**
- **Estrategia crítica**: Buscar refugio, evitar grietas, esquivar meteoritos
- Efectos variables según fase del dragón
- **Tensión creciente**: A menor HP del dragón, más caos
- Fase 4 es **supervivencia extrema** con Colapso Dimensional

---

## Ejemplo de Progresión en Batalla

### Fase 1 (100-75% HP):
- Desastre cada 3-4 minutos
- Lluvia de Fuego leve, Terremoto moderado
- Intensidad 1.0x - Manageable

### Fase 2 (75-50% HP):
- Desastre cada 2-3 minutos
- + Huracán del Vacío
- Intensidad 1.3x - Challenging

### Fase 3 (50-25% HP):
- Desastre cada 1.5-2.5 minutos
- + Lluvia de Meteoritos
- Intensidad 1.6x - Difícil
- Meteoritos grandes con advertencia

### Fase 4 (25-0% HP):
- Desastre cada 1-2 minutos
- + Colapso Dimensional
- Intensidad 2.0x - **EXTREMO**
- Caos total: Todos los efectos simultáneos

---

## Notas Técnicas

### Limpieza Automática:
- Fuegos persistentes: Auto-limpieza tras 30-40s
- Grietas: Auto-reparación tras 20s
- FallingBlocks: Timeout automático de Bukkit

### Rendimiento:
- Uso de BukkitRunnable para tareas asíncronas
- Límites en objetos activos:
  * Máx 15 objetos voladores
  * Máx 8 grietas
  * Máx 12 grietas (Colapso Dimensional)
- Partículas optimizadas con cantidades controladas

### Compatibilidad:
- **Versión Minecraft**: 1.21+
- **API Bukkit/Spigot**: Paper recomendado
- **No requiere** plugins adicionales
- Compatible con el sistema de fases del dragón existente

---

## Futuras Mejoras Posibles (Post-Implementación)

1. **Configuración en YAML**:
   - Intensidades ajustables
   - Duraciones personalizables
   - Habilitar/deshabilitar desastres específicos

2. **Más Variedad**:
   - Tormenta de Ender Pearls
   - Invasión de Shulkers
   - Lluvia de Chorus Fruit

3. **Integración con Estadísticas**:
   - Contar desastres sobrevividos
   - Logros por sobrevivir Colapso Dimensional

4. **Efectos Sonoros Custom**:
   - Música épica durante Colapso Dimensional
   - Sonidos ambient específicos por desastre

---

## Conclusión

El sistema de desastres naturales ha evolucionado de **simple decoración** a **mecánica central** de la batalla contra el Dragón del End. La intensidad escalada por fases crea una experiencia narrativa donde la batalla se vuelve progresivamente más desesperada, culminando en el apocalíptico **Colapso Dimensional** que pone a prueba todas las habilidades de los jugadores.

**Estado**: ✅ Implementado - Pendiente compilación final
**Archivo**: `AperturaEndEvent.java` (líneas 11580-12730)
**Líneas añadidas**: ~1200 líneas de código nuevo
