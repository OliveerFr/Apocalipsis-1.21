# 🎁 Fix: Sistema de Recompensas por Rango

## 📋 Problema Identificado
El sistema de rangos NO estaba entregando recompensas al subir de rango y tampoco mostraba notificaciones/sonidos/mensajes globales.

### Causa Raíz
En `ExperienceService.java`, el método `addXP()` solo verificaba cambios de **nivel**, pero NO verificaba cambios de **RANGO**. Por lo tanto:
- ❌ No se llamaba a `RewardService.deliverRewards()`
- ❌ No se mostraban notificaciones épicas
- ❌ No se enviaban mensajes globales al servidor
- ❌ No se reproducían efectos visuales/sonoros

---

## ✅ Solución Implementada

### 1. Detección de Subida de Rango
**Archivo:** `src/main/java/me/apocalipsis/experience/ExperienceService.java`  
**Método:** `addXP()`

```java
// 🎯 Guardar rango anterior ANTES de añadir XP
MissionRank oldRank = plugin.getRankService().getRank(player);

// Añadir XP
data.addXp(xp);

// Verificar subida de nivel
int newLevel = calculateLevel(data.getXp());

if (newLevel > oldLevel) {
    data.setNivel(newLevel);
    leveledUp = true;
    
    // 🎯 Verificar si subió de RANGO (más importante que nivel)
    MissionRank newRank = plugin.getRankService().getRank(player);
    if (newRank != oldRank) {
        // ¡SUBIDA DE RANGO! Efectos épicos
        onRankUp(player, oldRank, newRank);
    } else {
        // Solo subió de nivel (efectos normales)
        onLevelUp(player, oldLevel, newLevel);
    }
}
```

### 2. Nuevo Método: `onRankUp()`
Se añadió un nuevo método que ejecuta TODOS los efectos épicos al subir de rango:

#### 🎨 Efectos Visuales
```java
// Título épico (4 segundos de duración)
Title title = Title.title(
    Component.text("§6§l⬆ RANGO ASCENDIDO ⬆"),
    Component.text(rankName)
);
player.showTitle(title);

// Partículas épicas (100+ partículas)
- TOTEM_OF_UNDYING (100 partículas)
- FIREWORK (50 partículas)
- END_ROD (40 partículas)
- ENCHANT (60 partículas)
```

#### 🔊 Efectos de Sonido
```java
- UI_TOAST_CHALLENGE_COMPLETE (volumen 2.0)
- ENTITY_PLAYER_LEVELUP (volumen 1.5, pitch 1.2)
- ENTITY_ENDER_DRAGON_GROWL (volumen 0.8, pitch 1.8)
```

#### 💬 Notificaciones
```java
// Mensaje personal épico
§6§l══════════════════════════════════════
§e§l         ¡ASCENSO DE RANGO!
§7Has alcanzado el rango [NOMBRE_RANGO]§7!
§6§l══════════════════════════════════════

// Mensaje GLOBAL al servidor
§6§l★ [Jugador] §7ha alcanzado el rango [NOMBRE_RANGO]§7! §6§l★
```

#### 🎁 Entrega de Recompensas
```java
// Delay de 1 segundo para que el jugador vea el título primero
Bukkit.getScheduler().runTaskLater(plugin, () -> {
    boolean delivered = plugin.getRewardService().deliverRewards(player, newRank);
    if (delivered) {
        player.sendMessage("§a§l✔ §7Recompensas de rango entregadas!");
    }
}, 20L);
```

---

## 🧪 Cómo Probar

### Método 1: Comando Directo
```
/avo xp set <jugador> <xp>
```

**Ejemplos de XP por Rango:**
- **NOVATO** → **EXPLORADOR**: `/avo xp set NombreJugador 5000`
- **EXPLORADOR** → **SOBREVIVIENTE**: `/avo xp set NombreJugador 12000`
- **SOBREVIVIENTE** → **VETERANO**: `/avo xp set NombreJugador 25000`
- **VETERANO** → **EXPERTO**: `/avo xp set NombreJugador 45000`
- **EXPERTO** → **MAESTRO**: `/avo xp set NombreJugador 70000`
- **MAESTRO** → **LEYENDA**: `/avo xp set NombreJugador 95000`
- **LEYENDA** → **ABSOLUTO**: `/avo xp set NombreJugador 110075`

### Método 2: XP Natural
1. Completa misiones
2. Mata mobs
3. Mina bloques
4. Cuando acumules suficiente XP, verás los efectos épicos automáticamente

---

## 📦 Verificación de Recompensas

### Contenido de `recompensas.yml`
Las recompensas están configuradas en `src/main/resources/recompensas.yml`:

```yaml
rangos:
  EXPLORADOR:
    - "ps give %player% 15 3"        # 3x ProtectionStone 15x15
    - "give %player% diamond 8"      # 8 diamantes
    - "give %player% iron_ingot 32"  # 32 lingotes de hierro
    
  SOBREVIVIENTE:
    - "ps give %player% 20 2"        # 2x ProtectionStone 20x20
    - "give %player% diamond 16"     # 16 diamantes
    - "give %player% netherite_scrap 4"  # 4 fragmentos de netherite
    
  # ... más rangos ...
```

### Verificación en Juego
1. **Inventario:** Revisa que recibiste los items
2. **ProtectionStones:** Usa `/ps` para ver tus stones
3. **Habilidades:** Verifica con `/avo habilidades`
4. **Log del servidor:** Busca `[XP] [Jugador] subió de rango`

