# 🔧 CHANGELOG v1.22.75 - Fix: Anticlicker Bloqueando Trades

**Fecha:** 9 de febrero de 2026  
**Versión:** 1.22.75  
**Tipo:** Bugfix Crítico

---

## 🐛 PROBLEMA DETECTADO

El sistema de seguridad anti-autoclick (v1.22.73) estaba **bloqueando incorrectamente la obtención de XP al comerciar con aldeanos**.

### Causa Raíz
- Los trades con aldeanos se estaban procesando a través del verificador anticlicker
- El sistema detectaba clics rápidos legítimos (especialmente shift+click) como "autoclick"
- Esto causaba que los jugadores no pudieran obtener XP al tradear normalmente

### Impacto
- ❌ **XP bloqueada** al comerciar con aldeanos
- ❌ **Falsos positivos** en detección de autoclick durante trades
- ❌ **Experiencia de juego negativa** en comercio legítimo

---

## ✅ SOLUCIÓN IMPLEMENTADA

### Exención de Trades del Anticlicker

Los trades con aldeanos ahora están **exentos de la verificación anticlicker** porque:

1. **Stock limitado:** Los aldeanos tienen inventario limitado
2. **Cooldowns naturales:** Los trades tienen regeneración de stock
3. **Clics rápidos legítimos:** Shift+click es comportamiento normal
4. **No es farm infinito:** A diferencia de mobs o minería, no se puede explotar

### Cambios en el Código

**Archivo:** `DynamicXPManager.java`

```java
// ═══ VERIFICACIÓN DE SEGURIDAD ANTI-AUTOCLICK ═══
// EXCEPCIÓN: Los trades con aldeanos NO pasan por anticlicker
// Razón: Stock limitado, cooldowns naturales, clics rápidos son legítimos (shift+click)
boolean isTrading = (source == XPSource.TRADE || source == XPSource.TRADE_RARE);

if (securityManager != null && !isTrading) {
    // Solo verificar anticlicker para fuentes NO relacionadas con trades
    // ... resto del código de verificación
}
```

**Fuentes de XP exentas:**
- ✅ `XPSource.TRADE` - Comercio normal
- ✅ `XPSource.TRADE_RARE` - Comercio de items raros/valiosos

---

## 📋 TESTING RECOMENDADO

Para verificar que el fix funciona correctamente:

1. **Test básico:**
   - Tradear con un aldeano normalmente
   - Verificar que se obtiene XP correctamente

2. **Test de clics rápidos:**
   - Hacer múltiples trades seguidos (shift+click)
   - Verificar que NO se active la detección anticlicker
   - Confirmar que se obtiene XP por todos los trades

3. **Test de anticlicker en otras fuentes:**
   - Verificar que el anticlicker sigue funcionando para:
     - Minado repetitivo
     - Farmeo de mobs
     - Spam de acciones
   
---

## 🔍 NOTAS TÉCNICAS

### Lógica de Exención
```
SI (fuente == TRADE o TRADE_RARE):
    ↳ Saltar verificación anticlicker
    ↳ Procesar XP normalmente
    
SI NO:
    ↳ Aplicar verificación anticlicker normal
    ↳ Detectar patrones sospechosos
    ↳ Aplicar penalizaciones si es necesario
```

### Seguridad Mantenida
El sistema anticlicker sigue activo para:
- ✅ Minado (MINING)
- ✅ Combate (COMBAT)
- ✅ Farmeo (FARMING)
- ✅ Obtención de XP general (XP_GAIN)
- ✅ Drops de tokens (TOKEN_DROP)

---

## 📊 IMPACTO DEL FIX

| Aspecto | Antes | Después |
|---------|-------|---------|
| **XP en trades** | ❌ Bloqueada por anticlicker | ✅ Funciona correctamente |
| **Clics rápidos legítimos** | ❌ Detectados como autoclick | ✅ Permitidos |
| **Shift+click en trades** | ❌ Bloqueado | ✅ Funcional |
| **Seguridad anticlicker** | ✅ Activa (falsos positivos) | ✅ Activa (sin falsos positivos en trades) |
| **Farm de aldeanos** | ⚠️ "Protegido" innecesariamente | ✅ Limitado naturalmente por mecánicas vanilla |

---

## 🎯 CONCLUSIÓN

Este fix **restaura el funcionamiento correcto** del sistema de comercio con aldeanos mientras **mantiene la seguridad anticlicker** para las fuentes de XP que realmente necesitan protección.

Los trades con aldeanos son una mecánica vanilla de Minecraft con protecciones naturales (stock limitado, cooldowns, regeneración lenta), por lo que no requieren capas adicionales de seguridad anti-farm.

---

## 🔄 COMPATIBILIDAD

- ✅ Compatible con sistema anticlicker v1.22.73
- ✅ No afecta otras verificaciones de seguridad
- ✅ Mantiene todas las fuentes de XP existentes
- ✅ No requiere cambios en configuración

---

**Estado:** ✅ RESUELTO  
**Prioridad:** 🔴 CRÍTICA  
**Verificado:** ⏳ Pendiente de testing en servidor
