# 🌍 EVENTO 6: CUANDO EL MUNDO DECIDE OLVIDAR

## 📖 Descripción General

**Cuando el Mundo Decide Olvidar** es un evento narrativo cinematográfico que utiliza el **Sistema de Ciclos** para crear una experiencia de "reinicio del mundo" en vivo durante el stream.

### 🎯 Concepto Central

> *"El mundo hace esto cuando se cansa. Reiniciar es más fácil que cambiar."*

El evento simula que el mundo "decide" reiniciarse, borrando todas las construcciones y progreso material, pero manteniendo los recuerdos (rangos, skills, misiones) de los jugadores.

---

## 🎬 Estructura Narrativa - 10 Actos

### **ACTO 1: NORMALIDAD** (0:00 - 15:00)
- **Estado:** Todo funciona con normalidad
- **Observador:** Silencio casi total (solo "..." a los 5 minutos)
- **Gameplay:** Los jugadores juegan como siempre
- **Propósito:** Establecer la calma antes de la tormenta

---

### **ACTO 2: LAS PRIMERAS RAREZAS** (15:00 - 30:00)
- **Estado:** Detalles sutiles y extraños empiezan a aparecer
- **Efectos:**
  - Truenos sin lluvia
  - Cambios momentáneos del cielo
  - Sonidos lejanos inexplicables
  - Mobs que se quedan quietos mirando
- **Observador:** Mensajes muy espaciados
  - "Hmm..."
  - "No todavía..."
- **Propósito:** Crear inquietud sin revelar nada

---

### **ACTO 3: INESTABILIDAD** (30:00 - 50:00)
- **Estado:** Lo extraño ya no puede ignorarse
- **Efectos constantes:**
  - Partículas de ceniza en el aire
  - "Lag" simulado (freezes breves)
  - Relojes que se detienen momentáneamente
  - Portales del Nether que suenan sin activarse
- **Observador:** 
  - "Otra vez no..." (título)
- **Propósito:** Construir tensión creciente

---

### **ACTO 4: EL QUIEBRE** (50:00 - 55:00) ⚡
- **Estado:** Todo se rompe RÁPIDO
- **Secuencia automática (5 minutos):**
  1. **Ceguera** → Pantalla negra (5 segundos)
  2. **Sonido profundo** → Wither spawn pitch bajo
  3. **Vibración** → 1000 partículas eléctricas
  4. **Congelación** → Mundo se detiene (3 segundos)
  5. **Mensaje final:** *"El mundo ya tomó la decisión."*
  6. **Silencio** → 20 segundos de silencio total
- **Observador:** Mensaje final antes del corte
- **Propósito:** Clímax dramático

---

### **ACTO 5: EL REINICIO** (55:00 - 56:00) 🔄
- **Estado:** TRANSICIÓN - El mundo se reinicia usando el Sistema de Ciclos
- **Secuencia automática (1 minuto):**
  1. Pantalla negra (ceguera total 4 segundos)
  2. **Sistema de Ciclos crea mundo nuevo** (`world_ciclo_reset`)
  3. **Todos los jugadores son teleportados** al nuevo mundo
  4. Efectos de spawn (partículas END_ROD)
- **Tecnología:** Usa `CicloManager` para crear ciclo automáticamente
- **Reseteo:**
  - ✅ Inventario → Perdido (guardado en mundo anterior)
  - ✅ Construcciones → Mundo nuevo vacío
  - ✅ XP → Reseteado a nivel 1
  - ❌ Rangos → Se mantienen
  - ❌ Skills → Se mantienen
  - ❌ Misiones → Se mantienen
- **Items iniciales:** 16 madera + 8 pan
- **Propósito:** Ejecutar el reinicio real

---

### **ACTO 6: NUEVO MUNDO** (56:00 - 70:00) 🌱
- **Estado:** Todos aparecen juntos en el spawn del nuevo mundo
- **Realidad:**
  - Sin items
  - Sin armadura
  - Sin casas
  - Sin progresos materiales
  - **PERO:** Todos recuerdan todo (memoria intacta)
- **Observador:**
  - *"No los borró..."*
  - *"Solo borró el lugar."*
