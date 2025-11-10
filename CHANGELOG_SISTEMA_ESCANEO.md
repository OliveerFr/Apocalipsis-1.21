# 📋 CHANGELOG - Sistema de Escaneo de Protecciones

**Versión:** 1.21.8  
**Fecha:** 9 de Noviembre, 2025

---

## ✨ Nuevas Características

### Comandos Nuevos

#### `/avo escanear`
- **Función:** Escanea y visualiza protecciones cercanas en tiempo real
- **Permiso:** Todos los jugadores
- **Cooldown:** Ninguno
- **Output:**
  - Reporte detallado en chat con formato ASCII
  - Partículas verdes en bloques absorbentes (20s)
  - Partículas azules en agua (20s)
  - Partículas blancas indicando techo
  - Estadísticas de reducción por desastre

#### `/avo protecciones`
- **Función:** Muestra guía completa de todas las protecciones
- **Permiso:** Todos los jugadores
- **Cooldown:** Ninguno
- **Output:**
  - Guía educativa formateada
  - Información de cada desastre
  - Porcentajes de reducción exactos
  - Consejos prácticos
  - Sonido de confirmación

---

## 🔧 Cambios Técnicos

### ApocalipsisCommand.java

**Línea 1-20:** Imports agregados
```java
+ import org.bukkit.Location;
+ import org.bukkit.Material;
+ import org.bukkit.Sound;
+ import org.bukkit.Particle;
+ import org.bukkit.block.Block;
+ import java.util.*;
```

**Línea 45-48:** Menú de ayuda actualizado
```java
+ sender.sendMessage("§6=== Protecciones ===");
+ sender.sendMessage("§e/avo escanear §7- Escanea protecciones cercanas");
+ sender.sendMessage("§e/avo protecciones §7- Guía de protecciones");
```

**Línea 122-126:** Switch cases agregados
```java
+ case "escanear":
+     cmdEscanear(sender);
+     break;
+ case "protecciones":
+     cmdProtecciones(sender);
+     break;
```

**Líneas 1242-1360:** Métodos principales implementados
- `cmdEscanear(CommandSender sender)` - ~90 líneas
- `cmdProtecciones(CommandSender sender)` - ~70 líneas

**Líneas 1362-1520:** Métodos auxiliares implementados
- `escanearBloquesAbsorbentes(Location, int)` - ~25 líneas
- `escanearAgua(Location, int)` - ~20 líneas
- `escanearTecho(Player)` - ~10 líneas
- `spawnParticlesEnBloques(Map, Location, Particle)` - ~15 líneas
- `spawnParticlesEnAgua(Location, int, Particle)` - ~15 líneas
- `getNombreMaterial(Material)` - ~15 líneas
- `WaterScanResult` (clase interna) - ~8 líneas

---

## 📊 Estadísticas de Código

| Métrica | Valor |
|---------|-------|
| Líneas agregadas | ~350 |
| Métodos nuevos | 8 |
| Clases internas | 1 |
| Comandos nuevos | 2 |
| Imports nuevos | 6 |

---

## 🎨 Detalles de Implementación

### Escaneo de Terremoto
```java
// Materiales detectados (24 tipos)
- Lanas: 16 colores (WHITE, ORANGE, MAGENTA, etc.)
- Slime Block
- Blue Ice, Packed Ice, Ice
- Honey Block
- Hay Block
- Sponge, Wet Sponge

// Cálculos
Radio: 6 bloques
Cap: 5 bloques efectivos
Reducción Shake: 15% por bloque
Reducción Break: 20% por bloque
Reducción Daño: 25% por bloque
```

### Escaneo de Lluvia de Fuego
```java
// Detección de agua
Área: 3x3x3 (27 bloques)
Tipos: WATER (fluido y estacionario)
Profundidad: 2+ bloques verticales

// Reducción
Agua normal: -60% explosión
Agua profunda: -60% + inmune evaporación
```

### Escaneo de Huracán
```java
// Detección de techo
Rango: 1-5 bloques arriba
Tipo: Material.isSolid() && != BARRIER

// Reducción
Techo: -60% empuje
Agachado: -55% empuje
Combo: -85% empuje total
```

---

## 🐛 Bugs Corregidos

### Error de Compilación
**Problema:** `Particle.WATER_BUBBLE` no existe en Paper 1.21.8  
**Solución:** Cambiado a `Particle.BUBBLE_POP`  
**Línea:** 1306

---

## ⚙️ Configuración

### Sin Cambios en YAML
Este sistema no requiere configuración adicional. Utiliza los valores existentes de:
- `desastres.yml` (absorcion_impacto.radio_deteccion)
- `desastres.yml` (absorcion_impacto.materiales)

---

## 🔄 Compatibilidad

### Versiones Soportadas
- ✅ Paper 1.21.8
- ✅ Java 21
- ✅ Maven 3.9.11

### Dependencias
- ✅ Ninguna dependencia nueva
- ✅ Compatible con todos los plugins existentes

---

## 🧪 Testing

### Compilación
```bash
mvn clean package -DskipTests
[INFO] BUILD SUCCESS
[INFO] Total time: 19.421 s
```

### Warnings
- No nuevos warnings introducidos
- Warnings heredados existentes (deprecations no críticos)

---

## 📝 Notas de Migración

### Para Servidores Existentes
1. Reemplazar JAR en `plugins/`
2. Reiniciar servidor
3. No requiere cambios de configuración
4. No requiere borrar data

### Comandos Antiguos
- ✅ Todos los comandos existentes funcionan igual
- ✅ No hay breaking changes

---

## 🎯 Uso Recomendado

### Para Jugadores
```
1. Construir base
2. Usar /avo protecciones para aprender
3. Usar /avo escanear para verificar
4. Ajustar construcción según feedback
5. Re-escanear antes de cada desastre
```

### Para Admins
```
1. Enseñar comandos a jugadores nuevos
2. Incluir en tutorial del servidor
3. Mencionar en /help o /ayuda
4. Agregar a wiki/documentación
```

---

## 🔮 Roadmap Futuro

### v1.21.9 (Próxima)
- [ ] GUI clickeable con inventory
- [ ] Items visuales representando protecciones
- [ ] Tooltips interactivos

### v1.21.10
- [ ] Historial de escaneos
- [ ] Comparación día a día
- [ ] Alertas de deterioro

### v1.21.11
- [ ] Recomendaciones automáticas
- [ ] Cálculo de materiales necesarios
- [ ] Integración con sistema de logros

---

## 📞 Reporte de Bugs

**Si encuentras problemas:**
1. Activar debug: `/avo debug on`
2. Reproducir el problema
3. Revisar `logs/latest.log`
4. Reportar con contexto completo

---

*Compilado exitosamente el 9/11/2025 a las 13:04* ✅
