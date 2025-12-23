# 🎁 Sistema de Amigo Secreto - ACTUALIZADO

## ✨ Cambios Principales

### 🎯 Nueva Mecánica: Regalos del Inventario

**ANTES:** Los jugadores recibían items predefinidos aleatorios  
**AHORA:** Los jugadores **dan items de su propio inventario** - ¡lo que ellos quieran!

---

## 🌟 Características Nuevas

### 1. **Sistema de Valoración de Items**

Los jugadores pueden dar **CUALQUIER item** de su inventario. El sistema calcula automáticamente el valor:

#### Tabla de Valores (ejemplos):

| Material | Valor Unitario | Categoría |
|----------|----------------|-----------|
| Netherite Ingot | 100 pts | ⭐⭐⭐ LEGENDARIO |
| Diamond | 50 pts | ⭐⭐ ÉPICO |
| Emerald | 40 pts | ⭐⭐ ÉPICO |
| Gold Ingot | 25 pts | ⭐ RARO |
| Iron Ingot | 10 pts | ✦ POCO COMÚN |
| Netherite Block | 900 pts | ⭐⭐⭐ LEGENDARIO |
| Diamond Block | 450 pts | ⭐⭐⭐ LEGENDARIO |
| Beacon | 200 pts | ⭐⭐⭐ LEGENDARIO |
| Golden Apple | 30 pts | ⭐ RARO |
| Enchanted Golden Apple | 100 pts | ⭐⭐⭐ LEGENDARIO |
| Experience Bottle | 15 pts | ✦ POCO COMÚN |

**Bonus:** Items encantados reciben +20% por cada nivel de encantamiento

### 2. **Recompensas Escalables**

Las recompensas **SE MULTIPLICAN** según el valor total de tus regalos:

```
VALORES BASE:
- XP: 150
- Fragmentos: 2

MULTIPLICADORES:
┌──────────────┬──────────┬─────────┬─────────────┐
│ Valor Total  │ Multi    │ XP      │ Fragmentos  │
├──────────────┼──────────┼─────────┼─────────────┤
│ >= 200       │ x3.0     │ 450 XP  │ 6 frags     │ ⭐⭐⭐ BENEFACTOR LEGENDARIO
│ >= 100       │ x2.5     │ 375 XP  │ 5 frags     │ ⭐⭐ GENEROSO ÉPICO
│ >= 50        │ x2.0     │ 300 XP  │ 4 frags     │ ⭐ AMIGO GENEROSO
│ >= 20        │ x1.5     │ 225 XP  │ 3 frags     │ ✦ BUEN COMPAÑERO
│ >= 10        │ x1.2     │ 180 XP  │ 2 frags     │ ✦ PARTICIPANTE
│ < 10         │ x1.0     │ 150 XP  │ 2 frags     │ · MODESTO
└──────────────┴──────────┴─────────┴─────────────┘
```

**Ejemplo:** Si das 2 diamantes (50 pts cada uno) = 100 pts total → **x2.5 multiplicador** = **375 XP + 5 Fragmentos**

### 3. **Instrucciones Claras al Inicio**

Cuando se inicia el sorteo, cada jugador recibe:

```
╔════════════════════════════════════════╗
║    TU AMIGO SECRETO ES: JugadorX       ║
╚════════════════════════════════════════╝

▸ INSTRUCCIONES:
1. Acércate a JugadorX
2. Pon el item en tu mano principal
3. Usa: /avo navidad entregar

✦ ¡Entre más valioso tu regalo, mejores recompensas!
```

### 4. **Feedback Detallado**

Al entregar un regalo, el jugador ve:

```
✦ Regalo Entregado
Has entregado: ⭐ Diamond
Valor: ⭐⭐ ÉPICO
Progreso: 1/2
```

Al completar todas las entregas:

```
═════════════════════════════════════
      INTERCAMBIO COMPLETADO
═════════════════════════════════════

Calidad de regalos: ⭐⭐ GENEROSO ÉPICO
Multiplicador: 2.5x

Recompensas:
  • +375 XP (base: 150)
  • +5 Fragmentos (base: 2)

✦ Gracias por participar ✦
```

---

## 🎮 Cómo Funciona

### Para Administradores:

```bash
/avo navidad start           # Iniciar evento
/avo navidad amigo-secreto   # Iniciar sorteo (asignación aleatoria)
```

### Para Jugadores:

1. **Recibir asignación**: Mensaje privado con tu amigo secreto
2. **Preparar regalo**: Poner item en mano principal (puede ser cualquier cosa)
3. **Acercarse**: Estar a menos de 10 bloques del receptor
4. **Entregar**: `/avo navidad entregar`
5. **Repetir**: Hacer 2 entregas (o la cantidad configurada)
6. **Recibir recompensas**: Al completar, recompensas según valor total

---

## 📊 Categorías de Regalos

Al entregar, el sistema muestra la categoría del regalo:

- **⭐⭐⭐ LEGENDARIO** (valor >= 100): Netherite, bloques preciosos, Enchanted Golden Apple
- **⭐⭐ ÉPICO** (valor >= 50): Diamantes, herramientas de netherite
- **⭐ RARO** (valor >= 20): Oro, Emeraldas, Golden Apples
- **✦ POCO COMÚN** (valor >= 5): Hierro, herramientas básicas
- **✦ COMÚN** (valor < 5): Comida, materiales básicos