- **Propósito:** Revelación de lo que sucedió

---

### **ACTO 7: COMPRENSIÓN LENTA** (70:00 - 85:00) 🧠
- **Estado:** Los jugadores exploran y empiezan a entender
- **Observador:**
  - *"El mundo hace esto cuando se cansa."*
  - *"Reiniciar es más fácil que cambiar."*
- **Gameplay:** Exploración, recolección inicial, comprensión
- **Propósito:** Asimilación de la realidad

---

### **ACTO 8: LA FRACTURA** (85:00 - 95:00) 🔥
- **Estado:** Descubren que el **Nether NO se reseteó**
- **Detalle inquietante:**
  - El Nether sigue accesible
  - Contiene las construcciones anteriores
  - Se siente "distinto" (más pesado, opresivo)
  - Efectos especiales: partículas SOUL, sonidos inquietantes
- **Observador:**
  - *"Lo que está debajo..."*
  - *"...no olvida tan fácil."*
- **Propósito:** Plantar el Nether como problema futuro

---

### **ACTO 9: EL END PERMANECE** (95:00 - 105:00) 🌑
- **Estado:** El **End tampoco se reseteó**
- **Mensaje clave:**
  - *"Algunos lugares no se reinician."*
  - *"Solo observan."*
- **Efectos si entran al End:**
  - Partículas PORTAL
  - Sonido de Enderman mirando
  - Sensación de vigilancia constante
- **Propósito:** Misterio del End como observador eterno

---

### **ACTO 10: CIERRE DEL STREAM** (105:00 - 120:00) 🎭
- **Estado:** Los jugadores ya están asentándose
- **Observador - Mensaje final:**
  - *"Este no es un comienzo."*
  - *"Es una repetición."*
  - Silencio.
- **Recompensas finales:**
  - Fragmento de Memoria
  - Cicatriz Temporal
  - Eco de la Repetición
  - PS de participación
- **Propósito:** Cierre impactante y enigmático

---

## 🔧 Integración con Sistema de Ciclos

### Uso del Protocolo de Ciclos

El evento **requiere** que el sistema de ciclos esté activado (`ciclos.yml` → `enabled: true`).

#### Flujo de Integración:

```
ACTO 4: EL QUIEBRE
    ↓
ACTO 5: EL REINICIO
    ↓
[Sistema de Ciclos Activado]
    ↓
1. CicloManager.crearCicloNuevo("world_ciclo_reset")
    ↓
2. Multiverse-Core genera mundo nuevo (NORMAL, HARD)
    ↓
3. WorldDataManager guarda datos del mundo anterior
    ↓
4. WorldInventoryManager guarda inventarios
    ↓
5. Reseteo según configuración:
   - Inventario → VACÍO (guardado)
   - XP → 0
   - Construcciones → Mundo nuevo vacío
    ↓
6. CicloManager.teleportarTodos("world_ciclo_reset")
    ↓
7. Items iniciales entregados
    ↓
ACTO 6: NUEVO MUNDO
```

### Separación de Mundos

| Dimensión | ¿Se resetea? | Estado Post-Evento |
|-----------|-------------|-------------------|
| **Overworld** | ✅ SÍ | Mundo completamente nuevo |
| **Nether** | ❌ NO | Mantiene construcciones originales |
| **End** | ❌ NO | Mantiene estado original |

**Razón narrativa:**
- El **Overworld** se "olvida" porque es donde habitan
- El **Nether** "no olvida" porque es un lugar de memoria antigua
- El **End** "solo observa" porque es atemporal

---

## 🎁 Sistema de Recompensas

### PS por Participación

```
Base de Presencia:        100 PS
Por Acto Completo (x10):  200 PS (20 PS cada uno)
Bonus Comprensión:         50 PS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL MÁXIMO:             350 PS
```

### Items Coleccionables

#### 1. **Fragmento de Memoria** (Echo Shard)
- **Lore:** *"El mundo olvidó el lugar. Pero ustedes recuerdan todo."*
- **Obtención:** Todos al finalizar
- **Simbolismo:** Memoria persistente

