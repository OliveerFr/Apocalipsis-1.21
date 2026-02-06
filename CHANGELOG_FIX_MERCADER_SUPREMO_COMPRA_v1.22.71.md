# CHANGELOG v1.22.71 - Fix Compra Mercader Supremo

## 🐛 Problema Reportado

El usuario reportó que la habilidad **Mercader Supremo** no se otorgaba al comprarla. El sistema cobraba la XP pero la habilidad no aparecía en el inventario del jugador.

---

## ✅ Cambios Implementados

### 1. **Validación de Skills Deshabilitadas** 🔒

Se añadió validación para prevenir la compra de habilidades deshabilitadas en múltiples puntos:

#### **SkillService.java**
```java
public PurchaseResult purchaseSkill(Player player, Skill skill) {
    // [FIX] Verificar si la skill está habilitada
    if (!skill.isEnabled()) {
        plugin.getLogger().warning("[SkillService] Attempted to purchase disabled skill: " + skill.name());
        return PurchaseResult.DISABLED;
    }
    // ... resto del código
}
```

#### **SkillTreeGUI.java**
```java
private void handleSkillClick(Player player, Skill skill, SkillBranch branch, boolean isShiftClick) {
    // [FIX] Verificar si la skill está habilitada antes de permitir cualquier acción
    if (!skill.isEnabled()) {
        player.sendMessage("§c§l✗ §cEsta habilidad está temporalmente deshabilitada.");
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
        return;
    }
    // ... resto del código
}
```

### 2. **Nuevo Resultado: DISABLED**

Se añadió un nuevo resultado al enum `PurchaseResult`:

```java
public enum PurchaseResult {
    SUCCESS,
    ALREADY_OWNED,
    MISSING_REQUIREMENTS,
    NOT_ENOUGH_XP,
    WOULD_DROP_TOO_LOW,
    DURING_DISASTER,
    DISABLED  // NUEVO
}
```

### 3. **Manejo de Skills Deshabilitadas en GUI**

Se agregó un caso específico en el método de confirmación de compra:

```java
case DISABLED -> {
    player.sendMessage("§c§l✗ §cEsta habilidad está temporalmente deshabilitada.");
    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
}
```

### 4. **Logs de Debugging Mejorados** 🔍

Se añadieron logs detallados para diagnosticar problemas de persistencia:

```java
// Antes de añadir skill
plugin.getLogger().info("[SkillService] Pre-purchase - Skills count: " + data.getSkills().size());

// Después de añadir skill
plugin.getLogger().info("[SkillService] Post-purchase - Skills count: " + data.getSkills().size() + 
    " | Has skill: " + data.hasSkill(skill));

// Verificación en WorldDataManager
plugin.getLogger().info("[SkillService] Captured skills: " + updatedData.getSkillsDesbloqueadas().size() + 
    " | Contains " + skill.name() + ": " + updatedData.getSkillsDesbloqueadas().contains(skill.name()));
```

---

## 🔍 Posibles Causas del Problema

### **Causa 1: Skill Deshabilitada**
- **MERCADER_SUPREMO** podría estar en la lista `DISABLED_SKILLS`
- **Solución**: Los nuevos checks previenen la compra de skills deshabilitadas

### **Causa 2: Problema de Sincronización**
- El sistema de persistencia dual (SkillService + WorldDataManager) podría tener un timing issue
- **Solución**: Los logs ahora rastrean cada paso del proceso

### **Causa 3: Problema de Refresh**
- La GUI podría estar mostrando datos obsoletos después de la compra
- **Solución**: Los logs verifican que `data.hasSkill(skill)` retorna `true` inmediatamente después de comprar

---

## 📝 Archivos Modificados

1. **SkillService.java**
   - Línea ~537: Check de `skill.isEnabled()` antes de comprar
   - Línea ~513: Nuevo resultado `DISABLED` en enum
   - Líneas ~589-599: Logs de debugging mejorados

2. **SkillTreeGUI.java**
   - Líneas ~1333-1338: Check de `skill.isEnabled()` antes de clicks
   - Líneas ~1408-1411: Manejo de resultado `DISABLED`

---

## 🧪 Testing Recomendado

1. **Test 1: Compra Normal de Mercader Supremo**
   ```
   1. Asegurarse de tener 4500 XP
   2. Tener desbloqueadas: toque_fortuna y auto_recoleccion
   3. Comprar Mercader Supremo
   4. Verificar en consola:
      - "Pre-purchase - Skills count: X"
      - "Post-purchase - Skills count: X+1 | Has skill: true"
      - "Captured skills: X+1 | Contains MERCADER_SUPREMO: true"
   5. Verificar que la skill aparece en /avo habilidades
   ```

2. **Test 2: Skill Deshabilitada**
   ```
   1. Añadir "mercader_supremo" a DISABLED_SKILLS
   2. Intentar comprar la habilidad
   3. Debe mostrar: "Esta habilidad está temporalmente deshabilitada"
   4. No debe cobrar XP
   ```

3. **Test 3: Verificar Trade Discount**
   ```
   1. Comprar Mercader Supremo exitosamente
   2. Abrir trade con villager
   3. Verificar que se aplican descuentos (10%/15%/20%)
   4. Verificar que trades muestran 999+ usos
   ```

---

## 📊 Logs Esperados en Consola (Compra Exitosa)

```
[INFO] [SkillService] purchaseSkill called - Player: PlayerName | World: world | Skill: MERCADER_SUPREMO
[INFO] [SkillService] Refreshed player data from world: world | XP: 5000
[INFO] [SkillService] XP Check - PlayerXP: 5000 | Cost: 4500 | Has enough: true
[INFO] [SkillService] Pre-purchase - Skills count: 5
[INFO] [SkillService] Post-purchase - Skills count: 6 | Has skill: true
[INFO] [Skills] Aplicando efectos para PlayerName (6 habilidades)
[INFO] [SkillService] Captured skills: 6 | Contains MERCADER_SUPREMO: true
[INFO] [SkillService] Saved purchase to world data: world | New XP: 500 | Skill: MERCADER_SUPREMO
```

---

## 🎯 Próximos Pasos

1. **Compilar el plugin** con los cambios implementados
2. **Reiniciar el servidor** para aplicar el código actualizado
3. **Intentar comprar Mercader Supremo** de nuevo
4. **Revisar los logs** de consola para identificar dónde falla el proceso
5. **Reportar los logs** si el problema persiste para diagnóstico avanzado

---

## ⚠️ Notas Importantes

- **Mercader Supremo NO está en DISABLED_SKILLS** (lista vacía actualmente)
- La habilidad **SÍ funciona** según el fix anterior (v1.22.70) con el sistema de trades
- El problema reportado es **específico de la compra**, no de la funcionalidad
- Los logs añadidos permitirán **diagnosticar** exactamente dónde se pierde la skill

---

## 🔗 Relacionado con

- **v1.22.70**: Fix de Mercader Supremo para compatibilidad con API 1.21
- **v1.22.64**: Rebalanceo de sistema de habilidades
- **Sistema de Ciclos**: Integración de skills con WorldDataManager

---

**Fecha**: 6 de febrero de 2026  
**Versión**: 1.22.71  
**Tipo**: Bug Fix  
**Prioridad**: Alta
