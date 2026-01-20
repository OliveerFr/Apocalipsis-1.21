# EVENTO 5: LA APERTURA DEL END

## 📖 CONTEXTO NARRATIVO

### DESPUÉS DE LOS EVENTOS ANTERIORES

El mundo no colapsó… pero tampoco volvió a la normalidad.

Las grietas se cerraron.  
Los rituales terminaron.  
Los guardianes cayeron.

**Y aun así, algo quedó abierto.**

No en el Nether.  
No en el Overworld.

Un portal incompleto, antiguo, inestable, apareció lejos de todo.  
No fue construido por jugadores.  
No fue invocado.

Simplemente… **estaba ahí**.

El mundo no lo rechaza.  
Pero tampoco lo acepta.

---

## 👁️ EL OBSERVADOR

**Característica clave:** No habla directamente a los jugadores. Habla "para sí mismo".

### Mensajes previos (simples):

- _"No debería existir tan pronto."_
- _"Antes, esto tomaba más tiempo."_
- _"El mundo está… apurado."_
- _"Si cruzan ese umbral, no hay marcha atrás."_

❌ **Nada de explicar qué es el End.**  
❌ **Nada de decir "el dragón".**  
✅ **Solo tensión.**

---

## 🎬 INICIO DEL EVENTO (STREAM)

Entras al mundo. Todo parece normal.

Después de unos minutos:

### 📢 MENSAJE GLOBAL

```
El aire se vuelve denso.
Algo observa desde un lugar que no debería existir.
```

### 📍 COORDENADAS REVELADAS

El servidor anuncia las coordenadas del portal.

- **No aparece cerca del spawn**
- Los jugadores tienen que ir.
- Eso ya crea aventura, conversación y tiempo de stream.

---

## 🌀 EL PORTAL

El portal del End ya está formado, pero:

- ❌ No está "activo" visualmente
- ❌ No brilla como siempre
- ✅ Emite sonidos bajos, distorsionados

### Cuando los jugadores llegan:

**OBSERVADOR:**  
_"Aquí fue donde todo terminó… más de una vez."_  
_"Y aun así, siguen entrando."_

El portal se activa lentamente.  
No de golpe.  
**Que se sienta importante.**

---

## ⚖️ DECISIÓN COLECTIVA (CLAVE)

### Antes de entrar:

```
El portal espera.
```

- ❌ No hay votación explícita.
- ❌ No hay botón de "sí o no".

**Simplemente:**

- Si entran → el evento sigue
- Si no entran → el mundo queda en pausa incómoda

Obviamente entran.

---

## 🌑 EL END (CAMBIADO)

Al entrar:

- El End es más grande
- Hay estructuras a lo lejos
- El cielo se siente más "pesado"
- **Silencio incómodo**

### OBSERVADOR:

_"No es el mismo lugar."_  
_"Nunca lo es."_

---

## 🐉 EL DRAGÓN

### No aparece inmediatamente.

**Primero:**

1. Sonidos
2. Sombras
3. El cielo se oscurece

**Luego** aparece el Dragón:

- Más fuerte
- Más agresivo
- **Diferente**

### OBSERVADOR:

_"Antes era un final."_  
_"Ahora… es solo otro paso."_

- ❌ No lo llama enemigo.
- ❌ No lo llama mal.

---

## ⚔️ LA BATALLA

Características:

- Es **dura, pero posible**
- **No infinita**
- Se siente como un evento, no una pelea más

### Durante la batalla, el Observador suelta frases cortas:

- _"Siempre lo matan."_
- _"Siempre creen que eso arregla algo."_

❌ Nada de decir si está bien o mal.

---

## 💀 MUERTE DEL DRAGÓN

Cuando el dragón cae:

### ❌ No hay celebración inmediata.

### 📢 MENSAJE GLOBAL:

```
El End guarda silencio.
```

- El portal de regreso aparece.
- El huevo está ahí.
- La experiencia cae.

**Todo lo normal… pero con un tono raro.**

### OBSERVADOR (último mensaje):

_"El mundo avanza."_  
_"La memoria… no."_

---

## 🔮 CIERRE DEL EVENTO (CLIFFHANGER)

Antes de terminar el stream:

### 📢 MENSAJE FINAL:

```
Algo se ha activado más allá de este mundo.
```

**Nada más.**

- ❌ No Nether.
- ❌ No Warden.
- ❌ No Guardian.

