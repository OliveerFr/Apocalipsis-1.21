# 🔍 Sistema de Escaneo de Protecciones

**Versión:** 1.21.8  
**Fecha:** 9 de Noviembre, 2025  
**Autor:** Apocalipsis Plugin Team

---

## 📋 Resumen Ejecutivo

Se implementó un **Sistema de Escaneo de Protecciones** que permite a los jugadores visualizar y entender las protecciones contra desastres en tiempo real. Este sistema complementa la "mega update" de feedback añadiendo capacidades de **prevención** y **planificación estratégica**.

---

## ✨ Funcionalidades Nuevas

### 1️⃣ `/avo escanear` - Escáner de Protecciones en Tiempo Real

**Descripción:**  
Analiza el entorno del jugador y muestra un reporte completo de todas las protecciones activas contra los tres desastres principales.

**Características:**
- ✅ Escanea bloques absorbentes para Terremoto (radio 6 bloques)
- ✅ Detecta agua para Lluvia de Fuego (área 3x3x3)
- ✅ Verifica techo para Huracán (hasta 5 bloques arriba)
- ✅ Muestra estadísticas de reducción de daño en porcentajes
- ✅ Spawnea partículas de colores en bloques protectores (20 segundos)
- ✅ Formato visual con bordes ASCII bonitos

**Ejemplo de Salida:**
```
┌─────────────────────────────────────┐
│ 🛡 ESCANEO DE PROTECCIONES          │
├─────────────────────────────────────┤
│ ⛰️ Terremoto:                        │
│  ✓ 3 Lana WHITE                     │
│  ✓ 1 Slime                          │
│  Total: 4 bloques (efectivos: 4)   │
│  Shake -60% | Break -80% | Daño -100%│
│                                     │
│ 🔥 Lluvia de Fuego:                  │
│  ✓ Agua profunda (2+ bloques)       │
│  Explosión -60% | Fuego APAGADO     │
│                                     │
│ 🌪️ Huracán:                          │
│  ✓ Techo detectado                  │
│  Empuje -60% | Agachado -55%        │
│  Combo: -85% reducción total        │
└─────────────────────────────────────┘
```

**Efectos Visuales:**
- 🟢 Partículas **HAPPY_VILLAGER** en bloques absorbentes
- 🔵 Partículas **BUBBLE_POP** en agua protectora
- ⚪ Partículas **END_ROD** indicando techo
- 🔊 Sonido **NOTE_BLOCK_PLING** de confirmación

---

### 2️⃣ `/avo protecciones` - Guía Completa de Protecciones

**Descripción:**  
Muestra una guía educativa completa con toda la información sobre cómo protegerse de cada desastre.

**Contenido:**

#### 📚 Terremoto
- **Bloques Absorbentes** (radio 6 bloques):
  - Lana (16 colores): -15% cada una
  - Slime Block: -15%
  - Honey Block: -15%
  - Blue Ice: -10%
  - Hay Block: -10%
  - Sponge: -15%
- ⚠️ **Máximo:** 5 bloques efectivos
- 💡 **Reduce:** Shake, Break y Daño

#### 💧 Lluvia de Fuego
- **Protección de Agua** (área 3x3x3):
  - Agua Normal: -60% explosión
  - Agua Profunda (2+ bloques): -60% + inmunidad a evaporación
- 💡 **Coloca 3+ bloques cerca de ti**

#### 🌪️ Huracán
- **Protección Estructural:**
  - Techo (5+ bloques arriba): -60%
  - Agacharse (Sneaking): -55%
  - **COMBO** (Techo + Agachado): -85%
- 💡 **Durante ráfagas: agáchate siempre**

#### 💡 Consejos Generales
1. Usa `/avo escanear` para verificar
2. Prepara refugios **ANTES** del desastre
3. Combina múltiples protecciones
4. Revisa durabilidad con `/avo escanear`

**Efectos:**
- 🔊 Sonido **PLAYER_LEVELUP** al abrir la guía

---

## 🔧 Implementación Técnica

### Archivos Modificados
- `ApocalipsisCommand.java` (+~350 líneas)

### Nuevas Clases Internas
- `WaterScanResult` - Resultado de escaneo de agua

### Nuevos Métodos

| Método | Descripción |
|--------|-------------|
| `cmdEscanear()` | Comando principal de escaneo |
| `cmdProtecciones()` | Comando de guía educativa |
| `escanearBloquesAbsorbentes()` | Escanea bloques en radio 6 |
| `escanearAgua()` | Detecta agua en 3x3x3 |
| `escanearTecho()` | Verifica techo en 5 bloques |
| `spawnParticlesEnBloques()` | Partículas en bloques protectores |
| `spawnParticlesEnAgua()` | Partículas en agua |
| `getNombreMaterial()` | Traduce Material a nombre legible |

