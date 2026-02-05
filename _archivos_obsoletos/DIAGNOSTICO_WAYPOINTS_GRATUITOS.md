# 🔍 Diagnóstico: Waypoints Gratuitos No se Guardan

## 📋 Resumen del Problema Reportado

**Síntoma**: Los waypoints comprados con la habilidad se guardan correctamente al reiniciar el servidor, pero el waypoint gratuito (sin habilidad) NO se guarda.

## 🔎 Análisis del Código

He revisado completamente el código y encontré que **TÉCNICAMENTE no debería haber diferencia** en cómo se guardan los waypoints:

### Flujo de Guardado (IGUAL para todos los waypoints)

```java
// PASO 1: Crear waypoint (con o sin habilidad)
setWaypoint(player, "nombre")  // O setWaypoint(player) para waypoint gratuito
  └─> playerWaypoints.put(uuid, waypoints)
  └─> saveWaypoints()  // ✅ GUARDADO INMEDIATO

// PASO 2: El saveWaypoints() guarda TODOS los waypoints del jugador
for (Map.Entry<UUID, Map<String, Location>> playerEntry : playerWaypoints.entrySet()) {
    // Guarda TODOS los waypoints del mapa, sin distinción
}
```

**Conclusión**: El código NO diferencia entre waypoints gratuitos y comprados al guardar.

## 🐛 Posibles Causas del Problema

### 1️⃣ **Nombre del Waypoint Gratuito**

El waypoint gratuito usa el nombre `"default"`:

```java
public void setWaypoint(Player player) {
    setWaypoint(player, "default");  // <- Waypoint gratuito
}
```

**Pregunta**: ¿Estás usando el comando correcto para crear el waypoint gratuito?

```bash
# ❌ INCORRECTO (esto no funciona):
/waypoint

# ✅ CORRECTO (crear waypoint gratuito):
/waypoint set default
/wp set default

# ✅ ALTERNATIVA (waypoint con nombre personalizado):
/wp set casa
```

### 2️⃣ **El Archivo waypoints.yml No Se Crea**

Si la carpeta `plugins/Apocalipsis/` no existe o no tiene permisos de escritura, el archivo no se creará.

**Verificación**:
```bash
# 1. Verificar que existe la carpeta
ls -la plugins/Apocalipsis/

# 2. Verificar permisos
# La carpeta debe tener permisos de escritura

# 3. Verificar el archivo waypoints.yml
cat plugins/Apocalipsis/waypoints.yml
```

### 3️⃣ **La Persistencia Está Desactivada**

En el archivo `skills.yml` debe estar configurado:

```yaml
waypoints:
  persistencia: true  # ← DEBE SER true
```

**Verificación**:
```bash
# Ver configuración actual
cat plugins/Apocalipsis/skills.yml | grep -A 2 "waypoints:"
```

### 4️⃣ **El Waypoint Se Guarda Pero No Se Carga**

Es posible que el waypoint se guarde correctamente, pero haya un problema al cargarlo.

**Verificación en logs**:
```
[Skills] Cargando waypoints desde: ...
[Skills] Cargados X waypoints de Y jugadores
```

Si dice "Cargados 0 waypoints", entonces el problema es en la CARGA, no en el GUARDADO.

## 🧪 Prueba de Diagnóstico

Sigue estos pasos para identificar exactamente dónde está el problema:

### Paso 1: Crear Waypoint Gratuito
```bash
# Conectarse al servidor
/wp set default
```

**Verificar en consola**:
```
[Skills] ✓ Waypoints guardados exitosamente: 1 waypoints de 1 jugadores
```

### Paso 2: Verificar que el Archivo Existe
```bash
# Buscar el archivo
find plugins/ -name "waypoints.yml"

# Ver contenido
cat plugins/Apocalipsis/waypoints.yml
```

**Contenido esperado**:
```yaml
metadata:
  version: '2.0'
  last_save: 1738278000000
  total_waypoints: 1
waypoints:
  TU-UUID-AQUI:
    default:  # ← Debe aparecer "default"
      world: world
      x: 100.0
      y: 64.0
      z: 200.0
      yaw: 0.0
      pitch: 0.0
```

### Paso 3: Desconectarse
```bash
# Salir del servidor
/quit
```

**Verificar en consola**:
```
[Skills] Waypoints guardados por desconexión de TuNombre
```

### Paso 4: Reiniciar Servidor
```bash
# Detener servidor
stop

# Iniciar servidor de nuevo
```