✅ **Solo la sensación de:**

_"Ok… ahora sí viene algo grande."_

---

## 🎮 IMPLEMENTACIÓN TÉCNICA

### Comando Principal

```
/avo evento5 start
```

### Fases del Evento

1. **PREPARACION** (30 min)
   - Countdown visible para todos
   - Anuncios en: 30m, 15m, 5m, 1m, 10s

2. **PORTAL_ABIERTO**
   - Portal se activa lentamente
   - Títulos y sonidos sutiles
   - Espera a que jugadores entren

3. **COMBATE** (4 Fases del Dragón)
   - **Fase I** (100-75% HP): Fuerza renovada
   - **Fase II** (75-50% HP): Corrupción manifiesta
   - **Fase III** (50-25% HP): Comportamiento extraño
   - **Fase IV** (25-0% HP): Final

4. **VICTORIA**
   - Espera 3s para animación vanilla
   - Distribuye recompensas
   - MVP para top 3 damage dealers
   - **Cliffhanger final**

### Diálogos del Observador

Los diálogos están organizados en `apertura_end.yml`:

- **Descubrimiento del portal** (antes del evento)
- **Llegada al portal** (al estar físicamente cerca)
- **Entrada al End** (al cruzar el portal)
- **Durante la batalla** (en % de HP específicos)

### Auto-Detección de Plugins

El evento soporta 3 modos:

1. **EPIC** (MythicMobs + ModelEngine)
   - Dragón custom 3D
   - Mecánicas avanzadas

2. **ENHANCED** (Solo MythicMobs)
   - Mecánicas avanzadas
   - Modelo vanilla

3. **VANILLA+** (Sin plugins)
   - Dragón vanilla del End
   - Fases programadas en Java

### Sistema de Recompensas

**Garantizadas:**
- Elytra del Desolador
- Escamas del Vacío (5-10)
- 5,000 EXP adicionales (suma a los 12,000 vanilla)

**Por probabilidad:**
- Corazón del Desolador (30%)
- Escama Perfecta (15%)

**Top 3 Damage:**
- 🥇 **Puesto 1:** Corazón garantizado + 3 Escamas Perfectas + título "§5§lAzote del Desolador"
- 🥈 **Puesto 2:** 2 Escamas Perfectas + 10 Fragmentos + título "§5Cazador del Vacío"
- 🥉 **Puesto 3:** 1 Escama Perfecta + 5 Fragmentos + título "§7Desafiante del End"

---

## 🎨 TONO Y ESTILO

### ✅ SÍ:
- Mensajes cortos y crípticos
- Tensión constante
- Silencio incómodo
- El Observador habla "para sí mismo"
- Cliffhangers sutiles

### ❌ NO:
- Explicaciones largas
- Celebraciones épicas
- Diálogos dramáticos
- Nombres pomposos
- Resolución completa

---

## 📝 ESTADO DE IMPLEMENTACIÓN

### ✅ Completado:
- [x] Estructura básica de `AperturaEndEvent.java`
- [x] Sistema de fases (PREPARACION, PORTAL_ABIERTO, COMBATE, VICTORIA)
- [x] Countdown de 30 minutos
- [x] Spawneo de dragón con HP escalado
- [x] 4 fases de combate del dragón
- [x] BossBar para tracking de fase
- [x] Comandos `/avo evento5` implementados
- [x] Archivo `apertura_end.yml` con nueva narrativa
- [x] Registro del evento en `Apocalipsis.java`

### ⏳ Pendiente:
- [ ] Listener para tracking de daño (`AperturaEndListener.java`)
- [ ] Clase de items personalizados (`AperturaEndItems.java`)
- [ ] Sistema de distribución de recompensas
- [ ] Auto-detección de MythicMobs/ModelEngine
- [ ] Sistema de partículas según contexto
- [ ] Tracking de cristales del End
- [ ] Mensajes del Observador contextuales (trigger al llegar al portal)

---

## 🔗 CONEXIÓN CON EVENTOS ANTERIORES

Este evento es la **culminación narrativa** de:

1. **Eco de Brasas** - Primera grieta
2. **Eco de Sombras** - Corrupción expandida
3. **Susurro de Piedra Rota** - Warden misterioso
4. **El Camino al End** - Portal fragmentado revelado

**La Apertura del End** cierra el arco… pero abre uno nuevo.

---

_"El mundo avanza. La memoria… no."_
