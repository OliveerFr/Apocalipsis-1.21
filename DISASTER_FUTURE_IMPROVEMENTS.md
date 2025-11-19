# 🌪️ CHECKLIST DE MEJORAS FUTURAS PARA DESASTRES
**Versión Actual: v1.18.0**
**Fecha: 19 Nov 2025**

---

## 📊 SISTEMA DE ESTADÍSTICAS Y FEEDBACK

### ✅ Estadísticas Post-Desastre
- [ ] **Pantalla de resumen al finalizar desastre**
  - Mostrar fases sobrevividas (ej: "Sobreviviste hasta fase 4/5")
  - Tiempo total sobrevivido
  - Número de muertes durante el desastre
  - Ranking de supervivientes (quién sobrevivió más fases)
  - Mostrar en título + chat con formato bonito

- [ ] **Historial de desastres**
  - Comando `/avo stats desastres` para ver historial personal
  - Mejor racha de supervivencia
  - Total de desastres sobrevividos completamente
  - Porcentaje de supervivencia por tipo de desastre

- [ ] **Leaderboard de supervivencia**
  - Top 10 supervivientes del servidor
  - Estadísticas globales por desastre
  - Achievements especiales (ej: "Sobrevivió 10 terremotos sin morir")

---

## 🎯 MECÁNICAS DE SUPERVIVENCIA Y ESTRATEGIA

### ✅ Sistema de Refugios
- [ ] **Refugios temporales automáticos**
  - Estructuras de obsidiana que aparecen en fase 3
  - Duran 30 segundos y luego desaparecen
  - Máximo 3 refugios por desastre
  - Dar tiempo a jugadores para reagruparse

- [ ] **Zonas seguras dinámicas**
  - "Ojo del huracán" móvil con 50% menos daño
  - "Zona tectónica estable" en terremoto
  - "Manantial bendito" que reduce fuego en lluvia
  - Anunciar ubicación con coordenadas

- [ ] **Items de protección especiales**
  - "Casco Antisísmico" (reduce shake en terremoto)
  - "Capa del Viento" (reduce pushback en huracán)
  - "Escudo Ígneo" (inmunidad temporal al fuego)
  - Crafteable o drop raro de eventos

### ✅ Mecánicas de Cooperación
- [ ] **Revivir compañeros caídos**
  - Interactuar con jugador muerto para revivirlo (5 segundos)
  - Solo posible durante desastre activo
  - Revive con 50% de vida
  - Incentiva trabajo en equipo

- [ ] **Construcción de barricadas**
  - Permite colocar bloques temporales durante desastre
  - Se remueven automáticamente al finalizar
  - Límite de 20 bloques por jugador
  - Cooldown entre colocaciones

---

## 🌟 EFECTOS VISUALES Y ATMOSFÉRICOS

### ✅ Mejoras Visuales Generales
- [ ] **Transiciones de fase mejoradas**
  - Countdown de 5 segundos entre fases
  - Pantalla temblorosa o distorsionada
  - Partículas masivas al cambiar fase
  - Sonido dramático crescendo

- [ ] **Partículas ambientales por zona**
  - Polvo flotante en el aire (terremoto)
  - Hojas y debris volando (huracán)
  - Ceniza cayendo constantemente (lluvia fuego)
  - Efecto de calor distorsionado

- [ ] **Iluminación dinámica**
  - Oscurecer cielo progresivamente
  - Relámpagos frecuentes en huracán
  - Brillo rojo/naranja en lluvia de fuego
  - Partículas de luz en fase final

### ✅ Efectos Específicos por Desastre
**TERREMOTO:**
- [ ] Cámara shake más intenso en fase 5
- [ ] Partículas de rocas flotando
- [ ] Sonido de rocas quebrándose constantemente
- [ ] Efecto de "niebla de polvo" reduciendo visibilidad

**HURACÁN:**
- [ ] Hojas y ramas volando por el aire
- [ ] Agua acumulándose en bloques bajos
- [ ] Sonido de viento intenso estéreo
- [ ] Nubes moviéndose rápidamente

