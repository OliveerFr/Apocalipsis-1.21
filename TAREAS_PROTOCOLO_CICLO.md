# 📋 TAREAS PENDIENTES - PROTOCOLO NUEVO CICLO

## 🎯 Objetivo
Implementar y verificar el sistema completo de Nuevo Ciclo con Multiverse Core, garantizando todas las capas de seguridad para evitar transferencia de items entre mundos.

---

## ✅ FASE 1: IMPLEMENTACIÓN BASE (COMPLETADO)

### Core del Sistema
- [x] Crear `CicloManager.java` - Orquestador principal
- [x] Crear `WorldInventoryManager.java` - Gestión de inventarios por mundo
- [x] Crear `WorldDataManager.java` - Gestión de datos de jugador por mundo
- [x] Crear `ItemSanitizer.java` - Validador y limpiador de items
- [x] Crear `WorldChangeListener.java` - Listener de cambios de mundo
- [x] Crear `WorldProtectionListener.java` - Listener de protección
- [x] Crear `ciclos.yml` - Archivo de configuración
- [x] Integrar en `Apocalipsis.java` - Inicialización del sistema
- [x] Agregar comandos en `ApocalipsisCommand.java`
- [x] Crear documentación `SISTEMA_CICLOS.md`

---

## ✅ FASE 2: INTEGRACIÓN Y AJUSTES (COMPLETADO)

### 2.1 - Restricciones de Permisos
- [x] **Teleporte solo para admins** - Solo jugadores con `apocalipsis.ciclo.admin` pueden teleportarse
- [x] Verificar permisos en comando `/avo ciclo teleport`
- [x] Añadir mensaje de error apropiado si jugador sin permisos intenta teleportarse
- [x] Documentar en ayuda que teleporte es solo para admins

### 2.2 - Tab Completer
- [x] **Implementar tab completer completo para `/avo ciclo`**
- [x] Autocompletar subcomandos: nuevo, desactivar, listar, info, teleport
- [x] Autocompletar nombres de mundos en comandos que lo requieren
- [x] Filtrar mundos por permisos del jugador
- [x] Añadir sugerencias contextuales

### 2.3 - Métodos Faltantes en Servicios Existentes

#### ExperienceService
- [x] Verificar que existe `setXP(UUID, int)` - para aplicar XP por mundo
- [x] Verificar que existe `setLevel(UUID, int)` - para aplicar nivel por mundo
- [x] Métodos implementados con sobrecarga Player/UUID
- [x] Añadidos getters: `getXP(UUID)` y `getLevel(UUID)`

#### SkillService  
- [x] Crear método `resetPlayer(UUID)` - resetear todas las skills de un jugador
- [x] Crear método `applySkillData(UUID, Set<String>, Map<String, Integer>)` - aplicar skills guardadas
- [x] Verificar compatibilidad con sistema de skills actual
- [x] Añadido método público `getPlayerData(UUID)` para acceder a datos de skills

#### MissionService
- [x] Crear método `setPs(UUID, int)` - setter para Puntos de Supervivencia (ya existe setPS)
- [x] Verificar método `getPs(UUID)` - getter para PS
- [x] Añadir método `resetPlayerMissions(UUID)` para resetear misiones de un jugador por mundo

#### RankService
- [x] Verificar que `updatePlayerRank(UUID)` actualiza correctamente basado en XP
- [x] Asegurar que el rango se recalcula al cargar datos de un mundo
- [x] Añadidos métodos: `getRank(UUID)`, `getXP(UUID)`, `updatePlayerRank(UUID)`

### 2.4 - Ajustes en WorldDataManager
- [x] Implementar métodos de aplicación de datos a servicios
- [x] Completar el método `applyStateToServices()` con las llamadas correctas
- [x] Añadir manejo de errores en carga/guardado de datos
- [x] Implementar rollback en caso de error al aplicar datos

### 2.5 - Mejoras en ItemSanitizer
- [x] Verificar compatibilidad con versión 1.21.8 de Minecraft
- [x] Añadir detección de items con NBT personalizado
- [x] Implementar whitelist de items permitidos (configurable)
- [x] Añadir log detallado de items removidos

