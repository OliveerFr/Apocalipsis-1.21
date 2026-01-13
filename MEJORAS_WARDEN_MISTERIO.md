# Mejoras al Sistema del Warden - Narrativa de Misterio

## 📋 Resumen de Cambios

Se ha implementado una narrativa completa de misterio alrededor del Warden del Evento 4 (Camino al End), transformándolo de un simple boss final a un elemento narrativo que funciona como **spoiler/adelanto de un evento futuro** que ocurrirá después de la apertura del End.

---

## 🎭 Concepto Narrativo

### Tema Central: "Las Profundidades"
- **El Warden NO es del End** - El Observador lo confirma explícitamente
- **El Warden NO es de ningún mundo conocido** - Ni Overworld, ni Nether, ni End
- **Viene desde "Las Profundidades"** - Un lugar/dimensión desconocido más allá de todos los mundos
- **Propósito Ambiguo** - No se sabe si está:
  - Protegiendo algo
  - Advirtiendo sobre algo
  - Siendo enviado por alguien/algo
  - Actuando como heraldo de algo peor

### Rol del Observador
- **No es omnisciente** - Se muestra confundido y alarmado
- **Expresa miedo** - "Si ESO fue enviado como guardián... temo lo que vendrá después del End"
- **Reconoce sus límites** - "No es de NINGÚN mundo que yo conozca"
- **Crea tensión** - Sus reacciones hacen que el jugador sienta que algo grande se acerca

---

## 🔧 Cambios Técnicos Implementados

### 1. Identidad del Warden
**Archivo:** `CaminoEndEvent.java` (línea 3200)

**ANTES:**
```java
warden.setCustomName("§4§l☠ GUARDIÁN DEL VACÍO ABSOLUTO ☠");
```

**DESPUÉS:**
```java
warden.setCustomName("§4§l☠ GUARDIÁN DE LAS PROFUNDIDADES ☠");
```

---

### 2. Mensaje de Aparición del Warden
**Archivo:** `CaminoEndEvent.java` (líneas 3215-3238)

**ANTES:** 2 líneas simples de anuncio

**DESPUÉS:** 20+ líneas de diálogo del Observador mostrando alarma:

```java
messageBus.sendWorldMessage(world, "");
messageBus.sendWorldMessage(world, "§4§l☠§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━§r§4§l☠");
messageBus.sendWorldMessage(world, "§4§l            ⚠ APARICIÓN DEL GUARDIÁN FINAL ⚠");
messageBus.sendWorldMessage(world, "§4§l☠§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━§r§4§l☠");
messageBus.sendWorldMessage(world, "");
messageBus.sendWorldMessage(world, "§c§l§o\"...¿QUÉ... QUÉ ES ESO?...\"");
messageBus.sendWorldMessage(world, "");
messageBus.sendWorldMessage(world, "§7§o\"Eso... eso no es del End...\"");
messageBus.sendWorldMessage(world, "§7§o\"No es de NINGÚN mundo que yo conozca...\"");
messageBus.sendWorldMessage(world, "");
messageBus.sendWorldMessage(world, "§c§l§o\"...Viene desde las PROFUNDIDADES...\"");
messageBus.sendWorldMessage(world, "§8§o\"...desde más allá de todo lo conocido...\"");
messageBus.sendWorldMessage(world, "");
messageBus.sendWorldMessage(world, "§4§l§o\"...Alguien... o ALGO... lo envió...\"");
messageBus.sendWorldMessage(world, "");
messageBus.sendWorldMessage(world, "§8§o\"¿Está... protegiendo algo? ¿O es una advertencia?\"");
messageBus.sendWorldMessage(world, "§4§l§o\"...No lo sé... y eso me aterroriza...\"");
messageBus.sendWorldMessage(world, "");
messageBus.sendWorldMessage(world, "§c§l⚔ ¡DERRÓTALO PARA CONTINUAR! ⚔");
messageBus.sendWorldMessage(world, "");
messageBus.sendWorldMessage(world, "§4§l☠§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━§r§4§l☠");
```

