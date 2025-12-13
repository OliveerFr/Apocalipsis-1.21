# 🎮 SISTEMA DE PRESENCIA DEL STREAMER v2.0
## XP Normal cuando streameas, reducido cuando no estás

---

## 📊 RESUMEN EJECUTIVO - VERSIÓN SIMPLIFICADA

### ✅ SISTEMA ACTUAL:

**Cuando TÚ estás ONLINE:**
- ⭐ **XP normal (x1.0)** - Todo funciona como siempre
- ⭐ **Eventos especiales disponibles** (configurables)
- ⭐ **Drops especiales disponibles** (configurables)

**Cuando TÚ estás OFFLINE:**
- ⚠️ **XP reducido (x0.3)** - 70% menos XP
- ⚠️ Sin eventos especiales
- ⚠️ Sin drops exclusivos

**Cambios respecto a v1.0:**
- ❌ Eliminados multiplicadores excesivos (x3, x5, x15)
- ❌ Eliminado bonus acumulativo por horas
- ❌ Eliminado XP por minar cuarzo
- ✅ Sistema más simple y claro
- ✅ Los eventos ya proporcionan dinamismo suficiente

---

## 🎯 IMPACTO SIMPLIFICADO

### Progresión Comparada (jugador promedio):

| Escenario | XP/hora | Días para Nivel 50 |
|-----------|---------|---------------------|
| **CON STREAMER** | ~1,200 XP | ~90 días |
| **SIN STREAMER** | ~360 XP | ~300 días |
| **Diferencia** | **x3.3** | **x3.3** |

**Conclusión:** Los jugadores avanzarán **3 veces más rápido** cuando jueguen contigo.

---

## ⚙️ CONFIGURACIÓN

### Editar tu username:

**Archivo:** `recompensas.yml` (línea 149)
```yaml
presencia_streamer:
  streamer_username: "Riolu"  # ← CAMBIA ESTO
```

### Ajustar multiplicadores:

```yaml
# Cuando estás ONLINE
multiplicador_online: 1.0      # x1.0 = XP normal

# Cuando estás OFFLINE
multiplicador_offline: 0.3     # x0.3 = 70% menos (puedes bajar a 0.2 o 0.1)
```

---

## 🎮 INTEGRACIÓN CON /avo menu

El menú principal ahora muestra:

**Nuevo slot 33: "Estado del Stream"**
- Icono verde si estás online → "XP Normal"
- Icono gris si estás offline → "XP Reducido (x0.3)"
- Información clara del estado actual

---

## ❓ FAQ ACTUALIZADO

**P: ¿Los jugadores pueden farmear mientras yo duermo?**
R: Sí, pero solo ganarán el 30% del XP normal. Les tomará 3 veces más tiempo.

**P: ¿Es muy complicado el sistema?**
R: No, es muy simple:
   - Tú online = XP normal
   - Tú offline = XP reducido
   Nada más.

**P: ¿Qué pasó con los multiplicadores x3, x5, x15?**
R: Eliminados. Los eventos ya proporcionan suficiente dinamismo. El foco ahora es: jugar contigo = progreso normal, sin ti = lento.

**P: ¿Por qué eliminar el cuarzo?**
R: Evita farmeo excesivo en el Nether. Ahora el progreso se centra en misiones y combate.

---

## 📝 CAMBIOS v2.0

### Eliminado:
- ❌ Multiplicador x3 cuando online
- ❌ Multiplicador x2.5 en misiones
- ❌ Bonus acumulativo por horas
- ❌ XP por minar cuarzo
- ❌ Multiplicadores de fin de semana/noche

### Simplificado:
- ✅ Online = 1.0x (normal)
- ✅ Offline = 0.3x (reducido)
- ✅ Slot en /avo menu con estado visible
- ✅ Notificaciones claras

---

## 🎉 RESULTADO ESPERADO v2.0

**Antes:**
- Sistema complejo con muchos multiplicadores
- XP x15 durante eventos (demasiado)

**Ahora:**
- Sistema simple y claro
- Tu presencia importa pero no es excesivo
- Jugar contigo = normal
- Jugar sin ti = lento pero no imposible
- Los eventos ya dan dinamismo suficiente

---

## 🎁 CARACTERÍSTICAS PRINCIPALES

### 1. 📺 DROPS ESPECIALES DE STREAM

**Token de Stream** ⭐
- Drop de mobs hostiles (5% chance)
- Se canjean por recompensas épicas
- Solo dropea cuando streameas

