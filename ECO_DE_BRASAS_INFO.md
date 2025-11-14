# 🩸 Mini-evento: Eco de Brasas (versión dinámica)

## 🎭 Contexto narrativo

Desde que el Nether se abrió, el calor del inframundo se filtró a la superficie.
El Observador siente grietas que aparecen al azar, como si el mundo exhalara fuego para no colapsar.
Su tarea no es apagarlo, sino aprender a controlarlo.

> "El fuego busca forma... no enemigos."
> — El Observador

## 🕹️ Dinámica general

### 🔸 Objetivo principal:

Recolectar fragmentos de energía (Ceniza, Fulgor, Eco Roto) desde grietas que aparecen lejos de los jugadores — y luego estabilizar esas zonas antes de que se propaguen.

### 🔸 Cómo funciona:

1. El servidor detecta cada cierto tiempo (6–8 min) el jugador más activo en el overworld.
2. A 150–300 bloques de distancia de él (dirección aleatoria segura), genera una **Grieta de Vapor**.
3. En el chat global aparece un mensaje del Observador con las coordenadas.
4. Los jugadores deben llegar a las coordenadas, cerrar la grieta y recoger los fragmentos que suelta.
5. Si nadie va en 5 min, la grieta desaparece.
6. Si llegan, aparecen mobs vanilla de defensa y partículas de fuego.
7. Al limpiarla, sueltan:
   - **Fragmento de Ceniza** (60 %)
   - **Fragmento de Fulgor** (25 %)
   - **Eco Roto** (6 %)
8. Cada grieta cerrada aumenta el **pulso global** (0-100%).
9. Cuando llegan a 100 %, se activa el ritual de estabilización (segunda fase).

## 🔥 Fases del evento

### FASE 1 — Recolección: "El mundo exhala"

**Duración:** 20–25 min  
**Objetivo:** Buscar grietas y recolectar fragmentos.

- El servidor anuncia nuevas grietas con coordenadas globales.
- Jugadores las cierran para llenar el medidor global de energía.
- El Observador habla esporádicamente:
  - "El portal no duerme, solo respira más lento."
  - "La tierra quiebra donde ustedes caminan."
  - "Si no entienden el calor… lo perderán."

### FASE 2 — Estabilización: "Las tres anclas"

**Duración:** 45 min  
**Objetivo:** Cerrar 3 anclas de fuego que surgen cuando el pulso llega a 100 %.

- El sistema genera tres ubicaciones (anclas) basadas en la actividad de jugadores.
- Cada una pide entregas cooperativas:
  - 30 × Ceniza
  - 10 × Fulgor
  - 1 × Eco Roto
- Cada entrega parcial genera oleadas de mobs vanilla.
- A mitad de la fase aparece un **minijefe**, tipo Wither Skeleton fuerte, con drop especial:
  - **Fragmento de Equilibrio**
- Diálogos:
  - "Tres puntos sostienen el calor. Si caen juntos, el mundo respira mejor."
  - "No busquen destruirlo, aprendan su ritmo."
  - "Lo que tocan, respira. Lo que respira, recuerda."

### FASE 3 — Ritual final: "Dar forma al fuego"

**Duración:** 10–15 min  
**Objetivo:** Llenar el sello de energía usando los fragmentos recolectados.

- Aparece un altar global (con progreso visible).
- A mitad del ritual surge un **Guardián de Brasas** (Iron Golem resistente).
- Al terminar:
  - Partículas y sonido global.
  - Cada jugador recibe una **Luz Templada** (fragmento purificado).
  - Broadcast final: "El fuego cedió. Por hoy, el portal los escuchó."

## 🎁 Conexión con la historia

| Elemento | Significado narrativo |
|----------|----------------------|
| **Grietas** | Manifestaciones del desequilibrio del Nether filtrándose al mundo. |
| **Fragmentos** | Resto de los mundos pasados que aún laten (recuerdos). |
| **Luz Templada** | Energía estabilizada que se usará en el evento de Navidad. |
| **Observador** | Evalúa si los jugadores aprenden a moldear el ciclo en vez de repetirlo. |

> "No lo entienden aún... Cada chispa que guardan, alguna vez ya ardió en otro mundo."
> — El Observador

## 🎮 Comandos

### Iniciar el evento
```
/avo evento eco
```
Inicia el evento "Eco de Brasas". Requiere permisos de administrador.

### Detener el evento
```
/avo evento stop
```
Detiene el evento actual (cualquier desastre o evento en curso).

### Forzar inicio con comando existente
```
/avo force eco_brasas
```
Fuerza el inicio del evento ignorando restricciones (excepto SAFE_MODE).

## ⚙️ Configuración

Archivo: `desastres.yml`

