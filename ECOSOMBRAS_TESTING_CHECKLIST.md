# 🧪 CHECKLIST DE TESTING: ECO DE LAS SOMBRAS LARGAS

**Versión:** 1.15.0  
**Fecha:** 2025-11-18  
**Duración Total:** 2 horas máximo (7200 segundos)

---

## 📋 INSTRUCCIONES GENERALES

### Comandos de Testing
```
/avo eco_sombras start        # Iniciar evento
/avo eco_sombras stop         # Detener evento
/avo eco_sombras info         # Ver estado actual
/avo eco_sombras debug true   # Activar debug mode (si existe)
```

### Preparación Previa
- [ ] Server en versión 1.21.8
- [ ] Mínimo 1 jugador (ideal: 3-6 jugadores)
- [ ] Armadura Netherite con Protección IV
- [ ] Armas de diamante/netherite
- [ ] Suficiente comida y pociones
- [ ] Área de prueba libre de mobs naturales

---

## 🎬 ACTO 0: ACTIVACIÓN SILENCIOSA

**Duración:** 60 segundos  
**Trigger:** Manual (`/avo eco_sombras start`)

### ✅ Verificaciones

- [ ] **Comando funciona correctamente**
  - Comando: `/avo eco_sombras start`
  - Respuesta esperada: Confirmación de inicio

- [ ] **Oscurecimiento visual**
  - El tiempo de Minecraft cambia a 13000 (atardecer)
  - Duración del oscurecimiento: 5 segundos
  - El tiempo se restaura después

- [ ] **Partículas globales**
  - Tipo: ASH (ceniza)
  - Cantidad: ~50 partículas
  - Radio: 100 bloques alrededor de jugadores

- [ ] **Sonido inicial**
  - Tipo: ENTITY_WARDEN_HEARTBEAT
  - Volumen: 1.0, Pitch: 0.5
  - Audible para todos los participantes

- [ ] **Mensajes de chat**
  - Mensaje inicial: "§8Un eco desconocido se ha registrado en el mundo…"
  - Delay: 2 segundos
  - Visible para todos los jugadores

- [ ] **Transición automática a Acto 1**
  - Después de 60 segundos pasa al Acto 1
  - Verificar que no se quede estancado

### 📊 Resultado
- [ ] ✅ APROBADO
- [ ] ❌ FALLIDO - Detalles: _______________

---

## 🐛 ACTO 1: ALGO SE MUEVE DEBAJO (MANCHAS)

**Duración:** 900 segundos (15 minutos)  
**Trigger:** Automático tras Acto 0  
**⚠️ IMPLEMENTACIÓN ACTUAL:** Usa **SILVERFISH** en lugar de sistema de partículas

### ✅ Verificaciones

- [ ] **Transición automática desde Acto 0**
  - El acto inicia automáticamente sin intervención manual
  - Mensaje de inicio visible

- [ ] **Spawn de manchas (Silverfish)**
  - Cantidad simultánea: 5-8 silverfish
  - Radio de spawn: 10-30 bloques del jugador
  - Se spawneán continuamente durante los 15 minutos

- [ ] **Propiedades de las manchas**
  - Nombre: Debería tener nombre custom (verificar)
  - Visual: Son silverfish normales o invisibles?
  - Comportamiento: Huyen de jugadores?

- [ ] **🔧 FIX #8: Detector de proximidad**
  - [ ] Las manchas NO desaparecen cuando jugador está cerca
  - [ ] Sistema de huida funciona correctamente
  - [ ] Radio de detección: 5 bloques
  - [ ] Distancia de huida: 15-20 bloques
  - [ ] Sonido de huida: ENTITY_ENDERMAN_TELEPORT (pitch 0.3)
  - [ ] Partículas de huida: SMOKE_NORMAL (10 partículas)

- [ ] **Manchas vs Sistema de Partículas (YML)**
  - **NOTA:** El YML especifica sistema de partículas (SQUID_INK)
  - Implementación real usa SILVERFISH
  - Verificar cuál es la intención final
  - Documentar diferencias

- [ ] **Mensajes del Observador**
  - Texto: "§7§o\"No deberían moverse solas… eso pasó antes… y terminó mal.\""
  - Delay: 20 segundos desde inicio
  - Sonido: BLOCK_SCULK_SENSOR_CLICKING (volumen 0.3, pitch 0.8)

- [ ] **Condición de transición a Acto 2**
  - Trigger: `manchas_activas < 3`
  - Los jugadores deben matar manchas hasta que queden menos de 3
  - Verificar que el contador funciona correctamente

