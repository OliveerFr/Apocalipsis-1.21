# 🔧 Fix de Persistencia de Waypoints v1.22.57

## ❌ Problema Identificado
Los waypoints se reiniciaban al reiniciar el servidor debido a:

1. **Guardado asíncrono en onPlayerQuit**: El guardado se hacía en un thread asíncrono, lo que causaba que si el servidor se cerraba inmediatamente después, el guardado no se completaba.

2. **Falta de creación explícita de directorio**: El código no verificaba/creaba el directorio del plugin antes de intentar guardar.

3. **Logging insuficiente**: No había suficiente información para diagnosticar problemas de guardado/carga.

---

## ✅ Soluciones Implementadas

### 1️⃣ **Guardado Síncrono en Desconexión**
```java
// ANTES (asíncrono - podía perderse):
Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
    saveWaypoints();
});

// AHORA (síncrono - garantizado):
saveWaypoints();
```

**Impacto**: Garantiza que los waypoints se guarden antes de que el jugador sea desconectado completamente.

### 2️⃣ **Creación Automática de Directorio y Archivo**
```java
// Asegurar que el directorio existe
File dataFolder = plugin.getDataFolder();
if (!dataFolder.exists()) {
    dataFolder.mkdirs();
}

// Crear archivo si no existe
File waypointsFile = new File(dataFolder, "waypoints.yml");
if (!waypointsFile.exists()) {
    waypointsFile.createNewFile();
}
```

**Impacto**: Elimina errores de "archivo no encontrado" al guardar.

### 3️⃣ **Logging Detallado**
```java
// Guardado:
plugin.getLogger().info("[Skills] ✓ Waypoints guardados: X waypoints de Y jugadores en /ruta/absoluta");

// Carga:
plugin.getLogger().info("[Skills] Cargando waypoints desde: /ruta/absoluta");
plugin.getLogger().info("[Skills] Archivo waypoints.yml no existe todavía");

// Desconexión:
plugin.getLogger().info("[Skills] Waypoints guardados por desconexión de Jugador");
```

**Impacto**: Permite diagnosticar problemas de persistencia revisando los logs.

---

## 🧪 Pruebas de Verificación

### **Test 1: Verificar Guardado Manual**
```bash
# 1. Crear un waypoint
/wp set test_persistencia

# 2. Verificar en consola que aparezca:
[Skills] ✓ Waypoints guardados exitosamente: 1 waypoints de 1 jugadores en ...

# 3. Verificar que existe el archivo:
# Windows: plugins/Apocalipsis/waypoints.yml
# Linux: plugins/Apocalipsis/waypoints.yml
```

### **Test 2: Verificar Persistencia al Desconectar**
```bash
# 1. Crear waypoint
/wp set casa

# 2. Desconectarse del servidor
# 3. Verificar en consola:
[Skills] Waypoints guardados por desconexión de TuNombre

# 4. Verificar archivo waypoints.yml actualizado
```

### **Test 3: Verificar Persistencia al Reiniciar Servidor**
```bash
# 1. Crear waypoints
/wp set casa
/wp set granja
/wp set spawn

# 2. Verificar que existen
/wp

# 3. Reiniciar servidor completamente (stop + start)

# 4. Verificar en consola al iniciar:
[Skills] Cargando waypoints desde: /ruta/waypoints.yml
[Skills] Cargados 3 waypoints de 1 jugadores

# 5. Ingresar al servidor
/wp
# Debería mostrar: casa, granja, spawn
```

---

## 📁 Ubicación del Archivo

El archivo `waypoints.yml` debería estar en:
```
plugins/Apocalipsis/waypoints.yml
```

**Ruta absoluta** (verificar en logs):
```
# Windows ejemplo:
Z:\riolu\Videos\Eventos\Apocalipsis-1.21.8\plugins\Apocalipsis\waypoints.yml

# Linux ejemplo:
/home/minecraft/server/plugins/Apocalipsis/waypoints.yml
```

---

## 🔍 Formato del Archivo waypoints.yml

Ejemplo de cómo debería verse:
```yaml
metadata:
  version: '2.0'
  last_save: 1738195200000
  server_time: '2026-01-29 15:30:00'
  total_waypoints: 3
waypoints:
  550e8400-e29b-41d4-a716-446655440000:  # UUID del jugador
    casa:
      world: world
      x: 100.5
      y: 64.0
      z: -200.3
      yaw: 45.0
      pitch: 0.0
      created_time: 1738195200000
      creation_world: world
    granja:
      world: world
      x: 350.2
      y: 70.0
      z: 450.8
      yaw: 90.0
      pitch: 0.0
      created_time: 1738195300000
      creation_world: world
```

---

## 🚨 Diagnóstico de Problemas

### **Problema: "Waypoints no se guardan"**

1. **Verificar logs al crear waypoint:**
   ```
   [Skills] ✓ Waypoints guardados exitosamente: ...
   ```
   
   Si NO aparece este mensaje:
   - Revisar `skills.yml` → `waypoints.persistencia: true`
   - Verificar permisos de escritura en carpeta `plugins/Apocalipsis/`

2. **Verificar que el archivo existe:**
   ```bash
   # Buscar archivo
   ls -la plugins/Apocalipsis/waypoints.yml
   
   # O en Windows
   dir plugins\Apocalipsis\waypoints.yml
   ```

3. **Verificar contenido del archivo:**
   ```bash
   # Ver archivo
   cat plugins/Apocalipsis/waypoints.yml
   
   # O en Windows
   type plugins\Apocalipsis\waypoints.yml
   ```

