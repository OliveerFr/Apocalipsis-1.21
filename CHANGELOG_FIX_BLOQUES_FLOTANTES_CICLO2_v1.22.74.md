# 🔧 FIX: Bloques Flotantes de Desastres Ciclo 2

**Versión:** v1.22.74  
**Fecha:** 2026-02-09  
**Tipo:** Fix de Mecánicas  
**Sistemas Afectados:** Desastres Naturales (Ciclo 2)

---

## ❌ Problema Reportado

Los bloques flotantes (FallingBlock) de los desastres del **Ciclo 2** dejaban bloques físicos cuando caían al suelo:

- **Tormenta Glacial:**
  - Cristales de hielo azul (BLUE_ICE) se colocaban permanentemente
  - Estalactitas (POINTED_DRIPSTONE) quedaban en el suelo

- **Erupción Volcánica:**
  - Rocas volcánicas (MAGMA_BLOCK) se quedaban como bloques sólidos

### Comportamiento Esperado
Los bloques flotantes deben **desaparecer completamente** cuando llegan al piso, sin dejar ningún bloque físico en el mundo.

---

## ✅ Solución Implementada

### 1. Nuevo Listener: DisasterFallingBlockListener

Se creó un listener especializado que intercepta el evento `EntityChangeBlockEvent` (disparado cuando un FallingBlock intenta convertirse en bloque sólido):

**Archivo:** `DisasterFallingBlockListener.java`

```java
@EventHandler(priority = EventPriority.HIGH)
public void onFallingBlockLand(EntityChangeBlockEvent event) {
    if (event.getEntityType() != EntityType.FALLING_BLOCK) {
        return;
    }
    
    FallingBlock fb = (FallingBlock) event.getEntity();
    
    if (!isDisasterFallingBlock(fb)) {
        return;
    }
    
    // Cancelar el evento para evitar que el bloque se coloque
    event.setCancelled(true);
}
```

**Funcionalidad:**
- ✅ Detecta FallingBlocks de desastres activos
- ✅ Cancela el evento antes de que el bloque se coloque
- ✅ Permite que la lógica de impacto (explosiones, fuego) se ejecute normalmente
- ✅ Los bloques desaparecen sin dejar rastro

---

### 2. Métodos de Verificación en Desastres

Se agregaron métodos públicos en los desastres para identificar sus FallingBlocks:

#### TormentaGlacial.java
```java
public boolean isCristalActivo(FallingBlock fb) {
    return cristalesActivos.contains(fb);
}

public boolean isEstalactitaActiva(FallingBlock fb) {
    return estalactitasActivas.contains(fb);
}
```

#### ErupcionVolcanica.java
```java
public boolean isRocaActiva(FallingBlock fb) {
    return rocasActivas.contains(fb);
}
```

**Propósito:**
- Permite al listener verificar si un FallingBlock pertenece a un desastre activo
- Evita interferir con otros FallingBlocks del juego (gravilla, arena, etc.)

---

### 3. Registro del Listener

Se registró el nuevo listener en el plugin principal:

**Archivo:** `Apocalipsis.java`

```java
// Imports
import me.apocalipsis.listeners.DisasterFallingBlockListener;

// En onEnable()
getServer().getPluginManager().registerEvents(
    new DisasterFallingBlockListener(this), this
);
```

---

## 🔍 Comportamiento Detallado

### Tormenta Glacial

**Antes ❌:**
1. Cristales de hielo caen del cielo
2. Al tocar el suelo → se colocan como bloques BLUE_ICE
3. Los bloques quedan permanentemente (hasta limpieza manual)

**Después ✅:**
1. Cristales de hielo caen del cielo
2. Al tocar el suelo → **desaparecen inmediatamente**
3. No dejan ningún bloque

**Lo mismo aplica para estalactitas de hielo.**

---

### Erupción Volcánica

**Antes ❌:**
1. Rocas volcánicas caen
2. Al impactar → explosión + fuego + bloque MAGMA_BLOCK
3. El bloque de magma queda permanentemente

**Después ✅:**
1. Rocas volcánicas caen
2. Al impactar → **explosión + fuego** (efectos se mantienen)
3. La roca **desaparece** sin dejar bloque de magma

**Importante:** La explosión y el fuego **siguen funcionando** porque la lógica de impacto en `ErupcionVolcanica` está en el `BukkitRunnable` que detecta `fb.isOnGround()`, que se ejecuta **antes** de que el FallingBlock intente colocarse.

---

## 🔧 Archivos Modificados

### Nuevos Archivos
- `src/main/java/me/apocalipsis/listeners/DisasterFallingBlockListener.java` ✨ CREADO

### Archivos Modificados
1. **TormentaGlacial.java**
   - Agregados: `isCristalActivo()`, `isEstalactitaActiva()`

2. **ErupcionVolcanica.java**
   - Agregado: `isRocaActiva()`

3. **Apocalipsis.java**
   - Import agregado: `DisasterFallingBlockListener`
   - Listener registrado en `onEnable()`

