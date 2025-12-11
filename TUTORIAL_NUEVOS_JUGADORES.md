# 🎮 Tutorial para Nuevos Jugadores - Apocalipsis Survival

## 📋 Propuestas para Sistema de Integración Suave

### 🛡️ **SISTEMA DE DIFICULTAD PROGRESIVA PARA NUEVOS**

#### Sistema de Escalado Temporal (Recomendado)
```yaml
dificultad_progresiva_nuevos:
  enabled: true
  
  # Esperar 5 minutos antes de iniciar tutorial (tiempo de registro)
  retraso_inicio_tutorial_minutos: 5
  
  # Dificultad gradual basada en tiempo jugado
  escalado_temporal:
    # Primeros 30 minutos: MUY FÁCIL (10% dificultad)
    fase_1:
      duracion_minutos: 30
      multiplicador_daño: 0.1           # 10% del daño normal
      multiplicador_frecuencia: 0.2     # 80% menos frecuentes
      permitir_knockback: false         # Sin empujones
      mensaje_fase: "&a🌱 Fase Tutorial - Desastres muy suaves"
      
    # 30-60 minutos: FÁCIL (25% dificultad)
    fase_2:
      duracion_minutos: 30
      multiplicador_daño: 0.25          # 25% del daño
      multiplicador_frecuencia: 0.4     # 60% menos frecuentes
      permitir_knockback: false
      mensaje_fase: "&e🔰 Fase Adaptación - Desastres suaves"
      
    # 1-2 horas: MODERADO (50% dificultad)
    fase_3:
      duracion_minutos: 60
      multiplicador_daño: 0.5           # 50% del daño
      multiplicador_frecuencia: 0.6     # 40% menos frecuentes
      permitir_knockback: true          # Ya puede empujar
      mensaje_fase: "&6⚡ Fase Intermedia - Desastres moderados"
      
    # 2-4 horas: NORMAL (75% dificultad)
    fase_4:
      duracion_minutos: 120
      multiplicador_daño: 0.75          # 75% del daño
      multiplicador_frecuencia: 0.8     # 20% menos frecuentes
      permitir_knockback: true
      mensaje_fase: "&c⚔️ Fase Avanzada - Desastres fuertes"
      
    # 4+ horas: DIFICULTAD GLOBAL (100% dificultad)
    fase_final:
      multiplicador_daño: 1.0           # 100% dificultad global
      multiplicador_frecuencia: 1.0     # Igual que todos
      permitir_knockback: true
      mensaje_fase: "&4💀 Dificultad Global - Sin protección"
  
  # Notificaciones de cambio de fase
  notificar_cambio_fase:
    enabled: true
    mostrar_titulo: true
    sonido: "ENTITY_PLAYER_LEVELUP"
    
  beneficios_adicionales:
    - "XP bonus x1.5 durante primeras 4 horas"
    - "Regeneración pasiva en fase 1 y 2"
    - "Kit de inicio automático después de 5 minutos"
```

---

### 📚 **TUTORIAL INTERACTIVO AL ENTRAR**

#### Flujo Propuesto:

**1. BIENVENIDA INICIAL (Al conectar - No invasiva)**
```
╔════════════════════════════════════════╗
║  🌋 BIENVENIDO A APOCALIPSIS SURVIVAL  ║
╚════════════════════════════════════════╝

¡Bienvenido, %player%!

El tutorial comenzará en 5 minutos.
Mientras tanto, puedes explorar libremente.

Escribe /avo menu para ver opciones.
```

**2. MENSAJE DE INICIO DE TUTORIAL (Después de 5 minutos)**
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    🎓 TUTORIAL INTERACTIVO
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

¡Es hora de aprender a sobrevivir!

Los desastres empezarán MUY SUAVES
y aumentarán gradualmente.

Usa /avo menu para empezar.

[INICIAR TUTORIAL]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

**3. TUTORIAL EN ETAPAS (Comando /avo menu integrado)**