**Efectos:**
- Sonido: `ENTITY_WARDEN_ROAR` (volumen 3.0, pitch 0.8)
- Partículas: `SCULK_SOUL` (100 partículas), `SCULK_CHARGE` (50 partículas)

---

### 3. Título de Aparición
**Archivo:** `CaminoEndEvent.java` (líneas 3243-3244)

**ANTES:**
```java
player.sendTitle("§4§l☠ GUARDIÁN FINAL ☠", "§c¡Derrotadlo para avanzar!", 10, 70, 20);
```

**DESPUÉS:**
```java
player.sendTitle("§4§l☠ GUARDIÁN DE LAS PROFUNDIDADES ☠", 
                "§8§o¿Protector? ¿Advertencia? ¿Heraldo de algo peor?", 10, 70, 20);
```

---

### 4. Mensaje de Retirada del Warden
**Archivo:** `CaminoEndEvent.java` (líneas 3281-3301)

**ANTES:** 2 líneas simples

**DESPUÉS:** 12+ líneas con comentario del Observador:

```java
messageBus.sendWorldMessage(world, "");
messageBus.sendWorldMessage(world, "§4§l§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━§r");
messageBus.sendWorldMessage(world, "§c§l           ⚠ EL GUARDIÁN SE RETIRA ⚠");
messageBus.sendWorldMessage(world, "§4§l§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━§r");
messageBus.sendWorldMessage(world, "");
messageBus.sendWorldMessage(world, "§7§o\"...Regresa a las profundidades...\"");
messageBus.sendWorldMessage(world, "§8§o\"Su misión... aún no ha terminado...\"");
messageBus.sendWorldMessage(world, "");
messageBus.sendWorldMessage(world, "§c§l⚠ ACERCAOS PARA ENFRENTARLO ⚠");
messageBus.sendWorldMessage(world, "");
messageBus.sendWorldMessage(world, "§4§l§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━§r");
```

---

### 5. Mensaje de Bloqueo de Progresión
**Archivo:** `CaminoEndEvent.java` (líneas 356-365)

**ANTES:** 2 líneas simples

**DESPUÉS:** 6 líneas con explicación del Observador:

```java
messageBus.sendPlayerMessage(player, "");
messageBus.sendPlayerMessage(player, "§c§l⚠ EL GUARDIÁN DE LAS PROFUNDIDADES BLOQUEA TU AVANCE ⚠");
messageBus.sendPlayerMessage(player, "");
messageBus.sendPlayerMessage(player, "§7§o\"...No permitirá que avances...\"");
messageBus.sendPlayerMessage(player, "§8§o\"Debes derrotarlo para continuar...\"");
messageBus.sendPlayerMessage(player, "");
```

**Efectos cada 5 segundos:**
- Sonido: `ENTITY_WARDEN_HEARTBEAT` (volumen 2.0, pitch 0.5)
- Partículas: `SCULK_CHARGE` (30 partículas)

---

### 6. Anuncio de Victoria (Muerte del Warden)
**Archivo:** `CaminoEndListener.java` (líneas 679-701)

**ANTES:** Mensaje simple de victoria

**DESPUÉS:** 14+ líneas con el Observador perturbado:

