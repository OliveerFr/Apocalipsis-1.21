# 🌳 Sistema de Árbol de Habilidades - Propuesta de Implementación

## 📋 Concepto General

Un sistema de **habilidades desbloqueables** similar al menú de logros de Minecraft, donde los jugadores gastan **XP de rangos** para desbloquear mejoras permanentes. Si gastan demasiada XP, **bajan de rango** y deben volver a subir.

### Diferencia con Habilidades de Rango
| Habilidades de Rango | Árbol de Habilidades |
|---------------------|---------------------|
| Se obtienen automáticamente al subir | Se compran con XP |
| Son temporales (se pierden al bajar) | Son **permanentes** una vez compradas |
| Enfocadas en supervivencia de desastres | Enfocadas en **calidad de vida** |
| No tienen costo | Cuestan XP (pueden bajar de rango) |

---

## 🎯 Estructura del Árbol

```
                    [RAÍZ: Despertar]
                          │
          ┌───────────────┼───────────────┐
          │               │               │
    [ALMACENAMIENTO]  [UTILIDAD]    [SUPERVIVENCIA]
          │               │               │
     ┌────┴────┐     ┌────┴────┐     ┌────┴────┐
     │         │     │         │     │         │
  [Tier 1] [Tier 2] [Tier 1] [Tier 2] [Tier 1] [Tier 2]
     │         │     │         │     │         │
  [Tier 3] [Tier 3] [Tier 3] [Tier 3] [Tier 3] [Tier 3]
```

---

## 📦 RAMA: ALMACENAMIENTO

### Tier 1 - Básico
| Habilidad | Efecto | Costo XP | Requisito |
|-----------|--------|----------|-----------|
| **Bolsillos Profundos** | +9 slots de inventario (4ª fila) | 500 XP | Ninguno |
| **Cofre Interior** | Ender Chest +9 slots | 400 XP | Ninguno |
| **Mochila Espiritual** | /mochila - 9 slots extra portátiles | 600 XP | Ninguno |

### Tier 2 - Intermedio
| Habilidad | Efecto | Costo XP | Requisito |
|-----------|--------|----------|-----------|
| **Bolsillos Sin Fondo** | +18 slots de inventario (5ª fila) | 1,500 XP | Bolsillos Profundos |
| **Cofre Dimensional** | Ender Chest +18 slots (total 45) | 1,200 XP | Cofre Interior |
| **Mochila Expandida** | /mochila ahora tiene 27 slots | 1,800 XP | Mochila Espiritual |
| **Auto-Recolección** | Items van directo al inventario (radio 3 bloques) | 2,000 XP | Bolsillos Profundos |

### Tier 3 - Avanzado
| Habilidad | Efecto | Costo XP | Requisito |
|-----------|--------|----------|-----------|
| **Inventario Infinito** | +27 slots (6ª fila, máximo) | 4,000 XP | Bolsillos Sin Fondo |
| **Void Storage** | Ender Chest de 54 slots (cofre doble) | 3,500 XP | Cofre Dimensional |
| **Mochila Legendaria** | /mochila con 54 slots + no se pierde al morir | 5,000 XP | Mochila Expandida |

---

## ⚡ RAMA: UTILIDAD

### Tier 1 - Básico
| Habilidad | Efecto | Costo XP | Requisito |
|-----------|--------|----------|-----------|
| **Paso Ligero** | +10% velocidad de movimiento permanente | 400 XP | Ninguno |
| **Minero Eficiente** | +15% velocidad de minado | 450 XP | Ninguno |
| **Estómago de Hierro** | Hambre baja 20% más lento | 350 XP | Ninguno |
| **Visión Nocturna** | /nv - Toggle visión nocturna (30s cooldown) | 500 XP | Ninguno |

### Tier 2 - Intermedio
| Habilidad | Efecto | Costo XP | Requisito |
|-----------|--------|----------|-----------|
| **Zancadas** | +20% velocidad + salto mejorado | 1,400 XP | Paso Ligero |
| **Toque de Fortuna** | +10% drop de minerales | 1,600 XP | Minero Eficiente |
| **Metabolismo Lento** | Hambre baja 40% más lento | 1,000 XP | Estómago de Hierro |
| **Brújula Interna** | Ver coordenadas siempre en action bar | 800 XP | Ninguno |
| **Crafteo Rápido** | Shift+click craftea stacks completos | 1,200 XP | Ninguno |

