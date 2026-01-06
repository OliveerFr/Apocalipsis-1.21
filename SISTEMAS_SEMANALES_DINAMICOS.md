# 🔄 SISTEMAS SEMANALES DINÁMICOS - APOCALIPSIS SERVER
## 📅 Plan de Implementación y Tasklist

---

## 📋 RESUMEN EJECUTIVO

**Objetivo:** Crear un ciclo semanal rotativo que mantenga el servidor fresco y dinámico, independiente de eventos grandes (Navidad, Halloween, etc.).

**Filosofía del Sistema:**
- Rotación automática cada lunes a las 00:00
- Sistemas independientes que se pueden combinar
- Notificaciones previas (domingo 20:00)
- Progresión dentro de cada semana
- Recompensas únicas por semana

**8 Sistemas Principales:**
1. 🌟 Semanas Temáticas (8 rotaciones)
2. ⚔️ Jefes Semanales (6 bosses)
3. 🎪 Minijuegos Semanales (6 minijuegos)
4. 🏆 Desafíos Globales (6+ desafíos)
5. 📊 Modificadores Diarios (30+ modificadores)
6. 🎁 Sistema de Rachas (7 días consecutivos)
7. 🔧 Zonas Dinámicas (5 tipos de zonas)
8. 📅 Micro-Eventos (8 eventos, 3x semana)

---

## 🎯 ESTRUCTURA DE ARCHIVOS A CREAR

```
src/main/java/me/apocalipsis/
├── weekly/
│   ├── WeeklySystemManager.java       # Controlador principal ⭐ CORE
│   ├── WeeklyTheme.java                # Sistema de semanas temáticas
│   ├── WeeklyBoss.java                 # Sistema de bosses semanales
│   ├── WeeklyMinigame.java             # Sistema de minijuegos
│   ├── GlobalChallenge.java            # Desafíos globales
│   ├── DailyModifier.java              # Modificadores diarios
│   ├── StreakRewards.java              # Sistema de rachas
│   ├── DynamicZone.java                # Zonas dinámicas
│   └── MicroEvent.java                 # Micro eventos

src/main/resources/
├── weekly.yml                          # Configuración principal
├── weekly_bosses.yml                   # Configuración de bosses
├── weekly_minigames.yml                # Configuración de minijuegos
├── weekly_modifiers.yml                # Lista de modificadores
└── weekly_zones.yml                    # Configuración de zonas
```

---

## ✅ FASE 1: CORE SYSTEM (SEMANA 1-2)

### 1.1 ESTRUCTURA BASE
| # | Tarea | Archivo | Estado |
|---|-------|---------|--------|
| 1.1.1 | Crear package `weekly/` | `me/apocalipsis/weekly/` | ❌ |
| 1.1.2 | Crear `WeeklySystemManager.java` | `weekly/WeeklySystemManager.java` | ❌ |
| 1.1.3 | Crear `weekly.yml` base | `resources/weekly.yml` | ❌ |
| 1.1.4 | Registrar sistema en `Apocalipsis.java` | `Apocalipsis.java` | ❌ |

### 1.2 SISTEMA DE ROTACIÓN AUTOMÁTICA
| # | Tarea | Descripción | Estado |
|---|-------|-------------|--------|
| 1.2.1 | Implementar scheduler de rotación | Cada lunes 00:00 | ❌ |
| 1.2.2 | Sistema de persistencia de estado | Guardar semana/boss/minijuego actual | ❌ |
| 1.2.3 | Notificaciones previas | Avisar 4h antes del cambio | ❌ |
| 1.2.4 | Método de rotación manual | Comando admin para forzar cambio | ❌ |

### 1.3 COMANDOS BASE
| # | Comando | Descripción | Estado |
|---|---------|-------------|--------|
| 1.3.1 | `/avo weekly enable` | Activar sistema completo | ❌ |
| 1.3.2 | `/avo weekly disable` | Desactivar sistema completo | ❌ |
| 1.3.3 | `/avo weekly info` | Ver estado actual | ❌ |
| 1.3.4 | `/avo weekly reload` | Recargar configuración | ❌ |
| 1.3.5 | `/semanal` | Comando jugador (info) | ❌ |

### 1.4 TAB COMPLETER
| # | Tarea | Descripción | Estado |
|---|-------|-------------|--------|
| 1.4.1 | Añadir "weekly" a `AvoTabCompleter` | Subcomando principal | ❌ |
| 1.4.2 | Autocompletado de subcomandos | enable, disable, info, reload, etc. | ❌ |
| 1.4.3 | Añadir comando `/semanal` | Alias para jugadores | ❌ |

---

## ✅ FASE 2: SEMANAS TEMÁTICAS (SEMANA 3)

### 2.1 ESTRUCTURA BASE
| # | Tarea | Archivo | Estado |
|---|-------|---------|--------|
| 2.1.1 | Crear `WeeklyTheme.java` | `weekly/WeeklyTheme.java` | ❌ |
| 2.1.2 | Crear enum `ThemeType` | 8 semanas diferentes | ❌ |
| 2.1.3 | Sistema de aplicación de efectos | Modificadores por tema | ❌ |

### 2.2 IMPLEMENTAR 8 SEMANAS
| # | Semana | Mecánicas Principales | Estado |
|---|--------|----------------------|--------|
| 2.2.1 | **Escasez de Recursos** 🪨 | Drops -40%, compensación +50% PS minería | ❌ |
| 2.2.2 | **Abundancia Natural** 🌳 | Agricultura +100%, animales +50% | ❌ |
| 2.2.3 | **Noches Eternas** 🌙 | Noche 2x larga, mobs +60%, XP +100% | ❌ |
| 2.2.4 | **Tormenta Geomagnética** ⚡ | Tormentas 60%, rayos, channeling buff | ❌ |
| 2.2.5 | **Zona de Comercio** 💰 | Villagers -40% precios, trades buff | ❌ |
| 2.2.6 | **Invasión Hostil** 💀 | Hordas cada 2h, drops +80% | ❌ |
| 2.2.7 | **Renacimiento Tecnológico** 🔧 | Crafteo -25% costo, enchanting buff | ❌ |
| 2.2.8 | **Exploración Extrema** 🗺️ | Chunks nuevos = PS/XP, estructuras buff | ❌ |

### 2.3 OBJETIVOS SEMANALES
| # | Tarea | Descripción | Estado |
|---|-------|-------------|--------|
| 2.3.1 | Sistema de tracking de objetivos | Contador de progreso individual | ❌ |
| 2.3.2 | Implementar 8 objetivos únicos | Uno por cada semana temática | ❌ |
| 2.3.3 | Sistema de recompensas | Entregar PS + items al completar | ❌ |
| 2.3.4 | Comando `/semanal objetivo` | Ver progreso del objetivo | ❌ |

