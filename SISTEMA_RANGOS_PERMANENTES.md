# Sistema de Rangos Permanentes v1.0

## 📋 Descripción General

Sistema completo de rangos personalizados independiente del sistema de rangos por XP. Permite crear rangos permanentes o temporales con efectos, prefijos personalizados y prioridades.

## ⚙️ Características

### ✅ Completamente Funcional
- ✅ Crear rangos personalizados con nombre, color y efectos
- ✅ Asignar rangos a jugadores (permanente o temporal)
- ✅ Sistema de prioridades (rangos permanentes > rangos normales)
- ✅ Efectos de poción continuos
- ✅ Prefijos personalizados en tab y chat
- ✅ Auto-expiración de rangos temporales
- ✅ Persistencia en YAML
- ✅ Comandos solo para OPs

### 🎯 Uso

#### Comandos Disponibles

**1. Crear un rango nuevo**
```
/avo newrank <id> <tipo>
```
- `id`: Identificador único del rango (ej: vip, admin, streamer)
- `tipo`: "permanente" o "temporal"

Ejemplo:
```
/avo newrank vip permanente
/avo newrank moderador temporal
```

Después de crear el rango, edita `rangos_permanentes.yml` para personalizar:
- Display name y color
- Prefijos y sufijos de tab/chat
- Efectos de poción
- Prioridad (mayor = más importante)

**2. Asignar rango a un jugador**
```
/avo setpermrank <jugador> <rango> [tiempo]
```
- `jugador`: Nombre del jugador (debe estar online)
- `rango`: ID del rango a asignar
- `tiempo`: (Opcional) Duración del rango
  - Sin especificar = permanente
  - Formatos: `30d` (días), `24h` (horas), `60m` (minutos)

Ejemplos:
```
/avo setpermrank Steve vip              → VIP permanente
/avo setpermrank Alex moderador 7d      → Moderador por 7 días
/avo setpermrank Herobrine admin 24h    → Admin por 24 horas
```

**3. Remover rango de un jugador**
```
/avo removepermrank <jugador>
```

Ejemplo:
```
/avo removepermrank Steve
```

**4. Listar rangos disponibles**
```
/avo listpermranks
```

Muestra todos los rangos con sus características.

## 📝 Configuración (rangos_permanentes.yml)

### Estructura del Archivo

```yaml
configuracion:
  enabled: true
  
  tab_format:
    compacto: true  # Solo mostrar el prefijo principal, no repetir rango
    
  actualizar_efectos_ticks: 100      # Cada cuánto aplicar efectos (5 segundos)
  duracion_efectos_segundos: 15       # Duración de cada efecto aplicado

rangos:
  vip:
    display_name: "§6[VIP]"
    tab_prefix: "§6[VIP] "
    tab_suffix: ""
    chat_prefix: "§6[VIP] "
    color: "§6"
    prioridad: 50                       # Prioridad del rango (mayor = mejor)
    heredar_efectos_rango_normal: false # ¿Heredar efectos del rango de XP?
    efectos:
      - "SPEED:1"                       # Velocidad I
      - "NIGHT_VISION:1"                # Visión nocturna I
      - "REGENERATION:1"                # Regeneración I
  
  streamer:
    display_name: "§5✦ STREAMER ✦"
    tab_prefix: "§5✦ "
    tab_suffix: " §5✦"
    chat_prefix: "§5✦ STREAMER ✦ "
    color: "§5"
    prioridad: 100
    heredar_efectos_rango_normal: true
    efectos:
      - "SPEED:2"
      - "NIGHT_VISION:1"
      - "REGENERATION:1"
      - "RESISTANCE:1"
  
  moderador:
    display_name: "§3[MOD]"
    tab_prefix: "§3[MOD] "
    tab_suffix: ""
    chat_prefix: "§3[MOD] "
    color: "§3"
    prioridad: 75
    heredar_efectos_rango_normal: false
    efectos:
      - "SPEED:1"
      - "NIGHT_VISION:1"
  
  admin:
    display_name: "§c[ADMIN]"
    tab_prefix: "§c[A] "
    tab_suffix: ""
    chat_prefix: "§c[ADMIN] "
    color: "§c"
    prioridad: 90
    heredar_efectos_rango_normal: false
    efectos:
      - "SPEED:2"
      - "NIGHT_VISION:1"
      - "REGENERATION:1"
      - "DAMAGE_RESISTANCE:1"
  
  builder:
    display_name: "§e[BUILDER]"
    tab_prefix: "§e[B] "
    tab_suffix: ""
    chat_prefix: "§e[BUILDER] "
    color: "§e"
    prioridad: 60
    heredar_efectos_rango_normal: false
    efectos:
      - "NIGHT_VISION:1"
      - "HASTE:2"

asignaciones: {}  # Auto-gestionado por el plugin
```

### Efectos de Poción Disponibles

Formato: `"NOMBRE_EFECTO:NIVEL"`

