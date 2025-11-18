# 🎬 CHECKLIST EFECTOS CINEMATOGRÁFICOS - ECO DE LAS SOMBRAS LARGAS

## 🎥 OBJETIVO: Experiencia tipo película/serie

---

## 🌟 EFECTOS IMPLEMENTADOS

### 🎭 **Efecto Cinematográfico Base** (`efectoCinematico()`)
- ✅ Título en pantalla grande (configurable fadeIn/stay/fadeOut)
- ✅ Sonido de trueno atmosférico (ENTITY_LIGHTNING_BOLT_THUNDER)
- ✅ Sonido ambiente de cueva (AMBIENT_CAVE)
- ✅ Partículas de humo denso (LARGE_SMOKE) alrededor del jugador

**Usado en:**
- Transición SOMBRAS → ANCLAS (20 sombras muertas)
- Transición ANCLAS → NUCLEO (5 anclas selladas)
- Transición NUCLEO → RITUAL (núcleo destruido)
- Cada oleada del ritual (Oleada 1/3, 2/3, 3/3)
- Spawn del Guardián del Umbral
- Transición RITUAL → CLIFFHANGER (guardián derrotado)

---

## 📋 CHECKLIST DE EFECTOS POR ACTO

### **ACTO 0: ACTIVACION**
**Efectos actuales:**
- [ ] Música ambiental tensa (bucle continuo)
- [ ] Partículas de niebla oscura periódicas
- [ ] Sonidos distantes ominosos cada 30s
- [ ] Texto en chat con efecto de "eco" (repetición gradual)

**⚠️ NECESITA MEJORA:**
- [ ] **Fade in** lento al iniciar (pantalla negra → visible)
- [ ] **Camera shake** suave al inicio
- [ ] **Sonido de respiración** profunda al fondo
- [ ] **Partículas de portal** flotando en el aire

---

### **ACTO 1: MANCHAS**
**Efectos actuales:**
- ✅ Manchas de oscuridad spawn con partículas SMOKE
- [ ] Sonido de portal al aparecer mancha

**⚠️ NECESITA MEJORA:**
- [ ] **Distorsión visual** cuando te acercas a una mancha (partículas SPELL_WITCH)
- [ ] **Sonido de latido** aumentando cerca de manchas
- [ ] **Slow motion** simulado (slowness I por 0.5s al pasar por mancha)
- [ ] **Pantalla oscurecida** (blindness flash al tocar mancha)
- [ ] **Efecto de rayos** verticales desde manchas (FLAME particles)

---

### **ACTO 2: SOMBRAS LARGAS**
**Efectos actuales:**
- ✅ Spawn con partículas LARGE_SMOKE
- ✅ Sonido ENTITY_PHANTOM_DEATH al morir
- ✅ Partículas SMOKE al matar

**⚠️ NECESITA MEJORA:**
- [ ] **Slow motion** al spawnearse cada sombra (2 segundos congelada)
- [ ] **Camera zoom** dramático (FOV change simulado con potion effect)
- [ ] **Sonido de susurros** distorsionados al acercarse
- [ ] **Partículas de distorsión** constantes alrededor (PORTAL + END_ROD)
- [ ] **Efecto de sombra proyectada** en el suelo (SQUID_INK particles)
- [ ] **Screen flash** rojo al recibir daño de sombra
- [ ] **Fade to black momentáneo** cuando alcanzas 20 kills

---

### **ACTO 3: ANCLAS**
**Efectos actuales:**
- ✅ Partículas END_ROD al sellar
- ✅ Sonido BLOCK_RESPAWN_ANCHOR_CHARGE
- ✅ Partículas FLASH al sellar