#### 2. **Cicatriz Temporal** (Netherite Scrap)
- **Lore:** *"Lo que está debajo no olvida. El Nether permanece intacto."*
- **Obtención:** Todos al finalizar
- **Simbolismo:** Permanencia del Nether

#### 3. **Eco de la Repetición** (Recovery Compass) ✦
- **Lore:** *"Este no es un comienzo. Es una repetición."*
- **Obtención:** Todos al finalizar
- **Simbolismo:** Emblema de evento completado

### Items Iniciales (Post-Reinicio)

- **16x Madera de Roble** → "Resto del Anterior"
- **8x Pan** → "Pan Persistente"

---

## 🎮 Comandos del Evento

### Iniciar Evento
```bash
/avo evento6 start
```
**Requiere:** Permisos de admin

### Cancelar Evento (Emergencia)
```bash
/avo evento6 stop
```
**Efecto:** Detiene el evento, NO revierte el mundo

### Ver Progreso
```bash
/avo evento6 info
```
**Muestra:**
- Acto actual
- Tiempo restante
- Jugadores participantes

### Saltar a Acto (Debug)
```bash
/avo evento6 acto <numero>
```
**Solo en debug mode**

---

## ⚙️ Configuración Técnica

### Requisitos Previos

1. **Sistema de Ciclos activado**
   - `ciclos.yml` → `enabled: true`
   - Multiverse-Core instalado

2. **Backup automático**
   - `evento6_mundo_olvidado.yml` → `backup_antes_reinicio: true`

3. **Permisos**
   - `apocalipsis.evento6.admin` → Iniciar evento
   - `apocalipsis.ciclo.admin` → Gestionar ciclos

### Duración Total

- **Tiempo estimado:** ~2 horas (120 minutos)
- **Flexible:** Se adapta al ritmo del stream

### Archivos Generados

```
plugins/Apocalipsis/data/
├── evento6_progreso.yml       # Progreso del evento
├── evento6.log                # Logs detallados
└── ciclos/
    └── world_ciclo_reset/
        └── jugadores/
            └── <UUID>.yml     # Datos de cada jugador
```

---

## 🎨 Efectos Especiales

### Partículas Utilizadas

| Acto | Partícula | Cantidad | Propósito |
|------|-----------|----------|-----------|
| 3 | ASH | 5/5seg | Inestabilidad |
| 4 | ELECTRIC_SPARK | 1000 | Quiebre |
| 5 | END_ROD | 200 | Spawn nuevo mundo |
| 8 | SOUL | 10/2seg | Nether pesado |
| 9 | PORTAL | 3/3seg | End observador |

### Sonidos Destacados

| Acto | Sonido | Propósito |
|------|--------|-----------|
| 4 | ENTITY_WARDEN_SONIC_BOOM | Ceguera inicial |
| 4 | ENTITY_WITHER_SPAWN (pitch 0.3) | Sonido profundo |
| 4 | ENTITY_ENDER_DRAGON_DEATH | Mensaje final |
| 5 | BLOCK_RESPAWN_ANCHOR_CHARGE | Spawn nuevo |
| 8 | AMBIENT_BASALT_DELTAS_MOOD | Opresión Nether |
| 9 | ENTITY_ENDERMAN_STARE | Vigilancia End |

---

## 📝 Notas para el Streamer

### Timing Recomendado

1. **Iniciar al principio del stream** (0:00)
2. **Los primeros 15 min:** Gameplay normal (calma)
3. **15-50 min:** Construcción de tensión (rarezas progresivas)
4. **50-56 min:** CLÍMAX (quiebre y reinicio) ⚡
5. **56-120 min:** Exploración y comprensión del nuevo mundo

### Interacción Sugerida

- **NO explicar nada** durante los primeros actos
- Dejar que el chat especule
- Reaccionar naturalmente a los efectos
- Descubrir junto con los jugadores
- Enfatizar el mensaje final: *"Es una repetición"*

### Consejos Narrativos

✅ **Hacer:**
- Mantener el misterio
- Dejar espacios de silencio
- Reaccionar a los efectos extraños
- Explorar el nuevo mundo con curiosidad

