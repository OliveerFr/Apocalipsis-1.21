# CHANGELOG - Restricción Comando Habilidades (v1.22.73)

## 📅 Fecha
- **Versión:** v1.22.73
- **Fecha:** Actual

## 🎯 Objetivo
Restringir el acceso al comando `/habilidades` y sus alias para que solo jugadores con permisos de moderador puedan acceder al menú del árbol de habilidades.

---

## 🔒 Cambios Implementados

### 1. **Restricción de Comando** (`Apocalipsis.java`)

#### Ubicación: Línea 371
- **Antes:**
  ```java
  getCommand("habilidades").setExecutor((sender, cmd, label, args) -> {
      if (sender instanceof org.bukkit.entity.Player player) {
          skillTreeGUI.openMainMenu(player);
      } else {
          sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
      }
      return true;
  });
  ```

- **Después:**
  ```java
  getCommand("habilidades").setExecutor((sender, cmd, label, args) -> {
      if (sender instanceof org.bukkit.entity.Player player) {
          // Restringido a moderadores
          if (!player.hasPermission("apocalipsis.admin")) {
              player.sendMessage("§c§l✗ §cNo tienes permiso para usar este comando.");
              return true;
          }
          skillTreeGUI.openMainMenu(player);
      } else {
          sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
      }
      return true;
  });
  ```

---

## ⚙️ Funcionamiento

### Permisos Requeridos
- **Permiso:** `apocalipsis.admin`
- **Alternativa:** Operador del servidor (`/op`)

### Comandos Afectados
- `/habilidades`
- `/habilidad`
- `/skills`
- `/skill`

### Comportamiento
1. **Sin permiso:** Jugador recibe mensaje `§c§l✗ §cNo tienes permiso para usar este comando.`
2. **Con permiso:** Se abre normalmente el menú principal del árbol de habilidades

---

## 🧪 Testing

### ✅ Casos de Prueba
1. **Jugador sin permiso:**
   - Ejecutar `/habilidades` → Mensaje de error
   - Ejecutar `/skills` → Mensaje de error
   
2. **Jugador con permiso:**
   - Ejecutar `/habilidades` → Abre menú de habilidades
   - Ejecutar `/skill` → Abre menú de habilidades

3. **Operador:**
   - Ejecutar `/habilidades` → Abre menú de habilidades (bypass automático)

---

## 📊 Impacto

### Archivos Modificados
- ✅ `src/main/java/me/apocalipsis/Apocalipsis.java` - Añadido check de permisos

### Sistema de Habilidades
- **No afectado:** El sistema funciona igual para quienes tienen acceso
- **GUIs:** Sin cambios (`SkillTreeGUI.java`)
- **Servicios:** Sin cambios (`SkillService.java`)

### Compatibilidad
- ✅ Compatible con permisos existentes
- ✅ Usa permiso estándar `apocalipsis.admin` (ya definido en plugin.yml)
- ✅ Operadores mantienen acceso automático

---

## 🔧 Configuración de Permisos

### LuckPerms
```
/lp group moderador permission set apocalipsis.admin true
```

### Archivo permissions.yml
```yaml
groups:
  moderador:
    permissions:
      - apocalipsis.admin
```

---

## 📝 Notas Técnicas

1. **Permiso Centralizado:** Se usa `apocalipsis.admin` que ya existe en el sistema
2. **Mensaje Claro:** El usuario sabe que es por falta de permisos
3. **Sin Bypass:** No hay forma de acceder sin el permiso (excepto OP)
4. **Alias Cubiertos:** Todos los alias del comando están protegidos igualmente

---

## ✅ Estado
- [x] Cambios implementados
- [x] Código compilado sin errores
- [x] Documentación actualizada
- [ ] Testing en servidor (pendiente)
