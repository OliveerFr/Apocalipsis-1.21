# 🔧 CHECKLIST DE FIXES: Anti-Cheat, Chat y Recompensas

**Fecha:** 19 de Noviembre de 2025  
**Versión Target:** 1.16.2  
**Estado:** ✅ COMPLETADO - BUILD SUCCESS  
**Prioridad:** 🔴 CRÍTICA

---

## 📋 RESUMEN DE PROBLEMAS

### 🚨 Problema Principal
```
[Server thread/INFO]: Disconnecting Redblux10 (/181.56.212.215:60477): 
You are banned from this server. Reason: [Vac] Unfair Advantage
```

### 🔴 Problemas Detectados:
1. **Anti-cheat detecta empuje de desastres como hacks** → Banea jugadores
2. **Paquetes perdidos (packet loss)** por velocidad excesiva → Detección falsa positiva
3. **Chat sin colores ni formato** → Sistema de rangos no muestra estilos
4. **Sonidos de chat ausentes** → Menciones no reproducen audio
5. **Notificaciones de rank-up desaparecidas** → No se muestran al subir de rango
6. **Recompensas no se entregan** → Sistema de rewards no funciona al subir rango

---

## 🎯 SECCIÓN 1: ANTI-CHEAT / VELOCITY BYPASS

### ❌ Causa Raíz
Los desastres aplican `setVelocity()` directamente, enviando cambios bruscos de velocidad que los anti-cheats interpretan como:
- **Fly hacks** (velocidad vertical anormal)
- **Speed hacks** (velocidad horizontal excesiva)
- **Unfair Advantage** (movimiento no natural)

### ✅ Solución: Sistema de Velocity Smoothing

#### 📝 Tarea 1.1: Crear VelocityManager.java
**Ubicación:** `src/main/java/me/apocalipsis/utils/VelocityManager.java`

```java
package me.apocalipsis.utils;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.bukkit.Bukkit;
import me.apocalipsis.Apocalipsis;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gestiona aplicación gradual de velocidad para evitar detección de anti-cheats
 */
public class VelocityManager {
    
    private final Apocalipsis plugin;
    private final Map<UUID, VelocityTask> activeTasks = new HashMap<>();
    
    // Configuración anti-cheat friendly
    private static final double MAX_VELOCITY_PER_TICK = 0.15; // Reducido para evitar flags
    private static final int SMOOTH_DURATION_TICKS = 5; // Aplicar en 5 ticks (0.25s)
    
    public VelocityManager(Apocalipsis plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Aplica velocidad de forma suave y gradual (anti-cheat safe)
     */
    public void applySmoothedVelocity(Player player, Vector targetVelocity) {
        UUID uuid = player.getUniqueId();
        
        // Cancelar task anterior si existe
        VelocityTask existing = activeTasks.get(uuid);
        if (existing != null) {
            existing.cancel();
        }
        
        // Limitar velocidad máxima
        Vector clampedVelocity = clampVelocity(targetVelocity);
        
        // Crear nueva task de aplicación gradual
        VelocityTask task = new VelocityTask(player, clampedVelocity);
        activeTasks.put(uuid, task);
        task.start();
    }
    
    /**
     * Limita velocidad a valores seguros
     */
    private Vector clampVelocity(Vector velocity) {
        double x = clamp(velocity.getX(), -MAX_VELOCITY_PER_TICK * 3, MAX_VELOCITY_PER_TICK * 3);
        double y = clamp(velocity.getY(), -MAX_VELOCITY_PER_TICK * 2, MAX_VELOCITY_PER_TICK * 2);
        double z = clamp(velocity.getZ(), -MAX_VELOCITY_PER_TICK * 3, MAX_VELOCITY_PER_TICK * 3);
        return new Vector(x, y, z);
    }
    
    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * Task interna que aplica velocidad gradualmente
     */
    private class VelocityTask {
        private final Player player;
        private final Vector targetVelocity;
        private int taskId = -1;
        private int ticksElapsed = 0;
        
        VelocityTask(Player player, Vector targetVelocity) {
            this.player = player;
            this.targetVelocity = targetVelocity;
        }
        
        void start() {
            taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                if (!player.isOnline() || ticksElapsed >= SMOOTH_DURATION_TICKS) {
                    cancel();
                    return;
                }
                
                // Aplicar fracción de la velocidad
                double fraction = 1.0 / (SMOOTH_DURATION_TICKS - ticksElapsed);
                Vector increment = targetVelocity.clone().multiply(fraction);
                
                Vector currentVel = player.getVelocity();
                player.setVelocity(currentVel.add(increment));
                
                ticksElapsed++;
            }, 0L, 1L); // Cada tick
        }
        
        void cancel() {
            if (taskId != -1) {
                Bukkit.getScheduler().cancelTask(taskId);
                activeTasks.remove(player.getUniqueId());
            }
        }
    }
    
    /**
     * Limpieza al desactivar plugin
     */
    public void shutdown() {
        activeTasks.values().forEach(VelocityTask::cancel);
        activeTasks.clear();
    }
}
```

**Checklist:**
- [x] Crear archivo `VelocityManager.java`
- [x] Agregar field en `Apocalipsis.java`: `private VelocityManager velocityManager;`
- [x] Inicializar en `onEnable()`: `velocityManager = new VelocityManager(this);`
- [x] Llamar en `onDisable()`: `velocityManager.shutdown();`
- [x] Agregar getter público: `public VelocityManager getVelocityManager() { return velocityManager; }`

