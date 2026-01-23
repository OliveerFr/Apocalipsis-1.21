@echo off
cd /d "%~dp0"
echo Abortando merge si existe...
git merge --abort 2>nul

echo.
echo Reseteando al estado remoto...
git fetch origin
git reset --hard origin/main

echo.
echo Agregando pack de texturas...
git add .gitignore Apocalipsis.zip

echo.
echo Creando commit...
git commit -m "feat: Agregar pack de texturas Apocalipsis.zip al repositorio"

echo.
echo Subiendo a GitHub...
git push

echo.
echo Proceso completado.
pause
