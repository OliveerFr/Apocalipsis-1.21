@echo off
cd /d "%~dp0"
echo Compilando Apocalipsis...
C:\apache-maven-3.8.8\bin\mvn.cmd package -DskipTests -q
if errorlevel 1 (
    echo ERROR en compilacion
    pause
    exit /b 1
)
echo.
echo ========================================
echo COMPILACION EXITOSA
echo ========================================
echo.
dir /B target\Apocalipsis*.jar
echo.
pause
