# ✅ CHECKLIST COMPLETO - ECO DE LAS SOMBRAS LARGAS

## 📋 VERIFICACIÓN DE COMANDOS

### 🎯 Comando: `/avo eco_sombras start`

**Pruebas de validación:**
- [ ] Con 0 jugadores → ❌ "Necesitas al menos X jugadores" (X lee de eco_sombras.yml)
- [ ] Con 1 jugador → ❌ "Necesitas al menos X jugadores"
- [ ] Con 2 jugadores → ❌ "Necesitas al menos X jugadores"
- [ ] Con 3+ jugadores → ✅ Evento inicia correctamente
- [ ] Evento ya activo → ❌ "El evento ya está en progreso"

**Verificar después del start:**
- [ ] Acto actual = `ACTIVACION` (0)
- [ ] Mensaje global broadcast
- [ ] Jugadores reciben título de inicio
- [ ] `/avo eco_sombras info` muestra estado correcto

---

### 🛑 Comando: `/avo eco_sombras stop`

**Pruebas de limpieza:**
- [ ] Evento no activo → ❌ "El evento no está activo"
- [ ] Evento activo → ✅ Detiene correctamente

**Verificar limpieza completa:**
- [ ] Todas las entidades del evento removidas (Sombras Largas, Anclas, Núcleo, Guardian)
- [ ] Todas las tasks canceladas (manchasTask, spawnTask, oleadaTask)
- [ ] Lista `entidadesEvento` vaciada
- [ ] Lista `manchasLocations` vaciada
- [ ] Contadores reseteados: `sombrasLargasMuertas = 0`, `anclasSelladas = 0`, `oleadaActual = 0`
- [ ] Arena del ritual limpiada (si estaba en Act 5)
- [ ] Acto regresa a `ACTIVACION`

---

### 🔄 Comando: `/avo eco_sombras fase <0-6>`

**Forzar cada acto individual:**
- [ ] `fase 0` → Transiciona a **ACTIVACION** 
- [ ] `fase 1` → Transiciona a **MANIFESTACION** (spawn Sombras Largas)
- [ ] `fase 2` → Transiciona a **ANCLAS** (spawn 5 anclas)
- [ ] `fase 3` → Transiciona a **NUCLEO** (spawn núcleo central)
- [ ] `fase 4` → Transiciona a **INVASION** (oleadas intensificadas)
- [ ] `fase 5` → Transiciona a **RITUAL** (genera arena con Guardian)
- [ ] `fase 6` → Transiciona a **CLIFFHANGER** (formación de símbolos, monólogo)

**Verificar limpieza entre transiciones:**
- [ ] Al cambiar de acto 1→2: Sombras Largas del acto 1 removidas
- [ ] Al cambiar de acto 2→3: Anclas del acto 2 removidas
- [ ] Al cambiar de acto 3→4: Núcleo del acto 3 removido
- [ ] Al cambiar de acto 4→5: Oleadas del acto 4 canceladas
- [ ] Al cambiar de acto 5→6: Guardian removido, arena limpiada
- [ ] Tasks anteriores canceladas antes de nueva transición

**Mensajes esperados:**
```
✓ Forzando transición al acto: [NOMBRE_ACTO]
✓ Acto actual: [NOMBRE_ACTO]
```

---

### ⏭️ Comando: `/avo eco_sombras next` / `siguiente`

**Progresión secuencial:**
- [ ] Desde **ACTIVACION** → avanza a **MANIFESTACION**
- [ ] Desde **MANIFESTACION** → avanza a **ANCLAS**
- [ ] Desde **ANCLAS** → avanza a **NUCLEO**
- [ ] Desde **NUCLEO** → avanza a **INVASION**
- [ ] Desde **INVASION** → avanza a **RITUAL**
- [ ] Desde **RITUAL** → avanza a **CLIFFHANGER**
- [ ] Desde **CLIFFHANGER** → ❌ "Ya estás en el último acto"

**Verificar en cada avance:**
- [ ] Limpieza de entidades del acto anterior
- [ ] Spawn correcto de entidades del nuevo acto
- [ ] Mensaje muestra transición: `"ACTO_ANTERIOR → ACTO_NUEVO"`

---

### 📊 Comando: `/avo eco_sombras info` / `status`