#### 📖 **ETAPA 1: MENÚ PRINCIPAL (Comando /avo menu)**
```
🎯 TUTORIAL PASO 1: MENÚ PRINCIPAL

Acabas de usar /avo menu
Este es tu centro de control:

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📊 TUS ESTADÍSTICAS
   → Nivel, XP, Rango actual
   
🎯 MISIONES DIARIAS
   → Completa objetivos para ganar XP
   
🌟 ÁRBOL DE HABILIDADES
   → Desbloquea poderes únicos
   
🎁 RECOMPENSAS
   → Reclama premios por subir de rango
   
⚔️ ESTADÍSTICAS DE COMBATE
   → Kills, muertes, desastres evadidos
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

💡 TIP: Usa /avo menu frecuentemente
    para ver tu progreso.

[SIGUIENTE →]
```

#### 🌋 **ETAPA 2: DESASTRES NATURALES (Explicación + Demo)**
```
🎯 TUTORIAL PASO 2: DESASTRES NATURALES

El servidor sufre de 3 tipos de desastres:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🌪️ HURACÁN
   ⚡ Empuja a los jugadores
   ⚡ Rompe bloques ligeros
   ⚡ EVASIÓN: Agáchate o escóndete
   
🌋 TERREMOTO  
   ⚡ Sacude el suelo
   ⚡ Rompe bloques cercanos
   ⚡ EVASIÓN: Aléjate del epicentro
   
🔥 LLUVIA DE FUEGO
   ⚡ Cae fuego del cielo
   ⚡ Incendia todo
   ⚡ EVASIÓN: Corre o usa agua
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

💡 IMPORTANTE:
• Los desastres son PERSONALES
• Solo te afectan a ti, no a otros
• Empiezan MUY SUAVES y suben gradualmente
• Cada 30-60 min aumenta la dificultad

⏰ Tu dificultad actual: 10% (Muy Fácil)
   Próxima fase en: 25 minutos

[VER DEMOSTRACIÓN] [SIGUIENTE →]
```

#### 🛠️ **ETAPA 3: SUPERVIVENCIA BÁSICA**
```
🎯 TUTORIAL PASO 3: CÓMO SOBREVIVIR

✅ PRIORIDADES:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1️⃣ Construye un REFUGIO SUBTERRÁNEO
   → Los desastres rompen menos bajo tierra
   → Usa piedra, no madera
   
2️⃣ Consigue BLOQUES DE PROTECCIÓN
   → Se obtienen subiendo de rango
   → Protegen tu base de desastres
   
3️⃣ Completa MISIONES DIARIAS
   → Usa /misiones o /avo menu
   → Dan XP, recursos y recompensas
   
4️⃣ Mejora en el ÁRBOL DE HABILIDADES
   → Usa /habilidades o /avo menu
   → Desbloquea poderes permanentes
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🎁 RECOMPENSA: Kit de Inicio
   • 16 Comida
   • 64 Bloques de Construcción
   • 1 Pico de Hierro
   • 1 Espada de Hierro
   • 32 Antorchas

[RECLAMAR KIT] [SIGUIENTE →]
```

#### ⚔️ **ETAPA 4: SISTEMA DE PROGRESIÓN**
```
🎯 TUTORIAL PASO 4: CÓMO MEJORAR

📈 SISTEMA DE RANGOS:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔰 NOVATO → 🗺️ EXPLORADOR → 💪 SOBREVIVIENTE
         → 🎖️ VETERANO → ⭐ LEYENDA
         → 👑 MAESTRO → 🔱 TITÁN → 💎 ABSOLUTO

💡 Cada rango desbloquea:
   ✨ Habilidades permanentes (automáticas)
   🎁 Recompensas épicas (diamantes, netherite)
   🛡️ Bloques de protección (para tu base)
   🔥 Mayor resistencia a desastres

📊 Tu progreso actual:
   Rango: NOVATO (Nivel 1)
   XP: 0 / 200
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

[VER DEMO DE RANGOS] [SIGUIENTE →]
```

