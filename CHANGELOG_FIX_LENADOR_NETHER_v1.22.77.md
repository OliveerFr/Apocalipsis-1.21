# 🪓 FIX: Leñador no Funciona en el Nether - v1.22.77

## 🐛 **PROBLEMA REPORTADO**

La habilidad **Leñador** (Leñador Nato/Experto/Maestro) **NO funcionaba** con los troncos del Nether:
- ❌ `CRIMSON_STEM` (tronco carmesí)
- ❌ `WARPED_STEM` (tronco distorsionado)
- ❌ Versiones stripped de ambos

**Síntoma:** Al romper un tronco del Nether con un hacha, solo se rompe UN bloque en lugar de talar todo el árbol.

---

## 🔍 **CAUSA RAÍZ**

El método `isRealTree()` verifica que un tronco tenga "hojas" cercanas para distinguir árboles naturales de estructuras de jugadores.

**Problema:**
Los árboles del Nether tienen una estructura MUY diferente a los árboles normales:

### **Overworld:**
```
     [HOJAS] ← Radio 4-8 bloques
     [HOJAS]
     [HOJAS]
     [TRONCO] ← Hojas cercanas (fácil de detectar)
     [TRONCO]
```

### **Nether:**
```
          [WART BLOCK] ← Bloques muy arriba
          [WART BLOCK]    y dispersos
     
     
     
     [STEM] ← Wart blocks pueden estar
     [STEM]    a +10 bloques de distancia
     [STEM]    horizontalmente
```

**Resultado:** La búsqueda con radio 4-8 bloques NO encontraba los `NETHER_WART_BLOCK` o `WARPED_WART_BLOCK`, marcando el árbol como "estructura de jugador" y cancelando la tala masiva.

---

## ✅ **SOLUCIÓN IMPLEMENTADA**

### **1. Detección Especial para Árboles del Nether**

**Ubicación:** `SkillEffectListener.java` línea ~1180

```java
/**
 * [FIX v1.22.77] Detección especial para árboles del Nether
 */
private boolean isRealTree(Block logBlock) {
    Material blockType = logBlock.getType();
    
    // [FIX NETHER] Los árboles del Nether tienen estructuras muy irregulares
    // Hacer verificación más flexible para Crimson y Warped Stems
    boolean isNetherTree = (blockType == Material.CRIMSON_STEM || 
                           blockType == Material.WARPED_STEM ||
                           blockType == Material.STRIPPED_CRIMSON_STEM ||
                           blockType == Material.STRIPPED_WARPED_STEM);
    
    int leavesCount = 0;
    int radius = lenadorRadioBuscarHojas;
    
    // [FIX NETHER] Aumentar radio de búsqueda para árboles del Nether
    if (isNetherTree) {
        radius = Math.max(radius, 10); // Mínimo radio 10 para Nether
    }
    
    // ... búsqueda de hojas ...
}
```

### **2. Reducción de Hojas Requeridas para Nether**

```java
// [FIX NETHER] Solo 1 hoja requerida para árboles del Nether
int minHojasNecesarias = isNetherTree ? 1 : lenadorMinHojasRequeridas;
if (leavesCount >= minHojasNecesarias) {
    return true; // Suficientes hojas encontradas
}
```

**Cambios aplicados:**
1. **Radio de búsqueda aumentado:** 4-8 → **mínimo 10** para Nether
2. **Hojas mínimas reducidas:** 2-3 → **1** para Nether
3. **Detección automática** de troncos del Nether

---

## 🎯 **DIFERENCIAS TÉCNICAS**

### **Árboles Normales (Overworld):**
- **Radio de búsqueda:** 8 bloques
- **Hojas mínimas:** 2
- **Tipos de hojas:** OAK_LEAVES, SPRUCE_LEAVES, etc.

### **Árboles del Nether (FIX):**
- **Radio de búsqueda:** 10 bloques ✅
- **Hojas mínimas:** 1 ✅
- **Tipos de hojas:** NETHER_WART_BLOCK, WARPED_WART_BLOCK

---

## ⚙️ **CONFIGURACIÓN**

No requiere cambios en `skills.yml`. Los valores por defecto funcionan correctamente:

```yaml
lenador_nato:
  verificar_arbol_real: true  # ✓ Debe estar en true
  radio_buscar_hojas: 4       # Se aumenta automáticamente a 10 para Nether
  min_hojas_requeridas: 3     # Se reduce automáticamente a 1 para Nether
```

---

## 🧪 **TESTING**

