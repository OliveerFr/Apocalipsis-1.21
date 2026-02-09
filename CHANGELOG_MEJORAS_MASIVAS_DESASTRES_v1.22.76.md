# 🌟 CHANGELOG - Mejoras Masivas Desastres Ciclo 2 v1.22.76

## 📅 Fecha
09/02/2026

## 🎯 Objetivo
Implementación de mejoras masivas en los desastres del Ciclo 2 con efectos climáticos dinámicos, mecánicas especiales por fase, sonidos ambientales inmersivos y efectos de terreno únicos.

---

## 🌨️ TORMENTA GLACIAL - Mejoras Implementadas

### ✨ Efectos Climáticos Dinámicos
- **Clima por Fase**: El clima del mundo cambia según la fase actual
  - Fase INICIO: Tormenta leve
  - Fase ESCALADA/PICO: Tormenta con nieve
  - Fase CRÍTICO: Tormenta + Medianoche + Truenos (máxima oscuridad)
  - Fase DECLIVE: Tormenta decreciente

### 🌪️ Ventisca Cegadora (Fase PICO y CRÍTICO)
- **Partículas Masivas**:
  - PICO: 80 copos de nieve + nubes + ceniza blanca
  - CRÍTICO: 150 partículas por jugador
  
- **Efectos de Ventisca**:
  - Ceguera temporal (30% probabilidad en CRÍTICO)
  - Ralentización por viento helado (amplifier 1-2)
  - Sonido de viento constante
  - Action bar: "§f§l❄ §c§l¡VENTISCA CEGADORA!"

### 🎵 Sonidos Ambientales Continuos
- **Viento Helado Base**: Sonido de elytra a pitch bajo (cada 3 segundos)
- **Por Fase**:
  - ESCALADA/PICO: Cristales rompiéndose
  - CRÍTICO: Rugidos de Ender Dragon + aullidos de Wither
  
### 🌐 Restauración de Clima
- Al terminar el desastre, el clima vuelve a normal automáticamente

---

## 🌋 ERUPCIÓN VOLCÁNICA - Mejoras Implementadas

### ✨ Efectos Climáticos Dinámicos
- **Ceniza Volcánica**:
  - Fase INICIO-ESCALADA: Tormenta leve (ceniza bloquea luz)
  - Fase PICO: Oscuridad parcial (medianoche)
  - Fase CRÍTICO: Medianoche + truenos volcánicos
  
### ☄️ Lluvia de Meteoros (Fase CRÍTICO)
- **Meteoros Incandescentes**:
  - Spawn: 2-4 meteoros masivos cada 7.5 segundos
  - Material: NETHERRACK brillante (glowing)
  - Altura: 30-45 bloques sobre jugadores
  
- **Estela de Fuego**:
  - 20 partículas FLAME por tick
  - 10 partículas LAVA
  - 15 partículas SMOKE
  - Partículas DRIPPING_LAVA
  
- **Impacto Explosivo**:
  - Explosión de poder 4.0 (masiva)
  - 3 partículas EXPLOSION_EMITTER
  - Ondas de choque radiales (5 anillos)
  - Partículas FLAME y LAVA en círculos expansivos
  
- **Advertencia**:
  - Sonido: GHAST_SCREAM (terror)
  - Título para jugadores cercanos: "§c§l☂️ METEORO" / "§6§l¡BUSCA COBERTURA!"
  - Radio de advertencia: 30 bloques

### 🎵 Sonidos Ambientales de Volcán
- **Rumble Constante**: Ender Dragon growl a pitch bajo (cada 4 segundos)
- **Por Fase**:
  - ESCALADA/PICO: Lava pop + fuego ambiental
  - CRÍTICO: Explosiones + Blaze shoots (intenso)
  
### 🌐 Restauración de Clima
- Clima normal restaurado al finalizar

---

## ⚡ TORMENTA ELÉCTRICA - Mejoras Implementadas

### ✨ Efectos Climáticos Dinámicos
- **Tormenta Eléctrica Permanente**:
  - Tormenta + truenos activos todo el desastre
  - Fase CRÍTICO: Medianoche para máximo drama
  
### ⛓️ Cadenas Eléctricas entre Jugadores (Fase PICO y CRÍTICO)
- **Mecánica**:
  - Origen: Jugador aleatorio
  - Saltos: 2 en PICO, 4 en CRÍTICO
  - Rango: 15 bloques entre jugadores
  
- **Efectos Visuales**:
  - Partículas ELECTRIC_SPARK entre jugadores (línea continua)
  - Partículas FIREWORK brillantes
  - Distancia entre partículas: 0.3 bloques
  
