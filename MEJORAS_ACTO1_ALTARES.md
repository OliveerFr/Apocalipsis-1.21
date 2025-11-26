# 🎭 MEJORAS ACTO 1: SISTEMA DE ALTARES PROGRESIVO

## 📋 RESUMEN
Convertir cada fragmento en un **altar místico** donde el jugador debe realizar acciones específicas para activarlo. Cada altar revela parte de la historia y otorga poderes temporales.

---

## ✨ SISTEMA DE ALTARES (5 Fragmentos = 5 Altares)

### 🔮 **Altar 1: El Despertar**
**Ubicación**: Primer fragmento encontrado  
**Acción Requerida**: Permanecer cerca del altar por 10 segundos sin moverse  
**Diálogo**:
- `"...silencio... escucha el susurro antiguo..."`
- `"La piedra recuerda... ha estado rota por eones..."`
- `"Algo intenta comunicarse desde el vacío..."`

**Efectos**:
- Partículas de END_ROD girando alrededor del jugador
- Sonido de portal del End (distorsionado)
- Visión nocturna temporal (30 segundos) para facilitar búsqueda

**Recompensa**: `§d§l⧖ Fragmento del Despertar` + Efecto **Night Vision**

---

### ⚡ **Altar 2: La Resonancia**
**Ubicación**: Segundo fragmento  
**Acción Requerida**: Dropear 3 Ender Pearls cerca del altar (ofrenda)  
**Diálogo**:
- `"...ofrece lo que viaja entre dimensiones..."`
- `"La forma reconoce los ecos del Fin..."`
- `"Tu ofrenda resuena... la piedra vibra..."`

**Efectos**:
- Las Ender Pearls se consumen en partículas PORTAL
- Altar emite pulsos de energía violeta
- Speed II temporal (45 segundos)

**Recompensa**: `§d§l⧖ Fragmento de Resonancia` + Efecto **Speed II**

---

### 🌑 **Altar 3: El Sacrificio**
**Ubicación**: Tercer fragmento  
**Acción Requerida**: Perder 10 corazones de vida cerca del altar  
**Diálogo**:
- `"...la forma exige sacrificio..."`
- `"Tu esencia vital alimenta la conexión..."`
- `"El dolor es temporal... el conocimiento eterno..."`

**Efectos**:
- La vida perdida se convierte en partículas de sangre (REDSTONE)
- El altar absorbe la vida y brilla intensamente
- Regeneration III temporal (30 segundos) después del sacrificio
- Darkness effect durante 5 segundos (dramatismo)

**Recompensa**: `§d§l⧖ Fragmento del Sacrificio` + **Regeneration III** + **Absorption II** (60s)

---

### 🔥 **Altar 4: La Purificación**
**Ubicación**: Cuarto fragmento  
**Acción Requerida**: Eliminar 5 criaturas cerca del altar (spawn automático)  
**Diálogo**:
- `"...demuestra tu valía... defiende el altar..."`
- `"La forma invoca guardianes... supéralos..."`
- `"Solo los fuertes pueden unir lo roto..."`

**Efectos**:
- Spawneo de 5 zombies/skeletons con nombres personalizados:
  - "§8Guardián del Vacío"
  - "§8Eco Corrupto"  
  - "§8Sombra Fragmentada"
- Cada kill genera partículas SOUL_FIRE_FLAME
- Strength II temporal tras matar a todos (60 segundos)

**Recompensa**: `§d§l⧖ Fragmento de Purificación` + **Strength II** + **Resistance I**

---

### 💫 **Altar 5: La Unión (Final)**
**Ubicación**: Quinto y último fragmento  
**Acción Requerida**: Colocar los 4 fragmentos anteriores en pedestales alrededor del altar central  
**Diálogo**:
- `"...los fragmentos se unen... la verdad se revela..."`
- `"La piedra rota fue un sello... ahora se rompe..."`
- `"Has liberado lo que estaba contenido..."`
- `"§c¡EL ACTO 2 COMIENZA! ¡PREPÁRATE!"`

**Efectos Cinemáticos**:
- Los 4 fragmentos brillan y vuelan hacia el altar central
- Explosión de luz cegadora (FLASH_ENCHANTMENT)
- Terremoto (screen shake intenso)
- Grieta dimensional se abre en el cielo
- Transición automática al Acto 2

