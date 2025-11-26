# 🎭 MEJORAS ACTO 1: SISTEMA DE ALTARES PROGRESIVO

## 📋 RESUMEN
Convertir cada fragmento en un **altar místico** donde el jugador debe realizar acciones específicas para activarlo. Cada altar representa memorias fragmentadas del mundo, no una entidad comunicándose.

## 🌍 CONTEXTO NARRATIVO
**El Susurro de Piedra Rota** no es "La Forma" hablando - son **memorias del mundo fragmentándose**.  
Los altares rotos "glitchean" y el mundo **recuerda mal**, creando manifestaciones físicas de errores.  
El **Observador** deja pensamientos crípticos (`§8...algo se mueve en la piedra...`) como rastros de su presencia.

---

## ✨ SISTEMA DE ALTARES (5 Fragmentos = 5 Altares)

### 🔮 **Altar 1: El Despertar**
**Ubicación**: Primer fragmento encontrado  
**Acción Requerida**: Permanecer cerca del altar por 10 segundos sin moverse  
**Diálogo** (pensamientos del Observador):
- `§8"...algo se mueve en la piedra..."`
- `§8"...el mundo recuerda... mal..."`
- `§8"...fragmentos de lo que fue..."`

**Efectos**:
- Partículas de END_ROD girando alrededor del jugador
- Sonido de portal del End (distorsionado)
- Visión nocturna temporal (30 segundos) para facilitar búsqueda

**Recompensa**: `§8§l⧖ Fragmento del Despertar` + Efecto **Night Vision**

---

### ⚡ **Altar 2: La Resonancia**
**Ubicación**: Segundo fragmento  
**Acción Requerida**: Dropear 3 Ender Pearls cerca del altar (ofrenda)  
**Diálogo**:
- `§8"...objetos del vacío..."`
- `§8"...resuenan con lo roto..."`
- `§8"...la memoria acepta la ofrenda..."`

**Efectos**:
- Las Ender Pearls se consumen en partículas PORTAL
- Altar emite pulsos de energía gris oscuro
- Speed II temporal (45 segundos)

**Recompensa**: `§8§l⧖ Fragmento de Resonancia` + Efecto **Speed II**

---

### 🌑 **Altar 3: El Sacrificio**
**Ubicación**: Tercer fragmento  
**Acción Requerida**: Perder 10 corazones de vida cerca del altar  
**Diálogo**:
- `§8"...la esencia vital alimenta el recuerdo..."`
- `§8"...dolor compartido con el mundo..."`
- `§8"...sacrificio reconocido..."`

**Efectos**:
- La vida perdida se convierte en partículas de sangre (REDSTONE)
- El altar absorbe la vida y brilla intensamente
- Regeneration III temporal (30 segundos) después del sacrificio
- Darkness effect durante 5 segundos (dramatismo)

**Recompensa**: `§8§l⧖ Fragmento del Sacrificio` + **Regeneration III** + **Absorption II** (60s)

---

### 🔥 **Altar 4: La Purificación**
**Ubicación**: Cuarto fragmento  
**Acción Requerida**: Eliminar 5 criaturas cerca del altar (spawn automático)  
**Diálogo**:
- `§8"...copias defectuosas emergen..."`
- `§8"...errores de memoria atacan..."`
- `§8"...corrígelos..."`

**Efectos**:
- Spawneo de 5 zombies/skeletons con nombres personalizados:
  - "§8Recuerdo Defectuoso"
  - "§8Eco Corrupto"  
  - "§8Memoria Fragmentada"
- Cada kill genera partículas SOUL_FIRE_FLAME
- Strength II temporal tras matar a todos (60 segundos)

**Recompensa**: `§8§l⧖ Fragmento de Purificación` + **Strength II** + **Resistance I**

---

### 💫 **Altar 5: La Unión (Final)**
**Ubicación**: Quinto y último fragmento  
**Acción Requerida**: Colocar los 4 fragmentos anteriores en pedestales alrededor del altar central  
**Diálogo**:
- `§8"...fragmentos recolectados..."`
- `§8"...pero el eco permanece..."`
- `§8"...algo más grande despierta..."`
- `§8"...en el vacío..."`
- `§c¡UN MAL RECUERDO DESPIERTA!`

**Efectos Cinemáticos**:
- Los 4 fragmentos brillan y vuelan hacia el altar central
- Explosión de luz gris oscuro (ASH particles)
- Terremoto (screen shake intenso)
- Grieta dimensional se abre en el suelo
- Transición automática al Acto 2

**Recompensa**: Completar Acto 1 + **Glowing effect** permanente hasta fin de evento

---

## 🎬 MEJORAS CINEMÁTICAS

### **Entrada a Zona de Altar**
Cuando el jugador entra en radio de 20 bloques del altar:
```
§8⧖ ...algo antiguo dormita aquí...
§7Un fragmento de memoria corrupta
```

### **Progreso Visual**
- BossBar mostrando: `§8§l⧖ Fragmentos Reunidos: [2/5]`
- Mapa mostrando puntos de interés (altares restantes)
- Brújula apuntando al altar más cercano no completado

### **Breadcrumbs Místicos**
- Partículas ASH cada 10 bloques guiando hacia el altar más cercano
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

### **Sistema de Terraformación Mejorado**:
El sistema ahora **analiza el bioma circundante** y usa los bloques predominantes para crear una integración natural:
- Detecta los 3 bloques más comunes en un radio de 25 bloques
- Usa esos materiales para crear transiciones graduales (anillos concéntricos)
- Adapta el subsuelo según el tipo de superficie (arena→arenisca, hierba→tierra compacta, etc.)
- Resultado: Los altares parecen parte del terreno natural, no insertados artificialmente

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
§8⧖ ...un eco señala el camino...
§7"Busca hacia el [NORTE/SUR/ESTE/OESTE]... entre [bioma]..."
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

- [x] Sistema de terraformación adaptativo al bioma
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

✅ **Mayor inmersión narrativa** - Fragmentos del mundo, no entidad hablando  
✅ **Gameplay más activo** - No solo caminar y recoger  
✅ **Progresión visible** - Jugadores ven claramente su avance  
✅ **Recompensas tangibles** - Buffs que ayudan en Acto 2  
✅ **Rejugabilidad** - Diferentes desafíos hacen cada run único  
✅ **Integración natural** - Altares se adaptan al bioma circundante  

---

**PRÓXIMO PASO**: Implementar primero el Altar 1 (El Despertar) como prototipo y testear.