---

## ✅ FASE 3: CAPAS DE SEGURIDAD ADICIONALES (COMPLETADO)

### Capa 5: Protección de Comandos
- [x] Crear listener para `PlayerCommandPreprocessEvent`
- [x] Bloquear `/give` entre mundos (si jugador lo usa)
- [x] Bloquear comandos de plugins de economía que den items
- [x] Añadir configuración de comandos bloqueados (sistema dinámico)

### Capa 6: Protección de Animales/Entidades
- [x] Listener para prevenir montar caballo/burro con cofre y cambiar de mundo
- [x] Bloquear teletransporte de mascotas entre mundos con items
- [x] Verificar que llamas/burros con cofre no transfieren items

### Capa 7: Protección de Frames y Soportes
- [x] Verificar que Item Frames no pueden usarse para transferir
- [x] Verificar que Armor Stands no pueden usarse para transferir
- [x] Implementar limpieza de entities al cambiar de mundo si es necesario

### Capa 8: Protección Avanzada
- [x] Implementar marca NBT en items `{CicloWorld: "world_name"}`
- [x] Validar items al ser recogidos/usados en mundo diferente
- [x] Sistema de "cuarentena" para items sospechosos
- [x] Log de intentos de transferencia para análisis

---

## ✅ FASE 4: INTEGRACIÓN CON MULTIVERSE CORE (COMPLETADO)

### 4.1 - Verificación y Setup
- [x] Verificar que Multiverse-Core esté en `pom.xml` como dependency
- [x] Añadir Multiverse API al classpath de compilación
- [x] Configurar `softdepend: [Multiverse-Core]` en `plugin.yml`
- [x] Crear método de detección de Multiverse en startup

### 4.2 - Integración API
- [x] Importar Multiverse API en CicloManager
- [x] Usar `MVWorldManager` para verificar mundos
- [x] Integrar con eventos de Multiverse para cambios de mundo
- [x] Usar permisos de Multiverse si están disponibles

### 4.3 - Creación Automática de Mundos
- [x] Implementar método `createCycleWorld(String name, Environment env)`
- [x] Usar Multiverse API para crear mundos automáticamente
- [x] Configurar generadores, dificultad, reglas de spawn
- [x] Añadir comando `/avo ciclo crear <nombre> <tipo> <dificultad>`

### 4.4 - Sincronización con Multiverse
- [x] Sincronizar configuración de mundos con Multiverse
- [x] Respetar configuraciones de PvP, dificultad, spawns de Multiverse
- [x] Integrar con sistema de portales de Multiverse (automático)
- [x] Prevenir conflictos con otros plugins de mundos

---

## 🧪 FASE 5: TESTING Y VALIDACIÓN (CRÍTICO)

### 5.1 - Tests de Funcionalidad Básica
- [ ] **Test 1**: Crear mundo con Multiverse
  ```
  /mv create world_test NORMAL
  ```
- [ ] **Test 2**: Activar ciclo
  ```
  /avo ciclo nuevo world_test false
  ```
- [ ] **Test 3**: Verificar que aparece en lista
  ```
  /avo ciclo listar
  ```
- [ ] **Test 4**: Teleportarse manualmente
  ```
  /avo ciclo teleport world_test
  ```
- [ ] **Test 5**: Verificar inventario vacío en ciclo nuevo
- [ ] **Test 6**: Verificar nivel 1, XP 0, PS 0
- [ ] **Test 7**: Volver al mundo original
- [ ] **Test 8**: Verificar que inventario/datos se restauraron

### 5.2 - Tests de Seguridad (Items)
- [ ] **Test 9**: Intentar colocar Shulker Box → Debe bloquearse
- [ ] **Test 10**: Intentar abrir Ender Chest vanilla → Debe bloquearse
- [ ] **Test 11**: Meter items en Shulker, cambiar mundo → Items removidos
- [ ] **Test 12**: Usar Bundle con items, cambiar mundo → Bundle removido
- [ ] **Test 13**: Llenar backpack, cambiar mundo → Backpack separado
- [ ] **Test 14**: Usar comando /echest en ciclo → Debe funcionar (separado)
- [ ] **Test 15**: Dropear items, cambiar rápido de mundo → Items no siguen

