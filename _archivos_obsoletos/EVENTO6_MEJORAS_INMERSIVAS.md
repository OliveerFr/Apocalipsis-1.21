# Mejoras Inmersivas del Evento 6: Cuando el Mundo Decide Olvidar

## 📋 Cambios Implementados

### 🔄 Sistema de Ciclos Mejorado

#### Nombre del Nuevo Mundo
- **ANTES**: `world_ciclo_reset` (genérico, sin significado)
- **AHORA**: `Ciclo_2_Los_Que_Recuerdan` (narrativo, conecta con la historia)

#### Spawn en Superficie
- **PROBLEMA**: Jugadores aparecían en cuevas después del reinicio
- **SOLUCIÓN**: 
  - Nuevo método `encontrarSuperficieSegura()` en Java
  - Busca superficie plana, segura, Y >= 60
  - Evita agua, lava y bloques peligrosos
  - Radio de búsqueda de 50 bloques
  - Log detallado de coordenadas de spawn

```java
// Código implementado en Evento6MundoOlvidado.java
private Location encontrarSuperficieSegura(World mundo, Location origen) {
    // Busca en círculos concéntricos
    // Verifica Y >= 60 (no cuevas)
    // Verifica bloque sólido abajo
    // Verifica aire en posición de spawn
    // Evita agua y lava
}
```

---

## 🎭 Narrativa Expandida por Acto

### ACTO 1: Normalidad (0:00 - 5:00)
**Antes**: "Todo comienza normal. Nada parece fuera de lo común."

**Ahora**: 
> "Todo comienza perfectamente normal. El día es como cualquier otro. Los jugadores trabajan, construyen, viven. Nada, absolutamente nada, parece fuera de lo común. Esta calma es real... por ahora."

**Efectos añadidos**:
- Asegurar tiempo estable (día/noche normal)
- Mobs normales (spawn predecible)
- Clima normal (sin tormentas raras)
- Observador casi ausente (solo 2 mensajes sutiles en 5 minutos)

**Mensajes del Observador**:
- 3:00 min: `§8[§7...§8]` (chat)
- 4:30 min: `§8[§7.§8]` (actionbar - imperceptible)

---

### ACTO 2: Primeras Rarezas (5:00 - 10:00)
**Antes**: "Detalles pequeños, casi ignorables."

**Ahora**:
> "Un trueno sin nubes. Un animal que te mira fijamente. El reloj parece atrasarse un segundo. Cosas que podrías ignorar... si quisieras. Algo no está del todo bien, pero aún puedes convencerte de que todo es normal."

**Efectos mejorados**:
- **Frecuencia aumentada**: Intervalo 30-70 segundos (antes 45-90)
- **Incremento progresivo**: Las anomalías se vuelven más frecuentes
- **Nuevos efectos**:
  - Parpadeo de luz (1 segundo de oscuridad)
  - Partículas END_ROD apareciendo/desapareciendo
  - Bloques que parecen desaparecer momentáneamente
  - Mobs congelados 8 segundos (antes 5)

**Diálogos del Observador**:
```yaml
0:45 - §8[§7...§8] (actionbar)
2:00 - §8[§7...§8] §7¿Hmm?
3:40 - §8[§7...§8] §7No... no todavía.
4:40 - §8[§7..§8] (actionbar)
```

---

### ACTO 3: Inestabilidad (10:00 - 12:30)
**Antes**: "Las anomalías se vuelven imposibles de ignorar."

**Ahora**:
> "Ya no puedes ignorarlo. El mundo se comporta erráticamenteBloques parpadean. El tiempo se detiene por momentos. Sonidos de portal sin portales. Los animales huyen. Las anomalías son constantes, evidentes, aterradoras."

