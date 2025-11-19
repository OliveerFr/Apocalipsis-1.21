# 🌪️ CHECKLIST DE MEJORAS: Sistema de Desastres

**Fecha:** 19 de Noviembre de 2025  
**Versión Target:** 1.17.0  
**Prioridad:** 🔴 ALTA  
**Tiempo Estimado:** 8-12 horas

---

## 📋 RESUMEN EJECUTIVO

### 🎯 Objetivos Principales
1. **Fases más dinámicas** → Cada fase con mecánicas únicas y progresión clara
2. **Mejoras visuales impactantes** → Partículas, sonidos, efectos ambientales
3. **Mayor inmersión** → Advertencias tempranas, transiciones suaves, feedback constante
4. **Desafío escalado** → Dificultad progresiva a lo largo de las fases
5. **Recompensas por supervivencia** → Incentivos para sobrevivir cada fase

### 📊 Estado Actual de Desastres
- **Terremoto** (`TerremotoNew.java`) - Sistema básico de fases
- **Huracán** (`HuracanNew.java`) - Sistema básico de fases  
- **Lluvia de Fuego** (`LluviaFuegoNew.java`) - Sistema básico de fases

---

## 🎨 SPRINT 1: MEJORAS VISUALES Y SONORAS (3-4 horas)

### ✅ Tarea 1.1: Sistema de Pre-Advertencia (15-30 segundos antes)
**Estado:** ⏳ PENDIENTE  
**Prioridad:** 🔴 ALTA  
**Archivos:** `TerremotoNew.java`, `HuracanNew.java`, `LluviaFuegoNew.java`

**Implementación:**
```java
// Nuevo método en DisasterBase.java
protected void showPreWarning() {
    // 30 segundos antes del desastre
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
        broadcastWarning("§6⚠ §e§lALERTA TEMPRANA §6⚠");
        broadcastWarning("§7Un desastre se aproxima...");
        playGlobalSound(Sound.BLOCK_BELL_USE, 1.0f, 0.5f);
        
        // Partículas en el cielo para todos
        for (Player p : Bukkit.getOnlinePlayers()) {
            spawnWarningParticles(p.getLocation().add(0, 20, 0));
        }
    }, -600L); // 30 segundos antes (600 ticks)
}

private void spawnWarningParticles(Location loc) {
    World world = loc.getWorld();
    world.spawnParticle(Particle.SMOKE_LARGE, loc, 50, 10, 5, 10, 0.1);
    world.spawnParticle(Particle.CLOUD, loc, 30, 10, 5, 10, 0.05);
}
```

**Beneficio:** Los jugadores tienen tiempo para prepararse, aumenta la inmersión

---

### ✅ Tarea 1.2: Efectos ambientales por desastre
**Estado:** ⏳ PENDIENTE  
**Prioridad:** 🔴 ALTA  
**Archivos:** Cada clase de desastre

**TERREMOTO - Efectos Ambientales:**
```java
// En cada fase del terremoto
private void applyEarthquakeAmbience(int phase) {
    for (Player p : Bukkit.getOnlinePlayers()) {
        // Sonido ambiental constante
        p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 0.5f, 0.8f);
        
        // Partículas de polvo en el suelo
        Location ground = p.getLocation().subtract(0, 1, 0);
        p.spawnParticle(Particle.BLOCK_DUST, ground, 20, 2, 0.1, 2, 
            Material.STONE.createBlockData());
        
        // Fase 3+: Grietas visuales
        if (phase >= 3) {
            spawnCrackParticles(ground);
        }
    }
}

private void spawnCrackParticles(Location loc) {
    loc.getWorld().spawnParticle(Particle.BLOCK_CRACK, loc, 30, 1, 0, 1,
        Material.CRACKED_STONE_BRICKS.createBlockData());
}
```