**Recompensa**: Completar Acto 1 + **Glowing effect** permanente hasta fin de evento

---

## 🎬 MEJORAS CINEMÁTICAS

### **Entrada a Zona de Altar**
Cuando el jugador entra en radio de 20 bloques del altar:
```
§d⧖ Sientes una presencia antigua...
§8Un fragmento te llama desde la oscuridad
```

### **Progreso Visual**
- BossBar mostrando: `§d§l⧖ Fragmentos Reunidos: [2/5]`
- Mapa mostrando puntos de interés (altares restantes)
- Brújula apuntando al altar más cercano no completado

### **Breadcrumbs Místicos**
- Partículas END_ROD cada 10 bloques guiando hacia el altar más cercano
- Se activan solo si el jugador está perdido (sin movimiento por 30s)

---

## 🔧 CAMBIOS TÉCNICOS NECESARIOS

### **Variables Nuevas**:
```java
private Map<UUID, Set<Integer>> fragmentosRecolectados; // Track por jugador
private Map<Integer, Location> altaresLocations; // 5 altares
private Map<Integer, String> tipoAccionAltar; // PERMANECER, OFRENDA, SACRIFICIO, etc.
private Map<UUID, Integer> altarActualJugador; // Altar en el que está
```

### **Métodos Principales**:
```java
verificarProximidadAltar(Player p)
activarAltarTipo(Player p, int numAltar)
completarAltarYRecompensar(Player p, int numAltar)
mostrarProgresoFragmentos(Player p)
iniciarTransicionActo2()
```

---

## 🎯 FLUJO COMPLETO

1. **Jugador se acerca a fragmento** → Mensaje místico + Tutorial de acción
2. **Realiza acción específica** → Diálogo progresivo + Efectos visuales
3. **Altar se activa** → Recompensa + Buff temporal + Fragmento a inventario
4. **Progreso actualizado** → BossBar + Dirección al siguiente altar
5. **Al completar 5 altares** → Secuencia cinemática épica → Acto 2

---

## 💡 IDEAS ADICIONALES OPCIONALES

### **Sistema de Pistas**
Si el jugador tarda más de 5 minutos en encontrar un altar:
```
§d⧖ La Forma susurra una pista...
§7"Busca hacia el [NORTE/SUR/ESTE/OESTE]... más allá de las montañas..."
```

### **Fragmentos Voladores**
Los fragmentos levitan 2 bloques sobre su pedestal y giran lentamente (armor stands invisibles).

### **Sinergias entre Buffs**
Si el jugador tiene múltiples buffs de altares activos:
- **3 buffs activos**: Glowing permanente + Jump Boost
- **4 buffs activos**: Damage Resistance + Fire Resistance
- **5 buffs activos** (solo posible juntando todos rápido): Hero of the Village effect

### **Desafíos Opcionales por Altar**
Agregar un "desafío extra" por altar que otorga recompensa adicional:
- Completar en menos de 60 segundos
- Sin recibir daño
- Sin usar pociones

---

## ✅ CHECKLIST DE IMPLEMENTACIÓN

- [ ] Crear sistema de tracking de altares por jugador
- [ ] Implementar 5 tipos de activación diferentes
- [ ] Escribir diálogos para cada altar (3-4 líneas cada uno)
- [ ] Agregar efectos visuales únicos por altar
- [ ] Crear sistema de recompensas progresivas
- [ ] BossBar de progreso de fragmentos
- [ ] Breadcrumbs/guía hacia altares
- [ ] Secuencia cinemática de unión final
- [ ] Transición suave a Acto 2
- [ ] Testing de balance de dificultad

---

## 🎮 BENEFICIOS

✅ **Mayor inmersión narrativa** - Cada altar cuenta parte de la historia  
✅ **Gameplay más activo** - No solo caminar y recoger  
✅ **Progresión visible** - Jugadores ven claramente su avance  
✅ **Recompensas tangibles** - Buffs que ayudan en Acto 2  
✅ **Rejugabilidad** - Diferentes desafíos hacen cada run único  

---

**PRÓXIMO PASO**: Implementar primero el Altar 1 (El Despertar) como prototipo y testear.