**Verificar en consola al iniciar**:
```
[Skills] Cargando waypoints desde: /ruta/completa/waypoints.yml
[Skills] Cargados 1 waypoints de 1 jugadores
```

### Paso 5: Conectarse y Verificar
```bash
# Conectarse
# Listar waypoints
/wp list
```

**Resultado esperado**:
```
§e§l⚑ Tus Waypoints (1/1)§e:
  §a✓ §fdefault §7→ §f100§7, §f64§7, §f200
```

## 🔧 Soluciones Según el Diagnóstico

### Si el archivo waypoints.yml NO se crea:

1. **Verificar permisos de escritura**:
```bash
chmod 755 plugins/Apocalipsis/
```

2. **Crear manualmente el directorio**:
```bash
mkdir -p plugins/Apocalipsis/
```

3. **Verificar espacio en disco**:
```bash
df -h
```

### Si el archivo se crea pero está vacío:

1. **Verificar configuración de persistencia**:
```yaml
# En plugins/Apocalipsis/skills.yml
waypoints:
  persistencia: true
```

2. **Recargar configuración**:
```bash
/apocalipsis reload
```

### Si el archivo tiene contenido pero no se carga:

1. **Verificar formato YAML** (indentación correcta)
2. **Buscar errores en logs**:
```bash
tail -f logs/latest.log | grep -i waypoint
```

3. **Verificar que el mundo existe** al cargar el waypoint

## 📊 Datos Técnicos

### Estructura del Código

```java
// Waypoint Gratuito (sin habilidad):
- Límite: 1 waypoint
- Nombre por defecto: "default"
- Guardado: INMEDIATO en setWaypoint()
- Carga: Al iniciar servidor en loadWaypoints()

// Waypoint Comprado (con habilidad):
- Límite: 3 waypoints
- Nombre: personalizado
- Guardado: INMEDIATO en setWaypoint()
- Carga: Al iniciar servidor en loadWaypoints()
```

**Ambos usan la misma función de guardado**: `saveWaypoints()`

### Eventos de Guardado

1. **Inmediato** al crear/actualizar waypoint
2. **Al desconectar** (síncrono)
3. **Auto-guardado** cada 5 minutos
4. **Al cerrar servidor** (en shutdown())

## 🎯 Comando de Verificación Rápida

Ejecuta estos comandos en consola del servidor:

```bash
# Ver si la persistencia está activa
grep "persistencia" plugins/Apocalipsis/skills.yml

# Ver si existe el archivo
ls -la plugins/Apocalipsis/waypoints.yml

# Ver contenido del archivo
cat plugins/Apocalipsis/waypoints.yml

# Ver últimos logs relacionados con waypoints
tail -100 logs/latest.log | grep -i waypoint
```

## 📝 Información Importante

### El waypoint gratuito NO se crea automáticamente

Debes crearlo explícitamente con:
```bash
/wp set default
# O
/wp set cualquier_nombre
```

### El comando `/waypoint` sin argumentos NO crea el waypoint

Solo muestra información de uso. Para crear un waypoint:
```bash
/wp set <nombre>
```

## ✅ Checklist de Verificación

- [ ] El archivo `waypoints.yml` existe en `plugins/Apocalipsis/`
- [ ] La configuración `persistencia: true` está en `skills.yml`
- [ ] Los logs muestran "✓ Waypoints guardados exitosamente"
- [ ] Los logs muestran "Cargados X waypoints" al iniciar
- [ ] El archivo `waypoints.yml` contiene tu UUID
- [ ] El archivo `waypoints.yml` tiene el waypoint "default"
- [ ] El servidor tiene permisos de escritura en la carpeta
- [ ] No hay errores en los logs relacionados con waypoints

## 🚨 Si Sigue Sin Funcionar

Si después de todas estas verificaciones el waypoint gratuito SIGUE sin guardarse:

1. **Compartir logs**: Los logs del servidor al:
   - Crear el waypoint
   - Desconectarse
   - Reiniciar servidor
   - Conectarse de nuevo

2. **Compartir archivo**: El contenido de `waypoints.yml`

3. **Verificar versión**: Confirmar que estás usando la versión v1.22.57 o superior (que incluye el fix de persistencia)

---

**Versión del documento**: 1.0  
**Fecha**: 30 de enero de 2026  
**Relacionado con**: FIX_WAYPOINTS_PERSISTENCIA_v1.22.57.md
