# 🎮 SISTEMAS ADICIONALES - IDEAS DE EXPANSIÓN
## 💡 Nuevas Funcionalidades para el Servidor Apocalipsis

---

## 🎯 SISTEMA 1: ECONOMÍA Y TIENDAS DINÁMICAS

### 📋 Concepto
Sistema de economía con tiendas NPC que cambian precios según oferta/demanda y permite comercio entre jugadores.

### 💰 Características Principales

#### **Moneda Dual**
- **PS (Puntos de Supervivencia)** - Ya existente, para recompensas
- **Coins (Monedas)** - Nueva moneda para comercio diario
  - Conseguir coins: vender items, completar tareas, tiempo jugado
  - 1 PS = 100 Coins (conversión limitada)

#### **Tiendas NPC Dinámicas**
1. **Tienda del Explorador** 🗺️
   - Vende: Mapas, brújulas, elytras, cohetes
   - Compra: Mapas completados, items raros de exploración
   - Precios varían según stock

2. **Tienda del Herrero** ⚒️
   - Vende: Herramientas, armaduras, armas básicas
   - Compra: Minerales, lingotes, gemas
   - Descuentos según tu nivel de minería

3. **Tienda del Granjero** 🌾
   - Vende: Semillas, animales, pociones de crecimiento
   - Compra: Cultivos, carne, productos animales
   - Precios mejor si tienes habilidad de farming

4. **Mercado Negro** 🎭 (Solo disponible de noche)
   - Vende: Items raros, encantamientos prohibidos
   - Precios 3x más caros
   - Ubicación cambia cada día

#### **Sistema de Oferta/Demanda**
```yaml
economia:
  sistema_precio_dinamico:
    enabled: true
    
    # Si muchos venden diamantes, precio baja
    # Si nadie vende, precio sube
    factores:
      - stock_tienda          # Cantidad en inventario NPC
      - transacciones_dia     # Cuánto se vendió hoy
      - escasez_recursos      # Si es semana de escasez
      
    # Límites de fluctuación
    precio_minimo_porcentaje: 50    # Nunca baja de 50%
    precio_maximo_porcentaje: 200   # Nunca sube más de 200%
```

### 🎯 Comandos

**Jugadores:**
- `/shop` - Abrir menú de tiendas
- `/shop explorador` - Tienda específica
- `/balance` - Ver coins y PS
- `/sell` - Vender item en mano

**Admin:**
- `/economy set <jugador> <cantidad>` - Dar/quitar coins
- `/shop reload` - Recargar precios

---

## 🏰 SISTEMA 2: BASES PERSONALES Y PROTECCIÓN

### 📋 Concepto
Sistema de bases personales con territorios protegidos, mejoras progresivas y beneficios individuales.

### 🏠 Estructura de Bases

#### **Creación y Gestión**
```yaml
bases:
  costo_crear: 500          # 500 coins para crear base
  max_jugadores_compartir: 3  # Máximo 3 jugadores en una base
  max_territorios: 3        # Máximo 3 áreas protegidas
  
  permisos:
    - DUEÑO                 # Control total
    - CONFIANZA             # Puede construir/romper
    - INVITADO              # Solo puede entrar
```

#### **Niveles de Clan**
| Nivel | XP Requerida | Beneficios |
|-------|--------------|------------|
| 1 | 0 | +1 territorio, 10 miembros |
| 2 | 5,000 | +2 territorios, 15 miembros, Buff I |
| 3 | 15,000 | +3 territorios, 20 miembros, Buff II |
| 4 | 35,000 | +4 territorios, 25 miembros, Vault clan |
| 5 | 75,000 | +5 territorios, 30 miembros, Teletransporte |

#### **Niveles de Base**
| Nivel | XP Requerida | Beneficios |
|-------|--------------|------------|
| 1 | 0 | +1 territorio, 1 jugador invitado |
| 2 | 2,000 | +2 territorios, 2 invitados, Speed I en base |
| 3 | 5,000 | +3 territorios, 3 invitados, Regeneration I |
| 4 | 10,000 | Vault personal expandido, Teletransporte |
| 5 | 20,000 | Protección total, Totem respawn (1/día) |

