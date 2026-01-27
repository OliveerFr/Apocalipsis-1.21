# 🌍 Guía Rápida - Sistema de Ciclos Multi-Mundo

## ¿Qué es el Sistema de Ciclos?

El sistema de ciclos permite **resetear completamente el servidor** sin perder el progreso de los jugadores. Cada "ciclo" es un mundo independiente donde:
- Los jugadores mantienen inventarios separados
- XP, skills, misiones y rangos se mantienen separados
- Se puede volver al ciclo anterior en cualquier momento

## 🎯 Comandos Principales

### Crear y Activar un Nuevo Ciclo

```bash
# Opción 1: Creación automática (recomendado)
/avo ciclo nuevo <nombre> [teleport true/false]

# Opción 2: Creación manual avanzada
/avo ciclo crear <nombre> [NORMAL|NETHER|END] [EASY|NORMAL|HARD]
```

**Ejemplos:**
```bash
/avo ciclo nuevo temporada_2
/avo ciclo nuevo temporada_2 true          # Teletransporta a todos automáticamente
/avo ciclo crear temporada_2 NORMAL HARD   # Mundo normal con dificultad difícil
/avo ciclo crear nether_event NETHER HARD  # Mundo del Nether
```

### Consultar Ciclo Actual

```bash
/avo ciclo info
```

Muestra:
- ✓ Nombre del ciclo activo
- ✓ Mundo asociado
- ✓ Fecha de activación
- ✓ Número de jugadores que han entrado

### Viajar Entre Ciclos

```bash
/avo ciclo tp <nombre_ciclo>
```

**Importante:** 
- Solo admins pueden viajar entre ciclos
- Tu inventario y progreso cambiarán al ciclo correspondiente
- No puedes llevar items de un ciclo a otro

### Desactivar el Ciclo Actual

```bash
/avo ciclo desactivar
```

**Efecto:**
- Los jugadores pueden moverse libremente entre mundos
- Se desactivan las protecciones de transferencia de items
- Útil para mantenimiento o eventos especiales

## 🛡️ Protecciones Automáticas

El sistema bloquea automáticamente:

### Inventarios Especiales
- ❌ Ender Chest (bloqueado completamente)
- ❌ Shulker Boxes (no pueden abrirse)
- ❌ Bundles (no pueden usarse)
- ❌ Mochilas de otros plugins

### Comandos Peligrosos
- ❌ `/give` - Dar items
- ❌ `/item` - Manipular items
- ❌ `/summon minecraft:item` - Invocar items
- ❌ `/clear` - Limpiar inventarios
- ❌ `/replaceitem` - Reemplazar items

### Transferencia con Entidades
- ❌ Montar caballos/burros/llamas con cofres
- ❌ Teletransportarse con animales que llevan items
- ❌ Usar portales con entidades que llevan inventory

### Almacenamiento Temporal
- ❌ Item Frames (marcos para items)
- ❌ Armor Stands (soportes de armadura)

## 📊 Datos que se Separan por Ciclo

Cada ciclo mantiene **registros independientes** de:

1. **Inventario**
   - Inventario principal (27 slots)
   - Hotbar (9 slots)
   - Armadura (4 slots)
   - Off-hand (1 slot)

2. **Experiencia y Nivel**
   - Nivel actual
   - Puntos de XP
   - Barra de progreso

3. **Skills**
   - Skills desbloqueadas
   - Niveles de cada skill
   - Árboles de habilidades

4. **Misiones**
   - Puntos de Supervivencia (PS)
   - Progreso de misiones
   - Misiones completadas
   - Penalizaciones activas

5. **Rangos**
   - Rango actual
   - Rangos permanentes
   - Tiempo acumulado en cada rango

## 🚨 Casos de Uso Comunes

### Reseteo de Temporada

```bash
# Paso 1: Crear nuevo ciclo
/avo ciclo nuevo temporada_3 false

# Paso 2: Avisar a los jugadores (en chat/Discord)
"¡Nueva temporada comienza en 5 minutos!"

# Paso 3: Teletransportar a todos
/avo ciclo tp temporada_3
```

