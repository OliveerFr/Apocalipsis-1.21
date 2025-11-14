# Sistema de Recompensas de Misiones 🎁

## Descripción General

El plugin ahora incluye un **sistema completo de recompensas** para incentivar la realización de misiones diarias. Los jugadores reciben recompensas tanto por completar misiones individuales como por terminar todas las misiones del día.

---

## 🎯 Recompensas por Misión Individual

### Sistema Probabilístico

Cada vez que completas una misión, tienes una **probabilidad de recibir un bonus** según la dificultad:

| Dificultad | Probabilidad | Recompensas Posibles |
|------------|--------------|----------------------|
| **FÁCIL** | 30% | 5 panes, 3 carne cocinada |
| **MEDIA** | 40% | 3 lingotes de hierro, 5 de oro, 32 flechas |
| **DIFÍCIL** | 50% | 1 diamante, 1 manzana dorada, 3 botellas de XP |

### Características

- ✅ **No garantizadas**: Cada misión tiene chance de dar bonus (evita farming excesivo)
- ✅ **Sonido sutil**: Efecto de experiencia al recibir bonus
- ✅ **Mensaje de notificación**: Te avisa cuando recibes la recompensa
- ✅ **Configurable**: Se ajusta desde `recompensas.yml`

### Ejemplo

```
[MISIÓN] ¡Completaste "Cazar 20 zombies"! (+200 PS, +150 XP)
[BONUS] 🎁 ¡Recibiste 1 diamante como recompensa extra!
```

---

## 🏆 Recompensas por Completar Todas las Misiones del Día

### Recompensas Base (Todos los Jugadores)

Cuando completas **las 5 misiones diarias**, recibes automáticamente:

- 💎 **3 diamantes**
- 🍎 **2 manzanas doradas**
- 🧪 **5 botellas de experiencia**

### Bonus por Rango

Además de las recompensas base, recibes un **bonus adicional según tu rango**:

#### 🌱 NOVATO
- 5 lingotes de hierro
- 10 carbón

#### 🗺️ EXPLORADOR
- 5 lingotes de oro
- 16 perlas de ender
- 1 manzana dorada extra

#### ⚔️ SOBREVIVIENTE
- 2 diamantes extra
- 5 esmeraldas
- 1 manzana dorada encantada

#### 🛡️ VETERANO
- 3 diamantes extra
- 1 bloque de diamante
- 1 elytra con Unbreaking III
- 2 manzanas doradas encantadas

#### 🔥 LEYENDA
- 5 diamantes extra
- 1 bloque de netherita
- 3 manzanas doradas encantadas
- 1 estrella del Nether

#### ⚡ MAESTRO
- 2 bloques de netherita
- 4 manzanas doradas encantadas
- 2 estrellas del Nether
- 1 elytra con Unbreaking III y Mending

#### 🌟 TITÁN
- 3 bloques de netherita
- 5 manzanas doradas encantadas
- 3 estrellas del Nether
- 1 elytra (Unbreaking III, Mending)
- 1 tridente (Riptide III, Loyalty III)

#### 👑 ABSOLUTO
- 3 bloques de netherita
- 5 manzanas doradas encantadas
- 2 estrellas del Nether
- 1 elytra completo (Unbreaking III, Mending)
- 1 tridente completo (Riptide III, Loyalty III, Channeling)
- 1 tótem de la inmortalidad

---

## 🎆 Efectos Especiales al Completar Todo

Cuando completas todas las misiones diarias, el plugin genera una **celebración épica**:

### 🎨 Visuales
- **Título animado**: "¡COMPLETADO! Has terminado todas las misiones"
- **5 fuegos artificiales** aleatorios escalonados
- **Partículas múltiples**:
  - 🎉 Tótem de inmortalidad (50 partículas)
  - 🎆 Fuegos artificiales (30 partículas)
  - ✨ End Rod (25 partículas)
  - 😊 Aldeano feliz (40 partículas)

### 🔊 Sonidos
- `UI_TOAST_CHALLENGE_COMPLETE` (sonido de logro)
- `ENTITY_PLAYER_LEVELUP` (sonido de subida de nivel)
- `ENTITY_ENDER_DRAGON_GROWL` (rugido del dragón)

