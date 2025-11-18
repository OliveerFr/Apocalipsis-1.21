package me.apocalipsis.events.gameplay;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EventAudioSystem - Sistema de audio avanzado AAA
 * 
 * Características:
 * - Música dinámica por acto con transiciones suaves
 * - Sonido posicional 3D con cálculo de distancia
 * - Simulación de reverb (CAVE, TEMPLE, OPEN)
 * - Musical stingers para momentos clave
 * - Heartbeat intensificado por salud
 * - Audio ducking automático
 * 
 * @author Apocalipsis Team
 * @version 1.0
 */
public class EventAudioSystem {
    
    private final Plugin plugin;
    
    // Estado de audio por jugador
    private final Map<UUID, MusicTrack> currentMusic = new ConcurrentHashMap<>();
    private final Map<UUID, Float> musicVolume = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> musicTasks = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> heartbeatTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastStingerTime = new ConcurrentHashMap<>();
    
    // Configuración
    private static final float DEFAULT_VOLUME = 0.7f;
    private static final float DUCKING_VOLUME = 0.3f;
    private static final int CROSSFADE_DURATION = 40; // 2 segundos
    private static final int STINGER_COOLDOWN = 1000; // 1 segundo entre stingers
    
    /**
     * Tracks de música para cada acto
     */
    public enum MusicTrack {
        ACTIVATION("Acto 1: Activación", Sound.MUSIC_DISC_13, 1.0f, true),
        NUCLEUS("Acto 2: Núcleo", Sound.MUSIC_DISC_WARD, 1.1f, true),
        GUARDIAN("Acto 3: Guardián", Sound.MUSIC_DISC_PIGSTEP, 1.2f, true),
        VICTORY("Victoria", Sound.MUSIC_DISC_CAT, 1.0f, false),
        DEFEAT("Derrota", Sound.MUSIC_DISC_11, 0.9f, false);
        
        private final String name;
        private final Sound sound;
        private final float pitch;
        private final boolean loop;
        
        MusicTrack(String name, Sound sound, float pitch, boolean loop) {
            this.name = name;
            this.sound = sound;
            this.pitch = pitch;
            this.loop = loop;
        }
        
        public String getName() { return name; }
        public Sound getSound() { return sound; }
        public float getPitch() { return pitch; }
        public boolean shouldLoop() { return loop; }
    }
    
    /**
     * Tipos de sonido para efectos especiales
     */
    public enum SoundType {
        // Sombras
        SHADOW_SPAWN(Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 0.8f),
        SHADOW_ATTACK(Sound.ENTITY_VEX_AMBIENT, 1.2f, 0.7f),
        SHADOW_DEATH(Sound.ENTITY_PHANTOM_DEATH, 0.8f, 1.0f),
        
        // Anclas
        ANCHOR_ACTIVATE(Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 1.0f),
        ANCHOR_SEALED(Sound.BLOCK_END_PORTAL_FRAME_FILL, 1.0f, 1.2f),
        ANCHOR_BREAK(Sound.BLOCK_GLASS_BREAK, 0.5f, 0.8f),
        
        // Núcleo
        NUCLEUS_SPAWN(Sound.ENTITY_WITHER_SPAWN, 0.7f, 1.5f),
        NUCLEUS_PULSE(Sound.BLOCK_BEACON_AMBIENT, 1.0f, 0.9f),
        NUCLEUS_CHARGE(Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.2f, 1.0f),
        
        // Guardián
        GUARDIAN_SPAWN(Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 2.0f),
        GUARDIAN_ROAR(Sound.ENTITY_WARDEN_ROAR, 1.0f, 1.5f),
        GUARDIAN_ATTACK(Sound.ENTITY_WARDEN_ATTACK_IMPACT, 1.2f, 1.0f),
        GUARDIAN_DAMAGE(Sound.ENTITY_IRON_GOLEM_HURT, 0.7f, 1.2f),
        
        // Ambiente
        AMBIENT_WIND(Sound.ITEM_ELYTRA_FLYING, 0.4f, 0.5f),
        AMBIENT_WHISPER(Sound.ENTITY_PHANTOM_AMBIENT, 0.3f, 0.6f),
        AMBIENT_TENSION(Sound.BLOCK_PORTAL_AMBIENT, 0.5f, 0.7f);
        
        private final Sound sound;
        private final float pitch;
        private final float baseVolume;
        
        SoundType(Sound sound, float pitch, float baseVolume) {
            this.sound = sound;
            this.pitch = pitch;
            this.baseVolume = baseVolume;
        }
        
        public Sound getSound() { return sound; }
        public float getPitch() { return pitch; }
        public float getBaseVolume() { return baseVolume; }
    }
    
