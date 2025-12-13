# 👥 Sistema de Referidos y Bonificación por Población

## Versión 1.0 - 13 de Diciembre 2025

Este documento describe los sistemas dinámicos de recompensas por traer jugadores nuevos y bonificaciones cuando hay más población de lo normal en el servidor.

---

## 📋 ÍNDICE
1. [Sistema de Referidos](#sistema-de-referidos)
2. [Bonificación por Población](#bonificación-por-población)
3. [Configuración](#configuración)
4. [Implementación](#implementación)

---

## 👥 SISTEMA DE REFERIDOS

### Objetivo
Incentivar a los jugadores a traer amigos al servidor mediante recompensas inmediatas y progresivas.

### ¿Cómo funciona?

#### 1. Invitación Inmediata
Cuando un jugador nuevo se une usando tu código de referido:

**Recompensas instantáneas:**
- ✅ **+500 XP** (inmediato)
- ✅ **+5 Diamantes**
- ✅ **+3 Manzanas Doradas**
- ✅ **+10 Experience Bottles**
- 🎬 Título épico: "¡NUEVO JUGADOR!"
- 💬 Mensaje en chat con el nombre del referido

**Comando:** `/invitar <nombre_jugador>`

---

#### 2. Recompensas Progresivas (Hitos del Referido)

A medida que tu referido sube de nivel, TÚ recibes recompensas adicionales:

| Hito | Recompensas | Detalles |
|------|-------------|----------|
| **EXPLORADOR** (Nivel 2) | +300 XP, +3 Diamantes, +5 Perlas | Cuando alcanza rango 2 |
| **SOBREVIVIENTE** (Nivel 3) | +500 XP, +5 Diamantes, +1 Netherite Scrap, **+5 Bloques PS** | Cuando alcanza rango 3 |
| **VETERANO** (Nivel 4) | +800 XP, +1 Netherite Ingot, +2 Manzanas Enc., **+8 Bloques PS** | Cuando alcanza rango 4 |
| **LEYENDA** (Nivel 5+) | +1500 XP, +2 Netherite Ingots, +1 Estrella, **+15 Bloques PS** | Cuando alcanza rango 5 |

> ⚠️ **Importante:** Las recompensas se dan automáticamente cuando el referido alcanza cada hito.

---

#### 3. Bonus Acumulativo Permanente

Tener varios referidos **activos** te da multiplicadores de XP PERMANENTES:

| Referidos Activos | Bonus XP | Recompensa Extra |
|-------------------|----------|------------------|
| **3+ referidos** | +5% XP permanente | - |
| **5+ referidos** | +10% XP permanente | - |
| **10+ referidos** | +15% XP permanente | **+1 Élitro** |

**¿Qué es un "referido activo"?**
- Un jugador que ha jugado en las últimas **7 días** (168 horas)
- Si un referido deja de jugar por más de 7 días, pierde su estado "activo"
- Si vuelve a jugar, recupera su estado activo automáticamente

---

### Ejemplo de Progresión

**Día 1:** Invitas a "MiAmigo123"
```
✓ Inmediato: +500 XP, +5 Diamantes, +3 Manzanas, +10 XP Bottles
```

**Día 3:** MiAmigo123 alcanza Explorador
```
✓ +300 XP, +3 Diamantes, +5 Perlas de Ender
```

**Día 7:** MiAmigo123 alcanza Sobreviviente
```
✓ +500 XP, +5 Diamantes, +1 Netherite Scrap, +5 Bloques de Protección
```

**Día 10:** Invitas a "Jugador456" y "Player789"
```
✓ Ahora tienes 3 referidos activos → +5% XP PERMANENTE
✓ Este bonus se aplica a TODO el XP que ganes
```

**Día 30:** Tus referidos siguen activos y subiendo de nivel
```
✓ Continúas recibiendo recompensas cada vez que suben de rango
✓ Tu multiplicador de +5% XP sigue activo mientras jueguen
```

---

## 🌍 BONIFICACIÓN POR POBLACIÓN

### Objetivo
Recompensar a todos los jugadores cuando el servidor está más activo de lo normal.

### Población Base
**Configuración actual:** 5 jugadores (promedio normal)

### Niveles de Bonificación

#### 🟢 Nivel Moderado (6-8 jugadores)
- **Multiplicador:** x1.10 (+10% XP)
- **Icono en menú:** Lingote de Oro
- **ActionBar:** "⚡ Servidor Activo: +10% XP"

#### 🟡 Nivel Alto (9-12 jugadores)
- **Multiplicador:** x1.20 (+20% XP)
- **Icono en menú:** Diamante
- **ActionBar:** "⚡ Servidor Muy Activo: +20% XP"

#### 🟠 Nivel Muy Alto (13-16 jugadores)
- **Multiplicador:** x1.30 (+30% XP)
- **Icono en menú:** Diamante
- **ActionBar:** "⚡ ¡Servidor Épico!: +30% XP"

#### 🔴 RÉCORD (17+ jugadores)
- **Multiplicador:** x1.50 (+50% XP)
- **Bonus horario:** +200 XP, +3 Diamantes, +2 Manzanas cada hora
- **Icono en menú:** Estrella del Nether
- **ActionBar:** "⚡ ¡RÉCORD DEL SERVIDOR!: +50% XP"
- **Efectos:** Fuegos artificiales + Partículas + Sonidos

### Notificaciones

**Cuando sube el nivel de población:**
```
[POBLACIÓN] 9+ jugadores online! +20% XP
```

**Cuando alcanzas RÉCORD:**
```
[POBLACIÓN] 17+ jugadores! +50% XP + Bonus horario
⚡ ¡RÉCORD DEL SERVIDOR!: +50% XP
```

**Bonus Horario (solo en RÉCORD):**
```
⭐ HORA COMPLETA EN RÉCORD! +200 XP, +3 Diamantes
```

### Visualización en el Menú

El slot **40: Referidos y Población** muestra:
- **Jugadores online actuales**
- **Tus referidos activos**
- **Bonus de población activo**
- **Cambio de icono según población:**
  - 🔶 Hierro: Sin bonus (1-5 jugadores)
  - 🟡 Oro: Moderado (6-8 jugadores)
  - 💎 Diamante: Alto/Muy Alto (9-16 jugadores)
  - ⭐ Estrella: RÉCORD (17+ jugadores)

---

## ⚙️ CONFIGURACIÓN

### Archivo: `recompensas.yml`

#### Sistema de Referidos
```yaml
xp_dinamico:
  sistema_referidos:
    enabled: true
    
    # Recompensa inmediata
    recompensa_invitacion:
      xp: 500
      comandos:
        - "give %player% minecraft:diamond 5"
        - "give %player% minecraft:golden_apple 3"
        - "give %player% minecraft:experience_bottle 10"
      mensaje: "&6&l[REFERIDO] &e%jugador_nuevo% &ase unió por ti! &6+500 XP"
    
    # Hitos del referido
    hitos_referido:
      nivel_2:
        xp: 300
        comandos: [...]
      nivel_3:
        xp: 500
        comandos: [...]
      nivel_4:
        xp: 800
        comandos: [...]
      nivel_5:
        xp: 1500
        comandos: [...]
    
    # Bonus acumulativo
    bonus_acumulativo:
      enabled: true
      3_referidos:
        multiplicador_xp: 1.05    # +5%
      5_referidos:
        multiplicador_xp: 1.10    # +10%
      10_referidos:
        multiplicador_xp: 1.15    # +15%
        comandos:
          - "give %player% minecraft:elytra 1"
    
    # Definición de "activo"
    tiempo_actividad_horas: 168  # 7 días
```

#### Bonificación por Población
```yaml
xp_dinamico:
  bonificacion_poblacion:
    enabled: true
    poblacion_base: 5        # Ajusta según tu servidor
    
    niveles:
      moderado:
        jugadores_minimos: 6
        multiplicador: 1.10  # +10%
      alto:
        jugadores_minimos: 9
        multiplicador: 1.20  # +20%
      muy_alto:
        jugadores_minimos: 13
        multiplicador: 1.30  # +30%
      record:
        jugadores_minimos: 17
        multiplicador: 1.50  # +50%
        recompensas_horarias:
          enabled: true
          xp: 200
          comandos:
            - "give %player% minecraft:diamond 3"
            - "give %player% minecraft:golden_apple 2"
    
    notificar_cambios: true
    notificar_actionbar: true
    intervalo_notificacion_seg: 300  # Cada 5 minutos
```

### Ajustes Recomendados

**Para servidor pequeño (1-10 jugadores promedio):**
```yaml
poblacion_base: 3
moderado.jugadores_minimos: 4
alto.jugadores_minimos: 6
muy_alto.jugadores_minimos: 8
record.jugadores_minimos: 10
```

**Para servidor mediano (10-20 jugadores promedio):**
```yaml
poblacion_base: 8
moderado.jugadores_minimos: 10
alto.jugadores_minimos: 13
muy_alto.jugadores_minimos: 16
record.jugadores_minimos: 20
```

**Para servidor grande (20+ jugadores promedio):**
```yaml
poblacion_base: 15
moderado.jugadores_minimos: 18
alto.jugadores_minimos: 22
muy_alto.jugadores_minimos: 26
record.jugadores_minimos: 30
```

---

## 💻 IMPLEMENTACIÓN

### Estado Actual: ✅ Configuración Lista / ⚠️ Código Pendiente

#### ✅ Completado
- [x] Configuración completa en `recompensas.yml`
- [x] Slot en menú `/avo menu` (Slot 40)
- [x] Método `getBonusPoblacion()` implementado
- [x] Método `showReferidosInfo()` implementado
- [x] Handler de click en menú

#### ⚠️ Pendiente de Implementar

##### 1. Sistema de Referidos
**Archivo a crear:** `ReferralSystem.java`

**Funcionalidades:**
- Comando `/invitar <jugador>`
- Base de datos para rastrear referidos
- Sistema de tracking de "jugador activo" (últimas 168h)
- Event listeners para detectar cuando referido sube de nivel
- Sistema de recompensas automático
- Cálculo de multiplicador acumulativo

**Métodos principales:**
```java
public class ReferralSystem {
    // Registrar referido
    public void registerReferral(Player inviter, Player referred);
    
    // Obtener referidos activos
    public int getActiveReferralsCount(Player player);
    
    // Dar recompensa inmediata
    public void giveImmediateReward(Player inviter, Player referred);
    
    // Dar recompensa por hito
    public void giveMilestoneReward(Player inviter, Player referred, MissionRank rank);
    
    // Calcular multiplicador acumulativo
    public double getAccumulativeMultiplier(Player player);
    
    // Verificar si jugador está activo
    public boolean isReferralActive(UUID referredUUID);
}
```

##### 2. Sistema de Bonificación por Población
**Archivo a crear:** `PopulationBonusSystem.java`

**Funcionalidades:**
- Monitoreo constante de jugadores online
- Cálculo dinámico de multiplicador
- Notificaciones de cambio de nivel
- Sistema de bonus horario para RÉCORD
- ActionBar persistente mostrando bonus

**Métodos principales:**
```java
public class PopulationBonusSystem {
    // Obtener multiplicador actual
    public double getCurrentMultiplier();
    
    // Obtener nivel de población
    public String getPopulationLevel();
    
    // Verificar y dar bonus horario
    public void checkHourlyBonus();
    
    // Notificar cambio de nivel
    public void notifyLevelChange(String level);
    
    // Actualizar ActionBar de todos
    public void updateAllActionBars();
}
```

##### 3. Integración con RewardService
**Modificar:** `RewardService.java`

**Cambios necesarios:**
```java
// En el método calculateXP(), agregar:
double finalXP = baseXP;

// Multiplicador de referidos
if (referralSystem != null) {
    double referralMultiplier = referralSystem.getAccumulativeMultiplier(player);
    finalXP *= referralMultiplier;
}

// Multiplicador de población
if (populationBonusSystem != null) {
    double populationMultiplier = populationBonusSystem.getCurrentMultiplier();
    finalXP *= populationMultiplier;
}
```

##### 4. Base de Datos
**Tabla necesaria:** `apocalipsis_referrals`

```sql
CREATE TABLE apocalipsis_referrals (
    id INT AUTO_INCREMENT PRIMARY KEY,
    inviter_uuid VARCHAR(36) NOT NULL,
    referred_uuid VARCHAR(36) NOT NULL,
    referred_name VARCHAR(16) NOT NULL,
    invited_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    current_rank VARCHAR(32) DEFAULT 'NOVATO',
    is_active BOOLEAN DEFAULT TRUE,
    INDEX idx_inviter (inviter_uuid),
    INDEX idx_referred (referred_uuid),
    INDEX idx_active (is_active)
);
```

##### 5. Comandos a Crear

**`/invitar <jugador>`**
```java
@Command(name = "invitar", permission = "apocalipsis.referral")
public void onInvite(Player sender, String targetName) {
    // 1. Verificar que el jugador esté online
    // 2. Verificar que no esté ya registrado
    // 3. Verificar que sea nuevo (< 1 hora de juego)
    // 4. Registrar en base de datos
    // 5. Dar recompensas inmediatas
    // 6. Notificar a ambos jugadores
}
```

**`/misreferidos`**
```java
@Command(name = "misreferidos", permission = "apocalipsis.referral.list")
public void onListReferrals(Player sender) {
    // Mostrar lista de referidos:
    // - Nombre
    // - Estado (Activo/Inactivo)
    // - Rango actual
    // - Última conexión
    // - Multiplicador total actual
}
```

---

## 📊 BALANCE Y ECONOMÍA

### Recompensas por Referidos

**Total por referido que llega a LEYENDA:**
- XP: 500 + 300 + 500 + 800 + 1500 = **3,600 XP**
- Diamantes: 5 + 3 + 5 = **13 Diamantes**
- Bloques PS: 5 + 8 + 15 = **28 Bloques de Protección**
- Netherite: 1 Scrap + 1 Ingot + 2 Ingots = **~4 Lingotes**
- Extras: Perlas, Manzanas, Estrella del Nether

**Con 10 referidos activos:**
- Bonus permanente: +15% XP
- Recompensa única: 1 Élitro

### Bonificación por Población

**Comparativa con población base (5 jugadores):**
```
5 jugadores  = x1.0  (normal)
6 jugadores  = x1.1  (+10%)
9 jugadores  = x1.2  (+20%)
13 jugadores = x1.3  (+30%)
17 jugadores = x1.5  (+50%) + 200 XP/hora
```

**Ganancia horaria en RÉCORD (17+ jugadores):**
- Cada acción de XP vale +50%
- Bonus: +200 XP cada hora
- Bonus: +3 Diamantes cada hora
- Bonus: +2 Manzanas cada hora

**Ejemplo con misión fácil (80 XP base):**
```
5 jugadores:  80 XP
17 jugadores: 120 XP (+50%)
+ Bonus horario: 200 XP
+ Si tienes 10 referidos: 120 * 1.15 = 138 XP
```

---

## 🎮 ESTRATEGIAS DE JUEGO

### Para Jugadores

**Maximiza tus recompensas:**
1. Invita amigos temprano (más tiempo para que suban de nivel)
2. Ayuda a tus referidos a alcanzar rangos altos rápido
3. Juega cuando hay más población online
4. Mantén a tus referidos activos (jueguen al menos 1 vez/semana)

**Objetivo ideal:**
- 10+ referidos activos = +15% XP permanente
- Jugar en horas de alta población = +20-50% XP temporal
- **Combinado: x1.69 a x1.72 multiplicador total**

### Para el Servidor

**Beneficios del sistema:**
- ✅ Incentiva traer jugadores nuevos
- ✅ Promueve la retención (referidos activos)
- ✅ Aumenta actividad en horas pico
- ✅ Crea comunidad más unida
- ✅ Recompensa jugadores leales

**Métricas a monitorear:**
- Promedio de jugadores online por hora
- Tasa de conversión (invitados → activos)
- Retención de referidos a 7/30 días
- Horas pico de población

---

## 📝 NOTAS FINALES

### Prioridades de Implementación
1. **Alta:** Sistema de Referidos básico + Comando `/invitar`
2. **Alta:** Sistema de Bonificación por Población
3. **Media:** Bonus horario en RÉCORD
4. **Media:** Comando `/misreferidos`
5. **Baja:** Efectos visuales avanzados

### Testing Recomendado
- Probar con 2-3 jugadores el sistema de referidos
- Ajustar `poblacion_base` según tu promedio real
- Monitorear si las recompensas son balanceadas
- Verificar que el multiplicador acumulativo funcione correctamente

### Balance Futuro
Si ves que el sistema está desbalanceado:
- **Muy generoso:** Reduce XP de hitos o aumenta requisitos de referidos activos
- **Poco atractivo:** Aumenta recompensas o reduce tiempo de "activo" a 3-5 días

---

**Última actualización:** 13 de Diciembre 2025
**Versión:** 1.0
**Estado:** ✅ Configuración completa | ⚠️ Código pendiente