**Fragmento del Stream** 💎
- Drop común (10% chance)
- 9 fragmentos = 1 bloque de esmeralda

**Sistema de Canjeo:**
```
5 tokens  = Kit Diamante completo
8 tokens  = 3 Bloques de Protección
10 tokens = Élitro + 64 cohetes
15 tokens = Kit Netherite completo
25 tokens = MEGA PACK ÉPICO
```

### 2. 🎯 MISIONES EXCLUSIVAS DE STREAM

**+3 misiones extra** solo disponibles durante stream:
- **Cacería Épica**: Matar 50 mobs → 500 XP + 5 diamantes
- **Minero Legendario**: Minar 100 minerales raros → 800 XP + 3 netherite
- **Constructor Dedicado**: Colocar 500 bloques → 400 XP + 2 bloques de diamante

**Todas dan x2 XP** comparado con misiones normales.

### 3. 🎊 EVENTOS AUTOMÁTICOS (cada 30 min)

**Lluvia de Recursos** (30% chance)
- Todos reciben: 3 diamantes, 8 oro, 16 hierro, 2 esmeraldas
- Instantáneo

**Doble Drops** (25% chance)
- x2 drops de mobs durante 10 minutos
- Farmeo intensivo

**Boss del Stream** (20% chance)
- Wither épico con x3 HP y x1.5 daño
- Recompensas: 2 estrellas del nether, 5 netherite, 3 manzanas encantadas, 1000 XP
- Ubicación anunciada globalmente

**Mega Experiencia** (25% chance)
- x5 XP durante 15 minutos
- Se stackea con el x3 base = **x15 XP TOTAL**

### 4. 🏆 RANKING SEMANAL DE STREAM

**Top 3 jugadores más activos durante tus streams:**

**#1**: 3 bloques netherite + 10 manzanas encantadas + 5 protecciones + 2000 XP
**#2**: 2 bloques netherite + 6 manzanas encantadas + 3 protecciones + 1500 XP  
**#3**: 1 bloque netherite + 4 manzanas encantadas + 2 protecciones + 1000 XP

Se resetea cada domingo a medianoche.

### 5. 💬 NOTIFICACIONES ÉPICAS

**Cuando te conectas:**
```
Título pantalla: ⭐ STREAM INICIADO
Subtítulo: ¡XP x3 ACTIVO!
Chat: [STREAM] ¡El streamer está ONLINE! +200% XP ACTIVO
```

**Cuando te desconectas:**
```
Chat: [STREAM] El streamer se desconectó. XP reducido a 30%
```

**Recordatorios cada 20 min:**
```
[STREAM] ¡Recuerda que estás ganando x3 XP mientras el streamer está online!
```

---

## ⚙️ CONFIGURACIÓN

### Editar tu username:

**Archivo:** `recompensas.yml`
```yaml
presencia_streamer:
  streamer_username: "Riolu"  # ← CAMBIA ESTO
```

**Archivo:** `stream_features.yml`
```yaml
streamer:
  username: "Riolu"  # ← CAMBIA ESTO
```

### Ajustar multiplicadores:

```yaml
# Cuando estás ONLINE
multiplicador_online: 3.0      # x3 XP (puedes subir a 4.0 o 5.0)
multiplicador_offline: 0.3     # x0.3 cuando offline (puedes bajar a 0.2 o 0.1)

# Bonus acumulativo
bonus_por_hora: 0.1            # +10% por hora (puedes subir a 0.15 = +15%)
maximo_horas: 5                # Máximo 5 horas (puedes subir a 10)
```

---

## 📈 MATEMÁTICAS DEL SISTEMA

### Ejemplo: Jugador que juega 2 horas

**CON EL STREAMER:**
```
Base: 600 XP
x3.0 (streamer online)     = 1,800 XP
+20% (2 horas acumuladas)  = 2,160 XP
Evento x5 (15 min)         = +extra ~540 XP
──────────────────────────────────
TOTAL: ~2,700 XP en 2 horas
```

**SIN EL STREAMER:**
```
Base: 600 XP
x0.3 (streamer offline)    = 180 XP
──────────────────────────────────
TOTAL: ~180 XP en 2 horas
```

**Diferencia:** Jugar contigo vale **15 veces más** que jugar sin ti.

---

## 🎮 COMANDOS NUEVOS

