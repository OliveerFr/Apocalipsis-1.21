@echo off
REM Script de compilacion para Apocalipsis Plugin

echo.
echo ========================================
echo   APOCALIPSIS - Compilacion Maven
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

echo [1/3] Limpiando build anterior...
call mvn clean

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Fallo al limpiar
    pause
    exit /b 1
)

echo.
echo [2/3] Compilando plugin...
call mvn package

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Fallo la compilacion
    pause
    exit /b 1
)

echo.
echo [3/3] Verificando archivo JAR...
if exist "target\Apocalipsis-1.0.0.jar" (
    echo.
    echo ========================================
    echo   COMPILACION EXITOSA!
    echo ========================================
    echo.
    echo Archivo generado: target\Apocalipsis-1.0.0.jar
    echo.
    echo Instrucciones:
    echo 1. Copia el JAR a la carpeta plugins/ de tu servidor
    echo 2. Reinicia el servidor
    echo 3. Los archivos de config se generaran automaticamente
    echo.
) else (
    echo [ERROR] No se genero el archivo JAR
    pause
    exit /b 1
)

pause
