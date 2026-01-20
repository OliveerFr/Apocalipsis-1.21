# Cambios: BossBar Misteriosa y Mensajes del Observador

## 📋 Resumen
Se ha reemplazado la cuenta regresiva del Evento 5 (Apertura del End) con una **BossBar dinámica** que muestra mensajes misteriosos del **Observador preocupado**, sin revelar el nombre del evento.

---

## ✨ Cambios Implementados

### 1. **BossBar Misteriosa en Cuenta Regresiva**

#### Antes:
- ❌ Mensajes en chat diciendo "LA APERTURA DEL END"
- ❌ Revelaba explícitamente "abriendo portal en..."
- ❌ No había feedback visual constante

#### Ahora:
- ✅ **BossBar dinámica** en la parte superior de la pantalla
- ✅ Mensajes misteriosos del Observador preocupado
- ✅ Cambio de color según urgencia:
  - 🟣 **PURPLE**: Más de 2 minutos
  - 🩷 **PINK**: 1-2 minutos  
  - 🔴 **RED**: Menos de 30 segundos
- ✅ Progreso visual que disminuye con el tiempo

#### Mensajes de la BossBar:
```
⚠ El Observador detecta algo extraño...
⚠ El Observador percibe algo... 5:30
⚡ Algo se está construyendo... 4:15
⚡ La energía aumenta... 2:45
⚡ Una presencia se acerca... 1:30
⚠ Algo está a punto de emerger... 0:45
⚠⚠⚠ ALGO DESPIERTA ⚠⚠⚠ 0:30
⚠⚠⚠ 10 ⚠⚠⚠
```

---

### 2. **Mensajes del Observador (Sin Spoilers)**

#### Anuncio Inicial:
```
§8§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
§7§l⚠ ⚠ ⚠ ALERTA DEL OBSERVADOR ⚠ ⚠ ⚠
§5Algo está siendo construido en las sombras...
§7Una presencia oscura se acerca en: §f§l5 minutos
§8§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
```

#### Mensajes en Hitos Temporales:

**5 minutos:**
```
§8§l━━━━━━━━━━━━━━━━━━━━━
§7El Observador: 'Siento una energía... desconocida'
§8§l━━━━━━━━━━━━━━━━━━━━━
```

**3 minutos:**
```
§8§l━━━━━━━━━━━━━━━━━━━━━
§7El Observador: 'Esto es... inquietante'
§8§l━━━━━━━━━━━━━━━━━━━━━
```

**2 minutos:**
```
§8§l━━━━━━━━━━━━━━━━━━━━━
§7El Observador: 'Jamás había sentido tal poder...'
§8§l━━━━━━━━━━━━━━━━━━━━━
```

**1 minuto:**
```
§8§l━━━━━━━━━━━━━━━━━━━━━
§c§lEl Observador: '¡PREPÁRENSE PARA LO PEOR!'
§8§l━━━━━━━━━━━━━━━━━━━━━
```

**30 segundos:**
```
§8§l━━━━━━━━━━━━━━━━━━━━━
§4§lEl Observador: '¡NO... ESO ES IMPOSIBLE!'
§8§l━━━━━━━━━━━━━━━━━━━━━
```

**Conteo final (10-1):**
```
§4§l⚠ 10 ⚠
Título: §4§l⚠ 10 ⚠
Subtítulo: §8Algo emerge...
```

---

### 3. **Inicio del Evento - Observador Aterrado**

#### Antes:
```
§5§l⚡ ⚡ ⚡ EL PORTAL SE HA ABIERTO ⚡ ⚡ ⚡
§d§lEl Desolador del Vacío espera en las sombras...

Título: §5§l⚡ EVENTO INICIADO ⚡
Subtítulo: §d§l¡EL DESOLADOR ESPERA!
```

#### Ahora:
```
§8§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
§7§l⚠ ⚠ ⚠ EL OBSERVADOR GRITA ⚠ ⚠ ⚠
§4§l'¡ALGO HA EMERGIDO DE LAS SOMBRAS!'
§5Una presencia antigua y terrible se materializa...
§8§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬

Título: §8§l⚠ §4EMERGENCIA§8 ⚠
Subtítulo: §5§lEl Observador: '¡CORRAN!'
```

---

### 4. **Inicio Inmediato (Sin Cuenta Regresiva)**