### 2.4 COMANDOS DE SEMANAS
| # | Comando | Función | Estado |
|---|---------|---------|--------|
| 2.4.1 | `/avo weekly theme <numero>` | Cambiar tema manualmente | ❌ |
| 2.4.2 | `/avo weekly theme info` | Detalles de la semana actual | ❌ |
| 2.4.3 | `/semanal tema` | Jugadores ven tema actual | ❌ |

---

## ✅ FASE 3: JEFES SEMANALES (SEMANA 4)

### 3.1 ESTRUCTURA BASE
| # | Tarea | Archivo | Estado |
|---|-------|---------|--------|
| 3.1.1 | Crear `WeeklyBoss.java` | `weekly/WeeklyBoss.java` | ❌ |
| 3.1.2 | Crear `weekly_bosses.yml` | `resources/weekly_bosses.yml` | ❌ |
| 3.1.3 | Sistema de spawn automático | Spawn al inicio de semana | ❌ |
| 3.1.4 | Sistema de anuncios | Notificar ubicación del boss | ❌ |

### 3.2 IMPLEMENTAR 6 BOSSES
| # | Boss | Mecánicas | Drops | Estado |
|---|------|-----------|-------|--------|
| 3.2.1 | **Guardián del Páramo** 🌫️ | Wither Skeleton gigante, invoca menores | 300 PS + Espada Sharpness VI | ❌ |
| 3.2.2 | **Reina de las Profundidades** 🌊 | Elder Guardian, Mining Fatigue IV | 350 PS + Tridente Riptide IV | ❌ |
| 3.2.3 | **Titán de Obsidiana** 🪨 | Iron Golem corrupto, inmune knockback | 400 PS + Armor Protection VI | ❌ |
| 3.2.4 | **Fantasma del Bosque** 🌲 | Phantom ancestral, invisible | 280 PS + Elytra Unbreaking V | ❌ |
| 3.2.5 | **El Desollador** 🔥 | Blaze emperador, bolas de fuego | 320 PS + Arco Flame II Power VI | ❌ |
| 3.2.6 | **Enjambre Viviente** 🐝 | 20 abejas sincronizadas | 260 PS + Panal curativo | ❌ |

### 3.3 SISTEMA DE DROPS ESPECIALES
| # | Tarea | Descripción | Estado |
|---|-------|-------------|--------|
| 3.3.1 | Items custom con meta | Crear items únicos por boss | ❌ |
| 3.3.2 | Sistema de drop garantizado | Asegurar drops especiales | ❌ |
| 3.3.3 | Distribución equitativa | Damage > 10% = elegible para drop | ❌ |

### 3.4 COMANDOS DE BOSSES
| # | Comando | Función | Estado |
|---|---------|---------|--------|
| 3.4.1 | `/avo weekly boss spawn` | Spawn manual del boss | ❌ |
| 3.4.2 | `/avo weekly boss despawn` | Despawn del boss | ❌ |
| 3.4.3 | `/avo weekly boss info` | Info del boss actual | ❌ |
| 3.4.4 | `/semanal boss` | Ubicación del boss (jugadores) | ❌ |

---

## ✅ FASE 4: MINIJUEGOS SEMANALES (SEMANA 5)

### 4.1 ESTRUCTURA BASE
| # | Tarea | Archivo | Estado |
|---|-------|---------|--------|
| 4.1.1 | Crear `WeeklyMinigame.java` | `weekly/WeeklyMinigame.java` | ❌ |
| 4.1.2 | Crear `weekly_minigames.yml` | `resources/weekly_minigames.yml` | ❌ |
| 4.1.3 | Sistema de leaderboards | Top 3 por minijuego | ❌ |
| 4.1.4 | Sistema de premios | Entregar rewards automático | ❌ |

### 4.2 IMPLEMENTAR 6 MINIJUEGOS
| # | Minijuego | Mecánicas | Estado |
|---|-----------|-----------|--------|
| 4.2.1 | **Cazador de Tesoros** 💎 | 15 cofres, pistas cifradas | ❌ |
| 4.2.2 | **Survival Arena** ⚔️ | Oleadas infinitas, sin regen | ❌ |
| 4.2.3 | **Constructor Express** 🏗️ | Construir en 30 min, votación | ❌ |
| 4.2.4 | **Carrera de Élitras** 🪁 | 20 anillos, cohetes ilimitados | ❌ |
| 4.2.5 | **Pesca Competitiva** 🎣 | 2h pesca, puntos por rareza | ❌ |
| 4.2.6 | **Alquimista Maestro** ⚗️ | Crear 10 pociones, 45 min | ❌ |

### 4.3 SISTEMA DE ARENAS/UBICACIONES
| # | Tarea | Descripción | Estado |
|---|-------|-------------|--------|
| 4.3.1 | Definir zonas para minijuegos | Coordenadas configurables | ❌ |
| 4.3.2 | Sistema de teletransporte | `/semanal minigame join` | ❌ |
| 4.3.3 | Protección de áreas | WorldGuard integration | ❌ |

### 4.4 COMANDOS DE MINIJUEGOS
| # | Comando | Función | Estado |
|---|---------|---------|--------|
| 4.4.1 | `/avo weekly minigame start` | Iniciar minijuego | ❌ |
| 4.4.2 | `/avo weekly minigame stop` | Detener minijuego | ❌ |
| 4.4.3 | `/avo weekly minigame leaderboard` | Ver ranking | ❌ |
| 4.4.4 | `/semanal minigame join` | Unirse (jugadores) | ❌ |

---

## ✅ FASE 5: DESAFÍOS GLOBALES (SEMANA 6)

### 5.1 ESTRUCTURA BASE
| # | Tarea | Archivo | Estado |
|---|-------|---------|--------|
| 5.1.1 | Crear `GlobalChallenge.java` | `weekly/GlobalChallenge.java` | ❌ |
| 5.1.2 | Sistema de contador global | Tracking de progreso servidor | ❌ |
| 5.1.3 | Scoreboard de desafío | Mostrar progreso en sidebar | ❌ |
| 5.1.4 | Sistema de recompensas | Individual + global | ❌ |

