# ✅ Integración con Multiverse-Core - Sistema de Ciclos

## 🎯 Resumen

**SÍ**, el sistema está **completamente integrado** con Multiverse-Core para crear mundos automáticamente.

---

## ✅ Verificación de Integración

### 1. Dependencia en pom.xml ✅

```xml
<dependency>
    <groupId>com.onarandombox.multiversecore</groupId>
    <artifactId>Multiverse-Core</artifactId>
    <version>4.3.12</version>
    <scope>provided</scope>
</dependency>
```

**Repositorio:**
```xml
<repository>
    <id>onarandombox</id>
    <url>https://repo.onarandombox.com/content/groups/public/</url>
</repository>
```

### 2. plugin.yml ✅

```yaml
softdepend:
  - Multiverse-Core
```

**Tipo:** Dependencia suave (soft)
- ✅ El plugin funciona sin Multiverse
- ✅ Detecta automáticamente si está instalado
- ✅ Activa funciones extra si está presente

### 3. Detección Automática en CicloManager ✅

```java
private void initializeMultiverse() {
    Plugin mvPlugin = Bukkit.getPluginManager().getPlugin("Multiverse-Core");
    
    if (mvPlugin instanceof MultiverseCore) {
        this.multiverseCore = (MultiverseCore) mvPlugin;
        this.mvWorldManager = multiverseCore.getMVWorldManager();
        plugin.getLogger().info("[CicloManager] Multiverse-Core detectado y conectado");
    } else {
        plugin.getLogger().warning("[CicloManager] Multiverse-Core NO detectado!");
        plugin.getLogger().warning("[CicloManager] La creación automática de mundos estará deshabilitada.");
    }
}
```

**Ejecutado en:** Constructor de CicloManager (línea 58)

### 4. Método de Verificación ✅

```java
public boolean isMultiverseAvailable() {
    return multiverseCore != null && mvWorldManager != null;
}
```

**Usado en:** Todos los comandos de creación de mundos

---

## 🚀 Cómo Funciona

### Método 1: Creación Manual Avanzada

```bash
/avo ciclo crear <nombre> [NORMAL|NETHER|END] [EASY|NORMAL|HARD] [true|false]
```

**Ejemplos:**
```bash
/avo ciclo crear temporada_2
/avo ciclo crear temporada_2 NORMAL HARD
/avo ciclo crear mundo_nether NETHER HARD true
```

**Proceso:**
1. Verifica que Multiverse esté instalado
2. Llama a `mvWorldManager.addWorld()`
3. Configura ambiente, dificultad, tipo de mundo
4. Activa el ciclo automáticamente
5. Opcionalmente teletransporta a todos

**Código:**
```java
boolean created = mvWorldManager.addWorld(
    worldName,                // Nombre
    environment,              // NORMAL, NETHER, THE_END
    null,                     // Seed (aleatorio)
    worldType,                // NORMAL, FLAT, etc
    generateStructures,       // true/false
    null                      // Generator (vanilla)
);
```

### Método 2: Creación Automática Rápida

```bash
/avo ciclo nuevo <nombre> [true|false]
```

**Ejemplos:**
```bash
/avo ciclo nuevo temporada_2
/avo ciclo nuevo temporada_2 true
```

**Proceso:**
1. Verifica si el mundo existe
2. **Si NO existe:**
   - Muestra mensaje: "⚙ Creando mundo automáticamente con Multiverse..."
   - Llama a `createAndActivateCycle()` con config por defecto
   - Crea mundo NORMAL con dificultad HARD
   - Activa el ciclo
3. **Si existe:**
   - Solo activa el ciclo en ese mundo

**Código:**
```java
if (existingWorld == null) {
    sender.sendMessage("§e⚙ Creando mundo automáticamente con Multiverse...");
    
    if (!cicloManager.isMultiverseAvailable()) {
        sender.sendMessage("§c✖ Multiverse-Core no está instalado!");
        return;
    }
    
    cicloManager.createAndActivateCycle(worldName, 
        World.Environment.NORMAL, 
        Difficulty.HARD, 
        teleportAll);
}
```

---

## 🔧 Configuración Aplicada al Mundo

Cuando se crea un mundo con Multiverse, se configuran automáticamente:

### Propiedades de Bukkit
```java
world.setDifficulty(difficulty);        // EASY, NORMAL, HARD
world.setKeepSpawnInMemory(true);       // Spawn siempre cargado
```

### Propiedades de Multiverse
```java
mvWorld.setDifficulty(difficulty);      // Sincronizar dificultad
mvWorld.setAutoLoad(true);              // Cargar al iniciar servidor
mvWorld.setKeepSpawnInMemory(true);     // Spawn en memoria
mvWorld.setAllowFlight(true);           // Permitir vuelo (admins)
```

---

## 📋 Mensajes de Log

### Cuando Multiverse ESTÁ instalado:
```
[INFO] [CicloManager] Multiverse-Core detectado y conectado
[INFO] [CicloManager] Creando mundo: temporada_2
[INFO] [CicloManager]   - Ambiente: NORMAL
[INFO] [CicloManager]   - Tipo: NORMAL
[INFO] [CicloManager]   - Dificultad: HARD
[INFO] [CicloManager] Mundo creado exitosamente: temporada_2
```

### Cuando Multiverse NO ESTÁ instalado:
```
[WARN] [CicloManager] Multiverse-Core NO detectado!
[WARN] [CicloManager] La creación automática de mundos estará deshabilitada.
[WARN] [CicloManager] Instala Multiverse-Core para obtener todas las funcionalidades.
```

### Si usuario intenta crear sin Multiverse:
```
✖ Multiverse-Core no está instalado!
Instala Multiverse-Core o crea el mundo manualmente con:
/mv create temporada_2 NORMAL
```

