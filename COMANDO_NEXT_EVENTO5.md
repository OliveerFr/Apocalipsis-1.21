# Comando NEXT - Testing Rápido Evento 5

## 🎯 Propósito
Comando para **saltar rápidamente a la siguiente fase** durante testing del Evento 5, sin tener que esperar las animaciones y temporizadores completos.

---

## 📝 Uso

```bash
/avo evento5 next
```

**Alias:** `/avo aperturaend next`

---

## 🔄 Comportamiento por Fase

### 1️⃣ DESCUBRIMIENTO → Saltos por Diálogo (Mejorado ✨)
```
§a✓ Saltado a: §e40:00 §7(próximo diálogo)
§7Usa §e/avo evento5 next §7de nuevo para continuar
```
- **Acción:** Salta al siguiente diálogo programado (cada 5-15 minutos)
- **Diálogos disponibles:**
  - **40:00** (2400s) - "Huele a… antes. Cenizas. Vacío."
  - **35:00** (2100s) - "Este umbral… lo recuerdo sellado" (título)
  - **30:00** (1800s) - "Antes, esto tomaba más tiempo. Siglos."
  - **25:00** (1500s) - "Cuántas veces he visto este momento" (título largo)
  - **20:00** (1200s) - "El mundo está… apurado" (tensión)
  - **15:00** (900s) - "El portal se acerca" (título grande)
  - **10:00** (600s) - "Puedo sentir el vacío desde aquí" (suspenso)
  - **5:00** (300s) - "Ya casi están ahí" (alta tensión, título rojo)
  - **3:00** (180s) - "El End no olvida. Ni perdona."
  - **1:00** (60s) - "El portal despierta" (máxima tensión)
  - **0:10** (10s) - Countdown final numérico
  - **0:00** - Transición a LLEGADA

- **Resultado:** Ves cada mensaje del Observador sin esperar 45 minutos
- **Perfecto para:** Testing de narrativa, ver todos los diálogos
- **Hasta llegar a:** El momento donde se revelan las direcciones del portal

**Ejemplo de uso:**
```bash
/avo evento5 start
/avo evento5 next  # → 40:00 (primer diálogo)
/avo evento5 next  # → 35:00 (segundo diálogo)
/avo evento5 next  # → 30:00 (tercer diálogo)
# ... continúa hasta llegar al final
/avo evento5 next  # → 0:00 (transición a LLEGADA)
```

### 2️⃣ LLEGADA → PORTAL_ABIERTO
```
§a✓ [LLEGADA → PORTAL_ABIERTO] Portal abierto instantáneamente
§7Tip: Entra al portal para continuar a COMBATE
```
- **Acción:** Completa la animación de activación del portal
- **Resultado:** Portal totalmente funcional, listo para entrar
- **Nota:** Aún debes **entrar físicamente al portal** para ir al End

### 3️⃣ PORTAL_ABIERTO (Esperando entrada)
```
§e⚠ Fase PORTAL_ABIERTO activa - Entra al portal para continuar
§7Tip: Usa §e/tp @a <coords del End> §7para forzar
```
- **Acción:** Ninguna (no se puede saltar automáticamente)
- **Razón:** Necesitas entrar al portal para activar el combate
- **Alternativa:** Usa `/tp @a 0 100 0` (o coords del End) para forzar

### 4️⃣ COMBATE → VICTORIA
```
§a✓ [COMBATE → VICTORIA] Dragón eliminado
```
- **Acción:** Mata instantáneamente al dragón
- **Resultado:** Inicia la fase de silencio/victoria
- **Perfecto para:** Testing del cliffhanger final

### 5️⃣ VICTORIA → CLIFFHANGER
```
§a✓ [VICTORIA → CLIFFHANGER] Saltando a mensaje final...
§7El evento terminará en breve...
```
- **Acción:** Salta el silencio post-victoria
- **Resultado:** Muestra el mensaje final misterioso
- **Nota:** El evento terminará automáticamente después

### 6️⃣ CLIFFHANGER (Fase final)
```
§e⚠ Ya estás en la fase final (CLIFFHANGER)
§7El evento terminará automáticamente
```
- **Acción:** Ninguna (ya estás en la última fase)
- **Nota:** El evento se detendrá solo al completar el cliffhanger

---

## 🎮 Flujo Completo de Testing Rápido

### Opción A: Ver Todos los Diálogos (Narrativa Completa)
```bash
# 1. Iniciar evento
/avo evento5 start

# 2-12. Saltar de diálogo en diálogo
/avo evento5 next  # → 40:00 "Huele a… antes"
/avo evento5 next  # → 35:00 "Este umbral sellado" (título)
/avo evento5 next  # → 30:00 "Tomaba siglos"
/avo evento5 next  # → 25:00 "Cuántas veces..." (título largo)
/avo evento5 next  # → 20:00 "El mundo está apurado"
/avo evento5 next  # → 15:00 "El portal se acerca" (título)
/avo evento5 next  # → 10:00 "Siento el vacío"
/avo evento5 next  # → 5:00 "Ya casi están ahí" (TENSIÓN)
/avo evento5 next  # → 3:00 "El End no olvida"
/avo evento5 next  # → 1:00 "El portal despierta"
/avo evento5 next  # → 0:10 (countdown final)
/avo evento5 next  # → 0:00 TRANSICIÓN A LLEGADA

# 13. Ver revelación de direcciones del portal
# (Los jugadores ahora ven flechas y distancia)

# 14. Saltar animación portal
/avo evento5 next  # → PORTAL_ABIERTO

# 15. Entrar al portal
/tp @a 0 100 0

# 16. Saltar combate
/avo evento5 next  # → VICTORIA

# 17. Ver cliffhanger
/avo evento5 next  # → CLIFFHANGER
```