### 5.2 IMPLEMENTAR 6 DESAFÍOS
| # | Desafío | Objetivo | Recompensa | Estado |
|---|---------|----------|------------|--------|
| 5.2.1 | **Reforestación Global** 🌳 | 10,000 árboles plantados | 150 PS + saplings 2x | ❌ |
| 5.2.2 | **Exterminio Masivo** 💀 | 5,000 mobs matados | Espada Looting III + XP +50% | ❌ |
| 5.2.3 | **Construcción Comunitaria** 🏛️ | 50,000 bloques colocados | Shulker Box + hub temporal | ❌ |
| 5.2.4 | **Economía Próspera** 💰 | 1,000 trades | 32 Emeralds + precios -30% | ❌ |
| 5.2.5 | **Exploradores Unidos** 🗺️ | 2,000 chunks nuevos | Mapa + elytra buff | ❌ |
| 5.2.6 | **Granjeros del Apocalipsis** 🌾 | 20,000 crops | Hoe Fortune IV + grow +50% | ❌ |

### 5.3 COMANDOS DE DESAFÍOS
| # | Comando | Función | Estado |
|---|---------|---------|--------|
| 5.3.1 | `/avo weekly challenge progress` | Ver progreso global | ❌ |
| 5.3.2 | `/avo weekly challenge complete` | Forzar completar | ❌ |
| 5.3.3 | `/semanal desafio` | Ver desafío actual (jugadores) | ❌ |

---

## ✅ FASE 6: MODIFICADORES DIARIOS (SEMANA 6)

### 6.1 ESTRUCTURA BASE
| # | Tarea | Archivo | Estado |
|---|-------|---------|--------|
| 6.1.1 | Crear `DailyModifier.java` | `weekly/DailyModifier.java` | ❌ |
| 6.1.2 | Crear `weekly_modifiers.yml` | `resources/weekly_modifiers.yml` | ❌ |
| 6.1.3 | Sistema de rotación diaria | Cambio automático 00:00 | ❌ |
| 6.1.4 | Notificación de modificador | Anunciar en chat al cambiar | ❌ |

### 6.2 IMPLEMENTAR MODIFICADORES
| # | Categoría | Cantidad | Estado |
|---|-----------|----------|--------|
| 6.2.1 | **Modificadores Positivos** ✅ | 15 modificadores | ❌ |
| 6.2.2 | **Modificadores Negativos** ❌ | 10 modificadores | ❌ |
| 6.2.3 | **Modificadores Neutros** ⚖️ | 10 modificadores | ❌ |

### 6.3 COMANDOS DE MODIFICADORES
| # | Comando | Función | Estado |
|---|---------|---------|--------|
| 6.3.1 | `/avo weekly modifier set <nombre>` | Forzar modificador | ❌ |
| 6.3.2 | `/avo weekly modifier random` | Seleccionar aleatorio | ❌ |
| 6.3.3 | `/semanal modificador` | Ver modificador actual | ❌ |

---

## ✅ FASE 7: SISTEMA DE RACHAS (SEMANA 6)

### 7.1 ESTRUCTURA BASE
| # | Tarea | Archivo | Estado |
|---|-------|---------|--------|
| 7.1.1 | Crear `StreakRewards.java` | `weekly/StreakRewards.java` | ❌ |
| 7.1.2 | Sistema de check-in diario | Tracking de 30 min jugados | ❌ |
| 7.1.3 | Persistencia de rachas | Guardar progreso por jugador | ❌ |
| 7.1.4 | Reset automático | Perder racha si falta 1 día | ❌ |

### 7.2 RECOMPENSAS PROGRESIVAS
| # | Día | Recompensa | Estado |
|---|-----|------------|--------|
| 7.2.1 | Día 1 | 20 PS | ❌ |
| 7.2.2 | Día 2 | 40 PS + 5 Golden Apples | ❌ |
| 7.2.3 | Día 3 | 60 PS + Poción aleatoria | ❌ |
| 7.2.4 | Día 4 | 80 PS + Tool Unbreaking III | ❌ |
| 7.2.5 | Día 5 | 100 PS + Armor Protection III | ❌ |
| 7.2.6 | Día 6 | 150 PS + Elytra Unbreaking II | ❌ |
| 7.2.7 | Día 7 | 300 PS + Caja Legendaria + Rango temporal | ❌ |

### 7.3 CAJA LEGENDARIA
| # | Tarea | Descripción | Estado |
|---|-------|-------------|--------|
| 7.3.1 | Crear item "Caja Legendaria" | Item especial con lore | ❌ |
| 7.3.2 | Sistema de apertura | Click derecho = abrir | ❌ |
| 7.3.3 | Pool de rewards | 3-5 items aleatorios de lista | ❌ |

### 7.4 COMANDOS DE RACHAS
| # | Comando | Función | Estado |
|---|---------|---------|--------|
| 7.4.1 | `/semanal racha` | Ver racha actual | ❌ |
| 7.4.2 | `/semanal checkin` | Check-in manual | ❌ |
| 7.4.3 | `/avo weekly streak reset <jugador>` | Reset racha (admin) | ❌ |

---

## ✅ FASE 8: ZONAS DINÁMICAS (SEMANA 7)

### 8.1 ESTRUCTURA BASE
| # | Tarea | Archivo | Estado |
|---|-------|---------|--------|
| 8.1.1 | Crear `DynamicZone.java` | `weekly/DynamicZone.java` | ❌ |
| 8.1.2 | Crear `weekly_zones.yml` | `resources/weekly_zones.yml` | ❌ |
| 8.1.3 | Sistema de definición de áreas | Cuboid regions | ❌ |
| 8.1.4 | Aplicar efectos por zona | Detectar jugadores en zona | ❌ |

### 8.2 IMPLEMENTAR 5 TIPOS DE ZONAS
| # | Zona | Efectos | Estado |
|---|------|---------|--------|
| 8.2.1 | **Zona de Prosperidad** 💰 | XP +100%, drops +50%, ore +30% | ❌ |
| 8.2.2 | **Zona Maldita** 💀 | Mobs +200%, loot 3x, hunger rápido | ❌ |
| 8.2.3 | **Zona de Construcción** 🏗️ | Crafteo gratis 70%, flight | ❌ |
| 8.2.4 | **Zona de Santuario** ⛪ | No PvP, regen II, no mobs | ❌ |
| 8.2.5 | **Zona de Combate** ⚔️ | PvP, no drop items, kills = 50 PS | ❌ |

### 8.3 SISTEMA VISUAL
| # | Tarea | Descripción | Estado |
|---|-------|-------------|--------|
| 8.3.1 | Partículas por zona | Diferentes por tipo | ❌ |
| 8.3.2 | Bossbar al entrar | Indicar zona actual | ❌ |
| 8.3.3 | Comando de visualización | Mostrar límites de zona | ❌ |