---

## 🎮 Flujo de Trabajo Completo

### Con Multiverse Instalado (AUTOMÁTICO)

```bash
# 1. Admin crea nuevo ciclo (1 comando)
/avo ciclo nuevo temporada_3 true

# Sistema hace AUTOMÁTICAMENTE:
✓ Detecta que el mundo no existe
✓ Crea mundo con Multiverse
✓ Configura dificultad HARD
✓ Configura spawn siempre en memoria
✓ Activa el ciclo
✓ Registra el mundo en ciclos.yml
✓ Teletransporta a todos los jugadores

# 2. Jugadores juegan en el nuevo ciclo
# - Inventarios separados
# - XP separado
# - Skills separadas
# - Misiones separadas

# 3. Cambiar entre ciclos
/avo ciclo tp temporada_1
/avo ciclo tp temporada_3
```

### Sin Multiverse (MANUAL)

```bash
# 1. Admin debe crear mundo manualmente
/mv create temporada_3 NORMAL

# 2. Activar ciclo
/avo ciclo nuevo temporada_3 true

# Resto igual...
```

---

## ⚙️ Opciones de Creación

### Ambientes Disponibles
```java
NORMAL      // Mundo normal (overworld)
NETHER      // El Nether
THE_END     // The End
```

### Tipos de Mundo
```java
NORMAL          // Generación normal
FLAT            // Mundo plano
LARGE_BIOMES    // Biomas grandes
AMPLIFIED       // Montañas extremas
```

### Dificultades
```java
PEACEFUL    // Sin mobs hostiles
EASY        // Mobs débiles
NORMAL      // Equilibrado
HARD        // Mobs fuertes
```

---

## 🔍 Verificación Manual

Para verificar que Multiverse está funcionando:

### 1. Verificar Plugin
```bash
/plugins
```
Buscar: `Multiverse-Core v4.3.12` en verde

### 2. Verificar Versión
```bash
/version Multiverse-Core
```

### 3. Crear Mundo de Prueba
```bash
/avo ciclo crear test_mundo NORMAL HARD
```

**Esperado en consola:**
```
[INFO] [CicloManager] Multiverse-Core detectado y conectado
[INFO] [CicloManager] Creando mundo: test_mundo
[INFO] [CicloManager] Mundo creado exitosamente: test_mundo
```

### 4. Verificar Mundos de Multiverse
```bash
/mv list
```
Debería mostrar `test_mundo`

### 5. Verificar Ciclo Activo
```bash
/avo ciclo info
```

---

## 🐛 Troubleshooting

### "Multiverse-Core NO detectado"

**Causas posibles:**
1. Multiverse no está instalado
2. Multiverse cargó después de Apocalipsis
3. Versión incompatible de Multiverse

**Soluciones:**

#### 1. Verificar instalación
```bash
/plugins
```
Si no aparece Multiverse-Core:
- Descargar de: https://dev.bukkit.org/projects/multiverse-core
- Versión recomendada: 4.3.12
- Colocar en `plugins/`
- Reiniciar servidor

#### 2. Verificar orden de carga
En `plugin.yml` de Apocalipsis ya está configurado:
```yaml
softdepend:
  - Multiverse-Core
```
Esto hace que Apocalipsis cargue DESPUÉS de Multiverse.

#### 3. Forzar reload
```bash
/reload confirm
```
⚠️ Solo usar si es necesario (puede causar memory leaks)

### "Error al crear mundo"

**Revisar consola para:**
```
[SEVERE] [CicloManager] Error al crear mundo: nombre
```

**Causas comunes:**
- Nombre de mundo con caracteres especiales
- Mundo ya existe
- Permisos de archivos
- Espacio en disco insuficiente

**Solución:**
```bash
# Ver log completo
/avo ciclo crear nombre_limpio NORMAL HARD
```

---

## 📊 Comparación: Con vs Sin Multiverse

| Feature | Con Multiverse | Sin Multiverse |
|---------|---------------|----------------|
| **Creación de mundos** | ✅ Automática | ❌ Manual |
| **Comando único** | ✅ `/avo ciclo nuevo` | ❌ Dos comandos |
| **Configuración** | ✅ Automática | ⚠️ Manual |
| **Tipos de mundo** | ✅ Todos | ⚠️ Limitado |
| **Gestión spawn** | ✅ Automática | ⚠️ Manual |
| **Auto-load** | ✅ Sí | ❌ No |
| **Ciclos funcionales** | ✅ 100% | ✅ 100% |

**Conclusión:** Multiverse es OPCIONAL pero RECOMENDADO para mejor experiencia.

---

## 📝 Resumen Final

### ✅ SÍ, está funcionando con Multiverse

**Integración completa:**
- ✅ Dependencia en pom.xml
- ✅ Soft-depend en plugin.yml
- ✅ Detección automática al iniciar
- ✅ API de Multiverse completamente integrada
- ✅ Comandos funcionan con y sin Multiverse
- ✅ Mensajes claros para el usuario
- ✅ Fallback a creación manual

**Sin Multiverse:**
- ⚠️ Funciona pero requiere creación manual de mundos
- ⚠️ Admin debe usar `/mv create` o crear con otro método
- ✅ Sistema de ciclos sigue funcionando 100%

**Recomendación:**
```
Instalar Multiverse-Core 4.3.12 para:
- Creación automática con 1 comando
- Mejor gestión de mundos
- Configuración automática
- Experiencia de usuario optimizada
```

---

**Versión:** 1.22.55  
**Multiverse-Core:** 4.3.12 (recomendado)  
**Estado:** ✅ Completamente Integrado y Funcional