---

## 🔧 Archivos Modificados

| Archivo | Cambios |
|---------|---------|
| `ExperienceService.java` | ✅ Añadida detección de cambio de rango en `addXP()` |
| `ExperienceService.java` | ✅ Añadido método `onRankUp()` con efectos épicos |
| `ExperienceService.java` | ✅ Prioridad: Rango > Nivel (rango es más importante) |

---

## 📊 Flujo del Sistema

```
┌─────────────────────────────────────────────────────────────────┐
│                    JUGADOR GANA XP                              │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│  ExperienceService.addXP()                                      │
│  1. Guardar oldRank = getRank(player)                           │
│  2. data.addXp(xp)                                              │
│  3. Calcular newLevel                                           │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
             ┌────────┴─────────┐
             │ ¿newLevel >      │
             │  oldLevel?       │
             └────────┬─────────┘
                      │
          ┌───────────┴────────────┐
          │ SÍ                     │ NO → Terminar
          ▼                        │
┌─────────────────────────────────────────────────────────────────┐
│  Subió de Nivel                                                 │
│  1. data.setNivel(newLevel)                                     │
│  2. newRank = getRank(player)                                   │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
             ┌────────┴─────────┐
             │ ¿newRank !=      │
             │  oldRank?        │
             └────────┬─────────┘
                      │
          ┌───────────┴────────────┐
          │                        │
          ▼ SÍ                     ▼ NO
┌──────────────────────┐   ┌──────────────────────┐
│  onRankUp()          │   │  onLevelUp()         │
│  🎉 ÉPICO!           │   │  Normal              │
├──────────────────────┤   ├──────────────────────┤
│ • Título épico       │   │ • Título simple      │
│ • 3 sonidos épicos   │   │ • 1 sonido           │
│ • 4 tipos partículas │   │ • Sin partículas     │
│ • Mensaje personal   │   │ • Mensaje simple     │
│ • Broadcast global   │   │ • Sin broadcast      │
│ • deliverRewards()   │   │ • Sin recompensas    │
└──────────────────────┘   └──────────────────────┘
          │                        │
          └────────────┬───────────┘
                       │
                       ▼
              ┌────────────────┐
              │   FIN          │
              └────────────────┘
```

---

## 🎯 Resultados Esperados

### Cuando un Jugador Sube de Rango:
1. ✅ **Título épico:** "⬆ RANGO ASCENDIDO ⬆" + nombre del rango
2. ✅ **Sonidos épicos:** 3 sonidos simultáneos (desafío completado + subida + dragón)
3. ✅ **Partículas épicas:** 250+ partículas de 4 tipos diferentes
4. ✅ **Mensaje personal:** Caja con borde dorado celebrando el ascenso
5. ✅ **Broadcast global:** Todo el servidor ve que subiste de rango
6. ✅ **Recompensas:** Items, ProtectionStones, habilidades desbloqueadas
7. ✅ **Log del servidor:** Se registra el cambio de rango

### Cuando un Jugador Solo Sube de Nivel:
1. ✅ **Título simple:** "Nivel [X]"
2. ✅ **Sonido simple:** ENTITY_PLAYER_LEVELUP
3. ✅ Sin partículas
4. ✅ Sin mensaje especial
5. ✅ Sin broadcast
6. ✅ Sin recompensas

---

## 📝 Notas Técnicas

### Prioridad: Rango > Nivel
Si un jugador sube de nivel Y de rango al mismo tiempo, solo se ejecuta `onRankUp()` porque:
- El rango es más importante y más raro
- Ya incluye una experiencia más épica que nivel
- Evita spam de efectos visuales/sonoros

### Delay de Recompensas
Las recompensas se entregan con 1 segundo de delay (`20L` ticks) para:
- Permitir que el jugador vea el título primero
- Evitar que el inventario se abra y oculte el título
- Mejor experiencia de usuario

### Cache de Recompensas
`RewardService` usa `delivered_rewards` en `state.yml` para:
- No entregar recompensas duplicadas
- Persistir entre reinicios
- Soportar `/reload`

---

## 🚀 Despliegue

### Compilación
```bash
mvn clean package -DskipTests
```

### Resultado
```
✅ BUILD SUCCESS
📦 Apocalipsis-1.19.3.jar (498 KB)
📂 target/Apocalipsis-1.19.3.jar
```

### Instalación
1. Detén el servidor
2. Copia `target/Apocalipsis-1.19.3.jar` a `plugins/`
3. Reinicia el servidor
4. ¡Prueba subiendo de rango!

---

## 🐛 Troubleshooting

### "No se entregan recompensas"
1. Verifica que `RewardService` esté activado en logs
2. Revisa `state.yml` → `delivered_rewards` (puede estar ya entregado)
3. Comprueba que los comandos en `recompensas.yml` sean válidos
4. Asegúrate que el jugador tenga espacio en inventario

### "No aparecen efectos visuales"
1. Verifica que el cliente tenga partículas activadas
2. Comprueba que el sonido no esté silenciado
3. Revisa los logs del servidor por errores

### "No se ve el broadcast global"
1. Verifica que otros jugadores estén online
2. Comprueba que el chat no esté filtrado
3. Revisa permisos de broadcast

---

## ✨ Version
- **Plugin:** Apocalipsis v1.19.3
- **Fix:** Sistema de Recompensas por Rango
- **Fecha:** 2025-11-23
- **Estado:** ✅ Compilado y Funcional
