@echo off
setlocal enabledelayedexpansion

REM Script de versionado rapido para Apocalipsis
REM Uso: version.bat [patch|minor|major] [mensaje opcional]

set "TYPE=%~1"
set "MESSAGE=%~2"

if "%TYPE%"=="" set "TYPE=patch"

echo.
echo ====================================
echo   APOCALIPSIS - Version Manager
echo ====================================
echo.

REM Validar tipo
if /i not "%TYPE%"=="patch" if /i not "%TYPE%"=="minor" if /i not "%TYPE%"=="major" (
    echo [ERROR] Tipo invalido: %TYPE%
    echo Uso: version.bat [patch^|minor^|major] [mensaje]
    exit /b 1
)

REM Ejecutar script PowerShell
if "%MESSAGE%"=="" (
    powershell -ExecutionPolicy Bypass -File "%~dp0version.ps1" -Type %TYPE%
) else (
    powershell -ExecutionPolicy Bypass -File "%~dp0version.ps1" -Type %TYPE% -Message "%MESSAGE%"
)

pause
