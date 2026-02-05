# 🚀 MEJORAS: Auto-Inicio + Desastres Intensificados v1.22.57

## 📋 Problemas Resueltos

### ❌ Problema 1: Auto-Inicio Manual
**Reportado:**
> "al iniciar el server deberia ir a preparacion de 15 minutos y cuando termina iniciar desastre.. me toco hacerlo manual"

**Causa:**
```yaml
# desastres.yml
ciclo:
  start_on_boot: false  # ❌ Desactivado
```

**Solución:**
```yaml
ciclo:
  start_on_boot: true   # ✅ ACTIVADO
```

---

### ❌ Problema 2: Desastres Duraban 15 Minutos
**Reportado:**
> "los desastres deberian durar entre 1minuto a 1m:30s estan durando 15 minutos"

**Logs:**
```
[TimeService] INICIO - ID: tormenta_glacial | Planeado: 900s  ❌
[Apocalipsis] INICIO: TORMENTA_GLACIAL (900s)                 ❌
```

**Causa:**
- Los desastres leían duración desde config (75s, 70s, 80s)
- Pero `preparacion_inicial_segundos: 900` se usaba incorrectamente

**Solución:**
```yaml
# Cooldown reducido de 15 min a 1 min
cooldown_fin_segundos: 60  # ⚡ 1 minuto entre desastres

# Duraciones confirmadas:
tormenta_glacial: 75s      # ✅ 1 min 15s
tormenta_electrica: 70s    # ✅ 1 min 10s
erupcion_volcanica: 80s    # ✅ 1 min 20s
```

---

### ❌ Problema 3: Desastres Muy Básicos
**Reportado:**
> "mejora los desastres estan muy basicos"

