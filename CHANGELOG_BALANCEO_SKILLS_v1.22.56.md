# Balanceo de Habilidades - Versión 1.22.56
**Fecha:** 2026-01-27  
**Objetivo:** Reducir la brecha entre jugadores nuevos y veteranos

---

## 🎯 Objetivo del Balanceo

Los valores anteriores de las habilidades creaban una diferencia **demasiado grande** entre jugadores que recién empiezan y jugadores con todas las skills desbloqueadas. Esto podía **espantar a jugadores nuevos** al ver que los veteranos:

- Tenían **+14 corazones extra** (32 HP en total vs 20 HP)
- Se movían **+50% más rápido** (imposible alcanzarlos)
- Hacían **+80% más daño** (mataban mobs en 1 hit)
- Robaban **12% de vida** con cada golpe (casi inmortales en combate)

**Solución:** Reducir los valores máximos para que las habilidades sigan siendo **útiles y valiosas**, pero no tan **exageradas** que intimiden a nuevos jugadores.

---

## 📊 Cambios Implementados

### 🛡️ SUPERVIVENCIA - Reducción de Vida Extra

**Problema:** Los veteranos tenían 32 HP (16 corazones) mientras los nuevos tienen 20 HP (10 corazones).

| Habilidad | Nivel | ANTES | AHORA | Cambio |
|-----------|-------|-------|-------|--------|
| **Piel Gruesa** | 1 | +2♥ | +2♥ | Sin cambio |
| **Piel Gruesa** | 2 | +3♥ | +3♥ | Sin cambio |
| **Piel Gruesa** | 3 | +4♥ | +4♥ | Sin cambio |
| **Tanque** | 1 | +4♥ | +3♥ | **-1♥** |
| **Tanque** | 2 | +6♥ | +5♥ | **-1♥** |
| **Tanque** | 3 | +8♥ | +7♥ | **-1♥** |
| **Inmortal** | 1 | +8♥ | +6♥ | **-2♥** |
| **Inmortal** | 2 | +10♥ | +8♥ | **-2♥** |
| **Inmortal** | 3 | +14♥ | +10♥ | **-4♥** ⚠️ |

**Total Máximo con Todo:**
- **ANTES:** 20 HP base + 14 corazones = **48 HP** (2.4× más vida)
- **AHORA:** 20 HP base + 10 corazones = **40 HP** (2.0× más vida)

**Impacto:**
- ✅ Los veteranos siguen teniendo ventaja significativa (+100% vida)
- ✅ Los nuevos pueden matar a veteranos con estrategia
- ✅ La diferencia es notable pero NO intimidante

---

### ⚡ UTILIDAD - Reducción de Velocidad

**Problema:** Los veteranos corrían +50% más rápido, haciendo imposible escapar o alcanzarlos.

| Habilidad | Nivel | ANTES | AHORA | Cambio |
|-----------|-------|-------|-------|--------|
| **Paso Ligero** | 1 | +10% | +10% | Sin cambio |
| **Paso Ligero** | 2 | +15% | +15% | Sin cambio |
| **Paso Ligero** | 3 | +20% | +20% | Sin cambio |
| **Zancadas** | 1 | +20% | +15% | **-5%** |
| **Zancadas** | 2 | +30% | +20% | **-10%** |
| **Zancadas** | 3 | +40% | +25% | **-15%** |
| **Velocista** | 1 | +30% | +25% | **-5%** |
| **Velocista** | 2 | +40% | +30% | **-10%** |
| **Velocista** | 3 | +50% | +35% | **-15%** ⚠️ |

**Total Máximo (Paso + Zancadas + Velocista):**
- **ANTES:** +20% + +40% + +50% = **+110% velocidad** (más del doble)
- **AHORA:** +20% + +25% + +35% = **+80% velocidad** (1.8× más rápido)

**Ejemplo Práctico:**
- **Jugador nuevo:** 100 bloques en 10 segundos
- **Veterano ANTES:** 100 bloques en **4.7 segundos** (imposible alcanzar)
- **Veterano AHORA:** 100 bloques en **5.5 segundos** (difícil pero posible)

**Impacto:**
- ✅ Los veteranos siguen siendo más rápidos
- ✅ Los nuevos pueden escapar con estrategia (terreno, ítems)
- ✅ La velocidad es útil sin ser frustrante

---

### ⚔️ COMBATE - Reducción de Daño

**Problema:** Los veteranos mataban zombies en 1 hit y jugadores en 2 hits.

