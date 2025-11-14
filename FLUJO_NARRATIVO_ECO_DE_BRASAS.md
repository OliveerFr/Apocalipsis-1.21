# 🔥 ECO DE BRASAS - Flujo Narrativo del Evento

## 🎬 Visión General

**Eco de Brasas** es un evento narrativo cooperativo que sumerge a los jugadores en una historia interactiva con el **Observador** como narrador. El evento progresa automáticamente a través de 3 fases, manteniendo la inmersión con:

- **Cinematics automáticos** entre fases
- **Diálogos periódicos** del Observador
- **Progresión clara** con objetivos visuales
- **Recompensas narrativas** al completar cada fase

---

## 📖 NARRATIVA COMPLETA

### 🌋 Contexto Inicial
*Desde que el Nether se abrió, el calor del inframundo se filtró a la superficie. El Observador siente grietas que aparecen al azar, como si el mundo exhalara fuego para no colapsar. Tu tarea no es apagarlo, sino aprender a controlarlo.*

> **"El fuego busca forma... no enemigos."** — El Observador

---

## ⏱️ LÍNEA TEMPORAL AUTOMÁTICA

### 🎭 00:00 - INICIO DEL EVENTO
```
COMANDO: /avo eco start
```

**¿Qué pasa?**
1. **Espera 5 segundos** (silencio, tensión)
2. **Sonido**: Wither spawn (grave, ominoso)
3. **Mensajes aparecen uno por uno** (delay 2s entre cada uno):
   ```
   §5§l⚡ EL OBSERVADOR §r§7detecta una anomalía...
   §7El portal del Nether respira... §c§oexhala calor§7.
   §7La superficie tiembla. §e§oGrietas§7 aparecerán lejos de ti.
   §7Tu tarea: §a§lacércate, ciérralas, recolecta fragmentos§7.
   §8§o"El fuego busca forma... no enemigos." — El Observador
   ```
4. **Sonido**: Portal ambient (2s después del primer sonido)
5. **Título grande en pantalla**:
   ```
   Título: §5§lECO DE BRASAS
   Subtítulo: §7Fase I: §e§lRECOLECCIÓN
   (FadeIn: 1s, Stay: 3s, FadeOut: 1s)
   ```

**Jugadores entienden:**
- Evento narrativo iniciado
- Fase 1 comenzó
- Deben buscar grietas lejos

---

### 🌋 00:05 - FASE 1: RECOLECCIÓN (Duración: 25 minutos)

#### **Objetivo**: Encontrar y cerrar Grietas de Vapor, recolectar fragmentos

**Mecánica Automática:**
1. **Cada 8 minutos**: Spawn nueva Grieta de Vapor
   - Aparece **150-300 bloques** del jugador más cercano
   - **Partículas de humo** (SMOKE) constantes
   - **Sonido de fuego** cada 3 segundos
   - **Vida**: 100 HP (destruir bloques para cerrar)

2. **Al acercarse a grieta** (15 bloques):
   - Spawn **2-4 mobs de defensa** (Blaze, Magma Cube, Wither Skeleton)
   - Mensaje: `§c§o¡La grieta se defiende!`

3. **Al cerrar grieta**:
   - **Drop de fragmentos**:
     - **Ceniza** (60% probabilidad): 3-6 unidades
     - **Fulgor** (25% probabilidad): 1-3 unidades
     - **Eco Roto** (6% probabilidad): 1 unidad
   - Mensaje: `§a§l✓ §7Grieta cerrada. Fragmentos recolectados.`
   - Sonido: ENTITY_ITEM_PICKUP

4. **Diálogos del Observador** (cada 3 minutos, aleatorios):
   ```
   §5§l⚡ EL OBSERVADOR: §7§oEl portal no duerme, solo respira más lento.
   §5§l⚡ EL OBSERVADOR: §7§oLa tierra quiebra donde ustedes caminan.
   §5§l⚡ EL OBSERVADOR: §7§oSi no entienden el calor… lo perderán.
   ```

**Jugadores entienden:**
- Buscar columnas de humo
- Matar mobs, romper grieta
- Coleccionar fragmentos (los necesitarán después)