- [ ] **Duración máxima**
  - Si no completan objetivo en 15 minutos, ¿avanza automáticamente?
  - O se queda estancado hasta completar?

### 📊 Resultado
- [ ] ✅ APROBADO
- [ ] ⚠️ FUNCIONAL CON DIFERENCIAS (especificar)
- [ ] ❌ FALLIDO - Detalles: _______________

---

## 👤 ACTO 2: LAS SOMBRAS LARGAS

**Duración:** 1200 segundos (20 minutos)  
**Trigger:** Condición `manchas_activas < 3`

### ✅ Verificaciones

- [ ] **Transición desde Acto 1**
  - Se activa automáticamente al cumplir condición
  - Mensaje de transición visible

- [ ] **Spawn de Sombras Largas**
  - Tipo: ZOMBIE modificado
  - Nombre: "§8Sombra Larga"
  - Radio de spawn: 10-40 bloques
  - Intervalo: 15-20 segundos entre spawns
  - Máximo activas: 10-15 simultáneamente

- [ ] **Propiedades de las Sombras**
  - [ ] Invisibles (invisible: true)
  - [ ] Silenciosas (silencioso: true)
  - [ ] Equipamiento: LEATHER_HELMET negro (teñido 0,0,0)
  - [ ] Vida: 60 HP
  - [ ] Daño: 14 (penetra Netherite Prot 4)
  - [ ] Velocidad: 0.26
  - [ ] Armadura: 8
  - [ ] Knockback resistance: 0.3

- [ ] **Efectos visuales**
  - [ ] Partículas en pies: SQUID_INK (2 partículas cada 10 ticks)
  - [ ] Sonido ambiental: BLOCK_SCULK_SENSOR_CLICKING (cada 5 seg, pitch 0.5)
  - [ ] Partículas al spawn: SMOKE_LARGE (20 partículas)
  - [ ] Partículas al morir: SOUL (30 partículas hacia arriba)

- [ ] **Drops al morir**
  - Item: fragmento_sombra (custom item)
  - Probabilidad: 100%
  - Cantidad: 1-2 fragmentos

- [ ] **Mensajes del Observador**
  - Trigger: Después de matar 5 sombras
  - Texto: "§7§o\"Estiran su forma buscando un anfitrión… como lo hicieron en aquel mundo…\""

- [ ] **Condición de transición a Acto 3**
  - Trigger: `sombras_muertas >= 15`
  - Contador de sombras muertas funciona
  - Transición automática al cumplir

### 📊 Resultado
- [ ] ✅ APROBADO
- [ ] ❌ FALLIDO - Detalles: _______________

---

## 💠 ACTO 3: EL NÚCLEO DE SOMBRA LARGA

**Duración:** 1200 segundos (20 minutos)  
**Trigger:** Condición `sombras_muertas >= 15`

### ✅ Verificaciones

- [ ] **Spawn del Núcleo**
  - Tipo de entidad: SHULKER
  - Nombre: "§5§l§nNúcleo de Sombra Larga"
  - Altura sobre el suelo: 3 bloques
  - Busca posición válida en radio de 50 bloques
  - Vida: 400 HP (aumentado)

- [ ] **Propiedades del Núcleo**
  - [ ] No ataca a jugadores
  - [ ] Flotante (no cae)
  - [ ] Invulnerable a: FALL, DROWNING, FIRE
  - [ ] Resistente a PROJECTILE (50% reducción)

- [ ] **Efectos visuales continuos**
  - [ ] Rotación continua (velocidad 2.0)
  - [ ] Partículas PORTAL (5 cada 5 ticks, radio 2.0)
  - [ ] Partículas REVERSE_PORTAL (3 cada 10 ticks, radio 1.5)
  - [ ] Sonido ambiental: BLOCK_PORTAL_AMBIENT (loop, volumen 0.8, pitch 0.7)
  - [ ] Efecto latido cada 2 segundos (escala 0.9-1.1)

- [ ] **Sistema de Teletransporte**
  - [ ] Se teleporta cada 50 HP de daño recibido
  - [ ] O cada 25 segundos automáticamente
  - [ ] Distancia: 30-50 bloques
  - [ ] Valida que haya suelo sólido
  - [ ] Invulnerabilidad 2 segundos post-teleport
  - [ ] Partículas PORTAL (50) antes del TP
  - [ ] Partículas EXPLOSION_HUGE después
  - [ ] Sonido: ENTITY_ENDERMAN_TELEPORT (pitch 0.5)

