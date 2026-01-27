package me.apocalipsis.ciclos;

import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.WorldType;

import java.util.Date;

/**
 * Clase que representa los datos persistentes de un ciclo
 * Almacena toda la información necesaria para recuperar un ciclo después de reiniciar
 */
public class CicloData {
    
    // Identificadores
    private final String worldName;
    private final String displayName;
    private final String descripcion;
    
    // Estado
    private boolean activo;
    private boolean existe; // Si el mundo físico existe en disco
    
    // Configuración del mundo
    private World.Environment environment;
    private WorldType worldType;
    private Difficulty difficulty;
    private long seed;
    private boolean generateStructures;
    
    // Configuraciones de gameplay
    private boolean pvpEnabled;
    private boolean spawnMonsters;
    private boolean spawnAnimals;
    private boolean keepSpawnInMemory;
    
    // Spawn location
    private double spawnX;
    private double spawnY;
    private double spawnZ;
    
    // Timestamps
    private Date fechaCreacion;
    private Date ultimaActivacion;
    
    // Estadísticas
    private int jugadoresUnicos;
    private int tiempoTotalJugado; // En minutos
    
    public CicloData(String worldName) {
        this.worldName = worldName;
        this.displayName = worldName;
        this.descripcion = "Ciclo de supervivencia";
        this.activo = false;
        this.existe = false;
        this.fechaCreacion = new Date();
        
        // Valores por defecto
        this.environment = World.Environment.NORMAL;
        this.worldType = WorldType.NORMAL;
        this.difficulty = Difficulty.HARD;
        this.generateStructures = true;
        this.pvpEnabled = true;
        this.spawnMonsters = true;
        this.spawnAnimals = true;
        this.keepSpawnInMemory = true;
        this.jugadoresUnicos = 0;
        this.tiempoTotalJugado = 0;
    }
    
    // ==================== GETTERS ====================
    
    public String getWorldName() {
        return worldName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public boolean isActivo() {
        return activo;
    }
    
    public boolean existe() {
        return existe;
    }
    
    public World.Environment getEnvironment() {
        return environment;
    }
    
    public WorldType getWorldType() {
        return worldType;
    }
    
    public Difficulty getDifficulty() {
        return difficulty;
    }
    
    public long getSeed() {
        return seed;
    }
    
    public boolean generateStructures() {
        return generateStructures;
    }
    
    public boolean isPvpEnabled() {
        return pvpEnabled;
    }
    
    public boolean spawnMonsters() {
        return spawnMonsters;
    }
    
    public boolean spawnAnimals() {
        return spawnAnimals;
    }
    
    public boolean keepSpawnInMemory() {
        return keepSpawnInMemory;
    }
    
    public double getSpawnX() {
        return spawnX;
    }
    
    public double getSpawnY() {
        return spawnY;
    }
    
    public double getSpawnZ() {
        return spawnZ;
    }
    
    public Date getFechaCreacion() {
        return fechaCreacion;
    }
    
    public Date getUltimaActivacion() {
        return ultimaActivacion;
    }
    
    public int getJugadoresUnicos() {
        return jugadoresUnicos;
    }
    
    public int getTiempoTotalJugado() {
        return tiempoTotalJugado;
    }
    
    // ==================== SETTERS ====================
    
    public void setActivo(boolean activo) {
        this.activo = activo;
        if (activo) {
            this.ultimaActivacion = new Date();
        }
    }
    
    public void setExiste(boolean existe) {
        this.existe = existe;
    }
    
    public void setEnvironment(World.Environment environment) {
        this.environment = environment;
    }
    
    public void setWorldType(WorldType worldType) {
        this.worldType = worldType;
    }
    
    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }
    
    public void setSeed(long seed) {
        this.seed = seed;
    }
    
    public void setGenerateStructures(boolean generateStructures) {
        this.generateStructures = generateStructures;
    }
    
    public void setPvpEnabled(boolean pvpEnabled) {
        this.pvpEnabled = pvpEnabled;
    }
    
    public void setSpawnMonsters(boolean spawnMonsters) {
        this.spawnMonsters = spawnMonsters;
    }
    
    public void setSpawnAnimals(boolean spawnAnimals) {
        this.spawnAnimals = spawnAnimals;
    }
    
    public void setKeepSpawnInMemory(boolean keepSpawnInMemory) {
        this.keepSpawnInMemory = keepSpawnInMemory;
    }
    
    public void setSpawnLocation(double x, double y, double z) {
        this.spawnX = x;
        this.spawnY = y;
        this.spawnZ = z;
    }
    
    public void setFechaCreacion(Date fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
    
    public void setUltimaActivacion(Date ultimaActivacion) {
        this.ultimaActivacion = ultimaActivacion;
    }
    
    public void incrementarJugadoresUnicos() {
        this.jugadoresUnicos++;
    }
    
    public void agregarTiempoJugado(int minutos) {
        this.tiempoTotalJugado += minutos;
    }
    
    @Override
    public String toString() {
        return "CicloData{" +
                "worldName='" + worldName + '\'' +
                ", activo=" + activo +
                ", existe=" + existe +
                ", environment=" + environment +
                ", difficulty=" + difficulty +
                ", jugadores=" + jugadoresUnicos +
                '}';
    }
}