**HURACÁN - Efectos Ambientales:**
```java
private void applyHurricaneAmbience(int phase) {
    for (Player p : Bukkit.getOnlinePlayers()) {
        // Viento constante (sonido)
        p.playSound(p.getLocation(), Sound.ITEM_ELYTRA_FLYING, 0.8f, 0.6f);
        
        // Lluvia de partículas
        Location above = p.getLocation().add(0, 15, 0);
        p.spawnParticle(Particle.DRIPPING_WATER, above, 50, 5, 0, 5, 0.5);
        p.spawnParticle(Particle.CLOUD, above, 30, 5, 5, 5, 0.1);
        
        // Fase 3+: Relámpagos más frecuentes
        if (phase >= 3 && Math.random() < 0.3) {
            strikeLightningEffect(p.getLocation().add(
                (Math.random() - 0.5) * 20,
                0,
                (Math.random() - 0.5) * 20
            ));
        }
    }
}
```

**LLUVIA DE FUEGO - Efectos Ambientales:**
```java
private void applyFireRainAmbience(int phase) {
    for (Player p : Bukkit.getOnlinePlayers()) {
        // Sonido de fuego crepitante
        p.playSound(p.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 0.6f, 0.8f);
        
        // Humo ascendente
        Location ground = p.getLocation();
        p.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, ground, 10, 3, 0, 3, 0.05);
        
        // Brasas cayendo
        Location sky = p.getLocation().add(0, 20, 0);
        p.spawnParticle(Particle.FLAME, sky, 40, 5, 5, 5, 0.1);
        p.spawnParticle(Particle.LAVA, sky, 20, 5, 2, 5, 0.05);
        
        // Fase 3+: Más intenso
        if (phase >= 3) {
            p.spawnParticle(Particle.SOUL_FIRE_FLAME, sky, 30, 5, 5, 5, 0.15);
        }
    }
}
```

**Beneficio:** Cada desastre se siente único y amenazante

---

### ✅ Tarea 1.3: Títulos y subtítulos mejorados por fase
**Estado:** ⏳ PENDIENTE  
**Prioridad:** 🟡 MEDIA  
**Archivos:** Cada clase de desastre

**Sistema de Títulos Dinámicos:**
```java
// En DisasterBase.java
protected void showPhaseTitle(int phase, String disasterName) {
    String[] phaseNames = {
        "§7Temblor Leve", 
        "§eSacudida Moderada", 
        "§6Terremoto Fuerte",
        "§cTerremoto Severo",
        "§4§lTERREMOTO CATASTRÓFICO"
    };
    
    String title = "§l" + disasterName.toUpperCase();
    String subtitle = phaseNames[phase - 1];
    
    for (Player p : Bukkit.getOnlinePlayers()) {
        p.sendTitle(title, subtitle, 10, 40, 20);
        
        // Sonido según fase
        Sound sound = phase < 3 ? Sound.BLOCK_NOTE_BLOCK_BASS : Sound.ENTITY_ENDER_DRAGON_GROWL;
        float pitch = 0.5f + (phase * 0.2f);
        p.playSound(p.getLocation(), sound, 1.0f, pitch);
    }
}
```

**Beneficio:** Comunicación clara de la progresión del desastre

---

### ✅ Tarea 1.4: Bossbar de progreso del desastre
**Estado:** ⏳ PENDIENTE  
**Prioridad:** 🟡 MEDIA  
**Archivos:** `DisasterBase.java`, cada desastre

**Implementación:**
```java
// En DisasterBase.java
protected BossBar disasterBossBar;

protected void createDisasterBossBar(String disasterName) {
    disasterBossBar = Bukkit.createBossBar(
        "§c§l" + disasterName + " §7- §eFase 1/5",
        BarColor.RED,
        BarStyle.SEGMENTED_10
    );
    disasterBossBar.setProgress(0.2); // 20% por fase
    
    for (Player p : Bukkit.getOnlinePlayers()) {
        disasterBossBar.addPlayer(p);
    }
}

protected void updateBossBarPhase(int phase, int totalPhases) {
    if (disasterBossBar != null) {
        double progress = (double) phase / totalPhases;
        disasterBossBar.setProgress(progress);
        
        String phaseName = getPhaseDisplayName(phase);
        disasterBossBar.setTitle("§c§l" + getDisasterName() + " §7- §e" + phaseName);
        
        // Color según intensidad
        if (phase <= 2) {
            disasterBossBar.setColor(BarColor.YELLOW);
        } else if (phase <= 4) {
            disasterBossBar.setColor(BarColor.RED);
        } else {
            disasterBossBar.setColor(BarColor.PURPLE);
        }
    }
}

protected void removeDisasterBossBar() {
    if (disasterBossBar != null) {
        disasterBossBar.removeAll();
        disasterBossBar = null;
    }
}
```

