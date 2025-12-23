# Sistema de Amigo Secreto - Implementado ✓

## 📋 Resumen

Se ha implementado completamente el sistema de **Amigo Secreto** para el evento de Navidad, con asignación aleatoria automática y tracking de regalos entregados.

---

## 🎁 Características Implementadas

### 1. **Asignación Aleatoria Automática**
- Cuando el admin activa el sistema, cada jugador es asignado aleatoriamente a otro jugador
- **No se puede auto-asignar**: El sistema previene que un jugador sea su propio amigo secreto
- Mensaje privado: Cada jugador recibe un mensaje privado indicándole a quién debe dar regalos
- Notificación global: Todos saben que el sistema se activó, pero solo ellos saben su asignación

### 2. **Sistema de Entrega de Regalos**
- Los jugadores deben estar **dentro de 10 bloques** del receptor para entregar
- Cada jugador debe entregar **2 regalos** (configurable en `navidad.yml`)
- Los regalos son **aleatorios** de una lista configurada:
  - 🍰 Pastel Navideño (x3)
  - 🍪 Galletas de Jengibre (x16)
  - 🍎 Manzana Dorada Festiva (x3)
  - ✨ Esencia de Alegría (x10 botellas de XP)

### 3. **Tracking y Progreso**
- El sistema cuenta cuántos regalos ha entregado cada jugador
- El sistema cuenta cuántos regalos ha recibido cada jugador
- Mensajes de progreso: "Has entregado 1/2 regalos a JugadorX"
- Mensajes de recepción: "Has recibido 2 regalos hasta ahora"

### 4. **Completación y Recompensas**
- Cuando un jugador completa su entrega (2/2 regalos), recibe:
  - **200 XP** de experiencia del sistema
  - **3 Fragmentos de Navidad**
  - Mensaje de felicitación
- Cuando **TODOS** completan sus entregas:
  - 🎆 Celebración épica con fuegos artificiales para todos
  - 🎊 Mensaje global de felicitación
  - ✨ Efectos especiales celebratorios

### 5. **Recordatorios Automáticos**
- Sistema de recordatorios cada **10 minutos** (configurable)
- Máximo de **3 recordatorios** por jugador
- Solo recuerda a jugadores que aún no completaron
- Muestra cuántos regalos faltan: "Aún te faltan 2 regalos por entregar"

---

## 🎮 Comandos

### Para Administradores:
```
/avo navidad amigo-secreto
```
- **Descripción**: Inicia el sorteo de amigo secreto
- **Permiso**: `avo.admin`
- **Efecto**: Asigna aleatoriamente cada jugador online a otro
- **Notificaciones**: Mensaje privado a cada participante + anuncio global

### Para Jugadores:
```
/avo navidad entregar
```
- **Descripción**: Entrega un regalo a tu amigo secreto
- **Requisitos**: 
  - Estar dentro de 10 bloques del receptor
  - Haber sido asignado en el sorteo
  - No haber completado ya los 2 regalos
- **Efecto**: 
  - Crea un regalo aleatorio
  - Lo agrega al inventario del receptor
  - Actualiza contadores
  - Muestra efectos de partículas/sonido
  - Otorga recompensas si es el último regalo

---

## ⚙️ Configuración (navidad.yml)

```yaml
amigo_secreto:
  enabled: true                    # Activar/desactivar sistema
  jugadores_minimos: 2             # Mínimo de jugadores para iniciar
  regalos_requeridos: 2            # Cuántos regalos debe dar cada uno
  
  mensajes:
    asignacion: |
      §c§l✦ ═════ AMIGO SECRETO ═════ ✦
      
      §f¡Se ha realizado el sorteo!
      §7Tu amigo secreto es: §e§l{jugador}
      
      §7Debes entregarle §c{cantidad} regalos§7.
      §7Acércate a esa persona y usa:
      §e/avo navidad entregar
    
    entregado: |
      §a§l✓ Regalo entregado
      §7Has entregado §e{actual}§7/§e{total} §7regalos
      §7a §f{jugador}
    
    recibido: |
      §d§l✦ ¡Has recibido un regalo!
      §7Alguien te ha dado: §f{item}
      §7Total recibidos: §e{actual}
    
    completado: |
      §a§l✦ ═════ MISIÓN COMPLETADA ═════ ✦
      
      §f¡Has completado tus entregas!
      §7Entregaste todos los regalos a §e{jugador}
      
      §6Recompensas:
      §7  • §b+200 XP
      §7  • §d3 Fragmentos de Navidad
      
      §c✦ §fGracias por participar §c✦
    
    celebracion_global: |
      §c§l✦ ═══════════════════════════ ✦
      §f§l   ¡AMIGO SECRETO COMPLETADO!
      §c§l✦ ═══════════════════════════ ✦
      
      §7¡Todos han entregado sus regalos!
      §eEl espíritu navideño brilla en todos.
      
      §c✦ §fFeliz Navidad §c✦
    
    recordatorio: |
      §e§l⚠ Recordatorio
      §7Aún tienes pendiente entregar regalos
      §7a tu amigo secreto.
      §7Te faltan: §c{pendientes} §7regalo(s)
    
    no_asignado: "§cNo tienes un amigo secreto asignado."
    ya_completado: "§aYa completaste tus entregas."
    demasiado_lejos: "§c¡Debes estar más cerca de {jugador}!"
    pocos_jugadores: "§cSe necesitan al menos {minimo} jugadores online."
  
  items_regalo:
    - material: CAKE
      cantidad: 3
      nombre: "§d✦ §fPastel Navideño §d✦"
      lore:
        - "§7Un delicioso pastel"
        - "§7horneado con amor"
    
    - material: COOKIE
      cantidad: 16
      nombre: "§6✦ §fGalletas de Jengibre §6✦"
      lore:
        - "§7Crujientes y especiadas"
        - "§7Receta familiar antigua"
    
    - material: GOLDEN_APPLE
      cantidad: 3
      nombre: "§e✦ §fManzana Dorada Festiva §e✦"
      lore:
        - "§7Brilla con magia navideña"
        - "§7Restaura cuerpo y alma"
    
    - material: EXPERIENCE_BOTTLE
      cantidad: 10
      nombre: "§b✦ §fEsencia de Alegría §b✦"
      lore:
        - "§7Cristalizada de momentos felices"
        - "§7Comparte la experiencia"
  
  recordatorios:
    enabled: true                  # Activar recordatorios automáticos
    intervalo_minutos: 10          # Cada cuántos minutos recordar
    max_recordatorios: 3           # Máximo de recordatorios por jugador
  
  recompensas:
    xp: 200                        # XP al completar entregas
    fragmentos: 3                  # Fragmentos al completar
```