### 8.4 COMANDOS DE ZONAS
| # | Comando | Función | Estado |
|---|---------|---------|--------|
| 8.4.1 | `/avo weekly zone create <tipo>` | Crear zona en selección | ❌ |
| 8.4.2 | `/avo weekly zone delete <id>` | Eliminar zona | ❌ |
| 8.4.3 | `/avo weekly zone list` | Listar zonas activas | ❌ |
| 8.4.4 | `/semanal zona` | Ver zonas activas (jugadores) | ❌ |

---

## ✅ FASE 9: MICRO-EVENTOS (SEMANA 7)

### 9.1 ESTRUCTURA BASE
| # | Tarea | Archivo | Estado |
|---|-------|---------|--------|
| 9.1.1 | Crear `MicroEvent.java` | `weekly/MicroEvent.java` | ❌ |
| 9.1.2 | Sistema de scheduling | 3x por semana (L/M/S) | ❌ |
| 9.1.3 | Notificaciones previas | 30 min antes del evento | ❌ |
| 9.1.4 | Auto-inicio y auto-fin | Duración automática | ❌ |

### 9.2 IMPLEMENTAR 8 MICRO-EVENTOS
| # | Evento | Duración | Mecánicas | Estado |
|---|--------|----------|-----------|--------|
| 9.2.1 | **Happy Hour** ⏰ | 1h | XP x3, PS x2, drops x2 | ❌ |
| 9.2.2 | **Invasión Zombie** 🧟 | 2h | Zombies masivos, 100 PS reward | ❌ |
| 9.2.3 | **Lluvia de Recursos** 🎁 | 30min | Cofres caen del cielo cada 5 min | ❌ |
| 9.2.4 | **Mercado Ambulante** 🛒 | 1h | NPCs merchants, descuentos 50% | ❌ |
| 9.2.5 | **Competencia de Pesca** 🎣 | 1h | Top 3 pescadores = premios | ❌ |
| 9.2.6 | **Tormenta de Experiencia** ⭐ | 45min | XP orbs caen del cielo | ❌ |
| 9.2.7 | **Boss Rush** 👹 | 2h | 5 bosses consecutivos, cada 20 min | ❌ |
| 9.2.8 | **Crafteo Gratuito** 🔨 | 1h | Crafteo sin consumir, límite 64 | ❌ |

### 9.3 COMANDOS DE MICRO-EVENTOS
| # | Comando | Función | Estado |
|---|---------|---------|--------|
| 9.3.1 | `/avo weekly event start <evento>` | Iniciar evento | ❌ |
| 9.3.2 | `/avo weekly event stop` | Detener evento actual | ❌ |
| 9.3.3 | `/avo weekly event schedule` | Ver calendario | ❌ |

---

## ✅ FASE 10: POLISH Y TESTING (SEMANA 8)

### 10.1 BALANCEO
| # | Tarea | Descripción | Estado |
|---|-------|-------------|--------|
| 10.1.1 | Ajustar economía de PS | Revisar total semanal | ❌ |
| 10.1.2 | Balancear dificultad bosses | Testing de combate | ❌ |
| 10.1.3 | Ajustar modificadores | Equilibrar positivos/negativos | ❌ |
| 10.1.4 | Revisar recompensas | Comparar con sistemas existentes | ❌ |

### 10.2 TESTING
| # | Tarea | Descripción | Estado |
|---|-------|-------------|--------|
| 10.2.1 | Test de rotación automática | Verificar cambio semanal | ❌ |
| 10.2.2 | Test de persistencia | Guardar/cargar estado | ❌ |
| 10.2.3 | Test de comandos | Todos los comandos funcionan | ❌ |
| 10.2.4 | Test de conflictos | Compatibilidad con otros sistemas | ❌ |

### 10.3 OPTIMIZACIÓN
| # | Tarea | Descripción | Estado |
|---|-------|-------------|--------|
| 10.3.1 | Optimizar tasks periódicos | Reducir carga del servidor | ❌ |
| 10.3.2 | Optimizar queries de datos | Mejorar acceso a persistencia | ❌ |
| 10.3.3 | Caché de configuraciones | Evitar leer YML repetidamente | ❌ |

### 10.4 DOCUMENTACIÓN
| # | Tarea | Descripción | Estado |
|---|-------|-------------|--------|
| 10.4.1 | Documentar código | Javadocs en clases principales | ❌ |
| 10.4.2 | Crear guía de admin | Comandos y configuración | ❌ |
| 10.4.3 | Crear guía de jugadores | Cómo participar en sistemas | ❌ |
| 10.4.4 | README de sistemas semanales | Documentación completa | ❌ |

---

## 📊 DISEÑO DETALLADO DE SISTEMAS

### 🌟 SISTEMA 1: SEMANAS TEMÁTICAS DE SUPERVIVENCIA

### 📋 Concepto
Cada semana tiene un "desafío ambiental" que cambia cómo se juega el servidor.

### 🗓️ Rotación de Semanas (8 semanas = 2 meses de ciclo)

#### **SEMANA 1: Escasez de Recursos** 🪨
**Tema:** Los recursos naturales están agotándose
- **Mecánicas:**
  - Drop de minerales reducido -40%
  - Drop de madera reducido -30%
  - Menas generan menos items al romperlas
  - Cofres naturales contienen menos items
- **Compensación:**
  - Misiones de minería dan +50% PS
  - Craftear items da experiencia extra
- **Objetivo Semanal:** "Minero Eficiente" - Minar 500 bloques de piedra
- **Recompensa:** 200 PS + Kit de Herramientas Eficientes (Efficiency IV)

#### **SEMANA 2: Abundancia Natural** 🌳
**Tema:** La naturaleza florece con vigor
- **Mecánicas:**
  - Drop de agricultura +100%
  - Árboles crecen 2x más rápido
  - Animales se reproducen 50% más rápido
  - Fishing luck aumentado
- **Misiones Especiales:**
  - Recolectar 200 wheat
  - Criar 10 animales
  - Pescar 30 peces
- **Objetivo Semanal:** "Granjero Maestro"
- **Recompensa:** 180 PS + Azada con Fortune III + Caña de pescar especial

#### **SEMANA 3: Noches Eternas** 🌙
**Tema:** Las noches duran el doble, más mobs hostiles
- **Mecánicas:**
  - Ciclo día/noche alterado (noche 2x más larga)
  - Spawn de mobs +60% durante la noche
  - Mobs más agresivos (detección +5 bloques)
  - Phantoms aparecen antes