**⚠️ NECESITA MEJORA:**
- [ ] **Rayo del cielo** al spawn de cada ancla (FLASH + EXPLOSION_EMITTER vertical)
- [ ] **Pulso de energía** expandiéndose desde ancla (círculo de partículas)
- [ ] **Sonido de cristal resonando** (BLOCK_AMETHYST_BLOCK_CHIME)
- [ ] **Beam de luz** conectando ancla con el cielo (continuous END_ROD)
- [ ] **Screen shake fuerte** al sellar ancla
- [ ] **Fade to white** al sellar la 5ta ancla
- [ ] **Cámara lenta** (slowness II por 3s) en la transición a NUCLEO

---

### **ACTO 4: NÚCLEO**
**Efectos actuales:**
- ✅ Partículas PORTAL constantes
- ✅ Partículas REVERSE_PORTAL
- ✅ Sonido BLOCK_PORTAL_AMBIENT cada 5s
- ✅ Teleportación con EXPLOSION_EMITTER

**⚠️ NECESITA MEJORA:**
- [ ] **Spawn cinematográfico del núcleo:**
  - Fade to black total (3s)
  - Sonido de corazón latiendo (ENTITY_WARDEN_HEARTBEAT)
  - Explosion masiva de partículas (EXPLOSION_EMITTER x10)
  - Camera shake extremo
  - Fade in lento revelando núcleo
  
- [ ] **Durante la pelea:**
  - Partículas de distorsión espacial (SCULK_SOUL)
  - Pulsos de energía cada 10s (onda expansiva de partículas)
  - Sonido de viento intenso (WEATHER_RAIN + pitch alterado)
  - Screen tint oscuro aumentando con daño (DARKNESS effect)
  
- [ ] **Al destruir núcleo:**
  - Slow motion extremo (5 segundos)
  - Screen flash blanco cegador
  - Explosión masiva de partículas (todos los tipos)
  - Sonido de vidrio rompiéndose + explosión
  - Camera shake máximo
  - Fade to black antes de RITUAL

---

### **ACTO 5: RITUAL**
**Efectos actuales:**
- ✅ Arena generada con BLACKSTONE
- ✅ Sonido ENTITY_WITHER_SPAWN al inicio
- ✅ Título de oleadas ("OLEADA X/3")
- ✅ Spawn del Guardián con efecto cinematográfico

**⚠️ NECESITA MEJORA:**
- [ ] **Generación de arena:**
  - Time-lapse de construcción (bloques aparecen gradualmente)
  - Sonido de piedra cayendo por cada bloque
  - Partículas LAVA al completar círculo
  - Barrera de fuego alrededor (FLAME wall)
  
- [ ] **Spawn de cada oleada:**
  - Fade in/out rápido
  - Título con efecto de "glitch" (§k)
  - Sonido de alarma (NOTE_BLOCK_BELL spam)
  - Partículas EXPLOSION en puntos de spawn
  - Camera shake medio
  
- [ ] **Spawn del Guardián:**
  - Slow motion total (5s)
  - Pantalla negra → flash blanco → guardián visible
  - Rayo cayendo del cielo (FLASH + EXPLOSION_EMITTER)
  - Sonido ENTITY_WITHER_SPAWN + ENTITY_ENDER_DRAGON_GROWL
  - Círculo de fuego expandiéndose desde guardián
  - Boss bar con efecto pulsante
  - Título épico: "§5§l⚔ GUARDIÁN DEL UMBRAL ⚔"
  
- [ ] **Durante combate con Guardián:**
  - Partículas SOUL_FIRE_FLAME siguiendo al guardián
  - Sonido de espadas chocando al golpear (ITEM_SHIELD_BLOCK)
  - Screen shake al recibir golpe fuerte
  - Partículas de impacto al golpearlo (CRIT + SWEEP_ATTACK)
  - Efecto de "enraged" a 50% vida (velocidad aumenta, partículas LAVA)
  
