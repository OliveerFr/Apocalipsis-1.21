# Eco de Brasas - Sistema de Grietas Mejorado

## ✨ Mejoras Implementadas

### 1. **Grietas Flotantes Visuales**

#### Antes:
- Solo partículas en el suelo
- Sin entidad física
- No interactivo

#### Ahora:
- **ArmorStand invisible flotante** a 2 bloques de altura
- **Nombre visible**: `§c§l⚠ GRIETA DE VAPOR §c§l⚠`
- **Tag identificador**: `eco_grieta` (para detección)
- **Efectos continuos cada 0.5 segundos:**
  - 20 partículas FLAME flotando
  - 5 partículas LAVA
  - 10 partículas SMOKE hacia arriba
  - Sonido FIRE_AMBIENT cada 2 segundos
  - Sonido LAVA_POP ocasional

### 2. **Sistema de Cierre por Golpes**

#### Mecánica:
- Cada grieta tiene **100 HP**
- Los jugadores **golpean o hacen clic derecho** en el ArmorStand
- Cada golpe reduce 1 HP
- La barra de vida se muestra en el nombre: `§c§l⚠ GRIETA §c▓▓▓▓▓▓▓░░░ §c70%`
- Al llegar a 0 HP → **Grieta cerrada**

#### Feedback Visual:
- **Por golpe:**
  - 10 partículas FLAME
  - Sonido BLAZE_HURT
  - Mensaje al jugador: `§7[§cGrieta§7] §e70% §7restante`
  
- **Al cerrar:**
  - 5 EXPLOSION particles
  - 3 FLASH particles
  - 100 SMOKE particles
  - Sonidos: EXPLODE, FIRE_EXTINGUISH, LEVELUP
  - Title al jugador: `§a§l✓ GRIETA CERRADA`
  - Broadcast global con nombre del jugador

### 3. **Spawn Estratégico (150-300 bloques)**

#### Antes:
- 50-200 bloques (demasiado cerca)

#### Ahora:
- **150-300 bloques** del jugador más activo
- Búsqueda en área de 600x600 bloques
- 30 intentos para encontrar ubicación válida
- Coordenadas anunciadas por el Observador:

```
§6§l🌀 OBSERVADOR:
§f"Detecté un pulso inestable cerca de §cX: 324 Z: -578§f."
§f"El calor busca equilibrio."

§e» Acércate y §c§lGOLPEA LA GRIETA §epara cerrarla
§7  Se necesitan §c100 golpes §7para cerrarla
§7  Tiempo límite: §e5 minutos
```

### 4. **Sistema de Drops (Fragmentos)**

Al cerrar una grieta, el jugador recibe fragmentos con probabilidades:

| Fragmento | Probabilidad | Descripción |
|-----------|--------------|-------------|
| **Ceniza** | 60% | Fragmento común |
| **Fulgor** | 25% | Fragmento raro |
| **Eco Roto** | 6% | Fragmento muy raro |

**Efectos de drop:**
- 20 partículas END_ROD
- Sonido ITEM_PICKUP
- Mensaje en chat con color según rareza

> **Nota:** Los items custom deben implementarse. Actualmente solo muestra mensaje.

### 5. **ActionBar Mejorado**

```
§7[§6Grieta más cercana§7] §c156m §fN §7• §6▓▓▓▓▓▓░░░░ §6§lCERCA §7• §c❤ 85% §7• §e3/10 cerradas
```

Muestra:
- Distancia en metros
- Dirección cardinal (N/S/E/W)
- Barra de proximidad visual (5 niveles)
- **Vida de la grieta** (nuevo)
- Progreso global

### 6. **Listener Dedicado**

**Archivo nuevo:** `EcoBrasasListener.java`

Detecta dos eventos:
1. **EntityDamageByEntityEvent** - Cuando golpean el ArmorStand
2. **PlayerInteractAtEntityEvent** - Cuando hacen clic derecho

Verifica el tag `eco_grieta` y delega al método `onGrietaGolpeada()`

### 7. **Limpieza Automática**

Al detener el evento (`/avo eco stop`):
- Remueve todos los ArmorStands de grietas
- Limpia mapas de datos
- Log: `[EcoBrasas] Limpieza completada - X grietas eliminadas`

## 🎮 Flujo Completo del Evento