#### **Sistema de Territorios**
- **Reclamar Chunks:** `/base claim` (costo: 300 coins/chunk)
- **Tipos de Áreas:**
  - 🏠 **Casa Principal** - Spawn protegido
  - ⛏️ **Zona de Recursos** - Bonus de minería +20%
  - 🌾 **Granja** - Crops crecen +30% más rápido
  - 📦 **Almacén** - Chest protegidos

#### **Buffs de Base**
**Nivel 1:**
- Protección contra explosiones
- Bloquear entrada de mobs hostiles

**Nivel 2:**
- Speed I dentro de tu base
- Hunger reduction -50%

**Nivel 3:**
- Regeneration I en área principal
- XP farming +10%

**Nivel 4:**
- Vault con 54 slots
- Teletransporte ilimitado a base

**Nivel 5:**
- `/base tp` - TP instantáneo desde cualquier lugar
- Revive automático en base (cooldown 24h)
- Protección contra griefing total

### 🎯 Comandos de Base

**Básicos:**
- `/base create <nombre>` - Crear base personal
- `/base trust <jugador>` - Dar acceso a jugador
- `/base untrust <jugador>` - Quitar acceso
- `/base info` - Ver información de tu base

**Gestión:**
- `/base claim` - Reclamar chunk actual
- `/base unclaim` - Liberar chunk
- `/base sethome` - Establecer punto de spawn
- `/base tp` - Teletransportarse a base
- `/base upgrade` - Mejorar nivel de base

**Permisos:**
- `/base trust build <jugador>` - Puede construir
- `/base trust container <jugador>` - Puede usar chests
- `/base trust all <jugador>` - Acceso completo

---

## 🏆 SISTEMA 3: LOGROS Y ACHIEVEMENTS

### 📋 Concepto
Sistema completo de logros con recompensas, títulos y estadísticas detalladas.

### 🎖️ Categorías de Logros

#### **SUPERVIVENCIA** 🛡️
| Logro | Descripción | Recompensa |
|-------|-------------|------------|
| **Primer Día** | Sobrevivir tu primer día | 50 PS + Título "Novato" |
| **Semana Completa** | Jugar 7 días consecutivos | 200 PS + Kit de inicio |
| **Superviviente** | Sobrevivir 10 desastres | 500 PS + Armadura especial |
| **Inmortal** | No morir en 30 días | 1000 PS + Totem of Undying |
| **Leyenda** | Nivel 100 alcanzado | 2000 PS + Título "Leyenda" |

#### **COMBATE** ⚔️
| Logro | Descripción | Recompensa |
|-------|-------------|------------|
| **Primera Sangre** | Matar tu primer mob | 20 PS |
| **Cazador** | Matar 1,000 mobs | 300 PS + Espada Sharpness IV |
| **Asesino en Serie** | Matar 5 mobs en 10 segundos | 150 PS + Speed II potion x5 |
| **Jefe Caído** | Derrotar un boss semanal | 400 PS + Item del boss |
| **Exterminador** | Matar 10,000 mobs | 1500 PS + Espada legendaria |

#### **EXPLORACIÓN** 🗺️
| Logro | Descripción | Recompensa |
|-------|-------------|------------|
| **Aventurero** | Explorar 100 chunks | 100 PS + Brújula |
| **Cartógrafo** | Explorar 1,000 chunks | 500 PS + Mapa completo |
| **Descubridor** | Encontrar 10 estructuras | 300 PS + Elytra |
| **Mundo Completo** | Visitar todos los biomas | 800 PS + Título "Explorador" |
| **Viajero Dimensional** | Visitar Nether y End | 400 PS + Ender Pearls x32 |

#### **CONSTRUCCIÓN** 🏗️
| Logro | Descripción | Recompensa |
|-------|-------------|------------|
| **Primera Casa** | Colocar 100 bloques | 50 PS |
| **Arquitecto** | Colocar 10,000 bloques | 400 PS + Shulker Box |
| **Ciudad Propia** | Construir en 50 chunks | 800 PS + WorldEdit |
| **Maestro Constructor** | Ganar concurso de construcción | 1000 PS + Bloques raros |