### **Problema: "Waypoints desaparecen al reiniciar"**

1. **Verificar logs al cerrar servidor:**
   ```
   [Skills] Waypoints guardados por desconexión de ...
   [Skills] ✓ Waypoints guardados: ...
   ```

2. **Verificar logs al iniciar servidor:**
   ```
   [Skills] Cargando waypoints desde: ...
   [Skills] Cargados X waypoints de Y jugadores
   ```

3. **Verificar timestamp del archivo:**
   ```bash
   # Linux
   stat plugins/Apocalipsis/waypoints.yml
   
   # Windows
   dir plugins\Apocalipsis\waypoints.yml
   ```
   
   El timestamp debe ser reciente (última vez que alguien creó/usó waypoint).

### **Problema: "Error al guardar"**

Si aparece en consola:
```
[Skills] ✗ Error crítico guardando waypoints: ...
```

**Posibles causas:**
1. Sin permisos de escritura en carpeta
2. Disco lleno
3. Archivo waypoints.yml corrupto (eliminarlo y dejar que se recree)

**Solución:**
```bash
# 1. Verificar permisos
chmod 755 plugins/Apocalipsis/

# 2. Verificar espacio en disco
df -h

# 3. Si el archivo está corrupto, hacer backup y eliminar
mv plugins/Apocalipsis/waypoints.yml plugins/Apocalipsis/waypoints.yml.backup
# El plugin creará uno nuevo automáticamente
```

---

## 🎯 Checklist de Verificación Post-Fix

- [ ] Compilar plugin: `mvn clean package -DskipTests`
- [ ] Copiar JAR al servidor
- [ ] Reiniciar servidor
- [ ] Crear waypoint de prueba: `/wp set test`
- [ ] Verificar log: "✓ Waypoints guardados exitosamente"
- [ ] Verificar archivo existe: `plugins/Apocalipsis/waypoints.yml`
- [ ] Desconectarse
- [ ] Verificar log: "Waypoints guardados por desconexión"
- [ ] Reiniciar servidor
- [ ] Verificar log al inicio: "Cargados X waypoints"
- [ ] Conectarse
- [ ] Verificar waypoint persiste: `/wp`
- [ ] Confirmar que waypoint "test" sigue ahí

---

## 📊 Cambios en el Código

### Archivo: `SkillEffectListener.java`

#### Método `saveWaypoints()`
- ✅ Verifica/crea directorio `plugin.getDataFolder()`
- ✅ Verifica/crea archivo `waypoints.yml`
- ✅ Log detallado con ruta absoluta
- ✅ Manejo de excepciones con stack trace

#### Método `loadWaypoints()`
- ✅ Log cuando persistencia desactivada
- ✅ Log con ruta absoluta del archivo
- ✅ Log cuando archivo no existe (normal en primera ejecución)
- ✅ Log cuando falta sección "waypoints"

#### Método `onPlayerQuit()`
- ✅ Cambiado de asíncrono a **síncrono**
- ✅ Log específico por jugador

---

## 🔄 Flujo de Guardado

```
1. Jugador crea waypoint: /wp set casa
   └─> setWaypoint()
       └─> playerWaypoints.put(uuid, waypoints)
       └─> saveWaypoints() [INMEDIATO]
           └─> Log: "✓ Waypoints guardados exitosamente"

2. Jugador se desconecta
   └─> onPlayerQuit()
       └─> saveWaypoints() [SÍNCRONO]
           └─> Log: "Waypoints guardados por desconexión"

3. Auto-guardado cada 5 minutos
   └─> Scheduler task
       └─> saveWaypoints()
           └─> Log: "Auto-guardado de waypoints completado"

4. Servidor se cierra
   └─> onDisable()
       └─> skillEffectListener.shutdown()
           └─> saveWaypoints() [FINAL]
```

---

## 🔄 Flujo de Carga

```
1. Servidor inicia
   └─> onEnable()
       └─> SkillEffectListener constructor
           └─> loadWaypoints()
               └─> Log: "Cargando waypoints desde..."
               └─> Log: "Cargados X waypoints de Y jugadores"

2. Jugador entra
   └─> playerWaypoints ya contiene sus datos
   └─> /wp muestra waypoints correctamente
```

---

## ⚡ Mejoras Adicionales Implementadas

1. **Auto-guardado cada 5 minutos** (ya existía, ahora con mejor logging)
2. **Guardado al desconectar** (ahora síncrono y confiable)
3. **Guardado al cerrar servidor** (en `shutdown()`)
4. **Metadata en archivo**: versión, timestamp, total de waypoints

---

## 💡 Recomendaciones

1. **Monitorear logs** después de actualizar para confirmar guardado
2. **Hacer backup** del archivo waypoints.yml periódicamente
3. **No editar manualmente** el archivo mientras el servidor esté corriendo
4. Si hay problemas, revisar:
   - Permisos de carpeta `plugins/Apocalipsis/`
   - Espacio en disco
   - Logs del servidor para stack traces

---

## 📝 Próximos Pasos

1. **Compila** el plugin
2. **Reinicia** el servidor  
3. **Crea** un waypoint de prueba
4. **Verifica** los logs
5. **Reinicia** el servidor de nuevo
6. **Confirma** que el waypoint persiste

Si después de estos pasos los waypoints SIGUEN desapareciendo, revisar:
- Logs completos del servidor (inicio y cierre)
- Contenido del archivo `waypoints.yml`
- Permisos del sistema de archivos

---

**Archivos modificados**: `SkillEffectListener.java`  
**Versión**: 1.22.57  
**Fecha**: 29 de enero de 2026
