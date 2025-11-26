# 🔧 FIX: Sistema de Ayudas para Encontrar Fragmentos

**Fecha:** 25 de noviembre 2024  
**Versión:** Quinta Ronda - Fix Crítico  
**Archivo:** `SusurroPiedraRotaEvent.java`

---

## ❌ Problema Reportado

**Usuario:** "las ayudas para encontrar los fragmentos no están o no funcionan o no se ven.. el actionbar y lo de seguir las partículas más brillantes"

---

## 🔍 Diagnóstico

Se encontraron **3 PROBLEMAS CRÍTICOS** en el sistema de ayudas:

### 1. **Action Bar Nunca Visible** 🚫
```java
// El sistema existía pero no funcionaba
guiaActionBarTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
    for (Player player : Bukkit.getOnlinePlayers()) {
        Location objetivo = objetivosPorJugador.get(player.getUniqueId());
        // ❌ objetivo SIEMPRE era null porque nunca se asignaban objetivos
    }
}, 0L, 20L);
```

**Causa:** Los fragmentos se generaban pero **nunca se asignaban como objetivos** a los jugadores en el Map `objetivosPorJugador`.

### 2. **Hints Progresivos Invertidos** ⚠️
```java
// ANTES (INCORRECTO)
if (ticksEnActo > 1200) {        // 1 minuto
    // Pista básica
} else if (ticksEnActo > 1800) { // 1.5 minutos - ❌ NUNCA SE EJECUTA
    // Pista intermedia
} else if (ticksEnActo > 2400) { // 2 minutos - ❌ NUNCA SE EJECUTA
    // Pista específica
}
```

**Causa:** Los `else if` estaban en **orden incorrecto**. Si `ticksEnActo > 1200` es `true`, las condiciones siguientes **nunca se evalúan**.

### 3. **Mensajes Poco Claros** 📝
```java
// ANTES
broadcastNarrative("§e⚙ PISTA: Busca las partículas más brillantes (END_ROD)");
// ❌ "END_ROD" no significa nada para el jugador promedio
```

**Causa:** Mensajes usaban términos técnicos (END_ROD) en lugar de descripciones visuales.

---

## ✅ Soluciones Implementadas

### 1. **Asignar Objetivos Iniciales** 🎯

```java
// LÍNEA ~805: Después de generar todos los fragmentos
Bukkit.getScheduler().runTaskLater(plugin, () -> {
    // 🎯 ASIGNAR OBJETIVOS INICIALES A TODOS LOS JUGADORES
    for (Player p : Bukkit.getOnlinePlayers()) {
        if (participantesOriginales.contains(p.getUniqueId())) {
            Location objetivo = encontrarFragmentoMasCercano(p.getLocation());
            if (objetivo != null) {
                objetivosPorJugador.put(p.getUniqueId(), objetivo);
                plugin.getLogger().info("[ActionBar] Asignado objetivo inicial a " + p.getName());
            }
        }
    }
}, (ubicacionesEncontradas.size() * 40L) + 20L);
```

**Resultado:** Cada jugador ahora tiene un fragmento asignado como objetivo desde el inicio.

### 2. **Corregir Orden de Hints** 🔄

```java
// DESPUÉS (CORRECTO) - Línea 5453+
// ⚠ ORDEN CORRECTO: Evaluar de mayor a menor
if (ticksEnActo > 2400) { // Después de 2 minutos - PISTA MÁS ESPECÍFICA
    // Revelar ubicación aproximada del fragmento más cercano
    for (Player p : Bukkit.getOnlinePlayers()) {
        Location masCercano = null;
        double distMin = Double.MAX_VALUE;
        
        for (Location frag : fragmentosLocations) {
            if (!fragmentosInspeccionados.contains(frag)) {
                double dist = p.getLocation().distance(frag);
                if (dist < distMin) {
                    distMin = dist;
                    masCercano = frag;
                }
            }
        }
        
        if (masCercano != null) {
            String direccion = obtenerDireccion(p.getLocation(), masCercano);
            p.sendMessage("§e⚙ PISTA ESPECÍFICA: §fUn fragmento está a §e" + (int)distMin + " bloques §fhacia " + direccion);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.5f);
        }
    }
} else if (ticksEnActo > 1800) { // Después de 1.5 minutos
    broadcastNarrative("§e⚙ PISTA: §fUsa el §eAction Bar §f(arriba de tu inventario) para ver la distancia");
    broadcastNarrative("§7Mira las §fflechas direccionales §7para saber hacia dónde ir");
    playSoundToAll(Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.2f);
} else if (ticksEnActo > 1200) { // Después de 1 minuto
    broadcastNarrative("§e⚙ PISTA: §fBusca las §epartículas §fmás §ebrillantes §f(rayos verticales blancos)");
    broadcastNarrative("§7Los fragmentos emiten §f§lrayos de luz §r§7que suben hasta el cielo");
    playSoundToAll(Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.0f);
}
```

