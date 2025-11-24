# 🗿 El Susurro en la Piedra Rota - Documentación Completa

## 📋 Información General

**Nombre del Evento:** El Susurro en la Piedra Rota  
**Tipo:** Mini-evento narrativo  
**Duración:** 20 minutos (5 min por acto)  
**Jugadores:** 1-6 (flexible, diseñado para experiencia íntima)  
**Dificultad:** Baja-Media (narrativo con combate ligero)

## 🎭 Contexto Narrativo

Después del **Eco de las Sombras Largas**, algo "de afuera" reaccionó. La figura desconocida cambió la memoria del mundo. Ciertas estructuras antiguas están comenzando a "despertar".

Este evento introduce el concepto de **"forma"** (complemento de "sombra"), un nuevo elemento narrativo que será clave en futuros eventos relacionados con el End.

## 🎬 Estructura de 4 Actos

### Acto 1: LA PIEDRA ROTA DESPIERTA (5 min)
- 3-5 fragmentos de piedra aparecen aleatoriamente (50-150 bloques del spawn)
- Al acercarse (5 bloques), aparecen mensajes fragmentados inquietantes
- Cada fragmento está sobre un mini-altar de deepslate con vela
- Efectos visuales: partículas de humo y portal
- **Objetivo:** Inspeccionar todos los fragmentos

**Mecánicas:**
- Spawn aleatorio con distancia mínima de 30 bloques entre fragmentos
- Construcción automática de altares (3x3 deepslate + piedra + vela)
- Detección de proximidad cada 0.5s
- Mensajes fragmentados únicos por jugador

### Acto 2: LA PIEDRA SE QUIEBRA (5 min)
- Aparece una **Grieta de Forma** (agujero vertical con efectos)
- 3 oleadas de **Criaturas de Forma** (silverfish modificados)
- Criaturas débiles pero rápidas con efecto Glowing
- **Objetivo:** Eliminar todas las criaturas

**Mecánicas:**
- Grieta genera estructura de 3 bloques de radio x 8 de altura
- Partículas violetas en espiral ascendente
- Spawn 3-5 criaturas por oleada cada 20 segundos
- Tracking de participación en combate

### Acto 3: EL NÚCLEO DE FORMA (5 min)
- Al morir la última criatura, aparece el **Fragmento de Forma Desviada**
- Item único en item frame invisible con beam de luz
- **Objetivo:** Recoger el núcleo

**Mecánicas:**
- Item frame fijo e invisible en el centro de la grieta
- Beam de luz de 50 bloques de altura
- Partículas orbitando el núcleo
- Item persistente (ECHO_SHARD con encantamiento VANISHING_CURSE)

### Acto 4: EL SEGUNDO SUSURRO (5 min)
- La piedra "susurra" de nuevo con mensajes más claros
- Aparece un pensamiento del **Observador**
- **Cliffhanger:** Símbolo misterioso parpadea en el cielo
- **Final:** Evento completado, teaser para futuros eventos

**Mecánicas:**
- Secuencia automática de mensajes temporizada
- Símbolo formado por partículas (círculo + cruz)
- Transición fluida al final del evento

## 📦 Items del Evento

### Fragmento de Forma Desviada
- **Material:** ECHO_SHARD
- **Nombre:** §5§l✦ Fragmento de Forma Desviada
- **Lore:**
  ```
  §7Un núcleo cristalizado de energía
  §7de forma desviada. Su estructura
  §7es imposible y contradictoria.
  
  §8Quizás la forma también recuerda...
  
  §5§o"La piedra no debería hablar."
  ```
- **Encantamientos:** VANISHING_CURSE (nivel 1)
- **Flags:** Ocultar encantamientos, atributos, destrucción
- **Persistencia:** Item único, se mantiene para futuros eventos

## 🎯 Sistema de Recompensas

### Puntos de Supervivencia (PS)

| Acción | PS |
|--------|-----|
| Participación base | 30 |
| Por fragmento inspeccionado | 10 |
| Por criatura eliminada | 5 |
| Por recoger el núcleo | 50 |
| **Total máximo posible** | **~180 PS** |