**LLUVIA DE FUEGO:**
- [ ] Meteoros más grandes y visibles
- [ ] Rastros de fuego en el cielo
- [ ] Humo negro denso
- [ ] Explosiones secundarias aleatorias

---

## ⚙️ CONFIGURACIÓN Y BALANCEO

### ✅ Sistema de Dificultad Adaptativa
- [ ] **Escalar por número de jugadores**
  - Más jugadores online = más entidades/efectos
  - Menos jugadores = reducir intensidad
  - Ajuste dinámico en tiempo real

- [ ] **Niveles de dificultad configurables**
  - Fácil: 50% intensidad, sin mobs hostiles
  - Normal: 100% intensidad (actual)
  - Difícil: 150% intensidad, más mobs, menos refugios
  - Extremo: 200% intensidad, múltiples bosses

- [ ] **Modo "Primera vez"**
  - Tutorial interactivo del primer desastre
  - Mensajes explicativos
  - Daño reducido 50%
  - Se activa automáticamente para nuevos jugadores

### ✅ Configuración Detallada
- [ ] **Config por fase individual**
  - Poder configurar duración de cada fase
  - Intensidad específica por fase
  - Mecánicas activadas/desactivadas por fase

- [ ] **Whitelist/Blacklist de biomas**
  - Elegir en qué biomas pueden ocurrir desastres
  - Intensidad modificada por bioma
  - Biomas "inmunes" (ej: spawn protection)

---

## 🎭 VARIANTES Y EVENTOS ESPECIALES

### ✅ Variantes de Desastres
- [ ] **Terremoto Volcánico**
  - Lava emerge de grietas
  - Spawn de Blazes y Magma Cubes
  - Bloques se convierten en Magma Blocks

- [ ] **Huracán de Hielo**
  - Agua se congela
  - Spawn de Strays en lugar de Vex
  - Slowness por congelación

- [ ] **Lluvia Ácida**
  - Daño continuo sin refugio
  - Corrosión de armadura
  - Envenenamiento progresivo

### ✅ Eventos Combinados
- [ ] **"Apocalipsis Total"**
  - 2-3 desastres simultáneos
  - Anuncio global dramático
  - Recompensas especiales únicas
  - Ocurre muy raramente (0.1% chance)

- [ ] **Desastre Nocturno**
  - Solo ocurre de noche
  - Spawn adicional de mobs hostiles
  - Visibilidad reducida
  - Intensidad 1.5x

---

## 🏆 SISTEMA DE LOGROS Y DESAFÍOS

### ✅ Achievements de Supervivencia
- [ ] "Sobreviviente Novato" - Sobrevive tu primer desastre
- [ ] "Pie Firme" - Sobrevive Terremoto fase 5 sin morir
- [ ] "Ojo de la Tormenta" - Sobrevive Huracán fase 5 sin morir
- [ ] "Caminante de Fuego" - Sobrevive Lluvia fase 5 sin morir
- [ ] "Indestructible" - Sobrevive 10 desastres consecutivos
- [ ] "Maestro del Caos" - Sobrevive todos los tipos de desastre en un día
- [ ] "Salvador" - Revive a 5 jugadores en un solo desastre

### ✅ Desafíos Opcionales
- [ ] **Modo Hardcore**
  - Una muerte = expulsión del desastre
  - Recompensas 3x
  - Opt-in con comando

- [ ] **Speedrun Survival**
  - Temporizador de supervivencia
  - Ranking por tiempo
  - Modo competitivo

---

## 🔧 OPTIMIZACIÓN Y RENDIMIENTO

### ✅ Mejoras de Performance
- [ ] **Sistema de chunking**
  - Solo procesar chunks con jugadores cerca
  - Reducir carga en áreas vacías
  - Ajuste dinámico de tick rate