**Efectos dramáticamente ampliados**:
- **Partículas caóticas rotantes**: ASH, ELECTRIC_SPARK, END_ROD, SOUL
- **Lag simulado intensificado**: 1.5 segundos (antes 1), más frecuente
- **Reloj roto**: Tiempo retrocede, acelera, o se congela
- **Portal sonidos múltiples**: 4 tipos diferentes de sonidos
- **NUEVO - Bloques inestables**: Parpadean, flotan, caen solos
- **NUEVO - Animales huyendo**: Pánico masivo en radio de 50 bloques
- **NUEVO - Cielo inestable**: Cambios rápidos cada segundo

**Diálogos del Observador (más urgentes)**:
```yaml
0:10 - §8[§7...§8] §7Oh no...
0:45 - §c§lOtra vez no... (título grande)
1:30 - §8[§7...§8] §7Ya decidió.
2:10 - §cNo puedo detenerlo. (título)
```

---

### ACTO 4: El Quiebre (12:30 - 14:30)
**Antes**: "Todo ocurre rápido. El mundo se fractura sin previo aviso."

**Ahora**:
> "No hay advertencias. No hay cuenta atrás. El mundo simplemente... se rompe. La realidad se fractura en tiempo real. Bloques flotan. El cielo se vuelve negro. Sonidos ensordecedores. El Observador grita una última advertencia inútil. Todo colapsa."

**Secuencia de quiebre mejorada**:
```yaml
0:00 - ADVERTENCIA FINAL
       §c§l¡YA VIENE!
       "No puedo—" (Observador cortado)
       
0:02 - OSCURIDAD TOTAL
       Ceguera 5s + EXPLOSION_HUGE particles
       
0:03 - FRACTURA ESPACIAL
       Náusea extrema + WITHER_SPAWN sound
       
0:05 - COLAPSO VISUAL
       2000 partículas de 4 tipos aleatorios
       
0:07 - CONGELACIÓN TEMPORAL
       4 segundos de freeze total
       
0:10 - MENSAJE FINAL
       "El mundo ya tomó la decisión"
       
0:14 - SILENCIO DE MUERTE
       30 segundos: ceguera + silencio + inmovilidad
```

---

### ACTO 5: El Reinicio (14:30 - 15:00)
**Antes**: "Pantalla negra. Todo cambia."

**Ahora**:
> "Pantalla negra. Silencio total. Y entonces... todo cambia. El mundo ha sido completamente borrado y recreado desde cero. Es nuevo. Es vacío. Es el Ciclo 2. Pero ustedes... ustedes aún recuerdan."

**Mejoras técnicas**:
- **Nombre del mundo**: `Ciclo_2_Los_Que_Recuerdan`
- **Spawn en superficie garantizado**:
  - Busca superficie segura (Y >= 60)
  - Verifica bloques sólidos
  - Evita agua/lava
  - Todos aparecen juntos
- **Mensaje de llegada**:
  ```
  Título: §7CICLO 2
  Subtítulo: §8Los que recuerdan
  ```

**Secuencia mejorada**:
```yaml
0:00 - Pantalla negra (2s)
0:02 - Crear ciclo "Ciclo_2_Los_Que_Recuerdan"
0:02.5 - Teleport a SUPERFICIE (verificado)
0:03 - Partículas cayendo desde arriba (300 END_ROD)
0:03.5 - Mensaje: "CICLO 2 - Los que recuerdan"
```

---

### ACTO 6: Nuevo Mundo (15:00 - 20:00)
**Antes**: "Todos aparecen juntos. Sin inventarios, sin construcciones."

**Ahora**:
> "El reinicio está completo. Todos aparecen en superficie, en un mundo virgen. Tierra sin tocar. Cielo limpio. Silencio. Sus manos están vacías — inventarios borrados. Sus casas no existen — construcciones borradas. Pero... sus mentes están intactas. Recuerdan todo. El mundo borró el LUGAR, pero no los borró a ELLOS."

**Diálogos expandidos del Observador**:
```yaml
0:05 - §8[§7...§8] (actionbar - despertar)
0:15 - No los borró... (título)
0:45 - Solo borró el lugar.
1:30 - Ustedes... aún están aquí.
2:30 - El mundo no quiere cambiar. (título)
4:30 - Reiniciar es más fácil.
```