**Efectos Comunes:**
- `SPEED:1` - Velocidad I
- `SPEED:2` - Velocidad II
- `NIGHT_VISION:1` - Visión nocturna
- `REGENERATION:1` - Regeneración I
- `RESISTANCE:1` - Resistencia I
- `DAMAGE_RESISTANCE:1` - Resistencia al daño I
- `HASTE:1` - Prisa I
- `HASTE:2` - Prisa II
- `ABSORPTION:1` - Absorción I
- `HEALTH_BOOST:1` - Salud extra I
- `JUMP_BOOST:1` - Salto mejorado I

## 🔧 Funcionamiento Interno

### Prioridades
El sistema usa prioridades para determinar qué rango mostrar:
1. Si el jugador tiene un rango permanente, se muestra ese (independiente del XP)
2. Si tiene múltiples rangos permanentes, se muestra el de mayor prioridad
3. Si no tiene rango permanente, se muestra el rango normal de XP
4. Los efectos se aplican del rango con mayor prioridad

### Auto-Expiración
Los rangos temporales se verifican automáticamente:
- Al conectarse el jugador
- Al consultar el rango
- Si expiró, se remueve automáticamente

### Aplicación de Efectos
Los efectos de poción se aplican cada 5 segundos (100 ticks) con duración de 15 segundos para mantener efectos permanentes sin parpadeo.

### Persistencia
Las asignaciones de rangos se guardan automáticamente en `rangos_permanentes.yml` en la sección `asignaciones`:
```yaml
asignaciones:
  550e8400-e29b-41d4-a716-446655440000:  # UUID del jugador
    rank_id: "vip"
    assigned_date: "2025-01-15T10:30:00Z"
    expires_date: "2025-02-15T10:30:00Z"  # null si es permanente
```

## 🎨 Formato Tab List

El formato compacto (recomendado) muestra:
```
[VIP] Steve          (rango permanente)
[🏛️ VETERANO] Alex   (rango normal de XP)
✦ Herobrine ✦        (rango streamer con sufijos)
```

Sin formato compacto, muestra el displayName completo.

## ⚡ Performance

- Cache en memoria de rangos y asignaciones
- Una sola tarea repetitiva para efectos de todos los jugadores
- Solo carga YAML al iniciar/recargar
- Actualizaciones de tab optimizadas

## 🔄 Reload

Para recargar la configuración después de editar `rangos_permanentes.yml`:
```
/avo reload
```

Esto recarga:
- Definiciones de rangos
- Asignaciones de jugadores
- Configuración de efectos y formato

## 📊 Ejemplos de Uso

### Caso 1: Dar VIP a un jugador por 30 días
```
1. /avo setpermrank Steve vip 30d
2. Steve ahora tiene [VIP] en su nombre en el tab
3. Steve recibe Speed I, Night Vision I y Regeneration I
4. Después de 30 días, el rango expira automáticamente
```

### Caso 2: Dar rango de moderador permanente
```
1. /avo setpermrank Alex moderador
2. Alex tiene [MOD] permanentemente
3. Recibe efectos configurados en el YAML
```

### Caso 3: Crear un rango personalizado
```
1. /avo newrank helper permanente
2. Editar rangos_permanentes.yml:
   helper:
     display_name: "§a[HELPER]"
     tab_prefix: "§a[H] "
     tab_suffix: ""
     chat_prefix: "§a[HELPER] "
     color: "§a"
     prioridad: 40
     heredar_efectos_rango_normal: false
     efectos:
       - "SPEED:1"
3. /avo reload
4. /avo setpermrank Bob helper 7d
```

## 🐛 Troubleshooting

**Problema:** El rango no aparece en el tab
- Verificar que el jugador esté online
- Verificar que el rango existe con `/avo listpermranks`
- Recargar con `/avo reload`

**Problema:** Los efectos no se aplican
- Verificar formato de efectos: `"NOMBRE:NIVEL"`
- Verificar que `enabled: true` en la configuración
- Verificar que `actualizar_efectos_ticks` no sea muy alto

**Problema:** El rango temporal no expira
- Verificar formato de tiempo: `30d`, `24h`, `60m`
- El rango expira al conectarse o al consultarlo
- Verificar fecha de expiración en el YAML

## 📚 Notas Técnicas

### Archivos Modificados
- `src/main/java/me/apocalipsis/commands/ApocalipsisCommand.java` - Comandos
- `src/main/java/me/apocalipsis/commands/AvoTabCompleter.java` - Tab completion
- `src/main/java/me/apocalipsis/missions/PermRankManager.java` - Lógica principal
- `src/main/java/me/apocalipsis/listeners/PlayerListener.java` - Hook en join
- `src/main/java/me/apocalipsis/Apocalipsis.java` - Registro del manager
- `src/main/resources/rangos_permanentes.yml` - Configuración

### Dependencias
- Bukkit API 1.21.3-R0.1
- Requiere Java 21+

### Thread Safety
El sistema usa operaciones síncronas para garantizar thread safety en Bukkit.

---

**Version:** 1.0  
**Fecha:** Enero 2025  
**Estado:** ✅ Completamente funcional y testeado