- [ ] **Pooling de entidades**
  - Reutilizar entidades en lugar de spawn/despawn
  - Reducir garbage collection
  - Límite dinámico según TPS

- [ ] **Partículas optimizadas**
  - Reducción automática si TPS < 18
  - Sistema de LOD (Level of Detail)
  - Opciones de calidad gráfica por jugador

### ✅ Detección de Lag
- [ ] **Auto-ajuste de intensidad**
  - Monitorear TPS en tiempo real
  - Reducir spawns si TPS < 15
  - Pausar mecánicas pesadas si TPS < 10
  - Mensaje a staff alertando problemas

---

## 🎨 CALIDAD DE VIDA (QoL)

### ✅ Interfaz y Comandos
- [ ] **Panel de control admin**
  - GUI para ajustar configuración en vivo
  - Testing de desastres individuales
  - Ver estadísticas del servidor

- [ ] **Comandos mejorados**
  - `/avo preview <desastre>` - Preview seguro sin daño
  - `/avo skip-phase` - Saltar a siguiente fase (admin)
  - `/avo intensity <0-200>` - Ajustar intensidad temporal

- [ ] **Avisos anticipados**
  - Anuncio 60s antes de desastre aleatorio
  - Coordenadas de epicentro/origen
  - Tiempo estimado de duración

### ✅ Accesibilidad
- [ ] **Modo reducción de efectos**
  - Desactivar screen shake
  - Reducir partículas 90%
  - Mantener mecánicas de daño
  - Para jugadores sensibles a movimiento

- [ ] **Sistema de notificaciones**
  - Alerts en Discord via webhook
  - Logs detallados de eventos
  - Exportar estadísticas a CSV

---

## 🌍 INTEGRACIÓN CON EL MUNDO

### ✅ Consecuencias Persistentes
- [ ] **Daño al terreno**
  - Algunos bloques quedan destruidos permanentemente
  - Requiere reparación manual
  - Zonas "marcadas" por desastres

- [ ] **Recursos especiales post-desastre**
  - Minerales raros en grietas de terremoto
  - Cristales de viento tras huracán
  - Obsidiana encantada tras lluvia de fuego
  - Incentivo para explorar zonas afectadas

- [ ] **Reconstrucción comunitaria**
  - Misiones grupales para reparar daños
  - Recompensas por restaurar zonas
  - Contador de "salud del mundo"

---

## 💡 IDEAS EXPERIMENTALES

### ⚠️ Requieren Testing Extensivo
- [ ] **IA adaptativa de desastres**
  - Aprende patrones de jugadores
  - Ajusta dificultad automáticamente
  - Spawn estratégico de entidades

- [ ] **Desastres personalizados por jugador**
  - Cada jugador ve intensidad diferente según nivel
  - Balanceo automático
  - Experiencia individual

- [ ] **Modo "Profecía"**
  - Jugadores pueden predecir próximo desastre
  - Recompensa si aciertan
  - Sistema de apuestas PS

- [ ] **Ciclo lunar/estacional**
  - Luna llena = desastres más intensos
  - Estaciones afectan tipo de desastre
  - Calendario de eventos predecible

---

## 📝 PRIORIZACIÓN SUGERIDA

### 🔥 PRIORIDAD ALTA (Impacto Inmediato)
1. Estadísticas post-desastre (feedback al jugador)
2. Transiciones de fase mejoradas
3. Sistema de refugios temporales
4. Auto-ajuste de intensidad por TPS

### ⭐ PRIORIDAD MEDIA (Mejora Experiencia)
5. Partículas ambientales mejoradas
6. Variantes de desastres
7. Achievements de supervivencia
8. Configuración detallada por fase

### 💎 PRIORIDAD BAJA (Polish)
9. Modo reducción de efectos
10. Recursos especiales post-desastre
11. Eventos combinados raros
12. Ideas experimentales

---

**Total de mejoras listadas: ~80 items**
**Tiempo estimado implementación completa: 40-60 horas**

¿En qué categoría te gustaría enfocarte primero?
