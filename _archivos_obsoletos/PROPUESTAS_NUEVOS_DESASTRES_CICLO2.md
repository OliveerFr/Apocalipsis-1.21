# 🌊 PROPUESTAS DE RECONSTRUCCIÓN - NUEVOS DESASTRES PARA CICLO 2

**Fecha:** 27 Enero 2026  
**Versión:** 1.22.57  
**Estado:** Propuestas para implementación

---

## 📊 RESUMEN EJECUTIVO

### Desastres Actuales (Ciclo 1)
- **🌪️ Huracán:** Viento, empuje, objetos voladores, inundación
- **🔥 Lluvia de Fuego:** Meteoritos, calor extremo, ceniza, transformación terreno
- **⛰️ Terremoto:** Grietas, derrumbes, réplicas sísmicas, ondas

### Objetivo de la Reconstrucción
Renovar completamente los desastres para el **Ciclo 2** manteniendo:
- ✅ Sistema de fases (INICIO → PICO → DECLIVE)
- ✅ Mecánicas de protección/contramedidas
- ✅ Escalabilidad por intensidad
- ✅ Balance de dificultad

---

## 📋 OPCIÓN 1: DESASTRES NATURALES ELEMENTALES ⭐ RECOMENDADA
*Tema: Elementos en conflicto*

### 1. ❄️ **TORMENTA GLACIAL** (Reemplazo de Huracán)

**Concepto:** Frío extremo que congela el mundo progresivamente

#### Mecánicas Principales

**🧊 Congelación Progresiva**
- Agua → Hielo (radio 3 bloques por jugador)
- Lluvia → Nieve
- Bloques mojados → Hielo compactado
- Velocidad: 1 bloque cada 5s en PICO

**❄️ Hipotermia**
- Daño: 0.5 corazones cada 10s sin refugio
- Acumulativo: +0.25 daño cada 30s
- Indicador: Actionbar con "temperatura corporal"
- Particle: SNOWFLAKE azul intenso

**🌨️ Ráfagas Heladas**
- Frecuencia: Cada 20s
- Efectos: Slowness II (6s) + Mining Fatigue I (4s)
- Advertencia: Sonido AMBIENT_CAVE + particles 3s antes
- Intensidad: x2.5 durante ráfaga

**💎 Cristales de Hielo**
- Proyectiles que caen del cielo
- Daño: 1 corazón al impactar
- Knockback: Medio-Alto
- Efecto: Freeze (nuevo en 1.21) por 3s
- Frecuencia: 3-6 cristales cada 15s

**🧱 Estalactitas Caídas**
- FallingBlock de POINTED_DRIPSTONE
- Advertencia: Partículas ICE_BURST en columna 2s antes
- Daño: 2-4 corazones según altura
- Física: Gravity real, se clavan en suelo

**🌫️ Niebla Congelante**
- Blindness cada 40s durante 5s
- Particles SNOWFLAKE masivos (300 particles)
- Sonido: ENTITY_PLAYER_BREATH
- Solo en fase PICO

#### Protección y Contramedidas

**🔥 Zonas Calientes**
- Estar a <4 bloques de:
  - Fogatas (campfire)
  - Lava
  - Antorchas (5+)
  - Bloques de magma
  - Netherrack encendido
- Reducción: 80% daño hipotermia

**🛡️ Armadura**
- Cuero: -30% daño de frío
- Netherite: -50% daño de frío
- Frost Walker: Inmune a hielo bajo pies

**🏠 Refugio**
- Techo sólido: -40% efectos
- Paredes cerradas (4 lados): -60% efectos

#### Sistema de Fases

**INICIO (0-25% duración):**
- Nieve ligera
- Slowness I (-20% velocidad)
- 1 cristal cada 25s
- Hipotermia: 0.5 corazones/15s

**PICO (25-75% duración):**
- Ventisca intensa
- Slowness II (-40% velocidad)
- 5 cristales cada 10s
- Estalactitas activas
- Hipotermia: 1 corazón/8s
- Niebla congelante activa

**DECLIVE (75-100% duración):**
- Deshielo gradual
- Slowness I (-20% velocidad)
- 2 cristales cada 20s
- Hipotermia: 0.5 corazones/20s
- Charcos de agua se forman

#### Configuración YAML Propuesta