#### Antes:
```
§a✓ Evento §5§l⚡ La Apertura del End §ainiciado

Título: §5§l⚔ LA APERTURA DEL END ⚔
Subtítulo: §7El Desolador espera...
```

#### Ahora:
```
§a✓ Algo ha emergido... §c¡El Observador está aterrado!

Título: §8§l⚠ §4EMERGENCIA§8 ⚠
Subtítulo: §7El Observador: 'Algo... terrible ha despertado'
```

---

## 🎮 Experiencia de Juego

### Suspensión Narrativa:
- Los jugadores **NO saben** qué evento es hasta que comienza
- El Observador (personaje conocido) actúa como narrador preocupado
- Lenguaje vago: "algo", "presencia", "energía desconocida"
- Progresión del pánico: `preocupado → inquieto → aterrado → gritando`

### Retroalimentación Visual:
- **BossBar siempre visible** - no se pierde en el chat
- **Cambio de color** indica urgencia creciente
- **Barra de progreso** muestra tiempo restante visualmente
- **Títulos grandes** para momentos clave (10-1, inicio)

### Inmersión:
- ❌ Sin nombres de eventos revelados prematuramente
- ✅ Misterio mantenido hasta el final
- ✅ El Observador como testigo aterrado
- ✅ Sensación de algo "construyéndose" en las sombras

---

## 📝 Comandos Afectados

```bash
# Iniciar con cuenta regresiva (ahora con BossBar)
/avo evento5 start 5

# Iniciar inmediato (mensaje misterioso)
/avo evento5 start

# Alias
/avo aperturaend start 5
```

---

## 🔧 Detalles Técnicos

### Archivo Modificado:
- `ApocalipsisCommand.java` (líneas 6417-6676)

### Cambios de Código:
1. **Creación de BossBar:**
   ```java
   final org.bukkit.boss.BossBar bossBarCountdown = Bukkit.createBossBar(
       "§8⚠ El Observador detecta algo extraño...",
       org.bukkit.boss.BarColor.PURPLE,
       org.bukkit.boss.BarStyle.SEGMENTED_20
   );
   ```

2. **Actualización Dinámica:**
   ```java
   // Cambiar color según tiempo
   if (restante <= 30) {
       bossBarCountdown.setColor(org.bukkit.boss.BarColor.RED);
   } else if (restante <= 60) {
       bossBarCountdown.setColor(org.bukkit.boss.BarColor.PINK);
   } else if (restante <= 120) {
       bossBarCountdown.setColor(org.bukkit.boss.BarColor.PURPLE);
   }
   ```

3. **Progreso Visual:**
   ```java
   double progreso = (double) restante / segundos;
   bossBarCountdown.setProgress(Math.max(0.0, Math.min(1.0, progreso)));
   ```

4. **Limpieza al Finalizar:**
   ```java
   bossBarCountdown.removeAll(); // Elimina la BossBar cuando inicia el evento
   ```

---

## 🎯 Resultado Final

### Antes del Cambio:
- Jugadores veían "LA APERTURA DEL END" y sabían exactamente qué venía
- Mensajes se perdían en el chat
- Faltaba tensión narrativa

### Después del Cambio:
- ✅ **Misterio total** - nadie sabe qué es hasta que comienza
- ✅ **BossBar visible** - feedback constante sin distracciones
- ✅ **Narrativa mejorada** - El Observador como testigo aterrado
- ✅ **Tensión creciente** - Color y mensajes escalan con el tiempo
- ✅ **Inmersión completa** - Sensación de algo emergiendo de las sombras

---

## 📦 Versión
- **JAR Compilado:** `Apocalipsis-1.22.48.jar`
- **Ubicación:** `target/Apocalipsis-1.22.48.jar`
- **Estado:** ✅ Compilación exitosa

---

## 🚀 Próximos Pasos
1. Copiar JAR al servidor: `plugins/Apocalipsis-1.22.48.jar`
2. Reiniciar servidor
3. Probar comando: `/avo evento5 start 3`
4. Observar la BossBar misteriosa en acción
5. Verificar que el Observador aparece preocupado/aterrado
6. Confirmar que NO se revela el nombre del evento

---

**Fecha:** 2026-01-19  
**Cambios:** BossBar misteriosa + Observador aterrado + Sin spoilers del evento