### Tier 3 - Avanzado
| Habilidad | Efecto | Costo XP | Requisito |
|-----------|--------|----------|-----------|
| **Velocista** | +30% velocidad + sin penalización por sneaking | 3,500 XP | Zancadas |
| **Toque de Seda Natural** | 5% chance de silk touch sin encantamiento | 4,500 XP | Toque de Fortuna |
| **Autosuficiente** | Regenera 0.5 hambre cada 30s | 3,000 XP | Metabolismo Lento |
| **Mesa Portátil** | /craft - Abre mesa de crafteo en cualquier lugar | 2,500 XP | Crafteo Rápido |

---

## 🛡️ RAMA: SUPERVIVENCIA

### Tier 1 - Básico
| Habilidad | Efecto | Costo XP | Requisito |
|-----------|--------|----------|-----------|
| **Piel Gruesa** | +2 corazones permanentes | 600 XP | Ninguno |
| **Caída Suave** | -25% daño por caída | 400 XP | Ninguno |
| **Resistencia al Fuego** | -20% daño por fuego/lava | 500 XP | Ninguno |
| **Nadador** | +30% velocidad nadando | 350 XP | Ninguno |

### Tier 2 - Intermedio
| Habilidad | Efecto | Costo XP | Requisito |
|-----------|--------|----------|-----------|
| **Tanque** | +4 corazones permanentes (total +6) | 2,000 XP | Piel Gruesa |
| **Pluma** | -50% daño por caída | 1,200 XP | Caída Suave |
| **Ignífugo** | -40% daño fuego + inmune a daño por pisar fuego | 1,500 XP | Resistencia al Fuego |
| **Branquias** | +60% respiración bajo agua | 1,000 XP | Nadador |
| **Regeneración Pasiva** | Regenera 0.5 corazones cada 20s | 1,800 XP | Piel Gruesa |

### Tier 3 - Avanzado
| Habilidad | Efecto | Costo XP | Requisito |
|-----------|--------|----------|-----------|
| **Inmortal** | +8 corazones permanentes (total +14) | 5,000 XP | Tanque |
| **Vuelo de Emergencia** | Elytra glide al caer (1 min cooldown) | 4,000 XP | Pluma |
| **Fénix** | Revivir con 3 corazones 1 vez por día | 6,000 XP | Regeneración Pasiva |
| **Anfibio** | Respiración infinita bajo agua | 3,000 XP | Branquias |

---

## 🎮 RAMA: SOCIAL/ECONOMÍA (Opcional)

### Tier 1-3
| Habilidad | Efecto | Costo XP |
|-----------|--------|----------|
| **Comerciante** | -5% precios con villagers | 500 XP |
| **Negociador** | -15% precios con villagers | 1,500 XP |
| **Magnate** | -25% precios + trades extra | 4,000 XP |
| **Teletransporte** | /tpa una vez cada 10 min | 800 XP |
| **Home Extra** | +1 home adicional | 600 XP |
| **Homes Múltiples** | +3 homes adicionales | 2,000 XP |

---

## 💰 Sistema de Costos Dinámicos

### Fórmula de Costo
```java
costoFinal = costoBase * multiplicadorTier * multiplicadorRareza

Donde:
- multiplicadorTier = 1.0 (T1), 2.5 (T2), 5.0 (T3)
- multiplicadorRareza = 1.0 (común), 1.5 (raro), 2.5 (épico), 4.0 (legendario)
```

### Ejemplo de Costos
| Habilidad | Base | Tier | Rareza | **Costo Final** |
|-----------|------|------|--------|-----------------|
| Bolsillos Profundos | 200 | T1 (1.0) | Común (1.0) | **200 XP** |
| Auto-Recolección | 400 | T2 (2.5) | Raro (1.5) | **1,500 XP** |
| Inventario Infinito | 500 | T3 (5.0) | Épico (2.5) | **6,250 XP** |
| Fénix | 600 | T3 (5.0) | Legendario (4.0) | **12,000 XP** |

---

## 📉 Sistema de Rango Dinámico

### Mecánica Principal
```
XP del jugador: 5,000 (Rango: VETERANO)
Compra "Tanque" por 2,000 XP
Nueva XP: 3,000 → ¡BAJA A SOBREVIVIENTE!
```

### Protecciones
- **Confirmación**: Siempre pedir confirmación antes de comprar
- **Advertencia de bajada**: "⚠ Esta compra te bajará de rango"
- **Límite mínimo**: No puede gastar si quedaría en NOVATO (< 100 XP)

