# 🎁 Sistema de Canje de Tokens Mejorado - v1.22.56

## 🎯 Mejoras Implementadas

### **1. Sistema de Mensajes Clicables en Chat**

El menú de canje ahora muestra botones clicables directamente en el chat para facilitar la compra:

**Antes:**
```
✓ Kit Diamante
   Armadura completa de diamante + herramientas
   Costo: 8 tokens
   Comando: /avo canjear kit_diamante
```

**Ahora:**
```
✓ 💎 Kit Diamante
   Armadura completa de diamante + herramientas
   Costo: 8 tokens ██████████
   » CLICK AQUÍ PARA CANJEAR «  ← ¡CLICABLE!
```

✅ **Características:**
- Mensajes con hover text que muestra información adicional
- Click directo para ejecutar el canje
- Barra de progreso visual mostrando si tienes suficientes tokens
- Iconos de categoría para identificar rápidamente el tipo de recompensa

---

## 🎨 Interfaz Mejorada

### **Nuevo Diseño del Menú:**
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        🌟 TIENDA DE TOKENS DE STREAM
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Tus tokens: 45 (40 en DB + 5 en inventario)

🎯RECOMPENSAS DISPONIBLES:

✓ 🍖 Pack de Comida
   64 Bistecs + 32 Manzanas Doradas
   Costo: 3 tokens ██████████
   » CLICK AQUÍ PARA CANJEAR «

✓ 🏹 Pack de Arquero
   5 Stacks de flechas + Arco Power V
   Costo: 4 tokens ██████████
   » CLICK AQUÍ PARA CANJEAR «

✗ 👑 PACK SUPREMO DEL STREAM
   El pack definitivo: Kit Netherite Full + Extras
   Costo: 60 tokens ███████░░░
   (Te faltan 15 tokens)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
También puedes usar: /avo canjear <nombre>
```

---

## 📦 Nuevas Recompensas Añadidas

### **💎 Recursos Básicos (1-5 tokens)**

1. **🍖 Pack de Comida** - 3 tokens
   - 64 Bistecs cocidos
   - 32 Manzanas doradas

2. **🏹 Pack de Arquero** - 4 tokens
   - 320 flechas (5 stacks)
   - Arco encantado (Power V + Infinity)

3. **⭐ Pack de Experiencia** - 5 tokens
   - 32 Botellas de experiencia

---

### **⚔️ Equipamiento (6-12 tokens)**

4. **🚀 Cohetes para Élitras** - 6 tokens
   - 64 cohetes de fuegos artificiales

5. **💎 Kit Diamante** - 8 tokens
   - Armadura completa de diamante (4 piezas)
   - Espada, pico, hacha, y pala de diamante

6. **📚 Pack de Encantamientos** - 10 tokens
   - 64 Lapislázuli
   - Mesa de encantamientos
   - 16 Estanterías
   - 1 Yunque

7. **🛡️ Bloques de Protección x3** - 12 tokens
   - 3 bloques de protección de terreno

---

### **🌟 Épico (13-20 tokens)**

8. **🦅 Élitro del Stream** - 15 tokens
   - 1 Élitro
   - 128 cohetes

9. **📦 Pack de Shulker Boxes** - 16 tokens
   - 8 Shulker boxes de colores variados

10. **🔱 Pack de Beacon** - 18 tokens
    - 1 Beacon
    - 164 bloques de hierro (para pirámide completa)

11. **👁️ Kit del End** - 20 tokens
    - 16 Perlas de Ender
    - 12 Ojos de Ender
    - 1 Enderchest

---

### **🔥 Legendario (21-35 tokens)**

12. **🔥 Kit Netherite** - 25 tokens
    - Armadura completa de netherite (4 piezas)

13. **🗿 Pack de Tótems** - 28 tokens
    - 3 Tótems de la Inmortalidad

14. **🏗️ Kit Constructor Maestro** - 30 tokens
    - 320 piedra
    - 320 tablones de roble
    - 128 vidrio
    - 64 bloques de cuarzo
    - 64 piedra luminosa
    - 32 linternas marinas
    - Pico de netherite (Efficiency V)

15. **🍎 Pack de Manzanas Encantadas** - 32 tokens
    - 16 Manzanas doradas encantadas

---

### **💫 Mítico (36+ tokens)**

16. **💫 MEGA PACK ÉPICO** - 40 tokens
    - 20 Lingotes de netherite
    - 32 Diamantes
    - 8 Manzanas doradas encantadas
    - 3 Tótems
    - 5 Estrellas del Nether
    - 5 Bloques de protección

17. **⭐ Pack de Estrellas del Nether** - 45 tokens
    - 10 Estrellas del Nether

18. **👑 PACK SUPREMO DEL STREAM** - 60 tokens
    - Armadura Netherite Full (Protection IV, Unbreaking III, Mending)
    - Espada Netherite (Sharpness V, Unbreaking III, Mending, Looting III)
    - Pico Netherite (Efficiency V, Unbreaking III, Mending, Fortune III)
    - Élitro (Unbreaking III, Mending)
    - 32 Manzanas doradas encantadas
    - 5 Tótems
    - 10 Estrellas del Nether
    - 256 cohetes
    - 10 Bloques de protección

---

## 🔧 Cambios Técnicos

### **StreamFeaturesManager.java**

#### **Método showRedeemMenu() Reescrito:**

**Nuevas características:**
```java
// 1. Muestra tokens de DB + inventario
int tokensInv = countTokensInInventory(player);
int totalTokens = tokens + tokensInv;