### Imports Agregados
```java
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import java.util.*;
```

---

## 📊 Estadísticas de Implementación

- **Líneas de código agregadas:** ~350
- **Métodos nuevos:** 8
- **Comandos nuevos:** 2
- **Clases auxiliares:** 1
- **Tiempo de compilación:** 19.4s
- **Estado:** ✅ BUILD SUCCESS

---

## 🎮 Casos de Uso

### Caso 1: Jugador Nuevo
```
1. Juan entra al servidor por primera vez
2. Usa /avo protecciones para aprender
3. Construye refugio con lana y agua
4. Usa /avo escanear para verificar
5. Ve "4 bloques = -60%" y se siente seguro
```

### Caso 2: Optimización de Base
```
1. María tiene base con protecciones
2. Usa /avo escanear cada día
3. Ve partículas verdes en bloques intactos
4. Optimiza colocación según el radio
5. Mantiene 5 bloques efectivos siempre
```

### Caso 3: Preparación para Desastre
```
1. Server anuncia: "PREPARACIÓN - 10 minutos"
2. Todos usan /avo escanear
3. Ven qué protecciones faltan
4. Construyen rápidamente
5. Confirman con otro /avo escanear antes del desastre
```

---

## 🔗 Integración con Mega Update

Este sistema complementa perfectamente los sistemas de feedback implementados en la mega update:

| Fase | Sistema Mega Update | Sistema Escaneo |
|------|-------------------|----------------|
| **ANTES** | N/A | `/avo escanear` + `/avo protecciones` |
| **DURANTE** | ActionBar cada 5s | Conocimiento previo aplicado |
| **DESPUÉS** | Estadísticas finales | Evaluación de daños |

**Sinergia:**
- Mega Update: Feedback **reactivo** durante desastre
- Sistema Escaneo: Feedback **preventivo** antes del desastre
- Juntos: Ciclo completo de **preparación → supervivencia → mejora**

---

## ✅ Testing Realizado

### Compilación
- ✅ Maven build exitoso
- ✅ Sin errores de compilación
- ✅ Solo warnings heredados (no críticos)

### Partículas
- ✅ `HAPPY_VILLAGER` para bloques absorbentes
- ✅ `BUBBLE_POP` para agua
- ✅ `END_ROD` para techo
- ✅ Límite de 50 partículas (prevención de lag)

### Formato Chat
- ✅ Bordes ASCII correctos
- ✅ Colores consistentes con tema del plugin
- ✅ Estadísticas claras y legibles

---

## 🚀 Próximos Pasos Sugeridos

### Fase 2 - GUI Visual (Opcional)
- Implementar menú con inventario clickeable
- Items representando cada tipo de protección
- Animaciones al pasar mouse sobre items

### Fase 3 - Persistencia
- Guardar escaneos históricos
- Comparar protecciones día a día
- Alertas cuando protecciones se deterioran

### Fase 4 - Recomendaciones IA
- Analizar debilidades del jugador
- Sugerir mejoras específicas
- Calcular costo de materiales necesarios

---

## 📝 Notas de Desarrollo

### Decisiones de Diseño

1. **Radio de Escaneo:**
   - Terremoto: 6 bloques (según config)
   - LluviaFuego: 3 bloques (área 3x3x3)
   - Huracán: 5 bloques arriba

2. **Límites de Performance:**
   - Máx 50 partículas en bloques absorbentes
   - Máx 30 partículas en agua
   - Duración: 20 segundos

3. **Compatibilidad:**
   - Paper API 1.21.8
   - Java 21
   - Compatible con sistemas existentes

### Lecciones Aprendidas

- ✅ `Particle.WATER_BUBBLE` no existe → usar `BUBBLE_POP`
- ✅ Formato ASCII necesita caracteres Unicode correctos
- ✅ Limitar partículas es crucial para evitar lag

---

## 📞 Soporte

**Si encuentras problemas:**
1. Verifica permisos: `avo.admin` para comandos
2. Revisa logs del servidor
3. Confirma Paper API 1.21.8+
4. Reporta en GitHub con `/avo debug on`

---

## 🏆 Créditos

**Desarrollado por:** Apocalipsis Plugin Team  
**Basado en:** Mega Update de Desastres v1.21.8  
**Inspirado en:** Feedback de comunidad

---

*"De feedback reactivo a prevención proactiva"* 🛡️
