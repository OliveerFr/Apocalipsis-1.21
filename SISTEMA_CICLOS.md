# 🔄 SISTEMA DE CICLOS MULTI-MUNDO

## 📖 Descripción General

El **Sistema de Ciclos** es una característica avanzada que permite crear **mundos independientes** donde los jugadores pueden empezar completamente desde cero, manteniendo intacto su progreso en el mundo original.

### 🎯 Casos de Uso

- **Reset Periódico**: Crear temporadas/ciclos donde todos empiezan de cero
- **Eventos Especiales**: Mundos temporales para eventos sin afectar el progreso principal
- **Testing**: Probar mecánicas sin riesgo al progreso real
- **Competencias**: Mundos de competencia donde todos parten iguales

---

## 🏗️ Arquitectura del Sistema

### Componentes Principales

```
┌─────────────────────────────────────────────────────────────┐
│                      CICLO MANAGER                          │
│         (Orquestador principal del sistema)                 │
└──────────────┬──────────────────────────────────────────────┘
               │
      ┌────────┴────────┐
      │                 │
┌─────▼────┐      ┌────▼─────┐      ┌──────────┐
│ World    │      │  World   │      │   Item   │
│Inventory │      │   Data   │      │Sanitizer │
│ Manager  │      │ Manager  │      │          │
└──────────┘      └──────────┘      └──────────┘
      │                 │                  │
      │                 │                  │
┌─────▼─────────────────▼──────────────────▼──────┐
│           WORLD CHANGE LISTENER                  │
│      (Detecta cambios de mundo y coordina)       │
└──────────────────────────────────────────────────┘
      │
      │
┌─────▼─────────────────────────────────────────┐
│      WORLD PROTECTION LISTENER                │
│  (Bloquea acciones que permitan transferir)   │
└───────────────────────────────────────────────┘
```

---

## 🔐 Sistema de Seguridad Multi-Capa

### Capa 1: Separación de Datos

Cada mundo mantiene sus propios datos de jugador:

- ✅ **Inventario completo** (main, armor, offhand)
- ✅ **XP y Nivel** del plugin
- ✅ **Skills compradas** y sus niveles
- ✅ **Misiones activas** y completadas
- ✅ **PS (Puntos de Supervivencia)**
- ✅ **Rangos** (vuelven a NOVATO en ciclos nuevos)
- ✅ **Backpacks** (contenido separado por mundo)
- ✅ **Ender Chest** (separado por mundo)

### Capa 2: Limpieza al Cambiar de Mundo

Cuando un jugador cambia de un mundo a otro:

1. **Sanitizar inventario** → Elimina items problemáticos
2. **Guardar inventario** del mundo actual
3. **Guardar datos de progreso** (XP, skills, etc.)
4. **Cargar inventario** del nuevo mundo
5. **Aplicar datos de progreso** del nuevo mundo

### Capa 3: Bloqueo de Transferencias

Items y acciones bloqueadas:

| Item/Acción | ¿Bloqueado? | Motivo |
|-------------|-------------|--------|
| Shulker Boxes (con contenido) | ✅ SÍ | Permiten almacenar 27 slots de items |
| Bundles | ✅ SÍ | Permiten almacenar hasta 64 items |
| Ender Chest (vanilla) | ✅ SÍ | Inventario global entre mundos |
| Backpack (plugin) | ⚠️ SEPARADO | Cada mundo tiene su propio backpack |
| Minecarts con cofre/tolva | ✅ SÍ | Contienen inventario |

### Capa 4: Validación Continua

El `ItemSanitizer` verifica constantemente:

- Items con inventario interno
- Shulker Boxes vacías vs. llenas
- Bundles vacíos vs. con contenido
- Items personalizados problemáticos

---

## 📁 Archivos de Datos

El sistema crea y gestiona los siguientes archivos:

### `ciclos.yml`
Configuración principal del sistema de ciclos:
- Mundos de ciclo registrados
- Protecciones activas
- Mensajes personalizados
- Materiales bloqueados

### `world_inventories.yml`
Almacena inventarios de jugadores por mundo:
```yaml
<UUID>:
  world:
    inventory: [...]
    armor: [...]
    offhand: {...}
    level: 30
    exp: 0.75
    food: 20
    saturation: 5.0
    health: 20.0
  world_ciclo_1:
    inventory: [...]
    # ... datos del ciclo
```

### `world_data.yml`
Almacena progreso de jugadores por mundo:
```yaml
<UUID>:
  world:
    xp: 15000
    nivel: 22
    skills: [...]
    skill_levels: {...}
    ps: 500
    rango: "VETERANO"
  world_ciclo_1:
    xp: 0
    nivel: 1
    skills: []
    ps: 0
    rango: "NOVATO"
```

---

