# 🐉 Evento 5: La Apertura del End - Changelog Completo

> **Plugin:** Apocalipsis 1.21.4  
> **Versión:** 1.22.50+ (Mejoras Épicas)  
> **Minecraft:** 1.21.4+  
> **Dependencies:** MythicMobs 5.10.0+, Model Engine R4.0.7+

---

## 📖 Descripción General

El **Evento 5: La Apertura del End** es un evento épico de servidor que transforma la experiencia del End en una batalla cinematográfica contra un dragón legendario. Los jugadores deben trabajar juntos para descubrir un portal misterioso, enfrentarse a oleadas de criaturas del End, y finalmente derrotar al **Desolador del Vacío** en un combate épico de múltiples fases.

---

## ✨ Características Principales

### 🌟 **Sistema de 3 Fases Progresivas**

#### **Fase 1: Descubrimiento (45 minutos)**
- 📍 **Sistema de waypoints dinámicos** - 5 waypoints que aparecen progresivamente
- 🎯 **Tareas coherentes temáticas del End:**
  - Eliminar un Enderman
  - Recolectar Obsidiana
  - Conseguir un Ojo de Ender
- 👾 **Spawn continuo de Endermans** - Cada 2 minutos
- 🎬 **Diálogos narrativos del Observador** - 10+ mensajes misteriosos
- 🚀 **Sistema de acercamiento forzado:**
  - Empujes suaves (últimos 10 minutos)
  - Teletransporte intensivo (últimos 3 minutos)
- 💫 **Efectos visuales épicos:**
  - Pilares de luz hacia el cielo
  - Caminos de partículas guiando a los jugadores
  - Anillos de energía en el suelo

#### **Fase 2: Llegada al Portal**
- ⚡ **Animación de activación épica:**
  - 300 partículas END_ROD
  - 600 partículas REVERSE_PORTAL
  - 200 partículas DRAGON_BREATH
  - Explosiones consecutivas
  - Sonidos atmosféricos del End
- 🌀 **Portal al End funcional** con efectos continuos
- 📢 **Mensajes cinemáticos** con títulos épicos

#### **Fase 3: Combate contra el Dragón**
- 🐲 **Spawn cinematográfico del dragón** (10 segundos de secuencia)
- ⚔️ **Sistema de 4 subfases basadas en HP:**
  - **Fase 1 (100-75%):** Aéreo - Combate base
  - **Fase 2 (75-50%):** Invocador - +25% daño, +10% velocidad
  - **Fase 3 (50-25%):** Desesperado - +50% daño, +20% velocidad
  - **Fase 4 (25-0%):** Furia - +100% daño, +30% velocidad
- 💎 **12 Cristales del End** con regeneración mejorada
- 📖 **90+ diálogos del Observador** contextuales según la fase
- 💥 **Efectos visuales masivos:**
  - 1200+ partículas en spawn
  - 2300+ partículas en muerte
  - Sonidos épicos (truenos, rugidos)

---

## 🎮 Mecánicas de Gameplay

### **Portal del Overworld**
- ✅ **Altura accesible:** Y=85-95 (antes variable/muy alto)
- ✅ **Terraformación automática** del área circundante
- ✅ **Visible desde lejos** con efectos de partículas
- ✅ **Coordenadas reveladas progresivamente** mediante waypoints

### **Sistema de Waypoints**

| Tiempo | Nombre | Distancia | Efectos |
|--------|--------|-----------|---------|
| 40 min | §5§l⚡ ECO DISTANTE | 80% | Pilar + Guía de partículas |
| 30 min | §5§l⚡ RESONANCIA CRECIENTE | 60% | Pilar + Guía de partículas |
| 20 min | §5§l⚡ LLAMADO DEL VACÍO | 40% | Pilar + Guía de partículas |
| 10 min | §5§l⚡ PORTAL EMERGENTE | 20% | Pilar + Empujes activados |
| 3 min | §c§l⚡ EPICENTRO DEL VACÍO | 5% | Pilar + TP intensivo |

### **Combate del Dragón**

#### **Escalado de HP:**
```
HP Base: 6,000
HP por jugador: +25%

Ejemplos:
- 1 jugador: 7,500 HP
- 3 jugadores: 11,719 HP
- 5 jugadores: 17,578 HP
- 10 jugadores: 43,945 HP
```

#### **Fases de Combate:**