- [ ] **Mensajes de aparición**
  - Título: "§5§lUna raíz de la sombra"
  - Subtítulo: "§7ha despertado"
  - Chat: "§5§lEl Núcleo de la Sombra Larga ha emergido."
  - Sonido: ENTITY_WITHER_SPAWN (pitch 0.8)

- [ ] **Condición de transición a Acto 4**
  - Trigger: `nucleo_vida <= 40%` (160 HP o menos)
  - BossBar muestra vida correctamente
  - Transición automática al cumplir

### 📊 Resultado
- [ ] ✅ APROBADO
- [ ] ❌ FALLIDO - Detalles: _______________

---

## ⚓ ACTO 4: LAS ANCLAS DEL MUNDO

**Duración:** 900 segundos (15 minutos)  
**Trigger:** Condición `nucleo_vida <= 40%`

### ✅ Verificaciones

- [ ] **Generación de Anclas**
  - Cantidad: 5 anclas
  - Radio de spawn: 40-80 bloques
  - Centradas en la posición del Núcleo
  - Valida suelo sólido

- [ ] **Estructura de cada Ancla**
  - [ ] Base: DEEPSLATE_TILES (patrón 3x3, altura 0)
  - [ ] Centro: RESPAWN_ANCHOR (posición 0,1,0)
  - [ ] Decoración: PURPLE_CANDLE en las 4 esquinas (encendidas)
  - [ ] Indestructible (no se puede romper)
  - [ ] No se puede interactuar (excepto para sellar)

- [ ] **Efectos visuales de Anclas**
  - Partículas END_ROD verticales (3 cada 10 ticks, altura 1-4)
  - Visible desde distancia

- [ ] **🔧 FIX #9: Items suficientes para sellar**
  - [ ] Los jugadores tienen suficientes fragmentos_sombra
  - [ ] Se han dropeado 15+ fragmentos en Acto 2
  - [ ] Se necesitan 25 fragmentos totales (5 anclas × 5 fragmentos)
  - [ ] Sistema de drop está aumentado o
  - [ ] Kit inicial proporciona fragmentos adicionales

- [ ] **Sistema de Sellado**
  - [ ] Item requerido: fragmento_sombra
  - [ ] Cantidad por ancla: 5 fragmentos
  - [ ] Interacción: Click derecho en RESPAWN_ANCHOR
  - [ ] Valida inventario del jugador
  - [ ] Consume los 5 fragmentos
  - [ ] Efectos al sellar:
    - Partículas END_ROD hacia arriba (50, altura 20)
    - Sonido BLOCK_RESPAWN_ANCHOR_CHARGE (pitch 0.5)
    - Brillo en el ancla
    - Partículas continuas WAX_ON (2 cada 20 ticks)
  - [ ] Mensaje: "§5Ancla X/5 sellada"
  - [ ] Sonido: BLOCK_NOTE_BLOCK_BELL (pitch 1.5)

- [ ] **Contador de Anclas selladas**
  - Visible en BossBar o ActionBar
  - Actualiza correctamente (0/5, 1/5... 5/5)

- [ ] **Núcleo después de sellar anclas**
  - Al sellar 5 anclas, el Núcleo se vuelve vulnerable
  - Los jugadores deben destruir el Núcleo
  - Mensaje: "§5§lTodas las anclas han sido selladas.\n§7El Núcleo es vulnerable."

- [ ] **Mensajes del Observador**
  - Trigger: Primera ancla sellada
  - Texto: "§7§o\"Sellan la herida, pero no la causa…\""

- [ ] **Condición de transición a Acto 5**
  - Trigger: `anclas_selladas == 5 AND nucleo_muerto == true`
  - Requiere completar AMBAS condiciones
  - Verificar orden de ejecución

### 📊 Resultado
- [ ] ✅ APROBADO
- [ ] ❌ FALLIDO - Detalles: _______________

---

## 🔮 ACTO 5: RITUAL DE ESTABILIDAD

**Duración:** 1800 segundos (30 minutos máximo)  
**Trigger:** Condición `anclas_selladas == 5 AND nucleo_muerto == true`

### ✅ Verificaciones

#### **Arena del Ritual**

- [ ] **Generación de Arena**
  - Forma: Circular, radio 18 bloques
  - Centro: Posición donde murió el Núcleo
  - Material suelo: DEEPSLATE_BRICKS
  - Material borde: CHISELED_DEEPSLATE
  - Centro: LODESTONE (posición 0,0,0)
  - Indestructible y anti-explosiones

