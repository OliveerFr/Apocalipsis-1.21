# 🔥 Comandos del Evento Eco de Brasas

## Comando Principal: `/avo eco`

El evento **Eco de Brasas** es un evento cooperativo **manual único** que NO forma parte del ciclo automático de desastres.

---

## 📋 Subcomandos Disponibles

### Control Básico

#### `/avo eco start`
- **Descripción**: Inicia el evento Eco de Brasas
- **Requisitos**: 
  - No debe haber desastre/evento activo
  - Server no en SAFE_MODE (TPS normal)
- **Resultado**: Inicia Fase 1 (RECOLECCIÓN)

#### `/avo eco stop`
- **Descripción**: Detiene el evento actual
- **Requisitos**: Evento activo
- **Resultado**: Detiene todas las mecánicas, elimina entidades

---

### Control de Fases

#### `/avo eco fase <1|2|3>`
- **Descripción**: Fuerza una fase específica del evento
- **Parámetros**:
  - `1` = Fase RECOLECCIÓN (grietas de vapor)
  - `2` = Fase ESTABILIZACIÓN (anclas de fuego)
  - `3` = Fase RITUAL FINAL (altar central)
- **Ejemplo**: `/avo eco fase 2` → Salta directamente a anclas
- **Uso**: Recuperación si algo falla, testing, skip progreso

#### `/avo eco next`
- **Descripción**: Avanza a la siguiente fase secuencialmente
- **Resultado**: RECOLECCIÓN → ESTABILIZACIÓN → RITUAL FINAL
- **Nota**: Si ya estás en fase 3, no hace nada

---

### Información y Diagnóstico

#### `/avo eco info`
- **Descripción**: Muestra estado detallado del evento
- **Información mostrada**:
  - Fase actual (1/2/3)
  - Progreso de fase (0-100%)
  - En fase 1: grietas activas, fragments totales recogidos
  - En fase 2: anclas completadas (0/3), recursos entregados
  - En fase 3: progreso ritual (pulsos globales)
- **Uso**: Monitoreo, debugging, ver si jugadores están progresando

---

### Comandos de Recuperación

#### `/avo eco pulso <add|set> <valor>`
- **Descripción**: Modifica el pulso global del evento (progreso general)
- **Parámetros**:
  - `add <valor>` = Añade porcentaje (puede ser negativo)
  - `set <valor>` = Establece porcentaje exacto (0-100)
- **Ejemplos**:
  - `/avo eco pulso add 50` → Añade 50% al progreso actual
  - `/avo eco pulso set 100` → Establece progreso a 100%
- **Uso**: Forzar transición de fase si jugadores se traban

#### `/avo eco ancla <1|2|3>`
- **Descripción**: Completa forzadamente un ancla específica
- **Requisitos**: Estar en Fase 2 (ESTABILIZACIÓN)
- **Parámetros**: ID del ancla (1, 2 o 3)
- **Ejemplo**: `/avo eco ancla 2` → Marca ancla #2 como completada
- **Uso**: Si un ancla se bugea o jugadores no pueden completarla

---

## 🎮 Flujo de Uso Normal

```bash
# 1. Iniciar evento
/avo eco start

# 2. Monitorear progreso
/avo eco info

# 3. Si algo sale mal en fase X, forzar siguiente:
/avo eco next

# 4. O saltar directamente a fase específica:
/avo eco fase 3

# 5. Finalizar prematuramente si necesario:
/avo eco stop
```

---

## 🔧 Casos de Uso de Recuperación

### Problema: Jugadores no encuentran grietas
```bash
# Forzar transición a Fase 2 manualmente
/avo eco fase 2
```

### Problema: Ancla bugeada, no acepta recursos
```bash
# Completar ancla forzadamente
/avo eco ancla 1
/avo eco info  # Verificar que se marcó
```

### Problema: Ritual atascado en 80%
```bash
# Forzar progreso a 100%
/avo eco pulso set 100
```

### Problema: Evento crasheó, reiniciar desde fase actual
```bash
# Determinar última fase activa (consultar logs/memoria)
/avo eco start
/avo eco fase 2  # O la fase que corresponda
/avo eco pulso set 75  # Restaurar progreso aproximado
```

---

## ⚙️ Tab Completion

El sistema incluye autocompletado inteligente:

- `/avo eco <TAB>` → Muestra: start, stop, fase, next, info, pulso, ancla
- `/avo eco fase <TAB>` → Muestra: 1, 2, 3
- `/avo eco pulso <TAB>` → Muestra: add, set
- `/avo eco ancla <TAB>` → Muestra: 1, 2, 3

---

## 📝 Notas Importantes

1. **NO automático**: Eco de Brasas NO está en el ciclo automático (no tiene peso en `desastres.yml`)
2. **Evento único**: Diseñado para ocurrir una sola vez por razones narrativas
3. **Permisos**: Todos los comandos requieren `avo.admin`
4. **Logs**: Acciones críticas se loguean en consola con prefijo `[EcoBrasas]`
5. **Safe Mode**: Si TPS cae mucho, el evento NO puede iniciarse (protección)

---

## 🐛 Debugging

Para ayudar a debuggear problemas:

```bash
# 1. Ver estado actual
/avo eco info

# 2. Verificar fase
# (Salida muestra: "Fase actual: ESTABILIZACIÓN")

# 3. Si evento no responde, reiniciar:
/avo eco stop
/avo eco start
/avo eco fase <ultima_fase_conocida>
```

Si el evento crashea, los comandos de fase permiten **reanudar exactamente donde se quedó**, evitando tener que reiniciar desde cero.

---

## 📚 Referencias

- **Archivo principal**: `EcoBrasasNew.java`
- **Comando handler**: `ApocalipsisCommand.java` → método `cmdEco()`
- **Config**: `desastres.yml` → sección `eco_brasas`
- **Documentación técnica**: `ECO_DE_BRASAS_INFO.md`