---

## ⚙️ Configuración (navidad.yml)

```yaml
amigo_secreto:
  enabled: true
  jugadores_minimos: 2
  regalos_requeridos: 2
  
  recompensas:
    base:
      xp: 150                 # XP base (se multiplica)
      fragmentos: 2           # Fragmentos base (se multiplica)
  
  # Multiplicadores automáticos según valor total
  # No requiere configuración adicional
```

---

## 🔧 Archivos Modificados

### 1. NavidadEvent.java
**Añadido:**
- Campo `valorTotalRegalos` para tracking de valor
- Método `calcularValorItem()` - Sistema de valoración completo
- Método `obtenerCategoriaRegalo()` - Clasificación de regalos
- Método `calcularMultiplicadorRecompensa()` - Cálculo de multiplicador
- Método `obtenerTierRecompensa()` - Obtiene tier de jugador
- Instrucciones claras en `iniciarAmigoSecreto()`
- Sistema de entrega desde inventario en `entregarRegaloAmigoSecreto()`

**Modificado:**
- `entregarRegaloAmigoSecreto()` ahora usa item de la mano del jugador
- Mensajes más claros y detallados
- Efectos especiales para tiers altos (>=2.0x)

### 2. navidad.yml
**Actualizado:**
- Mensaje de asignación con instrucciones claras
- Configuración de recompensas base (no escalables)
- Documentación de multiplicadores
- Tabla de valores de ejemplo
- **Removido:** Sección `items_regalo` (ya no se usa)

### 3. ApocalipsisCommand.java
✅ Comandos ya implementados:
- `/avo navidad amigo-secreto` - Inicia sorteo (admin)
- `/avo navidad entregar` - Entrega regalo (jugadores)

### 4. AvoTabCompleter.java
✅ Tab completion completo para todos los subcomandos

---

## 📈 Ejemplos de Uso

### Ejemplo 1: Regalo Modesto
```
Jugador da: 2 pasteles
Valor: 2 pts cada uno = 4 pts total
Multiplicador: x1.0 (MODESTO)
Recompensas: 150 XP + 2 Fragmentos
```

### Ejemplo 2: Buen Amigo
```
Jugador da: 1 pico de hierro encantado (Efficiency III)
Valor: 8 pts + bonus encantamientos (40%) = 11.2 pts
Multiplicador: x1.2 (PARTICIPANTE)
Recompensas: 180 XP + 2 Fragmentos
```

### Ejemplo 3: Generoso
```
Jugador da: 5 lingotes de oro
Valor: 25 pts cada uno = 125 pts total
Multiplicador: x2.5 (GENEROSO ÉPICO)
Recompensas: 375 XP + 5 Fragmentos
+ Efectos especiales!
```

### Ejemplo 4: Legendario
```
Jugador da: 1 bloque de diamante + 2 diamantes
Valor: 450 + 100 = 550 pts total
Multiplicador: x3.0 (BENEFACTOR LEGENDARIO)
Recompensas: 450 XP + 6 Fragmentos
+ Efectos épicos!
```

---

## 💡 Consejos para Jugadores

1. **Calidad > Cantidad**: 1 diamante (50 pts) vale más que 50 panes (100 pts)
2. **Encanta tus regalos**: Items encantados valen más
3. **Bloques comprimidos**: 1 bloque de diamante = 9 diamantes comprimidos = 450 pts
4. **Planifica**: Puedes dar 2 regalos baratos (x1.0) o juntar valor para uno caro (x2.5+)
5. **Interacción social**: Los regalos se dan en persona, ¡es parte de la experiencia!

---

## 🎯 Filosofía del Diseño

### ✨ Por qué este cambio?

**ANTES:**
- Regalos predefinidos y aleatorios
- Sin incentivo para dar items valiosos
- Todos recibían las mismas recompensas
- Poca interacción real

**AHORA:**
- **Libertad de elección**: Los jugadores deciden qué dar
- **Incentivo real**: Mejores regalos = mejores recompensas
- **Recompensas justas**: Quien da más, recibe más
- **Interacción genuina**: Cercanía física + entrega manual
- **Estrategia**: Planificar qué dar para maximizar recompensas

### 🎁 Transmite Calidez Familiar

- **Generosidad recompensada**: Sistema incentiva dar, no acumular
- **Elección personal**: Cada regalo es único y significativo
- **Momento compartido**: Deben estar cerca para intercambiar
- **Reconocimiento**: El sistema celebra la generosidad
- **Comunidad**: Cuando todos completan → celebración épica

---

## ✅ Estado: COMPLETADO

✓ Sistema de valoración de items  
✓ Recompensas escalables (x1.0 a x3.0)  
✓ Entrega desde inventario  
✓ Instrucciones claras al inicio  
✓ Feedback detallado al entregar  
✓ Categorización de regalos  
✓ Efectos especiales para tiers altos  
✓ Tab completion completo  
✓ Configuración simplificada  
✓ Documentación completa  

**El sistema está listo para usar en el servidor** 🎄✨

---

## 🔮 Posibles Mejoras Futuras

- [ ] Leaderboard de jugadores más generosos
- [ ] Achievements por dar regalos legendarios
- [ ] Efectos visuales diferentes por categoría de regalo
- [ ] Historial de regalos dados/recibidos
- [ ] Sistema de "regalos sorpresa" con bonus aleatorios