```yaml
tormenta_glacial:
  duracion_segundos: 75
  
  congelacion:
    enabled: true
    radio: 3
    intervalo_ticks: 100  # Cada 5s
    agua_a_hielo: true
    bloques_mojados: true
  
  hipotermia:
    enabled: true
    damage_base: 0.5
    intervalo_base_ticks: 200  # 10s
    incremento_acumulativo: 0.25
    intervalo_incremento_ticks: 600  # 30s
  
  rafagas_heladas:
    enabled: true
    intervalo_ticks: 400  # 20s
    slowness_level: 2
    slowness_duration_ticks: 120  # 6s
    mining_fatigue_level: 1
    mining_fatigue_duration_ticks: 80  # 4s
    multiplicador_intensidad: 2.5
  
  cristales_hielo:
    enabled: true
    intervalo_ticks: 300  # 15s
    min_cristales: 3
    max_cristales: 6
    damage: 1.0
    freeze_duration_ticks: 60  # 3s
  
  estalactitas:
    enabled: true
    advertencia_ticks: 40  # 2s
    damage_min: 2.0
    damage_max: 4.0
    chance_por_jugador: 0.15
  
  niebla:
    enabled: true
    solo_en_pico: true
    intervalo_ticks: 800  # 40s
    duracion_ticks: 100  # 5s
    particle_count: 300
  
  proteccion:
    zonas_calientes:
      enabled: true
      radio_deteccion: 4
      reduccion_damage: 0.80
      fuentes:
        - "CAMPFIRE"
        - "SOUL_CAMPFIRE"
        - "LAVA"
        - "MAGMA_BLOCK"
        - "FIRE"
    
    armadura:
      cuero_reduccion: 0.30
      netherite_reduccion: 0.50
    
    refugio:
      techo_reduccion: 0.40
      paredes_completas_reduccion: 0.60
  
  fases:
    enabled: true
    # INICIO: 0-25%
    # PICO: 25-75%
    # DECLIVE: 75-100%
```

---

### 2. ⚡ **TORMENTA ELÉCTRICA CAÓTICA** (Reemplazo de Lluvia de Fuego)

**Concepto:** Rayos impredecibles que electrifican el terreno

#### Mecánicas Principales

**⚡ Rayos Dirigidos**
- Target: Jugadores sin techo
- Advertencia: 3s antes → columna ELECTRIC_SPARK + sonido
- Daño: 3 corazones directos
- Fuego: Incendia bloques circundantes
- Frecuencia: 1 rayo cada 12s (INICIO) → 4 cada 8s (PICO)

**🔗 Cadenas Eléctricas**
- Un rayo puede saltar entre jugadores cercanos
- Radio: 5 bloques por salto
- Máximo: 3 saltos
- Daño por salto: -1 corazón (3 → 2 → 1)
- Visual: Partículas ELECTRIC_SPARK conectando jugadores

**🧲 Sobrecarga Eléctrica**
- Items metálicos atraen rayos:
  - Armadura de hierro/oro/netherite
  - Herramientas metálicas en mano
  - Iron golems cercanos
- Multiplicador atracción: x3 probabilidad
- Advertencia: Items brillan (GLOW effect 2s antes)

**⚡ Zonas Ionizadas**
- Áreas del suelo quedan cargadas tras impacto
- Radio: 4 bloques
- Duración: 15 segundos
- Efecto: 0.5 corazones/segundo al pisar
- Visual: Partículas ELECTRIC_SPARK + bloques GLOW

**📡 EMP Pulses**
- Frecuencia: Cada 45s en PICO
- Radio: 12 bloques
- Efectos:
  - Elytras dejan de funcionar por 10s
  - Tridents con Riptide no lanzan
  - Crossbows fallan disparos
- Advertencia: Sonido BLOCK_BEACON_DEACTIVATE

**🔌 Cortocircuito**
- Redstone se desactiva temporalmente
- Pistons, doors, hoppers fallan
- Duración: 8 segundos
- Radio desde rayo: 6 bloques

#### Protección y Contramedidas

**⚡ Lightning Rods**
- Desvían rayos en radio de 16 bloques
- Prioridad: Rod > Metal > Jugador sin techo
- Stack: Múltiples rods = más protección

**🛡️ Aislamiento**
- Bloques no conductores bajo pies:
  - Madera (todos los tipos)
  - Lana (todos los colores)
  - Vidrio