- **Compensación:**
  - Matar mobs de noche: +100% XP
  - Misiones de combate: +75% PS
- **Objetivo Semanal:** "Cazador Nocturno" - Matar 200 mobs de noche
- **Recompensa:** 250 PS + Armadura con Protección contra Undead

#### **SEMANA 4: Tormenta Geomagnética** ⚡
**Tema:** Fenómenos eléctricos intensos
- **Mecánicas:**
  - Tormentas más frecuentes (60% del tiempo)
  - Rayos caen aleatoriamente cerca de jugadores
  - Rayos que impactan dan "Carga Eléctrica" (Speed II, 30s)
  - Tridents con Channeling más poderosos
- **Misiones:**
  - Sobrevivir 10 impactos de rayo
  - Usar Channeling en 5 mobs
- **Objetivo Semanal:** "Domador de Tormentas"
- **Recompensa:** 220 PS + Tridente con Channeling + Loyalty III

#### **SEMANA 5: Zona de Comercio** 💰
**Tema:** Los aldeanos ofrecen mejores tratos
- **Mecánicas:**
  - Villagers reducen precios -40%
  - Villagers dan items extra al comerciar
  - Spawn de Wandering Trader aumentado
  - Encontrar aldeanos da XP
- **Misiones:**
  - Comerciar 50 veces
  - Encontrar 5 aldeanos diferentes
  - Conseguir un trade de nivel Master
- **Objetivo Semanal:** "Magnate Comerciante"
- **Recompensa:** 200 PS + 32 Emeralds + Aldeano especial portable

#### **SEMANA 6: Invasión Hostil** 💀
**Tema:** Oleadas coordinadas de mobs
- **Mecánicas:**
  - Cada 2 horas spawn de "Horda" cerca de jugadores
  - Mobs en grupos organizados (10-15 entidades)
  - Mobs tienen equipamiento mejorado
  - Boss mobs aleatorios pueden aparecer
- **Compensación:**
  - Drops de mobs +80%
  - XP de combate +100%
  - Loot especial de hordas
- **Objetivo Semanal:** "Defensor Incansable" - Sobrevivir 5 hordas
- **Recompensa:** 300 PS + Espada con Sharpness V + Looting III

#### **SEMANA 7: Renacimiento Tecnológico** 🔧
**Tema:** Crafteo y construcción potenciados
- **Mecánicas:**
  - Craftear items cuesta -25% materiales
  - Furnaces/Blast Furnaces cocinan 2x más rápido
  - Anvils no consumen XP (o 50% menos)
  - Enchanting da mejores resultados
- **Misiones:**
  - Craftear 100 items diferentes
  - Fundir 200 items
  - Encantar 10 items
- **Objetivo Semanal:** "Ingeniero Supremo"
- **Recompensa:** 180 PS + Yunque inquebrable + Libro de Mending

#### **SEMANA 8: Exploración Extrema** 🗺️
**Tema:** Descubrir nuevos territorios es vital
- **Mecánicas:**
  - Descubrir chunks nuevos da XP y PS
  - Estructuras (templos, mansions, etc.) dan recompensas extra
  - Mapa revela estructuras cercanas (300 bloques)
  - Elytra y cohetes más duraderos
- **Misiones:**
  - Explorar 500 chunks nuevos
  - Encontrar 3 estructuras
  - Viajar 10,000 bloques
- **Objetivo Semanal:** "Cartógrafo Legendario"
- **Recompensa:** 250 PS + Elytra con Unbreaking III + Mapa del Tesoro

---

## ⚔️ SISTEMA 2: JEFES SEMANALES ROTATIVOS

### 📋 Concepto
Cada semana aparece un boss único con mecánicas especiales en un lugar del mapa.

### 🗓️ Rotación de Bosses (6 semanas)

#### **BOSS 1: El Guardián del Páramo** 🌫️
- **Tipo:** Wither Skeleton gigante
- **Spawn:** Bioma Plains, coordenadas aleatorias anunciadas
- **Mecánicas:**
  - Invoca wither skeletons menores
  - Lanza proyectiles de wither
  - Área de efecto Wither II
- **Drops:**
  - 300 PS
  - Espada "Hoja del Páramo" (Sharpness VI, Wither aspect)
  - Wither Rose x5

#### **BOSS 2: La Reina de las Profundidades** 🌊
- **Tipo:** Elder Guardian mejorado
- **Spawn:** Océano profundo, anunciado 1 hora antes
- **Mecánicas:**
  - Mining Fatigue IV en área grande
  - Invoca Guardians
  - Rayo láser potenciado
- **Drops:**
  - 350 PS
  - Tridente "Ira Oceánica" (Riptide IV, Impaling VI)
  - Esponja x32

#### **BOSS 3: Titán de Obsidiana** 🪨
- **Tipo:** Iron Golem corrupto gigante
- **Mecánicas:**
  - Inmune a knockback
  - Golpes causan slowness
  - Regenera vida si no es atacado 10s
- **Drops:**
  - 400 PS
  - Armor "Piel de Titán" (Protection VI, Thorns IV)
  - Block de hierro x64

#### **BOSS 4: Fantasma del Bosque** 🌲
- **Tipo:** Phantom ancestral
- **Spawn:** Bosque oscuro, solo de noche
- **Mecánicas:**
  - Invisible cada 30 segundos
  - Vuelo errático
  - Invoca Phantoms menores
- **Drops:**
  - 280 PS
  - Elytra "Alas Espectrales" (Unbreaking V, Mending)
  - Phantom Membrane x20

#### **BOSS 5: El Desollador** 🔥
- **Tipo:** Blaze emperador
- **Spawn:** Nether Wastes, portal conocido
- **Mecánicas:**
  - Bolas de fuego explosivas
  - Crea círculos de fuego
  - Inmune al agua
- **Drops:**
  - 320 PS
  - Arco "Arco Ígneo" (Flame II, Power VI)
  - Blaze Rod x32

#### **BOSS 6: Enjambre Viviente** 🐝
- **Tipo:** Enjambre de 20 abejas sincronizadas
- **Spawn:** Flower Forest
- **Mecánicas:**
  - Atacan en formación
  - Se curan entre ellas
  - Envenenan al tocar
- **Drops:**
  - 260 PS
  - Panal "Dulzura Eterna" (Restaura 5 corazones, stack 16)
  - Honey Block x64

---

## 🎪 SISTEMA 3: MINIJUEGOS SEMANALES

### 📋 Concepto
Cada semana activa un minijuego temporal con leaderboard y premios.

### 🎮 Lista de Minijuegos (Rotación de 6)

