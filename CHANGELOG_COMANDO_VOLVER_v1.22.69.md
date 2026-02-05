# Changelog v1.22.69 - Comando de Escape del End

**Fecha**: 2024
**Versión**: 1.22.69

## 🎯 Objetivo

Implementar un comando de emergencia que permita a los jugadores escapar del End cuando quedan atrapados sin acceso a un portal de retorno.

## 📋 Problema Reportado

> "hay gente que esta quedando atrapada y no puede salir"

Los jugadores reportaron estar quedando atrapados en la dimensión del End sin manera de volver al Overworld, probablemente debido a:
- Portales destruidos o no generados correctamente
- Muerte antes de crear portal de retorno
- Problemas con protecciones del End compartido entre ciclos

## ✨ Solución Implementada

### 1. Nuevo Comando: `/avo volver`

Se implementó un sistema de escape de emergencia con las siguientes características:

#### **Aliases disponibles:**
- `/avo volver`
- `/avo overworld`
- `/avo salir`
- `/avo escape`

#### **Funcionalidad:**
- ✅ **Verificación de dimensión**: Solo funciona cuando el jugador está en el End
- ✅ **Cooldown anti-abuso**: 30 segundos entre usos (configurable)
- ✅ **Bypass para admins**: Jugadores con permiso `apocalipsis.ciclo.admin` no tienen cooldown
- ✅ **Teleporte inteligente**: 
  - Detecta el ciclo activo del jugador
  - Teleporta al spawn del Overworld del ciclo correspondiente
  - Verifica que la ubicación de destino sea segura
  - Busca alternativa si el spawn no es seguro
- ✅ **Feedback visual**: 
  - Mensajes claros en español
  - Efectos de partículas (portal)
  - Sonido de teleporte (enderman)
- ✅ **Logging completo**: Registra cada uso del comando para monitoreo de administradores

#### **Mensajes de Error:**
- "Este comando solo puede usarse en el End" - Si se intenta usar fuera del End
- "Debes esperar X segundos..." - Si está en cooldown
- "No hay un ciclo activo disponible" - Si no se detecta ciclo (error de configuración)
- "El mundo del ciclo activo no está disponible" - Si el mundo no existe (error crítico)

#### **Uso típico:**
```
Jugador en End: /avo volver
→ ✓ ¡Has regresado al Overworld!
→ Fuiste teletransportado al spawn de ciclo2
```

### 2. Sistema de Cooldowns Ampliado

#### Archivos modificados:
**`CooldownManager.java`** - [managers package]

**Cambios:**
- Añadido nuevo tipo `END_ESCAPE` al enum `CooldownType`
- Implementado tiempo de cooldown configurable (30 segundos por defecto)
- Soporte para configuración en `ciclos.yml`:

```yaml
cooldowns:
  cambio_mundo: 10      # 10 segundos
  crear_ciclo: 300      # 5 minutos
  random_tp: 300        # 5 minutos
  end_escape: 30        # 30 segundos (nuevo)
```

### 3. Integración con Sistema de Ciclos

El comando utiliza la infraestructura existente de `CicloManager`:
- `getPlayerCycle(UUID)` - Detecta el ciclo del jugador
- `getActiveCycle()` - Obtiene ciclo activo como fallback
- `isLocationSafe()` - Verifica seguridad del spawn
- `findSafeLocation()` - Busca alternativa segura si es necesario

### 4. Ayuda Actualizada

Añadido en `/avo help`:
```
§6▸ Teleporte
  §e/avo rtp           §7TP aleatorio (1000-5000 bloques)
  §e/avo volver        §7Escapar del End al Overworld
§7¡Usa /avo volver si quedas atrapado en el End!
```

## 📊 Detalles Técnicos

### Archivos Modificados

1. **`ApocalipsisCommand.java`** - [commands package]
   - Línea ~247: Añadidos casos "volver", "overworld", "salir", "escape" al switch principal
   - Línea ~8050: Implementado método `cmdVolver(CommandSender)`
   - Línea ~351: Actualizada ayuda con nuevo comando

2. **`CooldownManager.java`** - [managers package]
   - Línea ~124: Añadido caso `END_ESCAPE` con cooldown de 30s
   - Línea ~149: Añadido `END_ESCAPE` al enum `CooldownType`

### Flujo de Ejecución

```
Player usa /avo volver en End
    ↓
1. Verificar que es jugador (no consola)
    ↓
2. Verificar que está en dimensión End
    ↓
3. Verificar cooldown (30s, skip si admin)
    ↓
4. Detectar ciclo del jugador
    ↓
5. Obtener mundo Overworld del ciclo
    ↓
6. Validar spawn seguro
    ↓
7. Aplicar cooldown
    ↓
8. Teleportar con efectos
    ↓
9. Mostrar mensajes de éxito
    ↓
10. Log para admins
```

### Seguridad y Prevención de Abuso

- **Cooldown de 30 segundos**: Previene spam del comando
- **Solo en End**: No se puede usar para TP rápido en Overworld
- **Logging completo**: Cada uso queda registrado en logs del servidor
- **Bypass para admins**: Permite rescates rápidos si es necesario
- **Verificación de spawn**: Evita teleportar a ubicaciones inseguras

## 🔧 Configuración Recomendada

Añadir al archivo `ciclos.yml`:

```yaml
# Cooldowns de comandos (en segundos)
cooldowns:
  cambio_mundo: 10
  crear_ciclo: 300
  random_tp: 300
  end_escape: 30    # Ajusta según necesidad

# Mensajes (opcional - valores por defecto ya están incluidos)
mensajes:
  cooldown_activo: "&cDebes esperar &e{tiempo}s &cantes de usar este comando nuevamente."
```

