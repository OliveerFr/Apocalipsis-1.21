# 📋 SISTEMA DE RANGOS - DOCUMENTACIÓN COMPLETA

## ✅ Nombres de Rangos Estandarizados

El sistema usa **dos tipos de nombres** para cada rango:

### 🔑 IDs Técnicos (usados en código/configuración)
Estos NUNCA deben cambiar porque son usados en:
- Enum `MissionRank.java`
- Claves en `rangos.yml`
- Claves en `recompensas.yml`
- Campo `rangos: []` en `misiones_new.yml`
- Comandos internos (`/avo setxp`, etc.)

**Lista de IDs técnicos:**
```
NOVATO
EXPLORADOR
SOBREVIVIENTE
VETERANO
LEYENDA
MAESTRO
TITAN
ABSOLUTO
```

### 🎨 Nombres de Display (mostrados al jugador)
Estos son configurables en `rangos.yml` bajo `display_name`:

| Rango Técnico | Display Name | Color |
|--------------|--------------|-------|
| `NOVATO` | `&7Perdido` | Gris |
| `EXPLORADOR` | `&aDespertar` | Verde |
| `SOBREVIVIENTE` | `&bResistente` | Aqua |
| `VETERANO` | `&ePerseverante` | Amarillo |
| `LEYENDA` | `&6&lAscendido` | Oro/Negrita |
| `MAESTRO` | `&d&lIluminado` | Rosa/Negrita |
| `TITAN` | `&5&lTrascendente` | Morado/Negrita |
| `ABSOLUTO` | `&c&l&kI&c&l Eterno &c&l&kI` | Rojo/Negrita/Ofuscado |

---

## 📂 Archivos y Uso de Rangos

### 1️⃣ `rangos.yml` - CONFIGURACIÓN MAESTRA
```yaml
ranks:
  NOVATO:                          # ← ID Técnico (NO CAMBIAR)
    display_name: "&7Perdido"      # ← Nombre mostrado al jugador
    xp_required: 0
    misiones_diarias: 10
    tab_prefix: "&7[Perdido] "     # ← TAB usa display_name
    chat_prefix: "&7P&8| "
    scoreboard_color: "GRAY"
```

**Regla:** Las claves (`NOVATO`, `EXPLORADOR`, etc.) deben coincidir EXACTAMENTE con el enum Java.

---

### 2️⃣ `misiones_new.yml` - ASIGNACIÓN DE MISIONES
```yaml
- id: "matar_zombies"
  nombre: "Elimina 10 zombies"
  tipo: "MATAR"
  objetivo: "ZOMBIE"
  cantidad: 10
  dificultad: "FACIL"
  rangos: ["NOVATO", "EXPLORADOR"]  # ← Usa IDs técnicos
  recompensa_ps: 35
```

**Regla:** El array `rangos: []` SIEMPRE usa los IDs técnicos.

---

### 3️⃣ `recompensas.yml` - RECOMPENSAS POR RANGO

#### Bonificaciones de misiones diarias:
```yaml
bonus_por_rango:
  NOVATO:                                    # ← ID técnico como clave
    mensaje: "&7[Bonus &7Perdido&7] ..."    # ← Display name en mensaje
  EXPLORADOR:
    mensaje: "&a[Bonus &aDespertar&7] ..."
```

#### Recompensas de subida de rango:
```yaml
rangos:
  EXPLORADOR:                                           # ← ID técnico
    titulo: "§a§l¡Despertar Alcanzado!"               # ← Display name
    descripcion: "§e%jugador% §7ha alcanzado el rango §aDespertar"
```

**Regla:** 
- **Claves de secciones** → ID técnico (`NOVATO`, `EXPLORADOR`)
- **Mensajes al jugador** → Display name (`Perdido`, `Despertar`)

---

### 4️⃣ `ExperienceService.java` - MENSAJES DE SUBIDA
```java
private void onRankUp(Player player, MissionRank oldRank, MissionRank newRank) {
    String rankName = newRank.getDisplayName();  // ← Obtiene display_name de rangos.yml
    
    player.sendMessage("§7Has alcanzado el rango " + rankName + "§7!");
    // Output: "Has alcanzado el rango §aDespertar§7!"
}
```

**Regla:** El código SIEMPRE usa `getDisplayName()` para mostrar el nombre bonito al jugador.

---

## 🔄 Flujo de Datos

```
1. Jugador gana XP
2. ExperienceService verifica si sube de rango
3. MissionRank.fromXp(xp) → determina rango según XP
4. Se obtiene display_name de rangos.yml
5. Mensajes usan display_name para mostrar al jugador
6. RewardService entrega recompensas según ID técnico
7. MissionService actualiza misiones disponibles
```