#### **ECONOMÍA** 💰
| Logro | Descripción | Recompensa |
|-------|-------------|------------|
| **Primer Venta** | Vender item en tienda | 30 PS |
| **Comerciante** | Realizar 100 trades | 200 PS + 500 Coins |
| **Magnate** | Acumular 10,000 coins | 500 PS + Título "Rico" |
| **Inversor** | Vender items por 50,000 coins total | 800 PS + Buff comercio permanente |

#### **PERSONAL** 🏠
| Logro | Descripción | Recompensa |
|-------|-------------|------------|
| **Primera Base** | Crear tu base personal | 50 PS |
| **Hogar Dulce Hogar** | Mejorar base a nivel 3 | 300 PS + Decoración especial |
| **Fortaleza** | Proteger 10 chunks | 500 PS + Protección mejorada |
| **Ermitaño** | Jugar 100 horas en solitario | 800 PS + Título "Lobo Solitario" |

#### **SECRETOS** 🔮
| Logro | Descripción | Recompensa |
|-------|-------------|------------|
| **???** | Encontrar el easter egg | 1000 PS + Item secreto |
| **Misterio Resuelto** | Completar quest oculta | 1500 PS + Título "Detective" |
| **El Elegido** | Evento especial aleatorio | 2000 PS + Rango temporal |

### 🏅 Sistema de Títulos

#### **Equipar Títulos:**
```
/logros titulos
```
- Menú GUI con todos tus títulos
- Click para equipar
- Se muestra en chat y tab

#### **Ejemplos de Títulos:**
- 🥇 **"El Primero"** - Primer jugador en lograr algo
- ⚔️ **"Guerrero"** - Matar 5,000 mobs
- 🏆 **"Campeón"** - Ganar 10 eventos
- 💀 **"Asesino"** - Hacer 100 kills PvP
- 🌟 **"Legendario"** - Completar todos los logros

### 📊 Estadísticas Detalladas

#### **Trackear:**
```
/stats [jugador]
```

**General:**
- Tiempo jugado total
- Dias desde registro
- Nivel actual

**Combate:**
- Mobs matados por tipo
- Daño total causado
- Muertes y kills
- Boss derrotados

**Exploración:**
- Chunks explorados
- Biomas descubiertos
- Estructuras encontradas
- Distancia caminada/volada

**Economía:**
- Coins ganados/gastados
- Trades realizados
- Items vendidos/comprados

**Social:**
- Tiempo en clan
- Guerras ganadas
- Eventos completados

### 🎯 Comandos

**Jugadores:**
- `/logros` - Ver todos los logros
- `/logros progreso` - Ver progreso actual
- `/logros titulos` - Equipar títulos
- `/stats` - Ver estadísticas
- `/leaderboard <categoria>` - Top jugadores

**Admin:**
- `/logros grant <jugador> <logro>` - Dar logro
- `/logros reset <jugador>` - Reset logros
- `/logros stats` - Ver estadísticas globales

---

## ⚗️ SISTEMA 5: CRAFTEOS CUSTOM Y ENCANTAMIENTOS

### 📋 Concepto
Añadir crafteos únicos y encantamientos custom que no existen en Minecraft vanilla.

### 🔨 Crafteos Custom

#### **Tier 1: Básicos Mejorados**

**Mochila Pequeña** 🎒
```
[Leather] [Leather] [Leather]
[Leather] [Chest]   [Leather]
[Leather] [Leather] [Leather]
```
- 9 slots portátiles
- Comando: `/mochila` (ya existe en el servidor)

**Botiquín de Emergencia** 🏥
```
[Paper]    [Glistering Melon] [Paper]
[Potion]   [Golden Apple]      [Potion]
[Iron]     [Iron]               [Iron]
```
- Cura 5 corazones instantáneos
- Regeneration II por 10s
- 1 uso

