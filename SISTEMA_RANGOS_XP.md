# Sistema de Rangos Basado en XP

## 🎯 Cambio Importante

El sistema de rangos ahora se basa en **niveles de experiencia (XP)** en lugar de Puntos de Supervivencia (PS).

---

## 📊 Mapeo de Niveles a Rangos

| Rango | Niveles Requeridos | Color |
|-------|-------------------|-------|
| **NOVATO** | 1-4 | §f (Blanco) |
| **EXPLORADOR** | 5-9 | §b (Aqua) |
| **SOBREVIVIENTE** | 10-14 | §a (Verde) |
| **VETERANO** | 15-19 | §e (Amarillo) |
| **LEYENDA** | 20-24 | §6 (Dorado) |
| **MAESTRO** | 25-29 | §c (Rojo) |
| **TITAN** | 30-34 | §5 (Púrpura) |
| **ABSOLUTO** | 35+ | §d (Rosa) |

---

## 🔄 Cambios Implementados

### 1. **MissionRank.java**
- ✅ Añadido método `fromLevel(int level)`
  - Mapea niveles de XP a rangos
  - Cada 5 niveles = 1 rango nuevo
  - Rango máximo (ABSOLUTO) a partir del nivel 35

### 2. **RankService.java**
- ✅ `getRank(Player)` - Ahora usa `ExperienceService.getLevel()`
  - Prioridad: Sistema de XP
  - Fallback: Sistema de PS (si ExperienceService no disponible)

- ✅ `getProgressToNextRank(Player)` - Progreso basado en XP
  - Llama a `ExperienceService.getProgressToNextLevel()`
  - Muestra progreso al siguiente nivel de XP, no PS

### 3. **ScoreboardManager.java**
- ✅ Barra de progreso actualizada:
  - **Antes**: `███████░░░ 150/200 PS`
  - **Ahora**: `███████░░░ 50/100 XP`
  - Se actualiza automáticamente cuando ganas XP

### 4. **AvoTabCompleter.java**
- ✅ Añadido completado de comandos:
  - `/avo xp` → `get`, `add`, `set`, `reset`
  - `/avo xp add <jugador>` → Sugerencias de nombres
  - `/avo xp add <jugador>` → Cantidades: `10`, `50`, `100`, `250`, `500`, `1000`
  - `/avo nivel` → Sugerencias de jugadores
  - `/avo experience` → Alias de `/avo xp`
  - `/avo level` → Alias de `/avo nivel`

---

## 🎮 Progresión del Jugador

### Ejemplo de Subida de Rango:

**Nivel 1-4 (NOVATO)**
```
Rango: §fNOVATO
Nivel: §b3 §8(§775/150 XP§8)
Progreso de rango:
███████████░░░░░░░░░ 75/150 XP
```

**Nivel 5 (EXPLORADOR)**
```
§e§l⬆ §6¡NIVEL SUBIDO! §e§l⬆
§7Has alcanzado el §bNivel 5
§7Nuevo rango: §bEXPLORADOR

Rango: §bEXPLORADOR
Nivel: §b5 §8(§70/200 XP§8)
Progreso de rango:
░░░░░░░░░░░░░░░░░░░░ 0/200 XP
```

---

## 🔧 Integración con Otros Sistemas

### **Habilidades (AbilityService)**
- Las habilidades se siguen aplicando por **rango**
- Ahora el rango se determina por **nivel de XP**
- Ejemplo: Nivel 10 = SOBREVIVIENTE → Habilidades de SOBREVIVIENTE

### **Recompensas (RewardService)**
- Las recompensas de rango se entregan al subir de **nivel**
- El nivel determina el rango → El rango determina las recompensas
- Ejemplo: Nivel 15 = VETERANO → Recompensas de VETERANO

### **Misiones (MissionService)**
- Las misiones otorgan **XP** en lugar de PS
- El XP acumulado determina el **nivel**
- El nivel determina el **rango**

---

## 📝 Ventajas del Nuevo Sistema

