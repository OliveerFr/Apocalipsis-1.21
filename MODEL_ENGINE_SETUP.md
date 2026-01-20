# Model Engine - Integración con AperturaEndEvent

## 🎯 Implementación Completada

Se ha implementado la integración completa con **Model Engine** para el evento "La Apertura del End".

### ✅ Cambios Realizados

#### 1. **pom.xml** - Dependencias
- ✅ Repositorio Lumine añadido
- ✅ Dependencia Model Engine R4.0.7 (provided scope)

#### 2. **AperturaEndEvent.java** - Código
- ✅ Imports de Model Engine API
- ✅ Variables para `ModeledEntity` y `ActiveModel`
- ✅ Detección automática de Model Engine al iniciar
- ✅ Método `spawnearDragonModelEngine()` - Modo épico
- ✅ Método `spawnearDragonVanilla()` - Modo compatibilidad
- ✅ Método `fallbackVanilla()` - Sistema de respaldo
- ✅ Limpieza del modelo en `onStop()`

#### 3. **apertura_end.yml** - Configuración
- ✅ Sección `modelo:` con configuración de Model Engine
- ✅ `model_engine_id: "corrupted_dragon"` (configurable)
- ✅ Opciones de partículas personalizadas

#### 4. **plugin.yml** - Soft Dependencies
- ✅ ModelEngine añadido a soft-depend
- ✅ MythicMobs añadido (futuro)

---

## 📦 Instalación

### 1. Descargar Model Engine
- Descarga Model Engine R4.x desde: https://mythiccraft.io/index.php?resources/model-engine%E2%80%94ultimate-entity-model-manager-1-16-5-1-20-4.389/
- Coloca `ModelEngine-X.X.X.jar` en `/plugins/`

### 2. Crear el Modelo Custom
Tienes dos opciones:

#### Opción A: Usar Blockbench (Recomendado)
1. Descarga **Blockbench**: https://www.blockbench.net/
2. Crea tu modelo de dragón custom
3. Exporta como `.bbmodel`
4. Renombra el archivo a `corrupted_dragon.bbmodel`
5. Coloca en `/plugins/ModelEngine/models/`

#### Opción B: Modelo Ejemplo
Si no tienes modelo, puedes:
1. Descargar modelos de ejemplo de Model Engine
2. Renombrar uno a `corrupted_dragon.bbmodel`
3. Modificar `apertura_end.yml` para usar otro ID:
   ```yaml
   modelo:
     model_engine_id: "tu_modelo_aqui"
   ```

### 3. Configurar Model Engine
Edita `/plugins/ModelEngine/config.yml` si es necesario.

---

## ⚙️ Configuración

### apertura_end.yml
```yaml
modelo:
  # ID del modelo (debe coincidir con el archivo .bbmodel)
  model_engine_id: "corrupted_dragon"
  
  # Tamaño del modelo
  escala: 1.5
  
  # Efectos de spawn
  particulas_spawn:
    - type: "PORTAL"
      cantidad: 200
      radio: 2.0
    - type: "DRAGON_BREATH"
      cantidad: 100
      radio: 3.0
```

---

## 🚀 Funcionamiento

### Detección Automática
El plugin detecta automáticamente si Model Engine está instalado:

```
[Apertura End] ✓ Model Engine detectado - Modo épico activado
[Apertura End] Modelo configurado: corrupted_dragon
```

### Modo Épico (con Model Engine)
- ✅ Dragón vanilla **invisible** (hitbox funcional)
- ✅ Modelo custom **visible** encima del dragón
- ✅ Efectos de partículas mejorados
- ✅ El dragón mantiene su IA y comportamiento vanilla

### Modo Vanilla (sin Model Engine)
- ✅ Dragón vanilla **normal** mejorado con efectos
- ✅ Compatibilidad total si Model Engine no está instalado
- ✅ Fallback automático si el modelo falla

### Sistema de Respaldo
Si ocurre un error con Model Engine:
```
[Apertura End] ⚠ No se pudo cargar el modelo 'corrupted_dragon' - usando vanilla
```
El dragón se hará visible automáticamente.

---

## 🎮 Comandos de Prueba

```bash
# Iniciar evento (admin)
/avo evento5 start

# El dragón aparecerá con el modelo si Model Engine está activo
# Si no está, usará el dragón vanilla mejorado
```

---

## 🔧 Solución de Problemas

### El modelo no aparece
1. **Verifica que Model Engine esté instalado:**
   ```
   /plugins
   ```
   Debe aparecer `ModelEngine` en verde

2. **Verifica el ID del modelo:**
   - Abre `/plugins/ModelEngine/models/`
   - El archivo debe ser exactamente: `corrupted_dragon.bbmodel`
   - O cambia el ID en `apertura_end.yml`

3. **Revisa la consola:**
   ```
   [Apertura End] ✓ Model Engine detectado
   [Apertura End] ✓ Modelo 'corrupted_dragon' aplicado al dragón
   ```

### El dragón es invisible
- Esto es **normal** si Model Engine funciona correctamente
- El dragón vanilla está invisible, el modelo custom es visible
- Si NO quieres usar Model Engine, desinstálalo y usará vanilla

### Error al compilar
Si obtienes errores de compilación:
1. Ejecuta: `mvn clean install`
2. El repositorio Lumine puede tardar en responder
3. Model Engine se descargará automáticamente como dependencia

---

## 📝 Notas Técnicas

### Arquitectura
- **Dragón Vanilla**: Provee hitbox, IA, comportamiento de combate
- **Modelo Custom**: Solo visual, se sincroniza con el dragón
- **Sincronización**: Model Engine sincroniza automáticamente posición/rotación

### Rendimiento
- Sin impacto significativo (Model Engine es muy optimizado)
- El modelo custom reemplaza textura, no añade entidad extra

### Compatibilidad
- ✅ Funciona sin Model Engine (fallback vanilla)
- ✅ Compatible con Spigot/Paper 1.21+
- ✅ No requiere MythicMobs (opcional para futuro)

---

## 🎨 Personalización

### Cambiar Efectos de Partículas
Edita `apertura_end.yml`:
```yaml
modelo:
  particulas_spawn:
    - type: "SOUL_FIRE_FLAME"  # Llamas de almas
      cantidad: 300
      radio: 3.0
    - type: "SCULK_SOUL"        # Partículas sculk
      cantidad: 150
      radio: 2.5
```

### Cambiar Tamaño del Modelo
```yaml
modelo:
  escala: 2.0  # Doble de tamaño (épico)
```

### Usar Otro Modelo
1. Coloca tu modelo en `/plugins/ModelEngine/models/tu_modelo.bbmodel`
2. Edita `apertura_end.yml`:
   ```yaml
   modelo:
     model_engine_id: "tu_modelo"
   ```

---

## 📚 Recursos

- **Model Engine Wiki**: https://git.mythiccraft.io/mythiccraft/model-engine/-/wikis/home
- **Blockbench**: https://www.blockbench.net/
- **Modelos de Ejemplo**: https://discord.gg/mythiccraft

---

## ✨ Estado

- ✅ Implementación completa
- ✅ Detección automática
- ✅ Fallback system
- ✅ Configuración flexible
- 🚀 Listo para producción

**Nota**: Si no tienes un modelo custom, el evento funcionará perfectamente en modo vanilla. Model Engine es **opcional** pero mejora la experiencia visual.