### Para jugadores:
```
/avo canjear                    - Ver tienda de tokens
/avo ranking stream             - Ver top jugadores del stream
/avo tokens                     - Ver tus tokens de stream
```

### Para admin:
```
/avo stream toggle              - Activar/desactivar modo stream manualmente
/avo stream evento <tipo>       - Lanzar evento específico
/avo stream tokens <player> <cantidad>  - Dar tokens
```

---

## 🚀 CÓMO USARLO

1. **Antes de tu stream:** Todo normal, XP reducido para todos
2. **Inicias stream:** Te conectas al server
3. **Automático:** 
   - Título épico en pantalla de todos
   - XP x3 activado
   - Eventos cada 30 min
   - Drops especiales enabled
4. **Durante stream:**
   - Jugadores grindean MUCHO más rápido
   - Tokens dropean de mobs
   - Eventos sorpresa cada rato
   - Misiones extra disponibles
5. **Terminas stream:** Te desconectas
6. **Automático:**
   - XP vuelve a x0.3
   - Jugadores notificados
   - Sin eventos ni drops

---

## 💡 TIPS ADICIONALES

### Para maximizar engagement:

1. **Anuncia en Discord:** "Stream en 1 hora, ¡prepárense para XP x3!"
2. **Mantén eventos visibles:** Los jugadores verán los eventos automáticos
3. **Interactúa con drops:** Cuando alguien saque un token, felicítalo en stream
4. **Crea hype del ranking:** "Veamos quién es #1 esta semana"
5. **Eventos manuales:** Lanza Boss del Stream cuando haya más gente

### Para balancear más:

Si aún avanzan rápido, puedes:
- Bajar `multiplicador_offline` a **0.2** (80% menos XP sin ti)
- Bajar `multiplicador_offline` a **0.1** (90% menos XP sin ti)
- Subir costos de tokens en `stream_features.yml`

---

## 📝 NOTAS TÉCNICAS

### Archivos modificados:
- ✅ `recompensas.yml` - Sistema de presencia del streamer
- ✅ `stream_features.yml` - Eventos, drops, misiones, canje

### Por implementar (necesitas código Java):
- StreamPresenceManager.java - Detectar conexión/desconexión del streamer
- StreamDropListener.java - Drops de tokens
- StreamEventScheduler.java - Eventos automáticos cada 30 min
- StreamMissionProvider.java - Misiones extra durante stream
- StreamTokenManager.java - Sistema de canje de tokens
- StreamRankingSystem.java - Ranking semanal

### Prioridad de implementación:
1. **Fase 1 (crítico):** StreamPresenceManager + multiplicadores XP
2. **Fase 2 (importante):** StreamDropListener + tokens
3. **Fase 3 (nice to have):** Eventos automáticos + ranking

---

## ❓ FAQ

**P: ¿Los jugadores pueden farmear mientras yo duermo?**
R: Sí, pero solo ganarán el 30% del XP normal (x0.3). Les tomará 3-4 veces más tiempo.

**P: ¿Qué pasa si me desconecto en medio del stream?**
R: El sistema detecta automáticamente y cambia a modo offline. Los eventos activos terminan su duración.

**P: ¿Puedo tener múltiples streamers?**
R: Sí, puedes agregar una lista de usernames y que cualquiera active el modo.

**P: ¿Los eventos se stackean?**
R: Sí! Durante "Mega Experiencia" tendrías: x3 (streamer) × x5 (evento) = **x15 XP total**.

**P: ¿Cuántos tokens se necesitan en promedio?**
R: Con 5% chance en mobs hostiles, matar ~100 mobs = ~5 tokens. En 1 hora activa de grind = 10-15 tokens.

---

## 🎉 RESULTADO ESPERADO

**Antes:**
- Jugadores grindeaban todo el día sin ti
- Avanzaban muy rápido 24/7
- Tu presencia no importaba mucho

**Después:**
- Jugar contigo vale **15x más** que sin ti
- Avanzar sin ti es **lento y aburrido**
- Tu conexión genera **hype y excitement**
- Los jugadores **esperarán tus streams**
- Discord/chat explotará cuando te conectes

---

## 📞 SOPORTE

Si necesitas ajustar algo:
1. Edita `recompensas.yml` para multiplicadores
2. Edita `stream_features.yml` para eventos/drops/tokens
3. Reinicia el server o `/avo reload`

¡Disfruta tu server con contenido centrado en ti! 🚀