#### **1. Cazador de Tesoros** 💎
- **Descripción:** Buscar cofres ocultos en el mapa
- **Mecánica:**
  - 15 cofres spawn semanalmente
  - Pistas cada 12 horas en chat
  - Coordenadas cifradas
- **Leaderboard:** Top 3 por cofres encontrados
- **Premios:**
  - 1°: 300 PS + Mapa del Tesoro permanente
  - 2°: 200 PS + Brújula especial
  - 3°: 150 PS + Ender Pearls x16

#### **2. Survival Arena** ⚔️
- **Descripción:** Oleadas infinitas de mobs
- **Mecánica:**
  - Arena PvE individual
  - Cada ola más difícil
  - Sin regeneración natural
- **Leaderboard:** Top 3 por oleadas sobrevividas
- **Premios:**
  - 1°: 350 PS + Armadura completa Protection IV
  - 2°: 250 PS + Espada Sharpness IV
  - 3°: 180 PS + Golden Apples x10

#### **3. Constructor Express** 🏗️
- **Descripción:** Construir estructura temática en 30 min
- **Mecánica:**
  - Tema aleatorio cada semana
  - Área delimitada (20x20)
  - Materiales limitados dados
- **Votación:** Comunidad vota mejores construcciones
- **Premios:**
  - 1°: 250 PS + Shulker Box + Bloques decorativos x64
  - 2°: 180 PS + Bloques variados x128
  - 3°: 120 PS + Wool x64

#### **4. Carrera de Élitras** 🪁
- **Descripción:** Circuito aéreo con anillos
- **Mecánica:**
  - 20 anillos a atravesar
  - Cohetes ilimitados
  - Obstáculos y trampas
- **Leaderboard:** Top 3 por tiempo
- **Premios:**
  - 1°: 280 PS + Elytra Unbreaking IV
  - 2°: 200 PS + Firework Rockets x128
  - 3°: 150 PS + Firework Rockets x64

#### **5. Pesca Competitiva** 🎣
- **Descripción:** Pescar items raros
- **Mecánica:**
  - 2 horas de pesca continua
  - Puntos por rareza del item
  - Ubicación especial de pesca
- **Leaderboard:** Top 3 por puntos
- **Premios:**
  - 1°: 220 PS + Caña con Luck of the Sea V
  - 2°: 170 PS + Caña con Lure IV
  - 3°: 120 PS + Name Tag x5

#### **6. Alquimista Maestro** ⚗️
- **Descripción:** Crear pociones complejas
- **Mecánica:**
  - Lista de 10 pociones a crear
  - Ingredientes escondidos en el mapa
  - Tiempo límite: 45 minutos
- **Leaderboard:** Top 3 por pociones creadas
- **Premios:**
  - 1°: 240 PS + Brewing Stand automático
  - 2°: 180 PS + Blaze Powder x32
  - 3°: 130 PS + Nether Wart x64

---

## 🏆 SISTEMA 4: DESAFÍOS GLOBALES SEMANALES

### 📋 Concepto
Todos los jugadores contribuyen a un objetivo común del servidor.

### 🌍 Ejemplos de Desafíos (Rotación infinita)

#### **DESAFÍO 1: Reforestación Global** 🌳
- **Objetivo:** Plantar 10,000 árboles entre todos
- **Tracking:** Contador en scoreboard
- **Incentivo Individual:** Cada 50 árboles = 10 PS
- **Recompensa Global (si se completa):**
  - Todos reciben: 150 PS
  - Unlock temporal: Saplings crecen 2x rápido (3 días)

#### **DESAFÍO 2: Exterminio Masivo** 💀
- **Objetivo:** Matar 5,000 mobs hostiles entre todos
- **Tracking:** Contador global
- **Incentivo Individual:** Cada 25 kills = 15 PS
- **Recompensa Global:**
  - Todos reciben: Espada con Looting III
  - Unlock: XP de mobs +50% (3 días)

#### **DESAFÍO 3: Construcción Comunitaria** 🏛️
- **Objetivo:** Colocar 50,000 bloques en zona designada
- **Tracking:** Área marcada, contador automático
- **Incentivo Individual:** Cada 100 bloques = 8 PS
- **Recompensa Global:**
  - Todos reciben: Shulker Box
  - Construcción se convierte en hub/market temporal

#### **DESAFÍO 4: Economía Próspera** 💰
- **Objetivo:** Realizar 1,000 trades con aldeanos
- **Tracking:** Contador de intercambios
- **Incentivo Individual:** Cada 10 trades = 12 PS
- **Recompensa Global:**
  - Todos reciben: 32 Emeralds
  - Unlock: Villagers -30% precios (3 días)

#### **DESAFÍO 5: Exploradores Unidos** 🗺️
- **Objetivo:** Descubrir 2,000 chunks nuevos entre todos
- **Tracking:** Chunks explorados por primera vez
- **Incentivo Individual:** Cada 20 chunks = 10 PS
- **Recompensa Global:**
  - Todos reciben: Mapa completo del servidor
  - Unlock: Elytra más duraderos (3 días)

#### **DESAFÍO 6: Granjeros del Apocalipsis** 🌾
- **Objetivo:** Cosechar 20,000 crops
- **Tracking:** Wheat, carrots, potatoes, beetroot
- **Incentivo Individual:** Cada 100 crops = 8 PS
- **Recompensa Global:**
  - Todos reciben: Hoe con Fortune IV
  - Unlock: Crops crecen 50% más rápido (3 días)

---

## 📊 SISTEMA 5: MODIFICADORES ALEATORIOS DIARIOS

### 📋 Concepto
Cada día (a las 00:00) activa un modificador pequeño que cambia ligeramente el gameplay.

### 🎲 Lista de Modificadores (30+, uno aleatorio por día)

#### **Modificadores Positivos** ✅
1. **Día de Suerte** - Luck effect para todos, mejor loot
2. **Experiencia Doble** - XP de todas las fuentes x2
3. **Regeneración Natural** - Regeneration I pasivo
4. **Velocidad Aumentada** - Speed I para todos
5. **Salto Alto** - Jump Boost II
6. **Resistencia al Fuego** - Fire Resistance permanente
7. **Respiración Acuática** - Water Breathing para todos
8. **Visión Nocturna** - Night Vision automático
9. **Caída Lenta** - Slow Falling reduce fall damage 50%
10. **Fuerza Hercúlea** - Strength I pasivo
11. **Hambre Reducida** - Saturation efecto, hambre -50%
12. **Vuelo Mejorado** - Elytra más rápido y duradero
13. **Crafteo Eficiente** - Craftear devuelve 10% materiales
14. **Midas Touch** - Items de oro duplicados al minar
15. **Diamantes Brillantes** - Diamantes más fáciles de encontrar