| Habilidad | Nivel | ANTES | AHORA | Cambio |
|-----------|-------|-------|-------|--------|
| **Francotirador** | 1 | +20% | +15% | **-5%** |
| **Francotirador** | 2 | +35% | +25% | **-10%** |
| **Francotirador** | 3 | +50% | +35% | **-15%** |
| **Ejecutor** | 1 | +25% | +20% | **-5%** |
| **Ejecutor** | 2 | +40% | +30% | **-10%** |
| **Ejecutor** | 3 | +60% | +40% | **-20%** |
| **Berserker** | 1 | +40% | +30% | **-10%** |
| **Berserker** | 2 | +60% | +40% | **-20%** |
| **Berserker** | 3 | +80% | +50% | **-30%** ⚠️ |
| **Vampirismo** | 1 | 5% | 4% | **-1%** |
| **Vampirismo** | 2 | 8% | 6% | **-2%** |
| **Vampirismo** | 3 | 12% | 8% | **-4%** |

**Ejemplo: Daño a Zombie (20 HP)**

Con espada de diamante (7 HP de daño):

| Situación | Golpes ANTES | Golpes AHORA |
|-----------|--------------|--------------|
| Sin skills | 3 golpes | 3 golpes |
| Con Berserker 3 | **1 golpe** (12.6 HP) | **2 golpes** (10.5 HP) |
| Con todas las skills | **1 golpe** (15+ HP) | **2 golpes** (12 HP) |

**Impacto:**
- ✅ Los veteranos siguen matando más rápido
- ✅ Los nuevos pueden sobrevivir combates PvP
- ✅ El combate requiere algo de estrategia, no solo stats

---

### 🔨 UTILIDAD - Reducción de Drops

**Problema:** Los veteranos conseguían recursos demasiado rápido.

| Habilidad | Nivel | ANTES | AHORA | Cambio |
|-----------|-------|-------|-------|--------|
| **Toque de Fortuna** | 1 | 10% | 8% | **-2%** |
| **Toque de Fortuna** | 2 | 20% | 12% | **-8%** |
| **Toque de Fortuna** | 3 | 30% | 15% | **-15%** ⚠️ |
| **Seda Natural** | 1 | 5% | 3% | **-2%** |
| **Seda Natural** | 2 | 10% | 5% | **-5%** |
| **Seda Natural** | 3 | 15% | 8% | **-7%** |

**Ejemplo: Minar 100 Diamantes**

| Situación | Diamantes ANTES | Diamantes AHORA | Diferencia |
|-----------|-----------------|-----------------|------------|
| Sin skills | 100 | 100 | - |
| Con Fortuna 3 | **130** | **115** | -15 💎 |

**Impacto:**
- ✅ Los veteranos consiguen más recursos
- ✅ La economía no se infla tanto
- ✅ Los nuevos pueden competir farmando más tiempo

---

### 🤝 SINERGIAS - Reducción de Multiplicadores

**Problema:** Las habilidades combinadas multiplicaban efectos de forma exagerada.

| Habilidad | Nivel | ANTES | AHORA | Cambio |
|-----------|-------|-------|-------|--------|
| **Cazador Sigiloso** | 3 | +60% | +40% | **-20%** |
| **Guerrero Inmortal** | 3 | +75% | +50% | **-25%** |
| **Explorador Veloz** | 3 | +80% | +50% | **-30%** |
| **Maestro Elemental** | 3 | +75% | +60% | **-15%** |
| **Cazador Experto** | 3 | +40% | +30% | **-10%** |
| **Minero Guerrero** | 3 | +60% | +40% | **-20%** |
| **Explorador Ligero** | 3 | +50% | +35% | **-15%** |
| **Mercader Supremo** | 3 | 30% | 20% | **-10%** |
| **Avatar del Caos** | 3 | +75% | +50% | **-25%** |

**Impacto:**
- ✅ Las sinergias siguen siendo poderosas
- ✅ No se multiplican hasta niveles ridículos
- ✅ Se mantiene el valor de desbloquear múltiples skills

---

## 🎮 Comparación: Jugador Nuevo vs Veterano

### ANTES del Balanceo

| Stat | Jugador Nuevo | Veterano | Ventaja |
|------|---------------|----------|---------|
| **Vida** | 20 HP (10♥) | 48 HP (24♥) | **+140%** ⚠️ |
| **Velocidad** | 100% | 210% | **+110%** ⚠️ |
| **Daño** | 100% | 230% | **+130%** ⚠️ |
| **Lifesteal** | 0% | 12% | **+∞** ⚠️ |