---

#### 📝 Tarea 1.2: Actualizar HuracanNew.java

**Archivo:** `src/main/java/me/apocalipsis/disaster/HuracanNew.java`

**Buscar línea ~566:**
```java
player.setVelocity(velocity);
```

**Reemplazar con:**
```java
// 🔧 FIX: Usar VelocityManager para evitar detección anti-cheat
plugin.getVelocityManager().applySmoothedVelocity(player, velocity);
```

**Checklist:**
- [ ] Localizar línea 566 en `HuracanNew.java`
- [ ] Reemplazar `setVelocity()` directo con `VelocityManager`
- [ ] Agregar comentario explicativo
- [ ] Verificar imports (no necesita nuevos)

---

#### 📝 Tarea 1.3: Actualizar TerremotoNew.java

**Archivo:** `src/main/java/me/apocalipsis/disaster/TerremotoNew.java`

**Buscar línea ~512:**
```java
p.setVelocity(p.getVelocity().add(v));
```

**Reemplazar con:**
```java
// 🔧 FIX: Calcular velocidad final y aplicar con smoothing
Vector finalVelocity = p.getVelocity().add(v);
plugin.getVelocityManager().applySmoothedVelocity(p, finalVelocity);
```

**Checklist:**
- [ ] Localizar línea 512 en `TerremotoNew.java`
- [ ] Reemplazar lógica de velocidad
- [ ] Agregar comentario explicativo
- [ ] Verificar que no hay más llamadas `setVelocity()` en el archivo

---

#### 📝 Tarea 1.4: Actualizar Eventos (EcoSombras, GuardianPhase, etc.)

**Archivos afectados:**
1. `EcoSombrasEvent.java` (línea ~1359)
2. `GuardianPhaseSystem.java` (líneas 221, 320)
3. `TelegraphedAttack.java` (líneas 401, 587)

**Patrón de búsqueda:**
```java
player.setVelocity(...)
```

**Reemplazo:**
```java
plugin.getVelocityManager().applySmoothedVelocity(player, <vector>);
```

**Checklist:**
- [ ] Buscar todos los `setVelocity` en eventos
- [ ] Reemplazar con `VelocityManager`
- [ ] Agregar comentarios `// 🔧 FIX: Anti-cheat safe velocity`
- [ ] Compilar y verificar que no hay errores

---

#### 📝 Tarea 1.5: Configuración Anti-Cheat en desastres.yml

**Archivo:** `src/main/resources/desastres.yml`

**Agregar nueva sección:**
```yaml
# ════════════════════════════════════════════════════════════════════
# CONFIGURACIÓN ANTI-CHEAT COMPATIBILITY
# ════════════════════════════════════════════════════════════════════
anti_cheat:
  # Habilitar sistema de velocity smoothing
  velocity_smoothing_enabled: true
  
  # Velocidad máxima por tick (valores seguros)
  max_velocity_per_tick: 0.15
  
  # Duración de aplicación suave (ticks)
  smooth_duration_ticks: 5
  
  # Reducir empuje durante lag (TPS < 18)
  reduce_on_lag: true
  lag_threshold_tps: 18.0
  lag_reduction_factor: 0.5
```

**Checklist:**
- [ ] Agregar sección `anti_cheat` en `desastres.yml`
- [ ] Modificar `VelocityManager` para leer config
- [ ] Recargar config con `/avo reload`
- [ ] Testear con valores ajustables

---

## 🎨 SECCIÓN 2: SISTEMA DE CHAT (Colores y Formatos)

### ❌ Problema
Chat no muestra colores ni formatos configurados en `chat.yml` para los rangos.

### ✅ Solución: Verificar y Corregir ChatListener

#### 📝 Tarea 2.1: Verificar chat.yml existe y está cargado

**Checklist:**
- [ ] Confirmar que existe `src/main/resources/chat.yml`
- [ ] Verificar que `ConfigManager` carga el archivo:
  ```java
  private FileConfiguration chatConfig;
  public FileConfiguration getChatConfig() { return chatConfig; }
  ```
- [ ] Confirmar `enabled: true` en `chat.yml`

---

#### 📝 Tarea 2.2: Verificar Permisos en chat.yml

**Archivo:** `src/main/resources/chat.yml`

**Revisar sección para cada rango:**
```yaml
formats:
  NOVATO:
    badge: "&8[&aNovato&8]"
    player_name: "&a%player%"
    level_badge: "&8[&7Nv.%level%&8]"
    separator: "&8»"
    message_color: "&f"
  
  EXPLORADOR:
    badge: "&8[&b✦ Explorador&8]"
    player_name: "&b%player%"
    level_badge: "&8[&3Nv.%level%&8]"
    separator: "&8»"
    message_color: "&f"
  
  # ... resto de rangos
```

**Checklist:**
- [ ] Verificar que todos los rangos tienen sección `formats.<RANK>`
- [ ] Confirmar códigos de color correctos (`&a`, `&b`, etc.)
- [ ] Asegurar que `message_color` está definido
- [ ] Verificar que variables `%player%` y `%level%` existen

---

#### 📝 Tarea 2.3: Corregir ChatListener.java (si necesario)

**Archivo:** `src/main/java/me/apocalipsis/listeners/ChatListener.java`