#### 🌟 **ETAPA 5: DEMOSTRACIÓN DE RANGOS (VISUAL)**
```
🎯 DEMO: ¿POR QUÉ SUBIR DE RANGO?

Mira lo que obtienes al alcanzar EXPLORADOR:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🎁 RECOMPENSAS AL SUBIR:
   • 8 Diamantes 💎
   • 5 Manzanas Doradas 🍎
   • 16 Bloques de Hierro
   • 1 Bloque de Protección 🛡️

✨ HABILIDADES PERMANENTES:
   • Velocidad I (permanente)
   • Visión nocturna en cuevas
   • +5% de XP en todas las acciones

📊 PROGRESIÓN:
   NOVATO: Sin habilidades
   ↓ (200 XP)
   EXPLORADOR: Speed I ← ESTÁS AQUÍ EN LA DEMO
   ↓ (550 XP)
   SOBREVIVIENTE: Speed I + Regeneración I
   ↓ (1,100 XP)
   VETERANO: + Resistencia I + Fuerza I
   ...y mucho más!
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

💡 Imagina tener Speed II, Regeneración III,
   Fuerza II y +20 corazones en ABSOLUTO!

[IMPRESIONANTE] [SIGUIENTE →]
```

#### 🎯 **ETAPA 6: ÁRBOL DE HABILIDADES**
```
🎯 TUTORIAL PASO 6: ÁRBOL DE HABILIDADES

Además de las habilidades por rango,
puedes PERSONALIZAR tu estilo de juego:

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🌳 ÁRBOL DE HABILIDADES
   Usa /habilidades o /avo menu

📌 CÓMO FUNCIONA:
   1. Ganas PUNTOS DE HABILIDAD subiendo de nivel
   2. Inviertes puntos en habilidades únicas
   3. Desbloqueas ventajas permanentes

🔥 EJEMPLOS DE HABILIDADES:
   
   ⚔️ RAMA COMBATE:
      • Golpe Crítico Mejorado
      • Más daño con espadas
      • Knockback reducido al recibir daño
      
   ⛏️ RAMA MINERÍA:
      • Doble drop de minerales
      • Velocidad de minado
      • Visión de vetas cercanas
      
   🌾 RAMA SUPERVIVENCIA:
      • Regeneración de hambre
      • Resistencia a veneno
      • Crafteo más rápido
      
   🔮 RAMA ESPECIAL:
      • Evasión de desastres mejorada
      • Daño reducido de desastres
      • Recompensas extra por misiones
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

💡 Tienes 1 punto disponible ahora!
   Úsalo sabiamente.

[ABRIR ÁRBOL] [SIGUIENTE →]
```

#### 📋 **ETAPA 7: COMANDOS IMPORTANTES**
```
🎯 TUTORIAL PASO 7: COMANDOS CLAVE

Memoriza estos comandos:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
/avo menu       → Menú principal (TODO)
/misiones       → Ver misiones diarias
/habilidades    → Árbol de habilidades
/recompensa     → Reclamar recompensas de rango
/avo stats      → Tus estadísticas
/avo help       → Lista completa de comandos
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

💡 TIP: /avo menu es el más importante
    Desde ahí puedes acceder a todo.

[FINALIZAR TUTORIAL]
```

---

### 🎁 **KIT DE INICIO PARA NUEVOS**

```yaml
kit_inicio_novatos:
  otorgar_automatico: true
  
  items:
    # Supervivencia básica
    - "give %player% minecraft:cooked_beef 16"
    - "give %player% minecraft:bread 16"
    - "give %player% minecraft:water_bucket 2"
    
    # Herramientas
    - "give %player% minecraft:iron_pickaxe 1"
    - "give %player% minecraft:iron_axe 1"
    - "give %player% minecraft:iron_sword 1"
    - "give %player% minecraft:iron_shovel 1"
    
    # Materiales de construcción
    - "give %player% minecraft:cobblestone 128"
    - "give %player% minecraft:dirt 64"
    - "give %player% minecraft:torch 32"
    - "give %player% minecraft:oak_planks 64"
    
    # Extras
    - "give %player% minecraft:bed 1"
    - "give %player% minecraft:crafting_table 1"
    - "give %player% minecraft:furnace 1"
    
  mensaje_bienvenida: |
    &6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    &c&l       🌋 APOCALIPSIS SURVIVAL
    &6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    
    &e¡Bienvenido, %player%!
    
    &7Has recibido un &akit de inicio&7.
    &7Tienes &e24 horas de protección&7 para adaptarte.
    
    &6Primeros pasos:
    &7• &f/tutorial &7- Ver la guía completa
    &7• &f/misiones &7- Empezar tus misiones
    &7• &f/habilidades &7- Ver tus poderes
    
    &a¡Buena suerte, sobreviviente!
    &6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

### 💬 **SISTEMA DE TIPS PROGRESIVOS**

```yaml
tips_automaticos:
  enabled: true
  intervalo_minutos: 5           # Cada 5 minutos
  solo_para_nuevos: true         # Solo nivel < 10
  
  tips:
    - mensaje: "&e💡 TIP: &7Construye bajo tierra para estar más seguro de los desastres."
      nivel_minimo: 1
      
    - mensaje: "&e💡 TIP: &7Completa misiones diarias con &f/misiones &7para ganar XP rápido."
      nivel_minimo: 1
      
    - mensaje: "&e💡 TIP: &7Los bloques de protección te protegen. ¡Consigue más subiendo de rango!"
      nivel_minimo: 2
      
    - mensaje: "&e💡 TIP: &7Usa &f/habilidades &7para ver tus poderes desbloqueados."
      nivel_minimo: 3
      
    - mensaje: "&e💡 TIP: &7El agua apaga el fuego. ¡Lleva siempre un cubo!"
      nivel_minimo: 4
      
    - mensaje: "&e💡 TIP: &7Puedes evadir desastres moviéndote estratégicamente."
      nivel_minimo: 5
