# Sistema de Tracking y Limpieza de Bloques en Desastres

## 📋 Descripción General

Se ha implementado un sistema automático de tracking (seguimiento) y restauración de bloques modificados durante los desastres. Este sistema garantiza que todos los bloques que un desastre crea, modifica o destruye sean automáticamente restaurados a su estado original cuando el desastre termine.

## ✨ Características Principales

### 1. **Tracking Automático**
- Los desastres ahora rastrean cada bloque que modifican
- Se guarda el estado original (Material + BlockData) de cada bloque
- Límite de seguridad: 10,000 bloques trackeados por desastre

### 2. **Restauración Automática**
- Cuando un desastre termina, todos los bloques modificados se restauran automáticamente
- Los jugadores verán el mundo volver a su estado pre-desastre
- Logs informativos muestran cuántos bloques fueron restaurados

### 3. **Rendimiento Optimizado**
- Uso de `ConcurrentHashMap` para thread-safety
- Límite de bloques para prevenir problemas de memoria
- Restauración por lotes con manejo de errores

## 🔧 Implementación Técnica

### Clase Base: DisasterBase

Se agregaron los siguientes componentes:

```java
// Estructura de datos para tracking
protected Map<Location, BlockData> modifiedBlocks = new ConcurrentHashMap<>();
protected int maxTrackedBlocks = 10000; // Límite de seguridad
```

### Métodos Principales

#### 1. `trackBlock(Block block)`
- Registra un bloque antes de modificarlo
- Guarda su estado original (BlockData)
- Verifica límites de seguridad

#### 2. `setBlockTracked(Block block, Material newMaterial)`
- Modifica un bloque de forma segura
- Automáticamente trackea el estado original
- **Usar este método en lugar de `block.setType()`**

#### 3. `setBlockTracked(Block block, BlockData newData)`
- Versión alternativa para modificar con BlockData completo
- Automáticamente trackea el estado original

#### 4. `restoreModifiedBlocks()`
- Restaura todos los bloques trackeados
- Se ejecuta automáticamente en `onStop()`
- Maneja errores individualmente por bloque

#### 5. `clearBlockTracking()`
- Limpia el tracking sin restaurar (para casos especiales)

## 🎯 Desastres Actualizados

Se aplicó el sistema de tracking a los siguientes desastres:

### ✅ LluviaFuegoNew
- **Bloques trackeados:**
  - Fuego colocado por bolas de fuego
  - Fuego persistente en zonas
  - Bloques transformados (tierra → tierra estéril)
  - Agua evaporada
- **Restauración:** Todo el fuego y transformaciones se limpian al terminar

### ✅ TerremotoNew
- **Bloques trackeados:**
  - Grietas (AIR/LAVA)
  - Bloques rotos por temblores
  - Bloques de protección destruidos
  - Rocas caídas (FallingBlocks)
- **Restauración:** Grietas se rellenan, bloques destruidos se restauran

### ✅ TormentaGlacial
- **Bloques trackeados:**
  - Agua congelada → Hielo
  - Hielo derretido → Agua
- **Restauración:** Todo el hielo generado se derrite

### ✅ ErupcionVolcanica
- **Bloques trackeados:**
  - Grietas volcánicas (AIR)
  - Lava colocada
  - Fuego volcánico (FIRE/SOUL_FIRE)
  - Bloques destruidos por erupciones
- **Restauración:** Lava se limpia, grietas se rellenan, fuego se apaga

### ✅ HuracanNew
- **Bloques trackeados:**
  - Bloques destruidos por viento
  - Agua de inundaciones
- **Restauración:** Agua se evapora, bloques destruidos se restauran

### ⚠️ TormentaElectrica
- **No modifica bloques directamente**
- Solo usa efectos de partículas y rayos vanilla

## 📊 Logs y Debugging

### Logs Informativos (siempre activos)
```
[Disaster] Restaurando 245 bloques modificados...
[Disaster] Restauración completada: 243 exitosos, 2 fallidos
```

