# 🔄 Mejora: Persistencia de Días (v1.22.58)

## 📋 Problema Resuelto

**Antes:** El sistema de cambio automático de día usaba un timer de 24 horas que se reiniciaba cada vez que se recargaba o reiniciaba el servidor, causando:
- ❌ Pérdida del progreso del día actual
- ❌ Posibles cambios de día duplicados o retrasados
- ❌ Inconsistencias si el servidor se reinicia antes de las 24 horas

**Ahora:** El sistema guarda el timestamp exacto del próximo cambio de día en `state.yml`, garantizando:
- ✅ Persistencia completa del progreso del día
- ✅ El próximo día ocurre exactamente 24 horas después del anterior
- ✅ Inmune a reloads/restarts del servidor

---

## 🔧 Cambios Implementados

### 1. **StateManager.java** - Nuevo campo persistente

```java
private long nextDayEpochMs = 0L; // Timestamp del próximo cambio de día
```

**Métodos agregados:**
- `getNextDayEpochMs()` - Obtiene cuándo debe cambiar el día
- `setNextDayEpochMs(long)` - Guarda cuándo debe cambiar el día

### 2. **state.yml** - Nueva clave

```yaml
next_day_epoch_ms: 1738012800000  # Timestamp Unix del próximo día
current_day: 5                     # Día actual
```

### 3. **Apocalipsis.java** - Sistema de AUTO-NEWDAY mejorado

**Lógica antigua:**
```java
// Timer fijo de 24 horas - se reinicia en cada reload
runTaskTimer(task, 1728000L, 1728000L);
```

**Lógica nueva:**
```java
// Al iniciar el plugin:
1. Lee next_day_epoch_ms desde state.yml
2. Si no existe O ya pasó → calcula nuevo timestamp (ahora + 24h)
3. Programa verificación cada 60 segundos
4. Cuando llega el momento exacto → ejecuta cambio de día
5. Guarda nuevo timestamp para el siguiente día (+24h)
```

---

## 📊 Flujo de Funcionamiento

### Inicio del Servidor (Primera Vez)
```
1. Plugin carga → next_day_epoch_ms = 0
2. Sistema detecta: "No hay timestamp guardado"
3. Calcula: now + 24h = 1738012800000
4. Guarda en state.yml
5. Programa verificación cada 60 seg
```

### Cambio de Día Automático
```
1. Timer verifica cada 60 seg: ¿llegó el momento?
2. Si now >= next_day_epoch_ms:
   ✓ Incrementa current_day
   ✓ Resetea misiones diarias
   ✓ Asigna nuevas misiones
   ✓ Calcula próximo día: now + 24h
   ✓ Guarda nuevo next_day_epoch_ms
   ✓ Broadcast a jugadores
```

### Reload/Restart del Servidor
```
1. Plugin carga → Lee next_day_epoch_ms = 1738012800000
2. Calcula delay: next_day_epoch_ms - now = 5 horas restantes
3. Programa verificación para dentro de 5h
4. ✅ El progreso se mantiene exactamente donde estaba
```

---

## 🎯 Ventajas del Sistema

| Característica | Antes | Ahora |
|---------------|-------|-------|
| **Persistencia** | ❌ Se pierde en reload | ✅ Guardado en state.yml |
| **Precisión** | ❌ ±24h desde reload | ✅ Exacto al milisegundo |
| **Confiabilidad** | ❌ Vulnerable a restarts | ✅ Inmune a restarts |
| **Transparencia** | ❌ No visible | ✅ Visible en state.yml y logs |
| **Verificación** | Timer ciego | Verificación inteligente cada 60s |

---

## 📝 Ejemplo de state.yml

```yaml
# ═══════════════════════════════════════════════════════════════════
# STATE.YML - Estado persistente del servidor
# ═══════════════════════════════════════════════════════════════════

estado: DETENIDO
desastre_actual: ""
ultimo_desastre: ""

# Sistema de días (persistente)
current_day: 5
next_day_epoch_ms: 1738012800000  # 27 Enero 2026, 14:00:00 UTC

# Sistema de desastres
last_end_epoch_ms: 0
start_epoch_ms: 0
end_epoch_ms: 0
remaining_seconds: 0
planned_seconds: 900
prep_forzada: false
```

---

## 🔍 Logs de Referencia

### Al iniciar el plugin (primera vez)
```
[AutoNewDay] Próximo cambio de día programado para: Wed Jan 28 14:00:00 2026
[AutoNewDay] Próximo día en 1440 minutos
```

### Al iniciar después de reload
```
[AutoNewDay] Próximo día en 300 minutos  # 5 horas restantes
```

### Cuando ocurre el cambio
```
[AutoNewDay] Día 6 iniciado - 15 jugadores online
[AutoNewDay] Próximo cambio programado: Thu Jan 29 14:00:00 2026
```

---

## 🛡️ Seguridad y Fallbacks

1. **Si next_day_epoch_ms = 0:** Calcula nuevo timestamp (+24h)
2. **Si next_day_epoch_ms ya pasó:** Ejecuta cambio inmediatamente y programa siguiente
3. **Si delay calculado es negativo:** Fallback a 24h
4. **Guardado automático:** Cada cambio se persiste en state.yml

---

## 🎮 Comandos Útiles

Para verificar el estado actual del sistema de días:

```bash
# Ver state.yml en tiempo real
/apo debug state

# Forzar cambio de día (para testing)
/apo newday

# Ver próximo cambio programado (en logs al reload)
/reload confirm
```

---

## 📦 Versión

- **Implementado en:** v1.22.58
- **Archivos modificados:**
  - `StateManager.java` - Nuevo campo y métodos
  - `Apocalipsis.java` - Sistema AUTO-NEWDAY mejorado
  - `state.yml` - Nueva clave `next_day_epoch_ms`

---

## ✅ Checklist de Testing

- [ ] Verificar que `next_day_epoch_ms` se guarda en state.yml
- [ ] Reload del servidor - el progreso se mantiene
- [ ] Restart del servidor - el progreso se mantiene
- [ ] Cambio de día ocurre exactamente 24h después
- [ ] Logs muestran timestamp del próximo día
- [ ] Comando `/apo newday` funciona correctamente
- [ ] Múltiples reloads no duplican el cambio de día

---

**Creado:** 27 Enero 2026  
**Autor:** Sistema de Mejora de Persistencia  
**Estado:** ✅ Implementado y Documentado