### 5.3 - Tests de Datos de Jugador
- [ ] **Test 16**: Ganar XP en ciclo, volver a original → XP separado
- [ ] **Test 17**: Comprar skill en ciclo, volver a original → Skill no aplicada en original
- [ ] **Test 18**: Completar misión en ciclo, volver a original → Misión separada
- [ ] **Test 19**: Rankear en ciclo, volver a original → Rango separado
- [ ] **Test 20**: Logout en ciclo, login → Datos del ciclo cargados

### 5.4 - Tests de Edge Cases
- [ ] **Test 21**: Desconectar durante cambio de mundo → Datos guardados
- [ ] **Test 22**: Crash del servidor durante cambio → Recuperación de datos
- [ ] **Test 23**: Múltiples jugadores cambiando simultáneamente → Sin conflictos
- [ ] **Test 24**: Jugador con bypass permission → Puede transferir (solo admin)
- [ ] **Test 25**: Desactivar ciclo → Jugadores pueden seguir en ese mundo

### 5.5 - Tests de Performance
- [ ] **Test 26**: 10 jugadores cambiando de mundo → Lag mínimo
- [ ] **Test 27**: Verificar tamaño de archivos .yml no crece excesivamente
- [ ] **Test 28**: Guardar/cargar datos con inventario lleno → Tiempo aceptable
- [ ] **Test 29**: Memoria del servidor estable tras múltiples cambios

---

## 📝 FASE 6: CONFIGURACIÓN Y DOCUMENTACIÓN (PENDIENTE)

### 6.1 - Configuración en plugin.yml
- [ ] Añadir dependencia de Multiverse-Core
  ```yaml
  depend: [Multiverse-Core]
  ```
- [ ] Añadir permisos del sistema de ciclos
  ```yaml
  permissions:
    apocalipsis.ciclo.admin:
      description: Administrar sistema de ciclos
      default: op
    apocalipsis.ciclo.bypass:
      description: Bypass de protecciones de transferencia
      default: op
  ```

### 6.2 - Configuración en pom.xml
- [ ] Añadir repositorio de Multiverse
  ```xml
  <repository>
    <id>onarandombox</id>
    <url>https://repo.onarandombox.com/content/groups/public/</url>
  </repository>
  ```
- [ ] Añadir dependency de Multiverse-Core
  ```xml
  <dependency>
    <groupId>com.onarandombox.multiversecore</groupId>
    <artifactId>Multiverse-Core</artifactId>
    <version>4.3.1</version>
    <scope>provided</scope>
  </dependency>
  ```

### 6.3 - Documentación para Usuarios
- [ ] Crear guía rápida en español: `GUIA_RAPIDA_CICLOS.md`
- [ ] Crear video tutorial (opcional)
- [ ] Añadir ejemplos de uso comunes
- [ ] Documentar troubleshooting más detallado

### 6.4 - Configuración de Ciclos de Ejemplo
- [ ] Crear plantilla de ciclo mensual
- [ ] Crear plantilla de ciclo de evento
- [ ] Crear plantilla de ciclo de testing
- [ ] Documentar mejores prácticas por tipo de ciclo

---

## 🚀 FASE 7: OPTIMIZACIONES (OPCIONAL)

### 7.1 - Performance
- [ ] Implementar cache de datos en memoria (ya parcialmente hecho)
- [ ] Lazy loading de inventarios (cargar solo cuando se necesita)
- [ ] Compresión de datos en YAML (si archivos muy grandes)
- [ ] Async I/O para guardar/cargar datos grandes

### 7.2 - Features Adicionales
- [ ] Sistema de "preview" antes de activar ciclo (mostrar qué se resetea)
- [ ] Comando `/avo ciclo backup` para backup manual
- [ ] Comando `/avo ciclo restore <backup>` para restaurar
- [ ] Sistema de logs detallado de cambios de mundo por jugador