**Efectos sutiles**:
- Partículas END_ROD flotando suavemente (cada 5s)
- Sensación de nuevo comienzo, calma post-tormenta

---

### ACTO 7: Comprensión Lenta (20:00 - 25:00)
**Antes**: "Lentamente comprenden."

**Ahora**:
> "Los jugadores exploran. Construyen refugios básicos. Recolectan recursos. Y lentamente... empiezan a comprender. El mundo los borró del mapa, pero conservó su esencia. No es la primera vez que esto pasa. El Observador lo sabe. Lo ha visto antes. Esto es un CICLO."

**Revelación progresiva de la verdad**:
```yaml
0:40 - El mundo hace esto...
1:40 - ...cuando se cansa de lo que hicieron. (título)
3:00 - §e§lReiniciar es más fácil que cambiar.
3:50 - Borrar el lugar...
4:30 - §cpero mantener a las personas. (título)
```

**Efectos**:
- Sonido AMBIENT_CAVE cada 1.5-2.5 min (ambiente pensativo)

---

### ACTO 8: La Fractura (25:00 - 30:00)
**Antes**: "El Nether sigue accesible."

**Ahora**:
> "Si alguien construye un portal al Nether... descubrirá algo perturbador. El Nether NO fue reseteado. Aún conserva las estructuras, los caminos, las fortalezas del Ciclo 1. El mundo borró el Overworld, pero lo que está DEBAJO permanece. El fuego no olvida. La piedra no olvida. Evidencia física de que esto YA PASÓ ANTES."

**Mensajes progresivos**:
```yaml
0:30 - Si bajan al Nether...
1:30 - verán que no todo fue borrado. (título)
3:00 - §eLo que está debajo permanece.
4:10 - El fuego no olvida.
```

**Efectos si entran al Nether**:
```yaml
ENTRADA:
- §cLo que está debajo... (título)
- §cno olvida tan fácil. (3s después)

CONSTANTES:
- 15 partículas SOUL cada 3s
- 3 tipos de sonidos ambient (Basalt, Nether Wastes, Respawn Anchor)
- Volumen 1.2, cada 40-80s
```

---

### ACTO 9: El End Permanece (30:00 - 35:00)
**Antes**: "El End tampoco fue reseteado."

**Ahora**:
> "El End tampoco fue reseteado. Es un espacio compartido entre TODOS los ciclos. Un lugar fuera del tiempo lineal. Si derrotaron al Dragón en el Ciclo 1, sigue muerto en el Ciclo 2. Las ciudades End que exploraron siguen ahí. El End es el único lugar que OBSERVA todos los ciclos sin participar en ellos. Por eso el Observador está allí."

**Revelación del Observador**:
```yaml
1:00 - El End...
2:00 - §5§lAlgunos lugares no se reinician. (título)
3:00 - Solo observan.
4:00 - §8Por eso estoy allí.
4:40 - §5Viendo cada ciclo repetirse. (título)
```

**Efectos si entran al End**:
```yaml
ENTRADA:
- "Bienvenidos... otra vez." (título inquietante)

CONSTANTES:
- 5 partículas PORTAL cada 4s
- ENDERMAN_STARE (sonido de vigilancia) cada 1-2min
- ENDER_DRAGON_AMBIENT cada 1.5-3min
```

---

### ACTO 10: Cierre (35:00 - 40:00)
**Antes**: "El mensaje final revela la verdad."

**Ahora**:
> "Los jugadores se adaptan. Construyen. Sobreviven. Se reorganizan en el Ciclo 2. Y entonces... el Observador revela la verdad final. Esto NO es la primera vez. No es el segundo ciclo. Es solo... otro más. El mundo ha hecho esto antes. Y lo volverá a hacer. Los ciclos se repiten. Eternamente."

**Revelación completa**:
```yaml
1:00 - Ya se están adaptando...
2:00 - Como siempre lo hacen.
3:00 - §7§lEste no es un comienzo. (título)
3:30 - §c§lEs una repetición. (título grande)
4:10 - El mundo ha hecho esto antes.
4:40 - §cY lo volverá a hacer. (título final)
4:55 - §8[§7...§8] (silencio)
```

