# Sistema de Persistencia de Ciclos - Versión 2.0

## 📋 Descripción General

El **Sistema de Persistencia de Ciclos** es una mejora crítica que garantiza la **seguridad total** de los datos de todos los mundos de ciclo creados. Este sistema resuelve el problema de pérdida de datos durante actualizaciones del plugin.

## 🎯 Problemas Resueltos

### ❌ Antes (Sin Persistencia)
- Los ciclos se almacenaban solo en `ciclos.yml` (configuración básica)
- Al actualizar el plugin, los ciclos podían "perderse"
- No había información de cuándo fueron creados
- Sin estadísticas de uso
- Sin validación de integridad

### ✅ Ahora (Con Persistencia v2.0)
- **Doble respaldo**: `ciclos.yml` + `ciclos_data.yml`
- **Recuperación automática** después de actualizaciones
- **Estadísticas completas** de cada ciclo
- **Validación de integridad** automática al iniciar
- **Migración automática** desde sistema antiguo

## 📁 Arquitectura del Sistema

### Archivos Creados

1. **`ciclos_data.yml`** (NUEVO)
   - Ubicación: `plugins/Apocalipsis/ciclos_data.yml`
   - Propósito: Almacenamiento persistente de TODOS los datos de ciclos
   - Formato: YAML estructurado con metadata completa

2. **`ciclos.yml`** (Existente - Mantiene compatibilidad)
   - Ubicación: `plugins/Apocalipsis/ciclos.yml`
   - Propósito: Configuración del sistema
   - Mantiene compatibilidad con versiones anteriores

### Clases Java

1. **`CicloData.java`** (NUEVA)
   - Modelo de datos de un ciclo
   - 200+ líneas de getters/setters
   - Contiene TODA la información de un ciclo

2. **`CicloPersistenceManager.java`** (NUEVA)
   - Gestor de persistencia
   - Carga/guarda datos en YAML
   - Validación y reparación automática
   - 500+ líneas de lógica robusta

3. **`CicloManager.java`** (MODIFICADO)
   - Integrado con `CicloPersistenceManager`
   - Usa persistencia en todos los métodos críticos
   - Migración automática de datos antiguos

## 🔧 Funcionalidades Implementadas

### 1. Registro Automático de Ciclos

Cada vez que se crea un ciclo, se guarda:

```yaml
ciclos:
  ciclo_1:
    display_name: "Ciclo 1"
    activo: true
    existe: true
    environment: "NORMAL"
    world_type: "NORMAL"
    difficulty: "HARD"
    seed: 1234567890
    spawn:
      x: 0.0
      y: 64.0
      z: 0.0
    fecha_creacion: "2026-01-26 12:00:00"
    estadisticas:
      jugadores_unicos: 0
      tiempo_jugado_minutos: 0
```

### 2. Recuperación al Iniciar

Al iniciar el servidor:

```
[CicloManager] ═══════════════════════════════════════
[CicloPersistence] Cargando datos de ciclos...
[CicloPersistence]   ✓ Cargado: ciclo_1 (existe=true, activo=true)
[CicloPersistence]   ✓ Cargado: ciclo_2 (existe=true, activo=false)
[CicloPersistence] Ciclos cargados: 2
[CicloManager] Validando integridad de datos...
[CicloPersistence]   ✓ Todos los datos están íntegros
[CicloManager] ═══════════════════════════════════════
```

### 3. Validación de Integridad

Ejecuta automáticamente:
- Verifica que mundos físicos existan en disco
- Repara discrepancias entre datos y realidad
- Elimina "ciclos fantasma" (registrados pero sin archivos)
- Logs detallados de reparaciones

### 4. Migración Automática

Si detecta ciclos en el sistema antiguo:

```
[CicloManager] Migrando ciclo: old_world
[CicloPersistence] ✓ Ciclo registrado: old_world
```

### 5. Carga Automática de Mundos

Si un ciclo está marcado como "activo" pero no está cargado:

```java
if (data.isActivo() && Bukkit.getWorld(worldName) == null) {
    loadExistingWorld(worldName, data);
}
```

## 📊 Comandos Mejorados

### `/avo ciclo listar`

Antes:
```
CICLOS ACTIVOS
  ● ciclo_1 - 0 jugadores
```