**Detector de Minerales** 🔍
```
[Redstone] [Diamond] [Redstone]
[Iron]     [Compass] [Iron]
[Redstone] [Iron]    [Redstone]
```
- Detecta minerales en 20 bloques
- 50 usos
- Cooldown 30s

#### **Tier 2: Herramientas Especiales**

**Pico de Velocidad** ⛏️
```
[Diamond] [Diamond] [Diamond]
[Air]     [Stick]   [Feather]
[Air]     [Stick]   [Feather]
```
- Efficiency VI
- Haste II al usar
- Durabilidad reducida 50%

**Espada Flamígera** 🔥
```
[Blaze Rod] [Blaze Rod] [Blaze Rod]
[Air]       [Diamond Sword] [Air]
[Air]       [Blaze Powder]  [Air]
```
- Fire Aspect III
- Prende fuego en área 3x3
- Inmune a lava

**Arco del Cazador** 🏹
```
[String]   [Diamond]    [String]
[Bow]      [Ender Eye]  [Spider Eye]
[String]   [Diamond]    [String]
```
- Power VI
- Flecha atraviesa mobs
- +30% headshot damage

#### **Tier 3: Items Legendarios**

**Corona del Superviviente** 👑
```
[Netherite] [Dragon Head] [Netherite]
[Gold]      [Nether Star] [Gold]
[Netherite] [Totem]       [Netherite]
```
- +4 corazones permanentes
- Absorption II constante
- Revive 1 vez al morir (cooldown 24h)

**Anillo de Teletransporte** 💍
```
[Ender Pearl] [Diamond] [Ender Pearl]
[Diamond]     [Chorus Fruit] [Diamond]
[Ender Pearl] [Nether Star] [Ender Pearl]
```
- Teletransporte a waypoint
- 10 usos
- Cooldown 5 minutos

**Báculo Elemental** ⚡
```
[Nether Star] [Air] [Air]
[Blaze Rod]   [Diamond Block] [Air]
[Air]         [Air] [Netherite Ingot]
```
- Click derecho: Lanza bola de fuego
- Shift + Click: Rayo
- Mana: 100 (regenera 1/segundo)

### ✨ Encantamientos Custom

#### **Para Herramientas** ⛏️

**Excavator** (Pico/Pala)
- Niveles: I-III
- Rompe área 3x3 (nivel 3)
- Costo XP: x3

**Lumberjack** (Hacha)
- Niveles: I-II
- Tala árbol completo
- Costo XP: x2

**Harvester** (Azada)
- Niveles: I-III
- Cosecha área 5x5 (nivel 3)
- Replanta automáticamente

**Smelting** (Pico)
- Niveles: I
- Items minados salen fundidos
- Incompatible con Fortune

#### **Para Armaduras** 🛡️

**Regeneration** (Pechera)
- Niveles: I-III
- Regeneración pasiva
- Nivel III = Regeneration I constante

**Speed** (Botas)
- Niveles: I-III
- Aumenta velocidad de movimiento
- Nivel III = Speed II

**Glowing** (Casco)
- Niveles: I
- Night Vision permanente
- Sin efecto de particulas

**Lightweight** (Todas las piezas)
- Niveles: I-II
- Reduce peso de armadura
- Sin reducción de velocidad con full armor

#### **Para Armas** ⚔️

**Lifesteal** (Espada)
- Niveles: I-III
- Recupera vida al atacar
- Nivel III = 20% del daño causado

**Beheading** (Espada/Hacha)
- Niveles: I-II
- Chance de dropear cabeza
- Nivel II = 10% chance

**Thunderstrike** (Espada)
- Niveles: I
- 5% chance de invocar rayo
- Solo funciona en tormenta

**Explosive** (Arco)
- Niveles: I-II
- Flechas explotan al impactar
- Nivel II = explosión mayor

#### **Universales** 🌟

**Curse of Greed**
- +50% drops de items
- -50% XP ganada
- Maldición permanente

**Curse of Wealth**
- +100% coins de mobs
- Items tienen 50% durabilidad
- Maldición permanente