```java
messageBus.sendWorldMessage(world, "");
messageBus.sendWorldMessage(world, "§4§l☠§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━§r§4§l☠");
messageBus.sendWorldMessage(world, "§4§l     ☠ EL GUARDIÁN DE LAS PROFUNDIDADES HA CAÍDO ☠");
messageBus.sendWorldMessage(world, "§4§l☠§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━§r§4§l☠");
messageBus.sendWorldMessage(world, "");
messageBus.sendWorldMessage(world, "§c§l§o\"...Ha caído... pero ¿por qué estaba aquí?...\"");
messageBus.sendWorldMessage(world, "");
messageBus.sendWorldMessage(world, "§7§o\"Este guardián no era del End...\"");
messageBus.sendWorldMessage(world, "§8§o\"...Venía desde más allá... desde LAS PROFUNDIDADES...\"");
messageBus.sendWorldMessage(world, "");
messageBus.sendWorldMessage(world, "§4§l§o\"Si ESO fue enviado como guardián...\"");
messageBus.sendWorldMessage(world, "§4§l§o\"...¿Qué está intentando proteger?...\"");
messageBus.sendWorldMessage(world, "§4§l§o\"...¿O de qué nos está ADVIRTIENDO?...\"");
messageBus.sendWorldMessage(world, "");
messageBus.sendWorldMessage(world, "§8§o\"Lo que viene después del End... podría ser peor...\"");
messageBus.sendWorldMessage(world, "");
messageBus.sendWorldMessage(world, "§d§l⟫ §fEl camino continúa... §c§lpero las sombras se alargan§f...");
messageBus.sendWorldMessage(world, "");
messageBus.sendWorldMessage(world, "§4§l☠§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━§r§4§l☠");
```

---

### 7. Título de Victoria
**Archivo:** `CaminoEndListener.java` (líneas 703-707)

**ANTES:**
```java
player.sendTitle("§4§l☠ VICTORIA ☠", "§c¡El Guardián ha sido derrotado!", 10, 70, 20);
```

**DESPUÉS:**
```java
player.sendTitle("§4§l☠ VICTORIA... TEMPORAL ☠", 
                "§8§o...pero las profundidades guardan más secretos...", 10, 70, 20);

world.playSound(player.getLocation(), Sound.ENTITY_WARDEN_DEATH, 2.0f, 0.7f);
world.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1.5f, 0.5f);
world.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.3f);
```

**Efectos de sonido añadidos:**
- `ENTITY_WARDEN_DEATH` - Muerte del Warden
- `BLOCK_END_PORTAL_SPAWN` - Portal del End (misterio)
- `ENTITY_ENDER_DRAGON_GROWL` - Dragón del End (amenaza futura)

---

### 8. Item Legendario: Corazón de las Profundidades
**Archivo:** `CaminoEndListener.java` (líneas 946-977)

**ANTES:** "Corazón del Vacío Absoluto" con lore genérico

**DESPUÉS:** "Corazón de las Profundidades" con 24 líneas de lore:

```java
private ItemStack crearCorazonDelVacio() {
    ItemStack corazon = new ItemStack(Material.NETHER_STAR);
    ItemMeta meta = corazon.getItemMeta();
    
    meta.setDisplayName("§4§l◆ CORAZÓN DE LAS PROFUNDIDADES ◆");
    
    List<String> lore = new ArrayList<>();
    lore.add("§m                                              ");
    lore.add("§cEl núcleo pulsante del Guardián de las Profundidades");
    lore.add("§8Emana una energía que no pertenece a ningún mundo conocido...");
    lore.add("");
    lore.add("§7• §oNo es del End");
    lore.add("§7• §oNo es del Nether");
    lore.add("§7• §oNo es del Overworld");
    lore.add("");
    lore.add("§5§lORIGEN DESCONOCIDO:");
    lore.add("§8§o\"Viene desde MÁS ALLÁ...\"");
    lore.add("§8§o\"...desde las PROFUNDIDADES que ningún mapa señala...\"");
    lore.add("");
    lore.add("§4§l§o\"...¿Protector? ¿Centinela? ¿Heraldo?...\"");
    lore.add("§8§o\"...No lo sabemos... y quizás sea mejor así...\"");
    lore.add("");
    lore.add("§6§l⚠ ADVERTENCIA DEL OBSERVADOR:");
    lore.add("§7\"Si esto fue enviado como guardián...\"");
    lore.add("§7\"...temo lo que vendrá después del End...\"");
    lore.add("");
    lore.add("§8§oPorta consigo ecos de un evento futuro...");
    lore.add("§8§o...un evento que hará temblar todos los mundos...");
    lore.add("");
    lore.add("§d§lLEGENDARIO §8§o- §4§oSpoiler del Futuro");
    lore.add("§m                                              ");
    
    meta.setLore(lore);
    meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 10, true);
    corazon.setItemMeta(meta);
    
    return corazon;
}
```