// 2. Ordenamiento automático por precio
sortedKeys.sort((a, b) -> {
    int costoA = recompensas.getConfigurationSection(a).getInt("costo_tokens", 0);
    int costoB = recompensas.getConfigurationSection(b).getInt("costo_tokens", 0);
    return Integer.compare(costoA, costoB);
});

// 3. Barra de progreso visual
int progress = Math.min(100, (int)((double)totalTokens / costo * 100));
int bars = progress / 10;
String progressBar = barColor + "█".repeat(bars) + "§8" + "░".repeat(10 - bars);

// 4. Mensajes clicables con Adventure API
net.kyori.adventure.text.Component clickable = net.kyori.adventure.text.Component.text()
    .append(Component.text("   ").color(NamedTextColor.GRAY))
    .append(Component.text("» CLICK AQUÍ PARA CANJEAR «")
        .color(NamedTextColor.GREEN)
        .decorate(TextDecoration.BOLD)
        .hoverEvent(HoverEvent.showText(
            Component.text("§aClick para canjear " + nombre)
        ))
        .clickEvent(ClickEvent.runCommand("/avo canjear " + rewardId))
    )
    .build();
player.sendMessage(clickable);
```

---

### **stream_features.yml**

#### **Nueva Estructura:**
```yaml
recompensas:
  pack_comida:
    nombre: "&a&lPack de Comida"
    descripcion: "64 Bistecs + 32 Manzanas Doradas"
    categoria: "🍖"  # NUEVO: Icono de categoría
    costo_tokens: 3
    items:
      - "give %player% minecraft:cooked_beef 64"
      - "give %player% minecraft:golden_apple 32"
```

**Categorías implementadas:**
- 🍖 Comida
- 🏹 Combate
- ⭐ Experiencia
- 🚀 Movilidad
- 💎 Recursos
- 📚 Encantamientos
- 🛡️ Protección
- 🦅 Élitros
- 📦 Almacenamiento
- 🔱 Utilidades
- 👁️ End
- 🔥 Netherite
- 🗿 Tótems
- 🏗️ Construcción
- 🍎 Consumibles
- 💫 Épico
- ⭐ Nether
- 👑 Supremo

---

## 📊 Comparación de Recompensas

### **Antes:**
- 6 recompensas totales
- Sin categorías
- Sin ordenamiento
- Comandos manuales

### **Ahora:**
- 18 recompensas totales (3x más variedad)
- 8 categorías organizadas
- Ordenamiento automático por precio
- Click directo para canjear
- Barras de progreso visuales
- Iconos identificativos

---

## 💡 Mejoras de UX

### **Información Clara:**
✅ Muestra tokens en DB e inventario por separado
✅ Indica cuántos tokens faltan si no puedes comprar
✅ Barra de progreso visual para ver tu avance
✅ Hover text con información adicional
✅ Iconos de categoría para identificación rápida

### **Facilidad de Uso:**
✅ Un solo click para canjear
✅ Ordenamiento de menor a mayor precio
✅ Comando alternativo disponible: `/avo canjear <nombre>`

### **Retroalimentación:**
✅ Indicadores visuales ✓ (disponible) / ✗ (no disponible)
✅ Colores según disponibilidad
✅ Mensajes de confirmación al canjear

---

## 🎮 Cómo Usar

### **Opción 1: Menú Clicable**
```
1. Usa: /avo canjear
2. Aparece el menú en el chat
3. Click en "» CLICK AQUÍ PARA CANJEAR «"
4. ¡Listo! Recibes los items
```

### **Opción 2: Comando Directo**
```
/avo canjear <nombre_recompensa>

Ejemplos:
/avo canjear pack_comida
/avo canjear kit_diamante
/avo canjear pack_supremo
```

---

## 📝 Archivos Modificados

- [StreamFeaturesManager.java](src/main/java/me/apocalipsis/missions/StreamFeaturesManager.java) - Método `showRedeemMenu()` completamente reescrito
- [stream_features.yml](src/main/resources/stream_features.yml) - 12 nuevas recompensas añadidas con categorías

---

## ✅ Compilación

```
BUILD SUCCESS
JAR: Apocalipsis-1.22.56.jar
```

---

## 🚀 Próximas Mejoras Sugeridas

- [ ] Sistema de descuentos por rangos
- [ ] Recompensas temporales por eventos especiales
- [ ] Historial de canjes del jugador
- [ ] Límites de compra diarios/semanales
- [ ] Pack de recompensas rotativo

---

**Fecha:** 28 de enero de 2026  
**Versión:** 1.22.56  
**Tipo:** Feature Enhancement  
**Prioridad:** Media (mejora de UX)