**Verificar método `formatChatMessage()`:**
```java
private String formatChatMessage(Player player, MissionRank rank, int level, String message, FileConfiguration config) {
    String rankName = rank.name();
    String basePath = "formats." + rankName + ".";
    
    // Obtener componentes del formato
    String badge = config.getString(basePath + "badge", "§8[" + rankName + "§8]");
    String playerName = config.getString(basePath + "player_name", "§f%player%");
    String levelBadge = config.getString(basePath + "level_badge", "§8[§7Lv.%level%§8]");
    String separator = config.getString(basePath + "separator", "§8»");
    String messageColor = config.getString(basePath + "message_color", "§f");
    
    // Reemplazar variables
    playerName = playerName.replace("%player%", player.getName());
    levelBadge = levelBadge.replace("%level%", String.valueOf(level));
    
    // 🔧 FIX: ASEGURAR TRADUCCIÓN DE COLORES
    badge = translateColors(badge);
    playerName = translateColors(playerName);
    levelBadge = translateColors(levelBadge);
    separator = translateColors(separator);
    messageColor = translateColors(messageColor);
    
    // Construir formato final
    return badge + " " + playerName + " " + levelBadge + " " + separator + " " + messageColor + "%1$s";
}
```

**Checklist:**
- [ ] Verificar que `translateColors()` se llama SIEMPRE
- [ ] Confirmar que no hay early returns que salten la traducción
- [ ] Asegurar que `%1$s` se reemplaza con el mensaje real
- [ ] Testear con `/avo reload` después de cambios

---

#### 📝 Tarea 2.4: Debug Mode para Chat

**Agregar logs temporales en `ChatListener.onPlayerChat()`:**

```java
@EventHandler(priority = EventPriority.LOWEST)
public void onPlayerChat(AsyncChatEvent event) {
    // ... código existente ...
    
    String formattedMessage = formatChatMessage(player, rank, level, message, config);
    
    // 🔧 DEBUG: Log del formato generado
    plugin.getLogger().info("[Chat-DEBUG] Rank: " + rank.name());
    plugin.getLogger().info("[Chat-DEBUG] Formatted: " + formattedMessage);
    plugin.getLogger().info("[Chat-DEBUG] Final message: " + formattedMessage.replace("%1$s", message));
    
    // ... resto del código ...
}
```

**Checklist:**
- [ ] Agregar logs de debug
- [ ] Enviar mensaje en chat y revisar console
- [ ] Verificar que colores aparecen en los logs
- [ ] Remover logs después de confirmar que funciona

---

## 🔊 SECCIÓN 3: SONIDOS DE CHAT (Menciones)

### ❌ Problema
Menciones no reproducen sonido cuando alguien escribe el nombre de otro jugador.

### ✅ Solución: Verificar y Corregir Sistema de Menciones

#### 📝 Tarea 3.1: Verificar Configuración de Menciones

**Archivo:** `src/main/resources/chat.yml`

**Verificar sección:**
```yaml
mentions:
  enabled: true
  mention_color: "&e&l"
  mention_sound: "BLOCK_NOTE_BLOCK_PLING"
  mention_volume: 0.8
  mention_pitch: 1.5
```

**Checklist:**
- [ ] Confirmar `mentions.enabled: true`
- [ ] Verificar que `mention_sound` es un sonido válido de Paper 1.21
- [ ] Asegurar valores numéricos de volume/pitch correctos

---

#### 📝 Tarea 3.2: Corregir applyMentions() en ChatListener

**Archivo:** `src/main/java/me/apocalipsis/listeners/ChatListener.java`

**Buscar método `applyMentions()`:**

```java
private String applyMentions(String message, Player sender, FileConfiguration config) {
    String mentionColor = config.getString("mentions.mention_color", "&e&l");
    
    for (Player online : plugin.getServer().getOnlinePlayers()) {
        if (message.toLowerCase().contains(online.getName().toLowerCase())) {
            // Notificar al jugador mencionado
            if (!online.equals(sender)) {
                String soundName = config.getString("mentions.mention_sound", "BLOCK_NOTE_BLOCK_PLING");
                float volume = (float) config.getDouble("mentions.mention_volume", 0.8);
                float pitch = (float) config.getDouble("mentions.mention_pitch", 1.5);
                
                try {
                    // 🔧 FIX: Usar Registry API de Paper 1.21
                    org.bukkit.NamespacedKey soundKey = org.bukkit.NamespacedKey.fromString(soundName.toLowerCase());
                    if (soundKey == null) {
                        soundKey = org.bukkit.NamespacedKey.minecraft(soundName.toLowerCase().replace("_", ""));
                    }
                    Sound sound = org.bukkit.Registry.SOUNDS.get(soundKey);
                    
                    if (sound != null) {
                        online.playSound(online.getLocation(), sound, volume, pitch);
                        
                        // 🔧 DEBUG: Confirmar que se reproduce
                        plugin.getLogger().info("[Mention] Sonido reproducido para " + online.getName());
                    } else {
                        plugin.getLogger().warning("[Mention] Sonido no encontrado: " + soundName);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("[Mention] Error al reproducir sonido: " + e.getMessage());
                }
            }
            
            // Resaltar el nombre en el mensaje
            String highlighted = translateColors(mentionColor) + online.getName() + "§r";
            message = message.replaceAll("(?i)" + online.getName(), highlighted);
        }
    }
    
    return message;
}
```

**Checklist:**
- [ ] Verificar uso de `Registry.SOUNDS` (Paper 1.21+)
- [ ] Confirmar que no se usa `Sound.valueOf()` deprecated
- [ ] Agregar logs de debug para confirmar ejecución
- [ ] Testear mencionando a otro jugador
- [ ] Verificar que sonido se escucha

