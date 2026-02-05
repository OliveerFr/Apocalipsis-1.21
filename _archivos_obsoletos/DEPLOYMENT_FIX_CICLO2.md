# ✅ Fix Implementado: Desastres Ciclo 2 Auto-Inicio

## 🎯 Resumen del Problema y Solución

### Problema Detectado
Los desastres del **Ciclo 2** (Tormenta Glacial, Tormenta Eléctrica, Erupción Volcánica) no iniciaban automáticamente cuando el cooldown llegaba a 0.

### Causa Raíz
El método `elegirSegunWeight()` siempre leía `desastres.weights` (Ciclo 1 con pesos = 0) en lugar de `desastres.weights_ciclo_2` (Ciclo 2 con pesos = 1).

### Solución Aplicada
✅ Modificado `DisasterController.java` para leer dinámicamente la tabla correcta según `usar_desastres_nuevos`  
✅ Agregada validación de weights=0 para detectar configuraciones inválidas  
✅ Mejorado sistema de logs para debugging  
✅ Compilado exitosamente: `Apocalipsis-1.22.56.jar` (1.4 MB)

---

## 📦 Archivos Modificados

### Código Fuente
- **[DisasterController.java](src/main/java/me/apocalipsis/disaster/DisasterController.java)**
  - Método: `elegirSegunWeight()` (líneas ~1357-1420)
  - Cambio: Lectura dinámica de `weights` vs `weights_ciclo_2`

### JAR Compilado
- **Location:** `target/Apocalipsis-1.22.56.jar`
- **Size:** 1.4 MB
- **Date:** 2026-01-28 10:17 AM
- **Status:** ✅ Compilado exitosamente

### Documentación
- **[CHANGELOG_FIX_DESASTRES_CICLO2_v1.22.59.md](CHANGELOG_FIX_DESASTRES_CICLO2_v1.22.59.md)** - Changelog completo
- **[FIX_DESASTRES_CICLO2_RESUMEN.md](FIX_DESASTRES_CICLO2_RESUMEN.md)** - Resumen ejecutivo
- **Este archivo** - Instrucciones de deployment

---

## 🚀 Instrucciones de Deployment

### 1. Backup del Plugin Actual
```bash
# En el servidor Minecraft
cd plugins/
cp Apocalipsis-1.22.56.jar Apocalipsis-1.22.56-backup.jar
```

### 2. Subir Nuevo JAR
```bash
# Copiar desde:
Z:\riolu\Videos\Eventos\Apocalipsis-1.21.8\target\Apocalipsis-1.22.56.jar

# Hacia:
<servidor_minecraft>/plugins/Apocalipsis-1.22.56.jar
```

### 3. Verificar Configuración
Asegurar que `plugins/Apocalipsis/desastres.yml` tenga:
```yaml
ciclo:
  usar_desastres_nuevos: true   # ✅ Debe ser true para Ciclo 2
  auto_cycle: true              # ✅ Debe ser true para auto-inicio
  cooldown_fin_segundos: 900    # 15 minutos (ajustar según preferencia)

desastres:
  weights_ciclo_2:
    tormenta_glacial: 1
    tormenta_electrica: 1
    erupcion_volcanica: 1
```

### 4. Reiniciar Servidor
```bash
# Opción 1: Reinicio completo (RECOMENDADO)
/stop

# Opción 2: Reload (puede no aplicar cambios en auto-cycle)
/reload confirm
```

⚠️ **IMPORTANTE:** Se recomienda **reinicio completo** del servidor, no solo reload del plugin, para garantizar que el sistema auto-cycle se reinicie correctamente.

### 5. Activar Debug (Opcional)
Para monitorear el funcionamiento:
```yaml
# En desastres.yml
debug_ciclo: true
```

Luego ejecutar:
```bash
/avo reload
```

---

## 🧪 Verificación Post-Deployment

### Test 1: Auto-Inicio Funcional
```bash
# 1. Iniciar un desastre manualmente
/avo force tormenta_glacial

# 2. Esperar a que termine (o detenerlo)
/avo stop

# 3. Esperar el cooldown (15 min por defecto)
# Observar logs: debe iniciar automáticamente un desastre del Ciclo 2
```