    /**
     * Tipos de reverb para diferentes espacios
     */
    public enum ReverbType {
        CAVE(3, 0.4f, 0.7f),      // Cueva: múltiples ecos, decay medio
        TEMPLE(5, 0.6f, 0.5f),    // Templo: muchos ecos, decay lento
        OPEN(1, 0.2f, 0.9f);      // Campo abierto: pocos ecos, decay rápido
        
        private final int echoCount;
        private final float echoDelay;
        private final float decayFactor;
        
        ReverbType(int echoCount, float echoDelay, float decayFactor) {
            this.echoCount = echoCount;
            this.echoDelay = echoDelay;
            this.decayFactor = decayFactor;
        }
        
        public int getEchoCount() { return echoCount; }
        public float getEchoDelay() { return echoDelay; }
        public float getDecayFactor() { return decayFactor; }
    }
    
    public EventAudioSystem(Plugin plugin) {
        this.plugin = plugin;
    }
    
    // ==================== MÚSICA DINÁMICA ====================
    
    /**
     * Reproduce música de acto con transición suave
     * 
     * @param player Jugador
     * @param track Track de música a reproducir
     */
    public void playActMusic(Player player, MusicTrack track) {
        UUID uuid = player.getUniqueId();
        
        // Si ya está sonando este track, no hacer nada
        MusicTrack current = currentMusic.get(uuid);
        if (current == track) {
            return;
        }
        
        // Crossfade: bajar volumen de música actual
        if (current != null) {
            crossfadeOut(player);
        }
        
        // Esperar al crossfade antes de iniciar nueva música
        new BukkitRunnable() {
            @Override
            public void run() {
                currentMusic.put(uuid, track);
                musicVolume.put(uuid, 0.0f);
                
                // Iniciar nueva música con fade in
                startMusicLoop(player, track);
            }
        }.runTaskLater(plugin, current != null ? CROSSFADE_DURATION : 0L);
    }
    