**Beneficio:** Feedback visual constante del estado del desastre

---

## ⚡ SPRINT 2: MECÁNICAS DINÁMICAS POR FASE (3-4 horas)

### ✅ Tarea 2.1: TERREMOTO - Mecánicas por fase
**Estado:** ⏳ PENDIENTE  
**Prioridad:** 🔴 ALTA  
**Archivo:** `TerremotoNew.java`

**FASE 1: Temblor Leve (15s)**
- Sacudida cada 3 segundos (actual)
- **NUEVO:** Caída ocasional de bloques de grava/arena desde altura
- **NUEVO:** Sonido de piedras crujiendo

**FASE 2: Sacudida Moderada (20s)**
- Sacudida cada 2 segundos
- **NUEVO:** Aparición de pequeñas grietas (bloques de aire en el suelo)
- **NUEVO:** Mobs hostiles empiezan a spawnear confundidos
- **NUEVO:** Daño leve por caída (0.5 corazones cada sacudida)

**FASE 3: Terremoto Fuerte (25s)**
- Sacudida cada 1.5 segundos
- **NUEVO:** Grietas más grandes (2x2)
- **NUEVO:** Spawneo de Silverfish desde el suelo
- **NUEVO:** Derrumbes: bloques caen desde altura mayor
- **NUEVO:** Efecto Slowness I por las sacudidas

**FASE 4: Terremoto Severo (30s)**
- Sacudida cada 1 segundo
- **NUEVO:** Fisuras profundas (exponen cuevas)
- **NUEVO:** Spawneo masivo de mobs subterráneos
- **NUEVO:** Destrucción de estructuras débiles (cercas, antorchas, flores)
- **NUEVO:** Daño moderado (1 corazón cada sacudida)

**FASE 5: Terremoto Catastrófico (40s)**
- Sacudida constante (0.5 segundos)
- **NUEVO:** Tsunami de piedras (bloques que se mueven)
- **NUEVO:** Aparición de Iron Golems hostiles "enloquecidos"
- **NUEVO:** Lava puede emerger de grietas profundas
- **NUEVO:** Knockback extremo + Daño alto (2 corazones)

**Código de ejemplo:**
```java
private void phase3Mechanics() {
    // Spawneo de Silverfish desde grietas
    if (tickCount % 40 == 0) { // Cada 2 segundos
        for (Player p : Bukkit.getOnlinePlayers()) {
            Location loc = p.getLocation().subtract(0, 1, 0);
            if (loc.getBlock().getType() == Material.AIR) {
                // Es una grieta, spawn silverfish
                loc.getWorld().spawnEntity(loc, EntityType.SILVERFISH);
            }
        }
    }
    
    // Derrumbes desde arriba
    if (tickCount % 60 == 0) { // Cada 3 segundos
        spawnFallingBlocks(Material.STONE, Material.COBBLESTONE, Material.ANDESITE);
    }
}
```

---

### ✅ Tarea 2.2: HURACÁN - Mecánicas por fase
**Estado:** ⏳ PENDIENTE  
**Prioridad:** 🔴 ALTA  
**Archivo:** `HuracanNew.java`

**FASE 1: Viento Suave (20s)**
- Empuje leve (actual)
- **NUEVO:** Lluvia intensa
- **NUEVO:** Objetos sueltos (items en el suelo) son empujados
- **NUEVO:** Sonido de viento constante

**FASE 2: Viento Moderado (25s)**
- Empuje medio
- **NUEVO:** Proyectiles desviados por el viento
- **NUEVO:** Caída de hojas de árboles
- **NUEVO:** Niebla reducida (visibilidad limitada)