**Resultado:** Jugador nuevo se desanima porque es imposible competir.

---

### DESPUÉS del Balanceo

| Stat | Jugador Nuevo | Veterano | Ventaja |
|------|---------------|----------|---------|
| **Vida** | 20 HP (10♥) | 40 HP (20♥) | **+100%** ✅ |
| **Velocidad** | 100% | 180% | **+80%** ✅ |
| **Daño** | 100% | 180% | **+80%** ✅ |
| **Lifesteal** | 0% | 8% | **Útil** ✅ |

**Resultado:** Jugador nuevo ve que puede alcanzar al veterano con esfuerzo.

---

## 💡 Filosofía del Balanceo

### Principios Aplicados

1. **Progresión Significativa:**
   - Las habilidades deben sentirse **valiosas**
   - Desbloquear skills debe dar **poder real**
   - El progreso debe sentirse **gratificante**

2. **No Intimidar:**
   - La diferencia debe ser **notable** pero no **abrumadora**
   - Un nuevo jugador con skill debe poder vencer a un veterano sin skill
   - La estrategia debe importar más que los stats

3. **Economía Equilibrada:**
   - Los drops no deben inflar la economía
   - Los veteranos pueden farmear más rápido pero no **demasiado**
   - Los recursos siguen teniendo valor

4. **PvP Justo:**
   - Un grupo de 2-3 nuevos puede vencer a 1 veterano
   - El combate no se decide solo por stats
   - Las emboscadas y el terreno siguen importando

---

## 📈 Curva de Progresión

### Nivel 1 (Principiante)
- **Objetivo:** Sentir mejora inmediata
- **Poder:** +10-20% en stats principales
- **Sensación:** "Esto ayuda bastante"

### Nivel 2 (Intermedio)
- **Objetivo:** Duplicar la ventaja del nivel 1
- **Poder:** +20-30% en stats principales
- **Sensación:** "Ahora soy más fuerte"

### Nivel 3 (Maestro)
- **Objetivo:** Sentirse poderoso sin ser ridículo
- **Poder:** +35-50% en stats principales (antes era +50-80%)
- **Sensación:** "Soy veterano pero no invencible"

---

## 🔍 Casos de Uso

### Escenario 1: PvP 1v1

**ANTES:**
- Veterano: 48 HP, +110% velocidad, +130% daño, 12% lifesteal
- Nuevo: 20 HP, 100% velocidad, 100% daño, 0% lifesteal
- **Resultado:** Veterano gana en 10 segundos sin esfuerzo

**AHORA:**
- Veterano: 40 HP, +80% velocidad, +80% daño, 8% lifesteal
- Nuevo: 20 HP, 100% velocidad, 100% daño, 0% lifesteal
- **Resultado:** Veterano gana pero el nuevo puede escapar/counterplay

### Escenario 2: Farmeo de Recursos

**ANTES:**
- Veterano mina 100 diamantes → consigue 130 (+30%)
- Nuevo mina 100 diamantes → consigue 100
- **Diferencia:** 30% más recursos por hora (inflación económica)

**AHORA:**
- Veterano mina 100 diamantes → consigue 115 (+15%)
- Nuevo mina 100 diamantes → consigue 100
- **Diferencia:** 15% más recursos (economía estable)

### Escenario 3: Combate PvE (Mobs)

**ANTES:**
- Veterano mata Zombie en 1 hit (excesivo)
- Nuevo mata Zombie en 3 hits (normal)
- **Sensación:** "No puedo competir"

**AHORA:**
- Veterano mata Zombie en 2 hits (rápido)
- Nuevo mata Zombie en 3 hits (normal)
- **Sensación:** "Puedo alcanzarlo si farmeo skills"

---

## ⚖️ Tabla Completa de Cambios