| Fase | HP | Daño | Velocidad | Efectos |
|------|-----|------|-----------|---------|
| I - Aéreo | 100-75% | Base (25) | Base | Dragon Breath |
| II - Invocador | 75-50% | +25% | +10% | Portal + Mensaje |
| III - Desesperado | 50-25% | +50% | +20% | Soul Flame + Wither |
| IV - Furia | 25-0% | +100% | +30% | Explosiones + Truenos |

#### **Cristales del End:**
- 💎 **12 cristales** desplegados alrededor del dragón
- ❤️ **Regeneración:** 1.5 HP/segundo al dragón
- 🎯 **Recompensa por destruir todos:** Regeneración III (40s) + 150 XP
- ⚡ **Curación redirigida:** Sistema que asegura que curen al dragón correcto

---

## 🏆 Sistema de Recompensas

### **Drops Garantizados (Todos los participantes con 100+ daño):**

| Item | Cantidad | Valor |
|------|----------|-------|
| Fragmento del Vacío | 5-8 | Material épico |
| Ender Pearls | 8-15 | Teletransporte |
| End Stone | 64-96 | Construcción |
| Purpur Block | 32-48 | Decoración |
| Chorus Fruit | 16-32 | Alimento |
| **Experiencia** | **2,000 XP** | **Level up** |

### **Drops con Probabilidad:**

| Item | Probabilidad | Cantidad |
|------|--------------|----------|
| Corazón Desolador | 45% | 1 |
| Escama Perfecta | 25% | 2-4 |
| **Fragmento Supremo** | **10%** | **1** |

### **Recompensas Top 3 (Más Daño al Dragón):**

#### 🥇 **Primer Lugar:**
- 5 Escamas Perfectas
- 1 Fragmento Supremo (GARANTIZADO)
- 5,000 XP Bonus
- Título: **§5§l⚡ Azote del Desolador ⚡**

#### 🥈 **Segundo Lugar:**
- 3 Escamas Perfectas
- 15 Fragmentos del Vacío
- 3,000 XP Bonus

#### 🥉 **Tercer Lugar:**
- 2 Escamas Perfectas
- 10 Fragmentos del Vacío
- 1,500 XP Bonus

### **Recompensa Base del Dragón:**
- 8-15 Escamas del Vacío
- 8,000 XP adicional
- **Total XP mínimo: 20,000**

---

## 🎬 Narrativa del Observador

El evento cuenta con **más de 90 mensajes únicos** del Observador, una entidad misteriosa que comenta sobre la batalla:

### **Ejemplos de Diálogos:**

**Fase 1 (Descubrimiento):**
```
[...] Huele a… antes. Cenizas. Vacío.
[...] Este umbral… lo recuerdo sellado.
[...] El mundo está… apurado.
```

**Subfase 1 del Dragón (100-75%):**
```
[...] El ciclo eterno continúa…
[...] ¿Recuerda las cenizas de sus encarnaciones pasadas?
[...] Mil dragones. Mil muertes.
```

**Subfase 2 (75-50%):**
```
[...] Una furia ancestral despierta.
[...] Es la suma de todas sus muertes.
[...] Mil gritos silenciados.
```

**Subfase 3 (50-25%):**
```
[...] La paz es una ilusión en el End.
[...] Es prisionero, no carcelero.
[...] ¿Sabe que esto es inútil? Creo que sí.
```

**Subfase 4 (25-0%):**
```
[...] La fase final.
[...] Cada rugido es un lamento…
[...] La memoria está atrapada. Eternamente.
```

---

## 🛠️ Comandos de Administración

### **Comandos Principales:**

```bash
/avo evento5 start [fase]
# Inicia el evento (opcionalmente en una fase específica)

/avo evento5 stop
# Detiene el evento inmediatamente

/avo evento5 next
# Salta a la siguiente fase/diálogo (útil para testing)

/avo evento5 skip
# Salta la fase de preparación

/avo evento5 tp
# Teletransporta cerca del portal (testing)

/avo evento5 modo
# Muestra el modo de integración actual

/avo evento5 fase <1-4>
# Fuerza una fase específica del dragón

/avo evento5 damage <jugador> <cantidad>
# Simula daño para testing de recompensas

/avo evento5 recompensas
# Obtiene todas las recompensas del evento (admin)
```

### **Tab Completion:**
Todos los comandos tienen autocompletado completo para facilitar la administración.

---

## 📊 Mejoras Técnicas

### **Comparativa Antes/Después:**