**FASE 3: Tormenta Fuerte (30s)**
- Empuje fuerte + levitación ocasional
- **NUEVO:** Relámpagos frecuentes cerca de jugadores
- **NUEVO:** Lluvia de escombros (bloques ligeros: hojas, tierra)
- **NUEVO:** Mobs voladores spawneados (Phantoms)
- **NUEVO:** Efecto Slowness por resistencia al viento

**FASE 4: Huracán Severo (35s)**
- Empuje muy fuerte + levitación frecuente
- **NUEVO:** Tornados visuales (columnas de partículas)
- **NUEVO:** Spawneo de Vex (representando escombros voladores)
- **NUEVO:** Estructuras ligeras destruidas (antorchas, carteles, flores)
- **NUEVO:** Daño por caída al ser levantado

**FASE 5: Huracán Catastrófico (45s)**
- Empuje extremo + levitación constante
- **NUEVO:** "Ojo del huracán" móvil (zona segura que se mueve)
- **NUEVO:** Mega-relámpagos (múltiples a la vez)
- **NUEVO:** Lluvia de bloques pesados (piedra, madera)
- **NUEVO:** Ender Dragons fantasma (visuales, sin daño real)
- **NUEVO:** Knockback extremo puede lanzar fuera del mundo

**Código de ejemplo:**
```java
private void phase4Mechanics() {
    // Tornado visual
    if (tickCount % 20 == 0) {
        spawnTornadoEffect(getTornadoCenter());
    }
    
    // Spawneo de Vex
    if (tickCount % 100 == 0) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            Location spawnLoc = p.getLocation().add(
                (Math.random() - 0.5) * 10,
                5,
                (Math.random() - 0.5) * 10
            );
            Vex vex = (Vex) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.VEX);
            vex.setCustomName("§7Escombro Volador");
        }
    }
}

private void spawnTornadoEffect(Location center) {
    for (int i = 0; i < 20; i++) {
        double angle = (i / 20.0) * Math.PI * 2;
        double radius = 3.0;
        double x = center.getX() + Math.cos(angle) * radius;
        double z = center.getZ() + Math.sin(angle) * radius;
        
        for (int y = 0; y < 30; y++) {
            Location particleLoc = new Location(center.getWorld(), x, center.getY() + y, z);
            center.getWorld().spawnParticle(Particle.CLOUD, particleLoc, 1, 0.1, 0.1, 0.1, 0.1);
        }
    }
}
```

---

### ✅ Tarea 2.3: LLUVIA DE FUEGO - Mecánicas por fase
**Estado:** ⏳ PENDIENTE  
**Prioridad:** 🔴 ALTA  
**Archivo:** `LluviaFuegoNew.java`

**FASE 1: Chispas (15s)**
- Bolas de fuego ocasionales (actual)
- **NUEVO:** Pequeñas llamas en el suelo (se apagan solas)
- **NUEVO:** Humo denso

**FASE 2: Lluvia Ligera (20s)**
- Más bolas de fuego
- **NUEVO:** Llamas persisten más tiempo
- **NUEVO:** Bloques inflamables empiezan a arder
- **NUEVO:** Daño leve por estar en llamas

**FASE 3: Lluvia Intensa (25s)**
- Bolas de fuego frecuentes
- **NUEVO:** Meteoros pequeños (Fireball entities)
- **NUEVO:** Spawneo de Blazes
- **NUEVO:** Charcos de lava temporales
- **NUEVO:** Efecto Hunger por el calor

**FASE 4: Infierno (30s)**
- Lluvia constante de fuego
- **NUEVO:** Meteoros grandes con explosiones
- **NUEVO:** Suelo se convierte temporalmente en Netherrack
- **NUEVO:** Spawneo de Wither Skeletons
- **NUEVO:** Efecto Wither I por el humo tóxico

**FASE 5: Apocalipsis Ígneo (40s)**
- Infierno total
- **NUEVO:** Columnas de fuego desde el suelo al cielo
- **NUEVO:** Ghasts spawneados
- **NUEVO:** Bloques se convierten en lava temporalmente
- **NUEVO:** "Dragón de Fuego" (Ender Dragon con trail de fuego)
- **NUEVO:** Daño masivo + Burning permanente

