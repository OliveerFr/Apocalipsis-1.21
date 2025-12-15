@echo off
cd /d "z:\riolu\Videos\Eventos\Apocalipsis-1.21.8"
echo Compilando Apocalipsis 1.22.18 con fix de sintaxis YAML...
call mvn clean package -DskipTests -q
if %errorlevel% equ 0 (
    echo.
    echo ============================================
    echo BUILD SUCCESS
    echo ============================================
    dir target\Apocalipsis-1.22.18.jar
    echo.
    echo El JAR esta listo en: target\Apocalipsis-1.22.18.jar
) else (
    echo.
    echo ============================================
    echo BUILD FAILED - revisa los errores arriba
    echo ============================================
)
pause
