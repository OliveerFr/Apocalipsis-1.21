# Comandos del Evento 6: Cuando el Mundo Decide Olvidar

## ✅ Comandos Implementados

### Control Principal

#### `/avo evento6 start` o `/avo evento6 iniciar`
- **Descripción**: Inicia el evento
- **Permisos**: `apocalipsis.evento6.admin`
- **Solo jugadores**: Sí
- **Validaciones**:
  - Verifica que el evento no esté ya activo
  - Verifica que CicloManager esté inicializado
  - Verifica que el sistema de ciclos esté habilitado en `ciclos.yml`
- **Resultado**: 
  - Inicia el evento de 40 minutos
  - Registra todos los jugadores online como participantes
  - Broadcast a todo el servidor

#### `/avo evento6 stop` o `/avo evento6 detener`
- **Descripción**: Detiene el evento manualmente
- **Permisos**: `apocalipsis.evento6.admin`
- **Solo jugadores**: Sí
- **Resultado**: Finaliza inmediatamente el evento

---

### Información y Estado

#### `/avo evento6 status` o `/avo evento6 estado`
- **Descripción**: Muestra el estado actual del evento (versión simple)
- **Permisos**: `apocalipsis.evento6.admin`
- **Muestra**:
  - ✓ Activo / ✗ Inactivo
  - Fase actual
  - Número de participantes
  - Tiempo transcurrido

#### `/avo evento6 info`
- **Descripción**: Muestra información detallada del evento
- **Permisos**: `apocalipsis.evento6.admin`
- **Muestra**:
  - Nombre completo del evento
  - Estado (activo/inactivo)
  - Fase actual
  - Participantes
  - Tiempo transcurrido (min:seg)
  - Siguiente acto programado

---

### Control de Fases

#### `/avo evento6 next` o `/avo evento6 skip` o `/avo evento6 siguiente`
- **Descripción**: Fuerza el avance al siguiente acto
- **Permisos**: `apocalipsis.evento6.admin`
- **Solo jugadores**: Sí
- **Validaciones**:
  - Verifica que el evento esté activo
  - Verifica que no estés ya en el último acto
- **Resultado**: 
  - Cambia inmediatamente a la siguiente fase
  - Broadcast informando del avance forzado
  - Ejecuta todos los efectos del nuevo acto

#### `/avo evento6 participantes` o `/avo evento6 players`
- **Descripción**: Lista todos los participantes del evento
- **Permisos**: `apocalipsis.evento6.admin`
- **Solo jugadores**: Sí
- **Muestra**:
  - Total de participantes
  - Lista con estado (✓ online / ✗ offline)
  - Contador de jugadores online vs total

---

## Alias del Comando Principal

Los siguientes alias funcionan igual que `evento6`:
- `/avo mundoolvidado`
- `/avo reinicio`

Ejemplo: `/avo mundoolvidado start` = `/avo evento6 start`

---

## Tab Completion

Todos los comandos tienen autocompletado. Presiona TAB después de escribir `/avo evento6 ` para ver las opciones:

```
/avo evento6 <TAB>
  start       iniciar      stop        detener
  status      estado       info        next
  skip        siguiente    participantes  players
```

---

## Secuencia de Actos (40 minutos)

| Acto | Tiempo | Fase |
|------|--------|------|
| 1 | 0:00 - 5:00 | Normalidad |
| 2 | 5:00 - 10:00 | Rarezas |
| 3 | 10:00 - 12:30 | Inestabilidad |
| 4 | 12:30 - 14:30 | Quiebre |
| 5 | 14:30 - 15:00 | **REINICIO** ⚡ |
| 6 | 15:00 - 20:00 | Nuevo Mundo |
| 7 | 20:00 - 25:00 | Comprensión |
| 8 | 25:00 - 30:00 | Fractura (Nether) |
| 9 | 30:00 - 35:00 | End Permanece |
| 10 | 35:00 - 40:00 | Cierre |

---

## Ejemplos de Uso

### Iniciar el evento
```
/avo evento6 start
```

### Ver estado rápido durante el evento
```
/avo evento6 status
```
Salida:
```
✓ Evento 6 activo
Fase: §eACTO 3: INESTABILIDAD
Participantes: §e12
Tiempo: §e11m 23s
```

### Ver información detallada
```
/avo evento6 info
```

### Saltar al siguiente acto (para pruebas)
```
/avo evento6 next
```
Salida:
```
✓ Avanzado a: ACTO 4: QUIEBRE
[EVENTO 6] ⚡ Avance forzado: ACTO 4: QUIEBRE
```

### Listar quién está participando
```
/avo evento6 participantes
```
Salida:
```
━━ PARTICIPANTES EVENTO 6 ━━
Total: 12

  ✓ OliveerF
  ✓ Player2
  ✗ Player3
  ...

Online: 10/12
━━━━━━━━━━━━━━━━━━━━━━
```

---

## Notas Técnicas

### Requisitos Previos
1. **CicloManager debe estar activo**
   - Verifica en `plugins/Apocalipsis/ciclos.yml`
   - `config.enabled` debe ser `true`

2. **Permisos necesarios**
   - `apocalipsis.evento6.admin` para todos los comandos

3. **Solo para jugadores**
   - Todos los comandos requieren ejecutarse desde un jugador
   - No funcionan desde consola

### Flujo del Comando `next/skip`
1. Verifica que el evento esté activo
2. Obtiene el acto actual
3. Calcula el siguiente acto en la secuencia
4. Usa reflexión para llamar al método privado `cambiarAActo()`
5. Ejecuta todos los efectos y mensajes del nuevo acto

### Integración con CicloManager
- El **Acto 5 (Reinicio)** crea un nuevo mundo usando `CicloManager.createAndActivateCycle()`
- Los inventarios se guardan automáticamente antes del reinicio
- Las skills, rangos y misiones se preservan entre ciclos
- Cada ciclo tiene su propio Nether
- Todos los ciclos comparten el mismo End

---

## Archivo de Configuración

Todos los diálogos, efectos y tiempos se configuran en:
```
plugins/Apocalipsis/evento6_mundo_olvidado.yml
```

Puedes editar:
- Mensajes de cada acto
- Efectos de partículas
- Sonidos
- Tiempos de delay entre efectos
- Recompensas finales (PS, items)

---

## Changelog

**v1.22.55** - 26 de enero de 2026
- ✅ Implementados comandos: `status`, `info`, `next`, `skip`, `participantes`
- ✅ Tab completion para todos los comandos
- ✅ Sistema de reflexión para avance forzado de actos
- ✅ Validaciones de CicloManager en inicio
- ✅ Import de UUID corregido
- ✅ Compilación exitosa: `Apocalipsis-1.22.55.jar` (1.03 MB)