- [ ] **Muerte del Guardián:**
  - **SLOW MOTION EXTREMO** (10 segundos)
  - Explosión de partículas multicolor
  - Sonido ENTITY_WITHER_DEATH + ENTITY_ENDER_DRAGON_DEATH
  - Screen flash violeta
  - Camera shake prolongado
  - Fade to black lento (5s)
  - Silencio absoluto por 3s antes de CLIFFHANGER

---

### **ACTO 6: CLIFFHANGER**
**Efectos actuales:**
- ✅ Mensaje "...silencio..."
- ✅ Sonido AMBIENT_CAVE
- ✅ Formación de símbolo con CRYING_OBSIDIAN
- ✅ Monólogo del Observador (8 líneas)
- ✅ Figura misteriosa spawn
- ✅ Sonido ENTITY_WITHER_SPAWN final

**⚠️ NECESITA MEJORA:**
- [ ] **Inicio:**
  - Pantalla negra total por 5s
  - Sonido de viento lejano (WEATHER_RAIN + pitch 0.1)
  - Fade in MUY lento (10s)
  - Solo sonidos ambientales (sin música)
  
- [ ] **Formación del símbolo:**
  - Time-lapse de construcción (bloques aparecen uno por uno)
  - Partículas REVERSE_PORTAL en cada bloque
  - Sonido de END_PORTAL al completarse
  - Beam de luz violeta disparando al cielo
  
- [ ] **Monólogo del Observador:**
  - Cada línea con fade in/out en pantalla
  - Sonido de susurros distorsionados de fondo
  - Partículas SPELL_WITCH flotando
  - Screen tint oscuro progresivo
  - Texto con efecto de "typing" (aparece letra por letra)
  
- [ ] **Aparición de figura misteriosa:**
  - Fade in gradual de la entidad (invisible → visible)
  - Distorsión visual extrema (PORTAL + END_ROD masivo)
  - Sonido de múltiples voces superpuestas
  - Camera shake intenso
  - Screen flash negro/blanco alternado
  - **Slow motion** al verla por primera vez
  
- [ ] **Cierre:**
  - Figura desaparece con FLASH
  - Screen fade to black total
  - Sonido ENTITY_WITHER_SPAWN + eco prolongado
  - Pantalla negra por 5s
  - Créditos tipo película (opcional)
  - "§8§l...continuará..."

---

## 🎨 EFECTOS ADICIONALES RECOMENDADOS

### **Transiciones entre actos:**
- [ ] Fade to black universal (2-3s)
- [ ] Título del nuevo acto en pantalla
- [ ] Sonido ambiental específico por acto
- [ ] Partículas de transición (PORTAL spiral)

### **Ambient continuo:**
- [ ] Música de fondo dinámica (cambia por acto)
- [ ] Sonidos ambientales aleatorios (susurros, pasos, respiración)
- [ ] Partículas flotantes constantes (diferente por acto)

### **Feedback al jugador:**
- [ ] **Daño recibido:**
  - Screen shake proporcional al daño
  - Screen flash rojo
  - Sonido de impacto (ENTITY_PLAYER_HURT)
  
- [ ] **Kill confirmado:**
  - Slow motion breve (0.5s)
  - Partículas de kill (SWEEP_ATTACK)
  - Sonido satisfactorio (EXPERIENCE_ORB_PICKUP)
  
- [ ] **Progreso de acto:**
  - Título pequeño con progreso (ej: "15/20 Sombras")
  - Barra de progreso en action bar
  - Sonido de checkpoint al alcanzar hito

### **Camera effects:**
- [ ] **Zoom in/out** (FOV change con potion effects)
- [ ] **Blur** (NAUSEA breve)
- [ ] **Shake** (movimiento de cámara rápido)
- [ ] **Tilt** (rotación simulada con efectos visuales)

### **Post-processing effects:**
- [ ] **Vignette** (oscuridad en bordes de pantalla)
- [ ] **Color grading** (tint general dependiendo del acto)
- [ ] **Bloom** (resplandor en elementos clave)
- [ ] **Chromatic aberration** (distorsión de color en momentos climáticos)