- [ ] **Efectos visuales perimetrales**
  - Partículas END_ROD (72 partículas, 1 cada 5°)
  - Altura: 0-4 bloques
  - Frecuencia: Cada 5 ticks
  - Animación: Onda vertical

- [ ] **Sistema de detección de jugadores**
  - Detecta si jugadores salen del círculo (cada 20 ticks)
  - Advertencia: "§c¡No abandones el círculo!" (ActionBar)
  - Sonido: BLOCK_NOTE_BLOCK_BASS (pitch 0.5)
  - Penalización tras 5 segundos fuera:
    - WEAKNESS nivel 1 (10 seg)
    - SLOWNESS nivel 1 (10 seg)

#### **Sistema de Oleadas**

- [ ] **Oleada 1 (50 segundos)**
  - Nombre: "§7Primera Ola"
  - Mobs: 10 Sombras Largas (spawn cada 5 seg)
  - Escalado: +50% por jugador extra

- [ ] **Oleada 2 (50 segundos)**
  - Nombre: "§7Segunda Ola"
  - Mobs: 15 Sombras Rápidas (spawn cada 3 seg)
  - Velocidad: 1.3x

- [ ] **Oleada 3 (55 segundos)**
  - Nombre: "§7Tercera Ola"
  - Mobs: 12 Sombras Largas + 3 Sombras Dobles
  - Sombras Dobles: 2x vida

- [ ] **Oleada 4 (60 segundos)**
  - Nombre: "§8Cuarta Ola"
  - Mobs: 18 Sombras Rápidas + 4 Sombras Dobles

- [ ] **Oleada 5 (65 segundos)**
  - Nombre: "§8§lÚltima Ola"
  - Mobs: 15 Sombras Largas + 10 Rápidas + 6 Dobles

- [ ] **Pausas entre oleadas**
  - Duración: 12 segundos
  - Mensaje visible indicando próxima oleada

- [ ] **Efectos visuales de inicio de oleada**
  - Mensaje: "§8§l▂▂▂ OLEADA X/5 ▂▂▂"
  - Título: "§8Oleada X"
  - Subtítulo: "§7Resistid"
  - Sonido: ENTITY_WITHER_AMBIENT (pitch 0.7)
  - Partículas SQUID_INK (100 en radio 20)

#### **🔧 FIX #10: Guardián no entierra jugadores**

- [ ] **Spawn del Guardián**
  - Trigger: Completar Oleada 5
  - Tipo: IRON_GOLEM
  - Nombre: "§8§l§nGuardián de la Sombra Larga"
  - **Spawn +5 bloques sobre el suelo sólido**
  - **Teleport jugadores si están enterrados en el suelo**

- [ ] **Atributos del Guardián**
  - Vida base: 600 HP
  - Escalado: +100 HP por jugador
  - Daño: 15
  - Velocidad: 0.25
  - Armadura: 10
  - Knockback resistance: 0.8
  - Tamaño: 1.8x
  - Brillo: Activado (color DARK_PURPLE)

#### **🔧 FIX #11: Reducir efectos del Guardián**

- [ ] **Partículas reducidas (60% menos)**
  - Partículas SQUID_INK: **2 cada 5 ticks** (antes: 5)
  - Partículas SOUL: **1 cada 15 ticks** (antes: 2)
  - Verificar que el rendimiento mejora

- [ ] **BossBar con HP visible**
  - Título: "§8§lGuardián de la Sombra Larga"
  - Color: PURPLE
  - Estilo: SOLID
  - Muestra HP en tiempo real (X/Y HP)
  - Actualiza cada tick o cada segundo

#### **🔧 FIX #12: Escalado para 3 jugadores**

- [ ] **Requisito de jugadores mínimo**
  - Mínimo: 3 jugadores para iniciar evento
  - Validación al usar `/avo eco_sombras start`
  - Mensaje de error si hay menos de 3

- [ ] **Escalado de dificultad funcional**
  - Vida del Guardián escala correctamente (+100 HP/jugador)
  - Cantidad de mobs en oleadas escala (+50% por jugador extra)
  - Sistema DifficultyScaler funciona

#### **Habilidades del Guardián**

- [ ] **Pulso de Sombra (cooldown 15 seg)**
  - Radio: 10 bloques
  - Daño: 8 HP
  - Efectos: WITHER nivel 1 (5 seg)
  - Empuje radial: Fuerza 1.5
  - Partículas SQUID_INK (200 en radio 10)
  - Sonido: ENTITY_WARDEN_SONIC_BOOM (pitch 0.5)