    /**
     * Inicia loop de música con fade in
     */
    private void startMusicLoop(Player player, MusicTrack track) {
        UUID uuid = player.getUniqueId();
        
        // Cancelar task anterior si existe
        BukkitTask oldTask = musicTasks.remove(uuid);
        if (oldTask != null) {
            oldTask.cancel();
        }
        
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                
                // Fade in durante los primeros 2 segundos
                if (ticks < CROSSFADE_DURATION) {
                    float volume = (float) ticks / CROSSFADE_DURATION * DEFAULT_VOLUME;
                    musicVolume.put(uuid, volume);
                    ticks++;
                } else {
                    musicVolume.put(uuid, DEFAULT_VOLUME);
                }
                
                // Reproducir sonido
                float volume = musicVolume.getOrDefault(uuid, DEFAULT_VOLUME);
                player.playSound(player.getLocation(), track.getSound(), volume, track.getPitch());
                
                // Si no es loop, cancelar después de reproducción
                if (!track.shouldLoop()) {
                    cancel();
                    currentMusic.remove(uuid);
                }
            }
        }.runTaskTimer(plugin, 0L, getTrackDuration(track));
        
        musicTasks.put(uuid, task);
    }
    
    /**
     * Crossfade out de música actual
     */
    private void crossfadeOut(Player player) {
        UUID uuid = player.getUniqueId();
        float currentVol = musicVolume.getOrDefault(uuid, DEFAULT_VOLUME);
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= CROSSFADE_DURATION) {
                    cancel();
                    stopMusic(player);
                    return;
                }
                
                float volume = currentVol * (1.0f - (float) ticks / CROSSFADE_DURATION);
                musicVolume.put(uuid, volume);
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    /**
     * Detiene la música de un jugador
     */
    public void stopMusic(Player player) {
        UUID uuid = player.getUniqueId();
        
        BukkitTask task = musicTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        
        currentMusic.remove(uuid);
        musicVolume.remove(uuid);
        
        // Detener todos los sonidos de música
        player.stopSound(Sound.MUSIC_DISC_13);
        player.stopSound(Sound.MUSIC_DISC_WARD);
        player.stopSound(Sound.MUSIC_DISC_PIGSTEP);
        player.stopSound(Sound.MUSIC_DISC_CAT);
        player.stopSound(Sound.MUSIC_DISC_11);
    }
    
    /**
     * Obtiene duración aproximada de un track en ticks
     */
    private long getTrackDuration(MusicTrack track) {
        switch (track) {
            case ACTIVATION:
                return 3560L; // ~178 segundos
            case NUCLEUS:
                return 6260L; // ~313 segundos
            case GUARDIAN:
                return 2980L; // ~149 segundos
            case VICTORY:
                return 3320L; // ~166 segundos
            case DEFEAT:
                return 1420L; // ~71 segundos
            default:
                return 3000L;
        }
    }
    
    // ==================== SONIDO POSICIONAL 3D ====================
    
    /**
     * Reproduce sonido con posición 3D y cálculo de distancia
     * 
     * @param player Jugador que escucha
     * @param location Ubicación del sonido
     * @param soundType Tipo de sonido
     * @param maxDistance Distancia máxima de audición (bloques)
     */
    public void playPositionalSound(Player player, Location location, SoundType soundType, double maxDistance) {
        Location playerLoc = player.getLocation();
        double distance = playerLoc.distance(location);
        
        // Si está fuera del rango, no reproducir
        if (distance > maxDistance) {
            return;
        }
        
        // Calcular volumen basado en distancia (inverse square law)
        float distanceFactor = (float) (1.0 - Math.pow(distance / maxDistance, 2));
        float volume = soundType.getBaseVolume() * distanceFactor;
        
        // Calcular pitch con ligero efecto Doppler
        Vector playerVel = player.getVelocity();
        Vector directionToSound = location.toVector().subtract(playerLoc.toVector()).normalize();
        double dopplerShift = playerVel.dot(directionToSound) * 0.1; // Sutil
        float pitch = soundType.getPitch() * (1.0f + (float) dopplerShift);
        
        // Clamp valores
        volume = Math.max(0.0f, Math.min(2.0f, volume));
        pitch = Math.max(0.5f, Math.min(2.0f, pitch));
        
        // Reproducir desde la ubicación exacta
        player.playSound(location, soundType.getSound(), volume, pitch);
    }
    
    /**
     * Reproduce sonido posicional con reverb
     */
    public void playPositionalSoundWithReverb(Player player, Location location, SoundType soundType, 
                                              double maxDistance, ReverbType reverb) {
        // Sonido principal
        playPositionalSound(player, location, soundType, maxDistance);
        
        // Agregar ecos con delays
        for (int i = 1; i <= reverb.getEchoCount(); i++) {
            final int echoIndex = i;
            
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) return;
                    
                    // Volumen decae con cada eco
                    float echoVolume = soundType.getBaseVolume() * 
                                      (float) Math.pow(reverb.getDecayFactor(), echoIndex);
                    
                    // Pitch ligeramente más bajo en ecos
                    float echoPitch = soundType.getPitch() * (1.0f - echoIndex * 0.05f);
                    
                    player.playSound(location, soundType.getSound(), echoVolume, echoPitch);
                }
            }.runTaskLater(plugin, (long) (echoIndex * reverb.getEchoDelay() * 20));
        }
    }
    
    // ==================== REVERB DINÁMICO ====================
    
    /**
     * Aplica reverb dinámico basado en entorno
     * 
     * @param player Jugador
     * @param location Ubicación para analizar
     * @return Tipo de reverb detectado
     */
    public ReverbType detectReverbType(Player player, Location location) {
        // Analizar bloques circundantes
        int solidBlocks = 0;
        int totalChecked = 0;
        
        // Verificar en radio de 5 bloques
        for (int x = -5; x <= 5; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -5; z <= 5; z++) {
                    Location checkLoc = location.clone().add(x, y, z);
                    if (checkLoc.getBlock().getType().isSolid()) {
                        solidBlocks++;
                    }
                    totalChecked++;
                }
            }
        }
        
        float solidRatio = (float) solidBlocks / totalChecked;
        
        // Determinar reverb según densidad de bloques
        if (solidRatio > 0.6f) {
            return ReverbType.CAVE; // Espacio cerrado
        } else if (solidRatio > 0.3f) {
            return ReverbType.TEMPLE; // Semi-cerrado
        } else {
            return ReverbType.OPEN; // Campo abierto
        }
    }
    
    // ==================== MUSICAL STINGERS ====================
    
    /**
     * Reproduce un stinger musical para momento clave
     * 
     * @param player Jugador
     * @param stingerType Tipo de stinger
     */
    public void playStinger(Player player, StingerType stingerType) {
        UUID uuid = player.getUniqueId();
        
        // Verificar cooldown
        long lastTime = lastStingerTime.getOrDefault(uuid, 0L);
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTime < STINGER_COOLDOWN) {
            return;
        }
        
        lastStingerTime.put(uuid, currentTime);
        
        // Aplicar ducking a música de fondo
        applyMusicDucking(player, stingerType.getDuration());
        
        // Reproducir stinger
        player.playSound(player.getLocation(), stingerType.getSound(), 
                        stingerType.getVolume(), stingerType.getPitch());
    }
    
    /**
     * Tipos de stingers musicales
     */
    public enum StingerType {
        ANCHOR_SEALED(Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f, 30),
        SHADOW_KILLED(Sound.BLOCK_NOTE_BLOCK_HARP, 0.8f, 1.2f, 20),
        NUCLEUS_SPAWNED(Sound.BLOCK_NOTE_BLOCK_PLING, 1.2f, 0.8f, 60),
        GUARDIAN_SPAWNED(Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1.5f, 0.5f, 80),
        PLAYER_DEATH(Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.6f, 40),
        VICTORY(Sound.ENTITY_PLAYER_LEVELUP, 1.2f, 1.0f, 60);
        
        private final Sound sound;
        private final float volume;
        private final float pitch;
        private final int duration; // ticks
        
        StingerType(Sound sound, float volume, float pitch, int duration) {
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
            this.duration = duration;
        }
        
        public Sound getSound() { return sound; }
        public float getVolume() { return volume; }
        public float getPitch() { return pitch; }
        public int getDuration() { return duration; }
    }
    
    /**
     * Aplica ducking (reducción temporal de volumen) a música
     */
    private void applyMusicDucking(Player player, int durationTicks) {
        UUID uuid = player.getUniqueId();
        float originalVolume = musicVolume.getOrDefault(uuid, DEFAULT_VOLUME);
        
        // Bajar volumen inmediatamente
        musicVolume.put(uuid, DUCKING_VOLUME);
        
        // Restaurar después del stinger
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    musicVolume.put(uuid, originalVolume);
                }
            }
        }.runTaskLater(plugin, durationTicks);
    }
    
    // ==================== HEARTBEAT SYSTEM ====================
    
    /**
     * Inicia sistema de heartbeat intensificado por salud
     * 
     * @param player Jugador
     */
    public void startHeartbeat(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Cancelar heartbeat anterior si existe
        stopHeartbeat(player);
        
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                
                // Calcular intensidad basada en salud
                double healthPercent = player.getHealth() / player.getMaxHealth();
                
                if (healthPercent > 0.7) {
                    // Salud alta: sin heartbeat
                    return;
                }
                
                // Determinar frecuencia y pitch según salud
                long interval;
                float pitch;
                float volume;
                
                if (healthPercent < 0.3) {
                    // Crítico: muy rápido
                    interval = 10L; // 0.5 segundos
                    pitch = 1.2f;
                    volume = 1.0f;
                } else if (healthPercent < 0.5) {
                    // Bajo: rápido
                    interval = 20L; // 1 segundo
                    pitch = 1.0f;
                    volume = 0.8f;
                } else {
                    // Medio: moderado
                    interval = 30L; // 1.5 segundos
                    pitch = 0.8f;
                    volume = 0.6f;
                }
                
                // Reproducir heartbeat con reverb CAVE
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 
                               volume, pitch);
                
                // Echo sutil
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (player.isOnline()) {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 
                                           volume * 0.4f, pitch * 0.9f);
                        }
                    }
                }.runTaskLater(plugin, 5L);
            }
        }.runTaskTimer(plugin, 0L, 20L); // Revisar cada segundo
        
        heartbeatTasks.put(uuid, task);
    }
    
    /**
     * Detiene el heartbeat de un jugador
     */
    public void stopHeartbeat(Player player) {
        UUID uuid = player.getUniqueId();
        BukkitTask task = heartbeatTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }
    
    // ==================== AMBIENT LOOPS ====================
    
    /**
     * Inicia ambient loop de tensión
     * 
     * @param player Jugador
     */
    public void startAmbientTension(Player player) {
        new BukkitRunnable() {
            int count = 0;
            
            @Override
            public void run() {
                if (!player.isOnline() || count >= 10) {
                    cancel();
                    return;
                }
                
                // Viento fantasmal
                playPositionalSound(player, player.getLocation(), 
                                  SoundType.AMBIENT_WIND, 32.0);
                
                // Susurros ocasionales
                if (count % 3 == 0) {
                    playPositionalSound(player, player.getLocation(), 
                                      SoundType.AMBIENT_WHISPER, 24.0);
                }
                
                count++;
            }
        }.runTaskTimer(plugin, 0L, 60L); // Cada 3 segundos
    }
    
    // ==================== CLEANUP ====================
    
    /**
     * Limpia todos los estados de audio de un jugador
     */
    public void cleanup(Player player) {
        stopMusic(player);
        stopHeartbeat(player);
        lastStingerTime.remove(player.getUniqueId());
    }
    
    /**
     * Limpia todos los estados de audio
     */
    public void cleanupAll() {
        // Cancelar todas las tasks de música
        musicTasks.values().forEach(BukkitTask::cancel);
        musicTasks.clear();
        
        // Cancelar todas las tasks de heartbeat
        heartbeatTasks.values().forEach(BukkitTask::cancel);
        heartbeatTasks.clear();
        
        // Limpiar mapas
        currentMusic.clear();
        musicVolume.clear();
        lastStingerTime.clear();
    }
}