Ahora:
```
CICLOS REGISTRADOS
  §a● ciclo_1 - 0 jugadores
    └─ Jugadores únicos: 15 | Creado: 26/01/2026
  §7○ ciclo_2 - 0 jugadores (sin cargar)
    └─ Jugadores únicos: 3 | Creado: 25/01/2026
  
Total: 2 (1 cargados, 1 sin cargar)
```

### `/avo ciclo info <mundo>`

Ahora muestra DOS secciones:

**Datos en vivo** (si está cargado):
```
Estado: ✓ Cargado en memoria
Jugadores online: 2
Dificultad: HARD
Seed: 1234567890
```

**Datos guardados** (persistencia):
```
» Datos Guardados:
Existe en disco: ✓ Sí
Jugadores únicos: 15
Tiempo total jugado: 3600 minutos
Fecha creación: 26/01/2026 12:00
Última activación: 26/01/2026 15:30
```

### `/avo ciclo validar` (NUEVO)

Valida y repara integridad:

```
⚙ Validando integridad de datos de ciclos...
[CicloPersistence] ═══════════════════════════════════
[CicloPersistence] Validando integridad de datos...
[CicloPersistence]   → Reparando estado de: ciclo_3
[CicloPersistence]   ✗ Eliminando ciclo fantasma: test_world
[CicloPersistence]   ✓ Reparados: 1
[CicloPersistence]   ✓ Eliminados: 1
[CicloPersistence] ═══════════════════════════════════
✓ Validación completada.
```

### `/avo ciclo reporte` (NUEVO)

Genera reporte completo:

```
═══════════════════════════════════════
REPORTE DE CICLOS
═══════════════════════════════════════
Total de ciclos: 3

• ciclo_1
  - Activo: SÍ
  - Existe: SÍ
  - Ambiente: NORMAL
  - Dificultad: HARD
  - Jugadores: 15
  - Creado: 2026-01-26 12:00:00

• ciclo_2
  - Activo: NO
  - Existe: SÍ
  - Ambiente: NORMAL
  - Dificultad: NORMAL
  - Jugadores: 3
  - Creado: 2026-01-25 10:30:00

═══════════════════════════════════════
```

## 🔄 Flujo de Datos

### Crear Nuevo Ciclo

```
Usuario: /avo ciclo crear ciclo_1 NORMAL HARD
    ↓
CicloManager.createCycleWorld()
    ↓
World creado con Bukkit
    ↓
persistenceManager.registerCiclo(worldName, world)
    ↓
Datos guardados en ciclos_data.yml
    ↓
✓ Ciclo creado y respaldado
```

### Actualizar Plugin

```
Servidor apaga
    ↓
Usuario actualiza plugin JAR
    ↓
Servidor reinicia
    ↓
CicloManager.loadCycleWorlds()
    ↓
persistenceManager.loadData()
    ↓
Todos los ciclos cargados desde ciclos_data.yml
    ↓
Mundos activos se cargan automáticamente
    ↓
persistenceManager.validateAndRepair()
    ↓
✓ Sistema completamente recuperado
```

### Validación al Iniciar

```
Servidor inicia
    ↓
CicloPersistenceManager carga datos
    ↓
Para cada ciclo:
  - Verifica si carpeta existe
  - Compara con datos guardados
  - Repara discrepancias
  - Elimina ciclos fantasma
    ↓
Logs de reparación
    ↓
✓ Integridad garantizada
```

## 📈 Beneficios

### Para Administradores

1. **Cero pérdida de datos** durante actualizaciones
2. **Logs detallados** de todos los ciclos
3. **Estadísticas** de uso de cada ciclo
4. **Recuperación automática** sin intervención manual
5. **Validación** de integridad con un comando

### Para Jugadores

1. **Transparente**: No notan cambios
2. **Confiable**: Sus ciclos nunca se pierden
3. **Estadísticas**: Pueden ver cuánto tiempo han jugado

### Para Desarrollo

1. **Código limpio** con separación de responsabilidades
2. **Fácil extensión** para nuevas funcionalidades
3. **Logs detallados** facilitan debugging
4. **Tests automáticos** de integridad

## 🛡️ Seguridad

### Respaldo Doble

- **ciclos.yml**: Configuración (compatible con v1.0)
- **ciclos_data.yml**: Datos completos (v2.0)

### Validación Automática

```java
public void validateAndRepair() {
    // Verifica existencia física
    File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
    boolean existeFisicamente = worldFolder.exists();
    
    // Repara discrepancias
    if (data.existe() != existeFisicamente) {
        data.setExiste(existeFisicamente);
        reparados++;
    }
    
    // Elimina ciclos fantasma
    if (!existeFisicamente && data.getJugadoresUnicos() == 0) {
        ciclosData.remove(worldName);
        eliminados++;
    }
}
```

