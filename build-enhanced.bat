@echo off
REM Script mejorado de compilacion para Apocalipsis Plugin
REM Incluye diagnosticos y troubleshooting

echo.
echo ========================================
echo   APOCALIPSIS - Compilacion Maven
echo   Con Diagnosticos Mejorados
echo ========================================
echo.

REM Verificar si Maven esta instalado
where mvn >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Maven no encontrado en PATH
    echo Por favor instala Maven: https://maven.apache.org/download.cgi
    pause
    exit /b 1
)

REM Verificar version de Java
echo [INFO] Verificando version de Java...
java -version 2>&1 | findstr /C:"21" >nul
if %ERRORLEVEL% NEQ 0 (
    echo [ADVERTENCIA] Este proyecto requiere Java 21
    echo Version actual de Java:
    java -version
    echo.
    echo Descarga Java 21 desde: https://adoptium.net/temurin/releases/?version=21
    echo.
    choice /C SN /M "Deseas continuar de todos modos"
    if errorlevel 2 exit /b 1
)

REM Verificar conectividad con repositorio Paper MC
echo [INFO] Verificando conectividad con Paper MC repository...
ping -n 1 repo.papermc.io >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ADVERTENCIA] No se puede acceder a repo.papermc.io
    echo Esto puede causar problemas al descargar dependencias
    echo.
    echo Soluciones:
    echo 1. Verifica tu conexion a Internet
    echo 2. Verifica configuracion de firewall/proxy
    echo 3. Consulta TROUBLESHOOTING_APOCALIPSIS_CLASS.md
    echo.
    choice /C CN /M "Deseas continuar"
    if errorlevel 2 exit /b 1
)

echo.
echo [1/4] Limpiando build anterior y cache...
call mvn clean

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Fallo al limpiar
    echo.
    echo Intenta ejecutar manualmente:
    echo   mvn clean -X
    echo.
    pause
    exit /b 1
)

echo.
echo [2/4] Actualizando dependencias (esto puede tomar tiempo)...
call mvn dependency:resolve -U

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Fallo al resolver dependencias
    echo.
    echo Posibles causas:
    echo - Paper API no disponible
    echo - Problemas de red
    echo - Cache de Maven corrupto
    echo.
    echo Soluciones:
    echo 1. Ejecuta: mvn clean install -U
    echo 2. Borra cache: rmdir /s /q %%USERPROFILE%%\.m2\repository\io\papermc
    echo 3. Consulta TROUBLESHOOTING_APOCALIPSIS_CLASS.md
    echo.
    pause
    exit /b 1
)

echo.
echo [3/4] Compilando plugin...
call mvn compile

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Fallo la compilacion
    echo.
    echo Si ves "cannot access Apocalipsis":
    echo 1. Limpia el proyecto: mvn clean
    echo 2. Actualiza en tu IDE: Reload Maven Project
    echo 3. Consulta TROUBLESHOOTING_APOCALIPSIS_CLASS.md
    echo.
    pause
    exit /b 1
)

echo.
echo [4/4] Empaquetando JAR final...
call mvn package -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Fallo al empaquetar
    pause
    exit /b 1
)

echo.
echo [INFO] Verificando archivo JAR...
if exist "target\Apocalipsis-1.0.1.jar" (
    echo.
    echo ========================================
    echo   ✓ COMPILACION EXITOSA!
    echo ========================================
    echo.
    echo Archivo generado: target\Apocalipsis-1.0.1.jar
    for %%I in ("target\Apocalipsis-1.0.1.jar") do echo Tamano: %%~zI bytes
    echo.
    echo Instrucciones:
    echo 1. Copia el JAR a la carpeta plugins/ de tu servidor Paper
    echo 2. Reinicia el servidor
    echo 3. Los archivos de config se generaran automaticamente
    echo.
    echo Archivos de configuracion que se generan:
    echo - config.yml
    echo - desastres.yml
    echo - eventos.yml
    echo - misiones_new.yml
    echo - rangos.yml
    echo - recompensas.yml
    echo.
) else (
    echo [ERROR] No se genero el archivo JAR esperado
    echo.
    echo Verifica:
    echo 1. Que la compilacion termino sin errores
    echo 2. Que target/ tiene permisos de escritura
    echo 3. Los logs de Maven arriba para mas detalles
    echo.
    pause
    exit /b 1
)

echo Presiona cualquier tecla para salir...
pause >nul