### Fase 1: Recolección (25 min)

```
1. Evento inicia → Cinemática intro
2. Cada 6 minutos → Spawn grieta lejos (150-300 bloques)
3. Broadcast con coordenadas del Observador
4. Jugadores viajan a la ubicación
5. Ven ArmorStand flotante con efectos
6. Golpean 100 veces para cerrarla
7. Reciben fragmentos (Ceniza/Fulgor/Eco Roto)
8. Progreso global aumenta
9. Al llegar a 10 grietas → Fase 2
```

### Características Técnicas

- **Intervalo spawn**: 360 seg (6 min) - configurable en `eventos.yml`
- **Duración fase**: 1500 seg (25 min)
- **Meta grietas**: 10 (configurable)
- **Grieta HP**: 100 golpes
- **Timeout**: 6000 ticks (5 min) - preparado para implementar

## 📊 Comandos de Testing

```bash
/avo eco start           # Inicia evento
/avo eco skip            # Salta intro → Fase 1 directa
/avo eco info            # Ver estado (grietas activas, cerradas)
/avo eco fase 1          # Forzar Fase 1 si necesitas reiniciar
```

## 🔧 Archivos Modificados

1. **EcoBrasasEvent.java** (+150 líneas)
   - Sistema de HP por grieta
   - Método `onGrietaGolpeada()`
   - Método `cerrarGrieta()` con efectos
   - Método `dropFragments()` con probabilidades
   - ArmorStand floating con efectos continuos

2. **EcoBrasasListener.java** (NUEVO)
   - Detecta golpes a grietas
   - Detecta clic derecho
   - Filtra por tag `eco_grieta`

3. **ApocalipsisCommand.java**
   - Comando `/avo eco skip` agregado
   - Todos los comandos eco arreglados

## 📦 Compilación

**JAR generado:** 250,544 bytes  
**Fecha:** 11/12/2025 11:58 AM  
**Estado:** ✅ BUILD SUCCESS (41 archivos compilados)

## 🚀 Próximos Pasos

### Para implementar completamente:

1. **Items custom** - Crear los 3 tipos de fragmentos en `items.yml` o custom ItemStack
2. **Fase 2 (Anclas)** - Sistema de entrega de fragmentos
3. **Fase 3 (Ritual)** - Consumir fragmentos en altar
4. **Timeout de grietas** - Auto-despawn después de 5 min si no son cerradas
5. **Spawn de mobs** - Blazes/Wither Skeletons al acercarse a grietas

### Para testing:

```bash
scp "C:\Users\riolu\Videos\Eventos\Apocalipsis-1.21.8\target\Apocalipsis-1.0.0.jar" oliveerf@Oliveerf.top:"C:\Users\OliveerF\Desktop\Servers\Server Test\plugins\Apocalipsis-1.0.0.jar"
```

Luego en servidor:
```
/reload
/avo eco start
[esperar intro o /avo eco skip]
[ir a coordenadas anunciadas]
[golpear el ArmorStand flotante 100 veces]
```

## 🎨 Adaptaciones al Diseño Original

| Característica | Diseño Original | Implementado |
|----------------|----------------|--------------|
| Grietas visibles | ✓ Flotando con efectos | ✅ ArmorStand + partículas continuas |
| Cierre por interacción | ✓ Golpear | ✅ Click derecho o golpe (100 HP) |
| Distancia spawn | 150-300 bloques | ✅ Implementado con búsqueda inteligente |
| Mensaje Observador | ✓ Con coordenadas | ✅ Formato narrativo completo |
| Drops (Ceniza/Fulgor/Eco) | 60%/25%/6% | ✅ Sistema de probabilidades |
| Timeout 5 min | ✓ Grieta desaparece | ⏳ Variable creada, lógica pendiente |
| Mobs defensores | ✓ Al llegar | ⏳ Pendiente implementar |
| Progreso global (pulso) | ✓ Barra 0-100% | ✅ Contador de grietas |

## 📝 Notas del Observador

```
"El fuego busca forma... no enemigos."

"Cada grieta cerrada es un respiro que le das al mundo.
Pero recuerda... el Nether no olvida.
Lo que tomas hoy, puede que tengas que devolverlo mañana."

— El Observador
```

---

**Listo para testear en servidor** 🔥