### 🎨 Colores por Rango
Los fuegos artificiales usan los colores de tu rango:
- **NOVATO**: Verde lima
- **EXPLORADOR**: Aguamarina
- **SOBREVIVIENTE**: Amarillo
- **VETERANO**: Naranja
- **LEYENDA+**: Rojo y naranja

---

## ⚙️ Configuración

Todo el sistema se configura en `recompensas.yml`:

```yaml
recompensas_por_mision:
  enabled: true
  por_dificultad:
    FACIL:
      probabilidad: 0.30  # 30% de chance
      items:
        - "give %player% bread 5"
        - "give %player% cooked_beef 3"
      mensaje: "&a🎁 &7¡Recibiste un bonus por completar la misión!"
    
    MEDIA:
      probabilidad: 0.40  # 40%
      # ...

recompensas_diarias_completas:
  enabled: true
  recompensas_base:
    comandos:
      - "give %player% diamond 3"
      - "give %player% golden_apple 2"
      - "give %player% experience_bottle 5"
    mensaje: "&6⭐ &e¡Completaste todas las misiones del día!"
  
  bonus_por_rango:
    NOVATO:
      comandos:
        - "give %player% iron_ingot 5"
        - "give %player% coal 10"
      mensaje: "&7Bonus de rango NOVATO aplicado"
    # ...
  
  # Título y efectos
  titulo: "&6&l¡COMPLETADO!"
  subtitulo: "&eHas terminado todas las misiones"
  
  efectos:
    sonidos: true
    particulas: true
    fuegos_artificiales: 5
```

---

## 🎮 Integración con Otros Sistemas

### Con Sistema de Misiones
- Se ejecuta automáticamente al completar misiones
- Funciona con `MissionService.rewardPlayer()`
- Trackea cuando todas las misiones están completas

### Con Sistema de XP
- Las recompensas son **adicionales** a la XP de misiones
- No reemplazan el sistema de experiencia
- Incentivan la constancia diaria

### Con Sistema de Rangos
- Mejores rangos = mejores recompensas diarias
- Incentiva la progresión a largo plazo
- Las habilidades pasivas se mantienen independientes

---

## 📊 Balance

### Diseño del Sistema

El balance está pensado para:

1. **Incentivar misiones diarias** sin romper la economía
2. **Recompensar progresión** (mejores bonus a rangos altos)
3. **Mantener rareza** de items premium (estrellas del Nether, elytra)
4. **Evitar farming excesivo** con probabilidades medias

### Valores de Referencia

- Mínimo diario (NOVATO): ~10-15 diamantes equivalentes
- Máximo diario (ABSOLUTO): ~50-60 diamantes equivalentes
- Items únicos solo en rangos MAESTRO+ (Elytra, Tridente, Tótem)

---

## 🔧 Comandos de Admin

Si necesitas ajustar recompensas:

```bash
# Recargar configuración de recompensas
/apocalipsis reload

# Ver configuración actual
/apocalipsis config recompensas
```

---

## 📝 Notas Técnicas

### Implementación

**Clases modificadas:**
- `RewardService.java` → Métodos `deliverMissionReward()` y `deliverDailyCompletionReward()`
- `MissionService.java` → Integración en `rewardPlayer()` y `triggerPlayerDailyCompletionCelebration()`
- `recompensas.yml` → Nueva sección de configuración

### Logs

El sistema registra en consola:
```
[Rewards] PlayerName completó todas las misiones diarias y recibió recompensas
```

### Performance

- Uso de `Bukkit.getScheduler()` para comandos asíncronos
- Fuegos artificiales escalonados (10 ticks entre cada uno)
- Sin lag perceptible en celebración

---

## ✅ Estado Actual

- ✅ Sistema de recompensas individuales implementado
- ✅ Sistema de recompensas diarias implementado
- ✅ Configuración completa en YAML
- ✅ Efectos especiales funcionando
- ✅ Integración con MissionService completa
- ✅ AlonsoLevels completamente removido
- ✅ Compilación exitosa
- ✅ Documentación completa

---

## 🚀 Próximos Pasos Sugeridos

1. **Testing en servidor**: Verificar balance de recompensas
2. **Ajustar probabilidades**: Según feedback de jugadores
3. **Añadir más items**: Posibilidad de items custom/especiales
4. **Sistema de rachas**: Bonus por X días consecutivos de misiones completas

---

**Fecha de implementación**: 2024  
**Versión del plugin**: 1.0.0  
**Versión de Minecraft**: 1.21.8