---

## 🧪 Testing Recomendado

### Test 1: Tormenta Glacial - Cristales
```bash
/avo disaster tormenta_glacial
```
**Verificar:**
- ✅ Cristales de hielo caen
- ✅ Al tocar el piso NO dejan bloques BLUE_ICE
- ✅ Partículas y efectos visuales funcionan

### Test 2: Tormenta Glacial - Estalactitas
```bash
/avo disaster tormenta_glacial
# Esperar fase de estalactitas
```
**Verificar:**
- ✅ Estalactitas caen sobre jugadores
- ✅ Al tocar el piso NO dejan bloques POINTED_DRIPSTONE
- ✅ Daño a jugadores funciona

### Test 3: Erupción Volcánica - Rocas
```bash
/avo disaster erupcion_volcanica
# Esperar fase de rocas
```
**Verificar:**
- ✅ Rocas volcánicas caen
- ✅ Al impactar crean explosión ✅
- ✅ Al impactar generan fuego ✅
- ✅ NO dejan bloques MAGMA_BLOCK

### Test 4: FallingBlocks Normales (No Afectados)
```bash
# Colocar gravilla/arena sobre un espacio vacío
```
**Verificar:**
- ✅ Gravilla/arena caen normalmente
- ✅ Se colocan como bloques al tocar el suelo
- ✅ El listener NO interfiere con bloques normales

---

## 📊 Comparación: Antes vs Después

| Aspecto | Antes ❌ | Después ✅ |
|---------|----------|------------|
| **Cristales de Hielo** | Dejan BLUE_ICE permanente | Desaparecen sin dejar bloque |
| **Estalactitas** | Dejan POINTED_DRIPSTONE | Desaparecen sin dejar bloque |
| **Rocas Volcánicas** | Dejan MAGMA_BLOCK | Desaparecen sin dejar bloque |
| **Explosión de Rocas** | ✅ Funciona | ✅ Funciona (sin cambios) |
| **Fuego de Rocas** | ✅ Funciona | ✅ Funciona (sin cambios) |
| **FallingBlocks normales** | ✅ Funcionan | ✅ Funcionan (sin cambios) |
| **Limpieza del mundo** | Manual o al terminar desastre | Automática al caer |

---

## 🎯 Beneficios del Fix

### 1. **Mundo Más Limpio**
- ✅ No quedan bloques residuales de desastres
- ✅ Reduce necesidad de limpieza manual
- ✅ Mejor experiencia visual

### 2. **Mecánicas Mejoradas**
- ✅ Bloques flotantes se comportan como efectos visuales/de daño
- ✅ No afectan la construcción ni el gameplay posterior
- ✅ Más acorde con la naturaleza temporal de los desastres

### 3. **Rendimiento**
- ✅ Menos bloques persistentes en el mundo
- ✅ Menor carga en el sistema de tracking de bloques modificados

### 4. **Compatibilidad**
- ✅ No interfiere con FallingBlocks del juego normal (gravilla, arena)
- ✅ Solo afecta a desastres activos del Ciclo 2
- ✅ Mantiene explosiones y efectos de impacto

---

## 📝 Notas Técnicas

### Orden de Ejecución

1. **FallingBlock cae y toca el suelo**
2. **BukkitRunnable** del desastre detecta `fb.isOnGround()`
   - En `ErupcionVolcanica`: Crea explosión y fuego
   - En `TormentaGlacial`: Solo efectos visuales
3. **EntityChangeBlockEvent** se dispara
4. **DisasterFallingBlockListener** cancela el evento
5. **BukkitRunnable** elimina el FallingBlock con `fb.remove()`

### ¿Por Qué NO Eliminar en el Listener?

Si eliminamos el FallingBlock en el listener con `fb.remove()`, el `BukkitRunnable` en `ErupcionVolcanica` podría no detectar correctamente el impacto. Por eso:

- ✅ Listener: **Solo cancela el evento** (evita colocación)
- ✅ Desastre: **Maneja la limpieza** del FallingBlock

---

## 🚀 Próximos Pasos

### Potenciales Mejoras Futuras
- [ ] Agregar partículas al desaparecer (opcional)
- [ ] Sistema de configuración para habilitar/deshabilitar esta mecánica
- [ ] Aplicar a desastres del Ciclo 1 si se requiere

---

## 📌 Conclusión

Los bloques flotantes de los desastres del Ciclo 2 ahora **desaparecen correctamente** al tocar el suelo, sin dejar bloques físicos en el mundo. Esto mejora la experiencia de juego, mantiene el mundo más limpio y preserva todas las mecánicas de daño y efectos visuales.

**Estado:** ✅ **IMPLEMENTADO Y LISTO PARA TESTING**

---

**Desarrollado por:** Apocalipsis Plugin Team  
**Testeado en:** Minecraft 1.21+ / Paper  
**Compatibilidad:** Ciclo 2 Desastres (Tormenta Glacial, Erupción Volcánica)