❌ **Evitar:**
- Explicar el evento antes de tiempo
- Romper la inmersión
- Saltarse actos importantes
- Revelar que el Nether/End no se resetearon hasta que alguien vaya

---

## 🔐 Seguridad y Backups

### Antes del Evento

1. **Backup automático** del mundo actual
2. **Verificación** de que el sistema de ciclos funciona
3. **Prueba en desarrollo** (opcional pero recomendado)

### Durante el Evento

- El sistema guarda progreso cada acto
- Los inventarios se guardan antes del reseteo
- Los datos de jugador se preservan según configuración

### Después del Evento

- Los jugadores pueden volver al mundo anterior con:
  ```bash
  /avo ciclo tp world
  ```
  (Solo admins - los jugadores normales quedan en el nuevo mundo)

---

## 🐛 Troubleshooting

### Problema: El evento no inicia
**Solución:** 
1. Verificar que `ciclos.yml` tiene `enabled: true`
2. Verificar que Multiverse-Core está instalado
3. Revisar permisos `apocalipsis.evento6.admin`

### Problema: El mundo no se resetea en el Acto 5
**Solución:**
1. Verificar logs en `evento6.log`
2. Comprobar que `CicloManager` está activo
3. Verificar espacio en disco para mundo nuevo

### Problema: Jugadores pierden rangos/skills
**Solución:**
1. Verificar `evento6_mundo_olvidado.yml`:
   ```yaml
   resetear_rangos: false
   resetear_skills: false
   ```
2. Restaurar desde backup si es necesario

### Problema: Nether/End se resetearon por error
**Solución:**
1. Configuración correcta:
   ```yaml
   mantener_nether_original: true
   mantener_end_original: true
   ```
2. Los mundos Nether/End NO deben tener prefijo del ciclo

---

## 🎯 Checklist Pre-Evento

- [ ] Sistema de Ciclos activado
- [ ] Multiverse-Core instalado y funcional
- [ ] Backup del mundo actual creado
- [ ] `evento6_mundo_olvidado.yml` configurado correctamente
- [ ] Permisos verificados
- [ ] Avisar a los jugadores que habrá un "evento especial"
- [ ] (Opcional) Prueba en servidor de desarrollo

---

## 📊 Métricas del Evento

### Participación

- Jugadores mínimos: 1
- Jugadores máximos: 30
- Entrada tardía: Permitida (no reciben items iniciales extra)

### Duración

- Mínimo recomendado: 90 minutos
- Duración configurada: 120 minutos
- Flexible según stream

### Resultados

Al final del evento:
- ✅ Mundo nuevo creado
- ✅ Jugadores reseteados (material)
- ✅ Memoria preservada (rangos, skills, misiones)
- ✅ Recompensas entregadas
- ✅ Nether y End intactos
- ✅ Narrativa completada

---

## 🌟 Temas Narrativos

### Filosofía del Evento

> **Reiniciar vs. Cambiar**
> 
> El mundo elige reiniciar porque es más fácil que adaptarse. Borra el lugar, pero no las personas. Es un comentario sobre ciclos, memoria y permanencia.

### Simbolismo

| Elemento | Simbolismo |
|----------|------------|
| **Overworld reseteado** | Lo superficial se pierde |
| **Nether intacto** | Lo profundo permanece |
| **End observando** | Lo eterno no cambia |
| **Memoria preservada** | La identidad persiste |
| **Repetición** | Los ciclos son inevitables |

### Mensaje Final

*"Este no es un comienzo. Es una repetición."*

Sugiere que esto ya ha pasado antes y volverá a pasar. El mundo tiene sus propios ciclos, independientes de los jugadores.

---

## 🎬 Inspiración y Referencias

Este evento está inspirado en:
- Narrativas de bucles temporales
- Reinicios cósmicos (multiverso)
- Filosofía del eterno retorno
- Estética minimalista de mensajes del Observador

---

**Creado:** 2026-01-26  
**Versión:** 1.0.0  
**Evento:** #6 - Cuando el Mundo Decide Olvidar  
**Sistema:** Protocolo de Ciclos