- Reducción: 70% probabilidad rayo

**💧 Vulnerabilidad en Agua**
- Estar en agua = x2 daño
- Cadenas eléctricas = radio x2 en agua
- Zonas ionizadas duran x2

**🏠 Techo Protector**
- Techo sólido (no cristal): Inmunidad a rayos directos
- Mínimo 3 bloques de altura

#### Sistema de Fases

**INICIO (0-30% duración):**
- 1 rayo cada 12s
- Sin cadenas eléctricas
- Zonas ionizadas: radio 2, duración 8s
- Sin EMP

**PICO (30-70% duración):**
- 4 rayos cada 8s
- Cadenas eléctricas activas
- Zonas ionizadas: radio 4, duración 15s
- EMP cada 45s
- Cortocircuito activo

**DECLIVE (70-100% duración):**
- 2 rayos cada 15s
- Cadenas reducidas (max 2 saltos)
- Zonas ionizadas se disipan rápido
- Sin EMP

#### Configuración YAML Propuesta

```yaml
tormenta_electrica:
  duracion_segundos: 70
  
  rayos_dirigidos:
    enabled: true
    advertencia_ticks: 60  # 3s
    damage: 3.0
    incendiar_bloques: true
    priorizar_sin_techo: true
  
  cadenas_electricas:
    enabled: true
    radio_salto: 5
    max_saltos: 3
    damage_base: 3.0
    reduccion_por_salto: 1.0
  
  sobrecarga_electrica:
    enabled: true
    multiplicador_atraccion: 3.0
    items_metalicos:
      - "IRON_HELMET"
      - "IRON_CHESTPLATE"
      - "IRON_LEGGINGS"
      - "IRON_BOOTS"
      - "GOLDEN_HELMET"
      - "GOLDEN_CHESTPLATE"
      - "GOLDEN_LEGGINGS"
      - "GOLDEN_BOOTS"
      - "NETHERITE_HELMET"
      - "NETHERITE_CHESTPLATE"
      - "NETHERITE_LEGGINGS"
      - "NETHERITE_BOOTS"
      - "IRON_SWORD"
      - "IRON_AXE"
      - "IRON_PICKAXE"
  
  zonas_ionizadas:
    enabled: true
    radio: 4
    duracion_ticks: 300  # 15s
    damage_por_segundo: 0.5
    particle_density: 40
  
  emp_pulse:
    enabled: true
    solo_en_pico: true
    intervalo_ticks: 900  # 45s
    radio: 12
    duracion_ticks: 200  # 10s
    afecta_elytras: true
    afecta_riptide: true
    afecta_crossbows: true
  
  cortocircuito:
    enabled: true
    radio_desde_rayo: 6
    duracion_ticks: 160  # 8s
  
  proteccion:
    lightning_rods:
      enabled: true
      radio_proteccion: 16
      prioridad_sobre_jugador: true
    
    bloques_aislantes:
      enabled: true
      reduccion_probabilidad: 0.70
      materiales:
        - "OAK_PLANKS"
        - "SPRUCE_PLANKS"
        - "BIRCH_PLANKS"
        - "JUNGLE_PLANKS"
        - "ACACIA_PLANKS"
        - "DARK_OAK_PLANKS"
        - "CRIMSON_PLANKS"
        - "WARPED_PLANKS"
        - "WHITE_WOOL"
        - "GLASS"
    
    agua_vulnerabilidad:
      multiplicador_damage: 2.0
      multiplicador_radio_cadenas: 2.0
      multiplicador_duracion_zonas: 2.0
    
    techo_solido:
      inmunidad_completa: true
      altura_minima: 3
  
  fases:
    enabled: true
```

---

### 3. 🌋 **ERUPCIÓN VOLCÁNICA** (Reemplazo de Terremoto)

**Concepto:** La tierra expulsa lava y rocas incandescentes

#### Mecánicas Principales

**🌊 Géiseres de Lava**
- Columnas de lava surgen del suelo
- Altura: 5-10 bloques
- Advertencia: Temblor + partículas LAVA 3s antes
- Duración: 8 segundos
- Daño: Fire + 2 corazones al tocar
- Frecuencia: 1 géiser cada 20s → 5 cada 10s (PICO)