## 📈 Beneficios

### Para Jugadores:
- ✅ No más quedarse atrapados en el End indefinidamente
- ✅ Comando intuitivo y fácil de recordar (`/avo volver`)
- ✅ Feedback claro sobre qué está pasando
- ✅ Múltiples aliases (volver/salir/overworld/escape)

### Para Admins:
- ✅ Menos tickets de soporte por jugadores atrapados
- ✅ Logging completo para auditoría
- ✅ Cooldown configurable para ajustar según comunidad
- ✅ Bypass automático para permisos de admin
- ✅ Compatible con sistema de ciclos existente

### Para el Servidor:
- ✅ Reduce frustración de jugadores
- ✅ No requiere intervención manual de moderadores
- ✅ Mantiene coherencia con protecciones del End compartido
- ✅ Performance óptima (async donde es posible)

## 🎮 Casos de Uso

### Caso 1: Jugador nuevo sin portal
```
Jugador entra al End → Muere antes de crear portal → Respawnea en End
Solución: /avo volver → Regresa al Overworld de su ciclo
```

### Caso 2: Portal destruido accidentalmente
```
Jugador destruye portal por error → Queda atrapado
Solución: /avo volver → Escape inmediato
```

### Caso 3: End compartido entre ciclos
```
Jugador entra al End desde ciclo1 → Ciclo cambia a ciclo2
Portal del End sigue apuntando a ciclo1 (protección compartida)
Solución: /avo volver → Regresa automáticamente al ciclo activo correcto
```

## ⚙️ Compatibilidad

- ✅ Compatible con sistema de ciclos multi-mundo
- ✅ Respeta protecciones del End compartido
- ✅ Integrado con `CooldownManager` existente
- ✅ Compatible con permisos de admin (`apocalipsis.ciclo.admin`)
- ✅ No afecta funcionamiento de portales normales

## 🔍 Testing Recomendado

1. **Funcionalidad básica:**
   - [ ] Usar `/avo volver` desde el End → Debe teleportar
   - [ ] Usar `/avo volver` desde Overworld → Debe mostrar error
   - [ ] Usar `/avo volver` dos veces seguidas → Debe respetar cooldown

2. **Múltiples ciclos:**
   - [ ] Jugador en End de ciclo1 → `/avo volver` → Debe ir a overworld de ciclo1
   - [ ] Cambiar ciclo activo → Jugador en End → `/avo volver` → Debe ir al nuevo ciclo activo

3. **Permisos:**
   - [ ] Admin usa `/avo volver` → No debe tener cooldown
   - [ ] Jugador normal usa `/avo volver` → Debe tener cooldown de 30s

4. **Casos extremos:**
   - [ ] Spawn del ciclo en lava → Debe encontrar ubicación segura
   - [ ] No hay ciclo activo → Debe mostrar error apropiado
   - [ ] Mundo del ciclo no existe → Debe mostrar error crítico

## 📝 Notas de Implementación

### Decisiones de Diseño:

1. **¿Por qué 30 segundos de cooldown?**
   - Suficiente para prevenir abuso como método de TP rápido
   - No tan largo que frustre a jugadores legítimos atrapados
   - Configurable por si necesita ajuste

2. **¿Por qué múltiples aliases?**
   - "volver" es intuitivo para hispano-hablantes
   - "overworld" para jugadores que piensan en inglés
   - "salir"/"escape" para situaciones de emergencia
   - Facilita recordar el comando en situación de estrés

3. **¿Por qué solo funciona en End?**
   - Previene uso como sistema de TP general
   - Mantiene propósito de "comando de emergencia"
   - Evita conflictos con otros comandos de teleporte

4. **¿Por qué teleportar al ciclo activo?**
   - Mantiene coherencia con sistema de ciclos
   - Evita jugadores perdidos en ciclos inactivos
   - Simplifica lógica de destino

## 🚀 Próximos Pasos (Opcional)

Posibles mejoras futuras:
- [ ] Añadir estadística de uso del comando por jugador
- [ ] Notificar a admins si un jugador usa el comando muy frecuentemente
- [ ] Implementar `/avo atascado` que funcione en cualquier dimensión
- [ ] Cooldown diferente para Nether si se reportan problemas similares

## 📌 Resumen de Cambios

| Archivo | Cambios | Líneas |
|---------|---------|--------|
| `ApocalipsisCommand.java` | Nuevos aliases + método cmdVolver() + ayuda | +90 líneas |
| `CooldownManager.java` | Nuevo tipo END_ESCAPE + configuración | +2 líneas |
| **Total** | **2 archivos modificados** | **~92 líneas añadidas** |

## ✅ Checklist de Deployment

- [x] Código implementado
- [x] Compilación exitosa
- [x] Cooldown añadido a CooldownManager
- [x] Ayuda actualizada
- [ ] Pruebas en servidor de desarrollo
- [ ] Configurar cooldown en ciclos.yml
- [ ] Informar a jugadores del nuevo comando
- [ ] Monitorear logs después de deployment

---

**Versión**: 1.22.69  
**Tipo**: Feature - Comando de Utilidad  
**Prioridad**: Alta (resuelve problema reportado por usuarios)  
**Impacto**: Positivo - Mejora experiencia de jugador  
**Riesgo**: Bajo - Cambio aislado sin afectar sistemas existentes