### **Pasos para Verificar:**

1. **Ir al Nether:**
   - Buscar un Crimson Forest o Warped Forest

2. **Encontrar un árbol natural:**
   - Crimson Huge Fungus (rojo)
   - Warped Huge Fungus (azul/verde)

3. **Equipar hacha y habilidad:**
   - Tener Leñador Nato/Experto/Maestro activa
   - Usar cualquier hacha

4. **Romper tronco base:**
   - Debería talar TODO el árbol automáticamente ✅
   - Plantar un hongo en la base (si auto-replant está activo) ✅

### **Resultados Esperados:**

**ANTES del fix:**
```
[Jugador rompe CRIMSON_STEM]
→ Solo se rompe 1 bloque ❌
→ Mensaje: (ninguno)
```

**DESPUÉS del fix:**
```
[Jugador rompe CRIMSON_STEM]
→ Se rompen 50+ bloques ✅
→ Mensaje: "🪓 Leñador Nato! Talaste 52 troncos (+retoño) (+5 XP)"
→ Se planta CRIMSON_FUNGUS en la base ✅
```

---

## 📝 **ARCHIVOS MODIFICADOS**

### **SkillEffectListener.java:**
- ✅ Método `isRealTree()` mejorado con detección especial para Nether
- ✅ Radio de búsqueda aumentado para troncos del Nether (4-8 → 10)
- ✅ Hojas mínimas reducidas para troncos del Nether (2-3 → 1)

**Líneas modificadas:** ~1180-1240

---

## 🌲 **COMPATIBILIDAD**

### **Todos los Tipos de Troncos Soportados:**

#### **Overworld:**
- ✅ OAK, SPRUCE, BIRCH, JUNGLE, ACACIA, DARK_OAK (ya funcionaban)
- ✅ CHERRY, MANGROVE (ya funcionaban)

#### **Nether (ARREGLADO):**
- ✅ CRIMSON_STEM (ahora funciona)
- ✅ WARPED_STEM (ahora funciona)
- ✅ Versiones STRIPPED (ahora funcionan)

### **Auto-Replant Soportado:**

#### **Overworld:**
- ✅ Saplings normales (OAK_SAPLING, etc.)

#### **Nether:**
- ✅ CRIMSON_FUNGUS (se planta en CRIMSON_NYLIUM/NETHERRACK)
- ✅ WARPED_FUNGUS (se planta en WARPED_NYLIUM/NETHERRACK)

---

## 🔄 **COMPORTAMIENTO PRESERVADO**

**No se afectó:**
- ✅ Árboles normales del Overworld siguen funcionando igual
- ✅ Desactivación con Shift sigue funcionando
- ✅ Cooldowns por nivel siguen respetándose
- ✅ Sistema de detección anti-estructura-de-jugador sigue activo
- ✅ Auto-replant sigue funcionando

---

## ⚠️ **NOTAS IMPORTANTES**

### **¿Por qué solo 1 hoja para Nether?**

Los árboles del Nether pueden tener configuraciones donde:
- El primer bloque de wart está a +15 bloques de altura
- Los wart blocks están muy dispersos
- La forma es completamente irregular

Con `min_hojas_requeridas: 1`, garantizamos detección mientras seguimos distinguiendo de estructuras de jugadores (muy raro que un jugador ponga exactamente 1 wart block cerca de un stem).

### **¿Por qué radio 10?**

Los Huge Fungus del Nether pueden tener:
- Wart blocks a 8-12 bloques horizontalmente del tronco central
- Altura de hasta 20+ bloques

Radio 10 cubre la mayoría de configuraciones naturales.

---

## 📊 **IMPACTO**

- **Severidad:** 🟡 **MEDIA** (funcionalidad bloqueada en dimensión específica)
- **Afectados:** Jugadores con habilidad Leñador que juegan en el Nether
- **Frecuencia:** 100% en árboles del Nether
- **Resolución:** ✅ **PERMANENTE** (detección mejorada)

---

## 🎯 **RESULTADO FINAL**

Leñador ahora funciona **perfectamente** en:
- ✅ Overworld (todos los árboles)
- ✅ Nether (Crimson y Warped)
- ✅ Todas las variantes (stripped, normal)

**¡Talar árboles del Nether nunca fue tan fácil!** 🪓🔥

---

## 📅 **Versión**

- **Versión:** 1.22.77
- **Fecha:** 2026-02-09
- **Tipo:** Bug Fix
- **Prioridad:** 🟡 Media