## 🎮 Comandos

### Para Administradores

#### Activar un Nuevo Ciclo
```
/avo ciclo nuevo <nombre_mundo> [teleport]
```
- **nombre_mundo**: Nombre del mundo en Multiverse
- **teleport**: `true` para teleportar a todos los jugadores
- **Requiere**: `apocalipsis.admin`
- **Tab Completer**: Autocompleta mundos disponibles y true/false

**Ejemplo:**
```
/avo ciclo nuevo world_ciclo_1 true
```

#### Desactivar un Ciclo
```
/avo ciclo desactivar <nombre_mundo>
```
- **Requiere**: `apocalipsis.admin`
- **Tab Completer**: Autocompleta mundos activos

#### Listar Ciclos Activos
```
/avo ciclo listar
```
- **Requiere**: `apocalipsis.admin`
- **Muestra**: Ciclos activos, jugadores por mundo, mundo original

#### Ver Información de un Mundo
```
/avo ciclo info <nombre_mundo>
```
- **Requiere**: `apocalipsis.admin`
- **Tab Completer**: Autocompleta todos los mundos
- **Muestra**: Tipo, jugadores, dificultad, PvP, tiempo

#### Teleportarse a un Mundo
```
/avo ciclo teleport <nombre_mundo>
```
- **Requiere**: `apocalipsis.ciclo.admin` (solo para cambiar entre ciclos)
- **Tab Completer**: Autocompleta todos los mundos
- **🔒 RESTRICCIÓN**: Solo admins con `apocalipsis.ciclo.admin` pueden teleportarse entre ciclos diferentes
- **Nota**: Los jugadores normales NO pueden cambiar entre ciclos para evitar transferencia de items

**Todos los comandos incluyen Tab Completer completo:**
- Subcomandos disponibles
- Nombres de mundos válidos
- Parámetros contextuales (true/false, etc.)

---

## 🔧 Configuración

### Ejemplo de `ciclos.yml`

```yaml
config:
  enabled: true
  mundo_original: "world"
  backup_before_cycle: true
  debug: false

protecciones:
  bloquear_enderchest_vanilla: true
  bloquear_shulker_boxes: true
  bloquear_bundles: true
  sanitizar_items: true
  
  materiales_bloqueados:
    - "SHULKER_BOX"
    - "WHITE_SHULKER_BOX"
    # ... todas las shulker boxes
    - "BUNDLE"

reseteo:
  resetear_xp: true
  resetear_rangos: true
  resetear_skills: true
  resetear_misiones: true
  resetear_ps: true
  xp_inicial: 0
  nivel_inicial: 1
  ps_inicial: 0

ciclos:
  world:
    nombre_display: "&aWorld Original"
    tipo: "original"
    activo: true
    
  world_ciclo_1:
    nombre_display: "&6Ciclo 1: &eNuevo Comienzo"
    tipo: "ciclo"
    activo: true
    fecha_creacion: "2026-01-25"
```

---

## � Sistema de Permisos

El sistema de ciclos implementa un modelo de permisos jerárquico para controlar el acceso:

### Permisos Disponibles

#### `apocalipsis.admin`
- **Nivel**: Administrador General
- **Permite**:
  - ✅ Crear y desactivar ciclos
  - ✅ Listar ciclos activos
  - ✅ Ver información de mundos
  - ✅ Acceso a todos los comandos administrativos básicos
- **No permite**:
  - ❌ Teleportarse entre ciclos diferentes (requiere ciclo.admin)

#### `apocalipsis.ciclo.admin`
- **Nivel**: Administrador de Ciclos (Super Admin)
- **Permite**:
  - ✅ Todo lo de `apocalipsis.admin`
  - ✅ **Teleportarse libremente entre ciclos diferentes**
  - ✅ Bypass completo de restricciones de movimiento
- **Uso recomendado**: Solo para staff senior/owner
- **Razón**: Previene que admins regulares transfieran items accidentalmente

#### `apocalipsis.ciclo.bypass`
- **Nivel**: Bypass de Protecciones
- **Permite**:
  - ✅ Usar Ender Chests en mundos restringidos
  - ✅ Colocar Shulker Boxes
  - ✅ Usar Bundles con contenido
- **⚠️ ADVERTENCIA**: Puede romper la separación de items entre mundos
- **Uso recomendado**: NO otorgar a nadie, o solo para debugging

### Configuración Recomendada

```yaml
# permissions.yml o tu plugin de permisos
groups:
  Owner:
    permissions:
      - apocalipsis.admin
      - apocalipsis.ciclo.admin
      # NO dar ciclo.bypass para mantener integridad
  
  Admin:
    permissions:
      - apocalipsis.admin
      # NO dar ciclo.admin para prevenir transferencias
  
  Moderator:
    permissions:
      - apocalipsis.admin  # Solo si necesitan gestionar ciclos
  
  Default:
    permissions:
      # Ningún permiso de ciclo - jugadores normales
```