---

## 🔧 Archivos Modificados

### 1. **NavidadEvent.java**
- ✅ Añadidos 5 campos nuevos para tracking
- ✅ Implementados 6 métodos nuevos:
  - `iniciarAmigoSecreto()` - Sorteo y asignación
  - `entregarRegaloAmigoSecreto(Player)` - Entrega de regalos
  - `crearRegaloAleatorio()` - Genera item aleatorio de config
  - `iniciarRecordatorios()` - Sistema de recordatorios periódicos
  - `verificarCompletacionTotal()` - Detecta cuando todos terminan
  - `detenerAmigoSecreto()` - Limpieza al finalizar evento
- ✅ Imports agregados: `ItemStack`, `ItemMeta`
- ✅ Persistencia de datos implementada (guarda/carga estado)

### 2. **ApocalipsisCommand.java**
- ✅ Añadido comando `amigo-secreto` (alias: `sorteo`) para admins
- ✅ Añadido comando `entregar` para jugadores
- ✅ Help menu actualizado con nuevos comandos

### 3. **AvoTabCompleter.java**
- ✅ Tab completion para `amigo-secreto`
- ✅ Tab completion para `entregar`

### 4. **navidad.yml**
- ✅ Nueva sección `amigo_secreto` completa
- ✅ Todos los mensajes configurables
- ✅ Lista de items de regalo configurable
- ✅ Configuración de recordatorios
- ✅ Configuración de recompensas

---

## 📊 Flujo de Uso

### Paso 1: Inicio del Evento
```
/avo navidad start
```

### Paso 2: Activar Amigo Secreto
```
/avo navidad amigo-secreto
```
- Sistema asigna aleatoriamente
- Cada jugador recibe mensaje privado
- Se activan recordatorios automáticos

### Paso 3: Jugadores Entregan Regalos
```
/avo navidad entregar
```
- Usar 2 veces (o las veces configuradas)
- Estar cerca del receptor (10 bloques)
- Recibir recompensas al completar

### Paso 4: Celebración Final
- Cuando el último jugador completa → Celebración épica automática
- Fuegos artificiales para todos
- Mensaje global de felicitación

### Paso 5: Finalizar Evento
```
/avo navidad stop
```
- Limpia datos del amigo secreto
- Para recordatorios
- Resetea contadores

---

## 🎯 Filosofía del Diseño

### ✨ Calidez Familiar
- Mensajes cálidos y acogedores
- Enfoque en dar, no en recibir
- Celebración comunitaria al final
- Regalos temáticos navideños

### 🎲 Aleatorización Justa
- Asignación completamente aleatoria
- Prevención de auto-asignación
- Redistribución en cada sorteo
- Todos participan por igual

### 🎁 Regalos Significativos
- Items útiles (comida, XP, manzanas doradas)
- Nombres y lore temáticos
- Variedad para sorpresa
- Balance entre útil y festivo

### 🏆 Recompensas Motivadoras
- XP para progresión
- Fragmentos para evento principal
- Satisfacción de completar
- Celebración comunitaria

---

## 🔒 Seguridad y Validaciones

- ✅ Verificación de jugadores online
- ✅ Mínimo de jugadores requerido (configurable)
- ✅ Prevención de duplicados en asignación
- ✅ Validación de distancia (10 bloques)
- ✅ Prevención de sobre-entrega
- ✅ Persistencia de datos (no se pierden en reload)
- ✅ Limpieza automática al detener evento

---

## 🎨 Efectos Visuales y Sonido

### Al Entregar Regalo:
- 🎆 Partículas de corazón
- 🔔 Sonido de campanas
- 💬 Mensajes formatados con colores

### Al Completar Entregas:
- ✨ Partículas adicionales
- 🎵 Sonido de logro
- 📜 Mensaje de recompensas

### Celebración Global:
- 🎆 Fuegos artificiales en círculo
- 🌟 Espiral de partículas
- 🎶 Secuencia musical
- 📢 Broadcast global

---

## 📝 Notas Técnicas

- **Thread-safe**: Usa Bukkit scheduler para operaciones async
- **Memory efficient**: Limpia datos al detener evento
- **Configurable**: Todo parametrizable en YAML
- **Extensible**: Fácil añadir más tipos de regalos
- **Persistente**: Guarda estado en `plugins/Apocalipsis/data/navidad_data.yml`

---

## ✅ Estado: COMPLETADO

✓ Sistema de asignación aleatoria  
✓ Comandos de admin y jugador  
✓ Tab completion  
✓ Configuración en YAML  
✓ Tracking de progreso  
✓ Recordatorios automáticos  
✓ Recompensas  
✓ Celebración global  
✓ Persistencia de datos  
✓ Efectos visuales/sonido  
✓ Validaciones de seguridad  

**El sistema está listo para usar en el servidor** 🎄✨