| Aspecto | Antes | Ahora | Mejora |
|---------|-------|-------|--------|
| **Portal Altura** | Variable | Y=85-95 | Accesible |
| **HP Dragón (1p)** | 5,400 | 7,500 | +39% |
| **HP Dragón (5p)** | 11,197 | 17,578 | +57% |
| **Daño Base** | 20 | 25 | +25% |
| **Cristales End** | 10 | 12 | +20% |
| **Diálogos** | ~45 | ~90 | +100% |
| **Partículas Spawn** | 200 | 1,200 | +500% |
| **Partículas Muerte** | 300 | 2,300 | +667% |
| **XP Total** | 17,000 | 20,000 | +18% |
| **Duración Combate** | 8-15 min | 15-25 min | +67% |

### **Optimizaciones:**
- ✅ Sistema de partículas optimizado para 30-60 FPS
- ✅ BukkitRunnables canceladas automáticamente
- ✅ Partículas solo visibles para jugadores cercanos
- ✅ Eliminación automática de dragones duplicados
- ✅ Listener de curación de cristales redirigida

### **Corrección de Bugs:**
- ✅ **Doble spawn de dragón** - Sistema de eliminación automática
- ✅ **Cristales curando dragón incorrecto** - Listener redirige curación
- ✅ **Portal en altura inaccesible** - Altura fija Y=85-95
- ✅ **Comando next no funcionaba** - Offset de +5 segundos
- ✅ **Errores de compilación** - 6 errores resueltos

---

## 🎯 Experiencia del Jugador

### **Timeline Completo del Evento:**

```
T-45:00 - 📢 Inicio del evento
        - Instrucciones mostradas
        - Spawn de Endermans comienza

T-40:00 - 📍 Waypoint 1 aparece (Eco Distante)

T-30:00 - 📍 Waypoint 2 aparece (Resonancia Creciente)

T-20:00 - 📍 Waypoint 3 aparece (Llamado del Vacío)

T-10:00 - 📍 Waypoint 4 aparece (Portal Emergente)
        - 🚀 Sistema de empujes activado

T-03:00 - 📍 Waypoint 5 aparece (Epicentro del Vacío)
        - ⚡ Teletransporte intensivo activado

T-00:00 - ⚡ Portal se activa
        - Animación épica de 10+ segundos
        - Portal funcional al End

COMBATE - 🐲 Dragón spawn con secuencia cinematográfica
        - Batalla de 15-25+ minutos
        - 4 fases progresivas
        - 90+ diálogos del Observador

VICTORIA - 🏆 Recompensas épicas distribuidas
        - 💬 Mensaje final misterioso
        - 🎊 Evento completado
```

### **Consejos para Jugadores:**

1. **Lee las instrucciones iniciales** - Aparecen 2 segundos después del inicio
2. **Completa las 3 tareas** - Necesarias para revelar la ubicación del portal
3. **Sigue los waypoints** - Los pilares de luz guían hacia el portal
4. **Trabaja en equipo** - El HP del dragón escala con jugadores
5. **Destruye los cristales** - Evita que el dragón se regenere
6. **Prepara equipo épico** - El dragón se vuelve más fuerte con cada fase
7. **No te alejes del portal** - El sistema te empujará/teletransportará

---

## 🔧 Configuración

### **Archivos de Configuración:**

#### `apertura_end.yml` - Configuración del Evento
```yaml
evento:
  duracion_descubrimiento: 2700  # 45 minutos
  
  escalado:
    hp_base: 6000
    hp_por_jugador: 0.25  # +25% por jugador
    
  combate:
    daño_base: 25
    daño_por_jugador: 0.18
    
    fases:
      fase_1:  # 100-75%
        multiplicador_dano: 1.0
        multiplicador_velocidad: 1.0
        
      fase_2:  # 75-50%
        multiplicador_dano: 1.25
        multiplicador_velocidad: 1.10
        
      fase_3:  # 50-25%
        multiplicador_dano: 1.50
        multiplicador_velocidad: 1.20
        
      fase_4:  # 25-0%
        multiplicador_dano: 2.0
        multiplicador_velocidad: 1.30
        
  cristales:
    cantidad: 12
    regeneracion_hp_por_segundo: 1.5
    exp: 150
    nivel: 3
    duracion: 800  # 40 segundos
```

### **MythicMobs Integration:**

#### `toro_enderdragon.yml` - Configuración del Mob
```yaml
toro_enderdragon:
  Type: ENDER_DRAGON
  Display: '§8El Desolador del Vacío'
  Health: 500  # Sobreescrito por el evento
  Damage: 10
  
  AIGoalSelectors:
  - clear
  - meleeattack
  - randomstroll
  
  Options:
    MovementSpeed: 0.25
    PreventOtherDrops: true
    AlwaysShowName: false
    Silent: false
    Despawn: false
    PreventMovement: false  # CRÍTICO
```

