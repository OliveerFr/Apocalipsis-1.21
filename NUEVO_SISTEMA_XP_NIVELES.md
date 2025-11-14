# SISTEMA DE EXPERIENCIA, NIVELES Y RECOMPENSAS - DOCUMENTACIÓN

## 📋 RESUMEN DE CAMBIOS

Se ha implementado un **sistema completo de progresión** que reemplaza AlonsoLevels con:

### ✨ Nuevos Sistemas

1. **Sistema de Experiencia (XP)** - Independiente de PS
2. **Sistema de Niveles** - Calculado automáticamente desde XP
3. **Sistema de Habilidades Pasivas** - Efectos permanentes por rango
4. **Sistema de Recompensas de Rango** - Items y comandos al subir de rango
5. **Sistema de Recompensas de Misiones** - Bonus por completar misiones diarias
6. **Fuentes Múltiples de XP** - No solo misiones

### 🚀 ESTADO ACTUAL

- ✅ **AlonsoLevels completamente removido** del código y recursos
- ✅ Sistema de XP con 6 fuentes implementado
- ✅ Habilidades pasivas configurables
- ✅ Recompensas de rango con comandos/items
- ✅ **Recompensas de misiones individuales (probabilísticas)**
- ✅ **Recompensas diarias completas (garantizadas + bonus por rango)**
- ✅ Compilación exitosa (JAR sin errores)
- ✅ Documentación completa

---

## 📁 ARCHIVOS NUEVOS

### Java Classes
```
src/main/java/me/apocalipsis/experience/
├── ExperienceService.java      - Gestión de XP y niveles
├── AbilityService.java         - Habilidades pasivas por rango
└── RewardService.java          - Recompensas de rango + misiones

src/main/java/me/apocalipsis/listeners/
└── ExperienceListener.java     - Captura eventos para XP
```

### Archivos de Configuración
```
src/main/resources/
└── recompensas.yml             - XP, habilidades, recompensas de rango y misiones

target/classes/
└── recompensas.yml             - Copia compilada
```

### Datos Persistentes
```
plugins/Apocalipsis/
└── experience_data.yml         - Almacena XP y niveles de jugadores
```

### Documentación
```
├── NUEVO_SISTEMA_XP_NIVELES.md     - Sistema general de XP/habilidades
└── RECOMPENSAS_MISIONES.md         - Sistema de recompensas de misiones
```

---

## 🎮 FUENTES DE EXPERIENCIA

### 1. Misiones Diarias (PRINCIPAL) - 85-90% del XP total
- **Fácil**: 100 XP
- **Media**: 200 XP  
- **Difícil**: 400 XP
- **Multiplicador por rango**: 1.0x - 1.3x (crece con el rango)
- **Balance**: ~1000-2000 XP/día completando misiones
- **Bonus**: Recompensas probabilísticas (30-50%) según dificultad

### 2. Matar Mobs (SECUNDARIO) - 5-8% del XP total
- **Hostiles** (Zombie, Skeleton, etc.): 2 XP
- **Pasivos** (Vaca, Cerdo, etc.): 1 XP
- **Jefes** (Wither, Dragon): 100 XP
- **Balance**: ~100-200 XP/día para jugadores 24/7

### 3. Minar Bloques (MUY BAJO)
- Piedra: 0.5 XP
- Carbón: 1 XP
- Hierro: 2 XP
- Diamante: 5 XP
- Ancient Debris: 10 XP
- **Cooldown**: 5 segundos
- **Balance**: ~50-100 XP/día

### 4. Farming/Cosecha (MUY BAJO)
- Cualquier crop: 0.5 XP
- **Cooldown**: 5 segundos

### 5. Craftear Items (MUY BAJO)
- Pico de piedra: 1 XP
- Pico de hierro: 2 XP
- Pico de diamante: 5 XP
- Pico de netherite: 10 XP
- **Cooldown**: 10 segundos

### 6. Pescar (MUY BAJO)
- Por pez capturado: 2 XP
- **Cooldown**: 15 segundos

---

## 🏆 HABILIDADES POR RANGO

### NOVATO
- Sin habilidades especiales (punto de inicio)

### EXPLORADOR
- ⚡ Velocidad I permanente