### 7.3 - Integración con Otros Sistemas
- [ ] Integrar con sistema de eventos del plugin
- [ ] Crear evento "CicloActivadoEvent" para otros plugins
- [ ] API pública para otros developers
- [ ] Webhooks de Discord para notificaciones de ciclos

---

## 🎨 FASE 8: MEJORAS DE EXPERIENCIA DE USUARIO (OPCIONAL)

### 8.1 - GUI/Menús
- [ ] Crear GUI de gestión de ciclos (`/avo ciclo gui`)
- [ ] Menú de selección de mundo para teleporte
- [ ] Panel de información visual de ciclos activos
- [ ] Confirmación visual antes de cambiar de mundo

### 8.2 - Mensajes y Feedback
- [ ] Mejorar mensajes con colores y formato
- [ ] Añadir sonidos al cambiar de mundo
- [ ] Partículas visuales al teleportarse
- [ ] Countdown antes de teleporte masivo (3, 2, 1...)

### 8.3 - Sistema de Avisos
- [ ] Avisar a jugadores cuando se activa un ciclo
- [ ] Notificar cambios en configuración de ciclos
- [ ] Recordatorios de fin de ciclo temporal
- [ ] Estadísticas de ciclo (tiempo jugado, progreso, etc.)

---

## 🔍 FASE 9: VERIFICACIÓN FINAL (CRÍTICO ANTES DE PRODUCCIÓN)

### 9.1 - Checklist de Seguridad
- [ ] ✅ Shulker Boxes bloqueadas
- [ ] ✅ Bundles bloqueados  
- [ ] ✅ Ender Chest vanilla bloqueado
- [ ] ✅ Backpack separado por mundo
- [ ] ✅ Inventario separado por mundo
- [ ] ✅ XP/Nivel separado por mundo
- [ ] ✅ Skills separadas por mundo
- [ ] ✅ Misiones/PS separadas por mundo
- [ ] ✅ Rangos separados por mundo
- [ ] ✅ Items sanitizados al cambiar
- [ ] ✅ Datos guardados automáticamente
- [ ] ✅ Datos cargados automáticamente

### 9.2 - Checklist de Funcionalidad
- [ ] ✅ Comando crear ciclo funciona
- [ ] ✅ Comando desactivar ciclo funciona
- [ ] ✅ Comando listar ciclos funciona
- [ ] ✅ Comando info mundo funciona
- [ ] ✅ Comando teleport funciona
- [ ] ✅ Backup automático funciona
- [ ] ✅ Multiverse-Core detectado correctamente
- [ ] ✅ Mundos verificados correctamente
- [ ] ✅ Permisos funcionan correctamente
- [ ] ✅ Configuración carga correctamente

### 9.3 - Checklist de Estabilidad
- [ ] Sin memory leaks tras uso prolongado
- [ ] Sin corrupción de datos tras crash
- [ ] Sin conflictos con otros plugins
- [ ] Performance aceptable con muchos jugadores
- [ ] Archivos YAML no corrompidos
- [ ] Logs sin errores críticos
- [ ] Sistema de rollback funciona

---

## 📊 FASE 10: MONITOREO POST-DEPLOYMENT

### 10.1 - Primera Semana
- [ ] Monitorear logs diariamente
- [ ] Revisar reportes de jugadores
- [ ] Verificar tamaño de archivos de datos
- [ ] Comprobar performance del servidor
- [ ] Hacer backup manual diario

### 10.2 - Primer Mes
- [ ] Recopilar feedback de jugadores
- [ ] Identificar bugs o problemas
- [ ] Optimizar configuración basado en uso real
- [ ] Ajustar valores por defecto si es necesario
- [ ] Documentar casos de uso reales

### 10.3 - Mantenimiento Continuo
- [ ] Backups automáticos programados
- [ ] Limpieza de datos de ciclos antiguos
- [ ] Actualización de documentación
- [ ] Mantener compatibilidad con nuevas versiones
- [ ] Responder a issues de GitHub

---

