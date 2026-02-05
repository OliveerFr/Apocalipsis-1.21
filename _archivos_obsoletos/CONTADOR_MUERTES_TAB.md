# Sistema de Contador de Muertes Diarias en TAB

## 📋 Descripción
Sistema que muestra cuántas veces ha muerto cada jugador durante el día actual del servidor, directamente después de su nombre en la lista TAB.

## ✨ Características Implementadas

### 1. **DeathTracker** (Nuevo)
- **Ubicación**: `me.apocalipsis.stats.DeathTracker`
- **Función**: Rastrea las muertes de cada jugador por día
- **Características**:
  - Almacenamiento en memoria con Map<UUID, Integer>
  - Auto-reset automático al cambiar de día
  - Verificación cada minuto para detectar cambios de día
  - Métodos para agregar, obtener y limpiar muertes

### 2. **Integración en TAB**
- **Formato**: `[Rango] NombreJugador §8[§c☠ §fX§8]`
- **Ejemplos**:
  - Jugador sin muertes: `§e[VETERANO] §fOliveer`
  - Jugador con 3 muertes: `§e[VETERANO] §fOliveer §8[§c☠ §f3§8]`
  - Jugador con 15 muertes: `§e[VETERANO] §fOliveer §8[§c☠ §f15§8]`

### 3. **Actualización Automática**
- Se actualiza inmediatamente al morir
- Se actualiza cada 3 segundos (ciclo normal del TAB)
- Resetea todas las muertes al cambiar de día

## 🔧 Componentes Modificados

### **1. DeathTracker.java** (NUEVO)
```java
- addDeath(UUID): Registra una muerte
- getDeaths(UUID): Obtiene muertes del jugador
- resetAllDeaths(): Limpia todas las muertes (nuevo día)
- checkDayChange(): Verifica cambios de día cada minuto
```

### **2. TablistManager.java**
```java
// Constructor actualizado para recibir DeathTracker
public TablistManager(..., DeathTracker deathTracker)

// applyTabPrefix() ahora incluye contador de muertes
int deaths = deathTracker.getDeaths(p.getUniqueId());
String deathSuffix = deaths > 0 ? " §8[§c☠ §f" + deaths + "§8]" : "";
String finalTab = prefix + p.getName() + deathSuffix;
```

### **3. PlayerListener.java**
```java
@EventHandler
public void onPlayerDeath(PlayerDeathEvent event) {
    // Registrar muerte
    plugin.getDeathTracker().addDeath(uuid);
    
    // Actualizar TAB inmediatamente
    plugin.getTablistManager().applyTabPrefix(player);
    
    // ... resto del código de penalizaciones
}
```

### **4. Apocalipsis.java**
```java
// Inicialización
deathTracker = new DeathTracker(this, stateManager);
tablistManager = new TablistManager(this, stateManager, 
                  performanceAdapter, rankService, deathTracker);

// Getter
public DeathTracker getDeathTracker() {
    return deathTracker;
}
```

## 📊 Ciclo de Vida

### **Al Iniciar el Servidor**
1. DeathTracker se inicializa
2. Inicia tarea de verificación de día (cada minuto)
3. Todas las muertes empiezan en 0

### **Durante el Juego**
1. Jugador muere → Se incrementa contador
2. TAB se actualiza automáticamente
3. Nombre muestra: `NombreJugador §8[§c☠ §f<muertes>§8]`

### **Al Cambiar de Día**
1. DeathTracker detecta cambio de día
2. Llama a `resetAllDeaths()`
3. Todos los contadores vuelven a 0
4. TAB se actualiza en el siguiente ciclo (3 segundos)

### **Al Salir/Entrar**
- Las muertes se mantienen en memoria
- Si el jugador se desconecta y reconecta, conserva su contador
- Solo se resetea al cambiar de día del servidor

## 🎨 Diseño Visual

### Colores y Símbolos
- **§c☠**: Símbolo de calavera en rojo
- **§f**: Número de muertes en blanco
- **§8[  ]**: Brackets en gris oscuro

### Ejemplos en TAB
```
§c[LEYENDA] §fOliveer §8[§c☠ §f2§8]
§e[VETERANO] §fJugador1 §8[§c☠ §f7§8]
§a[INTERMEDIO] §fJugador2
§7[NOVATO] §fNuevo §8[§c☠ §f1§8]
```

## 🔄 Compatibilidad

### ✅ Compatible con:
- Sistema de rangos existente
- Prefijos de TAB personalizados
- Sistema de teams para ordenamiento
- Scoreboard y demás UI
- Todos los sistemas de muerte existentes

### 🎯 Prioridad de Actualización
1. **Inmediata**: Al morir (0.05s después)
2. **Periódica**: Cada 3 segundos (ciclo TAB normal)
3. **Reset**: Al cambiar día del servidor

## 📈 Rendimiento

### Optimizaciones
- **Cache**: Usa Map en memoria (O(1) lookup)
- **No persistencia**: No usa base de datos
- **Tarea ligera**: Solo verifica día cada 60 segundos
- **Smart update**: Solo actualiza TAB si cambió contenido

### Consumo de Recursos
- **Memoria**: ~24 bytes por jugador activo
- **CPU**: Mínimo (1 check cada 60s)
- **Red**: Sin overhead adicional (usa ciclo TAB existente)

## 🐛 Consideraciones

### Casos Edge
1. **Servidor reinicia**: Muertes se pierden (esperado)
2. **Cambio manual de día**: Se detecta y resetea correctamente
3. **Múltiples muertes seguidas**: Todas se registran correctamente

### Futuras Mejoras Posibles
- [ ] Top 3 jugadores con más muertes en footer
- [ ] Persistencia en base de datos (opcional)
- [ ] Estadísticas históricas por día
- [ ] Comandos para ver ranking de muertes

## 📝 Notas Técnicas

### Thread Safety
- DeathTracker usa operaciones atómicas (HashMap no sincronizado pero acceso single-thread)
- Actualizaciones de TAB en main thread (Bukkit scheduler)

### Sincronización con Día del Servidor
- Lee `stateManager.getCurrentDay()` cada 60 segundos
- Compara con día anterior almacenado
- Si cambió → reset automático

## 🚀 Instalación y Uso

### Para Desarrolladores
1. Los cambios están en:
   - `me.apocalipsis.stats.DeathTracker` (nuevo)
   - `me.apocalipsis.ui.TablistManager` (modificado)
   - `me.apocalipsis.listeners.PlayerListener` (modificado)
   - `me.apocalipsis.Apocalipsis` (modificado)

2. Compilar: `mvn package -DskipTests`
3. Reemplazar JAR en plugins/
4. Reiniciar servidor

### Para Usuarios
- **Funciona automáticamente** al iniciar el servidor
- **No requiere configuración** adicional
- **Visible para todos** los jugadores en TAB

## 📄 Versión
- **Plugin**: Apocalipsis v1.22.56
- **Minecraft**: 1.21.4
- **Feature**: Death Counter v1.0
- **Fecha**: 2026-01-28