---

## ⚠️ REGLAS CRÍTICAS

### ✅ HACER:
1. **Cambiar display_name en `rangos.yml`** si quieres modificar cómo se muestra un rango
2. **Usar IDs técnicos** en configuración (`rangos: ["NOVATO"]`)
3. **Usar display_name** en mensajes al jugador
4. Mantener coherencia visual (colores, formato) entre archivos

### ❌ NO HACER:
1. **Cambiar los IDs técnicos** (`NOVATO`, `EXPLORADOR`, etc.) - romperá el código Java
2. **Usar display_name en configuración** (`rangos: ["Perdido"]`) - no funcionará
3. Mezclar nombres técnicos y display en el mismo contexto
4. Crear rangos nuevos sin actualizar el enum Java

---

## 🎯 Ejemplo Completo: Rango EXPLORADOR

### En `rangos.yml`:
```yaml
EXPLORADOR:
  display_name: "&aDespertar"
  xp_required: 2450
  misiones_diarias: 10
  tab_prefix: "&a[Despertar] "
  scoreboard_color: "GREEN"
```

### En `misiones_new.yml`:
```yaml
rangos: ["EXPLORADOR"]  # ID técnico
```

### En `recompensas.yml`:
```yaml
EXPLORADOR:  # ID técnico como clave
  titulo: "§a§l¡Despertar Alcanzado!"    # Display name en mensaje
  descripcion: "§e%jugador% §7ha alcanzado el rango §aDespertar"
```

### En chat/mensajes:
```
[Despertar] Steve: Hola!
Has alcanzado el rango §aDespertar§7!
[Bonus §aDespertar§7] +8 Hierro, +2 Perlas
```

---

## 📊 Tabla de Referencia Rápida

| ID Técnico | Display | XP Requerido | Misiones/día | Nivel Equiv. |
|-----------|---------|--------------|--------------|--------------|
| `NOVATO` | Perdido | 0 | 10 | 1 |
| `EXPLORADOR` | Despertar | 2,450 | 10 | 8 |
| `SOBREVIVIENTE` | Resistente | 9,800 | 8 | 15 |
| `VETERANO` | Perseverante | 22,400 | 7 | 22 |
| `LEYENDA` | Ascendido | 40,250 | 6 | 29 |
| `MAESTRO` | Iluminado | 63,350 | 6 | 36 |
| `TITAN` | Trascendente | 91,700 | 6 | 43 |
| `ABSOLUTO` | Eterno | 110,075 | 4 | 50 |

---

## 🔍 Comandos de Verificación

```bash
# Verificar que todos los archivos usan IDs correctos
grep -r "rangos:" src/main/resources/misiones_new.yml

# Verificar nombres de display en rangos.yml
grep "display_name:" src/main/resources/rangos.yml

# Ver definición del enum
cat src/main/java/me/apocalipsis/missions/MissionRank.java
```

---

## ✅ Cambios Aplicados (Hoy)

1. ✅ **misiones_new.yml** - Mejorados nombres de misiones (~100 misiones)
   - Cambio: Nombres genéricos → Específicos con cantidades
   - Ejemplo: "Matar zombies" → "Elimina 10 zombies"

2. ✅ **recompensas.yml** - Estandarizados mensajes de rangos
   - Bonus: Ahora usan formato `[Bonus &cNombreRango&7]`
   - Descripciones: "ha alcanzado el rango" (consistente)
   - Display names con colores y formato correcto

3. ✅ **Documentación** - Este archivo creado
   - Define IDs técnicos vs Display names
   - Reglas claras de uso en cada archivo
   - Ejemplos completos por rango

---

## 🎨 Tema Visual: "Ascenso del Caos"

Los nombres fueron diseñados siguiendo una progresión narrativa:

1. **Perdido** (Gris) - Jugador nuevo, confundido, vulnerable
2. **Despertar** (Verde) - Comienza a entender el mundo
3. **Resistente** (Aqua) - Sobrevive consistentemente
4. **Perseverante** (Amarillo) - Domina desafíos básicos
5. **Ascendido** (Oro) - Alcanza nivel élite
6. **Iluminado** (Rosa) - Comprende los secretos del servidor
7. **Trascendente** (Morado) - Supera límites mortales
8. **Eterno** (Rojo/Ofuscado) - Máximo nivel, casi divino

---

**Última actualización:** 27 de enero de 2026
**Versión del plugin:** 1.22.56
**Autor:** Sistema de rangos Apocalipsis