**Recompensas finales** (4:40):
- Fragmento de Memoria (ECHO_SHARD)
- Cicatriz Temporal (NETHERITE_SCRAP)
- Eco de la Repetición (RECOVERY_COMPASS)
- 350 PS totales por participación completa

---

## 🎨 Efectos Inmersivos Añadidos

### Por Acto

| Acto | Efectos Principales |
|------|-------------------|
| 1 - Normalidad | Ninguno (calma total) |
| 2 - Rarezas | 6 tipos de anomalías sutiles |
| 3 - Inestabilidad | 8 tipos de efectos caóticos |
| 4 - Quiebre | Secuencia de 7 pasos devastadores |
| 5 - Reinicio | Spawn en superficie + partículas |
| 6 - Nuevo Mundo | Partículas suaves de nuevo comienzo |
| 7 - Comprensión | Ambiente pensativo (sonidos cave) |
| 8 - Fractura | Efectos opresivos en Nether |
| 9 - End | Sensación de vigilancia eterna |
| 10 - Cierre | Silencio reflexivo final |

---

## 📝 Diálogos del Observador: Historia Completa

### Línea Narrativa

**Acto 1-2**: Silencio → Duda  
`[...]` → `¿Hmm?` → `No todavía...`

**Acto 3**: Reconocimiento → Desesperación  
`Oh no...` → `¡Otra vez no!` → `Ya decidió.` → `No puedo detenerlo.`

**Acto 4**: Advertencia → Colapso  
`¡YA VIENE!` → `No puedo—` → `El mundo ya tomó la decisión.`

**Acto 5**: Silencio del reinicio  
`...`

**Acto 6**: Revelación de qué pasó  
`No los borró...` → `Solo borró el lugar.` → `Ustedes aún están aquí.`

**Acto 7**: Revelación del porqué  
`El mundo hace esto...` → `cuando se cansa` → `Reiniciar es más fácil que cambiar.`

**Acto 8**: Evidencia física  
`Si bajan al Nether...` → `no todo fue borrado` → `El fuego no olvida.`

**Acto 9**: Revelación del Observador  
`El End...` → `Algunos lugares no se reinician` → `Por eso estoy allí.` → `Viendo cada ciclo repetirse.`

**Acto 10**: Verdad final  
`Ya se adaptan...` → `Como siempre lo hacen.` → `Este no es un comienzo.` → `Es una repetición.` → `Y lo volverá a hacer.`

---

## 🔧 Cambios Técnicos en el Código

### Archivo: `Evento6MundoOlvidado.java`

**Método modificado**: `ejecutarActo5()`
- Cambio de nombre del ciclo de `"world_ciclo_reset"` a `"Ciclo_2_Los_Que_Recuerdan"`
- Implementación de búsqueda de superficie segura
- Verificación de Y >= 60 para evitar cuevas
- Logging detallado de coordenadas de spawn

**Método añadido**: `encontrarSuperficieSegura(World, Location)`
```java
- Búsqueda en círculos concéntricos (radio 0-50 bloques)
- Ángulos de 45° para cobertura completa
- Verificación de Y >= 60 (no cuevas)
- Verificación de bloque sólido abajo
- Verificación de aire en posición de spawn
- Evita agua (WATER) y lava (LAVA)
- Retorna null si no encuentra superficie válida
```

### Archivo: `evento6_mundo_olvidado.yml`

**Cambios en configuración**:
```yaml
evento:
  mundo_nuevo_nombre: "Ciclo_2_Los_Que_Recuerdan"  # Antes: "world_ciclo_reset"
```

**Cambios en actos**: 10 actos completamente reescritos con:
- Descripciones expandidas (3-5x más texto)
- Diálogos del Observador aumentados (40+ mensajes nuevos)
- Efectos inmersivos detallados
- Configuración técnica de cada efecto