### Ejemplo de Cálculo
- Jugador que inspecciona 5 fragmentos: 30 + (5×10) = 80 PS
- Jugador que elimina 10 criaturas: 80 + (10×5) = 130 PS
- Jugador que recoge núcleo: 130 + 50 = **180 PS**

## 🎮 Comandos de Administración

### Comandos Principales
```
/avo evento3 start          - Inicia el evento
/avo evento3 stop           - Detiene el evento
/avo evento3 acto <1-4>     - Fuerza acto específico
/avo evento3 next           - Avanza al siguiente acto
/avo evento3 info           - Muestra información detallada
/avo evento3 fragmento spawn - Fuerza spawn de fragmento adicional
/avo evento3 grieta spawn   - Fuerza spawn de la grieta
```

### Alias
También se puede usar `/avo susurro` en lugar de `/avo evento3`

### Permisos
- **Requerido:** `avo.admin`

## 🔧 Configuración (eventos.yml)

```yaml
susurro_piedra_rota:
  enabled: true
  jugadores_minimos: 1
  jugadores_maximos: 6
  
  actos:
    acto1_piedra_despierta:
      duracion_ticks: 6000  # 5 minutos
      fragmentos_min: 3
      fragmentos_max: 5
      radio_spawn_min: 50
      radio_spawn_max: 150
      distancia_minima_entre_fragmentos: 30
      
    acto2_piedra_quiebra:
      duracion_ticks: 6000  # 5 minutos
      oleadas: 3
      criaturas_por_oleada_min: 3
      criaturas_por_oleada_max: 5
      delay_entre_oleadas: 400  # 20 segundos
      
    acto3_nucleo:
      duracion_ticks: 6000  # 5 minutos
      
    acto4_segundo_susurro:
      duracion_ticks: 6000  # 5 minutos
  
  recompensas_ps:
    base_participacion: 30
    por_fragmento_inspeccionado: 10
    por_criatura_eliminada: 5
    por_recoger_nucleo: 50
```

## 📁 Archivos Creados

### Clases Principales
1. **SusurroPiedraRotaEvent.java** (~1200 líneas)
   - Clase principal del evento
   - Lógica de los 4 actos completos
   - Sistema de fragmentos, grieta, criaturas y núcleo
   - Métodos públicos para comandos

2. **SusurroPiedraRotaItems.java** (~86 líneas)
   - Creación del item "Fragmento de Forma Desviada"
   - Configuración de lore y encantamientos

3. **SusurroPiedraRotaListener.java** (~120 líneas)
   - Listener para muerte de criaturas
   - Tracking de participación en combate

### Integraciones
- **Apocalipsis.java:** Registro del evento y listener
- **EventController:** Evento añadido al sistema de eventos
- **ApocalipsisCommand.java:** Comandos `/avo evento3`
- **eventos.yml:** Configuración completa (249 líneas)

## 🧪 Testing

### Pruebas Recomendadas

#### Test 1: Spawn de Fragmentos
```
/avo evento3 start
/avo evento3 acto 1
```
- Verificar que spawnen 3-5 fragmentos
- Comprobar distancias entre fragmentos
- Probar detección de proximidad

#### Test 2: Sistema de Grieta
```
/avo evento3 acto 2
```
- Verificar creación de estructura de grieta
- Comprobar efectos de partículas
- Validar spawn de oleadas

#### Test 3: Criaturas de Forma
- Verificar estadísticas de criaturas (10 HP, velocidad aumentada)
- Comprobar tracking de muerte
- Validar conteo de oleadas

#### Test 4: Núcleo
```
/avo evento3 acto 3
```
- Verificar spawn del item frame
- Comprobar efectos visuales (beam + partículas)
- Probar recogida del núcleo

#### Test 5: Cliffhanger
```
/avo evento3 acto 4
```
- Verificar secuencia de mensajes
- Comprobar símbolo en el cielo
- Validar transición al final

#### Test 6: Recompensas
- Ejecutar evento completo
- Verificar cálculo de PS por jugador
- Comprobar entrega del item único

### Comandos de Debug
```
/avo evento3 info           - Ver estado actual completo
/avo evento3 fragmento spawn - Añadir fragmento adicional
/avo evento3 grieta spawn   - Forzar grieta inmediata
```

