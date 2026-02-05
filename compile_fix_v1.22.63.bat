@echo off
echo Compilando FIX v1.22.63 - Recompensas duplicadas
echo.
mvn package -DskipTests
echo.
if %ERRORLEVEL% EQU 0 (
    echo BUILD SUCCESS
    dir target\*.jar
) else (
    echo BUILD FAILED
)
pause