- **Daño**:
  - 60% del daño normal de rayo
  - Escalado por fase (multiplicador)
  - Sonido LIGHTNING_BOLT_IMPACT en cada salto
  
- **Advertencia**:
  - Action bar al origen: "§e§l⚡ ¡CADENA ELÉCTRICA DESDE TI!"
  - Action bar al golpeado: "§c⚡ Golpeado por cadena eléctrica!"

### 💥 Sobrecarga Eléctrica (Fase CRÍTICO)
- **Daño Ambiental**:
  - 40% probabilidad cada tick (cada 2 segundos)
  - Daño: 0.5 corazones
  - Partículas: 10 ELECTRIC_SPARK alrededor del jugador
  - Action bar (30%): "§e⚡ Sobrecarga eléctrica ambiental"

### 🎵 Sonidos Ambientales Eléctricos
- **Base**: Beacon ambient a pitch alto (cada 3.5 segundos)
- **Por Fase**:
  - ESCALADA/PICO: Firework rocket blast
  - CRÍTICO: Warden sonic boom + Respawn anchor charge (máxima intensidad)
  
### 🌐 Restauración de Clima
- Clima despejado al finalizar

---

## 🔧 Cambios Técnicos

### Nuevas Tasks Implementadas
**TormentaGlacial**:
- `sonidosTask`: Sonidos ambientales cada 3 segundos
- `ventiscaTask`: Ventisca cada 5 segundos (solo PICO/CRÍTICO)

**ErupcionVolcanica**:
- `sonidosTask`: Sonidos volcánicos cada 4 segundos
- `meteorosTask`: Meteoros cada 7.5 segundos (solo CRÍTICO)

**TormentaElectrica**:
- `sonidosTask`: Sonidos eléctricos cada 3.5 segundos
- `cadenasTask`: Cadenas eléctricas cada 5 segundos (PICO/CRÍTICO)
- `sobrecargaTask`: Daño ambiental cada 2 segundos (CRÍTICO)

### Nuevos Métodos
Todos los desastres:
- `aplicarEfectosClimaticos(int faseNumero)`: Aplica clima según fase
- `startSonidosAmbientales()`: Inicia sonidos continuos
- `getFaseString(int faseNum)`: Convierte número de fase a String

Específicos:
- `startVentisca()` (TormentaGlacial): Ventisca con ceguera
- `startMeteoros()` (ErupcionVolcanica): Lluvia de meteoros explosivos
- `startCadenasElectricas()` (TormentaElectrica): Rayos en cadena
- `startSobrecargaElectrica()` (TormentaElectrica): DoT eléctrico

### Actualización de onStop()
- Todos los desastres ahora restauran el clima normal al terminar
- Cancelación de nuevas tasks agregadas

### Actualización de onTick()
- Detección de cambio de fase para actualizar efectos climáticos dinámicamente

---

## 📊 Resultados de Compilación
```
[INFO] BUILD SUCCESS
[INFO] Compiling 170 source files
[INFO] No compilation errors
```

---

## 🎮 Experiencia de Juego

### Inmersión Mejorada
- **Clima Dinámico**: Los jugadores sienten la progresión del desastre a través del clima
- **Sonidos Continuos**: Atmósfera constante de peligro
- **Efectos Visuales Masivos**: Partículas que llenan la pantalla en fases críticas

### Dificultad Escalada
- **Ventisca Cegadora**: Dificulta navegación en CRÍTICO
- **Meteoros Explosivos**: Peligro extremo desde arriba
- **Cadenas Eléctricas**: Múltiples jugadores afectados simultáneamente
- **Sobrecarga**: Daño constante inavitable en fase final

### Balance
- Efectos especiales solo en fases intensas (PICO/CRÍTICO)
- Warnings visuales y sonoros antes de ataques masivos
- Clima restaurado para no afectar gameplay post-desastre

---

## 🔮 Próximos Pasos Sugeridos
1. ✅ **Efectos climáticos** - COMPLETADO
2. ✅ **Mecánicas únicas por fase** - COMPLETADO
3. ✅ **Sonidos ambientales** - COMPLETADO
4. ✅ **Efectos especiales** - COMPLETADO
5. 🔄 **Testing en servidor** - Pendiente
6. 🔄 **Balance de daño** - Ajustar según feedback

---

## 📝 Notas
- Todas las mecánicas respetan las fases existentes
- Compatibilidad total con sistema de protecciones
- Sin cambios en configuración (config.yml)
- Performance optimizado (tasks canceladas correctamente)

---

## 👥 Créditos
**Desarrollador**: AI Assistant  
**Versión**: v1.22.76  
**Plugin**: Apocalipsis  
**Minecraft**: 1.21+  
**Sistema de Fases**: v1.22.75