```

---

### 📊 **MISIONES ESPECIALES PARA NOVATOS**

```yaml
misiones_tutorial:
  TUTORIAL_1:
    nombre: "Primeros Pasos"
    dificultad: TUTORIAL
    descripcion: "Recolecta recursos básicos"
    objetivos:
      - "Consigue 32 de madera"
      - "Craftea 1 mesa de trabajo"
      - "Craftea 1 pico de madera"
    recompensas:
      xp: 50
      comandos:
        - "give %player% minecraft:iron_ingot 8"
      
  TUTORIAL_2:
    nombre: "Construye tu Refugio"
    dificultad: TUTORIAL
    descripcion: "Crea un lugar seguro"
    objetivos:
      - "Coloca 50 bloques bajo tierra"
      - "Coloca 1 cama"
      - "Coloca 10 antorchas"
    recompensas:
      xp: 100
      comandos:
        - "give %player% minecraft:golden_apple 2"
        
  TUTORIAL_3:
    nombre: "Sobrevive tu Primer Desastre"
    dificultad: TUTORIAL
    descripcion: "Resiste un desastre completo"
    objetivos:
      - "Sobrevive 1 desastre sin morir"
    recompensas:
      xp: 150
      comandos:
        - "give %player% minecraft:diamond 3"
```

---

### 🎯 **LOGROS ESPECIALES PARA NUEVOS**

```yaml
logros_novatos:
  PRIMER_DIA:
    nombre: "&e🌅 Primer Día Completado"
    descripcion: "Sobrevive tu primer día en el servidor"
    criterio: "Juega durante 1 hora"
    recompensa:
      xp: 200
      items:
        - "give %player% minecraft:iron_block 4"
        
  PRIMERA_MISION:
    nombre: "&a✅ Primera Misión"
    descripcion: "Completa tu primera misión diaria"
    recompensa:
      xp: 100
      
  PRIMER_NIVEL:
    nombre: "&b⬆️ Primer Nivel"
    descripcion: "Alcanza el nivel 2"
    recompensa:
      items:
        - "give %player% minecraft:golden_apple 3"
```

---

### 🛡️ **IMPLEMENTACIÓN TÉCNICA SUGERIDA**

#### **Nuevos Archivos de Configuración:**

1. **`tutorial.yml`**
```yaml
tutorial:
  enabled: true
  
  # Esperar 5 minutos antes de iniciar (tiempo de registro)
  retraso_inicio_minutos: 5
  
  # Dificultad progresiva temporal
  dificultad_progresiva:
    enabled: true
    fases:
      - minutos: 30, daño: 0.1, frecuencia: 0.2    # Fase 1: 10% dificultad
      - minutos: 30, daño: 0.25, frecuencia: 0.4   # Fase 2: 25% dificultad
      - minutos: 60, daño: 0.5, frecuencia: 0.6    # Fase 3: 50% dificultad
      - minutos: 120, daño: 0.75, frecuencia: 0.8  # Fase 4: 75% dificultad
      # Después de 4 horas: 100% dificultad global
    
  kit_inicio:
    enabled: true
    otorgar_automatico: true
    dar_despues_de_minutos: 5
    
  # Demostración de rangos (simulación visual)
  demo_rangos:
    enabled: true
    simular_efectos_temporales: true      # Dar efectos temporales de EXPLORADOR
    duracion_demo_segundos: 60            # Demo dura 1 minuto
    
  tips_progresivos:
    enabled: true
    intervalo_minutos: 8
    basado_en_tiempo_jugado: true