**Mensaje de drop actualizado (línea 716):**
```java
messageBus.sendWorldMessage(world, "§d§l⟫ DROP LEGENDARIO: §4Corazón de las Profundidades");
```

---

### 9. Comando Admin Actualizado
**Archivo:** `ApocalipsisCommand.java` (líneas 2810-2830)

**ANTES:** Mensaje decía "DROPS DEL WARDEN FINAL" y "Corazón del Vacío Absoluto"

**DESPUÉS:**
```java
admin.sendMessage("§d§lDROPS DE ANOMALÍAS (Enderman):");
admin.sendMessage("  §8▪ §5Espada del Vacío");
admin.sendMessage("  §8▪ §5Pico del Vacío");
admin.sendMessage("  §8▪ §5Escudo Dimensional");
admin.sendMessage("  §8▪ §5Casco del Observador");
admin.sendMessage("  §8▪ §dPolvo del Vacío");
admin.sendMessage("");
admin.sendMessage("§4§lDROPS DEL GUARDIÁN DE LAS PROFUNDIDADES:");
admin.sendMessage("  §8▪ §4§lCorazón de las Profundidades §7(LEGENDARIO)");
admin.sendMessage("  §8▪ §7  §8§o\"Spoiler de un evento futuro...\"");
admin.sendMessage("  §8▪ §cEspada del Guardián §7(ÉPICO)");
admin.sendMessage("  §8▪ §cHacha del Guardián §7(ÉPICO)");
admin.sendMessage("  §8▪ §cPeto del Guardián §7(ÉPICO)");
admin.sendMessage("  §8▪ §cPantalones del Guardián §7(ÉPICO)");
```

---

## 🎮 Experiencia del Jugador

### Secuencia Completa de Eventos:

1. **Al alcanzar 35 fragmentos:**
   - El Observador grita confundido: "¿QUÉ... QUÉ ES ESO?"
   - Reconoce que no es del End ni de ningún mundo conocido
   - Menciona "Las Profundidades" con terror
   - Cuestiona si está protegiendo algo o es una advertencia
   - El Warden aparece con nombre "GUARDIÁN DE LAS PROFUNDIDADES"
   - Título pregunta: "¿Protector? ¿Advertencia? ¿Heraldo de algo peor?"

2. **Si los jugadores se alejan:**
   - El Observador comenta que regresa a las profundidades
   - Indica que su misión aún no ha terminado

3. **Al intentar obtener más fragmentos:**
   - El Observador explica que el Guardián no permitirá avanzar
   - Mensaje cada 5 segundos con latidos del corazón del Warden

4. **Al derrotar al Warden:**
   - El Observador expresa alivio... pero también temor
   - Pregunta por qué estaba ahí
   - Cuestiona qué está protegiendo o de qué advierte
   - Expresa miedo: "Si ESO fue enviado como guardián... temo lo que vendrá después del End"
   - Título: "VICTORIA... TEMPORAL"
   - Subtítulo: "...pero las profundidades guardan más secretos..."

5. **Al obtener el Corazón:**
   - Item legendario con lore extenso
   - 24 líneas explicando el misterio
   - Etiquetado como "Spoiler del Futuro"
   - Advertencia del Observador sobre lo que viene

---

## 📊 Elementos Narrativos Clave

### Palabras/Frases Recurrentes:
- ✅ "Las Profundidades" (mencionado 8+ veces)
- ✅ "No es del End" / "No es de ningún mundo conocido" (3+ veces)
- ✅ "¿Protegiendo algo?" / "¿Advertencia?" (5+ veces)
- ✅ "Después del End" / "Lo que viene" (3+ veces)
- ✅ "Temo..." / "Me aterroriza" (2+ veces)