### SOBREVIVIENTE
- ⚡ Velocidad I permanente
- 💚 Regeneración I permanente

### VETERANO
- ⚡ Velocidad I
- 💚 Regeneración I
- 🛡️ Resistencia I permanente
- 🍖 Saciedad mejorada

### LEYENDA
- ⚡ Velocidad I
- 💚 Regeneración II
- 🛡️ Resistencia I
- 🍖 Saciedad mejorada
- 💪 Fuerza I permanente

### MAESTRO
- ⚡ Velocidad II
- 💚 Regeneración II
- 🛡️ Resistencia II
- 🍖 Saciedad mejorada
- 💪 Fuerza I
- ⛏️ Prisa I permanente

### TITAN
- ⚡ Velocidad II
- 💚 Regeneración III
- 🛡️ Resistencia II
- 🍖 Saciedad mejorada
- 💪 Fuerza II
- ⛏️ Prisa II
- ❤️ +10 corazones permanentes

### ABSOLUTO
- ⚡ Velocidad II
- 💚 Regeneración IV
- 🛡️ Resistencia III
- 🍖 Saciedad mejorada
- 💪 Fuerza II
- ⛏️ Prisa II
- ❤️ +20 corazones permanentes
- 🔥 Resistencia al fuego
- 🌊 Respiración acuática
- 👁️ Visión nocturna

---

## 🎁 RECOMPENSAS POR RANGO

### EXPLORADOR
- 1 Bloque de Bedrock (protección)
- 5 Diamantes
- 3 Manzanas Doradas

### SOBREVIVIENTE
- 2 Bloques de Bedrock
- 10 Diamantes
- 5 Manzanas Doradas
- 16 Perlas de Ender

### VETERANO
- 3 Bloques de Bedrock
- 20 Diamantes
- 2 Manzanas Encantadas
- 32 Perlas de Ender
- 1 Tótem de Inmortalidad

### LEYENDA
- 4 Bloques de Bedrock
- 5 Lingotes de Netherite
- 3 Manzanas Encantadas
- 2 Tótems de Inmortalidad
- 1 Élitro

### MAESTRO
- 5 Bloques de Bedrock
- 10 Lingotes de Netherite
- 5 Manzanas Encantadas
- 3 Tótems de Inmortalidad
- 2 Estrellas del Nether

### TITAN
- 6 Bloques de Bedrock
- 20 Lingotes de Netherite
- 8 Manzanas Encantadas
- 5 Tótems de Inmortalidad
- 3 Estrellas del Nether

### ABSOLUTO
- 10 Bloques de Bedrock
- 32 Lingotes de Netherite
- 16 Manzanas Encantadas
- 10 Tótems de Inmortalidad
- 5 Estrellas del Nether
- 1 Faro (Beacon)

---

## 🔧 CONFIGURACIÓN (recompensas.yml)

### Niveles de Experiencia
```yaml
experiencia:
  nivel_inicial: 100        # XP para nivel 2
  multiplicador: 50         # XP adicional por nivel
  # Nivel 2 = 100 XP
  # Nivel 3 = 150 XP
  # Nivel 4 = 200 XP, etc.
```

### Fuentes de XP
Todas las fuentes son **completamente configurables** en `recompensas.yml`:
- Cantidades de XP por acción
- Cooldowns para evitar spam
- Habilitar/deshabilitar fuentes
- Multiplicadores por dificultad y rango

### Habilidades
```yaml
habilidades_config:
  intervalo_renovacion: 600    # Renovar cada 30 segundos
  duracion_efecto: 1200        # Duración de 60 segundos
  ocultar_particulas: false    # Mostrar partículas
  notificar_aplicacion: true   # Notificar al jugador
```

### Recompensas
Cada rango tiene:
- Lista de comandos a ejecutar (placeholders: `%player%`)
- Mensaje personalizado
- Ejecutados automáticamente al subir de rango

---

## 🎯 BALANCE Y PROGRESIÓN

### Progresión Esperada
- **Jugador casual** (solo misiones): ~1000-2000 XP/día
- **Jugador activo** (misiones + actividades): ~1200-2300 XP/día
- **Diferencia**: ~200-300 XP/día (10-15% más)