```

2. **`misiones_tutorial.yml`**
- Misiones especiales para niveles 1-5
- Más fáciles y educativas

#### **Nuevas Clases Java:**

```java
TutorialManager.java
├─ scheduleWelcomeMessage()              // Espera 5 min
├─ showTutorialSteps()                   // Tutorial por etapas
├─ giveStarterKit()                      // Kit después de 5 min
├─ showRankDemo()                        // Demo visual de EXPLORADOR
├─ showSkillTreeInfo()                   // Info árbol habilidades
├─ trackTutorialProgress()               // Track qué etapas completó
└─ sendProgressiveTips()                 // Tips según tiempo jugado

ProgressiveDifficultySystem.java
├─ getPlayerPhase(Player)                // Detecta fase según tiempo jugado
├─ getDamageMultiplier(Player)           // Retorna multiplicador de daño
├─ getFrequencyMultiplier(Player)        // Retorna multiplicador de frecuencia
├─ notifyPhaseChange(Player)             // Avisa cambio de fase
├─ getRemainingTimeToNextPhase(Player)   // Tiempo hasta próxima fase
└─ hasReachedGlobalDifficulty(Player)    // Check si ya está en 100%

TutorialCommand.java
├─ /avo menu (integrado)                 // Menú principal
├─ /tutorial                             // Reabrir tutorial
├─ /tutorial skip                        // Saltar tutorial (admin)
└─ /tutorial reset                       // Reiniciar tutorial (admin)
```

---

### 📱 **SISTEMA DE NOTIFICACIONES**

#### **Al conectar por primera vez:**
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    🌋 APOCALIPSIS SURVIVAL 🌋
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

¡Bienvenido, %player%!

El tutorial comenzará en 5 minutos.
Mientras tanto, explora libremente.

Usa /avo menu para ver todas tus opciones.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

#### **Después de 5 minutos (Inicio del tutorial):**
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    🎓 TUTORIAL INTERACTIVO
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

¡Es hora de aprender a sobrevivir!

✅ Kit de inicio entregado
✅ Dificultad: MUY FÁCIL (10%)
✅ Subirá gradualmente en 4 horas

Usa /avo menu para ver el tutorial completo.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

#### **Al cambiar de fase (cada 30-120 min):**
```
⚠️ CAMBIO DE DIFICULTAD

Fase 1 → Fase 2
Dificultad: 10% → 25%

Los desastres ahora son un poco más fuertes.
¡Sigue construyendo tu base!

Próxima fase en: 30 minutos
```

#### **Al alcanzar dificultad global (4 horas jugadas):**
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    ⚔️ DIFICULTAD GLOBAL ALCANZADA
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

¡Has completado la fase de adaptación!

Ahora los desastres tienen FUERZA COMPLETA.
Ya no tienes protección especial.

💡 Consejos finales:
• Completa misiones diarias para mejorar
• Sube de rango para obtener habilidades
• Construye una base protegida
• ¡Usa /avo menu para ver tu progreso!

¡Buena suerte, sobreviviente! 🔥

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

#### **Notificación de tiempo restante (ActionBar cada 10 min):**
```
⏰ Dificultad: 50% | Próxima fase en: 15 minutos | Usa /avo menu
```

---

### 🎬 **COMANDOS DE TUTORIAL**

```
/avo menu          → Menú principal (RECOMENDADO)
                     Incluye tutorial integrado
                     
/tutorial          → Reabrir tutorial desde el inicio
/tutorial skip     → Saltar tutorial (solo admins)
/tutorial reset    → Reiniciar tutorial (solo admins)
/tutorial fase     → Ver tu fase actual y tiempo restante