### Mensaje de Ejemplo
```
╔══════════════════════════════════════════════╗
║  ⚠ ADVERTENCIA DE COMPRA                    ║
╠══════════════════════════════════════════════╣
║  Habilidad: Tanque (+4 corazones)            ║
║  Costo: 2,000 XP                             ║
║                                              ║
║  Tu XP actual: 5,000 (VETERANO)              ║
║  XP después: 3,000 (SOBREVIVIENTE) ⬇         ║
║                                              ║
║  [CONFIRMAR]        [CANCELAR]               ║
╚══════════════════════════════════════════════╝
```

---

## 🖥️ Interfaz de Usuario (GUI)

### Menú Principal
```
╔═══════════════════════════════════════════════════════════╗
║           🌳 ÁRBOL DE HABILIDADES                         ║
║                                                           ║
║   Tu XP: 5,000    Rango: VETERANO    Habilidades: 7/45   ║
╠═══════════════════════════════════════════════════════════╣
║                                                           ║
║   [📦]              [⚡]              [🛡️]                ║
║   ALMACENAMIENTO    UTILIDAD         SUPERVIVENCIA       ║
║   3/12 desbloq.     2/13 desbloq.    2/12 desbloq.       ║
║                                                           ║
║   ─────────────────────────────────────────────────────  ║
║                                                           ║
║   [🔙 Volver]    [📊 Mis Habilidades]    [❓ Ayuda]       ║
║                                                           ║
╚═══════════════════════════════════════════════════════════╝
```

### Vista de Rama (ejemplo: Almacenamiento)
```
╔═══════════════════════════════════════════════════════════╗
║           📦 ALMACENAMIENTO                               ║
╠═══════════════════════════════════════════════════════════╣
║                                                           ║
║   TIER 1:  [✅]──────[✅]──────[🔒]                       ║
║            Bolsillos  Cofre    Mochila                    ║
║            Profundos  Interior Espiritual                 ║
║               │          │                                ║
║   TIER 2:  [🔓]──────[🔒]──────[🔒]──────[🔒]            ║
║            Bolsillos  Cofre    Mochila   Auto-            ║
║            Sin Fondo  Dimens.  Expandida Recolección      ║
║               │          │         │                      ║
║   TIER 3:  [🔒]      [🔒]      [🔒]                       ║
║            Inventario Void     Mochila                    ║
║            Infinito   Storage  Legendaria                 ║
║                                                           ║
║   ✅ = Desbloqueado   🔓 = Disponible   🔒 = Bloqueado   ║
╚═══════════════════════════════════════════════════════════╝
```

---

## 📁 Estructura de Archivos

### habilidades.yml
```yaml
version: 1

# Configuración global
config:
  enabled: true
  min_xp_restante: 100          # No puede quedar con menos de esto
  confirmar_compras: true
  advertir_bajada_rango: true
  permitir_reembolso: false     # ¿Pueden devolver habilidades?
  reembolso_porcentaje: 50      # Si sí, devuelve 50% XP

# Multiplicadores
multiplicadores:
  tier_1: 1.0
  tier_2: 2.5
  tier_3: 5.0
  rareza_comun: 1.0
  rareza_raro: 1.5
  rareza_epico: 2.5
  rareza_legendario: 4.0

# Definición de habilidades
habilidades:
  bolsillos_profundos:
    nombre: "Bolsillos Profundos"
    descripcion: "+9 slots de inventario"
    rama: "almacenamiento"
    tier: 1
    rareza: "comun"
    costo_base: 200
    requisitos: []
    efecto:
      tipo: "inventario_extra"
      valor: 9
    icono: "CHEST"
    
  tanque:
    nombre: "Tanque"
    descripcion: "+4 corazones permanentes"
    rama: "supervivencia"
    tier: 2
    rareza: "raro"
    costo_base: 400
    requisitos: ["piel_gruesa"]
    efecto:
      tipo: "vida_extra"
      valor: 4
    icono: "GOLDEN_APPLE"
```

### player_habilidades.yml
```yaml
players:
  uuid-del-jugador:
    habilidades_desbloqueadas:
      - "bolsillos_profundos"
      - "cofre_interior"
      - "paso_ligero"
    xp_gastada_total: 1350
    ultima_compra: 1701100800000
```

---

## 🔧 Implementación Técnica