**🪨 Rocas Volcánicas**
- FallingBlock de MAGMA_BLOCK
- Trayectoria parabólica
- Explosión: 1.5 power al impactar
- Fuego: Deja fire blocks en radio 2
- Frecuencia: 8-12 rocas cada 12s

**🔥 Grietas Magmáticas**
- El suelo se abre revelando lava
- Longitud: 4-8 bloques
- Profundidad: 2-3 bloques
- Lava permanente en fondo
- Advertencia: Cracks con particles SMOKE 2s antes
- Máximo activas: 6 grietas

**💨 Ceniza Volcánica**
- Nausea II por 6s
- Blindness I por 4s
- Particles ASH masivos (500 particles)
- Frecuencia: Cada 30s
- Radio: 15 bloques desde erupción

**💣 Bombas de Magma**
- Proyectiles grandes (MAGMA_CUBE spawn temporal)
- Trayectoria alta y lenta
- Advertencia: Sonido ENTITY_BLAZE_SHOOT
- Explosión: 2.5 power
- Fuego persistente: 20s en radio 3

**⚠️ Temblores Previos**
- 5s antes de cada géiser
- Vibración jugadores (velocity Y aleatorio)
- Sonido: ENTITY_WARDEN_DIG
- Bloques particles VIBRATION

#### Protección y Contramedidas

**🛡️ Bloques Resistentes**
- Obsidiana: Inmune a grietas
- Piedra (todos los tipos): -60% probabilidad grieta
- Bloques densos reducen explosiones

**💧 Water Buckets**
- Solidificar lava emergente
- Lava → Obsidiana/Cobblestone
- Géiseres pueden romper agua (20% chance)

**🏔️ Altura Elevada**
- Torres +10 bloques: Inmunes a grietas
- Reducción daño rocas: -40%
- Vulnerable a bombas aéreas

**❄️ Bloques de Hielo**
- Blue ice cerca: Enfría géiseres
- Radio efecto: 3 bloques
- Reducción: 50% daño y duración

#### Sistema de Fases

**INICIO (0-30% duración):**
- 1 géiser cada 20s
- 6 rocas cada 15s
- Grietas: 1 cada 25s
- Sin ceniza volcánica

**PICO (30-70% duración):**
- 5 géiseres cada 10s
- 12 rocas cada 8s
- Grietas: 2 cada 12s
- Bombas de magma activas
- Ceniza volcánica cada 30s
- Temblores constantes

**DECLIVE (70-100% duración):**
- 2 géiseres cada 18s
- 8 rocas cada 12s
- Lava se solidifica en basalto
- Sin nuevas grietas
- Ceniza residual

#### Configuración YAML Propuesta

```yaml
erupcion_volcanica:
  duracion_segundos: 80
  
  geiseres_lava:
    enabled: true
    altura_min: 5
    altura_max: 10
    advertencia_ticks: 60  # 3s
    duracion_ticks: 160  # 8s
    damage: 2.0
    aplicar_fire: true
  
  rocas_volcanicas:
    enabled: true
    min_rocas: 8
    max_rocas: 12
    intervalo_ticks: 240  # 12s
    explosion_power: 1.5
    radio_fuego: 2
    material: "MAGMA_BLOCK"
  
  grietas_magmaticas:
    enabled: true
    longitud_min: 4
    longitud_max: 8
    profundidad_min: 2
    profundidad_max: 3
    max_activas: 6
    advertencia_ticks: 40  # 2s
    lava_permanente: true
  
  ceniza_volcanica:
    enabled: true
    intervalo_ticks: 600  # 30s
    radio: 15
    nausea_level: 2
    nausea_duration_ticks: 120  # 6s
    blindness_level: 1
    blindness_duration_ticks: 80  # 4s
    particle_count: 500
  
  bombas_magma:
    enabled: true
    solo_en_pico: true
    intervalo_ticks: 350  # 17.5s
    explosion_power: 2.5
    radio_fuego: 3
    duracion_fuego_ticks: 400  # 20s
  
  temblores_previos:
    enabled: true
    duracion_ticks: 100  # 5s
    intensidad_vibration: 0.15
  
  proteccion:
    bloques_resistentes:
      enabled: true
      obsidiana_inmune: true
      piedra_reduccion: 0.60
      materiales_resistentes:
        - "OBSIDIAN"
        - "CRYING_OBSIDIAN"
        - "STONE"
        - "COBBLESTONE"
        - "DEEPSLATE"
        - "BLACKSTONE"
    
    water_buckets:
      solidificar_lava: true
      geiser_rompe_agua_chance: 0.20
    
    altura_elevada:
      altura_minima: 10
      inmunidad_grietas: true
      reduccion_damage_rocas: 0.40
    
    bloques_hielo:
      enabled: true
      radio_efecto: 3
      reduccion_geiser: 0.50
      materiales:
        - "BLUE_ICE"
        - "PACKED_ICE"
  
  fases:
    enabled: true
```