### Diseño del Sistema
1. **Las misiones son la fuente principal** (~85-90% del XP)
2. **Las actividades complementan** (~10-15% del XP)
3. **No se puede hacer "grind" excesivo** (cooldowns previenen spam)
4. **Las habilidades incentivan subir de rango** (no rotas, pero valiosas)
5. **Las recompensas son significativas** (ayudan en progresión)

---

## ⚙️ INTEGRACIÓN CON EL SISTEMA ACTUAL

### PS (Puntos de Supervivencia)
- **Se mantiene el sistema de PS**
- PS determina el **rango** (NOVATO → ABSOLUTO)
- PS se obtiene completando misiones

### XP (Experiencia)
- **Sistema nuevo e independiente**
- XP determina el **nivel** (1, 2, 3...)
- XP se obtiene de múltiples fuentes
- **NO afecta al rango** (solo visual/logro)

### Relación PS ↔ XP
```
Misión Completada
    ↓
  +PS (determina rango)
    ↓
  +XP (determina nivel)
    ↓
Rango Nuevo → Recompensas + Habilidades
```

---

## 🚀 COMANDOS Y FUNCIONES

### Al Completar Misión
1. Otorga PS (sistema existente)
2. **[NUEVO]** Otorga XP (según dificultad y rango)
3. Verifica subida de rango
4. Si sube de rango:
   - Aplica efectos visuales
   - **[NUEVO]** Entrega recompensas
   - **[NUEVO]** Actualiza habilidades

### Al Conectarse
1. Carga datos de experiencia
2. **[NUEVO]** Aplica habilidades pasivas del rango
3. **[NUEVO]** Verifica recompensas pendientes
4. Asigna misiones (si corresponde)

### Al Matar/Minar/Craftear/etc.
1. Verifica cooldown
2. Calcula XP a otorgar
3. Añade XP al jugador
4. Notifica discretamente
5. Verifica subida de nivel

---

## 📊 ARCHIVOS MODIFICADOS

### Core Classes
- `Apocalipsis.java` - Inicializa nuevos servicios
- `ConfigManager.java` - Carga recompensas.yml
- `MissionService.java` - Integra XP en misiones
- `PlayerListener.java` - Aplica habilidades al conectar

### Archivos Eliminados
- ❌ `alonsolevels.yml` - Ya no se usa
- ❌ Referencias a AlonsoLevels en código

---

## 🎮 EXPERIENCIA DE JUGADOR

### Jugador Casual (2-3h/día)
- Completa ~6-8 misiones diarias
- Obtiene ~1000-1500 XP/día
- Puede hacer algunas actividades extra
- **Total**: ~1200-1600 XP/día

### Jugador Activo (5-6h/día)
- Completa ~8-10 misiones diarias
- Obtiene ~1500-2000 XP/día
- Hace muchas actividades (matar, minar, etc.)
- **Total**: ~1700-2300 XP/día

### Diferencia
- ~300-700 XP/día de diferencia (≈30-40%)
- **Las misiones siguen siendo esenciales**
- Las actividades complementan, no reemplazan

---

## ✅ VENTAJAS DEL NUEVO SISTEMA

1. ✨ **Múltiples formas de progresar** (no solo misiones)
2. 🎯 **Balance mantenido** (misiones siguen siendo lo principal)
3. 🏆 **Incentivos claros** (habilidades valiosas pero no rotas)
4. 🎁 **Recompensas configurables** (fácil de ajustar)
5. 📊 **Dos métricas de progresión** (PS para rango, XP para nivel)
6. 🔧 **Totalmente configurable** (todo en recompensas.yml)
7. 🚫 **Anti-grind integrado** (cooldowns previenen spam)
8. 💾 **Datos persistentes** (se guarda en experience_data.yml)

---

## 🔄 MIGRACIÓN DESDE ALONSOLEVELS

### Pasos Realizados
1. ✅ Eliminadas todas las referencias a AlonsoLevels
2. ✅ Creado sistema de XP independiente
3. ✅ Integrado con sistema de misiones existente
4. ✅ Añadidas múltiples fuentes de XP
5. ✅ Implementado sistema de habilidades
6. ✅ Implementado sistema de recompensas
7. ✅ Todo configurable en recompensas.yml