**Tiempo total:** ~5-10 minutos para ver TODA la narrativa

---

### Opción B: Testing Rápido (Solo Mecánicas)
```bash
# 1. Iniciar evento
/avo evento5 start

# 2. Saltar TODO el descubrimiento
/avo evento5 skip

# O usar next múltiples veces rápidamente
/avo evento5 next
/avo evento5 next
/avo evento5 next
# ... (repetir ~12 veces)

# 3. Saltar activación
/avo evento5 next

# 4. Entrar al End
/tp @a 0 100 0

# 5. Saltar combate
/avo evento5 next

# 6. Ver cliffhanger
/avo evento5 next
```

**Tiempo total:** ~2 minutos (vs 45+ minutos completo)

---

## 📊 Comparación de Comandos

| Comando | Función | Limitaciones |
|---------|---------|--------------|
| `/avo evento5 skip` | Salta **solo** DESCUBRIMIENTO | Solo fase 1 |
| `/avo evento5 next` | Salta a la **siguiente** fase | Todas las fases |
| `/avo evento5 fase <1-4>` | Cambia fase del **dragón** | Solo subfases de combate |
| `/avo evento5 kill` | Mata al dragón | Solo en COMBATE |

---

## ⚙️ Detalles Técnicos

### Archivo Modificado:
- `ApocalipsisCommand.java` (líneas 6333-6387)
- `AvoTabCompleter.java` (línea 132)

### Métodos Llamados:
```java
switch (faseActual) {
    case DESCUBRIMIENTO:
        evento5.saltarPreparacion();
        break;
    case COMBATE:
        evento5.matarDragon();
        break;
    // ... etc
}
```

### Tab Completion:
```bash
/avo evento5 <TAB>
# Muestra: start, iniciar, stop, detener, info, status, next, skip
```

---

## 🔍 Uso Práctico

### Testing de Narrativa:
```bash
# Ver solo los mensajes del Observador
/avo evento5 start
/avo evento5 next  # Saltar búsqueda
/avo evento5 next  # Saltar activación
# (Entrar al portal)
/avo evento5 next  # Saltar combate
/avo evento5 next  # Ver cliffhanger
```

### Testing de Mecánicas:
```bash
# Probar solo el combate con dragón
/avo evento5 start
/avo evento5 next  # → LLEGADA
/avo evento5 next  # → PORTAL_ABIERTO
/tp @a 0 100 0     # → Forzar entrada
# Ahora puedes testear el combate normalmente
```

### Testing de BossBar:
```bash
# Probar cuenta regresiva con BossBar
/avo evento5 start 3  # 3 minutos de cuenta regresiva
# Observar BossBar misteriosa
# Esperar o cancelar con Ctrl+C en consola
```

---

## 🚀 Beneficios

### Para Desarrollo:
- ✅ **Testing 20x más rápido**
- ✅ **Ver cada diálogo individualmente** (40min, 35min, 30min... hasta 1min)
- ✅ Probar fases específicas sin repetir todo
- ✅ Verificar mensajes y animaciones uno por uno
- ✅ **Llegar al momento exacto de revelación de direcciones**

### Para Narrativa:
- ✅ **Revisar todos los mensajes del Observador** sin esperar
- ✅ Ver la progresión emocional: nostalgia → tristeza → tensión → pánico
- ✅ Testear títulos y sonidos de cada diálogo
- ✅ **Probar el sistema de brújula** (flechas y distancia)

### Para Streaming:
- ✅ Ensayar la narrativa completa en 10 minutos
- ✅ Practicar timing de transiciones
- ✅ Ajustar cuándo revelar direcciones del portal

### Para Debugging:
- ✅ Reproducir bugs en diálogos específicos (ej: el de 15:00)
- ✅ Testear fixes sin repetir 45 minutos
- ✅ Verificar logs de cada checkpoint temporal

---

## ⚠️ Advertencias

1. **Solo para Testing:** No usar en producción/stream real
2. **No salta PORTAL_ABIERTO:** Debes entrar físicamente al portal
3. **Fases Irreversibles:** No puedes volver atrás con `next`
4. **Usa `stop` para resetear:** Si quieres reiniciar el evento

---

## 📦 Versión
- **JAR Compilado:** `Apocalipsis-1.22.48.jar`
- **Comando Añadido:** `/avo evento5 next`
- **Tab Completion:** ✅ Agregado

---

**Fecha:** 2026-01-19  
**Feature:** Comando NEXT para testing rápido de fases