**Estadísticas mostradas:**
- [ ] **Acto actual:** Nombre del acto (ej: MANIFESTACION, ANCLAS, etc.)
- [ ] **Jugadores:** Cantidad actual de jugadores en el evento
- [ ] **Sombras eliminadas:** Contador real (no "N/A")
- [ ] **Anclas selladas:** X/5 con contador real
- [ ] **Oleada actual:** X/3 con contador real (en actos 4-5)
- [ ] **Anclas activas:** Lista de coordenadas (en acto 2-3)

**Verificar en diferentes actos:**
- [ ] **ACTIVACION:** Muestra 0/0/0 en contadores
- [ ] **MANIFESTACION:** Incrementa contador de sombras al matar
- [ ] **ANCLAS:** Muestra 5 anclas activas, incrementa selladas al sellar
- [ ] **NUCLEO:** Muestra coordenadas del núcleo
- [ ] **INVASION:** Muestra oleada 1/3, 2/3, 3/3
- [ ] **RITUAL:** Muestra oleada actual durante el combate
- [ ] **CLIFFHANGER:** Muestra estado final

---

### 🔐 Comando: `/avo eco_sombras ancla <1-5>`

**Pruebas de sellado:**
- [ ] Evento no activo → ❌ "El evento no está activo"
- [ ] Acto incorrecto → ❌ "Solo puedes sellar anclas en el acto ANCLAS o NUCLEO"
- [ ] `ancla 1` → ✅ Ancla 1 sellada, entidad removida
- [ ] `ancla 2` → ✅ Ancla 2 sellada, entidad removida
- [ ] `ancla 3` → ✅ Ancla 3 sellada, entidad removida
- [ ] `ancla 4` → ✅ Ancla 4 sellada, entidad removida
- [ ] `ancla 5` → ✅ Ancla 5 sellada, transiciona a NUCLEO automáticamente

**Verificar progresión automática:**
- [ ] Al sellar la 5ta ancla → Transición automática al acto NUCLEO
- [ ] Mensaje broadcast: "¡Todas las anclas han sido selladas!"
- [ ] Spawn del Núcleo Central

---

### 💀 Comando: `/avo eco_sombras nucleo`

**Subcomandos del núcleo:**

#### `nucleo spawn <x> <y> <z>`
- [ ] Evento no activo → ❌ Error
- [ ] Coordenadas inválidas → ❌ Error de formato
- [ ] Coordenadas válidas → ✅ Núcleo spawneado en ubicación

#### `nucleo tp`
- [ ] Núcleo no spawneado → ❌ "El núcleo no está spawneado"
- [ ] Núcleo activo → ✅ Teletransporta al jugador

#### `nucleo damage <cantidad>`
- [ ] Núcleo no spawneado → ❌ Error
- [ ] Daño negativo → ❌ Error
- [ ] Daño válido → ✅ Reduce vida del núcleo
- [ ] Vida llega a 0 → Transiciona a INVASION automáticamente

---

## 🎮 VERIFICACIÓN DE GAMEPLAY

### Act 0: ACTIVACION
- [ ] Jugadores pueden moverse libremente
- [ ] No hay spawns hostiles aún
- [ ] Chat muestra mensajes atmosféricos cada 30s

### Act 1: MANIFESTACION
- [ ] Spawn de Sombras Largas cada 45s
- [ ] Manchas de oscuridad aparecen en el suelo
- [ ] Matar 20 Sombras Largas → Progresión automática a ANCLAS
- [ ] Efectos de partículas en las manchas

### Act 2: ANCLAS
- [ ] 5 Anclas spawn en ubicaciones aleatorias
- [ ] Cada ancla tiene nombre "Ancla Dimensional #X"
- [ ] Hologramas de coordenadas visibles
- [ ] Sellar todas las anclas → Progresión a NUCLEO

### Act 3: NUCLEO
- [ ] Núcleo Central spawn en ubicación definida
- [ ] Boss bar visible: "Núcleo de la Sombra - X♥"
- [ ] Núcleo tiene 200 HP
- [ ] Aura de daño (5 bloques): 2♥ cada 2s
- [ ] Reducir vida a 0 → Progresión a INVASION

### Act 4: INVASION
- [ ] 3 oleadas progresivas
- [ ] Oleada 1: 8 Sombras Largas
- [ ] Oleada 2: 12 Sombras Largas + Vindicators
- [ ] Oleada 3: 15 Sombras Largas + Ravagers + Evokers
- [ ] Sobrevivir las 3 oleadas → Progresión a RITUAL