### Compatibilidad
- ✅ **No rompe saves existentes** (PS se mantiene)
- ✅ **Jugadores conservan su rango** (basado en PS)
- ✅ **Sistema de misiones intacto** (solo añade XP)
- ⚠️ **XP empieza desde 0** (nuevo sistema)

---

## 🎨 EFECTOS VISUALES

### Al Completar Misión Individual
- 🔊 Sonidos según dificultad
- ✨ Partículas de éxito
- 🎁 Bonus aleatorio (30-50% chance)

### Al Completar Todas las Misiones del Día
- 🎆 **5 fuegos artificiales** con colores del rango
- 🎵 **Sonidos épicos** (logro + dragón)
- 💬 **Título en pantalla** con subtítulo
- 🎉 **Partículas múltiples** (Tótem, Fuegos, End Rod, Aldeano)
- 🎁 **Recompensas base** (3 diamantes, 2 golden apples, 5 XP bottles)
- ⭐ **Bonus por rango** (escalando desde 5 iron hasta elytra + tridente)

### Al Subir de Nivel
- 🎆 Fuegos artificiales
- 🎵 Sonidos épicos
- 💬 Título en pantalla
- ✨ Partículas

### Al Subir de Rango
- 🎆 Fuegos artificiales con colores del rango
- 🎵 Sonidos de logro
- 💬 Mensaje especial
- 🎁 Entrega de recompensas (comandos/items)
- ⚡ Aplicación de nuevas habilidades

### Durante el Juego
- 📊 Barra de acción al ganar XP
- 💬 Mensajes discretos (+X XP)
- 🔔 Sonidos sutiles

---

## 🎯 SISTEMA DE RECOMPENSAS DE MISIONES

### Recompensas Individuales (Por Misión)
- **Probabilísticas**: No siempre se reciben
- **Según dificultad**:
  - FÁCIL: 30% chance → Pan, carne
  - MEDIA: 40% chance → Hierro, oro, flechas
  - DIFÍCIL: 50% chance → Diamante, manzana dorada, botellas XP
- **Incentivo adicional**: Motiva completar más misiones

### Recompensas Diarias Completas
- **Garantizadas**: Siempre se reciben al completar las 5 misiones
- **Recompensas base** (todos los rangos):
  - 3 diamantes
  - 2 manzanas doradas
  - 5 botellas de experiencia
- **Bonus por rango** (escalado):
  - NOVATO: 5 iron + 10 coal
  - EXPLORADOR: 5 gold + 16 ender pearls + 1 golden apple
  - SOBREVIVIENTE: 2 diamonds + 5 emeralds + 1 enchanted golden apple
  - VETERANO: 3 diamonds + 1 diamond block + 1 elytra (Unbreaking III) + 2 enchanted golden apples
  - LEYENDA: 5 diamonds + 1 netherite block + 3 enchanted golden apples + 1 nether star
  - MAESTRO: 2 netherite blocks + 4 enchanted golden apples + 2 nether stars + 1 elytra (Unbreaking III + Mending)
  - TITÁN: 3 netherite blocks + 5 enchanted golden apples + 3 nether stars + 1 elytra + 1 trident (Riptide III + Loyalty III)
  - ABSOLUTO: 3 netherite blocks + 5 enchanted golden apples + 2 nether stars + 1 elytra completo + 1 trident completo + 1 totem

Ver documentación completa en: **RECOMPENSAS_MISIONES.md**

---

## 📝 NOTAS FINALES

### Para Administradores
- Revisa `recompensas.yml` para ajustar balance
- Los cooldowns previenen abuso del sistema
- Las habilidades están balanceadas progresivamente
- Las recompensas pueden editarse fácilmente
- Ajusta probabilidades de recompensas según economía del servidor

### Para Jugadores
- **Haz las misiones** (principal fuente de XP + recompensas)
- Completar **todas las misiones diarias** da recompensas épicas
- Las actividades extra ayudan al progreso
- Las habilidades son **muy valiosas** a largo plazo
- Las recompensas de rango son **automáticas**
- Bonus de misiones individuales son **aleatorios** pero frecuentes

---

🎉 **SISTEMA COMPLETAMENTE FUNCIONAL Y LISTO PARA USO** 🎉