### Logs de Debug (cuando `debug_ciclo: true`)
```
[Disaster] Estadísticas de lluvia_fuego:
  Bloques modificados: 245
[Disaster] Límite de bloques trackeados alcanzado: 10000
```

## 🛠️ Cómo Usar en Nuevos Desastres

### Paso 1: Reemplazar modificaciones directas
❌ **Antes:**
```java
block.setType(Material.FIRE);
block.setType(Material.AIR);
```

✅ **Después:**
```java
setBlockTracked(block, Material.FIRE);
setBlockTracked(block, Material.AIR);
```

### Paso 2: Listo
El sistema automáticamente:
1. Trackea el bloque cuando lo modificas
2. Restaura el bloque cuando el desastre termina
3. Limpia la memoria después

## ⚙️ Configuración

### Límite de Bloques
Por defecto: `10,000 bloques por desastre`

Para cambiar el límite en un desastre específico:
```java
@Override
protected void onStart() {
    this.maxTrackedBlocks = 20000; // Duplicar límite
    // ... resto del código
}
```

### Desactivar Restauración (casos especiales)
Si por alguna razón NO quieres que se restauren los bloques:
```java
@Override
protected void onStop() {
    clearBlockTracking(); // Limpiar sin restaurar
    // ... resto del código
}
```

## 🎮 Experiencia del Jugador

### Antes del Sistema
- Los desastres dejaban bloques permanentes (lava, fuego, grietas)
- Los jugadores tenían que limpiar manualmente
- El mundo se deterioraba con cada desastre

### Después del Sistema
- ✅ El mundo se auto-limpia al terminar cada desastre
- ✅ No hay bloques permanentes indeseados
- ✅ Los jugadores pueden disfrutar los desastres sin preocuparse por el cleanup
- ✅ El mundo mantiene su apariencia original

## 📈 Rendimiento

### Memory Usage
- ~100 bytes por bloque trackeado
- Límite de 10,000 bloques = ~1MB máximo por desastre
- Limpieza automática al terminar el desastre

### CPU Usage
- Tracking: O(1) - HashMap lookup/insert
- Restauración: O(n) - donde n = bloques modificados
- Restauración en batch previene lag spikes

## 🔍 Troubleshooting

### "Límite de bloques trackeados alcanzado"
**Causa:** El desastre intentó modificar más de 10,000 bloques

**Solución:**
1. Aumentar `maxTrackedBlocks` en el desastre específico
2. Optimizar el desastre para modificar menos bloques
3. Dividir el desastre en fases más pequeñas

### "Error restaurando bloque"
**Causa:** El chunk fue descargado o el mundo cambió

**Solución:**
- El sistema maneja estos errores automáticamente
- Los bloques que fallan se reportan en logs de debug
- No afecta la restauración de otros bloques

## 🚀 Mejoras Futuras

### Posibles Extensiones
1. **Restauración Gradual:** En lugar de instantánea, restaurar bloques progresivamente
2. **Persistencia:** Guardar bloques modificados en archivo para restaurar después de reinicio
3. **Configuración por Tipo:** Permitir configurar qué tipos de bloques restaurar
4. **Integración con Protecciones:** Respetar regiones protegidas de otros plugins

## 📝 Notas Técnicas

### Thread Safety
- Uso de `ConcurrentHashMap` para acceso multi-threaded seguro
- Modificaciones de bloques en main thread de Bukkit

### Compatibilidad
- Compatible con Spigot/Paper 1.21+
- No requiere cambios en configuración
- Retrocompatible con desastres antiguos

### Mantenimiento
- Agregar `setBlockTracked()` a cualquier nuevo código que modifique bloques
- Revisar logs de restauración para detectar problemas
- Ajustar límites según necesidades del servidor

---

**Versión:** 1.22.63  
**Fecha:** 31 de Enero, 2026  
**Autor:** Sistema de Tracking de Bloques para Desastres
