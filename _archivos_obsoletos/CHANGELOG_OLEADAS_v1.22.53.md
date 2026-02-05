═══════════════════════════════════════════════════════════════════
  CHANGELOG v1.22.53 - SISTEMA DE OLEADAS PERIÓDICAS + BALANCE
═══════════════════════════════════════════════════════════════════

✅ COMPILACIÓN EXITOSA
- Apocalipsis-1.22.53.jar
- Apocalipsis-1.22.53-shaded.jar

═══════════════════════════════════════════════════════════════════
🔧 CAMBIOS IMPLEMENTADOS
═══════════════════════════════════════════════════════════════════

1. ⚖️ BALANCE DE DAÑO REDUCIDO
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   
   Multiplicadores reducidos de x4.0 a x3.0 máximo:
   
   ANTES:
   - HP: x2.0 → x3.5 (según intensidad)
   - Daño: x2.5 → x4.0 (según intensidad)
   - Velocidad: x1.1 → x1.35
   
   AHORA:
   - HP: x2.0 → x3.0 (según intensidad)
   - Daño: x2.0 → x3.0 (según intensidad) ✅ REDUCIDO
   - Velocidad: x1.1 → x1.35 (sin cambios)
   
   Afecta a:
   - Todos los mobs hostiles en Overworld
   - Vex invocados por Evokers
   - Reinforcements de Zombies
   - Spawns naturales durante evento

═══════════════════════════════════════════════════════════════════

2. ⚡ SISTEMA DE OLEADAS PERIÓDICAS NUEVO
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   
   ✨ FUNCIÓN: iniciarOleadasHostilesDescubrimiento()
   
   CARACTERÍSTICAS:
   - Frecuencia: CADA 60 SEGUNDOS (1200 ticks)
   - Inicio: 10 segundos después del evento (200 ticks delay)
   - Duración: Durante toda la fase DESCUBRIMIENTO
   
   MECÁNICA DE SPAWN:
   ┌─────────────────────────────────────────────────────────────┐
   │ POR CADA JUGADOR EN OVERWORLD:                              │
   ├─────────────────────────────────────────────────────────────┤
   │ • Distancia: 10-20 bloques del jugador                      │
   │ • Cantidad: 2-4 mobs (inicio) → 3-6 mobs (final)            │
   │ • Altura: Superficie (highest block + 1)                    │
   │ • Distribución: Radial aleatoria 360°                       │
   └─────────────────────────────────────────────────────────────┘
   
   PROGRESIÓN DE INTENSIDAD:
   
   ┏━━━━━━━━━━━━┳━━━━━━━━━━━━━┳━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
   ┃ Intensidad ┃ Tiempo      ┃ Tipos de Mobs                 ┃
   ┣━━━━━━━━━━━━╋━━━━━━━━━━━━━╋━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
   ┃ 0.0 - 0.3  ┃ 45m - 32m   ┃ BÁSICOS                       ┃
   ┃            ┃             ┃ • Zombie                      ┃
   ┃            ┃             ┃ • Skeleton                    ┃
   ┃            ┃             ┃ • Spider                      ┃
   ┣━━━━━━━━━━━━╋━━━━━━━━━━━━━╋━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
   ┃ 0.3 - 0.6  ┃ 32m - 18m   ┃ MEDIOS                        ┃
   ┃            ┃             ┃ • Básicos +                   ┃
   ┃            ┃             ┃ • Husk                        ┃
   ┃            ┃             ┃ • Stray                       ┃
   ┃            ┃             ┃ • Cave_Spider                 ┃
   ┣━━━━━━━━━━━━╋━━━━━━━━━━━━━╋━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
   ┃ 0.6 - 1.0  ┃ 18m - 0m    ┃ AVANZADOS                     ┃
   ┃            ┃             ┃ • Básicos + Medios +          ┃
   ┃            ┃             ┃ • Pillager (Crossbow)         ┃
   ┃            ┃             ┃ • Vindicator (Hacha)          ┃
   ┃            ┃             ┃ • Ravager (Tank)              ┃
   ┃            ┃             ┃ • Witch (Pociones)            ┃
   ┃            ┃             ┃ • Wither_Skeleton             ┃
   ┗━━━━━━━━━━━━┻━━━━━━━━━━━━━┻━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
   
   EFECTOS POR MOB SPAWNEADO:
   ✓ Buffs automáticos (HP x2.0-3.0, Daño x2.0-3.0)
   ✓ Nombres épicos con símbolos
   ✓ Efecto glowing activado
   ✓ Equipment progresivo (armas/armadura según intensidad)
   ✓ Partículas de spawn (Smoke + Flame)
   ✓ Tracking para cleanup
   
   MENSAJES AL JUGADOR:
   - Cada 3 oleadas (3 minutos): Mensaje broadcast con número de oleada
   - Muestra intensidad actual en porcentaje
   - Logs en consola con detalles de spawn

═══════════════════════════════════════════════════════════════════

3. 📊 ESTADÍSTICAS DE OLEADAS
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   
   EJEMPLO DE SESIÓN (3 JUGADORES, 45 MINUTOS):
   
   Tiempo     | Oleada | Intensidad | Mobs/Jugador | Total
   ─────────────────────────────────────────────────────────
   00:10      | #1     | 0%         | 2-4          | 6-12
   01:10      | #2     | 2%         | 2-4          | 6-12
   02:10      | #3     | 4%         | 2-4          | 6-12
   ...
   15:00      | #15    | 33%        | 2-4          | 6-12
   20:00      | #20    | 44%        | 2-4          | 6-12
   25:00      | #25    | 55%        | 3-6          | 9-18
   30:00      | #30    | 67%        | 3-6          | 9-18
   35:00      | #35    | 78%        | 3-6          | 9-18
   40:00      | #40    | 89%        | 3-6          | 9-18
   45:00      | #45    | 100%       | 3-6          | 9-18
   
   TOTAL APROXIMADO: 300-450 mobs spawneados en 45 minutos

═══════════════════════════════════════════════════════════════════

⚠️ CORRECCIONES DE BUGS
═══════════════════════════════════════════════════════════════════

BUG CORREGIDO #1: "Mobs no spawneaban cada minuto"
   - Causa: Solo existían spawns dramáticos cada 2-5 minutos
   - Solución: Sistema de oleadas periódicas cada 60 segundos
   
BUG CORREGIDO #2: "Solo aparecían los primeros esqueletos"
   - Causa: No había sistema de spawn continuo durante DESCUBRIMIENTO
   - Solución: BukkitRunnable que ejecuta cada 60s hasta fin de fase
   
BUG CORREGIDO #3: "Daño demasiado alto (x4.0)"
   - Causa: Multiplicadores muy agresivos
   - Solución: Reducción a x3.0 máximo en todo el sistema

═══════════════════════════════════════════════════════════════════

Build Time: ~3 minutos
Versión: 1.22.53
Estado: ✅ LISTO PARA TESTING
Fecha: 22 de Enero, 2026