**Soulbound**
- Item no se pierde al morir
- Se repara automáticamente
- Muy raro

### 🎯 Sistema de Crafteo

**Mesa de Crafteo Mejorada:**
```
/crafteo custom
```
- GUI con recetas custom
- Muestra materiales necesarios
- Preview del item resultante

**Libro de Recetas:**
- Se obtiene al nivel 10
- Desbloquear recetas con logros
- Actualización automática

---

## 🌾 SISTEMA 6: FARMING AVANZADO Y AGRICULTURA

### 📋 Concepto
Sistema de agricultura expandido con cultivos custom, temporadas y mecánicas realistas.

### 🌱 Cultivos Custom

#### **Nivel 1: Básicos**

**Arroz** 🍚
- Crece solo en agua
- 5 etapas de crecimiento
- Tiempo: 30 minutos
- Venta: 5 coins c/u

**Maíz** 🌽
- Crece en tierra fértil
- Planta de 2 bloques de altura
- Tiempo: 40 minutos
- Venta: 8 coins c/u

#### **Nivel 2: Intermedios**

**Café** ☕
- Requiere bioma Jungle
- Se planta en arbustos
- Tiempo: 60 minutos
- Craftear "Café" = +Speed I por 5 min
- Venta: 15 coins c/u

**Algodón** 🧶
- Crece en tierra seca (desierto mejor)
- Craftear en lana de colores
- Tiempo: 45 minutos
- Venta: 10 coins c/u

#### **Nivel 3: Avanzados**

**Uvas** 🍇
- Requiere enrejado (vallas)
- Se cultiva en racimos
- Tiempo: 90 minutos
- Craftear "Vino" = Regeneration II
- Venta: 25 coins c/u

**Hierbas Mágicas** ✨
- Requiere noche y luna llena
- Ingrediente para pociones custom
- Tiempo: 120 minutos
- Venta: 50 coins c/u

### 🌡️ Sistema de Temporadas

```yaml
temporadas:
  enabled: true
  duracion_dias: 28        # 28 días reales por temporada
  
  primavera:
    cultivos_bonus: ["trigo", "zanahoria", "arroz"]
    crecimiento: +30%
    lluvia: frecuente
    
  verano:
    cultivos_bonus: ["maiz", "algodon", "melon"]
    crecimiento: +50%
    sequia: true            # Requiere riego manual
    
  otoño:
    cultivos_bonus: ["papa", "calabaza", "uva"]
    crecimiento: normal
    cosecha: +20% cantidad
    
  invierno:
    cultivos_bonus: ["zanahoria", "remolacha"]
    crecimiento: -30%
    heladas: true           # Algunos cultivos mueren
```

### 💧 Sistema de Riego

**Aspersores Automáticos:**
```
[Iron] [Bucket] [Iron]
[Piston] [Dispenser] [Piston]
[Iron] [Redstone] [Iron]
```
- Riega área 9x9
- Requiere agua cercana
- Activa con redstone

**Canal de Riego:**
- Colocar water channels entre cultivos
- Distribuye agua eficientemente
- +10% crecimiento

### 🐄 Animales Mejorados

#### **Niveles de Animales**

**Vaca Nivel 1:**
- 1 leche cada 5 minutos
- 1-2 cuero al morir

**Vaca Nivel 5:**
- 3 leches cada 5 minutos
- Leather +50%
- Posibilidad de "Leche Dorada"

**Pollo Nivel 1:**
- 1 huevo cada 10 minutos

**Pollo Nivel 5:**
- 3 huevos cada 5 minutos
- Posibilidad de "Huevo de Oro"

#### **Sistema de Cría Selectiva**
- Criar animales da XP de farming
- Animales heredan stats de padres
- Posibilidad de mutaciones raras
- Animales brillantes (shiny) 1% chance

### 🏭 Máquinas de Farming

**Cosechadora Automática:**
```
[Iron Block] [Piston] [Iron Block]
[Hopper] [Diamond Hoe] [Hopper]
[Redstone] [Chest] [Redstone]
```
- Cosecha cultivos en área 5x5
- Automáticamente replanta
- Almacena en chest