---

## 📋 OPCIÓN 2: DESASTRES DIMENSIONALES
*Tema: Corrupción de otras dimensiones*

### 1. 🌀 **INVASIÓN DEL VOID**

**Concepto:** El vacío del End filtra hacia el Overworld

#### Mecánicas Principales
- **Agujeros del vacío:** Portales temporales que succionan jugadores (pullback velocity)
- **Corruption blocks:** Grass → End Stone, Stone → Purpur gradualmente
- **Endermites invasores:** Spawns masivos + teleport random players cada 30s
- **Levitación errática:** Levitation random + caídas letales cuando expira
- **Visión distorsionada:** Nausea III + shader distortion effects
- **Partículas END_ROD/PORTAL** masivas creando vórtices visuales

#### Protección
- Bloques del overworld puros (grass, oak wood) resisten corrupción
- Ender pearls para escapar de succión
- Shields bloquean teleports

---

### 2. 🔥 **INVASIÓN DEL NETHER**

**Concepto:** El Nether se derrama hacia el mundo normal

#### Mecánicas Principales
- **Portales temporales:** Spawns de Ghasts/Blazes/Piglins agresivos
- **Transformación terrain:** Grass → Netherrack, Water → Lava, Trees → Crimson
- **Soul Fire spread:** Fuego del alma inextinguible se propaga
- **Calor infernal:** Fire Resistance requerido o 1 corazón/3s
- **Wither effect zones:** Áreas que aplican Wither II
- **Basalto explosivo:** FallingBlocks que explotan al caer

#### Protección
- Water buckets constantes para enfriar
- Crying obsidian bloquea transformación
- Fire Resistance potions esenciales

---

### 3. 👻 **DIMENSIÓN FANTASMA**

**Concepto:** Mundo espectral se superpone al real

#### Mecánicas Principales
- **Fases espectrales:** Jugadores se vuelven Invisibility II (ven sus items flotar)
- **Bloques fantasma:** Se vuelven no-sólidos random → caídas inesperadas
- **Mobs espectrales:** Spawns masivos de Vex + Phantoms
- **Echo attacks:** Daño aparece 3s después del impacto real (confuso)
- **Soul theft:** XP draina -1 level cada 15s
- **Dimension overlap:** Estructuras espectrales aparecen temporalmente

#### Protección
- Bloques densos (deepslate, ancient debris) no se vuelven fantasma
- Soul Torches revelan bloques fantasma (particles)
- Spectral arrows marcan enemigos reales

---

## 📋 OPCIÓN 3: DESASTRES BIOLÓGICOS
*Tema: Naturaleza corrupta*

### 1. 🦠 **PLAGA TÓXICA**

#### Mecánicas
- **Nubes venenosas:** AOE clouds que se expanden (Poison III + Weakness)
- **Bloques contaminados:** Grass → Podzol verde, causan Poison al pisar
- **Spreads virales:** Vines corruptas crecen 3 bloques/segundo
- **Mutaciones:** Cows → aggro, Pigs → explotan, Sheep → lanzan thorns
- **Spore bursts:** Explosiones que no dañan bloques pero aplican efectos
- **Toxic rain:** Lluvia verde que infecta todo lo que toca

#### Protección
- Milk buckets para curar poison
- Hazmat armor (leather + specific enchants)
- Fire purifica bloques contaminados

---

### 2. 🌿 **INVASIÓN VEGETAL**

#### Mecánicas
- **Enredaderas vivas:** Vines que atrapan (Slowness IV + Mining Fatigue III)
- **Esporas:** Particles verdes masivas que aplican random debuffs
- **Bosque instantáneo:** Oak trees crecen en segundos, bloquean movimiento
- **Raíces subterráneas:** Te arrastran bajo tierra (teleport Y-5)
- **Pollen clouds:** Ceguera amarilla + Nausea
- **Thorny growth:** Sweet Berry Bushes spawn masivos