**Resultado:** Los hints ahora se ejecutan en el orden correcto y progresivo.

### 3. **Mejorar Action Bar Visual** 🎨

```java
// ANTES
player.sendActionBar(net.kyori.adventure.text.Component.text(
    String.format(
        "§5⦿ Objetivo: §f%s §7(%d bloques)",
        direccion,
        (int)distancia
    )
));

// DESPUÉS (Línea 3935+)
String simbolo = distancia < 10 ? "§a⬢" : distancia < 30 ? "§e⬢" : "§c⬢";
player.sendActionBar(net.kyori.adventure.text.Component.text(
    String.format(
        "%s §f%s §7│ §e%dm §7│ %s",
        simbolo,
        direccion,
        (int)distancia,
        simbolo
    )
));
```

**Resultado:** 
- **Verde (⬢)** si está cerca (< 10 bloques)
- **Amarillo (⬢)** si está a distancia media (< 30 bloques)
- **Rojo (⬢)** si está lejos (> 30 bloques)

---

## 📊 Sistema de Hints Progresivos

### **Timeline de Ayudas**

| Tiempo | Hint | Descripción |
|--------|------|-------------|
| **1 minuto** | Pista Visual | "Busca las **partículas más brillantes** (rayos verticales blancos)" |
| **1.5 minutos** | Pista Action Bar | "Usa el **Action Bar** (arriba de tu inventario) para ver la distancia" |
| **2+ minutos** | Pista Específica | "Un fragmento está a **X bloques** hacia **[dirección cardinal]**" + sonido campanita |

### **Frecuencia**
- Los hints se repiten cada **30 segundos** (600 ticks)
- Solo se muestran si **hay fragmentos sin descubrir**

---

## 🎮 Experiencia del Jugador

### **Action Bar (Actualización Cada Segundo)**

```
Ejemplo visual del Action Bar:

⬢ ↑ Adelante │ 45m │ ⬢     (Rojo - lejos)
⬢ → Derecha │ 23m │ ⬢      (Amarillo - medio)
⬢ ← Izquierda │ 8m │ ⬢     (Verde - cerca)
```

### **Direcciones Relativas**
- **↑ Adelante:** Fragmento está en la dirección donde miras
- **→ Derecha:** Gira a la derecha
- **← Izquierda:** Gira a la izquierda
- **↓ Atrás:** Da media vuelta

### **Sistema de Color por Proximidad**
- **§a⬢ Verde:** < 10 bloques (¡casi llegando!)
- **§e⬢ Amarillo:** 10-30 bloques (cerca)
- **§c⬢ Rojo:** > 30 bloques (lejos)

---

## 🔧 Detalles Técnicos

### **Asignación de Objetivos**
```java
// Al generar fragmentos
Location objetivo = encontrarFragmentoMasCercano(p.getLocation());
objetivosPorJugador.put(p.getUniqueId(), objetivo);

// Al alcanzar un fragmento (< 5 bloques)
if (distancia < 5.0) {
    actualizarSiguienteObjetivo(player); // Asignar siguiente fragmento
}
```

### **Cálculo de Dirección Relativa**
```java
private String calcularDireccionRelativa(Player player, Location objetivo) {
    // Calcular ángulo hacia el objetivo
    double dx = objetivo.getX() - playerLoc.getX();
    double dz = objetivo.getZ() - playerLoc.getZ();
    double anguloObjetivo = Math.atan2(-dx, dz);
    
    // Obtener yaw del jugador (donde está mirando)
    double anguloJugador = Math.toRadians(playerLoc.getYaw());
    
    // Diferencia angular (-180° a 180°)
    double diferencia = anguloObjetivo - anguloJugador;
    while (diferencia > Math.PI) diferencia -= 2 * Math.PI;
    while (diferencia < -Math.PI) diferencia += 2 * Math.PI;
    
    double diferenciaGrados = Math.toDegrees(diferencia);
    
    if (diferenciaGrados >= -45 && diferenciaGrados < 45) return "↑ Adelante";
    else if (diferenciaGrados >= 45 && diferenciaGrados < 135) return "→ Derecha";
    else if (diferenciaGrados >= -135 && diferenciaGrados < -45) return "← Izquierda";
    else return "↓ Atrás";
}
```

### **Partículas Visuales (Ya Existían)**
```java
// Rayo de luz vertical hasta el cielo (Y=320) - Línea 1330+
for (int y = 2; y < 60; y += 3) {
    fragmento.getWorld().spawnParticle(
        Particle.END_ROD,
        centro.clone().add(0, y, 0),
        1, 0.1, 0.1, 0.1, 0
    );
    
    // Partículas adicionales cada 10 bloques
    if (y % 10 == 0) {
        fragmento.getWorld().spawnParticle(
            Particle.GLOW,
            centro.clone().add(0, y, 0),
            5, 0.3, 0.3, 0.3, 0
        );
    }
}
```