Desde /avo menu puedes acceder a:
• Tutorial completo por etapas
• Demo de rangos y habilidades
• Misiones diarias
• Árbol de habilidades
• Estadísticas y progreso
```

---

### 📈 **MÉTRICAS SUGERIDAS**

Trackear para analizar experiencia de nuevos:
- Tiempo promedio hasta nivel 5
- % de jugadores que completan tutorial
- % de jugadores que regresan al día siguiente
- Muertes por desastres en primeras 24h
- Misiones completadas en primer día

---

## 🎨 **EXTRAS OPCIONALES**

### 🎁 **Sistema de Referidos**
```yaml
sistema_referidos:
  enabled: true
  
  beneficios:
    invitador:
      - "500 XP cuando el invitado alcanza nivel 5"
      - "1 Bloque de Protección extra"
      
    invitado:
      - "Protección extendida a 36 horas"
      - "Kit de inicio mejorado"
```

### 📚 **Libro de Supervivencia**
```yaml
libro_supervivencia:
  dar_al_inicio: true
  contenido:
    pagina1: "Guía de Desastres"
    pagina2: "Cómo Construir un Refugio"
    pagina3: "Sistema de Misiones"
    pagina4: "Comandos Útiles"
```

### 🏆 **Tabla de Clasificación Novatos**
```yaml
ranking_novatos:
  enabled: true
  mostrar_solo_nivel_1_a_10: true
  actualizar_cada_minutos: 5
```

---

## ✅ **CHECKLIST DE IMPLEMENTACIÓN**

- [ ] Crear archivo `tutorial.yml`
- [ ] Crear clase `TutorialManager.java`
- [ ] Crear clase `NoobProtectionSystem.java`
- [ ] Implementar comando `/tutorial`
- [ ] Crear misiones tutorial en `misiones_tutorial.yml`
- [ ] Añadir kit de inicio automático
- [ ] Sistema de tips progresivos
- [ ] Mensaje de bienvenida mejorado
- [ ] Reducción de daño para novatos (24h)
- [ ] Notificaciones de tiempo restante
- [ ] Logros especiales para nuevos
- [ ] Libro de supervivencia (opcional)
- [ ] Testing con jugadores reales

---

## 🔧 **CONFIGURACIÓN RECOMENDADA INICIAL**

```yaml
# Configuración optimizada para nueva experiencia
tutorial:
  # Esperar registro/login inicial
  retraso_inicio_minutos: 5
  
  # Sistema de dificultad progresiva temporal
  dificultad_progresiva:
    fase_1:
      duracion_minutos: 30
      multiplicador_daño: 0.1         # 10% daño (MUY FÁCIL)
      multiplicador_frecuencia: 0.2   # 80% menos frecuente
      permitir_knockback: false
      xp_bonus: 1.5                   # 50% más XP
      
    fase_2:
      duracion_minutos: 30
      multiplicador_daño: 0.25        # 25% daño (FÁCIL)
      multiplicador_frecuencia: 0.4   # 60% menos frecuente
      permitir_knockback: false
      xp_bonus: 1.3
      
    fase_3:
      duracion_minutos: 60
      multiplicador_daño: 0.5         # 50% daño (MODERADO)
      multiplicador_frecuencia: 0.6   # 40% menos frecuente
      permitir_knockback: true
      xp_bonus: 1.2
      
    fase_4:
      duracion_minutos: 120
      multiplicador_daño: 0.75        # 75% daño (NORMAL)
      multiplicador_frecuencia: 0.8   # 20% menos frecuente
      permitir_knockback: true
      xp_bonus: 1.1
      
    # Después de 240 minutos (4 horas): 100% dificultad global
    
  # Demo de rangos (simulación)
  demo_rangos:
    enabled: true
    mostrar_en_etapa: 5               # Etapa 5 del tutorial
    simular_efectos_explorador: true  # Dar Speed I temporal
    duracion_demo_segundos: 60        # Demo dura 1 minuto
    
  # Integración con /avo menu
  menu_principal:
    mostrar_tutorial_integrado: true
    icono_tutorial: "BOOK"
    destacar_hasta_nivel: 5