---

### 🔥 25:00 - TRANSICIÓN A FASE 2

**¿Qué pasa?**
1. **Todas las grietas desaparecen** automáticamente
2. **Espera 10 segundos** (silencio, anticipación)
3. **Sonido**: Ender Dragon growl (ominoso)
4. **Mensajes cinemáticos** (delay 2s entre cada uno):
   ```
   §5§l⚡ EL OBSERVADOR §r§7siente un cambio...
   §7Los fragmentos resuenan. §6§oTres Anclas§7 emergen del suelo.
   §7Deben estabilizarlas: §e§oentrega Ceniza, Fulgor y Eco Roto§7.
   §7Tres puntos sostienen el calor. §a§oSi caen juntos, el mundo respira mejor.§7
   ```
5. **Sonido**: Respawn anchor set spawn (3s después)
6. **Título en pantalla**:
   ```
   Título: §6§lFASE II
   Subtítulo: §7Estabilización - §e§lANCLAS DE FUEGO
   ```
7. **Spawn de 3 Anclas de Fuego**:
   - Separadas al menos 80 bloques entre sí
   - Cerca de spawn/jugadores
   - **Partículas de llamas** constantes
   - **Hologramas flotantes** mostrando requisitos:
     ```
     §6§lANCLA DE FUEGO #1
     §7Ceniza: §e0/30
     §7Fulgor: §e0/10
     §7Eco Roto: §e0/1
     ```

**Jugadores entienden:**
- Fase 1 terminó
- Ahora deben encontrar 3 anclas
- Entregar fragmentos cooperativamente

---

### 🔗 25:10 - FASE 2: ESTABILIZACIÓN (Duración: 45 minutos)

#### **Objetivo**: Estabilizar las 3 Anclas entregando fragmentos

**Mecánica Automática:**
1. **Jugador click derecho en Ancla con fragmentos**:
   - **Consume fragmentos del inventario**
   - **Actualiza holograma** en tiempo real:
     ```
     §6§lANCLA DE FUEGO #1
     §7Ceniza: §e15/30 §a§l▮▮▮▮▮§7▯▯▯▯▯
     §7Fulgor: §e0/10
     §7Eco Roto: §e0/1
     ```
   - **Sonido**: BLOCK_RESPAWN_ANCHOR_CHARGE (satisfactorio)
   - **Partículas**: FLAME burst

2. **Al completar un ancla**:
   - Mensaje global: `§a§l✓ §7Ancla #1 estabilizada (2/3)`
   - **Sonido global**: UI_TOAST_CHALLENGE_COMPLETE
   - **Efecto visual**: Beam de luz hacia arriba

3. **Diálogos del Observador** (cada 3 minutos):
   ```
   §5§l⚡ EL OBSERVADOR: §7§oNo busquen destruirlo, aprendan su ritmo.
   §5§l⚡ EL OBSERVADOR: §7§oLo que tocan, respira. Lo que respira, recuerda.
   §5§l⚡ EL OBSERVADOR: §7§oTres puntos, un equilibrio.
   ```

4. **Al completar LAS 3 ANCLAS**:
   - Mensaje global:
     ```
     §a§l✓ §7Las tres anclas resuenan en armonía.
     §e§oEl ritual puede comenzar§7.
     ```
   - **Sonido global**: UI_TOAST_CHALLENGE_COMPLETE + ENTITY_PLAYER_LEVELUP
   - **Fuegos artificiales** en cada ancla
   - **Auto-progreso a Fase 3** (incluso si no pasaron 45 min)

**Jugadores entienden:**
- Trabajo cooperativo necesario
- Cada ancla requiere muchos fragmentos
- Progreso visual claro (hologramas)
- Completar las 3 para avanzar

---

### 🔮 70:00+ - TRANSICIÓN A FASE 3