### Restricciones de Teleporte

El sistema implementa restricciones especiales para el comando `teleport`:

```
Escenario 1: Jugador normal intenta teleportarse
  → ❌ Bloqueado - Solo admins

Escenario 2: Admin con apocalipsis.admin intenta teleportarse al mismo mundo original
  → ✅ Permitido - No hay cambio de ciclo

Escenario 3: Admin con apocalipsis.admin intenta cambiar entre ciclos
  → ❌ Bloqueado - Requiere apocalipsis.ciclo.admin
  → Mensaje: "Solo los administradores pueden teleportarse entre ciclos diferentes"

Escenario 4: Super Admin con apocalipsis.ciclo.admin cambia entre ciclos
  → ✅ Permitido - Acceso completo
```

**Razón de la restricción**: Prevenir que administradores transfieran items accidentalmente entre ciclos al teleportarse. Solo super admins con conocimiento completo del sistema deben poder hacerlo.

---

## �🚀 Flujo de Activación de un Ciclo

### Paso a Paso

1. **Admin ejecuta comando**
   ```
   /avo ciclo nuevo world_ciclo_1 true
   ```

2. **Sistema verifica requisitos**
   - ✅ Multiverse-Core instalado
   - ✅ El mundo existe
   - ✅ Sistema de ciclos habilitado

3. **Backup automático** (si está configurado)
   - Crea respaldo de `mission_data.yml`
   - Crea respaldo de `skill_data.yml`
   - Guarda timestamp

4. **Registro del ciclo**
   - Añade el mundo a la lista de ciclos
   - Actualiza `ciclos.yml`
   - Marca como activo

5. **Teleporte masivo** (si se solicitó)
   - Para cada jugador online:
     - Guarda inventario del mundo actual
     - Guarda progreso del mundo actual
     - Teleporta al spawn del nuevo mundo
     - El listener maneja la carga de datos

6. **Confirmación**
   - Mensaje de éxito al admin
   - Log en consola
   - Jugadores informados

---

## 🔄 Flujo de Cambio de Mundo

### Cuando un Jugador Cambia de Mundo

```
1. TRIGGER: PlayerChangedWorldEvent
   │
   ├─→ 2. GUARDAR MUNDO ANTERIOR
   │    ├─ Sanitizar inventario (remover items problemáticos)
   │    ├─ Guardar inventario completo
   │    ├─ Capturar estado actual (XP, skills, PS, etc.)
   │    └─ Guardar a world_inventories.yml y world_data.yml
   │
   ├─→ 3. CARGAR NUEVO MUNDO
   │    ├─ Cargar inventario del nuevo mundo (o crear vacío)
   │    ├─ Cargar datos de progreso (o crear frescos si es ciclo)
   │    └─ Aplicar datos a los servicios del plugin
   │
   └─→ 4. NOTIFICAR
        ├─ Mensaje de salida del mundo anterior
        ├─ Mensaje de entrada al nuevo mundo
        └─ Log si debug está activo
```

---

## 🛡️ Protecciones Activas

### Bloqueos Implementados

#### 1. Ender Chest Vanilla
- ❌ No se puede abrir en mundos de ciclo
- ❌ No se puede interactuar con bloques de Ender Chest
- ✅ Mensaje informativo al jugador

#### 2. Shulker Boxes
- ❌ No se pueden colocar (ni vacías ni llenas)
- ✅ Detecta todas las 17 variantes de color
- ✅ Mensaje informativo al jugador

#### 3. Bundles
- ❌ No se pueden usar en mundos de ciclo
- ✅ Solo si contienen items
- ✅ Mensaje informativo al jugador

#### 4. Bypass para Admins
Los jugadores con permiso `apocalipsis.ciclo.bypass` pueden:
- ✅ Usar Ender Chest vanilla
- ✅ Colocar Shulker Boxes
- ✅ Usar Bundles
- ⚠️ **Usar con cuidado - puede romper la separación**

---

## ⚙️ Integración con Servicios Existentes

### ExperienceService
- `setXP(uuid, xp)` - Aplicar XP del mundo
- `setLevel(uuid, nivel)` - Aplicar nivel del mundo

### SkillService
- Reseteo completo de skills en ciclos nuevos
- Restauración de skills al volver al mundo original

### MissionService
- PS separados por mundo
- Misiones independientes por mundo

### RankService
- Rangos calculados basados en XP del mundo actual
- Rango NOVATO por defecto en ciclos nuevos

---

## 📊 Mejores Prácticas

### Para Administradores