| Aspecto | Antes (PS) | Ahora (XP/Nivel) |
|---------|-----------|-----------------|
| **Progresión** | Basada en misiones solamente | Múltiples fuentes de XP |
| **Visibilidad** | PS no visible en scoreboard | XP y nivel siempre visibles |
| **Feedback** | Sin notificaciones de PS | Notificaciones de XP constantes |
| **Balance** | Difícil de ajustar | Configurable en recompensas.yml |
| **Consistencia** | PS separado de XP | Todo unificado en XP |

---

## 🎯 Comandos Actualizados

### **Ver Tu Progreso:**
```
/avo nivel              - Ver tu nivel y progreso
```
**Output:**
```
§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
§6Nivel de §fOliveerFr

§7Nivel Actual: §b§l5
§7XP Total: §e250

§7Progreso al nivel 6:
§a████████████████§7████
§750 / 100 XP §8(§e50.0%§8)
§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
```

### **Gestión de XP (Admin):**
```
/avo xp get <jugador>        - Consultar XP
/avo xp add <jugador> <cant> - Dar XP
/avo xp set <jugador> <cant> - Establecer XP
/avo xp reset <jugador>      - Resetear XP
```

---

## 🚀 Cómo Ganar XP

### **Fuentes Principales:**
1. **Misiones** - 100-400 XP por misión (85-90% del XP total)
2. **Completar todas las misiones del día** - Bonus adicional

### **Fuentes Secundarias:**
3. **Matar mobs** - 1-2 XP (hostiles/pasivos), 100 XP (bosses)
4. **Minar bloques** - 0.5-10 XP (con cooldown de 5s)
5. **Cosechar cultivos** - 0.5-2 XP
6. **Craftear items** - 1-5 XP
7. **Pescar** - 1-3 XP

---

## 📊 Fórmula de XP por Nivel

```
XP necesario = nivel_inicial(100) + nivel * multiplicador(50)
```

**Ejemplos:**
- Nivel 1 → 2: 100 + 1*50 = **150 XP**
- Nivel 5 → 6: 100 + 5*50 = **350 XP**
- Nivel 10 → 11: 100 + 10*50 = **600 XP**
- Nivel 20 → 21: 100 + 20*50 = **1100 XP**

---

## ⚙️ Configuración

Todos los valores son configurables en `recompensas.yml`:

```yaml
experiencia:
  nivel_inicial: 100
  multiplicador: 50
  
fuentes_xp:
  misiones:
    facil: 100
    medio: 200
    dificil: 400
  mobs:
    hostil: 2
    pacifico: 1
    boss: 100
  # ... etc
```

---

## 🔄 Compatibilidad con PS

El sistema de **Puntos de Supervivencia (PS)** sigue existiendo pero **NO afecta el rango**.

**PS ahora solo se usa para:**
- Estadísticas del jugador
- Comando `/avo status`
- Comando `/avo setps` (admin)

**El rango se determina ÚNICAMENTE por nivel de XP.**

---

## ✅ Estado del Sistema

| Componente | Estado |
|------------|--------|
| Sistema de XP | ✅ Funcional |
| Sistema de Niveles | ✅ Funcional |
| Rangos basados en nivel | ✅ Implementado |
| Progreso en Scoreboard | ✅ Actualizado |
| Progreso en TabList | ⏳ Pendiente |
| Comandos XP | ✅ Funcional |
| TabCompleter | ✅ Actualizado |
| Habilidades por rango | ✅ Funcional |
| Recompensas por rango | ✅ Funcional |

---

## 🎉 ¡Todo Listo!

El sistema de rangos ahora está **completamente unificado con el sistema de XP**:
- ✅ Progreso visible en tiempo real
- ✅ Múltiples fuentes de XP
- ✅ Barra de progreso actualizada automáticamente
- ✅ TabCompleter configurado
- ✅ Comandos funcionales

**¡Reinicia el servidor y disfruta del nuevo sistema!** 🚀