```yaml
eco_brasas:
  duracion_segundos: 6000       # 100 minutos total (suma de las 3 fases)
  romper_bloques: false         # No rompe bloques físicamente
  
  # Fase 1: Recolección
  fase1_duracion_seg: 1500      # 25 minutos
  grieta_interval_seg: 480      # 8 minutos entre grietas
  grieta_distancia_min: 150     # Distancia mínima del jugador
  grieta_distancia_max: 300     # Distancia máxima del jugador
  grieta_despawn_seg: 300       # 5 minutos para cerrar grieta
  
  # Fase 2: Estabilización
  fase2_duracion_seg: 2700      # 45 minutos
  anclas_ceniza_req: 30         # Ceniza requerida por ancla
  anclas_fulgor_req: 10         # Fulgor requerido por ancla
  anclas_eco_req: 1             # Eco Roto requerido por ancla
  
  # Fase 3: Ritual Final
  fase3_duracion_seg: 900       # 15 minutos
  
  # Drop rates
  ceniza_drop_chance: 0.60      # 60% de probabilidad
  fulgor_drop_chance: 0.25      # 25% de probabilidad
  eco_drop_chance: 0.06         # 6% de probabilidad
```

## 🔧 Implementación técnica

### Archivos creados/modificados:

1. **EcoBrasasNew.java** - Clase principal del evento
   - Ubicación: `src/main/java/me/apocalipsis/disaster/EcoBrasasNew.java`
   - Extiende `DisasterBase`
   - Implementa las 3 fases con sistema de estados
   - Gestiona grietas, anclas y ritual

2. **DisasterRegistry.java** - Registro de desastres
   - Añadida línea para registrar `EcoBrasasNew`

3. **desastres.yml** - Configuración
   - Nueva sección `eco_brasas` con todos los parámetros
   - Añadido peso `eco_brasas: 1` en la sección `weights`

4. **ApocalipsisCommand.java** - Comandos
   - Nuevo método `cmdEvento()` para gestionar eventos especiales
   - Subcomandos: `eco` y `stop`

5. **AvoTabCompleter.java** - Autocompletado
   - Añadido `evento` a la lista de subcomandos
   - Autocompletado para `eco` y `stop`

### Características técnicas:

- ✅ Sistema de fases con transiciones automáticas
- ✅ Generación dinámica de grietas basada en actividad de jugadores
- ✅ Sistema de fragmentos con drop rates configurables
- ✅ Anclas con requisitos de entrega cooperativa
- ✅ Minijefe y jefe final
- ✅ Partículas y efectos visuales
- ✅ Mensajes del Observador periódicos
- ✅ Recompensas al completar
- ✅ Compatible con sistema de permisos y excepciones
- ✅ No rompe bloques (evento cooperativo)
- ✅ Integrado con el sistema de ciclo de desastres

## 🧪 Testing

Para probar el evento:

1. **Iniciar el evento:**
   ```
   /avo evento eco
   ```

2. **Verificar grietas:**
   - Esperar a que aparezca mensaje con coordenadas
   - Viajar a la ubicación
   - Cerrar la grieta acercándose (3 bloques)
   - Verificar drops de fragmentos

3. **Verificar Fase 2:**
   - Completar suficientes grietas para llegar a 100% pulso
   - Verificar aparición de 3 anclas
   - Probar entregas de fragmentos (TODO: implementar mechanic)

4. **Verificar Fase 3:**
   - Completar las 3 anclas
   - Verificar aparición del altar
   - Verificar spawn del Guardián de Brasas

5. **Detener evento:**
   ```
   /avo evento stop
   ```

## ⚠️ Notas importantes

1. **Peso en ciclo automático:** El evento tiene peso 1 igual a los demás desastres, puede ser seleccionado automáticamente.

2. **Duración total:** ~100 minutos (1h 40min), considerablemente más largo que los desastres normales (15-25 min).

3. **Cooperación requerida:** Este evento requiere cooperación entre jugadores, especialmente en Fase 2 y 3.

4. **Fragmentos:** Los fragmentos dropean como items físicos, los jugadores deben recogerlos manualmente.

5. **TODO - Mecánica de entrega:** La mecánica para entregar fragmentos a las anclas aún debe ser implementada (click derecho o drop cerca).

6. **Compatibilidad:** El evento respeta:
   - Sistema de excepciones (`/avo admin`)
   - SAFE_MODE (TPS bajo)
   - Permisos de administrador
   - Debug logs

## 📝 Mejoras futuras sugeridas

- [ ] Implementar mecánica de entrega de fragmentos a anclas (click derecho o drop)
- [ ] Agregar partículas más elaboradas para grietas y anclas
- [ ] Sistema de votación para activar el evento
- [ ] Estadísticas por jugador (fragmentos recolectados)
- [ ] Leaderboard de contribución al evento
- [ ] Drops especiales del minijefe y guardián
- [ ] Efectos de sonido personalizados
- [ ] Sistema de recompensas escalonadas según participación

---

**Creado por:** GitHub Copilot  
**Fecha:** 2025-11-11  
**Versión plugin:** Apocalipsis 1.21.8