```

---

## 📝 **NOTAS FINALES**

**Filosofía del Tutorial:**
- **Esperar 5 minutos** antes de bombardear al jugador (tiempo de registro)
- **Dificultad progresiva temporal** (10% → 25% → 50% → 75% → 100% en 4 horas)
- **Mostrar por qué vale la pena** subir de rango (demo visual con efectos temporales)
- **Integrar todo en /avo menu** (un solo punto de entrada)
- **Enseñar árbol de habilidades** (personalización del gameplay)
- No abrumar con información
- Aprender haciendo

**Cambios Clave vs Propuesta Anterior:**
1. ✅ **No es protección de 24h fija**, es dificultad progresiva de 4 horas
2. ✅ **Espera 5 minutos** antes de iniciar tutorial (tiempo de registro)
3. ✅ **Demo de rangos** con efectos temporales para motivar
4. ✅ **Tutorial de árbol de habilidades** integrado
5. ✅ **Todo centralizado en /avo menu** (más intuitivo)
6. ✅ **Tips basados en tiempo jugado**, no nivel

**Timeline de Experiencia del Nuevo Jugador:**
```
00:00 - Entra al servidor
        → Mensaje simple de bienvenida
        → "Tutorial comenzará en 5 minutos"
        → Puede explorar libremente

00:05 - Inicio del tutorial
        → Recibe kit de inicio
        → Dificultad: 10% (MUY FÁCIL)
        → Se le muestra /avo menu

00:10 - Tutorial Etapa 1: Menú Principal
        → Conoce /avo menu (hub central)
        
00:15 - Tutorial Etapa 2: Desastres
        → Explicación de los 3 tipos
        → Demo opcional
        
00:20 - Tutorial Etapa 3: Supervivencia
        → Refugio, protecciones, misiones
        
00:25 - Tutorial Etapa 4: Progresión
        → Sistema de rangos explicado
        
00:30 - Tutorial Etapa 5: DEMO DE RANGOS 🔥
        → Recibe Speed I temporal (60 segundos)
        → Ve recompensas de EXPLORADOR
        → Se motiva a subir de rango
        
        + CAMBIO DE FASE: 10% → 25% dificultad

00:35 - Tutorial Etapa 6: Árbol de Habilidades
        → Explicación de ramas
        → Cómo invertir puntos
        → Tiene 1 punto disponible
        
00:40 - Tutorial Etapa 7: Comandos
        → Lista de comandos clave
        → Énfasis en /avo menu
        → Tutorial completado ✅

01:00 - CAMBIO DE FASE: 25% → 50% dificultad
        → Notificación visual
        
02:00 - CAMBIO DE FASE: 50% → 75% dificultad
        → Ya tiene experiencia básica
        
04:00 - DIFICULTAD GLOBAL: 100%
        → Ya es un jugador "veterano"
        → Mensaje épico de graduación
```

**Próximos Pasos:**
1. Crear clase `ProgressiveDifficultySystem.java`
2. Modificar `DisasterController.java` para consultar dificultad por jugador
3. Crear clase `TutorialManager.java` con etapas
4. Integrar tutorial en GUI de `/avo menu`
5. Crear sistema de demo de rangos (efectos temporales)
6. Sistema de tips basado en tiempo jugado
7. Testing exhaustivo

---

## 🎯 **FLUJO DE IMPLEMENTACIÓN SUGERIDO**

### **FASE 1: Core System (Prioritario)**
1. `ProgressiveDifficultySystem.java`
   - Trackear tiempo jugado por jugador
   - Calcular fase actual (1-5)
   - Retornar multiplicadores de daño/frecuencia
   
2. Integración con `DisasterController.java`
   - Consultar multiplicadores antes de aplicar daño
   - Aplicar multiplicador de frecuencia

### **FASE 2: Tutorial System**
1. `TutorialManager.java`
   - Esperar 5 minutos antes de iniciar
   - Mostrar etapas progresivamente
   - Trackear progreso del tutorial
   
2. Integración con `/avo menu`
   - Añadir sección de tutorial
   - Iconos destacados para nuevos

### **FASE 3: Demo & Tips**
1. Sistema de demo de rangos
   - Aplicar efectos temporales de EXPLORADOR
   - Mostrar UI explicativa
   
2. Tips progresivos
   - Basados en tiempo jugado
   - Mostrar en ActionBar (menos intrusivo)

---

*Sistema completo diseñado. ¿Empezamos con la implementación?*