**Código de ejemplo:**
```java
private void phase5Mechanics() {
    // Columnas de fuego
    if (tickCount % 30 == 0) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (Math.random() < 0.3) {
                spawnFireColumn(p.getLocation().add(
                    (Math.random() - 0.5) * 15,
                    0,
                    (Math.random() - 0.5) * 15
                ));
            }
        }
    }
    
    // Dragón de fuego
    if (tickCount == 100 && fireDragon == null) {
        Location spawnLoc = getWorldCenter().add(0, 50, 0);
        fireDragon = (EnderDragon) spawnLoc.getWorld().spawnEntity(
            spawnLoc, 
            EntityType.ENDER_DRAGON
        );
        fireDragon.setCustomName("§c§lDragón del Apocalipsis");
        fireDragon.setPhase(EnderDragon.Phase.CIRCLING);
        
        // Trail de fuego
        startFireDragonTrail();
    }
}

private void spawnFireColumn(Location base) {
    for (int y = 0; y < 20; y++) {
        Location loc = base.clone().add(0, y, 0);
        loc.getWorld().spawnParticle(Particle.FLAME, loc, 20, 0.3, 0.3, 0.3, 0.1);
        loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 10, 0.2, 0.2, 0.2, 0.05);
    }
    
    // Daño a jugadores en el área
    for (Player p : Bukkit.getOnlinePlayers()) {
        if (p.getLocation().distance(base) < 2.0) {
            p.damage(4.0);
            p.setFireTicks(100);
        }
    }
}
```

---

## 🎁 SPRINT 3: SISTEMA DE SUPERVIVENCIA Y RECOMPENSAS (2-3 horas)

### ✅ Tarea 3.1: Tracking de supervivencia por fase
**Estado:** ⏳ PENDIENTE  
**Prioridad:** 🟡 MEDIA  
**Archivo:** `DisasterBase.java`

**Implementación:**
```java
// En DisasterBase.java
protected Map<UUID, Integer> playerSurvivalPhases = new HashMap<>();
protected Map<UUID, Integer> playerDeathsDuringDisaster = new HashMap<>();

protected void trackPlayerSurvival(Player player, int phase) {
    UUID uuid = player.getUniqueId();
    playerSurvivalPhases.put(uuid, phase);
}

protected void handlePlayerDeathInDisaster(Player player) {
    UUID uuid = player.getUniqueId();
    playerDeathsDuringDisaster.put(uuid, 
        playerDeathsDuringDisaster.getOrDefault(uuid, 0) + 1);
    
    // Mensaje de aliento
    player.sendMessage("§c§l☠ §7Has muerto durante el desastre. §e¡No te rindas!");
}

protected void awardSurvivalRewards() {
    for (Player p : Bukkit.getOnlinePlayers()) {
        UUID uuid = p.getUniqueId();
        int phasesCompleted = playerSurvivalPhases.getOrDefault(uuid, 0);
        int deaths = playerDeathsDuringDisaster.getOrDefault(uuid, 0);
        
        if (phasesCompleted == 5 && deaths == 0) {
            // Supervivencia perfecta
            awardPerfectSurvival(p);
        } else if (phasesCompleted >= 3) {
            // Supervivencia parcial
            awardPartialSurvival(p, phasesCompleted);
        }
    }
}
```

---

### ✅ Tarea 3.2: Recompensas por supervivencia
**Estado:** ⏳ PENDIENTE  
**Prioridad:** 🟡 MEDIA  
**Archivo:** `DisasterBase.java`