**Logs esperados:**
```
[Cycle] Cooldown cumplido → intentando iniciar
[Cycle] Usando weights desde: desastres.weights_ciclo_2 (usar_nuevos=true)
[Cycle] Desastre disponible: tormenta_glacial (weight=1)
[Cycle] Desastre disponible: tormenta_electrica (weight=1)
[Cycle] Desastre disponible: erupcion_volcanica (weight=1)
[Cycle] ✅ Desastre elegido: tormenta_electrica de pool con 3 opciones
[Cycle] ✅ INICIANDO desastre: tormenta_electrica (reason=cooldown)
```

### Test 2: Verificar Weights Correctos
```bash
# Ejecutar varias veces:
/avo force tormenta_glacial
# Esperar a que termine
# Repetir

# Verificar en logs que se elige un desastre diferente cada vez
# (no siempre el mismo)
```

### Test 3: Detección de Error (Opcional)
```bash
# En desastres.yml, cambiar temporalmente:
desastres:
  weights_ciclo_2:
    tormenta_glacial: 0
    tormenta_electrica: 0
    erupcion_volcanica: 0

# Reload: /avo reload
# Logs esperados:
# [ERROR] Todos los desastres tienen weight=0 en desastres.weights_ciclo_2
```

---

## 📊 Comportamiento Esperado

### Antes del Fix ❌
- Cooldown llega a 0
- Sistema intenta iniciar desastre
- Lee `desastres.weights` (todos en 0)
- Pool vacío → **NO INICIA NADA**
- Se queda esperando indefinidamente

### Después del Fix ✅
- Cooldown llega a 0
- Sistema intenta iniciar desastre
- Lee `desastres.weights_ciclo_2` (pesos = 1)
- Elige desastre aleatoriamente (excluyendo el último)
- **INICIA DESASTRE CORRECTAMENTE**
- Sistema funciona como se espera

---

## 🔍 Troubleshooting

### Problema: Desastres aún no inician
**Solución:**
1. Verificar que `usar_desastres_nuevos: true` en `desastres.yml`
2. Verificar que `auto_cycle: true`
3. Verificar que `weights_ciclo_2` tenga al menos un peso > 0
4. **Reiniciar servidor completo** (no solo reload)
5. Activar `debug_ciclo: true` y revisar logs

### Problema: Solo inicia el mismo desastre
**Causa:** Probablemente solo un desastre tiene weight > 0  
**Solución:** Verificar que los 3 desastres tengan weight = 1

### Problema: Logs no aparecen
**Solución:**
```yaml
# En desastres.yml
debug_ciclo: true

# En config.yml
debug: true
```

---

## 📝 Notas Adicionales

### Configuración Recomendada para Testing Rápido
```yaml
ciclo:
  usar_desastres_nuevos: true
  auto_cycle: true
  cooldown_fin_segundos: 60     # ⚠️ 1 min para testing (cambiar a 900 en prod)
  min_jugadores: 1

desastres:
  weights_ciclo_2:
    tormenta_glacial: 1
    tormenta_electrica: 1
    erupcion_volcanica: 1
  
  # Duración corta para testing
  tormenta_glacial:
    duracion_segundos: 120      # ⚠️ 2 min para testing (cambiar a 900 en prod)
```

### Rollback (Si Hay Problemas)
```bash
cd plugins/
rm Apocalipsis-1.22.56.jar
mv Apocalipsis-1.22.56-backup.jar Apocalipsis-1.22.56.jar
/stop
```

---

## ✅ Checklist de Deployment

- [ ] Backup del JAR actual realizado
- [ ] Nuevo JAR subido al servidor
- [ ] `desastres.yml` verificado (usar_desastres_nuevos: true)
- [ ] `desastres.yml` verificado (weights_ciclo_2 configurados)
- [ ] Servidor reiniciado (no solo reload)
- [ ] Debug activado para monitoreo
- [ ] Test de auto-inicio realizado
- [ ] Logs verificados (sin errores)
- [ ] Sistema funcionando correctamente

---

## 📞 Contacto

**Versión:** 1.22.56  
**Fix ID:** CICLO2_AUTOINICIO_v1.22.59  
**Fecha:** 2026-01-28  
**Prioridad:** Alta (bug crítico)

---

**Estado:** ✅ FIX IMPLEMENTADO Y COMPILADO  
**Próximo Paso:** Deployment en servidor de producción