### Volver a un Ciclo Anterior

```bash
# Ejemplo: Volver a la temporada 1 para un evento
/avo ciclo tp temporada_1

# Cuando termine el evento
/avo ciclo tp temporada_3
```

### Mantenimiento Temporal

```bash
# Desactivar ciclo para mantenimiento
/avo ciclo desactivar

# Hacer cambios en el mundo...

# Reactivar ciclo
/avo ciclo nuevo temporada_3_post_mantenimiento
```

## ⚙️ Configuración Avanzada

### Archivo: `ciclos.yml`

```yaml
ciclos:
  temporada_1:
    mundo: world_ciclo_1
    fecha_activacion: 1640995200000
    activo: false
    jugadores_visitados: 156
  temporada_2:
    mundo: world_ciclo_2
    fecha_activacion: 1672531200000
    activo: true
    jugadores_visitados: 203
```

### Archivo: `config.yml` (Sección de Ciclos)

```yaml
ciclo:
  multiverse_integration: true      # Usar Multiverse-Core
  auto_create_worlds: true          # Crear mundos automáticamente
  default_environment: NORMAL       # Tipo de mundo por defecto
  default_difficulty: HARD          # Dificultad por defecto
  generate_structures: true         # Generar estructuras (aldeas, etc)
  backup_on_cycle_change: true      # Backup automático al cambiar ciclo
  
  # NBT Tracking (Protección avanzada)
  use_world_tagging: true           # Etiquetar items con mundo de origen
  quarantine_suspicious_items: true # Cuarentena para items sospechosos
  
  # Lista blanca de items permitidos entre mundos
  whitelist:
    - DIAMOND
    - NETHERITE_INGOT
    - ENCHANTED_BOOK
```

## 🔧 Troubleshooting

### "El mundo no existe"
- **Solución:** Usa `/avo ciclo crear` en lugar de `/avo ciclo nuevo`
- **Causa:** El mundo no fue creado por Multiverse

### "No puedes abrir eso aquí"
- **Solución:** Es una protección del sistema. Usa cofres normales.
- **Causa:** Intentaste abrir Ender Chest o Shulker Box

### "Ese comando está bloqueado"
- **Solución:** Usa el sistema normal de juego, no comandos de admin
- **Causa:** Los comandos de items están bloqueados en ciclos

### Mi inventario está vacío al cambiar de ciclo
- **Solución:** Es normal. Cada ciclo tiene su propio inventario.
- **Verificación:** Vuelve al ciclo anterior con `/avo ciclo tp <ciclo_anterior>`

### No puedo montar mi caballo con cofre
- **Solución:** Es una protección anti-transferencia de items
- **Workaround:** Usa caballos sin cofre o almacena items en cofres normales

## 📝 Permisos

```yaml
# Administradores de ciclos
apocalipsis.ciclo.admin          # Crear, desactivar, teleportarse

# Bypass de protecciones (NO RECOMENDADO)
apocalipsis.ciclo.bypass         # Ignorar todas las protecciones
```

## 🎓 Tips Profesionales

1. **Anuncia con anticipación:** Avisa a los jugadores 24h antes de cambiar ciclo
2. **Haz backups:** Siempre haz backup antes de crear un nuevo ciclo
3. **Documenta tus ciclos:** Mantén un registro de qué ciclo fue qué temporada
4. **Prueba primero:** Crea un ciclo de prueba antes del oficial
5. **Usa nombres descriptivos:** `temporada_3_navidad` mejor que `mundo3`

## 📞 Soporte

- Archivo de logs: `logs/latest.log`
- Configuración: `plugins/Apocalipsis/ciclos.yml`
- Inventarios: `plugins/Apocalipsis/world_inventories.yml`
- Datos: `plugins/Apocalipsis/world_data.yml`

---

**Versión del Sistema:** 1.22.55  
**Última actualización:** 26 Enero 2026