#### Protección
- Shears para cortar enredaderas
- Fire/Lava destruye vegetación
- Stone platforms inmunes a raíces

---

### 3. 🕷️ **ENJAMBRE MUTANTE**

#### Mecánicas
- **Swarms:** Nubes de 50+ Silverfish/Endermites
- **Nidos temporales:** Monster spawners que duran 45s
- **Mordeduras acumulativas:** Cada hit aplica Weakness I (stack hasta V)
- **Túneles instantáneos:** Bloques desaparecen formando cuevas
- **Queen spawn:** Boss silverfish gigante (10 HP, spawn mini-swarms)
- **Infested blocks:** Bloques random se vuelven infested

#### Protección
- AOE weapons (sweeping edge, multishot)
- Potions of Harming (kill swarms fast)
- Stone bricks resisten infestación

---

## 🎯 RECOMENDACIÓN FINAL

### **OPCIÓN 1 - DESASTRES ELEMENTALES** ⭐

**Ventajas:**
1. ✅ **Temática clara y universal:** Hielo, Electricidad, Lava son conceptos intuitivos
2. ✅ **Contramedidas obvias:** Fuego contra frío, lightning rods, agua contra lava
3. ✅ **Escalabilidad perfecta:** Fácil ajustar intensidad sin romper balance
4. ✅ **Efectos visuales impactantes:** Cada desastre tiene identidad visual única
5. ✅ **Compatibilidad con sistema actual:** Usa mismas estructuras de código
6. ✅ **Variedad táctica:** Cada desastre requiere estrategia diferente

**Desventajas:**
- Pueden sentirse "predecibles" tras múltiples ciclos
- Menos "únicos" comparado con opciones dimensionales

---

### Tabla Comparativa de Opciones

| Aspecto | Opción 1: Elementales | Opción 2: Dimensionales | Opción 3: Biológicos |
|---------|---------------------|----------------------|---------------------|
| **Dificultad implementación** | Media | Alta | Media-Alta |
| **Rendimiento (TPS)** | Bueno | Moderado (spawns) | Malo (entities) |
| **Claridad mecánicas** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ |
| **Originalidad** | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Balance facilidad** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ |
| **Efectos visuales** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| **Compatibilidad código** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ |
| **Diversión jugadores** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |

---

## 📊 PLAN DE IMPLEMENTACIÓN (OPCIÓN 1)

### Fase 1: Configuración
1. Crear configuraciones YAML para los 3 nuevos desastres
2. Añadir sistema de toggle (viejos vs nuevos desastres)
3. Mantener desastres viejos como fallback

### Fase 2: Clases Base
1. `TormentaGlacial.java` - Extiende DisasterBase
2. `TormentaElectrica.java` - Extiende DisasterBase
3. `ErupcionVolcanica.java` - Extiende DisasterBase

### Fase 3: Testing
1. Test individual de cada desastre
2. Test de transiciones entre desastres
3. Balance de dificultad
4. Optimización de rendimiento

### Fase 4: Deployment
1. Desactivar desastres viejos en config
2. Activar desastres nuevos como default
3. Mantener viejos disponibles vía comando

---

## 🔧 CONFIGURACIÓN DE MIGRACIÓN

```yaml
# desastres.yml
ciclo:
  usar_desastres_nuevos: true  # Toggle para Ciclo 2
  
desastres_ciclo_1:  # Desactivados pero disponibles
  weights:
    huracan: 0
    lluvia_fuego: 0
    terremoto: 0

desastres_ciclo_2:  # Activos
  weights:
    tormenta_glacial: 1
    tormenta_electrica: 1
    erupcion_volcanica: 1
```

---

## 📈 MÉTRICAS DE ÉXITO

**Objetivos medibles:**
- ✅ 90%+ jugadores completan al menos 1 desastre nuevo en primera semana
- ✅ Tiempo promedio supervivencia: 60-75% de duración total
- ✅ Balance: Ningún desastre tiene >40% mortality rate
- ✅ Rendimiento: TPS mantiene >18 durante desastres
- ✅ Feedback positivo: >70% jugadores prefieren nuevos vs viejos

---

**Documento creado:** 27 Enero 2026  
**Autor:** Sistema de Ciclos - Apocalipsis  
**Estado:** ✅ Listo para implementación