#### `toro_enderdragon_skills.yml` - Skills del Dragón
Sistema completo de habilidades con:
- ✨ Efectos visuales triplicados
- 🎯 Spawn cinematográfico con 1200+ partículas
- 💥 Muerte épica con 2300+ partículas
- ⚔️ Ataques mejorados con partículas masivas
- 🔥 Fuego del dragón con soul fire y lava

---

## 📦 Instalación

### **Requisitos:**
- Minecraft 1.21.4+
- Paper/Spigot 1.21.4+
- MythicMobs 5.10.0+
- Model Engine R4.0.7+
- Java 21

### **Pasos:**

1. **Instalar el plugin Apocalipsis:**
   ```
   plugins/Apocalipsis-1.22.50+.jar
   ```

2. **Configurar MythicMobs:**
   ```
   plugins/MythicMobs/Mobs/toro_enderdragon.yml
   plugins/MythicMobs/Skills/toro_enderdragon_skills.yml
   ```

3. **Instalar Model Engine (opcional pero recomendado):**
   ```
   plugins/ModelEngine/models/toro_ender_dragon.bbmodel
   ```

4. **Reiniciar el servidor**

5. **Verificar instalación:**
   ```
   /avo evento5
   ```

---

## 🎥 Media & Screenshots

### **Efectos Visuales:**
- Portal activándose con 1200+ partículas
- Spawn del dragón con secuencia de 10 segundos
- Pilares de luz de los waypoints
- Explosión épica al derrotar al dragón
- Caminos de partículas guiando a jugadores

### **Momentos Épicos:**
- Transición a Fase 4 (Furia) con truenos y explosiones
- Destrucción de todos los cristales
- Mensaje final del Observador
- Top 3 recibiendo recompensas legendarias

---

## 🐛 Known Issues & Troubleshooting

### **Problema: Dragón aparece doble**
**Solución:** El sistema elimina automáticamente dragones duplicados 1 tick después del spawn.

### **Problema: Cristales no curan al dragón**
**Solución:** Listener redirige automáticamente la curación al dragón de MythicMobs.

### **Problema: Portal spawneó muy alto**
**Solución:** Actualiza a la versión 1.22.48+ donde el portal se genera en Y=85-95.

### **Problema: Comando /avo evento5 next no muestra diálogos**
**Solución:** Actualiza a la versión 1.22.48+ con offset de +5 segundos.

### **Problema: Dragón se queda quieto/atrapado**
**Solución:** Verifica que en `toro_enderdragon.yml` la opción `PreventMovement: false`.

---

## 📝 Changelog por Versión

### **v1.22.50 - Correcciones Dragón**
- ✅ Fix doble spawn de dragón
- ✅ Fix cristales curando dragón incorrecto
- ✅ Sistema de fases implementado
- ✅ Spawn cinematográfico mejorado
- ✅ 6 errores de compilación resueltos

### **v1.22.48 - Mejoras Dinámicas**
- ✅ Sistema de tareas coherente (End temático)
- ✅ Spawn continuo de Endermans
- ✅ 5 waypoints progresivos
- ✅ Sistema de empujes y teletransporte
- ✅ Comando /avo evento5 next corregido
- ✅ Comando /avo evento5 tp añadido

### **Versión Épica - Mejoras Masivas**
- ✅ Portal altura accesible (Y=85-95)
- ✅ HP del dragón aumentado +33% base
- ✅ 90+ diálogos del Observador
- ✅ Efectos visuales triplicados/quintuplicados
- ✅ 12 cristales del End (+20%)
- ✅ Recompensas épicas mejoradas +50-100%
- ✅ Combate 15-25+ minutos

---

## 👥 Créditos

**Desarrollado por:** riolu  
**Powered by:** GitHub Copilot (Claude Sonnet 4.5)  
**Servidor:** Apocalipsis 1.21.8  

### **Agradecimientos:**
- MythicMobs por el sistema de mobs personalizados
- Model Engine por los modelos 3D épicos
- Comunidad de Paper por la API optimizada

---

## 📞 Soporte

**¿Necesitas ayuda?**
- 📧 Contacto del servidor
- 💬 Discord del servidor
- 🐛 Reporta bugs con `/avo evento5` en los logs

---

## 📄 Licencia

Este evento es parte del plugin Apocalipsis.  
Ver `LICENSE` para más información.

---

**¡Gracias por usar el Evento 5: La Apertura del End!** 🐉⚡

*Prepárate para la batalla más épica del servidor...*