---

## 🎯 Resultados Esperados

### Inmersión Narrativa
- Los jugadores comprenden la historia sin explicaciones externas
- El Observador revela la verdad progresivamente
- La revelación final es impactante y coherente

### Experiencia Técnica
- **Spawn correcto**: Siempre en superficie, nunca en cuevas
- **Sistema de ciclos**: Funcionando correctamente con nombre narrativo
- **Efectos visuales**: Cada acto se SIENTE diferente

### Coherencia del Mundo
- **Nether**: Evidencia física del ciclo anterior (no reseteado)
- **End**: Observador eterno, trasciende ciclos
- **Overworld**: Completamente nuevo, pero jugadores recuerdan

---

## 📊 Comparación Antes/Después

| Aspecto | Antes | Ahora |
|---------|-------|-------|
| **Nombre ciclo** | world_ciclo_reset | Ciclo_2_Los_Que_Recuerdan |
| **Spawn** | Random (cuevas posibles) | Superficie garantizada (Y≥60) |
| **Descripción actos** | 1-2 líneas genéricas | 3-5 líneas narrativas |
| **Mensajes Observador** | ~20 mensajes básicos | ~60 mensajes profundos |
| **Efectos inmersivos** | Básicos | Detallados por acto |
| **Historia** | Implícita | Explícita y revelada |
| **Coherencia** | Media | Alta (todo conecta) |

---

## ✅ Checklist de Mejoras

- [x] Nombre del ciclo cambiado a narrativo
- [x] Spawn en superficie implementado
- [x] Método `encontrarSuperficieSegura()` añadido
- [x] Acto 1: Normalidad real con calma total
- [x] Acto 2: Rarezas sutiles pero inquietantes
- [x] Acto 3: Inestabilidad caótica evidente
- [x] Acto 4: Quiebre dramático y rápido
- [x] Acto 5: Reinicio con spawn correcto
- [x] Acto 6: Revelación de qué se conservó
- [x] Acto 7: Revelación del porqué (ciclos)
- [x] Acto 8: Nether como evidencia física
- [x] Acto 9: End como observador eterno
- [x] Acto 10: Verdad final del ciclo repetitivo
- [x] Diálogos del Observador expandidos
- [x] Efectos inmersivos por acto
- [x] Configuración YML actualizada
- [x] Código Java modificado y compilado

---

## 🚀 Para Ejecutar

1. **Compilar**: `mvn clean package -DskipTests`
2. **Copiar JAR**: `target/Apocalipsis-1.22.55.jar` al servidor
3. **Verificar config**: `plugins/Apocalipsis/ciclos.yml` debe tener `enabled: true`
4. **Iniciar evento**: `/avo evento6 start`

### Verificaciones Post-Inicio
- [ ] Jugadores aparecen en superficie (no cuevas)
- [ ] Mundo se llama "Ciclo_2_Los_Que_Recuerdan"
- [ ] Mensajes del Observador aparecen en tiempos correctos
- [ ] Efectos visuales funcionan en cada acto
- [ ] Reinicio funciona correctamente a los 14:30

---

## 📖 Narrativa Completa Resumida

1. **Normalidad** → Todo está bien (mentira)
2. **Rarezas** → Algo no encaja
3. **Inestabilidad** → El mundo se está rompiendo
4. **Quiebre** → Colapso total
5. **Reinicio** → El mundo se reinicia
6. **Nuevo Mundo** → Sin lugar, pero con memorias
7. **Comprensión** → Esto es un ciclo
8. **Fractura** → Evidencia: el Nether permanece
9. **End** → Observador eterno ve todo
10. **Cierre** → Esto pasó antes, pasará de nuevo

**Mensaje final**: El mundo prefiere reiniciar que cambiar. Los lugares desaparecen, pero las personas permanecen, condenadas a recordar cada ciclo.

---

**Versión**: 1.22.55  
**Fecha**: 26 de enero de 2026  
**Estado**: ✅ Compilado y listo para implementación