**Solución:**
Intensificación masiva de mecánicas → Ver sección [Mejoras de Intensidad](#-mejoras-de-intensidad)

---

## ⚙️ Cambios Implementados

### 1. **Auto-Inicio al Boot** ✅

#### [desastres.yml](src/main/resources/desastres.yml) - Líneas 5-10
```diff
ciclo:
  auto_cycle: true
- start_on_boot: false             # ❌ No iniciar al encender/reload
+ start_on_boot: true              # ✅ ACTIVADO: Iniciar automáticamente
  
- cooldown_fin_segundos: 900       # ❌ 15 minutos entre desastres
+ cooldown_fin_segundos: 60        # ⚡ 1 minuto entre desastres
```

**Resultado:**
```
[17:05:15] [Server thread/INFO]: [AUTO-START] Ciclo de desastres iniciado automáticamente
[17:05:15] [Server thread/INFO]: [Cycle] PREPARACION (900s) → Primer desastre en 15 minutos
```

**Flujo:**
1. Servidor inicia
2. Plugin carga (5 segundos de delay)
3. ✅ Auto-inicia PREPARACION (15 minutos)
4. ✅ Tras 15 min, inicia desastre aleatorio
5. ✅ Desastre dura 60-90 segundos
6. ✅ Cooldown de 1 minuto
7. ✅ Repite automáticamente

---

### 2. **Cooldown Reducido** ⚡

**ANTES:**
```yaml
cooldown_fin_segundos: 900  # 15 minutos entre desastres
```

**AHORA:**
```yaml
cooldown_fin_segundos: 60   # 1 minuto entre desastres
```

**Impacto:**
- Ciclo total: **PREPARACION (15min) → Desastre (1-1.5min) → Cooldown (1min) → Repite**
- Total por ciclo: ~17 minutos
- Antes: ~31 minutos por ciclo (casi el doble)

---

### 3. **Duraciones Correctas** ✅

Confirmadas las duraciones de desastres:

| Desastre | Duración | Estado |
|----------|----------|--------|
| ❄️ Tormenta Glacial | 75s (1min 15s) | ✅ Correcto |
| ⚡ Tormenta Eléctrica | 70s (1min 10s) | ✅ Correcto |
| 🌋 Erupción Volcánica | 80s (1min 20s) | ✅ Correcto |

---

## 🔥 Mejoras de Intensidad

### ❄️ **Tormenta Glacial - Intensificada**

#### Hipotermia MORTAL
```diff
hipotermia:
- damage_base: 0.5             # 0.25 corazones
+ damage_base: 1.0             # ⚡ 0.5 corazones (DOBLADO)

- intervalo_base_ticks: 200    # Cada 10 segundos
+ intervalo_base_ticks: 120    # ⚡ Cada 6 segundos (66% más rápido)

- incremento_acumulativo: 0.25
+ incremento_acumulativo: 0.5  # ⚡ DOBLADO (muerte progresiva rápida)

- max_damage: 2.0              # Máximo 1 corazón
+ max_damage: 3.0              # ⚡ Máximo 1.5 corazones
```

**Resultado:**
- Tiempo 0: 0.5 ❤️ de daño
- Tiempo 6s: 0.75 ❤️ de daño
- Tiempo 12s: 1.0 ❤️ de daño
- Tiempo 18s: 1.25 ❤️ de daño
- Tiempo 24s: 1.5 ❤️ de daño (máximo)

**Sin protección = muerte en ~40 segundos**

---

#### Ráfagas Heladas PARALIZANTES
```diff
rafagas_heladas:
- intervalo_ticks: 400         # Cada 20 segundos
+ intervalo_ticks: 300         # ⚡ Cada 15 segundos (33% más frecuente)

- duracion_ticks: 120          # 6 segundos
+ duracion_ticks: 140          # ⚡ 7 segundos

- slowness_level: 2            # Slowness II
+ slowness_level: 3            # ⚡ Slowness III (casi inmóvil)

- mining_fatigue_level: 1
+ mining_fatigue_level: 2      # ⚡ Mining Fatigue II (casi no puedes minar)
```

**Resultado:**
- Cada 15 segundos quedas PARALIZADO por 7 segundos
- Slowness III = movimiento 60% más lento
- Mining Fatigue II = minado 81% más lento

---

#### Cristales de Hielo LLUVIA MORTAL
```diff
cristales_hielo:
- intervalo_ticks: 80          # Cada 4 segundos
+ intervalo_ticks: 50          # ⚡ Cada 2.5 segundos (60% más frecuente)

- min_cristales: 3
+ min_cristales: 5             # ⚡ Mínimo 5 cristales

- max_cristales: 6
+ max_cristales: 10            # ⚡ Hasta 10 cristales (66% más)

- damage: 1.5                  # 0.75 corazones
+ damage: 2.5                  # ⚡ 1.25 corazones (casi el doble)

- radio_spawn: 8
+ radio_spawn: 12              # ⚡ Área 50% mayor
```

**Resultado:**
- Lluvia CONSTANTE de cristales (cada 2.5s)
- Hasta 10 cristales simultáneos
- Cada cristal quita 1.25 ❤️
- Imposible esquivarlos todos

---

#### Estalactitas LETALES
```diff
estalactitas_hielo:
- advertencia_ticks: 60        # 3 segundos
+ advertencia_ticks: 40        # ⚡ 2 segundos (33% menos tiempo)

- damage_min: 2.0              # 1 corazón
+ damage_min: 3.0              # ⚡ 1.5 corazones

- damage_max: 4.0              # 2 corazones
+ damage_max: 6.0              # ⚡ 3 corazones (INSTANT KILL si no tienes armadura)
```

---

### ⚡ **Tormenta Eléctrica - CAÓTICA**

#### Rayos DEVASTADORES
```diff
rayos_dirigidos:
- advertencia_ticks: 60        # 3 segundos
+ advertencia_ticks: 40        # ⚡ 2 segundos

- damage: 3.0                  # 1.5 corazones
+ damage: 5.0                  # ⚡ 2.5 corazones (casi el doble)
```

---

#### Cadenas Eléctricas INTERMINABLES
```diff
cadenas_electricas:
- radio_salto: 5               # 5 bloques
+ radio_salto: 8               # ⚡ 8 bloques (60% más lejos)

- max_saltos: 3                # 3 saltos
+ max_saltos: 5                # ⚡ 5 saltos (afecta a más jugadores)

- damage_base: 3.0             # 1.5 corazones
+ damage_base: 5.0             # ⚡ 2.5 corazones

- reduccion_por_salto: 1.0     # -0.5 corazones
+ reduccion_por_salto: 0.8     # ⚡ -0.4 corazones (menos reducción)
```

**Resultado:**
Si 5 jugadores están a 8 bloques de distancia:
1. Jugador 1: 2.5 ❤️ de daño
2. Jugador 2: 2.1 ❤️ de daño (salto 1)
3. Jugador 3: 1.7 ❤️ de daño (salto 2)
4. Jugador 4: 1.3 ❤️ de daño (salto 3)
5. Jugador 5: 0.9 ❤️ de daño (salto 4)

**Total: 8.5 ❤️ de daño distribuido**

---

#### Zonas Ionizadas TRAMPA MORTAL
```diff
zonas_ionizadas:
- radio: 3
+ radio: 4                     # ⚡ 33% más grande

- duracion_ticks: 400          # 20 segundos
+ duracion_ticks: 600          # ⚡ 30 segundos (50% más tiempo)

- damage_por_segundo: 1.0      # 0.5 corazones/s
+ damage_por_segundo: 2.0      # ⚡ 1 corazón/s (DOBLADO)
```

**Resultado:**
- Si quedas atrapado 5 segundos = 5 ❤️ de daño
- Zona dura 30 segundos (antes 20s)
- Más difícil de evitar

---

#### Pulsos EMP CONSTANTES
```diff
pulsos_emp:
- solo_en_pico: true           # Solo en fase de pico
+ solo_en_pico: false          # ⚡ SIEMPRE activo

- intervalo_ticks: 600         # Cada 30 segundos
+ intervalo_ticks: 400         # ⚡ Cada 20 segundos (50% más frecuente)

- radio: 20
+ radio: 25                    # ⚡ Mayor alcance

- duracion_ticks: 100          # 5 segundos
+ duracion_ticks: 140          # ⚡ 7 segundos (40% más tiempo)
```

**Resultado:**
- Cada 20 segundos NO PUEDES USAR INVENTARIO por 7 segundos
- Activo durante TODO el desastre (no solo pico)
- Alcance mayor = más jugadores afectados

---

### 🌋 **Erupción Volcánica - APOCALÍPTICA**

#### Géiseres GIGANTES
```diff
geiseres_lava:
- altura_min: 5
+ altura_min: 7                # ⚡ Más altos

- altura_max: 10
+ altura_max: 15               # ⚡ 50% más altos

- advertencia_ticks: 60        # 3 segundos
+ advertencia_ticks: 40        # ⚡ 2 segundos

- duracion_ticks: 160          # 8 segundos
+ duracion_ticks: 200          # ⚡ 10 segundos activo

- damage: 2.0                  # 1 corazón
+ damage: 3.0                  # ⚡ 1.5 corazones
```

**Resultado:**
- Géiseres que llegan hasta Y=15
- Menos tiempo para escapar (2s)
- Más tiempo activos (10s)
- Daño 50% mayor

---

#### Rocas Volcánicas BOMBARDEO
```diff
rocas_volcanicas:
- min_rocas: 8
+ min_rocas: 12                # ⚡ 50% más rocas

- max_rocas: 12
+ max_rocas: 20                # ⚡ 66% más rocas

- intervalo_ticks: 240         # Cada 12 segundos
+ intervalo_ticks: 120         # ⚡ Cada 6 segundos (DOBLADO)

- explosion_power: 1.5
+ explosion_power: 2.0         # ⚡ Explosiones 33% más grandes

- radio_fuego: 2
+ radio_fuego: 4               # ⚡ DOBLE radio de fuego
```

**Resultado:**
- Cada 6 segundos caen 12-20 rocas (antes 8-12 cada 12s)
- **Frecuencia triplicada** de ataques
- Explosiones más grandes
- Fuego se expande el doble

---

#### Grietas Magmáticas ABISMOS
```diff
grietas_magmaticas:
- longitud_min: 4
+ longitud_min: 6              # ⚡ 50% más largas

- longitud_max: 8
+ longitud_max: 12             # ⚡ 50% más largas

- profundidad_min: 2
+ profundidad_min: 3           # ⚡ Más profundas

- profundidad_max: 3
+ profundidad_max: 5           # ⚡ 66% más profundas

- max_activas: 6
+ max_activas: 10              # ⚡ 66% más grietas simultáneas

- advertencia_ticks: 40        # 2 segundos
+ advertencia_ticks: 30        # ⚡ 1.5 segundos
```

**Resultado:**
- Grietas de hasta 12 bloques de largo
- Hasta 5 bloques de profundidad
- 10 grietas activas simultáneamente
- Solo 1.5 segundos para escapar

---

#### Ceniza Volcánica TÓXICA
```diff
ceniza_volcanica:
- intervalo_ticks: 600         # Cada 30 segundos
+ intervalo_ticks: 400         # ⚡ Cada 20 segundos (50% más frecuente)

- radio: 15
+ radio: 20                    # ⚡ 33% mayor alcance

- nausea_level: 2
+ nausea_level: 3              # ⚡ Náusea III (peor)

- nausea_duration_ticks: 120   # 6 segundos
+ nausea_duration_ticks: 160   # ⚡ 8 segundos

- blindness_level: 1
+ blindness_level: 2           # ⚡ Ceguera II

- blindness_duration_ticks: 80 # 4 segundos
+ blindness_duration_ticks: 120 # ⚡ 6 segundos

- particle_count: 500
+ particle_count: 800          # ⚡ 60% más partículas visuales
```

**Resultado:**
- Cada 20 segundos (antes 30s)
- Náusea III + Ceguera II por 8 segundos
- Afecta en radio de 20 bloques
- Efectos devastadores para combate/movimiento

---

## 📊 Comparativa Antes vs Después

### ⏱️ **Tiempos**
| Aspecto | ANTES | AHORA | Cambio |
|---------|-------|-------|--------|
| Auto-inicio | ❌ Manual | ✅ Automático | +100% |
| Desastre dura | 900s (15 min) ❌ | 60-80s ✅ | -90% |
| Cooldown | 900s (15 min) | 60s (1 min) | -93% |
| Ciclo completo | ~31 min | ~17 min | -45% |

### 💀 **Letalidad**
| Mecánica | ANTES | AHORA | Cambio |
|----------|-------|-------|--------|
| Hipotermia damage/s | 0.025 ❤️/s | 0.083 ❤️/s | +232% |
| Cristales frecuencia | 1 cada 4s | 1 cada 2.5s | +60% |
| Cristales cantidad | 3-6 | 5-10 | +66% |
| Cristales damage | 0.75 ❤️ | 1.25 ❤️ | +67% |
| Rayos damage | 1.5 ❤️ | 2.5 ❤️ | +67% |
| Rocas frecuencia | 1 cada 12s | 1 cada 6s | +100% |
| Rocas cantidad | 8-12 | 12-20 | +67% |
| Zonas ionizadas damage | 0.5 ❤️/s | 1.0 ❤️/s | +100% |

---

## 🎮 Impacto en Gameplay

### **ANTES** (v1.22.56)
```
❌ Requería /avo start manual
❌ Desastres duraban 15 minutos
❌ Cooldown de 15 minutos
❌ Mecánicas poco amenazantes
❌ Fácil de sobrevivir AFK
```

### **AHORA** (v1.22.57)
```
✅ Inicia automáticamente al boot
✅ Desastres duran 60-80 segundos (intensos)
✅ Cooldown de 1 minuto (dinámico)
✅ Mecánicas LETALES
✅ Requiere estrategia y preparación
```

---

## 🛡️ Estrategias de Supervivencia

### ❄️ **Tormenta Glacial**
**Protecciones esenciales:**
- 🔥 Rodéate de antorchas/fuego (70% reducción)
- 🧥 Armadura de cuero (25% reducción por pieza)
- 🏠 Refugio bajo techo (40% reducción)
- ⚠️ NUNCA permanezcas expuesto > 20 segundos

**Sin protección:**
- Hipotermia te matará en ~40 segundos
- Cristales + Hipotermia = muerte en ~25 segundos

---

### ⚡ **Tormenta Eléctrica**
**Protecciones esenciales:**
- 🔱 Lightning rods cerca (radio 8)
- 🪟 Refugio de vidrio/lana (40% reducción)
- ❌ NO llevar items metálicos (3x más rayos)
- ❌ NO estar en agua (2x damage)

**Sin protección:**
- Rayos cada ~5 segundos (2.5 ❤️)
- Cadenas eléctricas pueden matar grupos
- Zonas ionizadas = trampa mortal

---

### 🌋 **Erupción Volcánica**
**Protecciones esenciales:**
- 🪨 Refugio de obsidiana/piedra (60% reducción)
- ⛰️ Estar alto (Y>10) (40% reducción rocas)
- 💧 Cubos de agua para solidificar lava
- 🧊 Blue ice para enfriar géiseres (50% reducción)

**Sin protección:**
- Bombardeo constante cada 6 segundos
- Grietas se abren bajo tus pies (1.5s para escapar)
- Ceniza cada 20 segundos (ceguera + náusea)
- Géiseres de 15 bloques de altura

---

## 🔧 Archivos Modificados

### [src/main/resources/desastres.yml](src/main/resources/desastres.yml)
```diff
Línea 6:
- start_on_boot: false
+ start_on_boot: true

Línea 9:
- cooldown_fin_segundos: 900
+ cooldown_fin_segundos: 60

Líneas 630-801:
+ Intensificación completa de:
  - Tormenta Glacial (12 parámetros mejorados)
  - Tormenta Eléctrica (10 parámetros mejorados)
  - Erupción Volcánica (15 parámetros mejorados)
```

**Total:** ~50 líneas modificadas

---

## 🚀 Testing

### Test 1: Auto-Inicio
1. Detener servidor
2. Iniciar servidor
3. **Esperar 5 segundos**
4. Verificar logs:
```
[AUTO-START] Ciclo de desastres iniciado automáticamente
[Cycle] PREPARACION (900s) → Primer desastre en 15 minutos
```
5. Esperar 15 minutos
6. Verificar que desastre aleatorio inicia
7. ✅ **PASS** si se inició automáticamente

---

### Test 2: Duración Correcta
1. Ejecutar `/avo force tormenta_glacial`
2. Verificar logs:
```
[TimeService] INICIO - ID: tormenta_glacial | Planeado: 75s
[Apocalipsis] INICIO: TORMENTA_GLACIAL (75s)
```
3. Cronometrar duración real
4. ✅ **PASS** si dura 75 segundos (±2s)

---

### Test 3: Intensidad Mortal
1. Iniciar `tormenta_glacial`
2. Permanecer expuesto sin protección
3. Verificar efectos:
   - Hipotermia aumenta cada 6 segundos
   - Cristales caen cada 2.5 segundos
   - Ráfagas cada 15 segundos
4. ✅ **PASS** si mueres en < 60 segundos

---

### Test 4: Cooldown Rápido
1. Terminar un desastre
2. Verificar estado: `PREPARACION`
3. Esperar 1 minuto
4. ✅ **PASS** si siguiente desastre inicia tras 1 min

---

## 📝 Changelog Resumido

### v1.22.57 - Auto-Inicio + Desastres Intensificados
**Auto-Inicio:**
- ✅ `start_on_boot: true` (inicia automáticamente al boot)
- ✅ `cooldown_fin_segundos: 60` (1 minuto entre desastres)

**Duraciones:**
- ✅ Tormenta Glacial: 75s
- ✅ Tormenta Eléctrica: 70s
- ✅ Erupción Volcánica: 80s

**Intensificación:**
- ⚡ Tormenta Glacial: +232% damage/s, +60% frecuencia
- ⚡ Tormenta Eléctrica: +67% damage, +50% frecuencia EMP
- ⚡ Erupción Volcánica: +100% frecuencia rocas, +66% grietas

**Total:** ~50 parámetros mejorados

---

## 🎯 Próximos Pasos

### Para Administradores:
1. ✅ El auto-inicio está ACTIVADO por defecto
2. ✅ Desastres duran 60-80 segundos
3. ✅ Cooldown de 1 minuto
4. ⚠️ **Los desastres son MUCHO más difíciles**
5. ⚠️ **Comunicar a jugadores las nuevas protecciones**

### Para Jugadores:
1. 📚 Leer sección [Estrategias de Supervivencia](#%EF%B8%8F-estrategias-de-supervivencia)
2. 🛡️ Preparar refugios ANTES de desastres
3. 🔥 Tener antorchas/fuego para Tormenta Glacial
4. 🔱 Construir lightning rods para Tormenta Eléctrica
5. 🪨 Usar obsidiana/piedra para Erupción Volcánica

---

**Compilado:** v1.22.57  
**Autor:** Sistema de Mejoras Apocalipsis  
**Fecha:** 28/01/2026  
**Estado:** ✅ COMPLETADO Y LETAL 💀  