### Logs Completos

Cada operación deja trace:

```
[CicloPersistence] ✓ Ciclo registrado: ciclo_1
[CicloPersistence] ✓ Datos guardados: 3 ciclos
[CicloPersistence] ✓ Cargado: ciclo_1 (existe=true, activo=true)
```

## 🚀 Uso

### Crear Ciclo

```bash
/avo ciclo crear ciclo_1 NORMAL HARD
```

El sistema automáticamente:
1. Crea el mundo
2. Registra en persistencia
3. Guarda todos los datos
4. Genera carpeta física

### Listar Ciclos

```bash
/avo ciclo listar
```

Muestra TODOS los ciclos (cargados y no cargados)

### Ver Información

```bash
/avo ciclo info ciclo_1
```

Muestra datos en vivo + datos guardados

### Validar Integridad

```bash
/avo ciclo validar
```

Repara automáticamente cualquier problema

### Generar Reporte

```bash
/avo ciclo reporte
```

Genera reporte completo de todos los ciclos

## 🔧 Configuración

### Ubicación de Archivos

```
servidor/
├── plugins/
│   └── Apocalipsis/
│       ├── ciclos.yml           # Configuración (v1.0 + v2.0)
│       └── ciclos_data.yml      # Datos persistentes (v2.0) ⭐ NUEVO
└── ciclo_1/                      # Mundos físicos
    └── ciclo_2/
```

### Formato de ciclos_data.yml

```yaml
metadata:
  version: "2.0"
  ultima_actualizacion: "2026-01-26 22:00:00"
  total_ciclos: 2

ciclos:
  ciclo_1:
    display_name: "Ciclo 1"
    activo: true
    existe: true
    environment: "NORMAL"
    world_type: "NORMAL"
    difficulty: "HARD"
    seed: 1234567890
    spawn:
      x: 0.0
      y: 64.0
      z: 0.0
    fecha_creacion: "2026-01-26 12:00:00"
    ultima_activacion: "2026-01-26 15:30:00"
    estadisticas:
      jugadores_unicos: 15
      tiempo_jugado_minutos: 3600
```

## ✅ Testing

### Test 1: Crear y Recuperar

1. Crear ciclo: `/avo ciclo crear test_ciclo NORMAL HARD`
2. Verificar en `ciclos_data.yml`
3. Reiniciar servidor
4. Verificar logs de carga
5. `/avo ciclo listar` - debe aparecer el ciclo

### Test 2: Migración Automática

1. Tener ciclos en sistema antiguo (solo en `ciclos.yml`)
2. Actualizar plugin con nueva versión
3. Reiniciar servidor
4. Verificar logs: `[CicloManager] Migrando ciclo: ...`
5. Verificar `ciclos_data.yml` contiene datos migrados

### Test 3: Validación de Integridad

1. Eliminar carpeta física de un ciclo manualmente
2. Ejecutar: `/avo ciclo validar`
3. Verificar logs muestran reparación
4. `/avo ciclo info` debe mostrar `existe: NO`

## 📝 Changelog

### v2.0 - Sistema de Persistencia Completo

**NUEVO:**
- `CicloData.java` - Modelo de datos
- `CicloPersistenceManager.java` - Gestor de persistencia
- `ciclos_data.yml` - Almacenamiento persistente
- Comandos: `/avo ciclo validar`, `/avo ciclo reporte`
- Migración automática desde v1.0
- Validación de integridad automática
- Carga automática de mundos activos
- Estadísticas de uso por ciclo

**MODIFICADO:**
- `CicloManager.java` - Integración con persistencia
- `/avo ciclo listar` - Muestra más información
- `/avo ciclo info` - Doble vista (vivo + guardado)

**MEJORADO:**
- Logs más detallados
- Recuperación post-actualización
- Seguridad de datos

## 🎓 Conclusión

El **Sistema de Persistencia de Ciclos v2.0** garantiza que:

✅ **NUNCA** se pierden datos de ciclos  
✅ **SIEMPRE** se pueden recuperar después de actualizaciones  
✅ **TODO** está respaldado en `ciclos_data.yml`  
✅ **VALIDACIÓN** automática de integridad  
✅ **MIGRACIÓN** automática desde sistema antiguo  

**Es 100% seguro actualizar el plugin.**