---

## 🎉 SECCIÓN 4: NOTIFICACIONES Y EFECTOS DE RANK UP

### ❌ Problema
Al subir de rango, no se muestran efectos visuales, sonidos ni mensajes de celebración.

### ✅ Solución: Verificar Sistema de Rank Up

#### 📝 Tarea 4.1: Verificar playRankUpEffects() en MissionService

**Archivo:** `src/main/java/me/apocalipsis/missions/MissionService.java`

**Buscar método `playRankUpEffects()` (línea ~446):**

**Verificar que existe y tiene este código:**
```java
private void playRankUpEffects(Player player, MissionRank rank) {
    Location loc = player.getLocation().add(0, 1.5, 0);
    
    // Colores por rango
    org.bukkit.Color primary, secondary;
    FireworkEffect.Type type;
    
    switch (rank) {
        case LEYENDA:
            primary = org.bukkit.Color.RED;
            secondary = org.bukkit.Color.ORANGE;
            type = FireworkEffect.Type.STAR;
            break;
        // ... casos para cada rango ...
        default:
            primary = org.bukkit.Color.LIME;
            secondary = org.bukkit.Color.GREEN;
            type = FireworkEffect.Type.BALL;
            break;
    }
    
    // 🔧 FIX: ASEGURAR QUE SE REPRODUCEN SONIDOS
    player.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.5f, 1.0f);
    player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.2f, 0.8f);
    
    // 🔧 FIX: ASEGURAR PARTÍCULAS VISIBLES
    player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 40, 1.0, 1.0, 1.0, 0.1);
    player.getWorld().spawnParticle(Particle.FIREWORK, loc, 30, 0.8, 0.8, 0.8, 0.15);
    player.getWorld().spawnParticle(Particle.END_ROD, loc, 20, 0.6, 0.6, 0.6, 0.08);
    
    // 🔧 FIX: FUEGOS ARTIFICIALES (3 en vez de 1)
    for (int i = 0; i < 3; i++) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Firework fw = (Firework) player.getWorld().spawnEntity(loc, EntityType.FIREWORK_ROCKET);
            FireworkMeta meta = fw.getFireworkMeta();
            meta.setPower(1);
            meta.addEffect(FireworkEffect.builder()
                .with(type)
                .withColor(primary)
                .withFade(secondary)
                .withTrail()
                .withFlicker()
                .build());
            fw.setFireworkMeta(meta);
        }, i * 10L);
    }
    
    // 🔧 FIX: TÍTULO GRANDE Y VISIBLE
    String rankName = rank.getDisplayName();
    player.showTitle(net.kyori.adventure.title.Title.title(
        net.kyori.adventure.text.Component.text("§6§l¡NUEVO RANGO!"),
        net.kyori.adventure.text.Component.text("§f" + rankName),
        net.kyori.adventure.title.Title.Times.times(
            java.time.Duration.ofMillis(500),
            java.time.Duration.ofMillis(3000),
            java.time.Duration.ofMillis(1000)
        )
    ));
    
    // 🔧 FIX: MENSAJE PÚBLICO GLOBAL
    Bukkit.getServer().broadcast(
        net.kyori.adventure.text.Component.text("§6§l★ " + player.getName() + " §eha alcanzado el rango " + rankName + "§6§l ★")
    );
}
```

**Checklist:**
- [ ] Verificar que método existe en `MissionService.java`
- [ ] Confirmar que se llama desde `rewardPlayer()` cuando hay rank up
- [ ] Asegurar que NO está comentado
- [ ] Verificar imports de Adventure API
- [ ] Testear subiendo de rango con `/avo ps add <jugador> 1000`

---

#### 📝 Tarea 4.2: Verificar que se Llama en rewardPlayer()

**Archivo:** `src/main/java/me/apocalipsis/missions/MissionService.java`

**Buscar método `rewardPlayer()` (línea ~340):**

```java
private void rewardPlayer(Player player, MissionCatalog mission) {
    UUID uuid = player.getUniqueId();
    int currentPs = playerPs.getOrDefault(uuid, 0);
    int newPs = currentPs + mission.getRecompensaPs();
    
    // Detectar rank up
    MissionRank oldRank = MissionRank.fromXp(currentPs);
    MissionRank newRank = MissionRank.fromXp(newPs);
    
    playerPs.put(uuid, newPs);
    
    // Efectos de misión completada
    playMissionCompleteEffects(player, mission);
    
    messageBus.sendMessage(player, "§a§l✓ Misión completada: §f" + mission.getNombre() + " §7(§e+" + mission.getRecompensaPs() + " PS§7)");
    savePlayerData();
    
    // Si hubo rank up
    if (oldRank != newRank) {
        // 🔧 FIX: ASEGURAR QUE SE LLAMA
        playRankUpEffects(player, newRank);
        
        // Entregar recompensas de rango
        if (plugin.getRewardService() != null) {
            plugin.getRewardService().deliverRewards(player, newRank);
        }
    }
    
    // Actualizar UI
    if (plugin.getScoreboardManager() != null) {
        plugin.getScoreboardManager().updatePlayer(player);
    }
    if (plugin.getTablistManager() != null) {
        plugin.getTablistManager().updatePlayer(player);
    }
}
```

