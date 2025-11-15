# Apocalipsis Plugin

Plugin completo de supervivencia con desastres globales, eventos narrativos, sistema de misiones y progresión para Minecraft 1.21+ (Paper/Spigot).

## 🎮 Características

- **Sistema de Desastres Globales**: Terremotos, huracanes, lluvia de fuego y más
- **Eventos Narrativos**: Eco de Brasas y otros eventos especiales
- **Sistema de Misiones**: Misiones dinámicas con recompensas
- **Progresión XP**: Sistema de experiencia y niveles
- **Rangos**: Sistema de rangos basado en progreso
- **Habilidades**: Sistema de habilidades pasivas y activas
- **Protección Anti-Griefing**: Control de bloques colocados por jugadores
- **Optimización TPS**: Adaptación dinámica según rendimiento del servidor

## 📋 Requisitos

- **Minecraft**: 1.21.8 o superior
- **Servidor**: Paper o Spigot
- **Java**: 21 o superior
- **Maven**: 3.6+ (solo para compilación)

## 🚀 Instalación

### Para Usuarios (JAR Precompilado)

1. Descarga el JAR más reciente de [Releases](../../releases)
2. Coloca `Apocalipsis-1.0.1.jar` en la carpeta `plugins/` de tu servidor
3. Reinicia el servidor
4. Los archivos de configuración se generarán automáticamente

### Para Desarrolladores (Compilar desde Código)

#### Opción 1: Script Automatizado (Recomendado)
```bash
# Linux/Mac
./build-enhanced.sh

# Windows
build-enhanced.bat
```

#### Opción 2: Maven Manual
```bash
mvn clean install
```

El JAR compilado estará en `target/Apocalipsis-1.0.1.jar`

## ⚙️ Configuración

Después de la primera ejecución, se generarán estos archivos en `plugins/Apocalipsis/`:

- `config.yml` - Configuración general
- `desastres.yml` - Configuración de desastres
- `eventos.yml` - Configuración de eventos narrativos
- `misiones_new.yml` - Definición de misiones
- `rangos.yml` - Sistema de rangos
- `recompensas.yml` - Recompensas por misiones

## 🎯 Comandos

- `/avo start` - Iniciar sistema de desastres
- `/avo stop` - Detener sistema de desastres
- `/avo disaster <nombre>` - Activar desastre específico
- `/avo event <nombre>` - Activar evento narrativo
- `/avo reload` - Recargar configuraciones
- `/avo info` - Ver información del sistema
- `/avo mission` - Ver misiones disponibles
- `/avo rank` - Ver información de rangos

## 🔧 Troubleshooting

### Error "Cannot Access Apocalipsis"

Este es un error común de compilación. **Soluciones rápidas:**

```bash
# Solución 1: Limpiar y recompilar
mvn clean install -U

# Solución 2: Borrar cache de Maven
rm -rf ~/.m2/repository/io/papermc  # Linux/Mac
rmdir /s /q %USERPROFILE%\.m2\repository\io\papermc  # Windows
mvn clean install
```

**Documentación completa de troubleshooting:**
- 📖 [QUICK_FIX_GUIDE.md](QUICK_FIX_GUIDE.md) - Guía rápida de soluciones
- 📚 [TROUBLESHOOTING_APOCALIPSIS_CLASS.md](TROUBLESHOOTING_APOCALIPSIS_CLASS.md) - Documentación detallada

### Otros Problemas Comunes

#### Error de Versión de Java
```bash
java -version  # Debe mostrar version 21
```
Descarga Java 21: https://adoptium.net/temurin/releases/?version=21

#### Problemas de Red/Dependencias
Verifica conectividad con el repositorio de Paper:
```bash
ping repo.papermc.io
```

#### IDE no Sincronizado
- **IntelliJ IDEA**: File → Invalidate Caches / Restart
- **Eclipse**: Right-click → Maven → Update Project
- **VS Code**: Command Palette → "Java: Clean Workspace"