**Fertilizadora:**
```
[Bone Meal] [Piston] [Bone Meal]
[Dispenser] [Hopper] [Dispenser]
[Iron] [Redstone] [Iron]
```
- Aplica bone meal automáticamente
- Radio: 7x7
- Consume 1 bone meal cada 30s

### 🎯 Comandos

**Jugadores:**
- `/farm stats` - Ver nivel de farming
- `/farm info` - Info de cultivo mirando
- `/farm season` - Ver temporada actual
- `/farm sell` - Vender inventario de cultivos

**Admin:**
- `/farm season set <temporada>` - Cambiar temporada
- `/farm growth <multiplicador>` - Ajustar velocidad
- `/farm reload` - Recargar configuración

---

## 🗺️ SISTEMA 7: DUNGEONS Y MAZMORRAS

### 📋 Concepto
Mazmorras procedurales con jefes, trampas y loot único.

### 🏛️ Tipos de Dungeons

#### **1. Mina Abandonada Profunda** ⛏️
- **Ubicación:** Y < 0, bajo tierra
- **Tamaño:** 50x50 bloques, 3 niveles
- **Dificultad:** ⭐⭐
- **Mobs:** Zombies mineros, esqueletos, arañas venenosas
- **Boss:** Espíritu Minero (Wither Skeleton gigante)
- **Loot:** 
  - Minerales raros (diamantes, ancient debris)
  - Pico encantado "Excavator"
  - Mapa del tesoro
- **Trampas:** Pozos, lava oculta, paredes falsas

#### **2. Templo Olvidado** 🏺
- **Ubicación:** Desierto/Jungle
- **Tamaño:** 40x40, 2 niveles
- **Dificultad:** ⭐⭐⭐
- **Mobs:** Momias, guardianes de piedra
- **Boss:** Faraón Maldito
- **Loot:**
  - Oro y esmeraldas
  - Armadura dorada encantada
  - Reliquia antigua (trinket)
- **Trampas:** Flechas, arena movediza, puertas secretas

#### **3. Torre del Mago** 🗼
- **Ubicación:** Montañas altas
- **Tamaño:** 20x20, 10 pisos verticales
- **Dificultad:** ⭐⭐⭐⭐
- **Mobs:** Esqueletos con arcos, brujas, blazes
- **Boss:** Archimago Corrupto
- **Loot:**
  - Báculo mágico
  - Libros de encantamientos nivel máximo
  - Pociones raras
- **Trampas:** Levitación, fuego, teleports aleatorios

#### **4. Fortaleza del Wither** 💀
- **Ubicación:** Nether Wastes
- **Tamaño:** 60x60, multi-nivel
- **Dificultad:** ⭐⭐⭐⭐⭐
- **Mobs:** Wither skeletons elite, blazes, ghasts
- **Boss:** Wither Rey (Wither mejorado)
- **Loot:**
  - Nether Stars
  - Armadura de Netherite
  - Beacon
- **Trampas:** Lava, explosiones, wither effect

#### **5. Cripta del Fin** 🌌
- **Ubicación:** The End
- **Tamaño:** 80x80, laberinto
- **Dificultad:** ⭐⭐⭐⭐⭐⭐
- **Mobs:** Enderman elite, Shulkers, End mites gigantes
- **Boss:** Dragón Ancestral
- **Loot:**
  - Elytra mejorada
  - Dragon Egg decorativo
  - Armadura del Vacío (custom)
- **Trampas:** Void, teletransportes, levitación mortal

### 🎲 Sistema de Generación

```yaml
dungeons:
  spawn_natural: true
  distancia_minima: 1000     # Entre dungeons
  max_por_mundo: 10
  
  generacion:
    procedural: true           # Aleatorio cada vez
    guardar_completados: true  # No regenerar si ya completado
    reset_dias: 7              # Regenerar cada 7 días
```

### 🔑 Sistema de Llaves