### Clases Principales
```
src/main/java/me/apocalipsis/
├── skills/
│   ├── SkillTree.java              # Árbol principal
│   ├── SkillBranch.java            # Rama (Almacenamiento, Utilidad, etc)
│   ├── Skill.java                  # Habilidad individual
│   ├── SkillTier.java              # Enum de tiers
│   ├── SkillRarity.java            # Enum de rarezas
│   ├── SkillEffect.java            # Interfaz para efectos
│   ├── SkillService.java           # Lógica de negocio
│   ├── SkillDataManager.java       # Persistencia
│   └── effects/
│       ├── ExtraInventoryEffect.java
│       ├── ExtraHealthEffect.java
│       ├── SpeedBoostEffect.java
│       ├── FallDamageReductionEffect.java
│       └── ...
├── ui/
│   ├── SkillTreeGUI.java           # Menú principal
│   ├── SkillBranchGUI.java         # Vista de rama
│   ├── SkillPurchaseGUI.java       # Confirmación de compra
│   └── SkillInfoGUI.java           # Detalle de habilidad
└── listeners/
    └── SkillEffectListener.java    # Aplica efectos activos
```

### Comandos
```
/habilidades                    - Abre el árbol de habilidades
/habilidades info <id>          - Info de una habilidad
/habilidades mis                - Lista tus habilidades
/habilidades admin give <player> <skill>   - Admin: dar habilidad
/habilidades admin remove <player> <skill> - Admin: quitar habilidad
/habilidades admin reset <player>          - Admin: resetear todo
```

---

## 🎨 Ideas de Habilidades Únicas

### Temáticas del Apocalipsis
| Habilidad | Efecto | Costo |
|-----------|--------|-------|
| **Presagio** | Notificación 30s antes del desastre | 3,000 XP |
| **Ojo del Huracán** | -30% daño durante huracanes | 2,000 XP |
| **Piel de Piedra** | -30% daño durante terremotos | 2,000 XP |
| **Sangre Fría** | -30% daño durante lluvia de fuego | 2,000 XP |
| **Bendición Divina** | +50% PS de misiones | 4,000 XP |
| **Evasor Maestro** | -1 nivel de evasión máximo | 5,000 XP |

### Habilidades Sociales
| Habilidad | Efecto | Costo |
|-----------|--------|-------|
| **Aura Protectora** | Aliados en 5 bloques reciben +10% defensa | 3,500 XP |
| **Compartir Fuerza** | Puedes transferir hasta 500 XP a otro jugador/día | 2,000 XP |
| **Mentor** | +10% XP a jugadores nuevos que ayudes | 1,500 XP |

---

## ⚠️ Consideraciones de Balance

### Límites Sugeridos
- **Máximo slots extra inventario**: 27 (total 63)
- **Máximo corazones extra**: 14 (total 24 corazones)
- **Máximo velocidad extra**: 30%
- **Habilidades únicas activas**: Sin límite (son permanentes)

### Anti-Abuse
- No se pueden desbloquear habilidades durante desastres
- Cooldown de 1 hora entre compras de Tier 3
- Las habilidades de reducción de daño no se acumulan multiplicativamente

---

## 📅 Fases de Implementación

### Fase 1: Core (2-3 días)
- [ ] Sistema base de habilidades
- [ ] Persistencia de datos
- [ ] GUI básica
- [ ] Integración con sistema de XP

### Fase 2: Habilidades Básicas (3-4 días)
- [ ] Rama Almacenamiento (inventario, ender chest, mochila)
- [ ] Rama Supervivencia (vida, resistencias)
- [ ] Efectos básicos funcionando

### Fase 3: Habilidades Avanzadas (3-4 días)
- [ ] Rama Utilidad (velocidad, minado, crafteo)
- [ ] Habilidades temáticas del Apocalipsis
- [ ] Sistema de confirmación y advertencias

### Fase 4: Polish (2-3 días)
- [ ] GUI mejorada con animaciones
- [ ] Sonidos y partículas
- [ ] Comandos admin
- [ ] Testing y balance

---

## 💡 Resumen

El **Árbol de Habilidades** ofrece:
1. ✅ Progresión permanente que no compite con rangos
2. ✅ Decisiones significativas (gastar XP = arriesgar rango)
3. ✅ Variedad de mejoras (almacenamiento, utilidad, supervivencia)
4. ✅ Sistema visual similar a logros de Minecraft
5. ✅ Balance mediante costos dinámicos
6. ✅ Integración con la temática del Apocalipsis

**¿Listo para implementar?** 🚀