## 🎯 PRIORIDADES INMEDIATAS (HACER YA)

### ⚠️ CRÍTICO - NO FUNCIONARÁ SIN ESTO
1. [x] **Implementar tab completer para /avo ciclo** (usabilidad crítica)
2. [x] **Restringir teleporte solo a admins** (seguridad)
3. [x] **Añadir Multiverse-Core a pom.xml** (sin esto no compila)
4. [x] **Verificar métodos setXP/setLevel en ExperienceService**
5. [x] **Implementar métodos faltantes en SkillService**
6. [x] **Añadir método setPs en MissionService**
7. [x] **Completar applyStateToServices() en WorldDataManager**

9. [ ] **Test seguridad: intentar transferir Shulker Box**
10. [ ] **Verificar que datos se guardan/cargan correctamente**
11. [ ] **Verificar compatibilidad con Multiverse-Core 4.x**
12. [ ] **Añadir depend en plugin.yml**/cargan correctamente**
9. [ ] **Verificar compatibilidad con Multiverse-Core 4.x**
10. [ ] **Añadir depend en plugin.yml**
3. [ ] **Todos los tests de FASE 5 pasados**
14. [ ] **Documentación de usuario completa**
15. [ ] **Sistema de backup funcionando**
16. [ ] **Performance aceptable con 10+ jugadores**
17. [ ] **Sistema de backup funcionando**
14. [ ] **Performance aceptable con 10+ jugadores**
15. [ ] **Plan de rollback en caso de problemas**

---

## 📌 NOTAS IMPORTANTES

### Consideraciones de Desarrollo
- **Siempre hacer backup antes de probar** en servidor de producción
- **Testear primero en servidor de desarrollo** con pocos jugadores
- **Mantener compatibilidad** con sistema actual de progreso
- **Documentar cada cambio** para facilitar debugging
- **Versionar archivos de configuración** para poder revertir

### Consideraciones de Seguridad
- **Nunca confiar en el cliente** - toda validación server-side
- **Logs detallados** de intentos de transferencia
- **Permisos restrictivos** por defecto
- **Bypass solo para admins** de confianza absoluta
- **Auditar regularmente** los archivos de datos

### Consideraciones de Performance
- **Limitar tamaño de inventarios** guardados si es necesario
- **Lazy loading** de datos no críticos
- **Cache inteligente** con TTL apropiado
- **Async I/O** para operaciones pesadas
- **Monitorear memoria** constantemente

---

## ✅ CHECKLIST FINAL ANTES DE ACTIVAR EN PRODUCCIÓN

```
[x] FASE 1: Implementación base - COMPLETADO ✅
[x] FASE 2: Integración y ajustes - COMPLETADO ✅
[x] FASE 3: Capas de seguridad - COMPLETADO ✅
[x] FASE 4: Multiverse Core - COMPLETADO ✅
[ ] FASE 5: Testing y validación - CRÍTICO ⚠️
[ ] FASE 6: Configuración - PENDIENTE
[ ] FASE 7: Optimizaciones - OPCIONAL
[ ] FASE 8: UX mejoras - OPCIONAL
[ ] FASE 9: Verificación final - CRÍTICO ⚠️
[ ] FASE 10: Plan de monitoreo - IMPORTANTE

TOTAL: 4/10 fases completadas
```

---

## 🏁 CRITERIOS DE ÉXITO

El sistema estará **listo para producción** cuando:

✅ Todos los tests de FASE 5 pasen sin errores  
✅ No hay transferencia de items posible entre mundos  
✅ Datos se guardan y restauran correctamente 100% del tiempo  
✅ Compatible con Multiverse-Core sin conflictos  
✅ Performance aceptable (<100ms para cambio de mundo)  
✅ Documentación completa para admins y usuarios  
✅ Sistema de backup automático funcionando  
✅ Plan de rollback probado y documentado  

---

**Última actualización:** 2026-01-26  
**Estado:** Fases 1-4 completadas - Sistema listo para testing  
**Próximo paso:** FASE 5 - Testing y validación (29 tests definidos)
