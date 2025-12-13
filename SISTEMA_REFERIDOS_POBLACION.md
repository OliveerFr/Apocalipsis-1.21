# 🎁 Sistema de Recompensas para el Streamer

## Versión 2.0 - 13 de Diciembre 2025

Este sistema proporciona **recompensas acumulables** para el streamer cuando alcanza hitos del servidor como traer jugadores nuevos, que suban de rango, batir récords de población, etc. Todas las recompensas se guardan en `/recompensas` para reclamarlas cuando quieras.

---

## 📋 ÍNDICE
1. [Cómo Funciona](#cómo-funciona)
2. [Tipos de Recompensas](#tipos-de-recompensas)
3. [Sistema de Acumulación](#sistema-de-acumulación)
4. [Configuración](#configuración)
5. [Implementación](#implementación)

---

## 🎮 CÓMO FUNCIONA

### Concepto General
Como streamer, recibes recompensas automáticamente cuando logras ciertos hitos:
- ✅ **Jugador nuevo** se une al servidor
- ✅ **Jugadores suben de rango** (Explorador, Sobreviviente, etc.)
- ✅ **Récords de población** (más jugadores online que nunca)
- ✅ **Población sostenida** (mantener X jugadores durante Y tiempo)
- ✅ **Hitos acumulativos** (total de 5, 10, 25, 50, 100 jugadores nuevos)
- ✅ **Logros especiales** (primer Absoluto, streams largos, retención)

### Flujo de Recompensas
```
Evento ocurre → Recompensa se crea → Guardada en /recompensas → Reclamas cuando quieras
```

**Ejemplo:**
1. Un jugador nuevo "Player123" se une → Recompensa guardada
2. Ves notificación: "§6[RECOMPENSA] ¡Tienes una nueva recompensa en /recompensas!"
3. Cuando quieras, haces `/recompensa` y la reclamas
4. Recibes: +300 XP, +3 Diamantes, +2 Manzanas Doradas

---

## 🏆 TIPOS DE RECOMPENSAS

### 1. 👤 Jugadores Nuevos

#### Primer Join (Se une por primera vez)
**Recompensas:**
- ✅ +300 XP
- ✅ +3 Diamantes  
- ✅ +2 Manzanas Doradas
- 📦 Acumulable en `/recompensas`

#### Primera Hora Jugada
**Recompensas:**
- ✅ +200 XP
- ✅ +2 Diamantes
- ✅ +5 Experience Bottles
- 📦 Acumulable en `/recompensas`

---

### 2. 📈 Jugadores Suben de Rango

Cada vez que un jugador alcanza un nuevo rango, recibes recompensas:

| Rango | XP | Recompensas |
|-------|-----|-------------|
| **EXPLORADOR** | +500 | +5 Diamantes, +3 Esmeraldas |
| **SOBREVIVIENTE** | +800 | +8 Diamantes, +1 Netherite Scrap, +5 Bloques PS |
| **VETERANO** | +1200 | +1 Netherite Ingot, +3 Manzanas Enc., +10 Bloques PS |
| **LEYENDA** | +2000 | +2 Netherite Ingots, +1 Estrella, +15 Bloques PS |
| **MAESTRO** | +3000 | +3 Netherite Ingots, +2 Estrellas, +1 Élitro |
| **TITÁN** | +5000 | +5 Netherite Ingots, +3 Estrellas, +5 Manzanas Enc. |
| **ABSOLUTO** | +10000 | +1 Bloque Netherite, +5 Estrellas, +1 Dragon Egg |

> **Ejemplo:** Si 3 jugadores alcanzan Veterano en una semana:
> - 3 recompensas en `/recompensas`
> - Total: 3,600 XP + 3 Netherite + 9 Manzanas + 30 Bloques PS

---

### 3. 🌍 Récords de Población

#### Nuevo Récord Histórico
Cada vez que superas el máximo histórico de jugadores online:

**Recompensas Base:**
- ✅ +100 XP por cada jugador
- ✅ +10 Diamantes
- ✅ +1 Estrella del Nether

**Recompensas Adicionales por Cantidad:**
- **10+ jugadores:** +1 Netherite Ingot
- **15+ jugadores:** +2 Netherite Ingots, +3 Manzanas Enc.
- **20+ jugadores:** +1 Bloque Netherite, +2 Estrellas
- **25+ jugadores:** +2 Bloques Netherite, +1 Dragon Head

**Ejemplo con 17 jugadores online (nuevo récord):**
```
Base: 1,700 XP (17 × 100) + 10 Diamantes + 1 Estrella
15+: +2 Netherite Ingots + 3 Manzanas Encantadas
Total: 1,700 XP + 10 Diamantes + 2 Netherite + 3 Manzanas + 1 Estrella
```

---

### 4. ⏱️ Población Sostenida

Mantener alta población durante tiempo:

| Hito | Recompensas |
|------|-------------|
| **10+ jugadores × 1 hora** | +1000 XP, +15 Diamantes, +2 Netherite Scraps |
| **15+ jugadores × 1 hora** | +2000 XP, +2 Netherite Ingots, +1 Estrella |
| **20+ jugadores × 1 hora** | +5000 XP, +1 Bloque Netherite, +3 Estrellas, +10 Manzanas Enc. |

---

### 5. 📊 Hitos Acumulativos

#### Total de Jugadores Nuevos

| Hito | Recompensas |
|------|-------------|
| **5 nuevos** | +1000 XP, +20 Diamantes, +3 Netherite Scraps |
| **10 nuevos** | +2500 XP, +2 Netherite Ingots, +1 Estrella |
| **25 nuevos** | +5000 XP, +5 Netherite Ingots, +3 Estrellas, +1 Élitro |
| **50 nuevos** | +10000 XP, +2 Bloques Netherite, +5 Estrellas, +1 Dragon Egg |
| **100 nuevos** | +25000 XP, +5 Bloques Netherite, +10 Estrellas, +1 Beacon |

#### Jugadores por Rango

| Hito | Recompensas |
|------|-------------|
| **5 Exploradores** | +800 XP, +10 Diamantes |
| **10 Sobrevivientes** | +2000 XP, +2 Netherite Ingots |
| **5 Veteranos** | +3000 XP, +3 Netherite Ingots, +1 Estrella |
| **5 Leyendas** | +5000 XP, +1 Bloque Netherite, +2 Estrellas |

---

### 6. 🎖️ Logros Especiales

#### Primer Absoluto
El primer jugador que alcanza ABSOLUTO:
- ✅ +15000 XP
- ✅ +3 Bloques Netherite
- ✅ +10 Estrellas
- ✅ +1 Beacon
- ⚠️ **Único:** Solo se da una vez

#### Streams Largos

| Duración | Recompensas |
|----------|-------------|
| **6 horas** | +2000 XP, +20 Diamantes |
| **12 horas** | +5000 XP, +3 Netherite Ingots, +2 Estrellas |
| **24 horas** | +20000 XP, +2 Bloques Netherite, +5 Estrellas, +1 Dragon Head |

#### Buena Retención
Jugadores que vuelven después de 7+ días:

| Hito | Recompensas |
|------|-------------|
| **5 jugadores** | +1500 XP, +15 Diamantes |
| **10 jugadores** | +3000 XP, +2 Netherite Ingots, +1 Estrella |

---

## 💾 SISTEMA DE ACUMULACIÓN

### Cómo se Guardan las Recompensas

1. **Evento ocurre** → Sistema detecta (ej: jugador alcanza Veterano)
2. **Recompensa se crea** → Se agrega a tu lista de recompensas pendientes
3. **Notificación** → Recibes mensaje + título en pantalla:
   ```
   [RECOMPENSA] ¡Tienes una nueva recompensa en /recompensas!
   ⭐ NUEVA RECOMPENSA
   Revisa /recompensas
   ```
4. **Acumulación** → La recompensa espera hasta que la reclames
5. **Reclamo** → Abres `/recompensas` y la reclamas cuando quieras

### Características

✅ **Persistente:** Se guardan en base de datos, no se pierden
✅ **Infinito:** Puedes acumular hasta 100 recompensas simultáneas
✅ **Sin expiración:** Nunca caducan (configurable a 0 días)
✅ **Acumulables:** Múltiples recompensas del mismo tipo se suman

### Ejemplo de Uso

**Durante tu stream de 4 horas:**
```
19:00 - Jugador "Alex" se une (primer join)
        → +300 XP, +3 Diamantes, +2 Manzanas guardadas

19:45 - "Alex" completa primera hora
        → +200 XP, +2 Diamantes, +5 XP Bottles guardadas

20:30 - "Maria" se une (primer join)
        → +300 XP, +3 Diamantes, +2 Manzanas guardadas

21:15 - "Alex" alcanza Explorador
        → +500 XP, +5 Diamantes, +3 Esmeraldas guardadas

22:00 - Población: 12 jugadores (nuevo récord)
        → +1200 XP, +10 Diamantes, +1 Estrella + extras guardados

23:00 - Terminas stream, abres /recompensas
        → Reclamas TODAS las 5 recompensas de una vez
        → Total: 2,500 XP + 23 Diamantes + montón de items
```

---

## ⚙️ CONFIGURACIÓN

### Archivo: `recompensas.yml`

```yaml
xp_dinamico:
  recompensas_streamer:
    enabled: true
    streamer_username: "Riolu"    # Tu username
    
    # Notificaciones
    configuracion:
      notificar_recompensa_disponible: true
      mensaje_notificacion: "&6&l[RECOMPENSA] &e¡Tienes una nueva recompensa!"
      
      titulo_notificacion:
        enabled: true
        titulo: "&6&l⭐ NUEVA RECOMPENSA"
        subtitulo: "&eRevisa /recompensas"
      
      sonido:
        enabled: true
        tipo: "ENTITY_PLAYER_LEVELUP"
      
      # Persistencia
      persistente: true
      dias_expiracion: 0           # 0 = nunca expiran
      limite_recompensas: 100      # Máximo 100 pendientes
```

### Ajustes Recomendados

**Para servidor pequeño (pocos jugadores):**
```yaml
jugador_nuevo.primer_join.xp: 500        # Más generoso
records_poblacion.nuevo_record.xp_por_jugador: 150
```

**Para servidor grande (muchos jugadores):**
```yaml
jugador_nuevo.primer_join.xp: 150        # Menos generoso
records_poblacion.nuevo_record.xp_por_jugador: 50
```

---

## 💻 IMPLEMENTACIÓN

### Estado Actual: ✅ Configuración Lista / ⚠️ Código Pendiente

#### ✅ Completado
- [x] Configuración completa en `recompensas.yml`
- [x] Todas las categorías de recompensas definidas
- [x] Sistema de notificaciones configurado
- [x] Persistencia y acumulación configurada

#### ⚠️ Pendiente de Implementar

##### 1. Clase StreamerRewardSystem
**Archivo a crear:** `StreamerRewardSystem.java`

**Funcionalidades:**
- Detectar eventos del servidor
- Crear recompensas para el streamer
- Guardar en sistema de recompensas pendientes
- Enviar notificaciones
- Trackear hitos acumulativos
- Gestionar récords históricos

**Métodos principales:**
```java
public class StreamerRewardSystem {
    // Obtener UUID del streamer
    private UUID getStreamerUUID();
    
    // Agregar recompensa al streamer
    public void addStreamerReward(String titulo, String descripcion, 
                                  int xp, List<String> comandos);
    
    // Eventos de jugadores nuevos
    public void onPlayerFirstJoin(Player player);
    public void onPlayerFirstHour(Player player);
    
    // Eventos de rangos
    public void onPlayerRankUp(Player player, MissionRank newRank);
    
    // Eventos de población
    public void onNewPopulationRecord(int playerCount);
    public void onSustainedPopulation(int players, int minutes);
    
    // Hitos acumulativos
    public void checkNewPlayerMilestones();
    public void checkRankMilestones();
    
    // Logros especiales
    public void onFirstAbsoluto(Player player);
    public void onStreamDuration(int hours);
    public void onRetentionMilestone(int returnedPlayers);
}
```

##### 2. Integración con Eventos Existentes

**PlayerJoinEvent:**
```java
@EventHandler
public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    
    if (!player.hasPlayedBefore()) {
        // Jugador nuevo - dar recompensa al streamer
        streamerRewardSystem.onPlayerFirstJoin(player);
    }
}
```

**RankUpEvent (o similar):**
```java
public void onRankUp(Player player, MissionRank oldRank, MissionRank newRank) {
    // Jugador subió de rango - recompensa al streamer
    streamerRewardSystem.onPlayerRankUp(player, newRank);
}
```

##### 3. Sistema de Tracking de Récords

**Archivo a crear:** `PopulationRecordTracker.java`

```java
public class PopulationRecordTracker {
    private int historicRecord = 0;
    
    // Verificar y actualizar récord cada minuto
    public void checkPopulationRecord() {
        int current = Bukkit.getOnlinePlayers().size();
        
        if (current > historicRecord) {
            historicRecord = current;
            streamerRewardSystem.onNewPopulationRecord(current);
        }
    }
    
    // Verificar población sostenida
    public void checkSustainedPopulation() {
        // Track durante 1 hora, verificar thresholds
    }
}
```

##### 4. Base de Datos

**Tabla necesaria:** `apocalipsis_streamer_stats`

```sql
CREATE TABLE apocalipsis_streamer_stats (
    id INT AUTO_INCREMENT PRIMARY KEY,
    stat_type VARCHAR(64) NOT NULL,    -- 'total_new_players', 'historic_record', etc.
    stat_value INT NOT NULL,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_stat (stat_type)
);

-- Ejemplos de registros:
-- stat_type='total_new_players', stat_value=47
-- stat_type='historic_population_record', stat_value=18
-- stat_type='total_exploradores', stat_value=12
```

---

## 📊 BALANCE Y ECONOMÍA

### Proyección de Recompensas

**Servidor con 10 jugadores nuevos al mes:**

```
Mes 1:
- 10 primeros joins: 3,000 XP + 30 Diamantes
- 10 primeras horas: 2,000 XP + 20 Diamantes  
- 5 alcanzan Explorador: 2,500 XP + 25 Diamantes
- 2 alcanzan Sobreviviente: 1,600 XP + 16 Diamantes + 2 Netherite Scraps
- Hito 10 nuevos: 2,500 XP + 2 Netherite Ingots + 1 Estrella

Total Mes 1: ~11,600 XP + 91 Diamantes + ~3 Netherite + extras
```

**Servidor con récord de 15 jugadores:**
```
Nuevo récord alcanzado:
- Base: 1,500 XP (15 × 100)
- 10+ bonus: +1 Netherite Ingot
- 15+ bonus: +2 Netherite Ingots + 3 Manzanas Enc.
- Total: 1,500 XP + 10 Diamantes + 3 Netherite + 3 Manzanas + 1 Estrella

Si lo mantienes 1 hora:
- +2,000 XP + 2 Netherite Ingots + 1 Estrella adicional
```

### Valor Total por Hitos Grandes

| Hito | Recompensa Total (aproximada) |
|------|-------------------------------|
| **25 jugadores nuevos** | ~30,000 XP + ~200 Diamantes + ~20 Netherite + varios uniques |
| **50 jugadores nuevos** | ~80,000 XP + ~500 Diamantes + ~50 Netherite + Dragon Egg |
| **100 jugadores nuevos** | ~200,000 XP + ~1500 Diamantes + ~150 Netherite + Beacon |

---

## 🎯 ESTRATEGIAS

### Para Maximizar Recompensas

1. **Promociona el servidor** en tus streams/videos
2. **Ayuda a nuevos jugadores** a completar sus primeras horas
3. **Organiza eventos** para mantener población alta
4. **Streams largos** en horarios donde más gente puede jugar
5. **Fomenta retención** con contenido regular

### Hitos Prioritarios

**Corto plazo (1 mes):**
- ✅ 5 jugadores nuevos → +1000 XP
- ✅ 5 Exploradores → +800 XP
- ✅ Récord de 10+ jugadores → +1000+ XP

**Medio plazo (3 meses):**
- ✅ 25 jugadores nuevos → +5000 XP + Élitro
- ✅ 10 Sobrevivientes → +2000 XP
- ✅ Récord de 15+ jugadores → +1500+ XP

**Largo plazo (6+ meses):**
- ✅ 100 jugadores nuevos → +25000 XP + Beacon
- ✅ 5 Leyendas → +5000 XP + Bloque Netherite
- ✅ Récord de 20+ jugadores → +2000+ XP

---

## 📝 NOTAS FINALES

### Diferencias con Sistema Anterior

❌ **ANTES:** Referidos para todos los jugadores (recompensas distribuidas)
✅ **AHORA:** Recompensas solo para el streamer (tú)

❌ **ANTES:** Se daban instantáneamente
✅ **AHORA:** Se acumulan en `/recompensas` para reclamar cuando quieras

❌ **ANTES:** Bonus de XP por población
✅ **AHORA:** Población da bonus a todos + recompensas especiales para streamer

### Ventajas del Nuevo Sistema

1. ✅ **Flexibilidad:** Reclamas cuando quieres, no interrumpen tu stream
2. ✅ **Visibilidad:** Ves todas tus recompensas acumuladas en un menú
3. ✅ **Motivación:** Sistema de logros claro con hitos grandes
4. ✅ **Balance:** No afecta economía de otros jugadores
5. ✅ **Escalable:** Recompensas crecen con el éxito del servidor

### Prioridades de Implementación

1. **ALTA:** StreamerRewardSystem básico
2. **ALTA:** Eventos de jugadores nuevos
3. **ALTA:** Eventos de rank-up
4. **MEDIA:** Sistema de récords
5. **MEDIA:** Población sostenida
6. **BAJA:** Logros especiales avanzados

---

**Última actualización:** 13 de Diciembre 2025  
**Versión:** 2.0  
**Estado:** ✅ Configuración completa | ⚠️ Código pendiente
**Tipo:** Sistema de recompensas para streamer (acumulables en `/recompensas`)

---

## 📋 ÍNDICE
1. [Sistema de Referidos](#sistema-de-referidos)
2. [Bonificación por Población](#bonificación-por-población)
3. [Configuración](#configuración)
4. [Implementación](#implementación)

---

## 👥 SISTEMA DE REFERIDOS

### Objetivo
Incentivar a los jugadores a traer amigos al servidor mediante recompensas inmediatas y progresivas.

### ¿Cómo funciona?

#### 1. Invitación Inmediata
Cuando un jugador nuevo se une usando tu código de referido:

**Recompensas instantáneas:**
- ✅ **+500 XP** (inmediato)
- ✅ **+5 Diamantes**
- ✅ **+3 Manzanas Doradas**
- ✅ **+10 Experience Bottles**
- 🎬 Título épico: "¡NUEVO JUGADOR!"
- 💬 Mensaje en chat con el nombre del referido

**Comando:** `/invitar <nombre_jugador>`

---

#### 2. Recompensas Progresivas (Hitos del Referido)

A medida que tu referido sube de nivel, TÚ recibes recompensas adicionales:

| Hito | Recompensas | Detalles |
|------|-------------|----------|
| **EXPLORADOR** (Nivel 2) | +300 XP, +3 Diamantes, +5 Perlas | Cuando alcanza rango 2 |
| **SOBREVIVIENTE** (Nivel 3) | +500 XP, +5 Diamantes, +1 Netherite Scrap, **+5 Bloques PS** | Cuando alcanza rango 3 |
| **VETERANO** (Nivel 4) | +800 XP, +1 Netherite Ingot, +2 Manzanas Enc., **+8 Bloques PS** | Cuando alcanza rango 4 |
| **LEYENDA** (Nivel 5+) | +1500 XP, +2 Netherite Ingots, +1 Estrella, **+15 Bloques PS** | Cuando alcanza rango 5 |

> ⚠️ **Importante:** Las recompensas se dan automáticamente cuando el referido alcanza cada hito.

---

#### 3. Bonus Acumulativo Permanente

Tener varios referidos **activos** te da multiplicadores de XP PERMANENTES:

| Referidos Activos | Bonus XP | Recompensa Extra |
|-------------------|----------|------------------|
| **3+ referidos** | +5% XP permanente | - |
| **5+ referidos** | +10% XP permanente | - |
| **10+ referidos** | +15% XP permanente | **+1 Élitro** |

**¿Qué es un "referido activo"?**
- Un jugador que ha jugado en las últimas **7 días** (168 horas)
- Si un referido deja de jugar por más de 7 días, pierde su estado "activo"
- Si vuelve a jugar, recupera su estado activo automáticamente

---

### Ejemplo de Progresión

**Día 1:** Invitas a "MiAmigo123"
```
✓ Inmediato: +500 XP, +5 Diamantes, +3 Manzanas, +10 XP Bottles
```

**Día 3:** MiAmigo123 alcanza Explorador
```
✓ +300 XP, +3 Diamantes, +5 Perlas de Ender
```

**Día 7:** MiAmigo123 alcanza Sobreviviente
```
✓ +500 XP, +5 Diamantes, +1 Netherite Scrap, +5 Bloques de Protección
```

**Día 10:** Invitas a "Jugador456" y "Player789"
```
✓ Ahora tienes 3 referidos activos → +5% XP PERMANENTE
✓ Este bonus se aplica a TODO el XP que ganes
```

**Día 30:** Tus referidos siguen activos y subiendo de nivel
```
✓ Continúas recibiendo recompensas cada vez que suben de rango
✓ Tu multiplicador de +5% XP sigue activo mientras jueguen
```

---

## 🌍 BONIFICACIÓN POR POBLACIÓN

### Objetivo
Recompensar a todos los jugadores cuando el servidor está más activo de lo normal.

### Población Base
**Configuración actual:** 5 jugadores (promedio normal)

### Niveles de Bonificación

#### 🟢 Nivel Moderado (6-8 jugadores)
- **Multiplicador:** x1.10 (+10% XP)
- **Icono en menú:** Lingote de Oro
- **ActionBar:** "⚡ Servidor Activo: +10% XP"

#### 🟡 Nivel Alto (9-12 jugadores)
- **Multiplicador:** x1.20 (+20% XP)
- **Icono en menú:** Diamante
- **ActionBar:** "⚡ Servidor Muy Activo: +20% XP"

#### 🟠 Nivel Muy Alto (13-16 jugadores)
- **Multiplicador:** x1.30 (+30% XP)
- **Icono en menú:** Diamante
- **ActionBar:** "⚡ ¡Servidor Épico!: +30% XP"

#### 🔴 RÉCORD (17+ jugadores)
- **Multiplicador:** x1.50 (+50% XP)
- **Bonus horario:** +200 XP, +3 Diamantes, +2 Manzanas cada hora
- **Icono en menú:** Estrella del Nether
- **ActionBar:** "⚡ ¡RÉCORD DEL SERVIDOR!: +50% XP"
- **Efectos:** Fuegos artificiales + Partículas + Sonidos

### Notificaciones

**Cuando sube el nivel de población:**
```
[POBLACIÓN] 9+ jugadores online! +20% XP
```

**Cuando alcanzas RÉCORD:**
```
[POBLACIÓN] 17+ jugadores! +50% XP + Bonus horario
⚡ ¡RÉCORD DEL SERVIDOR!: +50% XP
```

**Bonus Horario (solo en RÉCORD):**
```
⭐ HORA COMPLETA EN RÉCORD! +200 XP, +3 Diamantes
```

### Visualización en el Menú

El slot **40: Referidos y Población** muestra:
- **Jugadores online actuales**
- **Tus referidos activos**
- **Bonus de población activo**
- **Cambio de icono según población:**
  - 🔶 Hierro: Sin bonus (1-5 jugadores)
  - 🟡 Oro: Moderado (6-8 jugadores)
  - 💎 Diamante: Alto/Muy Alto (9-16 jugadores)
  - ⭐ Estrella: RÉCORD (17+ jugadores)

---

## ⚙️ CONFIGURACIÓN

### Archivo: `recompensas.yml`

#### Sistema de Referidos
```yaml
xp_dinamico:
  sistema_referidos:
    enabled: true
    
    # Recompensa inmediata
    recompensa_invitacion:
      xp: 500
      comandos:
        - "give %player% minecraft:diamond 5"
        - "give %player% minecraft:golden_apple 3"
        - "give %player% minecraft:experience_bottle 10"
      mensaje: "&6&l[REFERIDO] &e%jugador_nuevo% &ase unió por ti! &6+500 XP"
    
    # Hitos del referido
    hitos_referido:
      nivel_2:
        xp: 300
        comandos: [...]
      nivel_3:
        xp: 500
        comandos: [...]
      nivel_4:
        xp: 800
        comandos: [...]
      nivel_5:
        xp: 1500
        comandos: [...]
    
    # Bonus acumulativo
    bonus_acumulativo:
      enabled: true
      3_referidos:
        multiplicador_xp: 1.05    # +5%
      5_referidos:
        multiplicador_xp: 1.10    # +10%
      10_referidos:
        multiplicador_xp: 1.15    # +15%
        comandos:
          - "give %player% minecraft:elytra 1"
    
    # Definición de "activo"
    tiempo_actividad_horas: 168  # 7 días
```

#### Bonificación por Población
```yaml
xp_dinamico:
  bonificacion_poblacion:
    enabled: true
    poblacion_base: 5        # Ajusta según tu servidor
    
    niveles:
      moderado:
        jugadores_minimos: 6
        multiplicador: 1.10  # +10%
      alto:
        jugadores_minimos: 9
        multiplicador: 1.20  # +20%
      muy_alto:
        jugadores_minimos: 13
        multiplicador: 1.30  # +30%
      record:
        jugadores_minimos: 17
        multiplicador: 1.50  # +50%
        recompensas_horarias:
          enabled: true
          xp: 200
          comandos:
            - "give %player% minecraft:diamond 3"
            - "give %player% minecraft:golden_apple 2"
    
    notificar_cambios: true
    notificar_actionbar: true
    intervalo_notificacion_seg: 300  # Cada 5 minutos
```

### Ajustes Recomendados

**Para servidor pequeño (1-10 jugadores promedio):**
```yaml
poblacion_base: 3
moderado.jugadores_minimos: 4
alto.jugadores_minimos: 6
muy_alto.jugadores_minimos: 8
record.jugadores_minimos: 10
```

**Para servidor mediano (10-20 jugadores promedio):**
```yaml
poblacion_base: 8
moderado.jugadores_minimos: 10
alto.jugadores_minimos: 13
muy_alto.jugadores_minimos: 16
record.jugadores_minimos: 20
```

**Para servidor grande (20+ jugadores promedio):**
```yaml
poblacion_base: 15
moderado.jugadores_minimos: 18
alto.jugadores_minimos: 22
muy_alto.jugadores_minimos: 26
record.jugadores_minimos: 30
```

---

## 💻 IMPLEMENTACIÓN

### Estado Actual: ✅ Configuración Lista / ⚠️ Código Pendiente

#### ✅ Completado
- [x] Configuración completa en `recompensas.yml`
- [x] Slot en menú `/avo menu` (Slot 40)
- [x] Método `getBonusPoblacion()` implementado
- [x] Método `showReferidosInfo()` implementado
- [x] Handler de click en menú

#### ⚠️ Pendiente de Implementar

##### 1. Sistema de Referidos
**Archivo a crear:** `ReferralSystem.java`

**Funcionalidades:**
- Comando `/invitar <jugador>`
- Base de datos para rastrear referidos
- Sistema de tracking de "jugador activo" (últimas 168h)
- Event listeners para detectar cuando referido sube de nivel
- Sistema de recompensas automático
- Cálculo de multiplicador acumulativo

**Métodos principales:**
```java
public class ReferralSystem {
    // Registrar referido
    public void registerReferral(Player inviter, Player referred);
    
    // Obtener referidos activos
    public int getActiveReferralsCount(Player player);
    
    // Dar recompensa inmediata
    public void giveImmediateReward(Player inviter, Player referred);
    
    // Dar recompensa por hito
    public void giveMilestoneReward(Player inviter, Player referred, MissionRank rank);
    
    // Calcular multiplicador acumulativo
    public double getAccumulativeMultiplier(Player player);
    
    // Verificar si jugador está activo
    public boolean isReferralActive(UUID referredUUID);
}
```

##### 2. Sistema de Bonificación por Población
**Archivo a crear:** `PopulationBonusSystem.java`

**Funcionalidades:**
- Monitoreo constante de jugadores online
- Cálculo dinámico de multiplicador
- Notificaciones de cambio de nivel
- Sistema de bonus horario para RÉCORD
- ActionBar persistente mostrando bonus

**Métodos principales:**
```java
public class PopulationBonusSystem {
    // Obtener multiplicador actual
    public double getCurrentMultiplier();
    
    // Obtener nivel de población
    public String getPopulationLevel();
    
    // Verificar y dar bonus horario
    public void checkHourlyBonus();
    
    // Notificar cambio de nivel
    public void notifyLevelChange(String level);
    
    // Actualizar ActionBar de todos
    public void updateAllActionBars();
}
```

##### 3. Integración con RewardService
**Modificar:** `RewardService.java`

**Cambios necesarios:**
```java
// En el método calculateXP(), agregar:
double finalXP = baseXP;

// Multiplicador de referidos
if (referralSystem != null) {
    double referralMultiplier = referralSystem.getAccumulativeMultiplier(player);
    finalXP *= referralMultiplier;
}

// Multiplicador de población
if (populationBonusSystem != null) {
    double populationMultiplier = populationBonusSystem.getCurrentMultiplier();
    finalXP *= populationMultiplier;
}
```

##### 4. Base de Datos
**Tabla necesaria:** `apocalipsis_referrals`

```sql
CREATE TABLE apocalipsis_referrals (
    id INT AUTO_INCREMENT PRIMARY KEY,
    inviter_uuid VARCHAR(36) NOT NULL,
    referred_uuid VARCHAR(36) NOT NULL,
    referred_name VARCHAR(16) NOT NULL,
    invited_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    current_rank VARCHAR(32) DEFAULT 'NOVATO',
    is_active BOOLEAN DEFAULT TRUE,
    INDEX idx_inviter (inviter_uuid),
    INDEX idx_referred (referred_uuid),
    INDEX idx_active (is_active)
);
```

##### 5. Comandos a Crear

**`/invitar <jugador>`**
```java
@Command(name = "invitar", permission = "apocalipsis.referral")
public void onInvite(Player sender, String targetName) {
    // 1. Verificar que el jugador esté online
    // 2. Verificar que no esté ya registrado
    // 3. Verificar que sea nuevo (< 1 hora de juego)
    // 4. Registrar en base de datos
    // 5. Dar recompensas inmediatas
    // 6. Notificar a ambos jugadores
}
```

**`/misreferidos`**
```java
@Command(name = "misreferidos", permission = "apocalipsis.referral.list")
public void onListReferrals(Player sender) {
    // Mostrar lista de referidos:
    // - Nombre
    // - Estado (Activo/Inactivo)
    // - Rango actual
    // - Última conexión
    // - Multiplicador total actual
}
```

---

## 📊 BALANCE Y ECONOMÍA

### Recompensas por Referidos

**Total por referido que llega a LEYENDA:**
- XP: 500 + 300 + 500 + 800 + 1500 = **3,600 XP**
- Diamantes: 5 + 3 + 5 = **13 Diamantes**
- Bloques PS: 5 + 8 + 15 = **28 Bloques de Protección**
- Netherite: 1 Scrap + 1 Ingot + 2 Ingots = **~4 Lingotes**
- Extras: Perlas, Manzanas, Estrella del Nether

**Con 10 referidos activos:**
- Bonus permanente: +15% XP
- Recompensa única: 1 Élitro

### Bonificación por Población

**Comparativa con población base (5 jugadores):**
```
5 jugadores  = x1.0  (normal)
6 jugadores  = x1.1  (+10%)
9 jugadores  = x1.2  (+20%)
13 jugadores = x1.3  (+30%)
17 jugadores = x1.5  (+50%) + 200 XP/hora
```

**Ganancia horaria en RÉCORD (17+ jugadores):**
- Cada acción de XP vale +50%
- Bonus: +200 XP cada hora
- Bonus: +3 Diamantes cada hora
- Bonus: +2 Manzanas cada hora

**Ejemplo con misión fácil (80 XP base):**
```
5 jugadores:  80 XP
17 jugadores: 120 XP (+50%)
+ Bonus horario: 200 XP
+ Si tienes 10 referidos: 120 * 1.15 = 138 XP
```

---

## 🎮 ESTRATEGIAS DE JUEGO

### Para Jugadores

**Maximiza tus recompensas:**
1. Invita amigos temprano (más tiempo para que suban de nivel)
2. Ayuda a tus referidos a alcanzar rangos altos rápido
3. Juega cuando hay más población online
4. Mantén a tus referidos activos (jueguen al menos 1 vez/semana)

**Objetivo ideal:**
- 10+ referidos activos = +15% XP permanente
- Jugar en horas de alta población = +20-50% XP temporal
- **Combinado: x1.69 a x1.72 multiplicador total**

### Para el Servidor

**Beneficios del sistema:**
- ✅ Incentiva traer jugadores nuevos
- ✅ Promueve la retención (referidos activos)
- ✅ Aumenta actividad en horas pico
- ✅ Crea comunidad más unida
- ✅ Recompensa jugadores leales

**Métricas a monitorear:**
- Promedio de jugadores online por hora
- Tasa de conversión (invitados → activos)
- Retención de referidos a 7/30 días
- Horas pico de población

---

## 📝 NOTAS FINALES

### Prioridades de Implementación
1. **Alta:** Sistema de Referidos básico + Comando `/invitar`
2. **Alta:** Sistema de Bonificación por Población
3. **Media:** Bonus horario en RÉCORD
4. **Media:** Comando `/misreferidos`
5. **Baja:** Efectos visuales avanzados

### Testing Recomendado
- Probar con 2-3 jugadores el sistema de referidos
- Ajustar `poblacion_base` según tu promedio real
- Monitorear si las recompensas son balanceadas
- Verificar que el multiplicador acumulativo funcione correctamente

### Balance Futuro
Si ves que el sistema está desbalanceado:
- **Muy generoso:** Reduce XP de hitos o aumenta requisitos de referidos activos
- **Poco atractivo:** Aumenta recompensas o reduce tiempo de "activo" a 3-5 días

---

**Última actualización:** 13 de Diciembre 2025
**Versión:** 1.0
**Estado:** ✅ Configuración completa | ⚠️ Código pendiente