**¿Qué pasa?**
1. **Las 3 anclas se transforman** (partículas ascendentes)
2. **Espera 15 segundos** (máxima tensión)
3. **Sonido**: Wither spawn (pitch alto) + End portal spawn
4. **Mensajes épicos** (delay 3s entre cada uno):
   ```
   §5§l⚡ EL OBSERVADOR §r§7ve el ritual comenzar...
   §7Las anclas resonan. §d§oUn altar central§7 se manifiesta.
   §7Reúnanse. §a§oLlenen el sello con pulsos de energía§7.
   §7Si lo logran... §c§oun Guardián probará su valía§7.
   §8§o"Cada chispa que guardan, alguna vez ya ardió en otro mundo." — El Observador
   ```
5. **Título dramático**:
   ```
   Título: §c§lFASE III
   Subtítulo: §7Ritual Final - §d§lEL SELLO
   (Stay: 4s)
   ```
6. **Spawn del Altar Central**:
   - Ubicación: Centro geométrico de los jugadores online
   - **Partículas encantamiento** constantes (ENCHANT)
   - **Holograma flotante**:
     ```
     §d§lALTAR DEL ECO
     §7Progreso: §e0/100 §7pulsos
     §7Click derecho para contribuir
     ```

**Jugadores entienden:**
- Fase final comenzó
- Deben reunirse en el altar
- Boss llegará pronto

---

### ⚔️ 70:15 - FASE 3: RITUAL FINAL (Duración: 15 minutos)

#### **Objetivo**: Llenar el altar con 100 pulsos + derrotar al Guardián

**Mecánica Automática:**

1. **Jugador click derecho en altar**:
   - **Cooldown**: 2 segundos entre clicks
   - **Contribución**: +2 pulsos por click
   - **Actualiza holograma**:
     ```
     §d§lALTAR DEL ECO
     §7Progreso: §e45/100 §a§l▮▮▮▮▮▮▮▮▮§7▯▯▯▯▯▯▯▯▯▯▯
     ```
   - **Partículas**: ENCHANT burst
   - **Sonido**: BLOCK_ENCHANTMENT_TABLE_USE

2. **Al llegar a 75% del ritual** (75 pulsos):
   - **Spawn del Guardián de Brasas**:
     ```
     Nombre: §c§lGuardián de Brasas
     Vida: 300 HP (150 corazones)
     Velocidad: +30%
     Daño: 4 corazones por golpe
     ```
   - **Mensaje global**:
     ```
     §c§l⚠ §c§oEl Guardián ha despertado...
     §7Derrótenlo mientras completan el ritual.
     ```
   - **Sonido**: ENTITY_WITHER_SPAWN (volumen alto)
   - **Efecto de entrada**: Rayo + explosion particles

3. **Habilidades del Guardián** (automáticas):
   - **Cada 5 segundos**: Lanzallamas (cono de fuego, 8 bloques, 1 corazón de daño)
   - **Cada 15 segundos**: Invocar 2 Blazes

4. **Diálogos del Observador** (cada 3 minutos):
   ```
   §5§l⚡ EL OBSERVADOR: §7§oEl fuego busca forma... no enemigos.
   §5§l⚡ EL OBSERVADOR: §7§oCada pulso los acerca a la verdad.
   §5§l⚡ EL OBSERVADOR: §7§oNo teman al Guardián. Es una prueba, no una amenaza.
   ```

5. **Condiciones de victoria** (cualquiera de las dos):
   - Opción A: **Completar ritual** (100/100 pulsos)
   - Opción B: **Derrotar al Guardián**
   - **Óptimo**: Ambos (doble recompensa)

**Jugadores entienden:**
- Cooperación crítica (unos clickean, otros pelean)
- Boss es peligroso pero no imposible
- Múltiples caminos a la victoria

---

### 🏆 VICTORIA - CINEMÁTICA FINAL

**¿Qué pasa?**
1. **Todo se detiene por 3 segundos** (freeze de tensión)
2. **Explosión de partículas** en el altar (ENCHANT + FLAME + END_ROD)
3. **Mensajes de victoria** (delay 2s entre cada uno):
   ```
   §5§l⚡ EL OBSERVADOR §r§a§lasiente con aprobación...
   §7Dominaron el calor. §e§oEl eco se calma§7.
   §7El fuego ya no busca... §a§oencontró forma en ustedes§7.
   §6§l✦ EVENTO COMPLETADO ✦
   ```
