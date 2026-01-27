aña# Guía de Seguridad del Sistema de Ciclos

**Documento**: Manual de Administración y Seguridad  
**Versión**: 1.22.55  
**Audiencia**: Administradores de Servidor

---

## 🎯 Índice Rápido

1. [Verificación Diaria de Seguridad](#verificación-diaria)
2. [Problemas Comunes y Soluciones](#problemas-comunes)
3. [Comandos de Diagnóstico](#comandos-de-diagnóstico)
4. [Monitoreo de Logs](#monitoreo-de-logs)
5. [Mejores Prácticas](#mejores-prácticas)
6. [Respuesta a Incidentes](#respuesta-a-incidentes)

---

## 🔍 Verificación Diaria de Seguridad

### Checklist Rápido (5 minutos)

```bash
# 1. Verificar estado de seguridad
/avo ciclo security

# 2. Ver información de ciclos
/avo ciclo info

# 3. Revisar logs recientes
# Buscar en console.log: [SEGURIDAD]
```

### ✅ Indicadores de Sistema Saludable

- ✓ Solo 1 ciclo marcado como activo
- ✓ Spawn del ciclo activo es seguro
- ✓ No hay advertencias de cooldowns en logs
- ✓ No hay errores de mundos no cargados

### ⚠️ Señales de Alerta

- ✗ Múltiples ciclos activos simultáneos
- ✗ Logs frecuentes de "spawn no seguro"
- ✗ Jugadores reportan teleportes a void/lava
- ✗ Logs de "mundo no cargado" repetidos

---

## 🚨 Problemas Comunes y Soluciones

### Problema 1: Múltiples Ciclos Activos

**Síntomas**:
```
[SEGURIDAD] ✗ FALLO DE VALIDACIÓN: 2 ciclos activos simultáneos
```

**Causa**: Corrupción de datos o activación manual incorrecta

**Solución**:
```bash
# 1. Identificar ciclos activos
/avo ciclo info

# 2. Desactivar el ciclo INCORRECTO (el más viejo)
/avo ciclo desactivar <nombre_mundo_viejo>

# 3. Verificar que solo quede 1
/avo ciclo security
```

**Prevención**:
- Usar SIEMPRE `/avo ciclo activar` (nunca editar archivos manualmente)
- Hacer backup antes de cambiar ciclos

---

### Problema 2: Spawn Peligroso

**Síntomas**:
```
[SEGURIDAD] Ubicación de spawn no es segura en 'ciclo_1'
  └─ Coordenadas: 100, 10, -200
```

**Causa**: Spawn seteado en lava, void, o bloque sólido

**Solución**:
```bash
# 1. Teleportarse al ciclo
/avo ciclo tp <ciclo>

# 2. Volar a ubicación segura (planicie, spawn natural)
/fly

# 3. Setear nuevo spawn desde esa ubicación
/avo ciclo setspawn

# 4. Verificar que ahora es seguro
/avo ciclo info
```

**Prevención**:
- Setear spawn SOLO en áreas planas y despejadas
- Evitar spawn cerca de lava, agua, o cuevas
- Verificar que hay 2 bloques de aire sobre suelo sólido

---

### Problema 3: Jugadores con Reconexión Rápida

**Síntomas**:
```
[SEGURIDAD] player123 intentó reconexión rápida. Cooldown: 3s
```

**Causa**: Jugador se desconecta/reconecta rápidamente (< 5s)

**Solución**:
- **Automática**: Sistema bloquea teleporte, jugador carga en mundo actual
- **No requiere acción** (es comportamiento esperado)

**Acción solo si es abuso**:
```bash
# Si jugador hace spam de reconexiones
/kick player123 Deja de hacer spam de reconexiones
```

**Explicación al jugador**:
> El sistema tiene un cooldown de 5 segundos entre reconexiones para evitar abuso. Espera 5 segundos antes de reconectar.

---

### Problema 4: Mundo del Ciclo No Cargado

**Síntomas**:
```
[SEGURIDAD] Ciclo activo 'ciclo_2' no existe o no está cargado
```

**Causa**: 
- Mundo corrupto
- Multiverse no cargó el mundo
- Archivo del mundo eliminado

**Solución**:
```bash
# 1. Verificar que el mundo existe
/mv list

# 2. Si NO aparece, cargarlo manualmente
/mv load ciclo_2

# 3. Si da error, el mundo está corrupto:
#    a) Restaurar desde backup
#    b) O crear nuevo ciclo

# 4. Activar el ciclo restaurado/nuevo
/avo ciclo activar ciclo_2

# 5. Verificar
/avo ciclo security
```

**Prevención**:
- Hacer backups diarios de carpetas de mundos
- No eliminar carpetas de mundos manualmente
- Usar SIEMPRE comandos de Multiverse

---

### Problema 5: Memory Leak (Muchos Cooldowns)

**Síntomas**:
```
[SEGURIDAD] Limpieza de cooldowns: 500 entradas removidas
```

**Causa**: Normal si servidor tiene mucho tráfico

**Solución**:
- **Automática**: Limpieza cada 30 minutos
- **Manual** (si quieres limpiar ahora):
```bash
/avo ciclo security
```

**Acción solo si persiste**:
- Revisar logs: ¿Muchos jugadores reconectando frecuentemente?
- Considerar aumentar cooldown (requiere código)

---

## 🛠️ Comandos de Diagnóstico

### Información General
```bash
/avo ciclo info
```
**Muestra**: Todos los ciclos, cuál está activo, jugadores en cada uno

---

### Panel de Seguridad
```bash
/avo ciclo security
# o
/avo ciclo seguridad
```
**Muestra**: 
- Estado de validación de ciclo único
- Ciclo activo actual
- Coordenadas del spawn
- Limpia cooldowns manualmente

---

### Ver Spawn de Ciclo
```bash
/avo ciclo info
```
Busca línea: `Spawn: X, Y, Z`

---

### Setear Spawn Seguro
```bash
# Mundo actual
/avo ciclo setspawn

# Mundo específico
/avo ciclo setspawn ciclo_1
```
**Validaciones automáticas**:
- Verifica que no hay lava/fuego
- Verifica que no hay bloques sólidos
- Busca alternativa si no es seguro
- Cancela si no encuentra spawn seguro

---

## 📊 Monitoreo de Logs

### Logs Críticos (Revisar Inmediatamente)

```
[SEGURIDAD] ✗ FALLO DE VALIDACIÓN
```
→ **Acción**: Desactivar ciclos duplicados

```
[SEGURIDAD] No se encontró spawn seguro
```
→ **Acción**: Setear nuevo spawn manualmente

```
[SEGURIDAD] Ciclo activo 'X' no existe o no está cargado
```
→ **Acción**: Restaurar mundo desde backup o crear nuevo

---

### Logs Informativos (Comportamiento Normal)

```
[SEGURIDAD] ✓ Validación: Un ciclo activo (ciclo_1)
```
→ ✓ Todo correcto

```
[SEGURIDAD] Limpieza de cooldowns: X entradas removidas
```
→ ✓ Mantenimiento automático funcionando

```
[SEGURIDAD] Spawn seguro encontrado a 3 bloques del original
```
→ ✓ Sistema encontró alternativa automáticamente

---

### Logs de Advertencia (Monitorear)

```
[SEGURIDAD] player intentó reconexión rápida. Cooldown: Xs
```
→ ⚠️ Normal si ocasional. Revisar si es spam repetido del mismo jugador.

```
[SEGURIDAD] player se desconectó antes de teleporte
```
→ ⚠️ Normal. Solo significa que jugador salió durante login.

---

### Formato de Log de Teleporte Normal

```
[CicloManager] TELEPORTE AUTORIZADO: player123 (uuid) desde 'world' → 'ciclo_1'
  └─ Razón: Primera vez en ciclo activo
  └─ Destino: 100, 70, -200
[CicloManager] ✓ Teleporte completado: player123
```

---

## ✅ Mejores Prácticas

### 1. Setear Spawn de Ciclo

**DO** ✓:
- Setear en áreas PLANAS y DESPEJADAS
- Verificar que hay 2 bloques de AIRE sobre el spawn
- Usar spawn natural del mundo (generalmente seguro)
- Testear teleportándote varias veces

**DON'T** ✗:
- No setear spawn en cuevas o bajo tierra
- No setear spawn cerca de lava/agua
- No setear spawn en árboles o estructuras
- No setear spawn en el void (Y < 5)

---

### 2. Activar Nuevos Ciclos

**Procedimiento correcto**:
```bash
# 1. Crear mundo con Multiverse
/mv create ciclo_3 NORMAL

# 2. Teleportarse y verificar
/mv tp ciclo_3

# 3. Setear spawn seguro
/avo ciclo setspawn ciclo_3

# 4. Activar ciclo
/avo ciclo activar ciclo_3

# 5. Verificar seguridad
/avo ciclo security
```

---

### 3. Cambiar de Ciclo Activo

**Procedimiento correcto**:
```bash
# 1. Backup de datos actuales
# (Automático si configurado)

# 2. Desactivar ciclo viejo
/avo ciclo desactivar ciclo_1

# 3. Activar ciclo nuevo
/avo ciclo activar ciclo_2

# 4. Verificar que solo hay 1 activo
/avo ciclo security

# 5. Anunciar a jugadores
/broadcast §eNuevo ciclo activo: ciclo_2. ¡Reloguea para teleportarte!
```

---

### 4. Mantenimiento Semanal

**Checklist semanal**:
```bash
# Lunes
□ Verificar estado de seguridad
□ Revisar logs de la semana
□ Hacer backup de mundos de ciclos

# Miércoles
□ Verificar integridad de datos
□ Probar reconexión manual

# Viernes
□ Limpiar logs antiguos
□ Verificar espacio en disco
```

---

## 🔧 Respuesta a Incidentes

### Incidente: Jugador Spawneó en Lava

**Pasos**:
1. **Inmediato**: Revivir/teleportar jugador a seguridad
   ```bash
   /tp player123 spawn
   /heal player123
   ```

2. **Investigar**: Revisar logs
   ```
   [SEGURIDAD] Spawn de 'ciclo_X' no es seguro
   ```

3. **Corregir**: Setear nuevo spawn
   ```bash
   /avo ciclo tp ciclo_X
   # Volar a área segura
   /avo ciclo setspawn ciclo_X
   ```

4. **Verificar**: Testear
   ```bash
   /avo ciclo tp ciclo_X
   # Verificar que spawn es seguro
   ```

5. **Comunicar**: Informar a jugador
   > Spawn corregido. Ya es seguro reconectarse.

---

### Incidente: 2 Ciclos Activos Simultáneos

**Pasos**:
1. **Identificar**: 
   ```bash
   /avo ciclo info
   ```

2. **Decidir**: ¿Cuál es el ciclo CORRECTO?
   - Generalmente el más NUEVO
   - El que tiene más jugadores
   - El que fue activado intencionalmente

3. **Desactivar** el ciclo INCORRECTO:
   ```bash
   /avo ciclo desactivar <ciclo_viejo>
   ```

4. **Verificar**:
   ```bash
   /avo ciclo security
   ```
   Debe mostrar: `✓ Ciclo activo único: CORRECTO`

5. **Prevenir**: 
   - Documentar por qué pasó
   - No editar archivos .yml manualmente
   - Usar SOLO comandos

---

### Incidente: Mundo Corrupto/No Carga

**Pasos**:
1. **Verificar** disponibilidad:
   ```bash
   /mv list
   ```

2. **Intentar cargar**:
   ```bash
   /mv load <nombre_mundo>
   ```

3. **Si falla**:
   - Revisar carpeta del mundo (¿existe?)
   - Revisar permisos de archivos
   - Verificar espacio en disco

4. **Restaurar desde backup**:
   ```bash
   # Detener servidor
   # Copiar carpeta de backup a server/
   # Iniciar servidor
   /mv load <nombre_mundo>
   ```

5. **Si backup no disponible**:
   - Crear nuevo ciclo
   - Migrar jugadores manualmente

---

## 📋 Checklist de Setup Inicial

### Primera vez configurando sistema de ciclos:

```bash
# 1. Verificar Multiverse instalado
/mv list

# 2. Crear primer ciclo
/mv create ciclo_1 NORMAL

# 3. Teleportarse y setear spawn
/mv tp ciclo_1
/avo ciclo setspawn ciclo_1

# 4. Activar ciclo
/avo ciclo activar ciclo_1

# 5. Verificar seguridad
/avo ciclo security

# Debe mostrar:
# ✓ Ciclo activo único: CORRECTO
# ◆ Ciclo activo actual: ciclo_1
```

---

## 🔐 Permisos de Seguridad

### Comando de Seguridad
```yaml
apocalipsis.ciclo.admin
```
**Otorga acceso a**:
- `/avo ciclo security`
- `/avo ciclo setspawn`
- `/avo ciclo activar/desactivar`

**Dar solo a**: Admins/Moderadores senior

---

### Usuarios Normales
```yaml
apocalipsis.ciclo.tp
```
**Otorga acceso a**:
- `/avo ciclo tp` (teleportarse a ciclos)
- `/avo ciclo info` (ver información)

---

## 📞 Contacto de Soporte

Si encuentras un problema no cubierto en esta guía:

1. **Revisar logs** completos (últimas 100 líneas)
2. **Reproducir** el problema con pasos específicos
3. **Documentar**:
   - Versión de Minecraft
   - Versión del plugin
   - Pasos para reproducir
   - Logs de error

---

## 📌 Resumen de Comandos Esenciales

```bash
# Verificación diaria
/avo ciclo security

# Ver todos los ciclos
/avo ciclo info

# Setear spawn seguro
/avo ciclo setspawn [mundo]

# Activar ciclo
/avo ciclo activar <mundo>

# Desactivar ciclo
/avo ciclo desactivar <mundo>

# Teleportarse a ciclo
/avo ciclo tp <mundo>
```

---

**Última actualización**: 27 Enero 2026  
**Versión del documento**: 1.0  
**Compatibilidad**: Apocalipsis v1.22.55+