#### **Modificadores Negativos** ❌
1. **Gravedad Pesada** - Slowness I para todos
2. **Hambre Voraz** - Hambre se consume 2x más rápido
3. **Debilidad** - Damage -20%
4. **Torpeza** - Mining Fatigue I
5. **Noche Eterna** - No amanece en todo el día
6. **Mobs Agresivos** - Mobs detectan a +10 bloques
7. **Items Frágiles** - Durabilidad se gasta 50% más rápido
8. **Caos Climático** - Clima cambia cada 5 minutos
9. **Explosiones Amplificadas** - Creepers explotan más fuerte
10. **Veneno Ambiental** - Poison I cada 2 minutos, 10 segundos

#### **Modificadores Neutros/Interesantes** ⚖️
1. **Mundo Invertido** - Jugadores caminan en el techo
2. **Tamaño Aleatorio** - Jugadores cambian de escala
3. **Gravedad Lunar** - Jump boost extremo, fall damage reducido
4. **Efectos Locos** - Efectos aleatorios cada 30 segundos
5. **Bloques Extraños** - Algunos bloques cambian al romperlos
6. **Items Shuffle** - Hotbar se mezcla cada minuto
7. **Muerte Rápida** - Vida máxima reducida a 5 corazones
8. **Tanque Viviente** - Vida máxima aumentada a 15 corazones
9. **Sin Regeneración** - Regeneración natural desactivada
10. **Comercio Loco** - Villagers piden items aleatorios

---

## 🎁 SISTEMA 6: RECOMPENSAS DE RACHA SEMANAL

### 📋 Concepto
Premiar a jugadores que juegan consistentemente durante la semana.

### ⭐ Sistema de Rachas

#### **Check-in Diario**
- Jugador debe jugar **mínimo 30 minutos** cada día
- Comando `/avo checkin` para verificar progreso
- Scoreboard muestra: "Racha: X/7 días"

#### **Recompensas Progresivas**

| Días Consecutivos | Recompensa |
|-------------------|------------|
| **1 día** | 20 PS |
| **2 días** | 40 PS + 5 Golden Apples |
| **3 días** | 60 PS + Poción aleatoria |
| **4 días** | 80 PS + Tool con Unbreaking III |
| **5 días** | 100 PS + Armor piece con Protection III |
| **6 días** | 150 PS + Elytra con Unbreaking II |
| **7 días** | **300 PS + Caja Legendaria + Rango temporal "Dedicado" (3 días)** |

#### **Caja Legendaria** (Recompensa de 7 días)
Contiene (aleatorio 3-5 items):
- Netherite Scrap x2
- Diamond x5
- Enchanted Book (nivel alto)
- Totem of Undying
- Elytra
- Shulker Box
- Dragon Egg (decorativo)
- Beacon

---

## 🔧 SISTEMA 7: ZONAS DINÁMICAS DEL MAPA

### 📋 Concepto
Áreas del mapa cambian cada semana con bonificaciones especiales.

### 🗺️ Tipos de Zonas (Rotación aleatoria)

#### **1. Zona de Prosperidad** 💰
- **Ubicación:** Área de 500x500 bloques anunciada
- **Efecto:** Dentro de la zona:
  - XP +100%
  - Drops de mobs +50%
  - Ore generation +30%
  - Villagers con mejores trades
- **Duración:** Toda la semana
- **Visual:** Partículas doradas cayendo

#### **2. Zona Maldita** 💀
- **Ubicación:** Área de 400x400 bloques
- **Efecto:** Dentro de la zona:
  - Spawn de mobs +200%
  - Mobs más fuertes
  - Hunger deplete más rápido
  - Pero... Loot 3x mejor
- **Duración:** Toda la semana
- **Visual:** Partículas oscuras, niebla

#### **3. Zona de Construcción** 🏗️
- **Ubicación:** Área designada
- **Efecto:** Dentro de la zona:
  - Crafteo sin consumir recursos (70% chance)
  - Bloques se colocan instantáneamente
  - Flight permitido
- **Duración:** Toda la semana
- **Visual:** Partículas de construcción

#### **4. Zona de Santuario** ⛪
- **Ubicación:** Área protegida
- **Efecto:** Dentro de la zona:
  - PvP desactivado
  - Mobs no pueden entrar
  - Regeneration II constante
  - Tiendas/trades especiales
- **Duración:** Toda la semana
- **Visual:** Barrera luminosa, partículas curativas

#### **5. Zona de Combate** ⚔️
- **Ubicación:** Arena abierta
- **Efecto:** Dentro de la zona:
  - PvP permitido
  - No drop de items al morir (regresan al inventario)
  - Kills dan 50 PS
  - Leaderboard semanal de kills
- **Duración:** Toda la semana
- **Visual:** Partículas rojas, suelo marcado

---

## 📅 SISTEMA 8: CALENDARIO DE EVENTOS MICRO

### 📋 Concepto
Eventos pequeños de 1-2 horas que ocurren 3 veces por semana.

### ⏰ Horarios Fijos
- **Lunes 20:00** - Evento sorpresa
- **Miércoles 18:00** - Evento sorpresa
- **Sábado 16:00** - Evento grande semanal

### 🎪 Lista de Micro-Eventos (Aleatorios)

#### **1. Happy Hour** ⏰ (1 hora)
- XP x3
- PS por misiones x2
- Drops de mobs x2

#### **2. Invasión Zombie** 🧟 (2 horas)
- Zombies spawn masivamente
- Zombies buffed
- Recompensa por sobrevivir: 100 PS

#### **3. Lluvia de Recursos** 🎁 (30 min)
- Caen cofres del cielo cada 5 min
- Contienen recursos aleatorios
- Anunciado en coordenadas

#### **4. Mercado Ambulante** 🛒 (1 hora)
- NPCs merchants aparecen
- Venden items exclusivos por PS
- Descuentos del 50%

#### **5. Competencia de Pesca** 🎣 (1 hora)
- Top 3 pescadores ganan premios
- Fish más grandes = más puntos
- Premios: PS + items raros

#### **6. Tormenta de Experiencia** ⭐ (45 min)
- Orbs de XP caen del cielo
- Recolectar = XP masivo
- Competencia por más XP

#### **7. Boss Rush** 👹 (2 horas)
- 5 bosses spawn consecutivamente
- Cada 20 minutos uno nuevo
- Recompensas acumulativas

