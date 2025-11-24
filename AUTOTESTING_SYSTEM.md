# Sistema de Autotesting - Eventos

Sistema automatizado para testing de eventos con bots simulados que imitan comportamiento de jugadores reales.

## 📋 Características

- **Bots Simulados**: 3-5 bots con diferentes perfiles de comportamiento
- **Múltiples Escenarios**: Casos de prueba específicos para cada fase del evento
- **Reportes Detallados**: Generación automática de reportes con estadísticas
- **Detección de Bugs**: Identifica problemas y edge cases automáticamente
- **Suites Completas**: Ejecuta todos los escenarios de un evento secuencialmente

## 🤖 Perfiles de Bots

### 1. PRO_PLAYER
- Rápido y preciso
- Reacción: 0.5s
- Errores: 5%
- **Uso**: Probar mecánicas con jugadores habilidosos

### 2. CASUAL_PLAYER
- Velocidad media
- Reacción: 1.5s
- Errores: 15%
- **Uso**: Simular jugador promedio

### 3. NEWBIE_PLAYER
- Lento y cauteloso
- Reacción: 3.0s
- Errores: 30%
- **Uso**: Probar accesibilidad para novatos

### 4. CHAOTIC_PLAYER
- Comportamiento impredecible
- Reacción: 2.0s
- Errores: 25%
- **Uso**: Detectar bugs con comportamientos errático s

### 5. AFK_PLAYER
- Casi inactivo
- Reacción: 10.0s
- Errores: 50%
- **Uso**: Probar manejo de jugadores AFK

## 📝 Escenarios de Prueba

### Eco de Brasas

#### 1. BasicParticipation
- **Descripción**: Verificar registro de participantes
- **Duración**: 10s
- **Validación**: Todos los bots permanecen activos

#### 2. GrietaClosing
- **Descripción**: Simular cierre de grietas
- **Duración**: 20s
- **Validación**: Grietas cerradas, items recibidos

#### 3. AnclaCompletion
- **Descripción**: Entrega cooperativa a anclas
- **Duración**: 30s
- **Validación**: Anclas completadas, cooperación

#### 4. GuardianFight
- **Descripción**: Combate contra boss final
- **Duración**: 40s
- **Validación**: Ataques registrados, balance de dificultad

#### 5. PlayerDeath
- **Descripción**: Manejo de muertes y respawns
- **Duración**: 15s
- **Validación**: Muertes registradas, respawns correctos

#### 6. AFKPlayer
- **Descripción**: Jugador inactivo durante evento
- **Duración**: 20s
- **Validación**: Sistema maneja jugadores AFK

#### 7. PartialParticipation
- **Descripción**: Participación variada (activos/pasivos)
- **Duración**: 25s
- **Validación**: Recompensas proporcionales a participación

### Eco de Sombras

#### 1. SombrasEvasion
- **Descripción**: Evadir sombras enemigas
- **Duración**: 20s
- **Validación**: Bots evaden, todos sobreviven

#### 2. NucleoDefeat
- **Descripción**: Perseguir núcleo que se teletransporta
- **Duración**: 30s
- **Validación**: Ataques al núcleo, persecución activa

#### 3. AnclaSealing
- **Descripción**: Sellar 5 anclas con fragmentos
- **Duración**: 30s
- **Validación**: Anclas selladas, uso de fragmentos

#### 4. OleadaSurvival
- **Descripción**: Sobrevivir oleadas de enemigos
- **Duración**: 40s
- **Validación**: Enemigos eliminados, supervivencia

## 🎮 Uso

### Iniciar Autotesting

```
/avo autotest start <evento>
```

Crea 3-5 bots en tu ubicación actual y los prepara para testing.

**Eventos disponibles:**
- `eco_brasas`
- `eco_sombras`

### Ejecutar Escenario Individual

```
/avo autotest run <escenario>
```

**Escenarios disponibles:**
- `basic` - Participación básica
- `grieta` - Cierre de grietas (Eco de Brasas)
- `ancla` - Completar anclas (Eco de Brasas)
- `guardian` - Pelea contra guardián
- `death` - Muerte de jugadores
- `afk` - Jugador AFK
- `partial` - Participación parcial
- `sombras` - Evasión de sombras (Eco de Sombras)
- `nucleo` - Derrota del núcleo (Eco de Sombras)
- `ancla_sombras` - Sellado de anclas (Eco de Sombras)
- `oleada` - Supervivencia de oleadas (Eco de Sombras)

