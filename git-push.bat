@echo off
chcp 65001 > nul
echo ════════════════════════════════════════════════════════════
echo   APOCALIPSIS - PUSH A GITHUB
echo ════════════════════════════════════════════════════════════
echo.

REM Obtener versión del pom.xml
for /f "tokens=2 delims=<>" %%a in ('findstr "<version>" pom.xml ^| findstr -v "<?xml" ^| findstr -v "modelVersion"') do (
    set VERSION=%%a
    goto :version_found
)
:version_found

echo Versión actual: %VERSION%
echo.

REM Verificar si hay cambios sin commitear
git diff --quiet
set DIFF_EXIT=%ERRORLEVEL%
git diff --cached --quiet
set CACHED_EXIT=%ERRORLEVEL%

if %DIFF_EXIT% NEQ 0 (
    echo ⚠️  ATENCIÓN: Se detectaron cambios sin commitear
    echo.
    git status --short
    echo.
    set /p CONTINUAR="¿Deseas continuar y commitear estos cambios? (S/N): "
    if /i not "%CONTINUAR%"=="S" (
        echo.
        echo ❌ Operación cancelada por el usuario
        pause
        exit /b 0
    )
    echo.
) else if %CACHED_EXIT% NEQ 0 (
    echo ⚠️  ATENCIÓN: Se detectaron cambios en staging sin commitear
    echo.
    git status --short
    echo.
    set /p CONTINUAR="¿Deseas continuar y commitear estos cambios? (S/N): "
    if /i not "%CONTINUAR%"=="S" (
        echo.
        echo ❌ Operación cancelada por el usuario
        pause
        exit /b 0
    )
    echo.
)

REM Pedir descripción
set /p DESCRIPCION="Descripción del cambio: "

if "%DESCRIPCION%"=="" (
    echo ❌ Error: Debes proporcionar una descripción
    pause
    exit /b 1
)

echo.
echo 📦 Compilando proyecto...
call mvn clean package -DskipTests -q

if %ERRORLEVEL% NEQ 0 (
    echo ❌ Error en compilación
    pause
    exit /b 1
)

echo ✅ Compilación exitosa
echo.

echo 📝 Preparando commit...
git add .

echo.
echo 💾 Creando commit: v%VERSION% - %DESCRIPCION%
git commit -m "v%VERSION% - %DESCRIPCION%"

if %ERRORLEVEL% NEQ 0 (
    echo ⚠️  No hay cambios para commitear
    pause
    exit /b 0
)

echo.
echo 🚀 Enviando a GitHub...
git push origin main

if %ERRORLEVEL% NEQ 0 (
    echo ❌ Error al hacer push
    echo.
    echo Posibles soluciones:
    echo 1. Verifica tu conexión a internet
    echo 2. Asegúrate de haber configurado el remote: git remote add origin https://github.com/OliveerFr/Apocalipsis-1.21.git
    echo 3. Verifica tus credenciales de GitHub
    pause
    exit /b 1
)

echo.
echo ════════════════════════════════════════════════════════════
echo   ✅ CAMBIOS SUBIDOS A GITHUB EXITOSAMENTE
echo ════════════════════════════════════════════════════════════
echo.
echo Commit: v%VERSION% - %DESCRIPCION%
echo Repositorio: https://github.com/OliveerFr/Apocalipsis-1.21
echo.
pause
