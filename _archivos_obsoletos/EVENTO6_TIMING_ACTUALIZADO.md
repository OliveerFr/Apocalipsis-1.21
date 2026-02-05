# Evento 6 - Timing Actualizado ⏱️

## Cambios Realizados

✅ **El reinicio ahora ocurre a los 15 minutos** del inicio del evento (antes era a los 56 minutos)  
✅ **Duración total**: 40 minutos (antes 2 horas)

---

## Nueva Estructura de Actos

### 📋 Timeline Completo

| Acto | Nombre | Inicio | Duración | Tiempo Acum. |
|------|--------|--------|----------|--------------|
| **1** | La Normalidad Aparente | 0:00 | 5 min | 5 min |
| **2** | Las Primeras Señales | 5:00 | 5 min | 10 min |
| **3** | Anomalías Crecientes | 10:00 | 2.5 min | 12.5 min |
| **4** | El Quiebre | 12:30 | 2 min | 14.5 min |
| **5** | 🔥 **EL REINICIO** 🔥 | **14:30** | **30 seg** | **15 min** |
| **6** | Nuevo Mundo | 15:00 | 5 min | 20 min |
| **7** | Comprensión Lenta | 20:00 | 5 min | 25 min |
| **8** | La Fractura (Nether) | 25:00 | 5 min | 30 min |
| **9** | El End Permanece | 30:00 | 5 min | 35 min |
| **10** | Cierre del Ciclo | 35:00 | 5 min | **40 min** |

---

## Momento del Reinicio

### ⏰ ACTO 5: El Reinicio (14:30 - 15:00)

**El mundo se resetea exactamente a los 14.5 minutos (870 segundos)**

#### Secuencia del Reinicio:
1. **2 segundos** - Pantalla negra (ceguera)
2. **CicloManager crea el nuevo mundo**
3. **Teleport masivo** - Todos los jugadores al spawn del nuevo mundo
4. **Efectos visuales** - Partículas END_ROD + sonido RESPAWN_ANCHOR_CHARGE

#### Lo que se pierde:
- ❌ Inventario completo
- ❌ Experiencia (XP)
- ❌ Todas las construcciones del overworld

#### Lo que se conserva:
- ✅ Rangos
- ✅ Habilidades (skills)
- ✅ Misiones
- ✅ Estadísticas
- ✅ **El Nether permanece intacto**
- ✅ **El End permanece intacto**

---

## Ritmo Narrativo

### Primera Fase: Construcción de Tensión (0-15 min)
- **Actos 1-3**: Introducción gradual de anomalías
- **Acto 4**: Clímax dramático (El Quiebre)
- **Acto 5**: ⚡ REINICIO INSTANTÁNEO ⚡

### Segunda Fase: Exploración Post-Reinicio (15-40 min)
- **Actos 6-7**: Comprensión de lo que pasó
- **Actos 8-9**: Revelación (Nether y End no se resetearon)
- **Acto 10**: Cierre filosófico

---

## Mensajes Clave del Observador

### Durante el reinicio (Acto 5):
```
[...] No los borró...
[...] Solo borró el lugar.
```

### Post-reinicio (Acto 7):
```
[...] El mundo hace esto cuando se cansa.
[...] Reiniciar es más fácil que cambiar.
```

### Cierre final (Acto 10):
```
[...] Este no es un comienzo.
[...] Es una repetición.
```

---

## Recompensas

Los jugadores que completan el evento reciben:

1. **Fragmento de Memoria** (Echo Shard)
2. **Cicatriz Temporal** (Netherite Scrap)
3. **Eco de la Repetición** (Recovery Compass)
4. **Hasta 350 PS** por participación completa

---

## Comandos

```
/avo evento6 start  - Iniciar evento (requiere permiso)
/avo evento6 stop   - Detener evento
/avo evento6 info   - Ver estado actual
```

**Permiso requerido**: `apocalipsis.evento6.admin`

---

## Archivos Modificados

### Configuración
- ✅ `evento6_mundo_olvidado.yml` - Todos los tiempos actualizados

### Código Java
- ✅ `Evento6MundoOlvidado.java` - Método `obtenerActoPorTiempo()` actualizado

### Compilación
```bash
mvn package -DskipTests
```

**JAR generado**: `target/Apocalipsis-1.22.55.jar`

---

## Resumen Visual

```
┌─────────────────────────────────────────────────────────────┐
│                  EVENTO 6: MUNDO OLVIDADO                   │
│                   Duración Total: 40 min                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  0 min ──────────────────────── 15 min ────────── 40 min   │
│    │         Tensión            │      Exploración    │     │
│    │                            │                     │     │
│    └── Normalidad               └── REINICIO         │     │
│         ↓                            ↓                │     │
│         Anomalías                    Nuevo Mundo      │     │
│         ↓                            ↓                │     │
│         El Quiebre ═══════════> Comprensión ────────→ │     │
│                                      ↓                │     │
│                                 Nether/End ──────────→ │     │
│                                      ↓                │     │
│                                    Cierre ────────────┘     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Pruebas Recomendadas

1. **Iniciar el evento** con `/avo evento6 start`
2. **Verificar progresión** a los 5, 10, y 14.5 minutos
3. **Confirmar reinicio** a los 15 minutos exactos
4. **Validar que Nether/End no se resetean**
5. **Comprobar entrega de recompensas** al finalizar

---

**Estado**: ✅ Implementado y compilado  
**Fecha**: 26 de enero de 2026  
**Versión**: 1.22.55