#### **8. Crafteo Gratuito** 🔨 (1 hora)
- Craftear no consume materiales
- Limite: 64 items por jugador
- Ideal para preparar equipo

---

## 🎮 IMPLEMENTACIÓN TÉCNICA

### 📂 Estructura de Archivos

```
src/main/java/me/apocalipsis/
├── weekly/
│   ├── WeeklySystemManager.java       # Controlador principal
│   ├── WeeklyTheme.java                # Enum de semanas temáticas
│   ├── WeeklyBoss.java                 # Sistema de bosses semanales
│   ├── WeeklyMinigame.java             # Sistema de minijuegos
│   ├── GlobalChallenge.java            # Desafíos globales
│   ├── DailyModifier.java              # Modificadores diarios
│   ├── StreakRewards.java              # Sistema de rachas
│   ├── DynamicZone.java                # Zonas dinámicas
│   └── MicroEvent.java                 # Micro eventos

src/main/resources/
├── weekly.yml                          # Configuración de sistemas semanales
├── bosses.yml                          # Configuración de bosses
├── minigames.yml                       # Configuración de minijuegos
└── modifiers.yml                       # Lista de modificadores
```

### ⚙️ Configuración Ejemplo (weekly.yml)

```yaml
sistemas_semanales:
  enabled: true
  notificar_cambio_semana: true
  notificacion_previa_horas: 4  # Avisar 4 horas antes del cambio
  
  # Rotación automática cada lunes 00:00
  rotacion:
    auto: true
    dia: "MONDAY"
    hora: 0
    minuto: 0
  
  # Qué sistemas están activos
  activos:
    semanas_tematicas: true
    jefes_semanales: true
    minijuegos_semanales: true
    desafios_globales: true
    modificadores_diarios: true
    recompensas_racha: true
    zonas_dinamicas: true
    micro_eventos: true

semana_actual: 1  # 1-8, se actualiza automáticamente
boss_actual: 1    # 1-6, se actualiza automáticamente
minijuego_actual: 1  # 1-7, se actualiza automáticamente
```

### 🎯 Comandos de Administración

```bash
# Control del sistema
/avo weekly enable                    # Activar sistema semanal
/avo weekly disable                   # Desactivar sistema semanal
/avo weekly next                      # Forzar siguiente semana
/avo weekly set <numero>              # Cambiar a semana específica
/avo weekly reload                    # Recargar configuración

# Información
/avo weekly info                      # Ver semana/boss/minijuego actual
/avo weekly schedule                  # Ver calendario completo del mes

# Sistemas individuales
/avo weekly theme <numero>            # Cambiar tema semanal
/avo weekly boss spawn                # Spawn manual del boss
/avo weekly boss despawn              # Despawn del boss
/avo weekly modifier set <nombre>     # Forzar modificador del día
/avo weekly zone create <tipo>        # Crear zona dinámica

# Minijuegos
/avo weekly minigame start            # Iniciar minijuego de la semana
/avo weekly minigame stop             # Detener minijuego
/avo weekly minigame leaderboard      # Ver ranking

# Desafíos globales
/avo weekly challenge progress        # Ver progreso del desafío
/avo weekly challenge complete        # Forzar completar desafío
```

### 👤 Comandos de Jugadores

```bash
# Información
/semanal                              # Ver info de la semana actual
/semanal info                         # Detalles completos
/semanal racha                        # Ver tu racha de días
/semanal checkin                      # Check-in diario

# Participación
/semanal boss                         # Ubicación del boss semanal
/semanal minigame join                # Unirse al minijuego
/semanal zone                         # Ver zonas dinámicas activas

# Leaderboards
/semanal top                          # Top jugadores de la semana
/semanal rewards                      # Ver recompensas disponibles
```

---

## 📈 PROGRESIÓN Y BALANCEO

### 💎 Economía de PS Semanal

**Ingresos promedio por jugador activo:**
- Semana temática: 180-300 PS
- Boss semanal: 280-400 PS
- Minijuego (participación): 100-300 PS
- Desafío global: 150 PS
- Racha diaria (7 días): 550 PS
- Micro eventos (3 por semana): 150 PS

**Total aproximado:** 1,410 - 2,080 PS por semana

### ⚖️ Balance
- Suficiente para mantener interés
- No rompe economía existente
- Recompensas escaladas por esfuerzo
- Jugadores casuales: ~600 PS/semana
- Jugadores dedicados: ~2,000 PS/semana

---

## 🚀 ROADMAP DE IMPLEMENTACIÓN

### FASE 1: Core System (Semana 1-2)
- ✅ Crear WeeklySystemManager
- ✅ Implementar rotación automática
- ✅ Sistema de notificaciones
- ✅ Comandos básicos

### FASE 2: Semanas Temáticas (Semana 3)
- ✅ Implementar 8 semanas temáticas
- ✅ Mecánicas de modificadores
- ✅ Sistema de objetivos semanales

### FASE 3: Bosses Semanales (Semana 4)
- ✅ Crear 6 bosses únicos
- ✅ Sistema de spawn
- ✅ Drops especiales

### FASE 4: Minijuegos (Semana 5)
- ✅ Implementar 7 minijuegos
- ✅ Sistema de leaderboards
- ✅ Arenas/ubicaciones

### FASE 5: Sistemas Adicionales (Semana 6)
- ✅ Desafíos globales
- ✅ Modificadores diarios
- ✅ Sistema de rachas

### FASE 6: Zonas y Eventos (Semana 7)
- ✅ Zonas dinámicas
- ✅ Micro eventos
- ✅ Calendario automático

### FASE 7: Polish y Testing (Semana 8)
- ✅ Balanceo final
- ✅ Bug fixes
- ✅ Optimización
- ✅ Documentación

---

## 🎊 CONCLUSIÓN

Este sistema crea un **ciclo perpetuo de contenido** que mantiene el servidor fresco sin necesidad de eventos grandes constantes. La combinación de sistemas rotativos garantiza que:

1. **Siempre hay algo nuevo** - Cada semana es diferente
2. **Múltiples estilos de juego recompensados** - Combat, building, exploring, farming
3. **Recompensa la dedicación** - Rachas, desafíos globales
4. **No es invasivo** - Se puede ignorar y jugar normal
5. **Escalable** - Fácil añadir más semanas/bosses/minijuegos

**Resultado esperado:**
- ↑ Retención de jugadores (+30%)
- ↑ Tiempo promedio de sesión (+40%)
- ↑ Actividad comunitaria (+50%)
- ↑ Diversión y dinamismo (inmensurable 🎉)