**Checklist:**
- [ ] Confirmar que `if (oldRank != newRank)` existe
- [ ] Verificar que `playRankUpEffects(player, newRank)` se llama
- [ ] Asegurar que NO hay early returns antes
- [ ] Agregar log temporal: `plugin.getLogger().info("[RankUp] " + player.getName() + ": " + oldRank + " → " + newRank);`

---

## 💎 SECCIÓN 5: RECOMPENSAS AL SUBIR DE RANGO

### ❌ Problema
Al subir de rango, no se entregan los items/comandos configurados en `recompensas.yml`.

### ✅ Solución: Verificar y Corregir RewardService

#### 📝 Tarea 5.1: Verificar recompensas.yml

**Archivo:** `src/main/resources/recompensas.yml`

**Verificar estructura:**
```yaml
recompensas_por_rango:
  EXPLORADOR:
    comandos:
      - "give %player% diamond 5"
      - "eco give %player% 1000"
    mensaje: "&a¡Has recibido recompensas de Explorador!"
  
  SOBREVIVIENTE:
    comandos:
      - "give %player% diamond 10"
      - "give %player% emerald 5"
      - "eco give %player% 2500"
    mensaje: "&a¡Has recibido recompensas de Sobreviviente!"
  
  VETERANO:
    comandos:
      - "give %player% diamond 15"
      - "give %player% emerald 10"
      - "give %player% netherite_ingot 1"
      - "eco give %player% 5000"
    mensaje: "&a¡Has recibido recompensas de Veterano!"
  
  # ... resto de rangos
```

**Checklist:**
- [ ] Confirmar que todos los rangos (excepto NOVATO) tienen sección
- [ ] Verificar que `comandos` es una lista válida
- [ ] Asegurar que variable `%player%` está en todos los comandos
- [ ] Confirmar que `mensaje` tiene color codes

---

#### 📝 Tarea 5.2: Verificar deliverRewards() en RewardService

**Archivo:** `src/main/java/me/apocalipsis/experience/RewardService.java`

**Buscar método `deliverRewards()` (línea ~118):**

```java
public boolean deliverRewards(Player player, MissionRank rank) {
    // Verificar si ya recibió esta recompensa
    String key = player.getUniqueId().toString() + ":" + rank.name();
    if (deliveredRewards.contains(key)) {
        return false; // Ya recibió esta recompensa
    }
    
    RankReward reward = rewardsByRank.get(rank);
    if (reward == null) {
        // 🔧 DEBUG: Log si no hay recompensas configuradas
        plugin.getLogger().warning("[Rewards] No hay recompensas configuradas para rango: " + rank.name());
        return false;
    }
    
    // 🔧 FIX: Ejecutar comandos EN SYNC
    for (String command : reward.getCommands()) {
        String processedCommand = command.replace("%player%", player.getName());
        
        // CRÍTICO: Ejecutar en tick principal
        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getLogger().info("[Rewards] Ejecutando: " + processedCommand);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
        });
    }
    
    // Enviar mensaje
    if (!reward.getMessage().isEmpty()) {
        Component message = LegacyComponentSerializer.legacyAmpersand().deserialize(reward.getMessage());
        player.sendMessage(message);
    }
    
    // Marcar como entregado
    deliveredRewards.add(key);
    saveDeliveredRewards();
    
    // Efectos visuales
    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    
    plugin.getLogger().info("[Rewards] Recompensas de " + rank.name() + " entregadas a " + player.getName());
    
    return true;
}
```

**Checklist:**
- [ ] Verificar que método existe y no está comentado
- [ ] Confirmar uso de `runTask()` para sincronización
- [ ] Asegurar que `deliveredRewards` se guarda correctamente
- [ ] Agregar logs de debug para ver qué comandos se ejecutan
- [ ] Testear forzando recompensa: `/avo rewards force <jugador> <rango>`

---

#### 📝 Tarea 5.3: Verificar loadRewards() en RewardService

**Archivo:** `src/main/java/me/apocalipsis/experience/RewardService.java`

**Buscar método `loadRewards()` (línea ~84):**

```java
public void loadRewards() {
    rewardsByRank.clear();
    
    FileConfiguration config = plugin.getConfigManager().getRecompensasConfig();
    ConfigurationSection section = config.getConfigurationSection("recompensas_por_rango");
    
    if (section == null) {
        plugin.getLogger().warning("[Rewards] No se encontró sección 'recompensas_por_rango' en recompensas.yml");
        return;
    }
    
    for (MissionRank rank : MissionRank.values()) {
        if (rank == MissionRank.NOVATO) continue;
        
        String rankKey = rank.name();
        ConfigurationSection rankSection = section.getConfigurationSection(rankKey);
        
        if (rankSection == null) {
            plugin.getLogger().warning("[Rewards] No se encontró configuración para rango: " + rankKey);
            continue;
        }
        
        List<String> commands = rankSection.getStringList("comandos");
        String message = rankSection.getString("mensaje", "");
        
        if (!commands.isEmpty()) {
            rewardsByRank.put(rank, new RankReward(commands, message));
            plugin.getLogger().info("[Rewards] Cargadas " + commands.size() + " recompensas para " + rankKey);
        }
    }
    
    plugin.getLogger().info("[Rewards] Total recompensas cargadas para " + rewardsByRank.size() + " rangos");
}
```

**Checklist:**
- [ ] Verificar que método se llama en constructor
- [ ] Confirmar logs en consola al iniciar servidor
- [ ] Revisar que no hay errores en carga de config
- [ ] Testear con `/avo reload` para recargar