### Act 5: RITUAL
- [ ] Arena se genera en ubicación definida
- [ ] Guardian del Umbral spawn con equipo Netherite
- [ ] Guardian tiene 300 HP
- [ ] 3 oleadas durante el combate:
  - Oleada 1: 6 Sombras, 3 Zombies reforzados
  - Oleada 2: 8 Sombras, 4 Esqueletos reforzados
  - Oleada 3: 10 Sombras, Evocadores, Vindicators
- [ ] Derrotar Guardian → Progresión a CLIFFHANGER

### Act 6: CLIFFHANGER
- [ ] Formación de símbolos en el suelo (wool patterns)
- [ ] Monólogo del Observador en chat (8 líneas)
- [ ] Figura misteriosa spawn brevemente
- [ ] Sonido final ENTITY_WITHER_SPAWN
- [ ] Evento termina después del monólogo

---

## 🔧 VERIFICACIÓN TÉCNICA

### Limpieza de entidades
- [ ] `limpiarEntidadesActoAnterior()` cancela todas las tasks activas
- [ ] Todas las entidades en `entidadesEvento` son removidas
- [ ] Listas auxiliares vaciadas correctamente

### Configuración YML
- [ ] `eco_sombras.yml` existe en `src/main/resources/`
- [ ] `metadata.jugadores_minimos` lee correctamente
- [ ] Valor por defecto = 3 si no está definido

### Getters públicos
- [ ] `getJugadoresMinimos()` retorna valor de config
- [ ] `getSombrasLargasMuertas()` retorna contador
- [ ] `getAnclasSelladas()` retorna anclas selladas
- [ ] `getOleadaActual()` retorna oleada en actos 4-5

### Transiciones automáticas
- [ ] 20 Sombras muertas → ANCLAS
- [ ] 5 Anclas selladas → NUCLEO
- [ ] Núcleo destruido → INVASION
- [ ] 3 Oleadas completadas → RITUAL
- [ ] Guardian derrotado → CLIFFHANGER

---

## 🐛 CASOS EDGE A VERIFICAR

- [ ] Forzar acto mientras hay tasks activas → Limpieza correcta
- [ ] Jugadores salen del servidor durante evento → No crashea
- [ ] Stop durante combate intenso → Todas las entidades removidas
- [ ] Transición rápida entre actos (fase + next consecutivos) → Sin duplicados
- [ ] Arena del ritual se genera en chunk no cargado → Chunk se fuerza a cargar
- [ ] Múltiples comandos simultáneos → Thread-safe
- [ ] Reinicio del servidor durante evento → Estado se resetea correctamente

---

## 📦 CHECKLIST DE DEPLOYMENT

- [x] Código compilado sin errores
- [x] JAR generado: `target/Apocalipsis-1.15.0.jar`
- [ ] Archivo subido al servidor
- [ ] Reinicio del servidor
- [ ] `/avo eco_sombras info` funciona sin evento activo
- [ ] Prueba completa del ciclo: start → fase 1 → fase 2 → ... → fase 6 → stop
- [ ] Verificación de logs sin errores
- [ ] Backup del mundo antes de pruebas

---

## 📝 NOTAS IMPORTANTES

### Comandos implementados:
- ✅ `start` - Inicia evento (validación dinámica de jugadores)
- ✅ `stop` - Detiene y limpia completamente
- ✅ `fase <0-6>` - Fuerza acto específico con limpieza
- ✅ `next/siguiente` - Avanza secuencialmente
- ✅ `info/status` - Muestra estadísticas reales
- ✅ `ancla <1-5>` - Sella ancla dimensional
- ✅ `nucleo spawn/tp/damage` - Gestiona núcleo central

### Cambios recientes:
- `jugadores_minimos` ahora configurable en `eco_sombras.yml`
- `forzarActo()` limpia entidades del acto anterior
- `avanzarActo()` permite progresión secuencial
- Getters públicos para exponer contadores internos
- Comandos ahora ejecutan transiciones reales (antes solo mostraban mensajes)

### Limpieza automática:
- `manchasTask` cancelado
- `spawnTask` cancelado
- `oleadaTask` cancelado
- Todas las entidades en `entidadesEvento` removidas
- `manchasLocations` vaciada

---

**Versión:** 1.15.0  
**Fecha:** 18/Nov/2025  
**Estado:** ✅ Compilado y listo para pruebas