**Sistema de Recompensas:**
```java
private void awardPerfectSurvival(Player player) {
    // PS extra
    missionService.addPS(player.getUniqueId(), 50, "Supervivencia perfecta");
    
    // Items especiales
    ItemStack trophy = new ItemStack(Material.NETHER_STAR);
    ItemMeta meta = trophy.getItemMeta();
    meta.setDisplayName("§6§l⭐ Superviviente del " + getDisasterName());
    meta.setLore(Arrays.asList(
        "§7Has sobrevivido a todas las fases",
        "§7sin morir ni una sola vez.",
        "§e¡Hazaña legendaria!"
    ));
    trophy.setItemMeta(meta);
    player.getInventory().addItem(trophy);
    
    // Efectos visuales
    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    player.spawnParticle(Particle.TOTEM, player.getLocation().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.1);
    
    // Anuncio global
    Bukkit.broadcastMessage("§6§l⭐ §e" + player.getName() + " §7sobrevivió perfectamente al §c" + getDisasterName() + "§7!");
}

private void awardPartialSurvival(Player player, int phases) {
    int psReward = phases * 5; // 5 PS por fase
    missionService.addPS(player.getUniqueId(), psReward, "Supervivencia parcial");
    
    player.sendMessage("§a§l✓ §7Has sobrevivido §e" + phases + " fases§7. §a+" + psReward + " PS");
    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
}
```

---

### ✅ Tarea 3.3: Logros de desastres
**Estado:** ⏳ PENDIENTE  
**Prioridad:** 🟢 BAJA  
**Archivo:** Nuevo `DisasterAchievements.java`

**Logros sugeridos:**
- **"Tierra Firme"** - Sobrevive a Terremoto fase 5
- **"Ojo de la Tormenta"** - Encuentra el ojo del huracán en fase 5
- **"Caminante del Fuego"** - Sobrevive 30s en llamas durante Lluvia de Fuego
- **"Superviviente Perfecto"** - Completa cualquier desastre sin morir
- **"Maestro de Desastres"** - Sobrevive perfectamente a los 3 desastres

---

## 🎯 SPRINT 4: POLISH Y DETALLES (1-2 horas)

### ✅ Tarea 4.1: Transiciones suaves entre fases
**Estado:** ⏳ PENDIENTE  
**Prioridad:** 🟡 MEDIA  

**Implementación:**
```java
protected void transitionToNextPhase(int nextPhase) {
    // Countdown dramático
    Bukkit.broadcastMessage("§6§l⚠ §e¡El desastre se intensifica en 5 segundos!");
    
    for (int i = 5; i > 0; i--) {
        final int countdown = i;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendTitle("§c§l" + countdown, "", 0, 20, 10);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f + (countdown * 0.1f));
            }
        }, (5 - i) * 20L);
    }
    
    // Transición
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
        startPhase(nextPhase);
        showPhaseTitle(nextPhase, getDisasterName());
    }, 100L);
}
```

---

### ✅ Tarea 4.2: Refugios temporales
**Estado:** ⏳ PENDIENTE  
**Prioridad:** 🟢 BAJA  

**Implementación:**
- Spawneo aleatorio de estructuras de obsidiana (refugios)
- Jugadores dentro reciben Resistance II
- Refugios se destruyen en fase 5

---

### ✅ Tarea 4.3: Estadísticas post-desastre
**Estado:** ⏳ PENDIENTE  
**Prioridad:** 🟢 BAJA  

**Mensaje final con estadísticas:**
```java
protected void showDisasterStats() {
    Bukkit.broadcastMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    Bukkit.broadcastMessage("§c§l" + getDisasterName().toUpperCase() + " §7- §eEstadísticas");
    Bukkit.broadcastMessage("");
    Bukkit.broadcastMessage("§7Supervivientes: §a" + getSurvivors().size() + "§7/§f" + Bukkit.getOnlinePlayers().size());
    Bukkit.broadcastMessage("§7Muertes totales: §c" + getTotalDeaths());
    Bukkit.broadcastMessage("§7Duración: §e" + formatDuration(getTotalDuration()));
    Bukkit.broadcastMessage("§7Fases completadas: §6" + getCurrentPhase() + "§7/§f5");
    Bukkit.broadcastMessage("");
    
    // MVP
    Player mvp = getMVPSurvivor();
    if (mvp != null) {
        Bukkit.broadcastMessage("§6§l⭐ MVP: §e" + mvp.getName());
    }
    
    Bukkit.broadcastMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
}
```

---

## 🔧 SPRINT 5: OPTIMIZACIÓN Y CONFIGURACIÓN (1-2 horas)

### ✅ Tarea 5.1: Configuración por desastre en YML
**Estado:** ⏳ PENDIENTE  
**Prioridad:** 🟡 MEDIA  
**Archivo:** `desastres.yml`