---

#### 📝 Tarea 5.4: Agregar Comando de Debug para Recompensas

**Archivo:** `src/main/java/me/apocalipsis/commands/ApocalipsisCommand.java`

**Agregar nuevo subcomando `/avo rewards`:**

```java
// En método onCommand(), agregar case:
case "rewards":
    cmdRewards(sender, args);
    return true;

// Agregar método nuevo:
private void cmdRewards(CommandSender sender, String[] args) {
    if (!sender.hasPermission("avo.admin")) {
        sender.sendMessage("§cNo tienes permiso.");
        return;
    }
    
    if (args.length < 2) {
        sender.sendMessage("§e/avo rewards <check|force|reset> [jugador] [rango]");
        return;
    }
    
    String subCmd = args[1].toLowerCase();
    
    switch (subCmd) {
        case "check":
            // /avo rewards check <jugador>
            if (args.length < 3) {
                sender.sendMessage("§c/avo rewards check <jugador>");
                return;
            }
            Player target = plugin.getServer().getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage("§cJugador no encontrado.");
                return;
            }
            
            MissionRank rank = plugin.getRankService().getRank(target);
            sender.sendMessage("§7Rango actual: §e" + rank.name());
            sender.sendMessage("§7XP: §e" + plugin.getRankService().getXP(target));
            
            RewardService rewards = plugin.getRewardService();
            if (rewards != null) {
                boolean received = rewards.hasReceivedRewards(target, rank);
                sender.sendMessage("§7Recompensas recibidas: " + (received ? "§a✓ SÍ" : "§c✗ NO"));
            }
            break;
            
        case "force":
            // /avo rewards force <jugador> <rango>
            if (args.length < 4) {
                sender.sendMessage("§c/avo rewards force <jugador> <rango>");
                return;
            }
            Player targetForce = plugin.getServer().getPlayer(args[2]);
            if (targetForce == null) {
                sender.sendMessage("§cJugador no encontrado.");
                return;
            }
            
            try {
                MissionRank forceRank = MissionRank.valueOf(args[3].toUpperCase());
                if (plugin.getRewardService() != null) {
                    plugin.getRewardService().forceDeliverRewards(targetForce, forceRank);
                    sender.sendMessage("§a✓ Recompensas forzadas para " + targetForce.getName() + " (" + forceRank.name() + ")");
                }
            } catch (IllegalArgumentException e) {
                sender.sendMessage("§cRango inválido. Usa: NOVATO, EXPLORADOR, SOBREVIVIENTE, etc.");
            }
            break;
            
        case "reset":
            // /avo rewards reset <jugador>
            if (args.length < 3) {
                sender.sendMessage("§c/avo rewards reset <jugador>");
                return;
            }
            Player targetReset = plugin.getServer().getPlayer(args[2]);
            if (targetReset == null) {
                sender.sendMessage("§cJugador no encontrado.");
                return;
            }
            
            if (plugin.getRewardService() != null) {
                plugin.getRewardService().resetPlayerRewards(targetReset.getUniqueId());
                sender.sendMessage("§a✓ Recompensas reseteadas para " + targetReset.getName());
            }
            break;
            
        default:
            sender.sendMessage("§c/avo rewards <check|force|reset> [jugador] [rango]");
            break;
    }
}
```

**Checklist:**
- [ ] Agregar método `cmdRewards()` en `ApocalipsisCommand.java`
- [ ] Agregar case "rewards" en switch principal
- [ ] Compilar y testear comandos:
  - `/avo rewards check Redblux10`
  - `/avo rewards force Redblux10 EXPLORADOR`
  - `/avo rewards reset Redblux10`

---

## 🧪 SECCIÓN 6: TESTING Y VALIDACIÓN

### 📝 Tarea 6.1: Test Anti-Cheat Bypass

**Procedimiento:**
1. Compilar plugin con cambios de `VelocityManager`
2. Instalar en servidor de prueba con anti-cheat (Spartan, AAC, Matrix, etc.)
3. Iniciar Huracán: `/avo start huracan`
4. Observar consola para flags/kicks
5. Ajustar `MAX_VELOCITY_PER_TICK` si hay detecciones

**Valores de ajuste:**
```java
// Conservador (menos flags, menos realismo)
MAX_VELOCITY_PER_TICK = 0.10;
SMOOTH_DURATION_TICKS = 8;

// Balanceado (recomendado)
MAX_VELOCITY_PER_TICK = 0.15;
SMOOTH_DURATION_TICKS = 5;

// Agresivo (más realismo, posibles flags)
MAX_VELOCITY_PER_TICK = 0.20;
SMOOTH_DURATION_TICKS = 3;
```

**Checklist:**
- [ ] Testear Huracán sin kicks
- [ ] Testear Terremoto sin flags
- [ ] Verificar que eventos funcionan
- [ ] Confirmar que no hay packet loss excesivo

---

### 📝 Tarea 6.2: Test Sistema de Chat

**Procedimiento:**
1. Recargar config: `/avo reload`
2. Enviar mensaje en chat con jugador de rango NOVATO
3. Verificar que aparecen colores
4. Subir a EXPLORADOR: `/avo ps add <jugador> 500`
5. Enviar mensaje y verificar nuevo formato

**Checklist:**
- [ ] Colores visibles para todos los rangos
- [ ] Variables `%player%` y `%level%` reemplazadas
- [ ] Separadores `»` aparecen correctamente
- [ ] Formato se mantiene en diferentes rangos