### Misterios Sin Resolver (Intencional):
- ❓ ¿Qué son "Las Profundidades"?
- ❓ ¿Quién envió al Warden?
- ❓ ¿Qué está protegiendo?
- ❓ ¿De qué está advirtiendo?
- ❓ ¿Qué vendrá después del End?
- ❓ ¿Por qué el Observador tiene miedo?

---

## 🎯 Objetivos Cumplidos

✅ **El Warden NO parece del End** - Confirmado múltiples veces por el Observador  
✅ **Origen misterioso** - "Las Profundidades" establecido como lugar desconocido  
✅ **Spoiler de evento futuro** - Mencionado explícitamente en el lore del item  
✅ **Propósito ambiguo** - Se mantiene la duda: ¿protector, advertencia, o heraldo?  
✅ **Observador confundido** - Expresa alarma, miedo, y reconoce sus límites  
✅ **Tensión narrativa** - Cada interacción aumenta el misterio  
✅ **Foreshadowing consistente** - En diálogos, títulos, y lore de items  

---

## 🔮 Hooks para Eventos Futuros

El sistema está diseñado para que un futuro evento pueda:

1. **Revelar qué son "Las Profundidades"**
   - Nueva dimensión desconocida
   - Plano de existencia más allá de los mundos normales
   - Origen de criaturas antiguas/primordiales

2. **Explicar la misión del Warden**
   - ¿Estaba protegiendo el acceso al End de algo peor?
   - ¿Estaba advirtiendo sobre las consecuencias de abrirlo?
   - ¿Fue enviado por alguien que conoce el futuro?

3. **Conectar con la apertura del End**
   - El evento del End podría liberar/despertar algo
   - Las Profundidades podrían responder a la apertura
   - El Warden podría haber sido el primero de muchos

4. **Expandir el rol del Observador**
   - ¿Por qué no conocía este mundo?
   - ¿Qué más no sabe?
   - ¿Tiene conexión con Las Profundidades?

---

## 🛠️ Comandos de Prueba

### Obtener todos los items únicos (Admin):
```
/avo evento4 getitemsevento4
```

### Probar aparición del Warden:
```
/avo evento4 testwarden
```

### Ver estado del evento:
```
/avo evento4 status
```

---

## 📝 Notas Técnicas

- **Compilación:** ✅ Exitosa (Maven)
- **Archivos modificados:** 3
  - `CaminoEndEvent.java` - Spawn, títulos, mensajes de progresión
  - `CaminoEndListener.java` - Victoria, item legendario
  - `ApocalipsisCommand.java` - Comando admin, imports

- **Imports añadidos:**
  ```java
  import org.bukkit.inventory.ItemStack;
  import org.bukkit.event.HandlerList;
  import org.bukkit.event.RegisteredListener;
  ```

- **Líneas de código añadidas:** ~100+
- **Efectos de sonido usados:** 6 diferentes
- **Efectos de partículas:** 3 tipos
- **Mensajes del Observador:** 70+ líneas nuevas

---

## ✨ Conclusión

El Warden ha pasado de ser un simple "boss final" a convertirse en un **elemento narrativo profundo** que:

1. Crea misterio genuino (incluso el Observador está confundido)
2. Establece worldbuilding ("Las Profundidades")
3. Funciona como spoiler/adelanto de eventos futuros
4. Genera tensión y anticipación
5. Deja preguntas sin responder (apropiado para un misterio)

El jugador ahora experimenta el evento no solo como un desafío de combate, sino como una **experiencia narrativa** que planta semillas para futuros contenidos.

---

**Versión del Plugin:** 1.22.44  
**Fecha de Implementación:** 2026-01-12  
**Estado:** ✅ Compilado y listo para pruebas