1. **Antes de Activar un Ciclo**
   - ✅ Verificar que Multiverse-Core esté instalado
   - ✅ Crear el mundo en Multiverse primero
   - ✅ Configurar spawn del nuevo mundo
   - ✅ Anunciar a los jugadores con anticipación

2. **Durante un Ciclo Activo**
   - ✅ Monitorear logs para detectar problemas
   - ✅ Hacer backups regulares
   - ✅ Revisar `world_inventories.yml` y `world_data.yml`

3. **Al Finalizar un Ciclo**
   - ✅ Anunciar con tiempo
   - ✅ Permitir que jugadores guarden su progreso
   - ✅ Desactivar el ciclo: `/avo ciclo desactivar <mundo>`
   - ✅ Opcionalmente, eliminar archivos de datos del ciclo

### Para Jugadores

1. **Al Entrar a un Ciclo Nuevo**
   - ⚠️ TODO tu progreso será reseteado en este mundo
   - ✅ Tu progreso en el mundo original está seguro
   - ✅ No puedes transferir items entre mundos

2. **Durante el Ciclo**
   - ❌ No intentes usar Shulker Boxes
   - ❌ No intentes usar Ender Chest vanilla
   - ✅ Usa `/mochila` (separado por mundo)

3. **Al Regresar al Mundo Original**
   - ✅ Todo tu progreso será restaurado
   - ✅ Inventario completo restaurado
   - ✅ XP, skills, PS restaurados

---

## 🐛 Troubleshooting

### Problemas Comunes

#### "Multiverse-Core no está instalado"
**Solución:** Instala Multiverse-Core desde SpigotMC

#### "El mundo no existe"
**Solución:** 
1. Crea el mundo primero con Multiverse
2. Verifica el nombre exacto con `/mv list`
3. Usa el nombre exacto en `/avo ciclo nuevo`

#### "Los jugadores no se teleportaron"
**Solución:**
- Asegúrate de usar `true` como tercer parámetro
- Ejemplo: `/avo ciclo nuevo world_ciclo_1 true`

#### "Los items no se guardan"
**Solución:**
1. Verifica que no hay errores en consola
2. Revisa `world_inventories.yml` - debe existir
3. Comprueba permisos de escritura en la carpeta del plugin

#### "El jugador perdió items"
**Solución:**
1. Revisa `world_inventories.yml`
2. Los items están guardados por mundo
3. Al regresar al mundo correcto, se restauran
4. Si es necesario, restaura desde backup

---

## 📝 Notas Técnicas

### Rendimiento

- Los inventarios se guardan/cargan solo al cambiar de mundo
- Los datos se guardan en memoria durante la sesión
- Escritura a disco solo en shutdown o cambios importantes
- Cache eficiente para evitar I/O excesivo

### Persistencia

- **Automática**: Al cambiar de mundo, logout, shutdown
- **Manual**: Se puede forzar guardado con `/avo backup`

### Compatibilidad

- ✅ Compatible con Multiverse-Core
- ✅ Compatible con otros plugins de mundos
- ⚠️ Puede tener conflictos con plugins de economía global
- ⚠️ Plugins de clanes/facciones pueden necesitar configuración

---

## 🎨 Ejemplo de Uso: Evento de Temporada

### Escenario

Quieres hacer un evento mensual donde todos empiezan de cero.

### Pasos

1. **Preparación** (Día 1)
   ```
   /mv create world_temporada_1 NORMAL
   /avo ciclo nuevo world_temporada_1 false
   ```

2. **Anuncio** (Días 1-3)
   ```
   Anuncia el evento en Discord/servidor
   Explica que el progreso se resetea
   Fecha de inicio: Día 5
   ```

3. **Inicio del Evento** (Día 5)
   ```
   /avo ciclo nuevo world_temporada_1 true
   ```
   - Todos los jugadores son teleportados
   - Todos empiezan desde nivel 1, sin items
   - Progreso del mundo original intacto

4. **Durante el Evento** (Días 5-30)
   - Los jugadores juegan en el mundo de temporada
   - Todo funciona normal (misiones, skills, etc.)
   - Los datos están separados del mundo original

5. **Fin del Evento** (Día 30)
   ```
   Anuncia que el evento termina
   /avo ciclo desactivar world_temporada_1
   Teleporta jugadores de vuelta: /avo ciclo teleport world
   ```
   - Progreso del mundo original restaurado
   - Opcionalmente, guarda los datos de la temporada para premios

---

## 📞 Soporte

Para problemas o preguntas:
- Revisa los logs en `logs/latest.log`
- Activa debug mode en `ciclos.yml`
- Revisa los archivos de datos en `plugins/Apocalipsis/`

---

## 📄 Licencia

Este sistema es parte del plugin Apocalipsis.  
© 2026 - Todos los derechos reservados.