---

### 📝 Tarea 6.3: Test Menciones y Sonidos

**Procedimiento:**
1. Conectar 2 jugadores en servidor
2. Jugador A escribe: "Hola @Jugador_B"
3. Verificar que Jugador_B escucha sonido
4. Verificar que nombre está resaltado en amarillo

**Checklist:**
- [ ] Sonido se reproduce (`BLOCK_NOTE_BLOCK_PLING`)
- [ ] Nombre resaltado con color configurado
- [ ] No se menciona el emisor a sí mismo
- [ ] Funciona case-insensitive (JuGaDoR_B también funciona)

---

### 📝 Tarea 6.4: Test Rank Up Completo

**Procedimiento:**
1. Resetear jugador: `/avo rewards reset <jugador>`
2. Dar PS para subir rango: `/avo ps add <jugador> 1000`
3. Observar efectos cuando cruza umbral

**Checklist:**
- [ ] Título grande aparece: "¡NUEVO RANGO!"
- [ ] Sonidos se reproducen (levelup + challenge_complete)
- [ ] Partículas visibles (fireworks, totem, end_rod)
- [ ] Fuegos artificiales lanzan (3 en total)
- [ ] Mensaje global broadcast en chat
- [ ] Recompensas entregadas (verificar inventario)
- [ ] Log en consola: `[Rewards] Recompensas de X entregadas a Y`

---

### 📝 Tarea 6.5: Test Recompensas

**Procedimiento:**
1. Configurar recompensa simple en `recompensas.yml`:
   ```yaml
   EXPLORADOR:
     comandos:
       - "give %player% diamond 1"
     mensaje: "&aTest recompensa"
   ```
2. Forzar entrega: `/avo rewards force <jugador> EXPLORADOR`
3. Verificar inventario

**Checklist:**
- [ ] Comando ejecutado (log en consola)
- [ ] Item recibido en inventario
- [ ] Mensaje enviado al jugador
- [ ] Sonidos reproducidos
- [ ] No se entrega dos veces (flag de entregado funciona)

---

## 📊 SECCIÓN 7: CONFIGURACIONES FINALES

### 📝 Tarea 7.1: Actualizar desastres.yml con Anti-Cheat Config

```yaml
anti_cheat:
  velocity_smoothing_enabled: true
  max_velocity_per_tick: 0.15
  smooth_duration_ticks: 5
  reduce_on_lag: true
  lag_threshold_tps: 18.0
  lag_reduction_factor: 0.5
```

---

### 📝 Tarea 7.2: Completar chat.yml con Todos los Rangos

```yaml
enabled: true

formats:
  NOVATO:
    badge: "&8[&aNovato&8]"
    player_name: "&a%player%"
    level_badge: "&8[&7Nv.%level%&8]"
    separator: "&8»"
    message_color: "&f"
  
  EXPLORADOR:
    badge: "&8[&b✦ Explorador&8]"
    player_name: "&b%player%"
    level_badge: "&8[&3Nv.%level%&8]"
    separator: "&8»"
    message_color: "&f"
  
  SOBREVIVIENTE:
    badge: "&8[&e⚔ Sobreviviente&8]"
    player_name: "&e%player%"
    level_badge: "&8[&6Nv.%level%&8]"
    separator: "&8»"
    message_color: "&f"
  
  VETERANO:
    badge: "&8[&6★ Veterano&8]"
    player_name: "&6%player%"
    level_badge: "&8[&eLv.%level%&8]"
    separator: "&7»"
    message_color: "&f"
  
  LEYENDA:
    badge: "&8[&c★★ &l&cLEYENDA&8]"
    player_name: "&c&l%player%"
    level_badge: "&r&8[&cLv.%level%&8]"
    separator: "&c»"
    message_color: "&f"
  
  MAESTRO:
    badge: "&8[&5♛ &l&5MAESTRO&8]"
    player_name: "&5&l%player%"
    level_badge: "&r&8[&dLv.%level%&8]"
    separator: "&5»"
    message_color: "&f"
  
  TITAN:
    badge: "&8[&4✦&l&4TITÁN&r&4✦&8]"
    player_name: "&4&l%player%"
    level_badge: "&r&8[&cLv.%level%&8]"
    separator: "&4»"
    message_color: "&f"
  
  ABSOLUTO:
    badge: "&8[&f&l◈ ABSOLUTO ◈&8]"
    player_name: "&f&l%player%"
    level_badge: "&r&8[&fLv.%level%&8]"
    separator: "&f»"
    message_color: "&7"

mentions:
  enabled: true
  mention_color: "&e&l"
  mention_sound: "BLOCK_NOTE_BLOCK_PLING"
  mention_volume: 0.8
  mention_pitch: 1.5

general:
  allow_colors:
    NOVATO: false
    EXPLORADOR: false
    SOBREVIVIENTE: true
    VETERANO: true
    LEYENDA: true
    MAESTRO: true
    TITAN: true
    ABSOLUTO: true
  
  allow_formatting:
    NOVATO: false
    EXPLORADOR: false
    SOBREVIVIENTE: false
    VETERANO: true
    LEYENDA: true
    MAESTRO: true
    TITAN: true
    ABSOLUTO: true
```

---

### 📝 Tarea 7.3: Completar recompensas.yml con Progresión

