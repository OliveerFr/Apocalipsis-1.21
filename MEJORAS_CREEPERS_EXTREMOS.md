# MEJORAS DE CREEPERS - Sistema Extremo de Peligro

## 📅 Versión 1.22.49

## 🎯 Objetivo
Convertir los creepers en una amenaza EXTREMADAMENTE peligrosa con explosiones devastadoras y capacidades mejoradas.

---

## 🔥 Cambios Implementados

### 1. Nuevo Sistema: `CreeperEnhancer`

**Archivo:** `src/main/java/me/apocalipsis/listeners/CreeperEnhancer.java`

#### Características del Sistema:

1. **Explosiones Amplificadas (3.5x)**
   - Las explosiones de creepers son **350% más poderosas**
   - Radio de explosión aumentado de 3 a 6 bloques
   - Creepers cargados son aún más devastadores
   - Las explosiones dejan fuego

2. **Creepers Cargados Frecuentes (35%)**
   - 35% de probabilidad de spawn como creeper cargado
   - Normalmente solo ocurre con rayos (<1%)
   - Mucho más común y peligroso

3. **Velocidad Mejorada**
   - Speed II permanente
   - Los creepers son mucho más rápidos
   - Más difíciles de escapar

4. **Resistencia Aumentada**
   - Resistance II permanente
   - Aguantan muchos más golpes
   - Vida aumentada de 20 a 30 HP (50% más resistentes)

---

## ⚙️ Configuración

**Archivo:** `config.yml`

```yaml
# CREEPERS MEJORADOS - Sistema de creepers extremadamente peligrosos
creeper_mejoras:
  multiplicador_explosion: 3.5    # Multiplicador de poder de explosión (3.5 = 350% más potente)
  probabilidad_cargado: 0.35      # 35% de probabilidad de spawn como cargado (normalmente <1%)
  velocidad_extra: true           # Dar Speed II permanente
  resistencia_extra: true         # Dar Resistance II permanente
```

### Parámetros Configurables:

- **multiplicador_explosion**: Control del poder de explosión (por defecto 3.5)
- **probabilidad_cargado**: Probabilidad de spawn como cargado (por defecto 0.35 = 35%)
- **velocidad_extra**: Activar/desactivar Speed II (por defecto true)
- **resistencia_extra**: Activar/desactivar Resistance II (por defecto true)

---

## 🔧 Integración

### Archivos Modificados:

1. **Apocalipsis.java** (línea ~614-617)
   - Registrado nuevo listener `CreeperEnhancer`
   - Mensaje de confirmación en consola

2. **config.yml**
   - Nueva sección `creeper_mejoras` con parámetros configurables

3. **target/classes/config.yml**
   - Sincronizado con la configuración principal

---

## 🎮 Impacto en el Gameplay

### Antes:
- ✅ Creepers normales con explosión estándar
- ✅ Fáciles de matar
- ✅ Velocidad normal
- ✅ Raramente cargados

### Después:
- ⚠️ **EXPLOSIONES DEVASTADORAS** (3.5x más potentes)
- ⚠️ **35% spawn como cargados** (extremadamente peligroso)
- ⚠️ **Velocidad aumentada** (Speed II permanente)
- ⚠️ **Muy resistentes** (30 HP + Resistance II)
- ⚠️ **Radio de explosión 6 bloques** (el doble del normal)
- ⚠️ **Explosiones con fuego**

---

## 📊 Estadísticas Técnicas

| Parámetro | Antes | Después | Incremento |
|-----------|-------|---------|------------|
| Poder de Explosión | 3.0 | 10.5 (3.5x) | +250% |
| Radio Explosión | 3 bloques | 6 bloques | +100% |
| Vida | 20 HP | 30 HP | +50% |
| Velocidad | Normal | Speed II | +40% |
| Resistencia | Ninguna | Resistance II | +20% reducción daño |
| % Cargados | <1% | 35% | +3400% |

---

## ⚠️ ADVERTENCIAS

1. **EXTREMADAMENTE PELIGROSO**: Los creepers ahora son una amenaza letal
2. **DAÑO MASIVO**: Pueden destruir bases enteras
3. **DIFÍCIL DE ESCAPAR**: Velocidad aumentada hace imposible correr
4. **RESISTENTES**: Necesitan muchos más golpes para morir
5. **EXPLOSIONES FRECUENTES**: 35% de creepers cargados significa explosiones constantes

---

## 🛠️ Comandos de Gestión

Los creepers se configuran automáticamente. Para modificar su comportamiento:

1. Editar `config.yml` en la sección `creeper_mejoras`
2. Ejecutar `/avo reload` para recargar configuración
3. Los cambios aplicarán a los nuevos creepers que spawnen

---

## 🔍 Testing

### Verificación de Funcionamiento:

1. Los creepers deberían tener velocidad aumentada
2. Las explosiones deberían ser mucho más grandes
3. ~35% de creepers deberían estar cargados (brillantes)
4. Los creepers deberían aguantar más golpes
5. Mensaje en consola al iniciar: `[CreeperEnhancer] ✓ Creepers mejorados activados - ¡PELIGRO EXTREMO!`

### Log de Compilación:

```
[INFO] Building Apocalipsis 1.22.49
✓ CreeperEnhancer.java compilado correctamente
✓ Listener registrado en Apocalipsis.java
✓ Configuración añadida a config.yml
```

---

## 📝 Notas de Desarrollo

- Sistema completamente modular y configurable
- Compatible con el sistema anti-explosión existente (ExplosionGuard)
- No interfiere con otros listeners de mobs
- Prioridad NORMAL para permitir que otros plugins puedan modificar
- Usa ExplosionPrimeEvent para modificar explosión antes de que ocurra
- Logs detallados cuando `debug: true` en config.yml

---

## 🎯 Próximas Mejoras Sugeridas

1. Agregar partículas especiales a creepers mejorados
2. Sonido de advertencia cuando un creeper cargado está cerca
3. Sistema de "oleadas de creepers" en eventos
4. Creepers con efectos especiales (wither, poison, etc.)
5. Variantes de creepers (fuego, hielo, eléctricos)

---

## ✅ Checklist de Implementación

- [x] Crear CreeperEnhancer.java
- [x] Registrar listener en Apocalipsis.java
- [x] Añadir configuración a config.yml
- [x] Sincronizar target/classes/config.yml
- [x] Compilar proyecto
- [x] Documentar cambios

---

**¡Los creepers ahora son una AMENAZA EXTREMA! ☠️💥**