**Añadir opciones:**
```yaml
terremoto:
  fases:
    fase1:
      duracion: 15
      intensidad: 1
      spawn_silverfish: false
    fase2:
      duracion: 20
      intensidad: 2
      spawn_silverfish: true
      spawn_rate: 0.1
    # ... etc
  
  efectos:
    particulas: true
    sonidos: true
    bossbar: true
    pre_advertencia: true
  
  recompensas:
    perfecta:
      ps: 50
      item: NETHER_STAR
    parcial_por_fase: 5
```

---

### ✅ Tarea 5.2: Sistema de dificultad escalable
**Estado:** ⏳ PENDIENTE  
**Prioridad:** 🟢 BAJA  

**Escalar según jugadores online:**
```java
protected double getDifficultyMultiplier() {
    int online = Bukkit.getOnlinePlayers().size();
    
    if (online <= 3) return 0.7;      // Modo fácil
    if (online <= 6) return 1.0;      // Normal
    if (online <= 10) return 1.3;     // Difícil
    return 1.5;                        // Extremo
}
```

---

## 📊 RESUMEN DE TAREAS POR PRIORIDAD

### 🔴 ALTA (Implementar primero)
1. Pre-advertencias (Tarea 1.1)
2. Efectos ambientales por desastre (Tarea 1.2)
3. Mecánicas dinámicas TERREMOTO (Tarea 2.1)
4. Mecánicas dinámicas HURACÁN (Tarea 2.2)
5. Mecánicas dinámicas LLUVIA DE FUEGO (Tarea 2.3)

### 🟡 MEDIA (Implementar después)
1. Títulos y subtítulos mejorados (Tarea 1.3)
2. Bossbar de progreso (Tarea 1.4)
3. Tracking de supervivencia (Tarea 3.1)
4. Recompensas por supervivencia (Tarea 3.2)
5. Transiciones entre fases (Tarea 4.1)
6. Configuración YML (Tarea 5.1)

### 🟢 BAJA (Polish opcional)
1. Logros de desastres (Tarea 3.3)
2. Refugios temporales (Tarea 4.2)
3. Estadísticas post-desastre (Tarea 4.3)
4. Dificultad escalable (Tarea 5.2)

---

## 🎯 PLAN DE IMPLEMENTACIÓN RECOMENDADO

### Sesión 1 (3-4 horas): Fundamentos
1. Crear método `showPreWarning()` en `DisasterBase`
2. Implementar efectos ambientales básicos en cada desastre
3. Añadir sistema de BossBar

### Sesión 2 (3-4 horas): Mecánicas
1. Implementar nuevas mecánicas fase por fase para Terremoto
2. Implementar nuevas mecánicas fase por fase para Huracán
3. Implementar nuevas mecánicas fase por fase para Lluvia de Fuego

### Sesión 3 (2-3 horas): Recompensas
1. Sistema de tracking de supervivencia
2. Recompensas por fase completada
3. Sistema de recompensas perfectas

### Sesión 4 (1-2 horas): Polish
1. Transiciones suaves
2. Estadísticas finales
3. Configuración YML
4. Testing y balance

---

## ✅ CRITERIOS DE ÉXITO

- [ ] Cada desastre se siente único y peligroso
- [ ] Progresión clara y visible entre fases
- [ ] Efectos visuales impactantes en cada fase
- [ ] Recompensas que incentivan la supervivencia
- [ ] Sistema configurable desde YML
- [ ] Sin lag con 10+ jugadores online
- [ ] Feedback constante a los jugadores

---

## 📝 NOTAS TÉCNICAS

### Consideraciones de Performance
- Limitar partículas a 100 por tick
- Usar async tasks para cálculos pesados
- Cache de ubicaciones de refugios
- Despawn automático de mobs spawneados al terminar

### Compatibilidad
- Probar con Paper 1.21
- Verificar que Adventure API funciona correctamente
- Asegurar que no interfiere con otros sistemas (eventos, misiones)

---

**INICIO DE DESARROLLO:** Por definir  
**VERSIÓN TARGET:** 1.17.0  
**MANTENER ACTUALIZADO:** ✓