- [ ] **Invocación de Refuerzos (cooldown 30 seg)**
  - Cantidad: 4 Sombras Largas
  - Radio de spawn: 8 bloques
  - Mensaje: "§8El Guardián invoca refuerzos…"

- [ ] **Fase Furia (al 30% HP)**
  - Activa una sola vez
  - Velocidad: +30%
  - Daño: +20%
  - Cooldown habilidades: -30%
  - Partículas DAMAGE_INDICATOR extras (10 cada 5 ticks)
  - Mensaje: "§c§l¡El Guardián entra en furia!"

- [ ] **Diálogos del Guardián**
  - Al spawn: "§8§o\"Ro… po… sis… ten…\""
  - 75% HP: "§8§o\"El… eco… debe… ce… rrar…\""
  - 50% HP: "§8§o\"No… merecen… la… sombra…\""
  - 25% HP: "§8§o\"Debe… olvidar… se…\""

- [ ] **Entrada dramática**
  - Oscurecimiento: 3 segundos
  - Rayo visual (sin daño)
  - Partículas EXPLOSION_HUGE (5)
  - Título: "§8§lGuardián de la Sombra Larga"
  - Subtítulo: "§7Enfrentad vuestro destino"
  - Duración: 60 ticks (3 segundos)

#### **Muerte del Guardián**

- [ ] **Animación de muerte (5 segundos)**
  - Animación lenta
  - Partículas SOUL (300 hacia arriba, velocidad 0.3)
  - Partículas EXPLOSION_HUGE (3, delay 2 seg)
  - Onda de choque (radio 20):
    - Partículas SQUID_INK (500)
    - Limpia todos los mobs del evento
    - Sonido ENTITY_WITHER_DEATH (volumen 2.0, pitch 0.5)

- [ ] **Drops del Guardián**
  - Item: eco_resonante (custom item)
  - Cantidad: 1 para todo el grupo
  - Drop único compartido

#### **🔧 FIX #13: Transición automática Guardián→Acto 6**

- [ ] **Flag `guardianDerrotado` se activa correctamente**
- [ ] **Transición inmediata a Acto 6 tras muerte**
- [ ] **No hay múltiples triggers del evento de muerte**
- [ ] **Limpieza de entidades funciona**

### 📊 Resultado
- [ ] ✅ APROBADO
- [ ] ❌ FALLIDO - Detalles: _______________

---

## 🌑 ACTO 6: EL DESPERTAR (CLIFFHANGER)

**Duración:** 120 segundos (2 minutos)  
**Trigger:** Condición `guardian_muerto == true`

### ✅ Verificaciones

- [ ] **Spawn del Símbolo Geométrico**
  - Delay: 5 segundos tras muerte del Guardián
  - Posición: Donde murió el Guardián
  - Duración: 60 segundos
  - Forma: Geométrico custom
  - Radio: 4 bloques

- [ ] **Materiales del Símbolo**
  - BLACK_CONCRETE: Anillo exterior
  - PURPLE_STAINED_GLASS: Cruz central
  - CRYING_OBSIDIAN: Vértices
  - Altura: 0 (nivel del suelo)
  - Indestructible

- [ ] **Efectos visuales del Símbolo**
  - Rotación continua (velocidad 1.0)
  - Partículas PORTAL (20 en espiral, altura 0-2, cada 5 ticks)
  - Sonido ambiental: BLOCK_SCULK_SHRIEKER_SHRIEK (loop, pitch 0.3)

- [ ] **Desaparición del Símbolo**
  - Automática tras 60 segundos
  - Partículas SMOKE_LARGE (100)
  - Sonido: ENTITY_ENDERMAN_TELEPORT (pitch 0.1)

- [ ] **Monólogo del Observador**
  - Mensaje 1 (10 seg): "§7§o\"Eso… no debería existir.\""
  - Mensaje 2 (25 seg): "§7§o\"Ya dejó sombra. Lo siguiente será… forma.\""
  - Mensaje 3 (40 seg): "§7§o\"El mundo no recuerda así. Esto viene de más lejos.\""

- [ ] **Figura Misteriosa en el Horizonte**
  - Delay spawn: 55 segundos
  - Distancia: 300 bloques
  - Dirección: Aleatoria
  - Tipo: Armor Stand invisible
  - Tamaño: 3.0x (muy grande)
  - Partículas SQUID_INK (100) formando silueta humanoide (altura 6)