## 🏗️ Estructura del Proyecto

```
src/main/java/me/apocalipsis/
├── Apocalipsis.java           # Clase principal del plugin
├── commands/                  # Comandos del plugin
├── disaster/                  # Sistema de desastres
├── events/                    # Eventos narrativos
├── experience/                # Sistema XP y habilidades
├── listeners/                 # Event listeners
├── missions/                  # Sistema de misiones
├── state/                     # Gestión de estados globales
├── ui/                        # Interfaz (scoreboard, tablist, etc.)
└── utils/                     # Utilidades y helpers
```

## 📦 Dependencias

- **Paper API**: 1.21.8-R0.1-SNAPSHOT (provided)
- Java 21
- Maven 3.6+

## 🔄 Changelog

Ver [CHANGELOG_MEGA_UPDATE.md](CHANGELOG_MEGA_UPDATE.md) para historial completo de cambios.

### Versión 1.0.1
- Sistema de desastres mejorado
- Eventos narrativos integrados
- Sistema de experiencia unificado
- Optimización de rendimiento
- Protección anti-evasión de desastres

## 🤝 Contribuir

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add: AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

### Guías de Desarrollo

- Usa Java 21 features cuando sea apropiado
- Sigue las convenciones de código existentes
- Añade comentarios para lógica compleja
- Prueba en servidor local antes de PR

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Ver [LICENSE](LICENSE) para más detalles.

## 🛠️ Herramientas de Desarrollo

- **Build Scripts**:
  - `build.bat` - Script básico de compilación (Windows)
  - `build-enhanced.bat` - Script con diagnósticos (Windows)
  - `build-enhanced.sh` - Script con diagnósticos (Linux/Mac)

- **Documentación**:
  - [QUICK_FIX_GUIDE.md](QUICK_FIX_GUIDE.md) - Soluciones rápidas
  - [TROUBLESHOOTING_APOCALIPSIS_CLASS.md](TROUBLESHOOTING_APOCALIPSIS_CLASS.md) - Guía de troubleshooting
  - [SISTEMA_*.md](.) - Documentación de sistemas específicos

## 💬 Soporte

Si encuentras problemas:

1. Revisa [QUICK_FIX_GUIDE.md](QUICK_FIX_GUIDE.md)
2. Revisa [TROUBLESHOOTING_APOCALIPSIS_CLASS.md](TROUBLESHOOTING_APOCALIPSIS_CLASS.md)
3. Busca en [Issues](../../issues)
4. Crea un nuevo Issue con:
   - Versión de Minecraft/Paper
   - Versión de Java
   - Pasos para reproducir el problema
   - Logs relevantes

## 🌟 Características Destacadas

### Sistema de Desastres
- Múltiples tipos de desastres con configuración individual
- Adaptación dinámica según TPS del servidor
- Sistema de advertencias y notificaciones
- Boss bars informativos

### Eventos Narrativos
- Eco de Brasas: Evento narrativo con múltiples fases
- Sistema de oleadas y ritual
- Integración con sistema de misiones

### Progresión
- Sistema XP unificado
- Habilidades pasivas y activas
- Rangos basados en progreso
- Recompensas personalizables

### Optimización
- Monitoreo de TPS en tiempo real
- Adaptación automática de intensidad
- Sistema anti-lag integrado
- Gestión eficiente de entidades

## 📊 Estado del Proyecto

- ✅ Core funcionando
- ✅ Sistema de desastres completo
- ✅ Sistema de eventos narrativos
- ✅ Sistema XP y progresión
- ✅ Protecciones anti-griefing
- 🚧 Sistema de data.yml (planificado)
- 🚧 Más eventos narrativos (en desarrollo)

---

**Desarrollado con ❤️ para la comunidad de Minecraft**

Para más información, consulta la documentación en el directorio del proyecto.