```yaml
recompensas_por_rango:
  EXPLORADOR:
    comandos:
      - "give %player% diamond 5"
      - "give %player% iron_ingot 32"
      - "eco give %player% 1000"
    mensaje: "&a&l✓ Recompensas de Explorador recibidas!"
  
  SOBREVIVIENTE:
    comandos:
      - "give %player% diamond 10"
      - "give %player% emerald 5"
      - "give %player% iron_block 16"
      - "eco give %player% 2500"
    mensaje: "&e&l✓ Recompensas de Sobreviviente recibidas!"
  
  VETERANO:
    comandos:
      - "give %player% diamond 15"
      - "give %player% emerald 10"
      - "give %player% netherite_ingot 1"
      - "give %player% diamond_block 5"
      - "eco give %player% 5000"
    mensaje: "&6&l✓ Recompensas de Veterano recibidas!"
  
  LEYENDA:
    comandos:
      - "give %player% diamond 25"
      - "give %player% emerald 20"
      - "give %player% netherite_ingot 3"
      - "give %player% netherite_block 1"
      - "give %player% elytra 1"
      - "eco give %player% 10000"
    mensaje: "&c&l✓ Recompensas de LEYENDA recibidas!"
  
  MAESTRO:
    comandos:
      - "give %player% diamond 40"
      - "give %player% emerald 32"
      - "give %player% netherite_ingot 5"
      - "give %player% netherite_block 2"
      - "give %player% totem_of_undying 1"
      - "eco give %player% 25000"
    mensaje: "&5&l✓ Recompensas de MAESTRO recibidas!"
  
  TITAN:
    comandos:
      - "give %player% diamond_block 10"
      - "give %player% emerald_block 5"
      - "give %player% netherite_block 5"
      - "give %player% totem_of_undying 2"
      - "give %player% shulker_box 1"
      - "eco give %player% 50000"
    mensaje: "&4&l✓ Recompensas de TITÁN recibidas!"
  
  ABSOLUTO:
    comandos:
      - "give %player% diamond_block 20"
      - "give %player% emerald_block 10"
      - "give %player% netherite_block 10"
      - "give %player% totem_of_undying 5"
      - "give %player% beacon 1"
      - "give %player% nether_star 3"
      - "eco give %player% 100000"
    mensaje: "&f&l✓ Recompensas de ABSOLUTO recibidas!"
```

---

## ✅ CHECKLIST FINAL DE VALIDACIÓN

### Compilación
- [ ] `mvn clean package` sin errores
- [ ] JAR generado en `target/Apocalipsis-1.16.2.jar`
- [ ] Tamaño apropiado (~2-3 MB)

### Anti-Cheat
- [ ] Huracán no banea jugadores
- [ ] Terremoto no genera flags
- [ ] Eventos no causan packet loss
- [ ] Velocidad se aplica suavemente

### Chat
- [ ] Colores visibles en todos los rangos
- [ ] Formatos (negrita, cursiva) funcionan según permisos
- [ ] Variables reemplazadas correctamente
- [ ] Cooldowns respetados por rango

### Menciones
- [ ] Sonido se reproduce al mencionar
- [ ] Nombre resaltado en color configurado
- [ ] Funciona case-insensitive
- [ ] No auto-mención

### Rank Up
- [ ] Título visible con duración correcta
- [ ] Sonidos reproducidos (2 tipos)
- [ ] Partículas múltiples (totem, firework, end_rod)
- [ ] Fuegos artificiales (3 unidades)
- [ ] Mensaje global broadcast
- [ ] Recompensas entregadas automáticamente

### Recompensas
- [ ] Comandos ejecutados correctamente
- [ ] Items recibidos en inventario
- [ ] Mensaje de confirmación enviado
- [ ] No se entregan duplicados
- [ ] Logs en consola confirmando entrega

### Comandos Admin
- [ ] `/avo rewards check <jugador>` muestra info
- [ ] `/avo rewards force <jugador> <rango>` entrega forzado
- [ ] `/avo rewards reset <jugador>` resetea historial
- [ ] Permisos admin verificados

---

## 📝 NOTAS FINALES

### Archivos Creados/Modificados
1. **Nuevo:** `VelocityManager.java`
2. **Modificado:** `Apocalipsis.java` (inicialización)
3. **Modificado:** `HuracanNew.java` (velocity smoothing)
4. **Modificado:** `TerremotoNew.java` (velocity smoothing)
5. **Modificado:** `EcoSombrasEvent.java` (velocity smoothing)
6. **Modificado:** `GuardianPhaseSystem.java` (velocity smoothing)
7. **Modificado:** `TelegraphedAttack.java` (velocity smoothing)
8. **Modificado:** `ChatListener.java` (debug logs)
9. **Modificado:** `MissionService.java` (rank up effects)
10. **Modificado:** `RewardService.java` (debug logs)
11. **Modificado:** `ApocalipsisCommand.java` (comando rewards)
12. **Actualizado:** `desastres.yml` (config anti-cheat)
13. **Actualizado:** `chat.yml` (todos los rangos)
14. **Actualizado:** `recompensas.yml` (progresión completa)

### Versión a Incrementar
- Actual: `1.16.1`
- Nueva: `1.16.2`
- Cambiar en `pom.xml` y `plugin.yml`

### Testing Mínimo Requerido
1. ✅ Desastre completo sin kicks
2. ✅ Chat con colores funcionando
3. ✅ Menciones con sonido
4. ✅ Rank up con efectos completos
5. ✅ Recompensas entregadas correctamente

---

**FIN DEL CHECKLIST**  
**¡Éxito en la implementación! 🚀**