**Conseguir Llaves:**
- Matar boss semanal: Key Fragment x3
- Craftear 9 fragmentos = 1 llave
- Comprar en tienda (muy caro)

**Tipos de Llaves:**
- 🗝️ **Llave de Bronce** - Dungeons Tier 1-2
- 🔑 **Llave de Plata** - Dungeons Tier 3-4
- 💎 **Llave de Oro** - Dungeons Tier 5-6

### � Comandos

**Jugadores:**
- `/dungeon list` - Ver dungeons cercanos
- `/dungeon info` - Info del dungeon actual
- `/dungeon enter` - Entrar a dungeon (solo)
- `/dungeon leave` - Salir de dungeon
- `/dungeon progress` - Ver tu progreso

**Admin:**
- `/dungeon create <tipo>` - Generar dungeon
- `/dungeon reset <id>` - Resetear dungeon
- `/dungeon tp <id>` - Teletransporte a dungeon
- `/dungeon difficulty <1-5>` - Ajustar dificultad

---

## 🎁 IMPLEMENTACIÓN Y PRIORIDADES

### 📊 Complejidad vs Impacto (Comunidad Pequeña)

| Sistema | Complejidad | Impacto | Prioridad | Jugadores Min |
|---------|-------------|---------|-----------|---------------|
| Economía y Tiendas | Media | Alto | 🔴 Alta | 1 |
| Bases Personales | Media | Alto | 🔴 Alta | 1 |
| Logros | Baja | Medio | 🟢 Baja | 1 |
| Crafteos Custom | Media | Medio | 🟡 Media | 1 |
| Farming Avanzado | Media | Medio | 🟡 Media | 1 |
| Dungeons Solo | Alta | Muy Alto | 🔴 Alta | 1 |

### 🚀 Roadmap Sugerido (1-5 Jugadores)

**FASE 1 (1-2 meses):**
- ✅ Sistema de Economía básico
- ✅ Tiendas NPC
- ✅ Logros fundamentales (individuales)

**FASE 2 (2-3 meses):**
- ✅ Sistema de Bases Personales
- ✅ Territorios y protección
- ✅ Sistema de permisos

**FASE 3 (3-4 meses):**
- ✅ Crafteos custom (Tier 1-2)
- ✅ Encantamientos custom básicos
- ✅ Farming mejorado

**FASE 4 (4-6 meses):**
- ✅ Dungeons solo (2-3 tipos iniciales)
- ✅ Dificultad adaptativa
- ✅ Sistema de recompensas escalado

**FASE 5 (6+ meses):**
- ✅ Expansión de todos los sistemas
- ✅ Balance para jugador solitario
- ✅ Contenido endgame individual

---

## 📝 NOTAS FINALES

### ✅ Diseño para Comunidad Pequeña (1-5 Jugadores)

Estos sistemas están **optimizados** para:
- ✅ Jugabilidad en solitario completa
- ✅ Cooperación opcional (no obligatoria)
- ✅ Progresión individual
- ✅ Sin dependencia de otros jugadores
- ✅ Escalabilidad automática

### 🎮 Características Especiales

**Para 1 Jugador:**
- Todos los sistemas funcionan completamente solo
- Dungeons con dificultad ajustada
- Economía balanceada para progreso individual
- Logros alcanzables sin interacción social

**Para 2-3 Jugadores:**
- Bases compartidas opcionales (max 3 jugadores)
- Beneficios de cooperación en dungeons
- Comercio directo entre jugadores
- Dungeons con loot mejorado en grupo pequeño

**Para 4-5 Jugadores:**
- Eventos comunitarios opcionales
- Competencia amistosa en leaderboards
- Intercambio de recursos facilitado
- Posibilidad de mini-eventos cooperativos

**Compatibilidad:**
- Compatible con sistema de PS existente
- Integrable con eventos semanales
- Complementa sistema de rangos
- Se puede combinar con desastres
- **NO requiere población grande**

**Economía Balanceada:**
- No rompe sistema de misiones actual
- Añade objetivos de largo plazo
- Recompensa dedicación individual
- Comercio con NPCs (no depende de jugadores)