---

## 📊 PRIORIDAD DE IMPLEMENTACIÓN

### **🔴 ALTA PRIORIDAD (Máximo impacto):**
1. Slow motion en momentos clave (guardián spawn/muerte, transiciones)
2. Screen fades (black/white) entre actos
3. Títulos cinematográficos más elaborados
4. Explosiones de partículas masivas
5. Camera shake en eventos importantes

### **🟡 MEDIA PRIORIDAD:**
6. Sonidos ambientales continuos
7. Partículas de ambiente constantes
8. Efectos de distorsión visual
9. Feedback visual de progreso
10. Time-lapse de construcciones

### **🟢 BAJA PRIORIDAD (Polish):**
11. Música dinámica por acto
12. Créditos finales
13. Color grading
14. Efectos de post-procesamiento avanzados
15. Typing effect en texto

---

## 🛠️ TÉCNICAS DE IMPLEMENTACIÓN

### **Slow Motion:**
```java
// Aplicar slowness + mining fatigue
player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duracionTicks, 9, false, false));
player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, duracionTicks, 5, false, false));

// También ralentizar entidades cercanas
for (Entity entity : player.getNearbyEntities(50, 50, 50)) {
    if (entity instanceof LivingEntity) {
        ((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duracionTicks, 9));
    }
}
```

### **Screen Fade:**
```java
// Fade to black
player.sendTitle("", "", 20, 100, 20); // Título vacío con tiempos largos
player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duracionTicks, 1, false, false));

// Fade to white
player.sendTitle("§f█████████████████", "§f█████████████████", fadeIn, stay, fadeOut);
```

### **Camera Shake:**
```java
// Simular con velocidad
Vector originalVelocity = player.getVelocity();
new BukkitRunnable() {
    int count = 0;
    @Override
    public void run() {
        if (count >= 20) { // 1 segundo
            player.setVelocity(originalVelocity);
            cancel();
            return;
        }
        Vector shake = new Vector(
            (Math.random() - 0.5) * 0.5,
            (Math.random() - 0.5) * 0.2,
            (Math.random() - 0.5) * 0.5
        );
        player.setVelocity(shake);
        count++;
    }
}.runTaskTimer(plugin, 0L, 1L);
```

### **Partículas Masivas:**
```java
// Explosión de múltiples tipos
Location loc = entity.getLocation();
loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 10, 2, 2, 2);
loc.getWorld().spawnParticle(Particle.FLASH, loc, 5);
loc.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc, 100, 3, 3, 3, 0.2);
loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 50, 2, 2, 2, 0.1);
loc.getWorld().spawnParticle(Particle.END_ROD, loc, 80, 2, 2, 2, 0.15);
```

### **Typing Effect:**
```java
String mensaje = "El eco persiste. La sombra recuerda.";
AtomicInteger index = new AtomicInteger(0);
new BukkitRunnable() {
    @Override
    public void run() {
        if (index.get() >= mensaje.length()) {
            cancel();
            return;
        }
        String partial = mensaje.substring(0, index.incrementAndGet());
        player.sendMessage("§7§o" + partial);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.5f);
    }
}.runTaskTimer(plugin, 0L, 2L); // 1 letra cada 0.1s
```

---

## ✅ RESUMEN EJECUTIVO

**Efectos cinematográficos básicos:** ✅ Implementados  
**Transiciones automáticas:** ✅ Implementadas  
**Efectos avanzados:** ⚠️ Pendientes (50+ mejoras listadas)

**Próximos pasos:**
1. Compilar y probar transiciones automáticas
2. Implementar slow motion en momentos clave
3. Mejorar spawn del Guardián con efectos épicos
4. Agregar screen fades entre todos los actos
5. Partículas masivas en explosiones/transiciones

**Objetivo final:** Que cada transición y momento clave parezca una escena de película/anime épico.
