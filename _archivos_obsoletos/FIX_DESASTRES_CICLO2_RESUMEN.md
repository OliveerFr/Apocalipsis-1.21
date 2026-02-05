# Fix: Desastres Ciclo 2 No Iniciaban

## Problema
Los desastres del Ciclo 2 (Tormenta Glacial, Tormenta Eléctrica, Erupción Volcánica) **NO iniciaban automáticamente** aunque:
- ✅ `usar_desastres_nuevos: true` estaba activado
- ✅ `weights_ciclo_2` estaba correctamente configurado
- ✅ Cooldown llegaba a 0

## Causa
El método `elegirSegunWeight()` **siempre leía** `desastres.weights` (Ciclo 1) en lugar de `desastres.weights_ciclo_2` (Ciclo 2).

Como los weights del Ciclo 1 estaban en 0:
```yaml
desastres:
  weights:
    huracan: 0
    lluvia_fuego: 0
    terremoto: 0
```
El pool de desastres quedaba vacío → **ningún desastre se iniciaba**.

## Solución
Se modificó `elegirSegunWeight()` para leer dinámicamente la tabla correcta:

```java
boolean usarNuevos = plugin.getConfig().getBoolean("ciclo.usar_desastres_nuevos", true);
String weightsPath = usarNuevos ? "desastres.weights_ciclo_2" : "desastres.weights";
```

## Resultado
✅ Desastres del Ciclo 2 ahora inician correctamente  
✅ Sistema respeta `usar_desastres_nuevos`  
✅ Fácil cambio entre Ciclo 1 y Ciclo 2  
✅ Detección de configuraciones inválidas (weights=0)

## Testing
```bash
# Verificar auto-inicio:
# 1. Asegurar usar_desastres_nuevos: true
# 2. Compilar: compile_quick.bat
# 3. Reiniciar servidor
# 4. Esperar cooldown → debe iniciar desastre Ciclo 2
```

## Archivo Modificado
- [DisasterController.java](src/main/java/me/apocalipsis/disaster/DisasterController.java) - Método `elegirSegunWeight()`