- [ ] **Finalización del Evento**
  - Mensaje de finalización después de 120 segundos
  - Distribución de recompensas
  - Limpieza completa de entidades

### 📊 Resultado
- [ ] ✅ APROBADO
- [ ] ❌ FALLIDO - Detalles: _______________

---

## 🔧 FIX #14: CLEANUP COMPLETO FINAL

### ✅ Verificaciones de Limpieza

- [ ] **Método `finalizarEvento()` completo**
  - [ ] Cancela todas las BukkitTask activas:
    - mainTask
    - manchasTask
    - spawnTask
    - oleadaTask
    - itemSupplyTask
  - [ ] Elimina todas las entidades del evento (Set `entidadesEvento`)
  - [ ] Elimina BossBar del Núcleo
  - [ ] Elimina BossBar del Guardián
  - [ ] Limpia localizaciones de manchas (`manchasLocations`)
  - [ ] Limpia localizaciones de anclas (`anclaLocations`)
  - [ ] Limpia estructuras generadas (anclas, arena, símbolo)
  - [ ] Restaura tiempo de Minecraft a normal
  - [ ] Restaura clima a normal
  - [ ] Elimina efectos de poción de jugadores
  - [ ] Desregistra listener (`EcoSombrasListener`)
  - [ ] Resetea contadores y flags:
    - manchasActivas = 0
    - sombrasLargasMuertas = 0
    - nucleoTeleportes = 0
    - oleadaActual = 0
    - guardianSpawneado = false
    - guardianDerrotado = false
    - anclasSelladas.clear()
    - participacionSombras.clear()
    - participacionAnclas.clear()
    - participacionGuardian.clear()
    - participantesOriginales.clear()

- [ ] **Limpieza automática al detener evento**
  - Comando `/avo eco_sombras stop` llama a `finalizarEvento()`
  - No quedan entidades fantasma
  - No quedan tasks ejecutándose

- [ ] **Limpieza en caso de error**
  - Si el evento falla, la limpieza se ejecuta igual
  - Try-catch maneja excepciones

- [ ] **Verificación post-limpieza**
  - Ninguna entidad del evento permanece
  - Ninguna task del evento permanece
  - El área vuelve a estado normal
  - Los jugadores no tienen efectos permanentes

### 📊 Resultado
- [ ] ✅ APROBADO
- [ ] ❌ FALLIDO - Detalles: _______________

---

## 📊 RESUMEN DE TIEMPOS

| Acto | Nombre | Duración Configurada | Trigger | ¿Pasa automáticamente? |
|------|--------|---------------------|---------|------------------------|
| 0 | Activación | 60 seg | Manual | → Acto 1 |
| 1 | Manchas | 900 seg (15 min) | Auto | Solo si manchas < 3 |
| 2 | Sombras Largas | 1200 seg (20 min) | Condición | Solo si 15+ sombras muertas |
| 3 | Núcleo | 1200 seg (20 min) | Condición | Solo si núcleo ≤ 40% HP |
| 4 | Anclas | 900 seg (15 min) | Condición | Solo si 5 anclas selladas + núcleo muerto |
| 5 | Ritual | 1800 seg (30 min) | Condición | Solo si guardián muerto |
| 6 | Cliffhanger | 120 seg (2 min) | Auto | → Fin evento |

**Total esperado:** ~103 minutos (1h 43min) si se completan todos los objetivos  
**Máximo configurado:** 120 minutos (2 horas)

---

## 🎯 CRITERIOS DE ÉXITO GENERAL

- [ ] **Todos los actos progresan correctamente**
- [ ] **Transiciones automáticas funcionan**
- [ ] **Condiciones de progreso detectan correctamente**
- [ ] **No hay crashes ni errores en consola**
- [ ] **Performance es aceptable (TPS ≥ 18)**
- [ ] **Partículas y sonidos no causan lag**
- [ ] **Los jugadores pueden completar el evento**
- [ ] **Recompensas se otorgan correctamente**
- [ ] **Limpieza final completa y sin errores**

---

## 📝 NOTAS DE TESTING

### Diferencias encontradas
```
- Acto 1: Usa SILVERFISH en código, pero YML especifica sistema de partículas
- [Agregar más diferencias aquí]
```

### Bugs encontrados
```
[Lista de bugs durante testing]
```

### Sugerencias de mejora
```
[Mejoras propuestas]
```

---

**Última actualización:** 2025-11-18  
**Tester:** _______________  
**Versión del plugin:** 1.15.0  
**Versión de Minecraft:** 1.21.8