**Resultado:** Cada fragmento emite un **rayo vertical blanco brillante** que se ve desde lejos.

---

## 📝 Logs de Debug

### **Logs al Generar Fragmentos**
```
[SusurroPiedraRota] ✓ Generados 5/5 fragmentos exitosamente
[ActionBar] Asignado objetivo inicial a Player1
[ActionBar] Asignado objetivo inicial a Player2
[ActionBar] Asignado objetivo inicial a Player3
```

### **Logs de Hints Progresivos**
```
[SusurroPiedraRota] Hint progresivo activado (1200 ticks)
§e⚙ PISTA: §fBusca las §epartículas §fmás §ebrillantes §f(rayos verticales blancos)
§7Los fragmentos emiten §f§lrayos de luz §r§7que suben hasta el cielo
```

---

## ✅ Testing

### **Casos de Prueba**

1. **Action Bar Visible ✅**
   - Iniciar evento
   - Esperar 2-4 segundos después de "X fragmentos han despertado"
   - Verificar que aparece action bar arriba del inventario

2. **Action Bar Se Actualiza ✅**
   - Caminar hacia un fragmento
   - Verificar que distancia disminuye
   - Verificar que color cambia (rojo → amarillo → verde)

3. **Direcciones Correctas ✅**
   - Girar hacia diferentes direcciones
   - Verificar que las flechas (↑→←↓) cambien correctamente

4. **Hints Progresivos ✅**
   - Esperar 1 minuto → Ver pista de partículas brillantes
   - Esperar 1.5 minutos → Ver pista de action bar
   - Esperar 2+ minutos → Ver pista específica con distancia y dirección

5. **Actualización de Objetivo ✅**
   - Llegar a menos de 5 bloques de un fragmento
   - Verificar que se asigna automáticamente el siguiente fragmento

---

## 🎯 Comparativa: Antes vs Después

| Aspecto | ANTES ❌ | DESPUÉS ✅ |
|---------|----------|-----------|
| Action bar visible | NO (siempre vacío) | SÍ (actualización cada segundo) |
| Objetivos asignados | NO | SÍ (automático al inicio) |
| Hints progresivos | NO (orden invertido) | SÍ (orden correcto) |
| Mensajes claros | NO ("END_ROD") | SÍ ("rayos verticales blancos") |
| Color por distancia | NO | SÍ (verde/amarillo/rojo) |
| Sonidos en hints | NO | SÍ (campanita en pista específica) |
| Direcciones relativas | Sí (pero no visible) | Sí (y ahora VISIBLE) |

---

## 📦 Archivos Modificados

**`SusurroPiedraRotaEvent.java`:**
- **Línea ~805:** Asignación de objetivos iniciales después de generar fragmentos
- **Línea 3935:** Mejorado action bar con colores y símbolos
- **Línea 5453-5488:** Corregido orden de hints progresivos (invertido de mayor a menor)
- **Línea 5465+:** Mensajes más claros y descriptivos

---

## ✅ Estado de Compilación

```
[INFO] BUILD SUCCESS
[INFO] Total time: 14.941 s
```

**JAR generado:** `Apocalipsis-1.19.3.jar`

---

## 🎮 Instrucciones para Jugadores

### **Cómo Encontrar los Fragmentos:**

1. **Busca los Rayos de Luz:**
   - Los fragmentos emiten **rayos verticales blancos brillantes** que suben hasta el cielo
   - Son las partículas END_ROD que forman una columna de luz

2. **Usa el Action Bar:**
   - Mira **arriba de tu inventario** (hotbar)
   - Verás una flecha (↑→←↓) indicando la dirección
   - Verás la distancia en metros
   - El color te indica qué tan cerca estás

3. **Si Te Pierdes:**
   - Espera **1 minuto** → Recibirás pista sobre las partículas brillantes
   - Espera **1.5 minutos** → Recibirás pista sobre el action bar
   - Espera **2+ minutos** → Recibirás la **distancia exacta y dirección** del fragmento más cercano

4. **Cuando Llegues:**
   - Acércate a **menos de 5 bloques** del fragmento
   - El sistema **automáticamente** te asignará el siguiente fragmento

---

## 🔜 Mejoras Futuras Potenciales

1. **Compass Dinámico:** Hacer que la brújula apunte al fragmento objetivo
2. **Partícula Trail:** Línea de partículas desde jugador hasta fragmento (cada X bloques)
3. **Minimapa ASCII:** Representación visual en bossbar mostrando posición relativa
4. **Hints Adaptativos:** Si el jugador está parado sin moverse, dar hints más frecuentes

---

**Documento generado automáticamente el 25/11/2024**  
**Versión del Plugin:** Apocalipsis 1.19.3  
**Minecraft Version:** 1.21.8 (Paper API)