4. **Sonidos**:
   - UI_TOAST_CHALLENGE_COMPLETE (inmediato)
   - ENTITY_PLAYER_LEVELUP (2s después)
5. **Título épico**:
   ```
   Título: §a§l✓ EVENTO COMPLETADO
   Subtítulo: §7El Eco de Brasas §e§ose ha calmado
   (Stay: 5s)
   ```
6. **5 Fuegos artificiales** spawneados alrededor del altar
7. **Recompensas globales**:
   ```
   - 5 Diamonds por jugador
   - 10 Emeralds por jugador
   ```
8. **Drops del Guardián** (si fue derrotado):
   ```
   - 3x Netherite Scrap
   - 5x Blaze Rod
   - 1x Nether Star (50% probabilidad)
   ```

---

## 🎯 RESUMEN DE INMERSIÓN

### ✅ Elementos Narrativos Automáticos

1. **Cinematics entre fases**:
   - Delays para crear tensión
   - Múltiples mensajes con timing
   - Sonidos atmosféricos
   - Títulos grandes en pantalla

2. **Diálogos periódicos del Observador**:
   - Cada 3 minutos un mensaje aleatorio
   - Mantiene la presencia del narrador
   - Hints sutiles sobre la historia

3. **Feedback visual constante**:
   - Partículas en entidades clave
   - Hologramas con progreso
   - Beams de luz al completar objetivos

4. **Progresión clara**:
   - Objetivos siempre visibles (hologramas)
   - Mensajes globales de hitos
   - Fases se anuncian con pompa

5. **Sonidos atmosféricos**:
   - Ambientes (fuego, portal)
   - Eventos (wither, dragon)
   - Feedback (enchant, levelup)

### 🎬 Comparación: Desastre vs Evento

| Aspecto | Desastre (Terremoto) | Evento (Eco de Brasas) |
|---------|---------------------|------------------------|
| **Inicio** | Automático/aleatorio | Manual por comando |
| **Narrativa** | Ninguna | Observador como narrador |
| **Fases** | Intensidad variable | Fases con cinematics |
| **Objetivo** | Sobrevivir | Completar historia |
| **Cooperación** | Opcional | Requerida |
| **Recompensa** | Ninguna | Loot + sensación de logro |
| **Inmersión** | Media | Alta (historia + gameplay) |

---

## 📝 Notas de Diseño

### Por qué este diseño mantiene inmersión:

1. **Pausas dramáticas**: Los delays de 5-15s entre fases crean tensión
2. **Mensajes progresivos**: No todo se muestra de golpe, se revela por partes
3. **Sonidos temáticos**: Wither = ominoso, Dragon = épico, Portal = misterioso
4. **Títulos grandes**: Imposible ignorar cambios de fase
5. **Feedback inmediato**: Cada acción (cerrar grieta, entregar fragmento) tiene respuesta
6. **Presencia constante**: El Observador habla cada 3 minutos, nunca desaparece
7. **Progreso visual**: Hologramas = siempre sabes qué hacer
8. **Victoria épica**: La cinemática final recompensa el esfuerzo cooperativo

### Diferencias clave con sistema de desastres:

- **NO usa DisasterController** → usa EventController dedicado
- **NO está en weights** → no puede activarse aleatoriamente
- **NO tiene cooldown** → es único, solo pasa una vez
- **SÍ tiene narrativa** → Observador como personaje activo
- **SÍ tiene cinematics** → transiciones con delays/sonidos/títulos
- **SÍ tiene recompensas** → loot al completar, no solo supervivencia

---

## 🚀 Implementación Técnica

La nueva arquitectura en `src/main/java/me/apocalipsis/events/`:

```
events/
├── EventBase.java           # Clase abstracta para eventos
├── EventController.java     # Gestión de eventos (NO automático)
└── EcoBrasasEvent.java      # Implementación con cinematics
```

Configuración en `eventos.yml`:
- Delays de cinematics
- Textos de diálogos
- Timing de spawn
- Requisitos de fases
- Rewards

El evento **NO** depende de `DisasterController`, es completamente independiente y narrativo.