## 🎨 Efectos Visuales y Sonoros

### Partículas
- **SMOKE_NORMAL:** Fragmentos inactivos
- **PORTAL:** Fragmentos descubiertos
- **REVERSE_PORTAL:** Grieta de Forma (espiral)
- **END_ROD:** Núcleo (órbita + beam)

### Sonidos
- **BLOCK_STONE_BREAK:** Piedra raspándose
- **BLOCK_WITHER_BREAK_BLOCK:** Roca desgarrándose
- **BLOCK_PORTAL_AMBIENT:** Ambiente de grieta
- **ENTITY_ENDERMAN_TELEPORT:** Spawn de criaturas
- **ENTITY_WITHER_SPAWN:** Inicio de oleadas
- **BLOCK_BEACON_ACTIVATE:** Núcleo recogido
- **UI_TOAST_CHALLENGE_COMPLETE:** Evento completado

## 🔮 Futuro Narrativo

Este evento establece:
- **Concepto de "forma"** como complemento de "sombra"
- **El Observador** sigue presente y consciente
- **Estructuras antiguas** están despertando
- **Teaser para eventos del End** relacionados con forma/memoria

### Próximos Pasos Narrativos
1. Investigación del Fragmento de Forma Desviada
2. Relación entre "forma" y el End
3. Uso del fragmento en futuros eventos
4. Conexión con la memoria del mundo

## 📊 Estadísticas del Desarrollo

- **Líneas de código:** ~1,500 (3 archivos principales)
- **Configuración:** 249 líneas (eventos.yml)
- **Tiempo estimado:** 20 minutos de gameplay
- **Actos:** 4 completos
- **Sistema de partículas:** 8 tipos diferentes
- **Sistema de sonidos:** 7 efectos únicos
- **Comandos:** 7 subcomandos + info
- **Items únicos:** 1 (Fragmento de Forma Desviada)

## ✅ Estado de Implementación

| Componente | Estado | Notas |
|------------|---------|-------|
| Configuración YML | ✅ 100% | eventos.yml completo |
| Item único | ✅ 100% | Fragmento creado |
| Acto 1 | ✅ 100% | Fragmentos + proximidad |
| Acto 2 | ✅ 100% | Grieta + oleadas |
| Acto 3 | ✅ 100% | Núcleo + recogida |
| Acto 4 | ✅ 100% | Cliffhanger completo |
| Listener | ✅ 100% | Tracking criaturas |
| Comandos | ✅ 100% | 7 subcomandos |
| Integración | ✅ 100% | Plugin registrado |
| Testing | ⏳ Pendiente | Usuario debe probar |
| Autotesting | ⏳ Opcional | Escenarios pendientes |

## 🚀 Cómo Probar

1. **Compilar el plugin:**
   ```bash
   mvn clean package
   ```

2. **Copiar JAR al servidor:**
   ```
   target/Apocalipsis-*.jar → plugins/
   ```

3. **Reiniciar servidor y verificar:**
   ```
   [INFO] [EventController] ✓ Eventos narrativos registrados (Eco de Brasas, Eco de Sombras, Susurro Piedra Rota)
   [INFO] [SusurroPiedraRota] Listener registrado
   ```

4. **Ejecutar evento:**
   ```
   /avo evento3 start
   ```

5. **Monitorear consola:**
   ```
   [INFO] [SusurroPiedraRota] Iniciando evento...
   [INFO] [SusurroPiedraRota] Iniciando Acto 1: La Piedra Rota Despierta
   ```

## 📝 Notas Finales

- **Diseñado para:** 1-6 jugadores (flexible, desde solo hasta grupo pequeño)
- **Narrativa:** Enfocado en atmósfera y misterio
- **Combate:** Ligero, no es el foco principal
- **Duración:** Más corto que eventos anteriores (20 vs 120+ min)
- **Propósito:** Teaser narrativo + introducir concepto "forma"
- **Sistema de bots:** Compatible con autotesting usando escenarios genéricos

---

**Desarrollado para:** Apocalipsis Plugin v1.21.8  
**Fecha:** Noviembre 2025  
**Evento:** #3 - El Susurro en la Piedra Rota  