| Categoría | Skill | Nivel | Efecto | Antes | Ahora | Δ |
|-----------|-------|-------|--------|-------|-------|---|
| **Supervivencia** | Tanque | 1 | Corazones | +4 | +3 | -1 |
| **Supervivencia** | Tanque | 2 | Corazones | +6 | +5 | -1 |
| **Supervivencia** | Tanque | 3 | Corazones | +8 | +7 | -1 |
| **Supervivencia** | Inmortal | 1 | Corazones | +8 | +6 | -2 |
| **Supervivencia** | Inmortal | 2 | Corazones | +10 | +8 | -2 |
| **Supervivencia** | Inmortal | 3 | Corazones | +14 | +10 | -4 |
| **Utilidad** | Zancadas | 1 | Velocidad % | +20 | +15 | -5 |
| **Utilidad** | Zancadas | 2 | Velocidad % | +30 | +20 | -10 |
| **Utilidad** | Zancadas | 3 | Velocidad % | +40 | +25 | -15 |
| **Utilidad** | Velocista | 1 | Velocidad % | +30 | +25 | -5 |
| **Utilidad** | Velocista | 2 | Velocidad % | +40 | +30 | -10 |
| **Utilidad** | Velocista | 3 | Velocidad % | +50 | +35 | -15 |
| **Utilidad** | Toque Fortuna | 1 | Drop % | +10 | +8 | -2 |
| **Utilidad** | Toque Fortuna | 2 | Drop % | +20 | +12 | -8 |
| **Utilidad** | Toque Fortuna | 3 | Drop % | +30 | +15 | -15 |
| **Utilidad** | Seda Natural | 1 | Chance % | 5 | 3 | -2 |
| **Utilidad** | Seda Natural | 2 | Chance % | 10 | 5 | -5 |
| **Utilidad** | Seda Natural | 3 | Chance % | 15 | 8 | -7 |
| **Combate** | Francotirador | 1 | Daño % | +20 | +15 | -5 |
| **Combate** | Francotirador | 2 | Daño % | +35 | +25 | -10 |
| **Combate** | Francotirador | 3 | Daño % | +50 | +35 | -15 |
| **Combate** | Ejecutor | 1 | Daño % | +25 | +20 | -5 |
| **Combate** | Ejecutor | 2 | Daño % | +40 | +30 | -10 |
| **Combate** | Ejecutor | 3 | Daño % | +60 | +40 | -20 |
| **Combate** | Berserker | 1 | Daño % | +40 | +30 | -10 |
| **Combate** | Berserker | 2 | Daño % | +60 | +40 | -20 |
| **Combate** | Berserker | 3 | Daño % | +80 | +50 | -30 |
| **Combate** | Vampirismo | 1 | Lifesteal % | 5 | 4 | -1 |
| **Combate** | Vampirismo | 2 | Lifesteal % | 8 | 6 | -2 |
| **Combate** | Vampirismo | 3 | Lifesteal % | 12 | 8 | -4 |

---

## 🎯 Resultados Esperados

### Para Jugadores Nuevos
- ✅ Ven progresión alcanzable
- ✅ No se sienten abrumados por veteranos
- ✅ Pueden competir con estrategia

### Para Jugadores Veteranos
- ✅ Siguen teniendo ventaja clara
- ✅ Sus skills siguen siendo valiosas
- ✅ No pierden poder de forma drástica

### Para el Servidor
- ✅ Retención de nuevos jugadores mejorada
- ✅ Economía más estable
- ✅ PvP más equilibrado y divertido

---

## 📝 Notas de Implementación

**Archivos Modificados:**
- `SkillConfig.java` - Valores de efectos por nivel
- `ApocalipsisCommand.java` - Fix de variable duplicada (bug)

**Compatibilidad:**
- ✅ Los jugadores mantienen sus skills desbloqueadas
- ✅ Los efectos se recalculan automáticamente al entrar
- ✅ No se requiere reset de datos

**Testing Recomendado:**
1. Verificar que los nuevos valores se aplican correctamente
2. Testear PvP entre jugador nuevo vs veterano
3. Medir tiempo de farmeo de recursos
4. Verificar que los corazones extra se muestran bien
5. Comprobar que la velocidad no cause bugs de movimiento

---

## 🔄 Futuras Iteraciones

Si después de testing se detecta que:

**Los veteranos se quejan:**
- Subir ligeramente nivel 3 de skills principales (+5%)
- Mantener nivel 1 y 2 como están

**Los nuevos siguen intimidados:**
- Reducir aún más nivel 3 (-5%)
- Aumentar XP necesaria para nivel 3
- Añadir items que nerfeen temporalmente skills

**La economía se infla:**
- Reducir más Toque Fortuna y Seda Natural
- Añadir cooldowns a skills de farmeo

---

## ✅ Conclusión

Este balanceo reduce la brecha entre nuevos y veteranos **sin quitar el valor** de las habilidades. Los veteranos siguen siendo **más poderosos**, pero ahora los nuevos no se **espantan** al ver la diferencia.

**Resultado:** Servidor más equilibrado y divertido para todos.

---

**¿Dudas o sugerencias?** Contacta a los admins para ajustes adicionales.