### Ejecutar Suite Completa

```
/avo autotest suite <evento>
```

Ejecuta todos los escenarios del evento secuencialmente. Esto puede tardar varios minutos.

### Ver Bots Activos

```
/avo autotest bots
```

Muestra lista de bots con sus estadísticas:
- Estado actual
- Acciones realizadas
- Items recolectados
- Muertes

### Generar Reporte

```
/avo autotest report
```

Genera reporte completo con:
- Estadísticas generales
- Tests pasados/fallidos
- Estadísticas de cada bot
- Errores detectados

### Reporte Rápido

```
/avo autotest quick
```

Muestra resumen rápido: `X/Y tests pasados (Z%)`

### Limpiar Resultados

```
/avo autotest clear
```

Borra resultados de tests anteriores.

### Detener Autotesting

```
/avo autotest stop
```

Detiene el sistema y elimina todos los bots.

## 📊 Interpretación de Resultados

### Tests PASS ✓
- Todas las validaciones pasaron correctamente
- El escenario funciona como se espera
- Pueden haber warnings (avisos) pero no errores

### Tests FAIL ✗
- Una o más validaciones fallaron
- Se detectaron errores críticos
- Revisar la lista de errores en el reporte

### Warnings ⚠
- Comportamientos sospechosos pero no críticos
- Posibles problemas de balance
- Situaciones edge case

## 🔍 Ejemplo de Uso Completo

```bash
# 1. Iniciar autotesting
/avo autotest start eco_brasas

# 2. Ver bots creados
/avo autotest bots

# 3. Ejecutar escenario individual
/avo autotest run grieta

# 4. Ver resultado rápido
/avo autotest quick

# 5. Ejecutar suite completa
/avo autotest suite eco_brasas

# 6. Generar reporte completo
/avo autotest report

# 7. Detener autotesting
/avo autotest stop
```

## 🐛 Detección de Bugs

El sistema detecta automáticamente:

1. **Problemas de Registro**: Jugadores no registrados como participantes
2. **Recompensas**: Items no entregados, PS incorrectos
3. **Balance**: Dificultad muy alta/baja
4. **Cooperación**: Mecánicas cooperativas no funcionando
5. **Muerte/Respawn**: Problemas con muertes y respawns
6. **AFK**: Jugadores inactivos no manejados correctamente
7. **Edge Cases**: Situaciones inusuales que causan problemas

## 💡 Mejores Prácticas

1. **Antes de Desplegar**: Ejecuta suite completa
2. **Después de Cambios**: Re-ejecuta escenarios afectados
3. **Testing Regular**: Ejecuta tests periódicamente
4. **Revisar Warnings**: Incluso si el test pasa, revisa los warnings
5. **Diversidad**: Asegúrate de que haya bots con diferentes perfiles

## 🔧 Extensibilidad

Para añadir nuevos escenarios:

1. Crear clase que extienda `TestScenario`
2. Implementar métodos abstractos:
   - `getName()`: Nombre del escenario
   - `getDescription()`: Descripción
   - `getDurationTicks()`: Duración en ticks
   - `execute()`: Lógica del escenario
   - `validate()`: Validaciones
3. Añadir al método `getTestScenariosForEvent()` en `EventAutoTestingSystem`

## ⚙️ Configuración

El sistema no requiere configuración adicional. Los bots se crean automáticamente con perfiles predefinidos para asegurar diversidad de comportamiento.

## 📈 Métricas Recopiladas

Por cada test:
- Tiempo de ejecución
- Número de errores
- Número de warnings
- Acciones por bot
- Items recolectados por bot
- Muertes por bot

## 🎯 Objetivos del Sistema

1. ✅ Reducir tiempo de testing manual
2. ✅ Detectar bugs antes del despliegue
3. ✅ Validar balance de dificultad
4. ✅ Probar edge cases automáticamente
5. ✅ Asegurar calidad de eventos

---

**Nota**: Los bots NO son jugadores reales, son simulaciones. No aparecen en la lista de jugadores ni interactúan con el mundo más allá de la simulación.
